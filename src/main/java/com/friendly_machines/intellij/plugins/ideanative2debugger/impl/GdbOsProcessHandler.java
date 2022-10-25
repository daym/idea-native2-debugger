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
        if (debugProcess == null) {
            throw new RuntimeException("Debug process is missing");
        }
        try {
            debugProcess.startDebugging();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            // just stop
        }
    }

    /**
     * Don't throw any exception to the caller - it would break the whole communication channel to the process/GDB.
     * @param text
     * @param outputType
     */
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
                        Thread.currentThread().interrupt();
                        return;
                    }
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                    Thread.currentThread().interrupt();
                    return;
                }
            } else {
                // a sync response we didn't wait for
                ApplicationManager.getApplication().invokeLater(() -> {
                    var debugProcess = (DebugProcess) GdbOsProcessHandler.this.getUserData(DebugProcess.DEBUG_PROCESS_KEY);
                    var errMsg = "ignored unknown sync response: " + text;
                    if (debugProcess == null) {
                        // Pech gehabt.
                        System.err.println(errMsg);
                        return;
                    }
                    debugProcess.reportError(errMsg);
                });
            }

            // For async response handling, see GdbMiFilter
        } else {
            // Move to UI thread.
            ApplicationManager.getApplication().invokeLater(() -> {
                var debugProcess = (DebugProcess) GdbOsProcessHandler.this.getUserData(DebugProcess.DEBUG_PROCESS_KEY);
                if (debugProcess == null) {
                    // too late
                    return;
                }
                try {
                    debugProcess.processAsync(token, scanner);
                } catch (IOException e) {
                    e.printStackTrace();
                    debugProcess.getSession().reportError(e.toString());
                } catch (InterruptedException e) {
                    //throw new RuntimeException(e);
                    // just stop
                }
            });
        }
//        println(Thread.currentThread().getId() + Thread.currentThread().getName() +"done notify");

        super.notifyTextAvailable(text, outputType);
    }
}
