// Copyright 2022 Danny Milosavljevic. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.

package com.friendly_machines.intellij.plugins.ideanative2debugger.impl;

public class GdbMiOperationException extends Exception {
    private final GdbMiStateResponse myDetails;

    public GdbMiOperationException(GdbMiStateResponse response) {
        myDetails = response;
    }

    public GdbMiStateResponse getDetails() {
        return myDetails;
    }
}
