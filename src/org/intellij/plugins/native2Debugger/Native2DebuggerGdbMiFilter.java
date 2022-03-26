package org.intellij.plugins.native2Debugger;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.process.OSProcessHandler;
import org.intellij.plugins.native2Debugger.impl.Native2DebugProcess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Native2DebuggerGdbMiFilter implements Filter {
    private final OSProcessHandler myOsProcessHandler;

    public Native2DebuggerGdbMiFilter(OSProcessHandler osProcessHandler) {
        myOsProcessHandler =  osProcessHandler;
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

    private static String parseCString(Scanner scanner) {
        String result = "";
        scanner.next("\"");
        while (scanner.hasNext()) {
            if (scanner.hasNext("\"")) {
                break;
            }
            char c = scanner.next().charAt(0);
            // FIXME: backslash escape
            result += c;
        }
        scanner.next("\"");
        return result;
    }

    // Not specified in GDB manual
    private static String parseString(Scanner scanner) {
        String result = scanner.next("[a-zA-Z-]");

        while (scanner.hasNext("[a-zA-Z0-9-]")) {
            char c = scanner.next().charAt(0);
            result += c;
        }
        return result;
    }

    private static String parseKlass(Scanner scanner) {
        return parseString(scanner);
    }

    private static Object parseValue(Scanner scanner) {
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
                HashMap<String, Object> result = new HashMap<String, Object>();
                while (scanner.hasNext() && !scanner.hasNext("\\]")) {
                    String name = parseString(scanner);
                    scanner.next("=");
                    Object value = parseValue(scanner);
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

    static void parseLine(String line, Native2DebugProcess process) {
        Scanner scanner = new Scanner(line);
        scanner.useDelimiter(""); // character by character mode
        Optional<String> token = parseToken(scanner);
        if (scanner.hasNext("[*+=^]")) {
            // "+": contains on-going status information about the progress of a slow operation.
            // "*": contains asynchronous state change on the target (stopped, started, disappeared)
            // "=": contains supplementary information that the client should handle (e.g., a new breakpoint information)
            // "^": sync command result
            char mode = scanner.next().charAt(0);
            String klass = parseKlass(scanner); // Note: not specified
            HashMap<String, Object> result = new HashMap<String, Object>();
            while (scanner.hasNext(",")) {
                scanner.next(",");
                String name = parseString(scanner);
                scanner.next("=");
                Object value = parseValue(scanner);
                result.put(name, value);
            }
            System.err.println("STATE: " + mode + klass + result);

            // "*stopped"
            // "=breakpoint-modified"
            process.handleGdbMiStateOutput(mode, klass, result);
        } else if (scanner.hasNext("[~@&]")) { // streams
            char mode = scanner.next().charAt(0);
            String text = parseCString(scanner);
            // Ignore for now
        } // TODO: else "(gdb)" maybe?
    }

    // Called for each LINE.
    @Override
    public @Nullable Result applyFilter(@NotNull String line, int entireLength) {
        if (line.endsWith("\n"))
            line = line.substring(0, line.length() - 1);
        if (line.endsWith("\r"))
            line = line.substring(0, line.length() - 1);
        if (line.strip() == "(gdb)") {
            // Ignore
        } else if (line.length() > 0) {
            try {
                Native2DebugProcess process = Native2DebugProcess.getInstance(myOsProcessHandler);
                parseLine(line, process);
            } catch (NoSuchElementException e) {
                e.printStackTrace();
                // ignore error
            }
        }
        return null;
    }
}
