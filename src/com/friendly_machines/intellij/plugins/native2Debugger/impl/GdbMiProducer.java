// Copyright 2022 Danny Milosavljevic. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.

package com.friendly_machines.intellij.plugins.native2Debugger.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.pty4j.unix.Pty;
import jtermios.JTermios;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * This reads async and sync responses from a pty input stream.
 * It sends each async responses to the application main thread via invokeLater.
 * It fills the sync response into a blocking queue.
 */
public class GdbMiProducer extends Thread {
    private final Pty myChildOut;
    private final DebugProcess myProcess;
    private final BlockingQueue<GdbMiStateResponse> myQueue = new LinkedBlockingDeque<GdbMiStateResponse>(1);

    // Both requests and responses have an optional "id" token in front (a numeral) which can be used to find the corresponding request to a response. Maybe use those.
    // But async outputs, so those starting with one of "*+=", will not have them.
    protected static Optional<String> parseToken(@NotNull Scanner scanner) {
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
    private static void interpretEscapeSequenceBody(@NotNull Scanner scanner, @NotNull String result) {
        if (scanner.hasNext("[0-7]")) {
            result += parseDigitsIntoCode(scanner, 8, 3);
        } else {
            char c = consume(scanner);
            switch (c) {
                case 'a':
                    result += (char) 0x7;
                    break;
                case 'b':
                    result += (char) 0x8;
                    break;
                case 'f':
                    result += (char) 0xc;
                    break;
                case 'n':
                    result += (char) 0xa;
                    break;
                case 'r':
                    result += (char) 0xd;
                    break;
                case 't':
                    result += (char) 0x9;
                    break;
                case 'v':
                    result += (char) 0xb;
                    break;
                case 'x':
                    result += parseDigitsIntoCode(scanner, 16, 2);
                    break;
                case 'u':
                    result += parseDigitsIntoCode(scanner, 16, 4);
                    break;
                case 'U':
                    result += parseDigitsIntoCode(scanner, 16, 8);
                    break;
                default:
                    result += c;
                    break;
            }
        }
    }

    private static String parseCString(@NotNull Scanner scanner) {
        String result = "";
        scanner.next("\"");
        while (scanner.hasNext()) {
            if (scanner.hasNext("[\\x5C]")) { // backslash
                scanner.next();
                interpretEscapeSequenceBody(scanner, result);
            } else if (scanner.hasNext("\"")) {
                break;
            } else {
                char c = consume(scanner);
                result += c;
            }
        }
//        if (escape) {
//
//        }
        scanner.next("\"");
        return result;
    }


    // Not specified in GDB manual
    public static String parseString(@NotNull Scanner scanner) {
        String result = scanner.next("[a-zA-Z-]");

        while (scanner.hasNext("[a-zA-Z0-9-]")) {
            char c = consume(scanner);
            result += c;
        }
        return result;
    }

    public static String parseKlass(Scanner scanner) {
        return parseString(scanner);
    }

    @NotNull
    private static HashMap<String, Object> parseTuple(@NotNull Scanner scanner) {
        scanner.next("\\{");
        HashMap<String, Object> result = new HashMap<String, Object>();
        while (scanner.hasNext()) {
            if (scanner.hasNext("\\}")) {
                break;
            }
            String name = parseString(scanner);
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
            String name = parseString(scanner);
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

    private void processLine(@NotNull String line) throws InterruptedException {
        if ("(gdb)".equals(line.strip())) {
            return;
        }

        Scanner scanner = new Scanner(line);
        scanner.useDelimiter(""); // character by character mode
        Optional<String> token = parseToken(scanner);
        // "+": contains on-going status information about the progress of a slow operation.
        // "*": contains asynchronous state change on the target (stopped, started, disappeared)
        // "=": contains supplementary information that the client should handle (e.g., a new breakpoint information)
        // "^": sync command result
        if (scanner.hasNext("[*+=^]")) {
            GdbMiStateResponse response = GdbMiStateResponse.decode(token, scanner);

            // "*stopped"
            // "=breakpoint-modified"
            if (response.getMode() == '^') {
                if (response.getToken().isPresent()) {
                    myQueue.put(response); // note: Can block
                } else { // that's a sync response for something we didn't ask
                }
            } else { // async
                ApplicationManager.getApplication().invokeLater(() -> {
                    myProcess.handleGdbMiStateOutput(response);
                });
            }
        } else if (scanner.hasNext("[~@&]")) { // streams
            char mode = consume(scanner);
            String text = parseCString(scanner);
            ApplicationManager.getApplication().invokeLater(() -> {
                myProcess.handleGdbTextOutput(mode, text);
            });
        } else if (scanner.hasNext("-")) { // our echo
        } else {
        }
    }

    @NotNull
    private String readLine() throws IOException, InterruptedException {
        StringBuilder buffer = new StringBuilder();
        int fd = myChildOut.getMasterFD();
        byte[] buf = new byte[1];
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

    public GdbMiProducer(Pty childOut, DebugProcess process) {
        myProcess = process;
        myChildOut = childOut;
    }

    @Override
    public void run() {
        while (true) {
            try {
                final String line = readLine();
                processLine(line);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                break;
            }
//            synchronized (this) {
//                responseLine = line;
//                this.notify();
//            }
        }
    }

    public GdbMiStateResponse readResponse() {
        try {
            return myQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
