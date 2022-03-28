package com.friendly_machines.intellij.plugins.native2Debugger.impl;

import com.intellij.execution.process.OSProcessHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.pty4j.unix.PTYInputStream;
import com.pty4j.unix.PTYOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Native2DebuggerGdbMiFilter /*implements Filter*/ {
    private final OSProcessHandler myOsProcessHandler;
    private final Project myProject;
    private final PTYOutputStream myChildIn;
    private final InputStream myChildOut;

    public Native2DebuggerGdbMiFilter(OSProcessHandler osProcessHandler, @NotNull Project project, PTYInputStream childOut, PTYOutputStream childIn) {
        myOsProcessHandler =  osProcessHandler;
        myProject = project;
        //myChildOut = childOut;
        // TODO: InputStreamReader ?
        myChildOut = new InputStream() { // FIXME: Use InputStreamReader wrapper to decode utf-8
            @Override
            public int read() throws IOException {
                int result = childOut.read();
                System.err.print(Character.toString(result));
                System.err.flush();
                return result;
            }
        };
        // TODO: PipedReader, PipedWriter

        myChildIn = childIn;
        // https://www.kfu.com/~nsayer/Java/jni-filedesc.html
        // new FileReader(FileDescriptor)
    }

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

    private Native2DebuggerGdbMiStateResponse processLine(String line, Native2DebugProcess process) {
        Scanner scanner = new Scanner(line);
        scanner.useDelimiter(""); // character by character mode
        Optional<String> token = parseToken(scanner);
        if (scanner.hasNext("[*+=^]")) {
            StatusBar.Info.set(line, myProject, "Debugger");
            Native2DebuggerGdbMiStateResponse response = Native2DebuggerGdbMiStateResponse.decode(scanner);
            //System.err.println("STATE: " + mode + klass + result);

            // "*stopped"
            // "=breakpoint-modified"
            process.handleGdbMiStateOutput(response);
            return response;
        } else if (scanner.hasNext("[~@&]")) { // streams
            char mode = scanner.next().charAt(0);
            String text = parseCString(scanner);
            if (mode == '&') {
                StatusBar.Info.set(text, myProject, "Debugger");
            } else {
                // TODO: Find a better place for these
                StatusBar.Info.set(text, myProject, "Stream");
            }
            return null;
        } else { // TODO: else "(gdb)" maybe?
            return null;
        }
    }

//    // Called for each LINE.
//    @Override
//    public @Nullable Result applyFilter(@NotNull String line, int entireLength) {
//        if (line.endsWith("\n"))
//            line = line.substring(0, line.length() - 1);
//        if (line.endsWith("\r"))
//            line = line.substring(0, line.length() - 1);
//        if (line.strip() == "(gdb)") {
//            // Ignore
//        } else if (line.length() > 0) {
//            try {
//                Native2DebugProcess process = Native2DebugProcess.getInstance(myOsProcessHandler);
//                parseLine(line, process);
//            } catch (NoSuchElementException e) {
//                e.printStackTrace();
//                // ignore error
//            }
//        }
//        return null;
//    }

    private String readLine() throws IOException {
        StringBuilder buffer = new StringBuilder();
        int c;
        // TODO: timeout
        while ((c = myChildOut.read()) >= 0) {
            System.err.println("L2 got C: " + Character.toString(c));
            System.err.flush();
            buffer.append(Character.toString(c));
            if (c == 10) {
                break;
            }
        }
        return buffer.toString();
    }
    /// Read the sync response from gdb. If there are async responses, those are handled as a side-effect.
    private Native2DebuggerGdbMiStateResponse readResponse() throws IOException {
        Native2DebugProcess process = Native2DebugProcess.getInstance(myOsProcessHandler);
        Native2DebuggerGdbMiStateResponse response = null;
        do {
            String line = readLine();
            System.err.println("got line: " + line);
            StatusBar.Info.set(line, myProject, "Debugger");
            response = processLine(line, process);
        } while (response == null || response.getMode() != '^');
        // "+": contains on-going status information about the progress of a slow operation.
        // "*": contains asynchronous state change on the target (stopped, started, disappeared)
        // "=": contains supplementary information that the client should handle (e.g., a new breakpoint information)
        // "^": sync command result
        return response;
    }

    public Native2DebuggerGdbMiStateResponse gdbSend(String operation, String[] options, String[] parameters) {
        try {
            myChildIn.write(operation.getBytes(StandardCharsets.UTF_8));
            for (String option : options) {
                myChildIn.write(" ".getBytes(StandardCharsets.UTF_8));
                myChildIn.write(option.getBytes(StandardCharsets.UTF_8));  // TODO: c string quote
            }
            if (parameters.length > 0) {
                myChildIn.write(" --".getBytes(StandardCharsets.UTF_8));
                for (String parameter : parameters) {
                    myChildIn.write(" ".getBytes(StandardCharsets.UTF_8));
                    myChildIn.write(parameter.getBytes(StandardCharsets.UTF_8));  // TODO: c string quote
                }
            }
            myChildIn.write("\r\n".getBytes(StandardCharsets.UTF_8));
            myChildIn.flush();
            System.err.println("BEFORE RESPONSE READING");
            System.err.flush();
            Native2DebuggerGdbMiStateResponse response = readResponse();
            System.err.println("AFTER RESPONSE READING " + response);
            System.err.flush();
            return response;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public HashMap<String, Object> gdbCall(String operation, String[] options, String[] parameters) throws Native2DebuggerGdbMiOperationException {
        Native2DebuggerGdbMiStateResponse response = gdbSend(operation, options, parameters);
        assert response.getMode() == '^';
        if (!"done".equals(response.getKlass())) {
            throw new Native2DebuggerGdbMiOperationException();
        }
        return response.getAttributes();
    }
}
