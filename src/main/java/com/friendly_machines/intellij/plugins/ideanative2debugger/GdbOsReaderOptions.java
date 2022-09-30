package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.intellij.util.io.BaseDataReader;
import com.intellij.util.io.BaseOutputReader;
import org.jetbrains.annotations.NotNull;

public class GdbOsReaderOptions extends BaseOutputReader.@NotNull Options {
    @Override
    public BaseDataReader.SleepingPolicy policy() {
        return BaseDataReader.SleepingPolicy.BLOCKING;
    }

    @Override
    public boolean splitToLines() {
        return true;
    }

    @Override
    public boolean sendIncompleteLines() {
        return false;
    }

    @Override
    public boolean withSeparators() {
        return true;
    }
}
