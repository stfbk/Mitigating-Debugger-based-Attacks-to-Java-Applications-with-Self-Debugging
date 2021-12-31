package org.company.asm;

import org.company.App;
import org.company.jacoco.JaCoCoMethod;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.Arrays;

import static java.lang.System.exit;

/**
 * This class is a visitor with the purpose of adding an 'assert false' statement in the given list
 * of methods
 */
public class AddAssertFalseAdapterClass extends MyClassVisitor {

    /**
     * the methods that must be modified with the 'assert false' statement
     */
    JaCoCoMethod methodAssertFalse;


    /**
     * Simple constructor
     * @param api the API for this visitor
     * @param cv the class writer object that will output the modified class
     *                    (i.e., methods with the 'assert false' annotation)
     * @param methodAssertFalse the method to modify
     */
    public AddAssertFalseAdapterClass(Integer api, ClassWriter cv, JaCoCoMethod methodAssertFalse) {
        super(api, cv);
        this.methodAssertFalse = methodAssertFalse;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

        // note that this also checks the overload of methods
        if (methodAssertFalse.getMethodName().equals(name) && methodAssertFalse.getDesc().equals(desc)) {

            App.logger.info("[{}{}{}{}{}{}{} ", "AddAssertFalseAdapterClass", " (" + "visitMethod" + ")]: ",
                    "adding false assertion to method: ", getClassName().replace("/","."), ".", name, desc);

            return new AddAssertFalseAdapterMethod(api, cv.visitMethod(access, name, desc, signature, exceptions));
        }
        else
            return cv.visitMethod(access, name, desc, signature, exceptions);
    }
}
