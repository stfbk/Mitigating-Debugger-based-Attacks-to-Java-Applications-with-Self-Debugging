package org.company.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class ByteCodeWriter extends ClassWriter
{
    static final String OBJECT_REPRESENTATION = "java/lang/Object";
    ClassLoader classLoader;

    public ByteCodeWriter(ClassReader classReader, ClassLoader loader, int writerFlag)
    {
        super(classReader, writerFlag);
        this.classLoader = loader;
    }

    protected String getCommonSuperClass(String className1, String className2)
    {
        Class class1;
        Class class2;
        try
        {
            class1 = Class.forName(className1.replace('/', '.'), false, this.classLoader);
            class2 = Class.forName(className2.replace('/', '.'), false, this.classLoader);
        }
        catch (Exception th) {
            return "java/lang/Object";
            //throw new RuntimeException(th.getMessage());
        }

        if (class1.isAssignableFrom(class2)) {
            return className1;
        }
        if (class2.isAssignableFrom(class1)) {
            return className2;
        }

        if ((class1.isInterface()) || (class2.isInterface())) {
            return "java/lang/Object";
        }

        do {
            class1 = class1.getSuperclass();
        }
        while (!(class1.isAssignableFrom(class2)));
        return class1.getName().replace('.', '/');
    }
}