// Copyright 2022 Danny Milosavljevic. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.

package com.friendly_machines.intellij.plugins.ideanative2debugger.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import static java.lang.Thread.currentThread;

/**
 * This reads async and sync responses from a pty input stream.
 * It sends each async responses to the application main thread via invokeLater.
 * It fills the sync response into a blocking queue.
 */
public class GdbMiProducer /*extends Thread*/ {
    //    private final BufferedReader myChildOut;
    private final BlockingQueue<GdbMiStateResponse> myQueue = new LinkedBlockingDeque<GdbMiStateResponse>(1);

    // Both requests and responses have an optional "id" token in front (a numeral) which can be used to find the corresponding request to a response. Maybe use those.
    // But async outputs, so those starting with one of "*+=", will not have them.
    public static Optional<String> parseToken(@NotNull Scanner scanner) {
        String result = "";
        while (scanner.hasNext("[0-9]")) {
            String part = scanner.next("[0-9]");
            result = result + part;
        }
        if (result.length() > 0)
            return Optional.of(result);
        else
            return Optional.empty();
    }

    // Consume one character and give that one back
    private static char consume(Scanner scanner) {
        return scanner.next().charAt(0);
    }

    private static String digits = "01234567";

    @NotNull
    private static String parseDigitsIntoCode(Scanner scanner, int radix, int maxLength) {
        int result = 0;
        for (; maxLength > 0; --maxLength) {
            char c = consume(scanner);
            int digit = digits.indexOf(c);
            if (digit == -1 || digit >= radix) { // error
                break;
            }
            result *= radix;
            result += digit;
        }
        return Character.toString(result);
    }

    // Modifies RESULT.
    @NotNull
    private static void interpretEscapeSequenceBody(@NotNull Scanner scanner, @NotNull Appendable result) throws IOException {
        if (scanner.hasNext("[0-7]")) {
            result.append(parseDigitsIntoCode(scanner, 8, 3));
        } else {
            char c = consume(scanner);
            switch (c) {
                case 'a':
                    result.append((char) 0x7);
                    break;
                case 'b':
                    result.append((char) 0x8);
                    break;
                case 'f':
                    result.append((char) 0xc);
                    break;
                case 'n':
                    result.append((char) 0xa);
                    break;
                case 'r':
                    result.append((char) 0xd);
                    break;
                case 't':
                    result.append((char) 0x9);
                    break;
                case 'v':
                    result.append((char) 0xb);
                    break;
                case 'x':
                    result.append(parseDigitsIntoCode(scanner, 16, 2));
                    break;
                case 'u':
                    result.append(parseDigitsIntoCode(scanner, 16, 4));
                    break;
                case 'U':
                    result.append(parseDigitsIntoCode(scanner, 16, 8));
                    break;
                default:
                    result.append(c);
                    break;
            }
        }
    }

    @NotNull
    public static String parseCString(@NotNull Scanner scanner) {
        StringBuilder result = new StringBuilder();
        scanner.next("\"");
        try {
            while (scanner.hasNext()) {
                if (scanner.hasNext("\\\\")) { // backslash
                    scanner.next();
                    interpretEscapeSequenceBody(scanner, result);
                } else if (scanner.hasNext("\"")) {
                    break;
                } else {
                    char c = consume(scanner);
                    result.append(c);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        if (escape) {
//
//        }
        scanner.next("\"");
        return result.toString();
    }


    // Not specified in GDB manual
    @NotNull
    public static String parseSymbol(@NotNull Scanner scanner) {
        String result = scanner.next("[a-zA-Z_-]");

        while (scanner.hasNext("[a-zA-Z0-9_-]")) {
            char c = consume(scanner);
            result += c;
        }
        return result;
    }

    @NotNull
    public static String parseKlass(Scanner scanner) {
        return parseSymbol(scanner);
    }

    @NotNull
    private static HashMap<String, Object> parseTuple(@NotNull Scanner scanner) {
        scanner.next("\\{");
        HashMap<String, Object> result = new HashMap<String, Object>();
        while (scanner.hasNext()) {
            if (scanner.hasNext("\\}")) {
                break;
            }
            String name = parseSymbol(scanner);
            scanner.next("=");
            Object value = parseValue(scanner);
            result.put(name, value);
            if (scanner.hasNext(",")) {
                scanner.next();
            } else {
                break;
            }
        }
        scanner.next("\\}");
        return result;
    }

    @NotNull
    private static List<Map.Entry<String, Object>> parseKeyValueList(@NotNull Scanner scanner) {
        List<Map.Entry<String, Object>> result = new ArrayList<>();
        while (scanner.hasNext() && !scanner.hasNext("\\]")) {
            String name = parseSymbol(scanner);
            scanner.next("=");
            Object value = parseValue(scanner);
            result.add(new AbstractMap.SimpleEntry<>(name, value));
            if (scanner.hasNext(",")) {
                scanner.next();
            } else {
                break;
            }
        }
        scanner.next("\\]");
        return result;
    }

    @NotNull
    private static ArrayList<Object> parsePrimitiveList(@NotNull Scanner scanner) {
        ArrayList<Object> result = new ArrayList<Object>();
        while (scanner.hasNext() && !scanner.hasNext("\\]")) {
            Object value = parseValue(scanner);
            result.add(value);
            if (scanner.hasNext(",")) {
                scanner.next();
            } else {
                break;
            }
        }
        scanner.next("\\]");
        return result;
    }

    @NotNull
    private static List<?> parseList(@NotNull Scanner scanner) {
        scanner.next("\\[");
        if (scanner.hasNext("\\]")) {
            scanner.next("\\]");
            return new ArrayList<Object>();
        } else if (scanner.hasNext("[a-zA-Z-]")) { // name=value
            List<Map.Entry<String, Object>> result = parseKeyValueList(scanner);
            return result;
        } else { // list of "value"s, not of "name=value"s
            ArrayList<Object> result = parsePrimitiveList(scanner);
            return result;
        }
    }

    @Nullable
    public static Object parseValue(@NotNull Scanner scanner) {
        /* c-string | tuple | list
        tuple ==> "{}" | "{" result ( "," result )* "}"
        list ==> "[]"
               | "[" value ( "," value )* "]"
               | "[" result ( "," result )* "]"
        result ==> variable "=" value
        value ==> const | tuple | list
        */
        if (scanner.hasNext("\\{")) {
            return parseTuple(scanner);
        } else if (scanner.hasNext("\\[")) {
            return parseList(scanner);
        } else {
            return parseCString(scanner);
        }
    }

    public GdbMiProducer() {

    }

    public void produce(GdbMiStateResponse item) throws InterruptedException {
        System.err.println(currentThread().getId() + currentThread().getName() + " produce");
        myQueue.put(item);
    }

    public GdbMiStateResponse consume() throws InterruptedException {
        System.err.println(currentThread().getId() + currentThread().getName() + " consume");
        return myQueue.take();
    }
}
