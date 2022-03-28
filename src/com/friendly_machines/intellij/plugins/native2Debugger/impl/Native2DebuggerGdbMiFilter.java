package com.friendly_machines.intellij.plugins.native2Debugger.impl;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.pty4j.unix.PTYInputStream;
import com.pty4j.unix.PTYOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Native2DebuggerGdbMiFilter {
    private final Native2DebugProcess myProcess;
    private final Project myProject;
    private final PTYOutputStream myChildIn;
    //private final InputStream myChildOut;
    private final Native2DebuggerGdbMiProducer myReaderThread;

    public Native2DebuggerGdbMiFilter(Native2DebugProcess process, @NotNull Project project, PTYInputStream childOut, PTYOutputStream childIn) {
        myProcess =  process;
        myProject = project;
        //myChildOut = childOut;
        // TODO: InputStreamReader ?
        //Native2DebugProcess process = Native2DebugProcess.getInstance(myOsProcessHandler);
        myReaderThread = new Native2DebuggerGdbMiProducer(childOut, process);
        myChildIn = childIn;

        myReaderThread.start();
        // TODO: PipedReader, PipedWriter

        // https://www.kfu.com/~nsayer/Java/jni-filedesc.html
        // new FileReader(FileDescriptor)
    }

    //    // Called for each LINE.
//    @Override
//    public @Nullable Result applyFilter(@NotNull String line, int entireLength) {
//        if (line.endsWith("\n"))
//            line = line.substring(0, line.length() - 1);
//        if (line.endsWith("\r"))
//            line = line.substring(0, line.length() - 1);
//        if (line.strip() == "(gdb)") {
//            // Ignore
//        } else if (line.length() > 0) {
//            try {
//                Native2DebugProcess process = myProcess
//                parseLine(line, process);
//            } catch (NoSuchElementException e) {
//                e.printStackTrace();
//                // ignore error
//            }
//        }
//        return null;
//    }

    /// Read the sync response from gdb. If there are async responses, those are handled as a side-effect.
    private Native2DebuggerGdbMiStateResponse readResponse() throws IOException {
        Native2DebuggerGdbMiStateResponse response = myReaderThread.readResponse();
        return response;
    }

    private int counter = 0;

    public Native2DebuggerGdbMiStateResponse gdbSend(String operation, String[] options, String[] parameters) {
        try {
            System.err.println("SENDING " + operation);
            ++counter;
            myChildIn.write(Integer.toString(counter).getBytes(StandardCharsets.UTF_8));
            myChildIn.write(operation.getBytes(StandardCharsets.UTF_8));
            for (String option : options) {
                myChildIn.write(" ".getBytes(StandardCharsets.UTF_8));
                myChildIn.write(option.getBytes(StandardCharsets.UTF_8));  // TODO: c string quote
            }
            if (parameters.length > 0) {
                myChildIn.write(" --".getBytes(StandardCharsets.UTF_8));
                for (String parameter : parameters) {
                    myChildIn.write(" ".getBytes(StandardCharsets.UTF_8));
                    myChildIn.write(parameter.getBytes(StandardCharsets.UTF_8));  // TODO: c string quote
                }
            }
            myChildIn.write("\r\n".getBytes(StandardCharsets.UTF_8));
            myChildIn.flush();
            System.err.println("BEFORE RESPONSE READING");
            System.err.flush();
            Native2DebuggerGdbMiStateResponse response = readResponse();
            System.err.println("AFTER RESPONSE READING " + response);
            System.err.flush();
            return response;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public HashMap<String, Object> gdbCall(String operation, String[] options, String[] parameters) throws Native2DebuggerGdbMiOperationException {
        Native2DebuggerGdbMiStateResponse response = gdbSend(operation, options, parameters);
        assert response.getMode() == '^';
        if (!"done".equals(response.getKlass())) {
            throw new Native2DebuggerGdbMiOperationException();
        }
        return response.getAttributes();
    }
}
