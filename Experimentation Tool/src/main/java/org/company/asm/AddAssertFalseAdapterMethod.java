package org.company.asm;

import org.objectweb.asm.MethodVisitor;


import static org.objectweb.asm.Opcodes.*;

public class AddAssertFalseAdapterMethod extends MethodVisitor {

    public AddAssertFalseAdapterMethod(Integer api, MethodVisitor mv) {
        super(api);
        this.mv = mv;
    }

    @Override
    public void visitCode() {
        visitTypeInsn(NEW, "java/lang/AssertionError");
        visitInsn(DUP);
        visitMethodInsn(INVOKESPECIAL, "java/lang/AssertionError", "<init>", "()V", false);
        visitInsn(ATHROW);
    }
}