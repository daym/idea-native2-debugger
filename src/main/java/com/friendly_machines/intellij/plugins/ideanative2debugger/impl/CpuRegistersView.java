package com.friendly_machines.intellij.plugins.ideanative2debugger.impl;

import com.intellij.util.ui.components.BorderLayoutPanel;
import com.intellij.xdebugger.XDebugSession;

import javax.swing.*;
import java.io.IOException;
import java.util.Map;

public class CpuRegistersView extends BorderLayoutPanel {
    private JButton btnRefresh;
    private JTextArea txtRegisters;
    private JPanel panel1;

    public CpuRegistersView(XDebugSession session, DebugProcess process) {
        this.add(panel1);
        btnRefresh.addActionListener(e -> {
            txtRegisters.setText("");
            try {
                var registerNames = process.dataListRegisterNames();
                var registerValues = process.dataListRegisterValues("x");
                for (Map<String, ?> entry: registerValues) {
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
                process.reportError("Failed getting registers", e2);
            } catch (RuntimeException | IOException e3) {
                e3.printStackTrace();
                process.reportError(e3.toString());
            } catch (InterruptedException e4) {
                // just stop
                return;
            }
            txtRegisters.revalidate();

        });
    }
    public JComponent getDefaultFocusedComponent() {
        return btnRefresh;
    }
    public void setActive(boolean value) {

    }
}
