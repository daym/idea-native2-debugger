// Copyright 2022 Danny Milosavljevic. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.

package com.friendly_machines.intellij.plugins.native2Debugger.impl;

import com.intellij.openapi.project.Project;
import com.pty4j.unix.PTYOutputStream;
import com.pty4j.unix.Pty;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class GdbMiFilter {
    private final DebugProcess myProcess;
    private final Project myProject;
    private final PTYOutputStream myChildIn;
    //private final InputStream myChildOut;
    private final GdbMiProducer myReaderThread;

    public GdbMiFilter(DebugProcess process, @NotNull Project project, Pty childOut, PTYOutputStream childIn) {
        myProcess =  process;
        myProject = project;
        //myChildOut = childOut;
        // TODO: InputStreamReader ?
        //Native2DebugProcess process = Native2DebugProcess.getInstance(myOsProcessHandler);
        myReaderThread = new GdbMiProducer(childOut, process);
        myChildIn = childIn;

        myReaderThread.start();
        // TODO: PipedReader, PipedWriter

        // https://www.kfu.com/~nsayer/Java/jni-filedesc.html
        // new FileReader(FileDescriptor)
    }

    /// Read the sync response from gdb. If there are async responses, those are handled as a side-effect.
    private GdbMiStateResponse readResponse() throws IOException {
        GdbMiStateResponse response = myReaderThread.readResponse();
        return response;
    }

    private int counter = 0;

    private static byte digit(byte value) {
        return (byte) (48 + value);
    }
    // Given TEXT, escapes it (if necessary) into a C string so you can use it as an GDB parameter in an input stream
    private static byte[] escape(byte[] text) {
        boolean escaping_needed = false;
        for (byte b: text) {
            if (b <= 32 || b > 127 || b == 92) {
                escaping_needed = true;
                break;
            }
        }
        if (escaping_needed) {
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            s.write(34); // quote
            for (byte b: text) {
                if (b == 32) {
                    s.write(32); // space
                } else if (b < 32 || b > 127 || b == 92) {
                    s.write(92); // backslash
                    s.write(digit((byte) ((b >> 6) & 7)));
                    s.write(digit((byte) ((b >> 3) & 7)));
                    s.write(digit((byte) ((b >> 0) & 7)));
                } else {
                    s.write(b);
                }
            }
            s.write(34); // quote
            return s.toByteArray();
        } else {
            return text;
        }
    }

    public GdbMiStateResponse gdbSend(String operation, String[] options, String[] parameters) {
        try {
            ++counter;
            myChildIn.write(Integer.toString(counter).getBytes(StandardCharsets.UTF_8));
            myChildIn.write(operation.getBytes(StandardCharsets.UTF_8));
            for (String option : options) {
                myChildIn.write(" ".getBytes(StandardCharsets.UTF_8));
                myChildIn.write(escape(option.getBytes(StandardCharsets.UTF_8)));
            }
            if (parameters.length > 0) {
                myChildIn.write(" --".getBytes(StandardCharsets.UTF_8));
                for (String parameter : parameters) {
                    myChildIn.write(" ".getBytes(StandardCharsets.UTF_8));
                    myChildIn.write(escape(parameter.getBytes(StandardCharsets.UTF_8)));
                }
            }
            myChildIn.write("\r\n".getBytes(StandardCharsets.UTF_8));
            myChildIn.flush();
            GdbMiStateResponse response = readResponse();
            return response;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public HashMap<String, Object> gdbCall(String operation, String[] options, String[] parameters) throws GdbMiOperationException {
        GdbMiStateResponse response = gdbSend(operation, options, parameters);
        assert response.getMode() == '^';
        if (!"done".equals(response.getKlass()) && !"connected".equals(response.getKlass()) && !"running".equals(response.getKlass())) { // "connected" is returned by -target-select only; "running" is by exec-run
            throw new GdbMiOperationException(response);
        }
        return response.getAttributes();
    }
}
