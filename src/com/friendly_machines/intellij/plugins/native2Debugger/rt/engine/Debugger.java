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

import java.util.List;

public interface Debugger {
  enum State {
    CREATED, RUNNING, SUSPENDED, STOPPED
  }

  State getState();
//
//  boolean start();
//
//  void stop(boolean force);
//
//  void step();
//
//  void stepInto();
//
//  void resume();
//
//  void pause();
//
//  boolean isStopped();
//
//  //List<Variable> getGlobalVariables();
//
//  BreakpointManager getBreakpointManager();
//
//  boolean waitForDebuggee();
//
//  State waitForStateChange(State state);

}
