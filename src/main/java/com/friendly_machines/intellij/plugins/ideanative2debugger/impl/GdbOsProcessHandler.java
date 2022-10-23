package com.friendly_machines.intellij.plugins.ideanative2debugger.impl;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Key;
import com.intellij.util.io.BaseDataReader;
import com.intellij.util.io.BaseOutputReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class GdbOsProcessHandler extends OSProcessHandler {
    private final GdbMiProducer myProducer;

    public GdbOsProcessHandler(@NotNull GeneralCommandLine commandLine) throws ExecutionException {
        super(commandLine);
        myProducer = new GdbMiProducer(); // TODO: clean up if not needed anymore
    }

    @Override
    protected void doDestroyProcess() {
        super.doDestroyProcess();
    }

    //@Override
    //protected boolean processHasSeparateErrorStream() {
//        return true;
//    } // automatic

    @Override
    protected BaseOutputReader.@NotNull Options readerOptions() {
        return
        new BaseOutputReader.Options() {
            @Override
            public BaseDataReader.SleepingPolicy policy() {
                return BaseDataReader.SleepingPolicy.BLOCKING;
            }

            @Override
            public boolean splitToLines() {
                return true;
            }

            @Override
            public boolean sendIncompleteLines() {
                return false;
            }

            @Override
            public boolean withSeparators() {
                return true;
            }
        };
    }

    public GdbMiStateResponse readResponse() throws InterruptedException {
        var result = myProducer.consume();
        if (result == null) { // timeout
            this.destroyProcess();
            throw new RuntimeException("timeout while waiting for response from GDB/MI");
        } else {
            return result;
        }
    }

    @Nullable
    public Charset getCharset() {
        return StandardCharsets.UTF_8;
    }

    // This is here so we don't have an ordering problem with the startNotified events
    public void startNotify() {
        super.startNotify();
        var debugProcess = (DebugProcess) GdbOsProcessHandler.this.getUserData(DebugProcess.DEBUG_PROCESS_KEY);
        if (debugProcess != null) {
            try {
                debugProcess.startDebugging();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Debug process is missing");
        }
    }
    @Override
    public void notifyTextAvailable(@NotNull String text, @NotNull Key outputType) {
        // Note: Runs in "output stream of gdb" thread.
//        println(Thread.currentThread().getId() + Thread.currentThread().getName() + " notifyTextAvailable: " + text);
        var scanner = new Scanner(text);
        scanner.useDelimiter(""); // character by character mode
        var token = GdbMiProducer.parseToken(scanner);
        if (scanner.hasNext("\\^")) { // sync response
            if (token.isPresent()) {
                try {
                    var item = GdbMiStateResponse.decode(token, scanner);
                    myProducer.produce(item);
                } catch (RuntimeException e) { // InputMismatchException
                    e.printStackTrace();
                    // Put an error response into the queue--otherwise the caller would wait for an answer indefinitely.
                    var item2 = GdbMiStateResponse.errorResponse(token, '^', "error", e.toString());
                    try {
                        myProducer.produce(item2);
                    } catch (InterruptedException ex) {
                        //ex.printStackTrace();
                        throw new RuntimeException(ex);
                    }
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                    throw new RuntimeException(e); // FIXME
                }
            } else {
                // a sync response we didn't wait for
                System.err.println(Thread.currentThread().getId() + Thread.currentThread().getName() +": ignored unknown sync response");
            }

            // For async response handling, see GdbMiFilter
        } else {
            // Move to UI thread.
            ApplicationManager.getApplication().invokeLater(() -> {
                var debugProcess = (DebugProcess) GdbOsProcessHandler.this.getUserData(DebugProcess.DEBUG_PROCESS_KEY);
                if (debugProcess != null) {
                    try {
                        debugProcess.processAsync(token, scanner);
                    } catch (IOException e) {
                        // pucgenie: So who catches this?
                        throw new UncheckedIOException(e);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                } else { // too late
                }
            });
        }
//        println(Thread.currentThread().getId() + Thread.currentThread().getName() +"done notify");

        super.notifyTextAvailable(text, outputType);
    }
}
