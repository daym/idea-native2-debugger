// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.

package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.intellij.util.PlatformIcons;
import com.intellij.xdebugger.frame.XValue;
import com.intellij.xdebugger.frame.XValueNode;
import com.intellij.xdebugger.frame.XValuePlace;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class Value extends XValue {
    private final String myName;
    private final String myValue;
    private final boolean myArg;

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
