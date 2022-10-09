// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.
package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

/**
 * (c) 2020 Silent Forest AB
 * created: 06 August 2020
 */
public class DebuggerBundle extends DynamicBundle {
    @NonNls
    private static final String BUNDLE = "messages.Native2DebuggerBundle";
    private static final DebuggerBundle INSTANCE = new DebuggerBundle();

    private DebuggerBundle() {
        super(BUNDLE);
    }

    @NotNull
    public static @Nls String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object /*@NotNull*/... params) {
        return INSTANCE.getMessage(key, params);
    }
}
