package com.friendly_machines.intellij.plugins.ideanative2debugger.impl;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
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

    @Override
    public void notifyTextAvailable(@NotNull String text, @NotNull Key outputType) {
        try {
            throw new RuntimeException("e");
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        System.err.println( Thread.currentThread().getId() + Thread.currentThread().getName() + " text available: " + text);
        System.err.flush();
        if (text.startsWith("^")) { // sync response
            var scanner = new Scanner(text);
            scanner.useDelimiter(""); // character by character mode
            Optional<String> token = GdbMiProducer.parseToken(scanner);
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
        }
        System.err.println(Thread.currentThread().getId() + Thread.currentThread().getName() +"done notify");
        System.err.flush();

        super.notifyTextAvailable(text, outputType);
    }
}
