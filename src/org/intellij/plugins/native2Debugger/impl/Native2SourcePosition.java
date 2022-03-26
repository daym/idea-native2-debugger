package org.intellij.plugins.native2Debugger.impl;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.XSourcePositionWrapper;
import org.intellij.plugins.native2Debugger.rt.engine.Debugger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;

public class Native2SourcePosition extends XSourcePositionWrapper {
    protected Native2SourcePosition(@NotNull XSourcePosition position) {
        super(position);
    }
//  protected Native2SourcePosition(HashMap<String, Object> gdbFrame) {
//    super(position);
//  }
    //private final Debugger.Locatable myLocation;

    //frame={addr="0x00007ffff7b53857",func="amd_host_image_builder::main",args=[],file="src/main.rs",fullname="/home/dannym/src/Oxide/crates/main/amd-host-image-builder/src/main.rs",line="2469",arch="i386:x86-64"}

    @Nullable
    public static XSourcePosition createFromFrame(HashMap<String, Object> gdbFrame) {
        String file = (String) gdbFrame.get("file"); // TODO: or fullname
        VirtualFile p = VfsUtil.findFile(Path.of(file), false);
        String line = (String) gdbFrame.get("line");
        return XDebuggerUtil.getInstance().createPosition(p, Integer.parseInt(line) - 1);
    }

//  Native2SourcePosition(Debugger.Locatable location, XSourcePosition position) {
//    super(position);
//
//    myLocation = location;
//  }
//
//  @Nullable
//  public static XSourcePosition create(Debugger.Locatable location) {
//    final VirtualFile file;
//    try {
//      file = VfsUtil.findFileByURL(new URI(location.getURI()).toURL());
//    } catch (Exception e) {
//      // TODO log
//      return null;
//    }
//
//    final int line = location.getLineNumber() - 1;
//    final XSourcePosition position = XDebuggerUtil.getInstance().createPosition(file, line);
//    return line >= 0 && position != null ? new Native2SourcePosition(location, position) : null;
//  }
//
//  public Debugger.Locatable getLocation() {
//    return myLocation;
//  }
}
