package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class WatchpointAddingDialog extends DialogWrapper {
    private JPanel myPanel;
    private JTextField myExpression;
    private JCheckBox myReadsCheckBox;
    private JCheckBox myWritesCheckBox;
    @Override
    protected @Nullable JComponent createCenterPanel() {
        return myPanel;
    }

    @Override
    protected void doOKAction() {
        if (getExpression().length() == 0) {
            Messages.showErrorDialog(myPanel, DebuggerBundle.message("expression.not.specified"));
            return;
        }
        super.doOKAction();
    }

    public String getExpression() {
        return myExpression.getText();
    }
    public boolean isMonitoringReads() {
        return myReadsCheckBox.isSelected();
    }
    public boolean isMonitoringWrites() {
        return myWritesCheckBox.isSelected();
    }

    public WatchpointAddingDialog(Project project) {
        super(project, true);
        setTitle(DebuggerBundle.message("watchpoint.adding.title"));
        init();
    }
}
