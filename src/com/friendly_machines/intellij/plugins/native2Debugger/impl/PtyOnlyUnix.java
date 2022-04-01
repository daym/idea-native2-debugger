// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.

package com.friendly_machines.intellij.plugins.native2Debugger.impl;

import com.pty4j.unix.Pty;
import jtermios.JTermios;

import java.io.IOException;
import java.io.OutputStream;

public class PtyOnlyUnix implements PtyOnly {
    private Pty pty;

    @Override
    public String readLine() throws InterruptedException {
        // TODO: InputStreamReader in order to decode from UTF-8 ?
        StringBuilder buffer = new StringBuilder();
        int fd = getMasterFD();
        byte[] buf = new byte[1]; // TODO: Buffer more
        int count;
        // TODO: timeout
        while ((count = JTermios.read(fd, buf, buf.length)) > 0) {
            int c = buf[0];
            buffer.append(Character.toString(c));
            if (c == 10) {
                break;
            }
        }
        if (count == -1) {
            throw new InterruptedException();
        }
        return buffer.toString();
    }

    @Override
    public OutputStream getOutputStream() {
        return pty.getOutputStream();
    }

    @Override
    public void close() throws IOException {
        pty.close();
    }

    @Override
    public String getSlaveName() {
        return pty.getSlaveName();
    }

    private int getMasterFD() {
        return pty.getMasterFD();
    }

    public PtyOnlyUnix() throws IOException {
        pty = new Pty(true, true);
    }
}
