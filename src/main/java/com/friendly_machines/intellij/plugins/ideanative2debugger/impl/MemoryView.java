package com.friendly_machines.intellij.plugins.ideanative2debugger.impl;

import com.intellij.util.ui.components.BorderLayoutPanel;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.memory.component.InstancesTracker;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MemoryView extends BorderLayoutPanel {
    private static Pattern partRegex = Pattern.compile(".{1,8}", Pattern.DOTALL);
    private final DebugProcess myProcess;
    private JTextField beginningAddressText;
    private JPanel panel1;
    private JButton viewButton;
    private JTextArea memoryText;
    private JPanel panel2;


    public JComponent getDefaultFocusedComponent() {
        return beginningAddressText;
    }

    public void setActive(boolean value) {

    }
    public void setAddressRange(String nodeName) {
        beginningAddressText.setText(nodeName); // loop?
        try {
            // TODO: incrementally increase count.
            var response = (Map) myProcess.dataReadMemoryBytes(0, nodeName, 0x100);
            memoryText.setText("");
            var memory = (List) response.get("memory");
            for (var itemx: memory) {
                var item = (Map) itemx;
                var offset = item.get("offset");
                var contents = (String) item.get("contents");
                var begin = item.get("begin");
                // TODO: memoryText.setText(response.toString());
                memoryText.append("\n");
                memoryText.append("at " + begin.toString() + " + " + offset.toString() + ":\n  ");

                int chunkIndex = 0;
                var m = partRegex.matcher(contents);
                while (m.find()) {
                    var part = contents.substring(m.start(), m.end());
                    memoryText.append(part);
                    memoryText.append(" ");
                    ++chunkIndex;
                    if ((chunkIndex & 3) == 0) {
                        memoryText.append("\n  ");
                    }
                }
                memoryText.append(contents);
            }
        } catch (GdbMiOperationException e) {
            // TODO error throw new RuntimeException(e);
            // TODO process.reportError("failed reading memory", e);
        } catch (IOException e) {
            // TODO error throw new RuntimeException(e);
            // TODO process.reportError(e.toString());
        } catch (InterruptedException e) {
            // probably on purpose
        }
    }

    public MemoryView(XDebugSession session, DebugProcess process, InstancesTracker tracker) {
        myProcess = process;
        this.add(panel2);
        //panel2.setDefaultButton(viewButton);
        /*
        TODO: [address|symbol] -Read->  [    ]
                               <-Write-
         */
        viewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                var beginningA = beginningAddressText.getText();
                setAddressRange(beginningA);
            }
        });
    }
}
