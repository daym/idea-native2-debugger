package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.intellij.openapi.project.Project;
import com.intellij.ui.PanelWithAnchor;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import static com.ibm.icu.text.PluralRules.Operand.v;

public class ProjectDebugSettingsEditorComponent implements PanelWithAnchor {
    private final DefaultTableModel myArgs;
    private JPanel p;
    private JButton addBeforeButton;
    private JTable table1;
    private JButton addAfterwardsButton;
    private JButton button2r;

    private Project myProject;
    private JComponent myAnchor;

    public ProjectDebugSettingsEditorComponent(Project project) {
        myProject = project;
        myArgs = new DefaultTableModel(new String[] { "Argument" }, 0);
        table1.setModel(myArgs);
        addBeforeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                int cursorRowIndex = table1.getSelectedRow();
                if (cursorRowIndex != -1) {
                    myArgs.insertRow(cursorRowIndex, new String[]{""}); // TODO map
                } else {
                    myArgs.addRow(new String[]{""});
                }
            }
        });
        addAfterwardsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                int cursorRowIndex = table1.getSelectedRow();
                if (cursorRowIndex != -1) {
                    myArgs.insertRow(table1.getSelectedRow() + 1, new String[]{""}); // TODO map
                } else {
                    myArgs.addRow(new String[]{""});
                }
            }
        });
        button2r.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                int selectedRowIndex = table1.getSelectedRow();
                if (selectedRowIndex != -1) {
                    myArgs.removeRow(selectedRowIndex);
                }
            }
        });

    }

    @Override
    public JComponent getAnchor() {
        return myAnchor;
    }

    @Override
    public void setAnchor(@Nullable JComponent anchor) {
        myAnchor = anchor;
        // TODO: p.setAnchor(anchor);
    }

    public JComponent getComponent() {
        return p;
    }

    public String[] getExecArguments() {
        return myArgs.getDataVector().stream().map(v -> v.get(0)).toArray(size -> new String[size]);
    }

    public void setExecArguments(String[] execArguments) {
        myArgs.setDataVector(Arrays.stream(execArguments).map(v -> new String[] { v }).toArray(size -> new String[size][1]), new Object[] { 0 });
    }
}
