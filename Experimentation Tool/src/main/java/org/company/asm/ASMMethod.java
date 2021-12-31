package org.company.asm;

/**
 * an object to represent a generic Java method
 */
public class ASMMethod {

    /**
     * the fully qualified name of the class (ASM style, so '.' to separate names and not '/')
     */
    private String classFQN;

    /**
     * the name of this test
     */
    private final String methodName;

    /**
     * the signature of the method (ASM style)
     * e.g., '([Ljava/lang/String;)V'
     */
    private String desc;


    /**
     * Simple constructor
     * @param methodName the name of this method
     */
    public ASMMethod(String methodName) {
        this.methodName = methodName;
    }


    /**
     * getter for classFQN
     * @return classFQN
     */
    public String getClassFQN() {
        return classFQN;
    }

    /**
     * setter for classFQN (e.g., "org.company.TriangleTest")
     * @param classFQN the class fully qualified name
     */
    public void setClassFQN(String classFQN) {
        this.classFQN = classFQN;
    }

    /**
     * getter for methodName
     * @return methodName
     */
    public String getMethodName() {
        return methodName;
    }

    // no setter for method name, that is done in the constructor

    /**
     * getter for desc
     * @return the signature of the method (ASM style)
     */
    public String getDesc() {
        return desc;
    }


    /**
     * setter for desc
     * @param desc the signature of the method (ASM style)
     */
    public void setDesc(String desc) {
        this.desc = desc;
    }


    /**
     * getter for desc without returning type
     * @return the signature of the method (ASM style) without returning style (i.e., only parameters)
     */
    public String getDescWithoutReturningType() {
        return getDesc().substring(0, getDesc().lastIndexOf(")")+1);
    }


    /**
     * return the classFQN (with '.' and not '/' as separator), the method name and the desc parameters (asm style)
     * e.g., package.name.here.ClassName.methodName(arguments)
     * e.g., org.company.Triangle.calculatePerimeter([Ljava/lang/String;)
     * @return the classFQN (with '.' and not '/' as separator), the method name and the desc parameters (asm style)
     */
    public String getMethodForASM() {
        return getClassFQNForASM() + "." + getMethodName() + getDescWithoutReturningType();
    }


    /**
     * return the classFQN (with '.' and not '/' as separator) (asm style)
     * e.g., package.name.here.ClassName
     * e.g., org.company.Triangle.calculatePerimeter
     * @return the classFQN (with '.' and not '/' as separator) (asm style)
     */
    public String getClassFQNForASM() {
        return getClassFQN().replace('/','.');
    }

    /**
     * Override the toString method to return the info about this instance
     * @return a string representing this instance
     */
    @Override
    public String toString () {
        return  "Method " + getClassFQN() + "." + getMethodName() + getDesc();
    }
}
