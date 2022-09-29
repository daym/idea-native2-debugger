// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.

package com.friendly_machines.intellij.plugins.ideanative2debugger.impl;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import jtermios.windows.WinAPI;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class PtyOnlyWin implements PtyOnly {

    protected String    pipeName="\\\\.\\pipe\\" + File.createTempFile("native2", "gdb").getName();
    protected final WinNT.HANDLE hNamedPipe;

    protected volatile boolean isConnected = false;

    //protected BufferedReader bufReader = new BufferedReader();

    /**
     * // GDB/MI3 protocol: 7-bit ASCII
     * @return
     */
    @Override
    public InputStream getInputStream() {
        return new InputStream() {
            private final ByteBuffer buf = ByteBuffer.allocate(256);

            private boolean hasDataBuffered = false;

            @Override
            public int read() throws IOException {
                final byte[] b = new byte[1];
                loop: while (true) {
                    switch (this.read(b, 0, 1)) {
                        case 0:
                            // try again
                            Thread.yield();
                            continue;
                        case 1:
                            // ok
                            break loop;
                        default:
                            throw new EOFException();
                    }
                }
                return b[0];
            }

            @Override
            public int read(@NotNull byte[] b, int off, int len) throws IOException {
                if (hasDataBuffered){
                    int bufByteCount = Math.min(len, buf.remaining());
                    if (bufByteCount > 0) {
                        buf.get(b, off, bufByteCount);
                        if (!buf.hasRemaining()) {
                            buf.clear();
                            hasDataBuffered = false;
                        }
                        return bufByteCount;
                    }
                }
                if (!isConnected) {
                    throw new IOException("Call #waitForClient() first!");
                }
                IntByReference lpNumberOfBytesRead = new IntByReference(0);
                if (!Kernel32.INSTANCE.ReadFile(hNamedPipe, buf.array(), buf.capacity(), lpNumberOfBytesRead, null)) {
                    int lastError = Kernel32.INSTANCE.GetLastError();
                    if (lastError == WinError.ERROR_BROKEN_PIPE) {
                        //Thread.currentThread().interrupt();
                        throw new EOFException("Pipe closed. Bytes read: " + lpNumberOfBytesRead.getValue());
                    }
                    throw new IOException("Couldn't read from pipe. LastError:" + Integer.toUnsignedString(lastError, 16));
                }
                int bytesRead = lpNumberOfBytesRead.getValue();
                // EOF?
                if (bytesRead == 0) {
                    throw new EOFException();
                }
                hasDataBuffered = true;
                buf.limit(bytesRead);
                buf.rewind();
                buf.get(b, off, Math.min(len, bytesRead));
                if (!buf.hasRemaining()) {
                    buf.clear();
                    hasDataBuffered = false;
                }
                return bytesRead;
            }
        };
    }

    public void waitForClient() throws IOException {
        if (!Kernel32.INSTANCE.ConnectNamedPipe(hNamedPipe, null)) {
            int errno = Kernel32.INSTANCE.GetLastError();
            if (errno != WinError.ERROR_PIPE_CONNECTED) {
                throw new IOException("Couldn't connect to our own created named pipe. errno:" + Integer.toUnsignedString(errno, 16));
            }
        }
        this.isConnected = true;
    }

    @Override
    public OutputStream getOutputStream() {
        return new OutputStream() {

            private final IntByReference lpNumberOfBytesWritten = new IntByReference(0);

            @Override
            public void write(@NotNull byte[] expData, int off, int len) throws IOException {
                if (!isConnected) {
                    throw new IOException("Call #waitForClient() first!");
                }
                if (off != 0) {
                    expData = Arrays.copyOfRange(expData, off, off + len);
                }

                if (!Kernel32.INSTANCE.WriteFile(hNamedPipe, expData, len, lpNumberOfBytesWritten, null)) {
                    throw new IOException("not all bytes written, " + lpNumberOfBytesWritten.getValue() + " out of " + len);
                }
                assert lpNumberOfBytesWritten.getValue() == len : "Blame M$ for their implementation not complying to their own API documentation. This must not happen using a BLOCKING named pipe.";
            }

            @Override
            public void write(int b) throws IOException {
                if (!isConnected) {
                    throw new IOException("Call #waitForClient() first!");
                }
                this.write(new byte[]{(byte) (b & 0xFF)}, 0, 1);
            }
        };
    }

    @Override
    public void close() throws IOException {
        if (!Kernel32.INSTANCE.CloseHandle(hNamedPipe)) {
            throw new IOException("Couldn't close pipe handle, wtf.");
        }
    }

    @Override
    public String getSlaveName() {
        return pipeName;
    }

    public PtyOnlyWin() throws IOException {
        hNamedPipe = Kernel32.INSTANCE.CreateNamedPipe(pipeName,
                WinBase.PIPE_ACCESS_DUPLEX | WinNT.FILE_FLAG_WRITE_THROUGH | WinAPI.FILE_FLAG_FIRST_PIPE_INSTANCE,        // dwOpenMode
                WinBase.PIPE_TYPE_BYTE | WinBase.PIPE_READMODE_BYTE | WinBase.PIPE_WAIT | WinBase.PIPE_REJECT_REMOTE_CLIENTS ,    // dwPipeMode
                1,    // nMaxInstances,
                0,    // nOutBufferSize,
                0,    // nInBufferSize,
                1000,    // nDefaultTimeOut,
                null);    // lpSecurityAttributes
        if (WinBase.INVALID_HANDLE_VALUE.equals(hNamedPipe)) {
            throw new IOException("Couldn't create named pipe for communication. LastError:" + Integer.toUnsignedString(Kernel32.INSTANCE.GetLastError(), 16));
        }
    }

    public static void main(String[] args) {
        try {
            if (false) {
                var baos = new ByteArrayInputStream("oho\r\na".getBytes(StandardCharsets.UTF_8));
                var bufRdr = new BufferedReader(new InputStreamReader(baos, StandardCharsets.UTF_8));
                System.out.println("#1:");
                System.out.println(bufRdr.readLine());
                System.out.println("#2:");
                System.out.println(bufRdr.readLine());
            }

            if (true) {
                var ding = new PtyOnlyWin();
                System.out.println(ding.pipeName + " -- ENTER druecken");
                System.in.read();
                System.out.println("waitForClient...");

                ding.waitForClient();
                //ding.isConnected = true;

                OutputStream outStream = ding.getOutputStream();


                System.out.println("Going to write to pipe...");
                new PrintWriter(outStream, true).println("1-exec-run");

                System.out.println("Going to read from pipe...");
                var bRdr = new BufferedReader(new InputStreamReader(ding.getInputStream()));
                System.out.println(bRdr.readLine());

                new PrintWriter(outStream, true).println("Pipe eternal.");
                System.out.println("Written #2 to pipe. Reading response #2...");
                System.out.println(bRdr.readLine());

                System.out.println("Ended gracefully.");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
