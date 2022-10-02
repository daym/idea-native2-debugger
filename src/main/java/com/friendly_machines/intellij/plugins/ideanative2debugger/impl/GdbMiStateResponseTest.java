package com.friendly_machines.intellij.plugins.ideanative2debugger.impl;

import java.util.Optional;
import java.util.Scanner;

//import static org.junit.jupiter.api.Assertions.*;

class GdbMiStateResponseTest {
    @org.junit.jupiter.api.Test
    void decode() {
        var scanner = new Scanner("^done,asm_insns=[src_and_asm_line={line=\"2\",file=\"src/main.rs\",fullname=\"/home/dannym/src/Oxide/ex1/src/main.rs\",line_asm_insn=[{address=\"0x00007ffff7f9dda4\",func-name=\"_ZN3ex14main17h2bf65616aeb83c38E\",offset=\"4\",inst=\"lea    rdi,[rsp+0x8]\"},{address=\"0x00007ffff7f9dda9\",func-name=\"_ZN3ex14main17h2bf65616aeb83c38E\",offset=\"9\",inst=\"lea    rsi,[rip+0x5c4d0]        # 0x7ffff7ffa280\"},{address=\"0x00007ffff7f9ddb0\",func-name=\"_ZN3ex14main17h2bf65616aeb83c38E\",offset=\"16\",inst=\"mov    edx,0x1\"},{address=\"0x00007ffff7f9ddb5\",func-name=\"_ZN3ex14main17h2bf65616aeb83c38E\",offset=\"21\",inst=\"lea    rcx,[rip+0x492b4]        # 0x7ffff7fe7070\"},{address=\"0x00007ffff7f9ddbc\",func-name=\"_ZN3ex14main17h2bf65616aeb83c38E\",offset=\"28\",inst=\"xor    eax,eax\"},{address=\"0x00007ffff7f9ddbe\",func-name=\"_ZN3ex14main17h2bf65616aeb83c38E\",offset=\"30\",inst=\"mov    r8d,eax\"},{address=\"0x00007ffff7f9ddc1\",func-name=\"_ZN3ex14main17h2bf65616aeb83c38E\",offset=\"33\",inst=\"call   0x7ffff7f9dbf0 <_ZN4core3fmt9Arguments6new_v117h892389e75857fc7eE>\"},{address=\"0x00007ffff7f9ddc6\",func-name=\"_ZN3ex14main17h2bf65616aeb83c38E\",offset=\"38\",inst=\"lea    rdi,[rsp+0x8]\"},{address=\"0x00007ffff7f9ddcb\",func-name=\"_ZN3ex14main17h2bf65616aeb83c38E\",offset=\"43\",inst=\"call   QWORD PTR [rip+0x5ee57]        # 0x7ffff7ffcc28\"}]}]");
        scanner.useDelimiter(""); // char-by-char mode
        GdbMiStateResponse.decode(Optional.of("14"), scanner);
    }
}