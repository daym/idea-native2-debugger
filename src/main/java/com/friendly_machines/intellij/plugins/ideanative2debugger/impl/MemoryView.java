package com.friendly_machines.intellij.plugins.ideanative2debugger.impl;

import com.intellij.util.ui.components.BorderLayoutPanel;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.memory.component.InstancesTracker;

import javax.swing.*;

public class MemoryView extends BorderLayoutPanel {
    private final DebugProcess myProcess;
    private JTextField textField1;
    private JPanel panel1;


    public JComponent getDefaultFocusedComponent() {
        return null;
    }

    public void setActive(boolean value) {

    }
    public MemoryView(XDebugSession session, DebugProcess process, InstancesTracker tracker) {
        myProcess = process;
        this.add(panel1);
        /*
        TODO: [address|symbol] -Read->  [    ]
                               <-Write-
         */
    }

    /*
    TODO: myProcess.
        Object dataReadMemoryBytes(int byteOffset, String addressExpr, int countBytes) throws GdbMiOperationException {
        Object dataWriteMemoryBytes(String addressExpr, byte[] contents) throws GdbMiOperationException
    */
}
