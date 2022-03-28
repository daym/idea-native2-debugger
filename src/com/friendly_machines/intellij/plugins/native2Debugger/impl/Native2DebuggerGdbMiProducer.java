package com.friendly_machines.intellij.plugins.native2Debugger.impl;

import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.pty4j.unix.PTYInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * This reads async and sync responses from a pty input stream.
 * It sends each async responses to the application main thread via invokeLater.
 * It fills the sync response into a blocking queue.
 */
public class Native2DebuggerGdbMiProducer extends Thread {
    private final InputStream myChildOut;
    private final Native2DebugProcess myProcess;
    private final BlockingQueue<Native2DebuggerGdbMiStateResponse> myQueue = new LinkedBlockingDeque<Native2DebuggerGdbMiStateResponse>(1);

    // Both requests and responses have an optional "id" token in front (a numeral) which can be used to async-find the corresponding items. Maybe use those. (but async outputs, so those starting with one of "*+=", will not have them.
    protected static Optional<String> parseToken(Scanner scanner) {
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

    static String digits = "0123456789abcdef";
    private static String parseDigitsIntoCode(Scanner scanner, int radix, int maxLength) {
        int result = 0;
        for (; maxLength > 0; --maxLength) {
            char c = scanner.next().charAt(0);
            int digit = digits.indexOf(c);
            if (digit == -1 || digit >= radix) { // error
                return "";
            }
            result *= radix;
            result += digit;
        }
        return Character.toString(result);
    }
    private static String parseCString(Scanner scanner) {
        String result = "";
        scanner.next("\"");
        boolean escape = false;
        while (scanner.hasNext()) {
            if (escape) {
                char c = scanner.next().charAt(0);
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
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                        result += parseDigitsIntoCode(scanner,8, 3);
                        break;
                    case 'x':
                        scanner.next();
                        result += parseDigitsIntoCode(scanner,16, 2);
                        break;
                    case 'u':
                        scanner.next();
                        result += parseDigitsIntoCode(scanner,16, 4);
                        break;
                    case 'U':
                        scanner.next();
                        result += parseDigitsIntoCode(scanner,16, 8);
                        break;
                    default:
                        result += c;
                        break;
                }
                escape = false;
                continue;
            }
            if (scanner.hasNext("[\\x5C]")) {
                escape = true;
            } else if (scanner.hasNext("\"")) {
                break;
            } else {
                char c = scanner.next().charAt(0);
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
    public static String parseString(Scanner scanner) {
        String result = scanner.next("[a-zA-Z-]");

        while (scanner.hasNext("[a-zA-Z0-9-]")) {
            char c = scanner.next().charAt(0);
            result += c;
        }
        return result;
    }

    public static String parseKlass(Scanner scanner) {
        return parseString(scanner);
    }

    public static Object parseValue(Scanner scanner) {
        /* c-string | tuple | list
        tuple ==> "{}" | "{" result ( "," result )* "}"
        list ==> "[]"
               | "[" value ( "," value )* "]"
               | "[" result ( "," result )* "]"
        result ==> variable "=" value
        value ==> const | tuple | list
        */
        if (scanner.hasNext("\\{")) {
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
        } else if (scanner.hasNext("\\[")) {
            scanner.next("\\[");
            if (scanner.hasNext("\\]")) {
                scanner.next("\\]");
                return new String[0];
            } else if (scanner.hasNext("[a-zA-Z-]")) { // name=value
                List<Map.Entry<String, Object>> result = new ArrayList<>();
                while (scanner.hasNext() && !scanner.hasNext("\\]")) {
                    String name = parseString(scanner);
                    scanner.next("=");
                    Object value = parseValue(scanner);
                    result.add(new java.util.AbstractMap.SimpleEntry<>(name, value));
                    if (scanner.hasNext(",")) {
                        scanner.next();
                    } else {
                        break;
                    }
                }
                scanner.next("\\]");
                return result;
            } else { // list of "value"s, not of "name=value"s
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
        } else {
            return parseCString(scanner);
        }
    }

    private void processLine(String line) throws InterruptedException {
        if (line.strip() == "(gdb)") {
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
            // FIXME StatusBar.Info.set(line, myProject, "Debugger");
            Native2DebuggerGdbMiStateResponse response = Native2DebuggerGdbMiStateResponse.decode(token, scanner);
            System.err.println("STATE: " + response);

            // "*stopped"
            // "=breakpoint-modified"
            if (response.getMode() == '^') {
                System.err.println("PUTTING INTO QUEUE: " + response);
                if (!response.getToken().isPresent()) { // that's a sync response for something we didn't ask
                    System.err.println("IGNORING UNSOLICITED RESPONSE " + response);
                } else {
                    myQueue.put(response); // note: Can block
                }
            } else { // async
                ApplicationManager.getApplication().invokeLater(() -> {
                    myProcess.handleGdbMiStateOutput(response);
                });
            }
        } else if (scanner.hasNext("[~@&]")) { // streams
            char mode = scanner.next().charAt(0);
            String text = parseCString(scanner);
            if (mode == '&') {
                // FIXME StatusBar.Info.set(text, myProject, "Debugger");
            } else {
                // TODO: Find a better place for these
                // FIXME StatusBar.Info.set(text, myProject, "Stream");
            }
        } else if (scanner.hasNext("-")) { // our echo
        } else { // TODO: else "(gdb)" maybe?
            System.err.println("THREAD: JUNK " + line);
            System.err.flush();
        }
    }

    private String readLine() throws IOException {
        StringBuilder buffer = new StringBuilder();
        int c;
        // TODO: timeout
        while ((c = myChildOut.read()) >= 0) {
            buffer.append(Character.toString(c));
            if (c == 10) {
                break;
            }
        }
        return buffer.toString();
    }

    public Native2DebuggerGdbMiProducer(PTYInputStream childOut, Native2DebugProcess process) {
        myProcess = process;
        myChildOut = new InputStream() { // FIXME: Use InputStreamReader wrapper to decode utf-8
            @Override
            public int read() throws IOException {
                int result = childOut.read();
                System.err.print(Character.toString(result));
                System.err.flush();
                return result;
            }
        };
    }

    @Override
    public void run() {
        while (true) {
            try {
                final String line = readLine();
                System.err.println("THREAD: GOT LINE " + line);
                System.err.flush();
                processLine(line);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e); // FIXME?
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
//            synchronized (this) {
//                responseLine = line;
//                this.notify();
//            }
        }
        System.err.println("THREAD: AFTER RUNNING");
        System.err.flush();
    }

    public Native2DebuggerGdbMiStateResponse readResponse() {
        try {
            return myQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
