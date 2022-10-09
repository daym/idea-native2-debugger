// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.

package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;

import javax.swing.*;

public class ProjectSettingsComponent {
    private final JPanel myMainPanel;
    private final TextFieldWithBrowseButton myGdbExecutable;
    private final TextFieldWithBrowseButton myGdbSysRoot;
    private final JComboBox myGdbTargetType;
    private final JBTextField myGdbTargetArg;
    private final JComboBox myGdbArch;
    private final TextFieldWithBrowseButton mySymbolFile;

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
        myGdbArch = new ComboBox(new String[]{"auto", "alpha", "armbe", "armle", "ia64", "mips32be", "mips32le", "mips64be", "mips64le", "ppc32", "ppc64", "V8", "sparc-v9", "x86-64", "x86"});
        // TODO: Maybe don't hard-code
        myGdbTargetType = new ComboBox(new String[]{"native", "core", "exec", "extended-remote", "record-btrace", "record-core", "record-full", "remote", "tfile"});
        myGdbTargetArg = new JBTextField();
        mySymbolFile = new TextFieldWithBrowseButton();
        mySymbolFile.addBrowseFolderListener("GDB executable", "The symbol table", null, new FileChooserDescriptor(true, false, false, false, false, false));
        myMainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("GDB executable: "), myGdbExecutable, 1, false)
                .addLabeledComponent(new JBLabel("Sysroot: "), myGdbSysRoot, 1, false)
                .addLabeledComponent(new JBLabel("Architecture: "), myGdbArch, 1, false)
                .addLabeledComponent(new JBLabel("Target type: "), myGdbTargetType, 1, false)
                .addLabeledComponent(new JBLabel("Target arg: "), myGdbTargetArg, 1, false)
                .addLabeledComponent(new JBLabel("Debug symbol file: "), mySymbolFile, 1, false)
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

    public void setGdbTargetTypeText(String value) {
        myGdbTargetType.setSelectedItem(value);
    }

    public String getGdbTargetTypeText() {
        Object result = myGdbTargetType.getSelectedItem();
        if (result == null)
            return "";
        else
            return (String) result;
    }

    public void setGdbTargetArgText(String value) {
        myGdbTargetArg.setText(value);
    }

    public String getGdbTargetArgText() {
        return myGdbTargetArg.getText();
    }

    public void setSymbolFile(String value) {
        mySymbolFile.setText(value);
    }

    public String getSymbolFile() {
        return mySymbolFile.getText();
    }

    public String getSymbolFileText() {
        return mySymbolFile.getText();
    }

    public void setSymbolFileText(String symbolFile) {
        mySymbolFile.setText(symbolFile);
    }
}
