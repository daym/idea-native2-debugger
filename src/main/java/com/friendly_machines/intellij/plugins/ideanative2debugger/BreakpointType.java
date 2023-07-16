// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.
package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.breakpoints.XLineBreakpointType;
import com.intellij.xdebugger.breakpoints.ui.XBreakpointCustomPropertiesPanel;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BreakpointType extends XLineBreakpointType<BreakpointProperties> {
    private final EditorsProvider myMyEditorsProvider1 = new EditorsProvider();

    public BreakpointType() {
        super("native2-breakpoint", DebuggerBundle.message("title.native2.breakpoints"));
        // TODO: Bundle.message("debugger.line.breakpoint.type"), new PyDebuggerEditorsProvider()
    }

    @Override
    public boolean canPutAt(@NotNull VirtualFile file, int line, @NotNull Project project) {
        final Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null) return false;

        final PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
        if (psiFile == null) {
            return false;
        }
        final FileType fileType = psiFile.getFileType();
        String fileTypeName = fileType.getName();
        String defaultExtension = psiFile.getFileType().getDefaultExtension();
        // TODO: Objective-C
        if ("Rust".equalsIgnoreCase(fileTypeName) || "C++".equalsIgnoreCase(fileTypeName)) {
            return true;
        } else if ("d".equalsIgnoreCase(defaultExtension) || "go".equalsIgnoreCase(defaultExtension) || "pas".equalsIgnoreCase(defaultExtension) || "pp".equalsIgnoreCase(defaultExtension) || "inc".equalsIgnoreCase(defaultExtension) || "f90".equalsIgnoreCase(defaultExtension) || "f95".equalsIgnoreCase(defaultExtension) || "f03".equalsIgnoreCase(defaultExtension) || "f".equalsIgnoreCase(defaultExtension) || "adb".equalsIgnoreCase(defaultExtension) || "ads".equalsIgnoreCase(defaultExtension) || "mod".equalsIgnoreCase(defaultExtension) || "def".equalsIgnoreCase(defaultExtension) || "cl".equalsIgnoreCase(defaultExtension) || "clcpp".equalsIgnoreCase(defaultExtension)) {
            return true;
        } else if ("groovy".equalsIgnoreCase(fileTypeName) || "kotlin".equalsIgnoreCase(fileTypeName) || "java".equalsIgnoreCase(fileTypeName)) {
            return false;
        } else {
            return false;
        }
    }

    @Override
    public XDebuggerEditorsProvider getEditorsProvider(@NotNull XLineBreakpoint<BreakpointProperties> breakpoint, @NotNull Project project) {
        final XSourcePosition position = breakpoint.getSourcePosition();
        if (position == null) {
            return null;
        }

        final PsiFile file = PsiManager.getInstance(project).findFile(position.getFile());
        if (file == null) {
            return null;
        }

        return myMyEditorsProvider1;
    }

    @Override
    public BreakpointProperties createBreakpointProperties(@NotNull VirtualFile file, int line) {
        return new BreakpointProperties();
    }

    @Override
    public @Nullable BreakpointProperties createProperties() {
        return new BreakpointProperties();
    }

    @Override
    public @Nullable XBreakpointCustomPropertiesPanel<XLineBreakpoint<BreakpointProperties>> createCustomPropertiesPanel(@NotNull Project project) {
        return new BreakpointPropertiesPanel();
    }
}
