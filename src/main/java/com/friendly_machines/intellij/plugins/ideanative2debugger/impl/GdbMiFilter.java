// Copyright 2022 Danny Milosavljevic. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.

package com.friendly_machines.intellij.plugins.ideanative2debugger.impl;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

import static com.friendly_machines.intellij.plugins.ideanative2debugger.impl.GdbMiProducer.parseToken;

public class GdbMiFilter {
    private final DebugProcess myProcess;
    private final Project myProject;
    private final OutputStream myChildIn;
    private final GdbOsProcessHandler myChildOut;
    //private final InputStream myChildOut;
    //private final GdbMiProducer myReaderThread;

    public GdbMiFilter(DebugProcess process, @NotNull Project project, GdbOsProcessHandler childIO) throws IOException {
        myProcess = process;
        myProject = project;
        //myChildOut = childOut;
        //Native2DebugProcess process = Native2DebugProcess.getInstance(myOsProcessHandler);
        myChildOut = childIO;
        myChildIn = childIO.getProcessInput();
        //myReaderThread = new GdbMiProducer(new BufferedReader(new InputStreamReader(childIO.getInputStream(), StandardCharsets.UTF_8), 1), process);

        // TODO: PipedReader, PipedWriter

        // https://www.kfu.com/~nsayer/Java/jni-filedesc.html
        // new FileReader(FileDescriptor)
    }

//    / Read the sync response from gdb. If there are async responses, those are handled as a side-effect.
    private GdbMiStateResponse readResponse() throws InterruptedException {
        return myChildOut.readResponse();
    }

    private int requestId = 0;

    private static byte digit(byte value) {
        return (byte) ((byte) '0' + value);
    }

    // Given TEXT, escapes it into a C string so you can use it as an GDB parameter in an input stream
    @NotNull
    private static byte[] makeCString(byte[] text) {
        ByteArrayOutputStream s = new ByteArrayOutputStream();
        s.write((byte) '"'); // quote
        for (byte b : text) {
            if (b == (byte) ' ') {
                s.write((byte) ' '); // space
            } else if (b < 32 || b > 127 || b == (byte) '\\') {
                s.write((byte) '\\');
                s.write(digit((byte) ((b >> 6) & 7)));
                s.write(digit((byte) ((b >> 3) & 7)));
                s.write(digit((byte) ((b >> 0) & 7)));
            } else {
                s.write(b);
            }
        }
        s.write((byte) '"'); // quote
        return s.toByteArray();
    }

    // Given TEXT, escapes it (if necessary) into a C string so you can use it as an GDB parameter in an input stream
    private static byte[] maybeEscape(byte[] text) {
        boolean escaping_needed = false;
        for (byte b : text) {
            if (b <= 32 || b > 127 || b == (byte) '\\') {
                escaping_needed = true;
                break;
            }
        }
        if (escaping_needed) {
            return makeCString(text);
        } else {
            return text;
        }
    }

    public GdbMiStateResponse gdbSend(String operation, String[] options, String[] parameters) {
        System.err.println("gdbSend " + operation);
        System.err.flush();
        try {
            ++requestId;
            myChildIn.write(Integer.toString(requestId).getBytes(StandardCharsets.UTF_8));
            myChildIn.write(operation.getBytes(StandardCharsets.UTF_8));
            for (String option : options) {
                myChildIn.write(" ".getBytes(StandardCharsets.UTF_8));
                myChildIn.write(maybeEscape(option.getBytes(StandardCharsets.UTF_8)));
            }
            if (parameters.length > 0) {
                myChildIn.write(" --".getBytes(StandardCharsets.UTF_8));
                for (String parameter : parameters) {
                    myChildIn.write(" ".getBytes(StandardCharsets.UTF_8));
                    myChildIn.write(maybeEscape(parameter.getBytes(StandardCharsets.UTF_8)));
                }
            }
            myChildIn.write("\r\n".getBytes(StandardCharsets.UTF_8));
            myChildIn.flush();
            GdbMiStateResponse response = readResponse();
            System.err.println("done gdbSend " + operation);
            System.err.flush();
            return response;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> gdbCall(String operation, String[] options, String[] parameters) throws GdbMiOperationException {
        GdbMiStateResponse response = gdbSend(operation, options, parameters);
        assert response.getMode() == '^';
        if (!"done".equals(response.getKlass()) && !"connected".equals(response.getKlass()) && !"running".equals(response.getKlass())) { // "connected" is returned by -target-select only; "running" is by exec-run
            throw new GdbMiOperationException(response);
        }
        return response.getAttributes();
    }

    public void startReaderThread() {
        //myReaderThread.start();
    }

    private static char consume(Scanner scanner) {
        return scanner.next().charAt(0);
    }
    public void processLine(@NotNull String line) {
        line = line.strip();
        if ("(gdb)".equals(line)) {
            return;
        }

        Scanner scanner = new Scanner(line);
        scanner.useDelimiter(""); // character by character mode
        Optional<String> token = parseToken(scanner);
        // "+": contains on-going status information about the progress of a slow operation.
        // "*": contains asynchronous state change on the target (stopped, started, disappeared)
        // "=": contains supplementary information that the client should handle (e.g., a new breakpoint information)
        // "^": sync command result
        if (scanner.hasNext("[*+=]")) {
            GdbMiStateResponse response = GdbMiStateResponse.decode(token, scanner);

            // "*stopped"
            // "=breakpoint-modified"
            //ApplicationManager.getApplication().invokeLater(() -> {
                myProcess.handleGdbMiStateOutput(response);
            //});
        } else if (scanner.hasNext("[~@&]")) { // streams
            char mode = consume(scanner);
            @NotNull String text = GdbMiProducer.parseCString(scanner);
            //ApplicationManager.getApplication().invokeLater(() -> {
                myProcess.handleGdbTextOutput(mode, text);
            //});
        } else if (scanner.hasNext("-")) { // our echo
        } else {
        }
    }

}
