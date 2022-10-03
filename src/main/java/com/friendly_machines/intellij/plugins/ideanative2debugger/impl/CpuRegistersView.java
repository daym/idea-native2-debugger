package com.friendly_machines.intellij.plugins.ideanative2debugger.impl;

import com.intellij.util.ui.components.BorderLayoutPanel;
import com.intellij.xdebugger.XDebugSession;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

public class CpuRegistersView extends BorderLayoutPanel {
    private JButton btnRefresh;
    private JTextArea txtRegisters;
    private JPanel panel1;

    public CpuRegistersView(XDebugSession session, DebugProcess process) {
        this.add(panel1);
        btnRefresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                txtRegisters.setText("");
                try {
                    List<String> registerNames = process.dataListRegisterNames();
                    List<Map<String, Object>> registerValues = process.dataListRegisterValues("x");
                    for (Map<String, Object> entry: registerValues) {
                        String numberString = (String) entry.get("number");
                        Object value = entry.get("value");
                        txtRegisters.append("\n");
                        var number = Integer.parseInt(numberString);
                        var name = registerNames.get(number);
                        txtRegisters.append(name);
                        txtRegisters.append(" = ");
                        txtRegisters.append(value.toString());
                    }
                } catch (GdbMiOperationException e2) {
                    e2.printStackTrace();
                } catch (RuntimeException e3) {
                    e3.printStackTrace();
                }
                txtRegisters.revalidate();

            }
        });
    }
    public JComponent getDefaultFocusedComponent() {
        return btnRefresh;
    }
    public void setActive(boolean value) {

    }
}
