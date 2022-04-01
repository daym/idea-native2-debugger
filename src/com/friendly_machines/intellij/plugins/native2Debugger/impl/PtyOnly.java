// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.

package com.friendly_machines.intellij.plugins.native2Debugger.impl;

import java.io.IOException;
import java.io.OutputStream;

public interface PtyOnly {
    String readLine() throws InterruptedException;

    OutputStream getOutputStream();

    void close() throws IOException;

    String getSlaveName();
}
