package com.friendly_machines.intellij.plugins.native2Debugger;

import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;

import javax.swing.*;

public class ProjectSettingsComponent {
    private final JPanel myMainPanel;
    private final TextFieldWithBrowseButton myGdbExecutable; // TODO: file selector!

    //  private final JBTextField myUserNameText = new JBTextField();
    //  private final JBCheckBox myIdeaUserStatus = new JBCheckBox("Do you use IntelliJ IDEA? ");
    public JComponent getPreferredFocusedComponent() {
        return myGdbExecutable;
    }

    public JComponent getPanel() {
        return myMainPanel;
    }

    public ProjectSettingsComponent() {
        myGdbExecutable = new TextFieldWithBrowseButton();
        myMainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("GDB executable: "), myGdbExecutable, 1, false)
                //.addComponent(myIdeaUserStatus, 1)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        // TODO: Architecture
        // TODO: Remote Debugger: Target, Sysroot
    }

    public void setGdbExecutableNameText(String value) {
        myGdbExecutable.setText(value);
    }

    public String getGdbExecutableNameText() {
        return myGdbExecutable.getText();
    }
}
