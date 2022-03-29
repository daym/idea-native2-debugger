package com.friendly_machines.intellij.plugins.native2Debugger.impl;

import com.intellij.openapi.project.Project;
import com.pty4j.unix.PTYOutputStream;
import com.pty4j.unix.Pty;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
    private GdbMiStateResponse readResponse() throws IOException {
        GdbMiStateResponse response = myReaderThread.readResponse();
        return response;
    }

    private int counter = 0;

    public GdbMiStateResponse gdbSend(String operation, String[] options, String[] parameters) {
        try {
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
        if (!"done".equals(response.getKlass())) {
            throw new GdbMiOperationException();
        }
        return response.getAttributes();
    }
}