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

package org.intellij.plugins.native2Debugger;

import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * ProcessListener that manages the connection to the debugged NATIVE2-process
 */
class DebugProcessListener extends ProcessAdapter {
  private final Project myProject;
  private final int myPort;
  private final String myAccessToken;

  DebugProcessListener(Project project, int port, String accessToken) {
    myProject = project;
    myPort = port;
    myAccessToken = accessToken;
  }

  @Override
  public void startNotified(@NotNull ProcessEvent event) {
    //final DebuggerConnector connector = new DebuggerConnector(myProject, event.getProcessHandler(), myPort, myAccessToken);
    //ApplicationManager.getApplication().executeOnPooledThread(connector);
  }

  @Override
  public void processWillTerminate(@NotNull ProcessEvent event, boolean willBeDestroyed) {
//    try {
//      final Native2DebuggerSession session = Native2DebuggerSession.getInstance(event.getProcessHandler());
//      if (session != null) {
//        session.stop();
//      }
//    } catch (VMPausedException e) {
//      // VM is paused, no way for a "clean" shutdown
//    } catch (DebuggerStoppedException e) {
//      // OK
//    }
//
//    super.processWillTerminate(event, willBeDestroyed);
  }

  @Override
  public void processTerminated(@NotNull ProcessEvent event) {
//    super.processTerminated(event);
//
//    final Native2DebuggerSession session = Native2DebuggerSession.getInstance(event.getProcessHandler());
//    if (session != null) {
//      session.close();
//    }
  }
}
