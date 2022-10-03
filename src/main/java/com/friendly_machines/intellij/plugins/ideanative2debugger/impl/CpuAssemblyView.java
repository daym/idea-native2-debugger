package com.friendly_machines.intellij.plugins.ideanative2debugger.impl;

import com.intellij.util.ui.components.BorderLayoutPanel;
import com.intellij.xdebugger.XDebugSession;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

    public void setActive(boolean value) {

    }

    public JComponent getDefaultFocusedComponent() {
        return btnDisassemble;
    }

    public CpuAssemblyView(XDebugSession session, DebugProcess process) {
        myProcess = process;
        this.add(panel1);
        btnDisassemble.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                txtRegisters.setText("");
                try {
                    List<String> registerNames = process.dataListRegisterNames();
                    List<Map<String, Object>> registerValues = process.dataListRegisterValues("x");
                    for (Map<String, Object> entry: registerValues) {
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
                } catch (RuntimeException e3) {
                    e3.printStackTrace();
                }
                txtRegisters.revalidate();
                try {
                    // FIXME: spnCount
                    var result = process.dataDisassemble(txtBeginning.getText(), "$pc+16", GdbMiDisassemblyMode.MixedSourceAndDisassembly);
                    if (result.containsKey("asm_insns")) {
                        var asm_insns = (List<Map.Entry<String, Object>>) result.get("asm_insns");
                        for (var asm_insn : asm_insns) {
                            if ("src_and_asm_line".equals(asm_insn.getKey())) {
                                var value = (Map<String,Object>) asm_insn.getValue();
                                var line = Optional.ofNullable(value.get("line"));
                                var file = Optional.ofNullable(value.get("file"));
                                if (file.isPresent() || line.isPresent()) {
                                    txtAssembly.append(";;; ");
                                    if (file.isPresent()) {
                                        txtAssembly.append(file.get().toString());
                                    }
                                    if (line.isPresent()) {
                                        txtAssembly.append(":");
                                        txtAssembly.append(line.get().toString());
                                    }
                                }
                                // TODO: what if it's missing?
                                Object line_asm_insnObject = value.get("line_asm_insn");
                                if (line_asm_insnObject != null) {
                                    List<Map<String, Object>> line_asm_insn = (List<Map<String, Object>>) line_asm_insnObject;
                                    for (var line_asm_ins : line_asm_insn) {
                                        txtAssembly.append("\n");
                                        var address = Optional.ofNullable(line_asm_ins.get("address"));
                                        if (address.isPresent()) {
                                            txtAssembly.append(" " + address.get());
                                        }
                                        // TODO: func-name, offset
                                        var inst = Optional.ofNullable(line_asm_ins.get("inst"));
                                        if (inst.isPresent()) {
                                            txtAssembly.append(" " + inst.get());
                                        }
                                    }
                                }
                                // value: line, file, line_asm_insn
                                // asm_insn.get("src_and_asm_line");
                                // line_asm_insn
                            }
                        }
                    }
                } catch (GdbMiOperationException ex) {
                    ex.printStackTrace();
                    myProcess.reportError("Assembly error", ex);
                }
                // TODO: process.dataDisassembleFile("filename", 42, 2, true);// or that
            }
        });
    }
}
