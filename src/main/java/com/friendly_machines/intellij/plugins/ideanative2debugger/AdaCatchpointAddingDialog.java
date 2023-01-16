package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.friendly_machines.intellij.plugins.ideanative2debugger.DebuggerBundle;
import com.friendly_machines.intellij.plugins.ideanative2debugger.impl.AdaCatchpointCatchType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Optional;

public class AdaCatchpointAddingDialog extends DialogWrapper {
    private JPanel myPanel;
    private JComboBox myBreakOnComboBox;
    private JTextField myExceptionField;

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
        /*if (getException().length() == 0) OK for 'unknown exceptions' */
        super.doOKAction();
    }

    public String getException() {
        return myExceptionField.getText();
    }

    public AdaCatchpointAddingDialog(Project project) {
        super(project, true);
        setTitle(DebuggerBundle.message("exception.adding.title"));
        init();
    }

    public Optional<AdaCatchpointCatchType> getCatchpointType() {
        switch (myBreakOnComboBox.getSelectedIndex()) {
            case 0:
                return Optional.of(AdaCatchpointCatchType.Exception);
            case 1:
                return Optional.of(AdaCatchpointCatchType.Handlers);
            case 2:
                return Optional.of(AdaCatchpointCatchType.Assertion);
            default:
                return Optional.empty();
        }
    }
}
