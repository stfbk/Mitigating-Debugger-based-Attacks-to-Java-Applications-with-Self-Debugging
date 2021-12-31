package org.company.asm;

import org.company.App;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import static java.lang.System.exit;


/**
 * This class is a wrapper class visitor to keep the value of the class and superclass name
 */
public class MyClassVisitor extends ClassVisitor {

    /**
     * the name of the class this class visitor is parsing
     */
    private String className;

    /**
     * the name of the superclass of the class this class visitor is parsing
     */
    private String superClassName;

    /**
     * true if, after the visit, the whole class should be deleted from the jar
     */
    private boolean isClassToBeDeleted = false;

    /**
     * true if, after the visit, the whole class should be ignored (i.e., added @Ignore annotation)
     */
    private boolean isClassToBeIgnored = false;

    /**
     * Simple constructor
     * @param api the API for this visitor
     * @param cv the class writer object that will output the modified class
     *                    (i.e., failed tests without the @Test annotation)
     */
    public MyClassVisitor(Integer api, ClassWriter cv) {

        super(api, cv);
    }

    /**
     * Simple constructor
     * @param api the API for this visitor
     */
    public MyClassVisitor(Integer api) {

        super(api);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        className = name;
        superClassName = superName;
    }

    /**
     * getter for className
     * @return className
     */
    public String getClassName() {
        return className;
    }

    /**
     * getter for superClassName
     * @return superClassName
     */
    public String getSuperClassName() {
        return superClassName;
    }

    /**
     * strips the signature of a method of the prefix and returns the class name only. The method also handles
     * primitive types and arrays
     * example: "(Ljava/lang/String;Ljava/util/TimeZone;)Lorg/apache/commons/lang3/time/DatePrinter;" becomes "(String, TimeZone)"
     * example: "(ILjava/lang/String;[I)J;" becomes "(int, String, int[])"
     * @param originalDesc the original desc. Can even be an already normalized desc
     * @return the normalized desc
     */
    public static String normalizeDesc(String originalDesc) {

        // if true, it means that we have a NOT normalized desc
        if (!originalDesc.endsWith(")")) {

            // e.g., "(ILjava/lang/String;[I)J;" becomes "(int, String, int[])"
            StringBuilder normalizedDesc = new StringBuilder();

            StringBuilder ifArray = new StringBuilder();

            // remove the "(" at the beginning
            char firstLetter = originalDesc.charAt(0);
            originalDesc = originalDesc.substring(1);

            while (firstLetter != ')') {

                switch (firstLetter) {
                    case '(':
                        normalizedDesc.append("(");
                        break;
                    case '[':
                        ifArray.append("[]");
                        originalDesc = originalDesc.substring(1);
                        break;
                    case 'Z':
                        normalizedDesc.append("boolean").append(ifArray).append(", ");
                        ifArray = new StringBuilder();
                        originalDesc = originalDesc.substring(1);
                        break;
                    case 'B':
                        normalizedDesc.append("byte").append(ifArray).append(", ");
                        ifArray = new StringBuilder();
                        originalDesc = originalDesc.substring(1);
                        break;
                    case 'C':
                        normalizedDesc.append("char").append(ifArray).append(", ");
                        ifArray = new StringBuilder();
                        originalDesc = originalDesc.substring(1);
                        break;
                    case 'S':
                        normalizedDesc.append("short").append(ifArray).append(", ");
                        ifArray = new StringBuilder();
                        originalDesc = originalDesc.substring(1);
                        break;
                    case 'I':
                        normalizedDesc.append("int").append(ifArray).append(", ");
                        ifArray = new StringBuilder();
                        originalDesc = originalDesc.substring(1);
                        break;
                    case 'J':
                        normalizedDesc.append("long").append(ifArray).append(", ");
                        ifArray = new StringBuilder();
                        originalDesc = originalDesc.substring(1);
                        break;
                    case 'F':
                        normalizedDesc.append("float").append(ifArray).append(", ");
                        ifArray = new StringBuilder();
                        originalDesc = originalDesc.substring(1);
                        break;
                    case 'D':
                        normalizedDesc.append("double").append(ifArray).append(", ");
                        ifArray = new StringBuilder();
                        originalDesc = originalDesc.substring(1);
                        break;
                    case 'L':
                        int indexOfSemiColon = originalDesc.indexOf(";");
                        int indexOfLastSlash = (originalDesc.substring(0, indexOfSemiColon)).lastIndexOf("/");
                        normalizedDesc.append(originalDesc, indexOfLastSlash + 1, indexOfSemiColon).append(ifArray).append(", ");
                        ifArray = new StringBuilder();
                        originalDesc = originalDesc.substring(indexOfSemiColon + 1);
                        break;
                    default:
                        App.logger.error("[{}{}{}{}", "MyClassVisitor", " (" + "normalizeDesc" + ")]: ",
                                "character not supported: ", firstLetter);
                        exit(6);
                }

                firstLetter = originalDesc.charAt(0);
            }
            if (normalizedDesc.length() != 1)
                normalizedDesc.delete(normalizedDesc.length() - 2, normalizedDesc.length());
            normalizedDesc.append(")");

            return normalizedDesc.toString();
        }
        else {

            // to remove the returning type
            return originalDesc.substring(0, originalDesc.indexOf(")")+1);
        }
    }

    /**
     * getter for classToBeDeleted
     * @return classToBeDeleted
     */
    public boolean isClassToBeDeleted() {
        return isClassToBeDeleted;
    }

    /**
     * setter for classToBeDeleted
     * @param classToBeDeleted classToBeDeleted
     */
    void setClassToBeDeleted(boolean classToBeDeleted) {
        this.isClassToBeDeleted = classToBeDeleted;
    }

    /**
     * getter for isClassToBeIgnored
     * @return isClassToBeIgnored
     */
    public boolean isClassToBeIgnored() {
        return isClassToBeIgnored;
    }


    /**
     * setter for isClassToBeIgnored
     * @param isClassToBeIgnored isClassToBeIgnored
     */
    void setClassToBeIgnored(boolean isClassToBeIgnored) {
        this.isClassToBeIgnored = isClassToBeIgnored;
    }
}
