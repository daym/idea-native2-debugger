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

    @Override
    public @NotNull JComponent getComponent() {
        // TODO: Use Bundle for translations.
        myHardwareCheckBox = new JCheckBox("Hardware breakpoint");
        var mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(myHardwareCheckBox);
        return mainPanel;
    }

    @Override
    public void loadFrom(@NotNull XLineBreakpoint<BreakpointProperties> breakpoint) {
        myHardwareCheckBox.setSelected(breakpoint.getProperties().myHardware);
    }

    @Override
    public void saveTo(@NotNull XLineBreakpoint<BreakpointProperties> breakpoint) {
        var changed = breakpoint.getProperties().myHardware != myHardwareCheckBox.isSelected();
        breakpoint.getProperties().myHardware = myHardwareCheckBox.isSelected();
        if (changed) {
            ((XBreakpointBase) breakpoint).fireBreakpointChanged();
        }
    }
}
