package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.friendly_machines.intellij.plugins.ideanative2debugger.impl.ShlibCatchpointCatchType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Optional;

public class ShlibCatchpointAddingDialog extends DialogWrapper {
    private JPanel myPanel;
    private JComboBox myBreakOnComboBox;
    private JTextField myLibraryNameRegexp;
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
        if (getLibraryNameRegexp().length() == 0) {
            Messages.showErrorDialog(myPanel, DebuggerBundle.message("library.name.pattern.not.specified"));
            return;
        }
        super.doOKAction();
    }

    public String getLibraryNameRegexp() {
        return myLibraryNameRegexp.getText();
    }

    public ShlibCatchpointAddingDialog(Project project) {
        super(project, true);
        setTitle(DebuggerBundle.message("shared.library.manager.breakpoint.adding.title"));
        init();
    }

    public Optional<ShlibCatchpointCatchType> getCatchpointType() {
        switch (myBreakOnComboBox.getSelectedIndex()) {
            case 0:
                return Optional.of(ShlibCatchpointCatchType.Load);
            case 1:
                return Optional.of(ShlibCatchpointCatchType.Unload);
            default:
                return Optional.empty();
        }
    }
}
