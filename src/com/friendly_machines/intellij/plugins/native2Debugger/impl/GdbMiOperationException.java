package com.friendly_machines.intellij.plugins.native2Debugger.impl;

public class GdbMiOperationException extends Exception {
    private final GdbMiStateResponse myDetails;

    public GdbMiOperationException(GdbMiStateResponse response) {
        myDetails = response;
    }

    public GdbMiStateResponse getDetails() {
        return myDetails;
    }
}
