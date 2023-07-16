package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.breakpoints.ui.XBreakpointCustomPropertiesPanel;
import com.intellij.xdebugger.impl.breakpoints.XBreakpointBase;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class BreakpointPropertiesPanel extends XBreakpointCustomPropertiesPanel<XLineBreakpoint<BreakpointProperties>> {
    private JCheckBox myHardwareCheckBox;
    private JCheckBox myForceCheckBox;

    @Override
    public @NotNull JComponent getComponent() {
        // TODO: Use Bundle for translations.
        myHardwareCheckBox = new JCheckBox("Hardware breakpoint");
        myForceCheckBox = new JCheckBox("Force breakpoint even if location is currently unknown");
        var mainPanel = new JPanel(new BorderLayout());
        var box = Box.createVerticalBox();
        mainPanel.add(box, BorderLayout.CENTER);
        box.add(myHardwareCheckBox);
        box.add(myForceCheckBox);
        return mainPanel;
    }

    @Override
    public void loadFrom(@NotNull XLineBreakpoint<BreakpointProperties> breakpoint) {
        var properties = breakpoint.getProperties();
        myHardwareCheckBox.setSelected(properties.myHardware);
        myForceCheckBox.setSelected(properties.myForce);
    }

    @Override
    public void saveTo(@NotNull XLineBreakpoint<BreakpointProperties> breakpoint) {
        var properties = breakpoint.getProperties();
        var changed = properties.myHardware != myHardwareCheckBox.isSelected() ||
        properties.myForce != myForceCheckBox.isSelected();
        properties.myHardware = myHardwareCheckBox.isSelected();
        properties.myForce = myForceCheckBox.isSelected();
        if (changed) {
            ((XBreakpointBase) breakpoint).fireBreakpointChanged();
        }
    }
}
