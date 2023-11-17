package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.intellij.openapi.project.Project;
import com.intellij.ui.PanelWithAnchor;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.Arrays;
import java.util.Comparator;

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
        myArgs = new DefaultTableModel(new String[]{"Argument"}, 0);
        table1.setModel(myArgs);
        addBeforeButton.addActionListener(actionEvent -> {
            int cursorRowIndex = table1.getSelectionModel().getLeadSelectionIndex(); // TODO Anchor instead ?
            if (cursorRowIndex != -1) {
                var modelIndex = table1.convertRowIndexToModel(cursorRowIndex);
                myArgs.insertRow(modelIndex, new String[]{""});
            } else {
                myArgs.addRow(new String[]{""});
            }
        });
        addAfterwardsButton.addActionListener(actionEvent -> {
            int cursorRowIndex = table1.getSelectionModel().getLeadSelectionIndex(); // TODO Anchor instead ?
            if (cursorRowIndex != -1 && cursorRowIndex < table1.getRowCount() - 1) {
                var modelIndex = table1.convertRowIndexToModel(cursorRowIndex + 1);
                myArgs.insertRow(modelIndex, new String[]{""});
            } else {
                myArgs.addRow(new String[]{""});
            }
        });
        button2r.addActionListener(actionEvent -> {
            Arrays.stream(table1.getSelectedRows()).map(table1::convertRowIndexToModel).boxed().sorted(Comparator.reverseOrder()).forEach(myArgs::removeRow);
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
        myArgs.setDataVector(Arrays.stream(execArguments).map(v -> new String[]{v}).toArray(size -> new String[size][1]), new Object[]{0});
    }
}
