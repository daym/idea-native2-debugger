package com.friendly_machines.intellij.plugins.native2Debugger;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;

import javax.swing.*;

public class ProjectSettingsComponent {
    private final JPanel myMainPanel;
    private final TextFieldWithBrowseButton myGdbExecutable;
    private final TextFieldWithBrowseButton myGdbSysRoot;
    private final JBTextField myGdbTarget; // TODO: split this up more (kind, parameters etc)
    private final JComboBox myGdbArch;

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
        myGdbExecutable.addBrowseFolderListener("GDB executable", "The GDB executable", null, new FileChooserDescriptor(true, false, false, false, false, false));

        myGdbSysRoot = new TextFieldWithBrowseButton();

        myGdbSysRoot.addBrowseFolderListener("GDB sysroot", "The source directory (corresponding to the target) on the host", null,
                new FileChooserDescriptor(false, true, false, false, false, false));

        // TODO: Maybe don't hard-code
        myGdbArch = new JComboBox(new String[] { "auto", "alpha", "armbe", "armle", "ia64", "mips32be", "mips32le", "mips64be", "mips64le", "ppc32", "ppc64", "V8", "sparc-v9", "x86-64", "x86" });
        myGdbTarget = new JBTextField();
        myMainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("GDB executable: "), myGdbExecutable, 1, false)
                .addLabeledComponent(new JBLabel("Sysroot: "), myGdbSysRoot, 1, false)
                .addLabeledComponent(new JBLabel("Architecture: "), myGdbArch, 1, false)
                .addLabeledComponent(new JBLabel("Target: "), myGdbTarget, 1, false)
                //.addComponent(myIdeaUserStatus, 1)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        // TODO: Remote Debugger: Default Target
    }

    public void setGdbExecutableNameText(String value) {
        myGdbExecutable.setText(value);
    }

    public String getGdbExecutableNameText() {
        return myGdbExecutable.getText();
    }

    public String getGdbSysRootText() {
        return myGdbSysRoot.getText();
    }
    public void setGdbSysRootText(String value) {
        myGdbSysRoot.setText(value);
    }

    public String getGdbArchText() {
        Object result = myGdbArch.getSelectedItem();
        if (result == null)
            return "";
        else
            return (String) result;
    }
    public void setGdbArchText(String value) {
        myGdbArch.setSelectedItem(value);
    }

    public void setGdbTargetText(String value) {
        myGdbTarget.setText(value);
    }

    public String getGdbTargetText() {
        return myGdbTarget.getText();
    }
}
