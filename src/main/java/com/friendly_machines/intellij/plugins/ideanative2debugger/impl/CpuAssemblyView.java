package com.friendly_machines.intellij.plugins.ideanative2debugger.impl;

import com.intellij.util.ui.components.BorderLayoutPanel;
import com.intellij.xdebugger.XDebugSession;

import javax.swing.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CpuAssemblyView extends BorderLayoutPanel {
    private final DebugProcess myProcess;
    private JTextArea txtAssembly;
    private JPanel panel1;
    private JTextField txtBeginning;
    private JButton btnDisassemble;
    private JSpinner spnCount;
    private JTextArea txtRegisters;
    private JButton stepInstructionButton;

    public void setActive(boolean value) {

    }

    public JComponent getDefaultFocusedComponent() {
        return btnDisassemble;
    }

    public CpuAssemblyView(XDebugSession session, DebugProcess process) {
        myProcess = process;
        this.add(panel1);
        btnDisassemble.addActionListener(ev -> {
            txtRegisters.setText("");
            try {
                var registerNames = process.dataListRegisterNames();
                var registerValues = process.dataListRegisterValues("x");
                for (var entry: registerValues) {
                    String numberString = (String) entry.get("number");
                    Object value = entry.get("value");
                    txtRegisters.append("\n");
                    var number = Integer.parseInt(numberString);
                    var name = registerNames.get(number);
                    txtRegisters.append(name);
                    txtRegisters.append(" = ");
                    txtRegisters.append(value.toString());
                }
            } catch (GdbMiOperationException e2) {
                e2.printStackTrace();
                process.reportError("Failed reading registers", e2);
            } catch (RuntimeException | IOException e3) {
                e3.printStackTrace();
                process.reportError(e3.toString());
                return; // FIXME
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
            txtRegisters.revalidate();
            try {
                // FIXME: spnCount
                // TODO: process.dataDisassembleFile("filename", 42, 2, true);// or that
                var asm_insns = process.dataDisassemble(txtBeginning.getText(), "$pc+16", GdbMiDisassemblyMode.MixedSourceAndDisassembly).get("asm_insns");
                if (asm_insns == null) {
                    return;
                }
                for (var asm_insn : asm_insns) {
                    if ("src_and_asm_line".equals(asm_insn.getKey())) {
                        @SuppressWarnings("unchecked")
                        var value = (Map<String,Object>) asm_insn.getValue();
                        var line = Optional.ofNullable(value.get("line"));
                        var file = Optional.ofNullable(value.get("file"));
                        if (file.isPresent() || line.isPresent()) {
                            txtAssembly.append(";;; ");
                            file.ifPresent(x -> txtAssembly.append(x.toString()));
                            line.ifPresent(x -> {
                                txtAssembly.append(":");
                                txtAssembly.append(line.get().toString());
                            });
                        }
                        // TODO: what if it's missing?
                        Object line_asm_insnObject = value.get("line_asm_insn");
                        if (line_asm_insnObject != null) {
                            @SuppressWarnings("unchecked")
                            var line_asm_insn = (List<Map<String, Object>>) line_asm_insnObject;
                            for (var line_asm_ins : line_asm_insn) {
                                txtAssembly.append("\n");
                                var address = Optional.ofNullable(line_asm_ins.get("address"));
                                address.ifPresent(x -> txtAssembly.append(" " + x));
                                // TODO: func-name, offset
                                var inst = Optional.ofNullable(line_asm_ins.get("inst"));
                                inst.ifPresent(x -> txtAssembly.append(" " + x));
                            }
                        }
                        // value: line, file, line_asm_insn
                        // asm_insn.get("src_and_asm_line");
                        // line_asm_insn
                    }
                }
            } catch (GdbMiOperationException ex) {
                ex.printStackTrace();
                myProcess.reportError("Assembly error", ex);
            } catch (RuntimeException | IOException ex) {
                ex.printStackTrace();
                myProcess.reportError("Assembly error");
            } catch (InterruptedException ex) {
                // just stop
                Thread.currentThread().interrupt();
                return;
            }
        });
        stepInstructionButton.addActionListener(e -> {
            try {
                process.stepInstruction(false);
            } catch (GdbMiOperationException ex) {
                process.reportError("Failed stepping", ex);
                ex.printStackTrace();
            } catch (RuntimeException | IOException ex) {
                process.reportError(ex.toString());
                ex.printStackTrace();
            } catch (InterruptedException ex) {
                // just stop
            }
        });
    }
}
