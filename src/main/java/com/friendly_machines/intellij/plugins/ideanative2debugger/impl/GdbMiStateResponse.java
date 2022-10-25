// Copyright 2022 Danny Milosavljevic. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.

package com.friendly_machines.intellij.plugins.ideanative2debugger.impl;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

public class GdbMiStateResponse {
    private final char myMode;
    private final Optional<String> myToken;
    private final String myKlass;
    private final Map<String, ?> myAttributes;

    public GdbMiStateResponse(Optional<String> token, char mode, String klass, Map<String, ?> attributes) {
        myToken = token;
        myMode = mode;
        myKlass = klass;
        myAttributes = attributes;
    }

    public static GdbMiStateResponse decode(Optional<String> token, @NotNull Scanner scanner) {
        // "+": contains on-going status information about the progress of a slow operation.
        // "*": contains asynchronous state change on the target (stopped, started, disappeared)
        // "=": contains supplementary information that the client should handle (e.g., a new breakpoint information)
        // "^": sync command result
        char mode = scanner.next().charAt(0);
        String klass = GdbMiProducer.parseKlass(scanner); // Note: not specified
        var result = new java.util.HashMap<String, Object>();
        while (scanner.hasNext(",")) {
            scanner.next(",");
            var name = GdbMiProducer.parseSymbol(scanner);
            scanner.next("=");
            var value = GdbMiProducer.parseValue(scanner);
            result.put(name, value);
        }
        return new GdbMiStateResponse(token, mode, klass, result);
    }

    public static GdbMiStateResponse errorResponse(Optional<String> token, char mode, String klass, String errorMessage) {
        // TODO: add error message somehow?
        return new GdbMiStateResponse(token, mode, klass, new java.util.HashMap<>());
    }

    public char getMode() {
        return myMode;
    }

    public String getKlass() {
        return myKlass;
    }

    public Map<String, ?> getAttributes() {
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
