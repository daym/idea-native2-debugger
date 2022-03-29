package com.friendly_machines.intellij.plugins.native2Debugger;

import com.intellij.util.PlatformIcons;
import com.intellij.xdebugger.frame.XValue;
import com.intellij.xdebugger.frame.XValueNode;
import com.intellij.xdebugger.frame.XValuePlace;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class Value extends XValue {
    private String myName;
    private String myValue;
    private boolean myArg;
    @Override
    public void computePresentation(@NotNull XValueNode node, @NotNull XValuePlace xValuePlace) {
        Icon icon = myArg ? PlatformIcons.PARAMETER_ICON : PlatformIcons.VARIABLE_ICON; // TODO: or FIELD or PROPERTY
        node.setPresentation(icon, myName, myValue, false); // last one is HasChildren. TODO
    }
    public Value(String name, String value, boolean arg) {
        myName = name;
        myValue = value;
        myArg = arg;
    }
}
