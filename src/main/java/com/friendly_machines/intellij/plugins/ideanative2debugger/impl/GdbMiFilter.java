// Copyright 2022 Danny Milosavljevic. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.

package com.friendly_machines.intellij.plugins.ideanative2debugger.impl;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

public class GdbMiFilter {
    private final DebugProcess myProcess;
    private final Project myProject;
    private final PrintStream myChildIn;
    private final GdbOsProcessHandler myChildOut;
    //private final InputStream myChildOut;
    //private final GdbMiProducer myReaderThread;

    public GdbMiFilter(DebugProcess process, @NotNull Project project, GdbOsProcessHandler childIO) {
        myProcess = process;
        myProject = project;
        //myChildOut = childOut;
        //Native2DebugProcess process = Native2DebugProcess.getInstance(myOsProcessHandler);
        myChildOut = childIO;
        // pucgenie: It is 7-bit ASCII per mi3 specification, so... use US_ASCII instead?
        myChildIn = new PrintStream(childIO.getProcessInput(), false, StandardCharsets.UTF_8);
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

    // Given TEXT, escapes it into a C string so you can use it as an GDB parameter in an output stream
    private static void makeCString(byte[] text, OutputStream s) throws IOException {
        s.write((byte) '"'); // quote
        for (byte b : text) {
            if (b == (byte) ' ') {
                s.write((byte) ' '); // space
            } else if (b < 32 || b < 0 || b == (byte) '\\') {
                s.write((byte) '\\');
                var bb = Byte.toUnsignedInt(b);
                s.write(digit((byte) ((bb >> 6) & 7)));
                s.write(digit((byte) ((bb >> 3) & 7)));
                s.write(digit((byte) ((bb >> 0) & 7)));
            } else {
                s.write(b);
            }
        }
        s.write((byte) '"'); // quote
    }

    // Given TEXT, escapes it (if necessary) into a C string so you can use it as an GDB parameter in an input stream
    private static void maybeEscape(byte[] text, OutputStream out) throws IOException {
        boolean escaping_needed = false;
        for (byte b : text) {
            if (b <= 32 || b < 0 || b == (byte) '\\') {
                escaping_needed = true;
                break;
            }
        }
        if (escaping_needed) {
            makeCString(text, out);
            return;
        } else {
            out.write(text);
            return;
        }
    }

    public GdbMiStateResponse gdbSend(String operation, Iterable<String> options, Iterable<String> parameters) throws IOException, InterruptedException {
//        println("gdbSend " + operation);
        ++requestId;
        myChildIn.print(Integer.toString(requestId));
        myChildIn.print(operation);
        for (String option : options) {
            myChildIn.print(" ");
            maybeEscape(option.getBytes(StandardCharsets.UTF_8), myChildIn);
        }
        // pucgenie: Maybe just use this one iterator instead of creating a second one...
        if (parameters.iterator().hasNext()) {
            // mind the space being PREfixed to all following parameters
            myChildIn.print(" --");
            for (var parameterStr : parameters) {
                myChildIn.print(" ");
                maybeEscape(parameterStr.getBytes(StandardCharsets.UTF_8), myChildIn);
            }
        }
        myChildIn.print("\r\n");
        myChildIn.flush();
        return readResponse();
    }

    public Map<String, ?> gdbCall(String operation, Iterable<String> options, Iterable<String> parameters) throws GdbMiOperationException, IOException, InterruptedException {
        var response = gdbSend(operation, options, parameters);
        if (response.getMode() != '^') {
            // pucgenie: I don't like that repacking just for adding an error message. Data is lost too (see com.friendly_machines.intellij.plugins.ideanative2debugger.impl.GdbMiStateResponse#errorResponse ).
            throw new GdbMiOperationException(GdbMiStateResponse.errorResponse(response.getToken(), response.getMode(), response.getKlass(), "Invalid response mode, expected '^'."));
        }
        // "connected" is returned by -target-select only; "running" is by exec-run
        switch (response.getKlass()) {
            case "done":
            case "connected":
            case "running":
                return response.getAttributes();
            default:
                throw new GdbMiOperationException(response);
        }
    }

    public void startReaderThread() {
        //myReaderThread.start();
    }

    private static char consume(Scanner scanner) {
        return scanner.next().charAt(0);
    }
    public void processAsync(@Nullable Optional<String> token, @NotNull Scanner scanner) throws IOException, InterruptedException {
        scanner.useDelimiter(""); // character by character mode
        //Optional<String> token = parseToken(scanner);
        // "+": contains on-going status information about the progress of a slow operation.
        // "*": contains asynchronous state change on the target (stopped, started, disappeared)
        // "=": contains supplementary information that the client should handle (e.g., a new breakpoint information)
        // "^": sync command result (already handled)
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
