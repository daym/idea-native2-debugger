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

import com.friendly_machines.intellij.plugins.ideanative2debugger.impl.DebugProcess;
import com.friendly_machines.intellij.plugins.ideanative2debugger.impl.GdbMiOperationException;
import com.intellij.xdebugger.breakpoints.XBreakpoint;

import java.util.HashMap;

public class Breakpoint {
    private final DebugProcess myDebugProcess;
    private XBreakpoint myXBreakpoint;
    private HashMap<String, Object> myGdbBkpt; /* FIXME unroll.
        Mandatory are:
        number
        type
        catch-type
        enabled
        addr

        Optional are:
        addr_flags
        func
        file[name]
        fullname
        line
        at
        pending
        evaluated-by
        thread
        task
!        cond
        ignore
        enable
        traceframe-usage
        static-tracepoint-marker-string-id
        mask
        pass
        original-location
        times
        installed
        what
        locations (list of location)

        A location in a multi-location breakpoint is represented as a tuple with the following fields:
        number (as a dotted pair)
        enabled y n N
        addr
        addr_flags (optional)
        func (optional)
        file (optional)
        fullname (optional)
        line (optional)

                        String times = (String) bkpt.get("times");
                String originalLocation = (String) bkpt.get("original-location");
                String type_ = (String) bkpt.get("breakpoint");
                // String addr
                String disp = (String) bkpt.get("disp");
                String enabled = (String) bkpt.get("enabled");
                ArrayList<Object> locations = (ArrayList<Object>) bkpt.get("locations");

    */

    Breakpoint(DebugProcess debugProcess, XBreakpoint xBreakpoint, HashMap<String, Object> gdbResponse) {
        myDebugProcess = debugProcess;
        myXBreakpoint = xBreakpoint;
        myGdbBkpt = gdbResponse;
        if (!gdbResponse.containsKey("number")) {
            throw new RuntimeException("NO");
        }
    }

    public XBreakpoint getXBreakpoint() {
        return myXBreakpoint;
    }

    public String getNumber() {
        return (String) myGdbBkpt.get("number");
    }

    public void setEnabled(boolean b) {
        String number = getNumber();
        try {
            if (b) {
                myDebugProcess.breakEnable(number);
                myGdbBkpt.put("enabled", "y");
            } else {
                myDebugProcess.breakDisable(number);
                myGdbBkpt.put("enabled", "n");
            }
        } catch (GdbMiOperationException e) {
            myDebugProcess.reportError("could not enable/disable breakpoint in GDB", e);
        }
    }

    public void setFromGdbBkpt(HashMap<String, Object> bkpt) {
        myGdbBkpt = bkpt;
    }

//    public boolean isEnabled() {
//        return "y".equals(((String) myGdbResponse.get("enabled")));
//    }
}
