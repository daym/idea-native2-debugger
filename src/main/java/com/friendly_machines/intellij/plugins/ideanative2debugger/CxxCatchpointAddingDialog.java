package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.friendly_machines.intellij.plugins.ideanative2debugger.impl.CxxCatchpointCatchType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.ui.Messages;

import javax.swing.*;
import java.util.Optional;

public class CxxCatchpointAddingDialog extends DialogWrapper {
    private JPanel myPanel;
    private JComboBox myBreakOnComboBox;
    private JTextField myExceptionRegexp;

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return myPanel;
    }

    @Override
    protected void doOKAction() {
        if (getCatchpointType().isEmpty()) {
            Messages.showErrorDialog(myPanel, DebuggerBundle.message("catchpoint.type.not.specified"));
            return;
        }
        // TODO: validate regexp
        if (getExceptionRegexp().length() == 0) {
            Messages.showErrorDialog(myPanel, DebuggerBundle.message("exception.pattern.not.specified"));
            return;
        }
        super.doOKAction();
    }

    public String getExceptionRegexp() {
        return myExceptionRegexp.getText();
    }

    public CxxCatchpointAddingDialog(Project project) {
        super(project, true);
        setTitle(DebuggerBundle.message("exception.adding.title"));
        init();
    }

    public Optional<CxxCatchpointCatchType> getCatchpointType() {
        switch (myBreakOnComboBox.getSelectedIndex()) {
            case 0:
                return Optional.of(CxxCatchpointCatchType.Throw);
            case 1:
                return Optional.of(CxxCatchpointCatchType.Rethrow);
            case 2:
                return Optional.of(CxxCatchpointCatchType.Catch);
            default:
                return Optional.empty();
        }
    }
}
