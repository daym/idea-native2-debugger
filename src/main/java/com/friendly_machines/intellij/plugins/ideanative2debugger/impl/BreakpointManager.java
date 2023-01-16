/*
 * Copyright 2007 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.friendly_machines.intellij.plugins.ideanative2debugger.impl;

import com.friendly_machines.intellij.plugins.ideanative2debugger.AdaCatchpointProperties;
import com.friendly_machines.intellij.plugins.ideanative2debugger.CxxCatchpointProperties;
import com.friendly_machines.intellij.plugins.ideanative2debugger.ShlibCatchpointProperties;
import com.friendly_machines.intellij.plugins.ideanative2debugger.WatchpointProperties;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.friendly_machines.intellij.plugins.ideanative2debugger.impl.ShlibCatchpointCatchType.Load;

// Note: one line can map to multiple actual addrs! (but that's GDB's business)
public class BreakpointManager {
    private final List<Breakpoint> myBreakpoints = new ArrayList<>();
    private final DebugProcess myDebugProcess;

    public BreakpointManager(DebugProcess debugProcess) {
        myDebugProcess = debugProcess;
    }

    public static String fileLineReference(XSourcePosition position) {
        return position.getFile().getPath() + ":" + (position.getLine() + 1);
    }

    public boolean addCxxCatchpoint(XBreakpoint<CxxCatchpointProperties> key) throws InterruptedException {
        var options = new ArrayList<String>();
//        if (key.isTemporary()) // FIXME: MISSING!
//            options.add("-t");
        switch (key.getSuspendPolicy()) {
            case NONE:
                break;
            // TODO: the others
        }
        // TODO: key.isLogMessage()
        try {
            var properties = (CxxCatchpointProperties) key.getProperties();
            options.add("-r");
            options.add(properties.myExceptionRegexp);
            var gdbResponse = switch (properties.myCatchType) {
                case Throw -> myDebugProcess.catchThrow(options);
                case Rethrow -> myDebugProcess.catchRethrow(options);
                case Catch -> myDebugProcess.catchCatch(options);
            };
            var bkpt = (Map<String, Object>) gdbResponse.get("bkpt");
            if (!key.isEnabled()) {
                // Note: I think this is never reached
                String number = (String) bkpt.get("number");
                myDebugProcess.breakDisable(number);
                bkpt.put("enabled", "n");
            }

            myBreakpoints.add(new Breakpoint(myDebugProcess, key, bkpt)); // Note: confuses breakpoints and catchpoints.
            return true;
        } catch (GdbMiOperationException | ClassCastException | IOException e) { // TODO
            // TODO: doesn't work: myDebugProcess.getSession().setBreakpointInvalid(key, "Unsupported breakpoint");
            return false;
        }
    }
    public boolean addAdaCatchpoint(XBreakpoint<AdaCatchpointProperties> key) throws InterruptedException {
        var options = new ArrayList<String>();
//        if (key.isTemporary()) // FIXME: MISSING!
//            options.add("-t");
        switch (key.getSuspendPolicy()) {
            case NONE:
                break;
            // TODO: the others
        }
        // TODO: key.isLogMessage()
        try {
            var properties = (AdaCatchpointProperties) key.getProperties();
            if (!properties.myCondition.isEmpty()) {
                options.add("-c");
                options.add(properties.myCondition);
            }
            if (properties.myException.isEmpty()) {
                switch (properties.myCatchType) {
                    case Exception:
                        options.add("-u");
                        break;
                    default:
                        /* no -u, but OK */
                        break;
                }
            } else {
                switch (properties.myCatchType) {
                    case Exception:
                    case Handlers:
                        options.add("-e");
                        options.add(properties.myException);
                        break;
                    case Assertion:
                        break;
                }
            }
            if (!key.isEnabled()) {
                options.add("-d");
            }
            var gdbResponse = switch (properties.myCatchType) {
                case Exception -> myDebugProcess.catchException(options);
                case Handlers -> myDebugProcess.catchHandlers(options);
                case Assertion -> myDebugProcess.catchAssert(options);
            };
            var bkpt = (Map<String, Object>) gdbResponse.get("bkpt");

            myBreakpoints.add(new Breakpoint(myDebugProcess, key, bkpt)); // Note: confuses breakpoints and catchpoints.
            return true;
        } catch (GdbMiOperationException | ClassCastException | IOException e) { // TODO
            // TODO: doesn't work: myDebugProcess.getSession().setBreakpointInvalid(key, "Unsupported breakpoint");
            return false;
        }
    }
    public boolean addShlibCatchpoint(XBreakpoint<ShlibCatchpointProperties> key) throws InterruptedException {
        var options = new ArrayList<String>();
//        if (key.isTemporary()) // FIXME: MISSING!
//            options.add("-t");
        switch (key.getSuspendPolicy()) {
            case NONE:
                break;
            // TODO: the others
        }
        // TODO: key.isLogMessage()
        try {
            var properties = (ShlibCatchpointProperties) key.getProperties();
            if (!key.isEnabled()) {
                options.add("-d");
            }
            options.add(properties.myLibraryNameRegexp);
            var gdbResponse = switch (properties.myCatchType) {
                case Load -> myDebugProcess.catchLoad(options);
                case Unload -> myDebugProcess.catchUnload(options);
            };
            var bkpt = (Map<String, Object>) gdbResponse.get("bkpt");

            myBreakpoints.add(new Breakpoint(myDebugProcess, key, bkpt)); // Note: confuses breakpoints and catchpoints.
            return true;
        } catch (GdbMiOperationException | ClassCastException | IOException e) { // TODO
            // TODO: doesn't work: myDebugProcess.getSession().setBreakpointInvalid(key, "Unsupported breakpoint");
            return false;
        }
    }

    public boolean addWatchpoint(XBreakpoint<WatchpointProperties> key) throws InterruptedException {
        var options = new ArrayList<String>();
//        if (key.isTemporary()) // FIXME: MISSING!
//            options.add("-t");
        switch (key.getSuspendPolicy()) {
            case NONE:
                break;
            // TODO: the others
        }
        // TODO: key.isLogMessage()
        try {
            var properties = key.getProperties();
            // TODO: !key.isEnabled()
            if (properties.myWatchReads && properties.myWatchWrites)
                options.add("-a");
            else if (properties.myWatchReads)
                options.add("-r");
            options.add(properties.myExpression);
            var gdbResponse = myDebugProcess.breakWatch(options);
            var bkpt = (Map<String, Object>) gdbResponse.get("bkpt");

            myBreakpoints.add(new Breakpoint(myDebugProcess, key, bkpt)); // Note: confuses breakpoints and catchpoints.
            return true;
        } catch (GdbMiOperationException | ClassCastException | IOException e) { // TODO
            // TODO: doesn't work: myDebugProcess.getSession().setBreakpointInvalid(key, "Unsupported breakpoint");
            return false;
        }
    }

    public boolean addBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> key) throws InterruptedException {
        // TODO: Just store our Breakpoint in the user data of KEY and then you don't need myBreakpoints in the first place.
        final XSourcePosition sourcePosition = key.getSourcePosition();
        if (sourcePosition == null || !sourcePosition.getFile().exists() || !sourcePosition.getFile().isValid()) {
            myDebugProcess.getSession().setBreakpointInvalid(key, "Unsupported file for breakpoint");
            return false;
        }

        //final VirtualFile file = sourcePosition.getFile();
        //final Project project = myDebugProcess.getSession().getProject();
        final int lineNumber = sourcePosition.getLine();
        if (lineNumber == -1) {
            myDebugProcess.getSession().setBreakpointInvalid(key, "Unsupported breakpoint position");
            return false;
        }

        ArrayList<String> options = new ArrayList<>();
        // TODO: "-h" for hardware breakpoint
        // TODO: "-f" for creating a pending breakpoint if necessary
        // TODO: "-i ignore-count"
        // TODO: "-p thread-id"
        // TODO: -break-passcount <tracepoint-id> <passcount>
        if (key.isTemporary())
            options.add("-t");
        if (!key.isEnabled())
            options.add("-d");
        @Nullable String condition = key.getCondition();
        if (condition != null) {
            options.add("-c");
            options.add(condition);
        }
        switch (key.getSuspendPolicy()) {
        case NONE:
            // options.add("-a"); // tracepoints are not that well-supported and it also doesn't do what we want
            break;
        // TODO: the others
        }
        // TODO: key.getProperties().getCOUNT_FILTER();
        // TODO: breakpoint.isLogStack()
        try {
            Map<String, ?> gdbResponse;
            if (key.isLogMessage()) {
                gdbResponse = myDebugProcess.dprintfInsert(options, List.of(fileLineReference(key.getSourcePosition()), "Breakpointhit"));
            } else {
                gdbResponse = myDebugProcess.breakInsert(options, List.of(fileLineReference(key.getSourcePosition())));
            }
            @SuppressWarnings("unchecked")
            var bkpt = (Map<String, Object>) gdbResponse.get("bkpt");
            myBreakpoints.add(new Breakpoint(myDebugProcess, key, bkpt));
            return true;
        } catch (GdbMiOperationException | ClassCastException | IOException e) {
            myDebugProcess.getSession().setBreakpointInvalid(key, "Unsupported breakpoint position");
            return false;
        }
    }

    public Optional<Breakpoint> getBreakpointByGdbNumber(String key) {
        for (Breakpoint breakpoint : myBreakpoints) {
            String number = breakpoint.getNumber();
            if (number.equals(key)) {
                return Optional.of(breakpoint);
            }
        }
        return Optional.empty();
    }

    public Optional<Breakpoint> getBreakpoint(XBreakpoint key) {
        for (Breakpoint breakpoint : myBreakpoints) {
            if (breakpoint.getXBreakpoint() == key) {
                return Optional.of(breakpoint);
            }
        }
        return Optional.empty();
    }

    // Or catchpoint.
    public boolean deleteBreakpoint(XBreakpoint key) throws InterruptedException {
        Optional<Breakpoint> breakpointo = getBreakpoint(key);
        if (breakpointo.isPresent()) {
            Breakpoint breakpoint = breakpointo.get();
            String number = breakpoint.getNumber();
            try {
                myDebugProcess.breakDelete(number);
                myBreakpoints.remove(breakpoint);
                return true;
            } catch (GdbMiOperationException e) {
                myDebugProcess.reportError("Breakpoint could not be deleted in GDB", e);
                return false;
            } catch (IOException e) {
                myDebugProcess.reportError("GDB communication error, " + e.toString());
                return false;
            }
        } else {
            myDebugProcess.reportError("Breakpoint to delete was not found");
            return false;
        }
    }

    /// Note: This does not send a -break-delete to gdb because it's usually called as a reaction to gdb deleting the
    /// breakpoint in the first place
    public boolean deleteBreakpointByGdbNumber(String number) {
        Optional<Breakpoint> breakpointo = getBreakpointByGdbNumber(number);
        if (breakpointo.isPresent()) {
            Breakpoint breakpoint = breakpointo.get();
            // not myDebugProcess.breakDelete(number);
            myBreakpoints.remove(breakpoint);
            return true;
        } else
            return false;
    }
}
