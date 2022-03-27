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

package com.friendly_machines.intellij.plugins.native2Debugger.rt.engine;

import com.intellij.xdebugger.breakpoints.XBreakpoint;

class BreakpointImpl implements Breakpoint {
  private final String myUri;
  private final int myLine;
  private boolean myEnabled;
  private XBreakpoint myXBreakpoint;

  public void setEnabled(boolean value) {
    myEnabled = value;
  }

  @Override
  public boolean isEnabled() {
    return myEnabled;
  }

  BreakpointImpl(XBreakpoint xBreakpoint, String uri, int line) {
    myUri = uri;
    myLine = line;
    myEnabled = true;
    myXBreakpoint = xBreakpoint;
  }

  @Override
  public XBreakpoint getXBreakpoint() {
    return myXBreakpoint;
  }
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final BreakpointImpl that = (BreakpointImpl)o;

    if (myLine != that.myLine) return false;
    return myUri.equals(that.myUri);
  }

  public int hashCode() {
    int result;
    result = myUri.hashCode();
    result = 31 * result + myLine;
    return result;
  }


  public String toString() {
    return "Breakpoint{" +
           "myUri='" + myUri + '\'' +
           ", myLine=" + myLine +
           ", myEnabled=" + myEnabled +
           '}';
  }
}
