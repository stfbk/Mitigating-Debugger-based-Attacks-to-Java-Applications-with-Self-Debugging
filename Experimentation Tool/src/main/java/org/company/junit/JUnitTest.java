package org.company.junit;


import org.company.asm.ASMMethod;

import java.util.ArrayList;
import java.util.Objects;

/**
 * This class is a simply POJO to collect together relevant information about tests execution metrics
 */
public class JUnitTest extends ASMMethod {

    /**
     * the superclass FQN (for JDB testing only)
     */
    private String superclassFQN;

    /**
     * the execution time of this test
     */
    private Double executionTime;

    /**
     * if this test was repeated more times, store here all the execution times
     */
    private final ArrayList<Double> executionTimes = new ArrayList<>();

    /**
     * if this test was repeated more times, this is the standard deviation
     */
    private Double standardDeviation;

    /**
     * flag for test outcome, either PASSED, FAILED or UNUSABLE
     */
    private Integer outcome;

    /**
     * flag for PASSED test outcome
     */
    public static final int PASSED = 0;

    /**
     * flag for FAILED test outcome
     */
    public static final int FAILED = 1;

    /**
     * flag for UNUSABLE test outcome
     */
    public static final int UNUSABLE = 2;

    /**
     * true if the test was executed with JUnit5, false otherwise
     */
    private boolean isJunit5;

    /**
     * Simple constructor
     * @param testName the name of the test which this class collects execution metrics
     */
    public JUnitTest(String testName) {

        super(testName);
    }

    /**
     * getter for classFQN + "." + testName
     * @return classFQN + "." + testName
     */
    public String getTestFQNName() {
        return getClassFQN() + "." + getMethodName();
    }


    /**
     * getter for executionTime
     * @return executionTime
     */
    public Double getExecutionTime() {
        return executionTime;
    }

    /**
     * setter for executionTime
     * @param executionTime executionTime
     */
    public void setExecutionTime(Double executionTime) {
        this.executionTime = executionTime;
    }

    /**
     * getter for success
     * @return success
     */
    public Integer getOutcome() {
        return outcome;
    }

    public String getOutComeAsString() {
        return getOutcome() == PASSED ? "true" : (getOutcome() == FAILED ? "false" : "java error");
    }

    /**
     * setter for success
     * @param outcome success
     */
    public void setOutcome(Integer outcome) {
        this.outcome = outcome;
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

        return
                "Test " + getClassFQN() + "." + getMethodName() + getDesc() + " was"  +
                (outcome == PASSED ? " successful" : ( outcome == FAILED ? " NOT successful" : " UNUSABLE") ) +
                ", execution time: " + executionTime + "(superclass FQN for JDB testing: " + getSuperclassFQN() + ")";
    }

    /**
     * In equals, we do not consider neither the execution time nor whether the test succeeded or not, as we
     * are mainly interested in detected parametrized tests
     * @param o the test to compare
     * @return true if the two tests share the same class FQN, method name and signature, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JUnitTest jUnitTest = (JUnitTest) o;
        return  getMethodName().equals(jUnitTest.getMethodName()) &&
                getDesc().equals(jUnitTest.getDesc()) &&
                getClassFQN().equals(jUnitTest.getClassFQN());
    }

    /**
     * calculate the hash of the class FQN, method name and signature
     * @return the hash of the class FQN, method name and signature
     */
    @Override
    public int hashCode() {
        return Objects.hash(getMethodName(), getDesc(), getClassFQN());
    }

    /**
     * getter for executionTimes
     * @return executionTimes
     */
    public ArrayList<Double> getExecutionTimes() {
        return executionTimes;
    }

    /**
     * add execution time
     * @param executionTime execution time
     */
    public void addExecutionTime(Double executionTime) {
        executionTimes.add(executionTime);
    }

    /**
     * add execution times
     * @param executionTimes execution times
     */
    public void addExecutionTimes(ArrayList<Double> executionTimes) {
        this.executionTimes.addAll(executionTimes);
    }

    /**
     * getter for standardDeviation
     * @return standardDeviation
     */
    public Double getStandardDeviation() {
        return standardDeviation;
    }

    /**
     * setter for standardDeviation
     * @param standardDeviation standardDeviation
     */
    public void setStandardDeviation(Double standardDeviation) {
        this.standardDeviation = standardDeviation;
    }

    public String getSuperclassFQN() {
        return superclassFQN;
    }

    public void setSuperclassFQN(String superclassFQN) {
        this.superclassFQN = superclassFQN;
    }
}
