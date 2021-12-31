package org.company.junit;

import java.util.ArrayList;

/**
 * This class is a simply POJO to collect together classes and tests of a test suite
 */
public class JUnitClassesAndTestsBundle {

    private ArrayList<JUnitTest> junitTests;
    private ArrayList<JUnitClass> jUnitClasses;

    /**
     * Simple constructor
     * @param junitTests junitTests
     * @param jUnitClasses jUnitClasses
     */
    public JUnitClassesAndTestsBundle (ArrayList<JUnitTest> junitTests, ArrayList<JUnitClass> jUnitClasses) {
        this.jUnitClasses = jUnitClasses;
        this.junitTests = junitTests;
    }

    /**
     * getter for junitTests
     * @return junitTests
     */
    public ArrayList<JUnitTest> getJunitTests() {
        return junitTests;
    }

    /**
     * setter for junitTests
     * @param junitTests junitTests
     */
    public void setJunitTests(ArrayList<JUnitTest> junitTests) {
        this.junitTests = junitTests;
    }

    /**
     * getter for jUnitClasses
     * @return jUnitClasses
     */
    public ArrayList<JUnitClass> getJUnitClasses() {
        return jUnitClasses;
    }

    /**
     * setter for jUnitClasses
     * @param jUnitClasses jUnitClasses
     */
    public void setJUnitClasses(ArrayList<JUnitClass> jUnitClasses) {
        this.jUnitClasses = jUnitClasses;
    }
}
