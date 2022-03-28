package com.friendly_machines.intellij.plugins.native2Debugger.impl;

import java.util.HashMap;
import java.util.Optional;
import java.util.Scanner;

public class Native2DebuggerGdbMiStateResponse {
    private final char myMode;
    private final Optional<String> myToken;
    private final String myKlass;
    private final HashMap<String, Object> myAttributes;

    public Native2DebuggerGdbMiStateResponse(Optional<String> token, char mode, String klass, HashMap<String, Object> attributes) {
        myToken = token;
        myMode = mode;
        myKlass = klass;
        myAttributes = attributes;
    }
    public static Native2DebuggerGdbMiStateResponse decode(Optional<String> token, Scanner scanner) {
        // "+": contains on-going status information about the progress of a slow operation.
        // "*": contains asynchronous state change on the target (stopped, started, disappeared)
        // "=": contains supplementary information that the client should handle (e.g., a new breakpoint information)
        // "^": sync command result
        char mode = scanner.next().charAt(0);
        String klass = Native2DebuggerGdbMiProducer.parseKlass(scanner); // Note: not specified
        HashMap<String, Object> result = new HashMap<String, Object>();
        while (scanner.hasNext(",")) {
            scanner.next(",");
            String name = Native2DebuggerGdbMiProducer.parseString(scanner);
            scanner.next("=");
            Object value = Native2DebuggerGdbMiProducer.parseValue(scanner);
            result.put(name, value);
        }
        return new Native2DebuggerGdbMiStateResponse(token, mode, klass, result);
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
                "myToken=" + myToken +
                ", myMode=" + myMode +
                ", myKlass='" + myKlass + '\'' +
                ", myAttributes=" + myAttributes +
                '}';
    }

    public Optional<String> getToken() {
        return myToken;
    }
}
