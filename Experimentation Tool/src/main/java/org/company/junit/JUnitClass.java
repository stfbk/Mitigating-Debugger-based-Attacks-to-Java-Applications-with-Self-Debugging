package org.company.junit;

import java.util.Objects;

/**
 * This class is a simply POJO to collect together relevant information about a test class
 */
public class JUnitClass {

    /**
     * the fully qualified name of this class (ASM style, so '.' to separate names and not '/')
     */
    private final String classFQN;

    /**
     * Simple constructor
     * @param classFQN the fully qualified name of this class
     */
    public JUnitClass(String classFQN) {
        this.classFQN = classFQN;
    }

    /**
     * true if the test was executed with JUnit5, false otherwise
     */
    private boolean isJunit5;

    /**
     * getter for the class FQN
     * @return the class FQN
     */
    public String getClassFQN() {
        return classFQN;
    }


    /**
     * getter for isJunit5
     * @return isJunit5
     */
    public boolean isJunit5() {
        return isJunit5;
    }

    /**
     * setter for isJunit5
     * @param junit5 isJunit5
     */
    public void setJunit5(boolean junit5) {
        isJunit5 = junit5;
    }

    /**
     * Override the toString method to return the info about this instance
     * @return a string representing this instance
     */
    @Override
    public String toString () {
        return "Class " + getClassFQN();
    }

    /**
     * In equals, we consider the FQN of the class only
     * @param o the class to compare
     * @return true if the two classes share the same FQN, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JUnitClass jUnitClass = (JUnitClass) o;
        return getClassFQN().equals(jUnitClass.getClassFQN());
    }

    /**
     * calculate the hash of the class FQN
     * @return the hash of the class FQN
     */
    @Override
    public int hashCode() {
        return Objects.hash(getClassFQN());
    }
}
