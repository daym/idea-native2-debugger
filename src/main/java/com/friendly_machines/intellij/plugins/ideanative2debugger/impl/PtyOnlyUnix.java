// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.

package com.friendly_machines.intellij.plugins.ideanative2debugger.impl;

import com.pty4j.unix.Pty;
import com.sun.jna.platform.win32.WinError;
import jtermios.JTermios;
import org.jetbrains.annotations.NotNull;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PtyOnlyUnix implements PtyOnly {
    private Pty pty;

    @Override
    public InputStream getInputStream() {
        return new InputStream() {
            // TODO: InputStreamReader in order to decode from UTF-8 ?
            private final int fd = getMasterFD();
            private final byte[] buf = new byte[1]; // TODO: Buffer more

            @Override
            public int read() throws IOException {
                int count = JTermios.read(fd, buf, buf.length);
                if (count == 0) {
                    throw new EOFException();
                } else if (count == -1) {
                    if (JTermios.errno() == 0x20) {
                        throw new EOFException();
                    }
                    throw new IOException();
                }
                return buf[0];
            }

            @Override
            public int read(@NotNull byte[] b, int off, int len) throws IOException {
                if (off != 0) {
                    throw new RuntimeException("Offset not supported.");
                }
                int count = JTermios.read(fd, b, len);
                if (count == 0) {
                    throw new EOFException();
                } else if (count == -1) {
                    if (JTermios.errno() == 0x20) {
                        throw new EOFException();
                    }
                    throw new IOException();
                }
                return count;
            }
        };
    }

    @Override
    public OutputStream getOutputStream() {
        return pty.getOutputStream();
    }

    @Override
    public void close() throws IOException {
        pty.close();
    }

    @Override
    public String getSlaveName() {
        return pty.getSlaveName();
    }

    private int getMasterFD() {
        return pty.getMasterFD();
    }

    public PtyOnlyUnix() throws IOException {
        pty = new Pty(true, true);
    }
}
