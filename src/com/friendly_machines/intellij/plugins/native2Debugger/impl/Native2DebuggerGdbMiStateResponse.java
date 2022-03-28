package com.friendly_machines.intellij.plugins.native2Debugger.impl;

import java.util.HashMap;
import java.util.Scanner;

public class Native2DebuggerGdbMiStateResponse {
    private final char myMode;
    private final String myKlass;
    private final HashMap<String, Object> myAttributes;

    public Native2DebuggerGdbMiStateResponse(char mode, String klass, HashMap<String, Object> attributes) {
        myMode = mode;
        myKlass = klass;
        myAttributes = attributes;
    }
    public static Native2DebuggerGdbMiStateResponse decode(Scanner scanner) {
        // "+": contains on-going status information about the progress of a slow operation.
        // "*": contains asynchronous state change on the target (stopped, started, disappeared)
        // "=": contains supplementary information that the client should handle (e.g., a new breakpoint information)
        // "^": sync command result
        char mode = scanner.next().charAt(0);
        String klass = Native2DebuggerGdbMiFilter.parseKlass(scanner); // Note: not specified
        HashMap<String, Object> result = new HashMap<String, Object>();
        while (scanner.hasNext(",")) {
            scanner.next(",");
            String name = Native2DebuggerGdbMiFilter.parseString(scanner);
            scanner.next("=");
            Object value = Native2DebuggerGdbMiFilter.parseValue(scanner);
            result.put(name, value);
        }
        return new Native2DebuggerGdbMiStateResponse(mode, klass, result);
    }

    public char getMode() {
        return myMode;
    }

    public String getKlass() {
        return myKlass;
    }

    public HashMap<String, Object> getAttributes() {
        return myAttributes;
    }

    @Override
    public String toString() {
        return "Native2DebuggerGdbMiStateResponse{" +
                "myMode=" + myMode +
                ", myKlass='" + myKlass + '\'' +
                ", myAttributes=" + myAttributes +
                '}';
    }
}
