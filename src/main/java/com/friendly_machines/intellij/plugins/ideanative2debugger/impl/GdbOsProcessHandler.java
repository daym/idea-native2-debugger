package com.friendly_machines.intellij.plugins.ideanative2debugger.impl;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.util.Key;
import com.intellij.util.io.BaseDataReader;
import com.intellij.util.io.BaseOutputReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
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
        return myProducer.consume();
    }

    @Nullable
    public Charset getCharset() {
        return StandardCharsets.UTF_8;
    }

    // This is here so we don't have an ordering problem with the startNotified events
    public void startNotify() {
        if (myCommandLine != null) {
            notifyTextAvailable(myCommandLine + '\n', ProcessOutputTypes.SYSTEM);
        }

        addProcessListener(new ProcessAdapter() {
            @Override
            public void startNotified(@NotNull final ProcessEvent event) {
                var debugProcess = (DebugProcess) GdbOsProcessHandler.this.getUserData(DebugProcess.DEBUG_PROCESS_KEY);
                try {
                    BaseDataReader stdOutReader = createOutputDataReader();
                    BaseDataReader stdErrReader = processHasSeparateErrorStream() ? createErrorDataReader() : null;
                    debugProcess.startDebugging();

                    myWaitFor.setTerminationCallback(exitCode -> {
                        try {
                            // tell readers that no more attempts to read process' output should be made
                            if (stdErrReader != null) stdErrReader.stop();
                            stdOutReader.stop();

                            try {
                                if (stdErrReader != null) stdErrReader.waitFor();
                                stdOutReader.waitFor();
                            }
                            catch (InterruptedException ignore) { }
                        }
                        finally {
                            onOSProcessTerminated(exitCode);
                        }
                    });
                }
                finally {
                    removeProcessListener(this);
                }
            }
        });

        super.startNotify();
    }

    @Override
    public void notifyTextAvailable(@NotNull String text, @NotNull Key outputType) {
        System.err.println(Thread.currentThread().getId() + Thread.currentThread().getName() + " notifyTextAvailable: " + text);
        System.err.flush();
        var scanner = new Scanner(text);
        scanner.useDelimiter(""); // character by character mode
        Optional<String> token = GdbMiProducer.parseToken(scanner);
        if (scanner.hasNext("\\^")) { // sync response
            if (token.isPresent()) {
                var item = GdbMiStateResponse.decode(token, scanner);
                try {
                    System.err.println(Thread.currentThread().getId() + Thread.currentThread().getName() +"before putting item in queue");
                    System.err.flush();
                    myProducer.produce(item);
                    System.err.println(Thread.currentThread().getId() + Thread.currentThread().getName() +"after putting item in queue");
                    System.err.flush();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e); // FIXME
                }
            } else {
                // a sync response we didn't wait for
                System.err.println(Thread.currentThread().getId() + Thread.currentThread().getName() +"ignored unknown sync response");
                System.err.flush();
            }

            // For async response handling, see GdbMiFilter
        } else {
            var debugProcess = (DebugProcess) GdbOsProcessHandler.this.getUserData(DebugProcess.DEBUG_PROCESS_KEY);
            debugProcess.processAsync(token, scanner);
        }
        System.err.println(Thread.currentThread().getId() + Thread.currentThread().getName() +"done notify");
        System.err.flush();

        super.notifyTextAvailable(text, outputType);
    }
}
