package com.friendly_machines.intellij.plugins.ideanative2debugger.impl;

public enum GdbMiDisassemblyMode {
    OnlyDisassembly(0),
    MixedSourceAndDisassembly(1),
    DisassemblyWithRawOpcodes(2),
    MixedSourceAndDDisassemblyWithRawOpcodes(3);

    private final int code;

    public int code() {
        return this.code;
    }
    GdbMiDisassemblyMode(int code) {
        this.code = code;
    }
}
