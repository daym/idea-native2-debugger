package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter;
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.rust.cargo.runconfig.ConfigurationExtensionContext;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
public class DebuggerRunConfigurationExtension extends org.rust.cargo.runconfig.CargoCommandConfigurationExtension {
    @Override
    public void attachToProcess(
            CargoCommandConfiguration configuration,
            ProcessHandler handler,
            ExecutionEnvironment environment,
            ConfigurationExtensionContext context
    ) {
        System.err.println("!!! MAYBE USEFUL!!!!");
        final MessageBusConnection connection = configuration.getProject().getMessageBus().connect();
        connection.subscribe(SMTRunnerEventsListener.TEST_STATUS, new SMTRunnerEventsAdapter() {
            @Override
            public void onTestingFinished(@NotNull SMTestProxy.SMRootTestProxy testsRoot) {
                if (testsRoot.getHandler() != handler) return;
                System.err.println("TESTS FINISHED!!" + testsRoot);
//                    testsRoot.getAllTests();
//                            testsRoot.getChildren();
//                            testsRoot.getMetainfo();
//                                    testsRoot.getName()
//                                            testsRoot.getPresentableName();
                connection.disconnect();
                //Disposer.dispose(disposable);
            }
        });
    }

    @Override
    public boolean isEnabledFor(CargoCommandConfiguration configuration, RunnerSettings runnerSettings) {
        return false;
    }

    @Override
    public boolean isApplicableFor(CargoCommandConfiguration configuration) {
        return false;
    }

    @Override
    public void patchCommandLine(
            CargoCommandConfiguration configuration,
            ExecutionEnvironment environment,
            GeneralCommandLine cmdLine,
            ConfigurationExtensionContext context
    ) {
    }
}
