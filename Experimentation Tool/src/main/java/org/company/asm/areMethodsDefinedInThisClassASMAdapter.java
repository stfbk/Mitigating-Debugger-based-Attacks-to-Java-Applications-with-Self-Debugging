package org.company.asm;

import org.objectweb.asm.MethodVisitor;
import java.util.ArrayList;

/**
 * This class is a visitor to search the given methods in the java class.
 * This class contains a fields with found methods after the visit ended
 */
public class areMethodsDefinedInThisClassASMAdapter extends MyClassVisitor {

    /**
     * a list containing the methods to search in this class
     */
    private final ArrayList<ASMMethod> methodsToSearch;

    /**
     * a list containing the methods that were found in this class
     */
    private final ArrayList<ASMMethod> methodsThatWereFound;

    /**
     * a list containing the methods that were not found in this class
     */
    private final ArrayList<ASMMethod> methodsThatWereNotFound;


    /**
     * Simple constructor
     * @param api the API for this visitor
     * @param methodsToSearch a list containing the methods to search in this class
     */
    public areMethodsDefinedInThisClassASMAdapter(Integer api, ArrayList<ASMMethod> methodsToSearch) {

        super(api);
        this.methodsToSearch = methodsToSearch;
        methodsThatWereFound = new ArrayList<>();
        methodsThatWereNotFound = new ArrayList<>();
    }

    /**
     * Simple constructor
     * @param api the API for this visitor
     * @param methodToSearch the method to search in this class
     */
    public areMethodsDefinedInThisClassASMAdapter(Integer api, ASMMethod methodToSearch) {

        super(api);
        this.methodsToSearch = new ArrayList<>();
        this.methodsToSearch.add(methodToSearch);
        methodsThatWereFound = new ArrayList<>();
        methodsThatWereNotFound = new ArrayList<>();
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

        for (ASMMethod currentMethod : methodsToSearch) {
            if (currentMethod.getMethodName().equals(name) && normalizeDesc(currentMethod.getDesc()).equals(normalizeDesc(desc))) {
                methodsThatWereFound.add(currentMethod);
            }
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public void visitEnd() {

        methodsThatWereNotFound.addAll(methodsToSearch);
        methodsThatWereNotFound.removeAll(methodsThatWereFound);
        super.visitEnd();
    }

    /**
     * return the list of methods that were found in this class
     * @return the list of methods that were found in this class
     */
    public ArrayList<ASMMethod> getMethodsThatWereFound() {
        return methodsThatWereFound;
    }

    /**
     * return the list of methods that were not found in this class
     * @return the list of methods that were not found in this class
     */
    public ArrayList<ASMMethod> getMethodsThatWereNotFound() {
        return methodsThatWereNotFound;
    }
}
