package org.company.jacoco;

import org.company.asm.ASMMethod;

/**
 * This class is a simply POJO to collect together relevant information about tests methods coverage
 */
public class JaCoCoMethod extends ASMMethod {

    /**
     * the number of instructions missed by tests
     */
    private Integer instructionsMissed;

    /**
     * the number of instructions covered by tests
     */
    private Integer instructionsCovered;

    /**
     * the number of branches missed by tests
     */
    private Integer branchesMissed;

    /**
     * the number of branches covered by tests
     */
    private Integer branchesCovered;


    /**
     * Simple constructor, initializes values to 0 (if a method does not have branches, JaCoCo won't even set
     * the tag in the XML report. Therefore, if we do not initialize to 0, we will have null values)
     * @param methodName the name of the method on which this class collects coverage information
     */
    public JaCoCoMethod(String methodName) {

        super(methodName);
        instructionsMissed = 0;
        instructionsCovered = 0;
        branchesMissed = 0;
        branchesCovered = 0;
    }







    /**
     * getter for instructionsMissed
     * @return instructionsMissed
     */
    public Integer getInstructionsMissed() {
        return instructionsMissed;
    }

    /**
     * setter for instructionsMissed
     * @param instructionsMissed the number of instructions missed by tests
     */
    public void setInstructionsMissed(Integer instructionsMissed) {
        this.instructionsMissed = instructionsMissed;
    }

    /**
     * getter for instructionsCovered
     * @return instructionsCovered
     */
    public Integer getInstructionsCovered() {
        return instructionsCovered;
    }

    /**
     * setter for instructionsCovered
     * @param instructionsCovered the number of instructions covered by tests
     */
    public void setInstructionsCovered(Integer instructionsCovered) {
        this.instructionsCovered = instructionsCovered;
    }

    /**
     * getter for branchesMissed
     * @return branchesMissed
     */
    public Integer getBranchesMissed() {
        return branchesMissed;
    }

    /**
     * setter for branchesMissed
     * @param branchesMissed the number of branches missed by tests
     */
    public void setBranchesMissed(Integer branchesMissed) {
        this.branchesMissed = branchesMissed;
    }

    /**
     * getter for branchesCovered
     * @return branchesCovered
     */
    public Integer getBranchesCovered() {
        return branchesCovered;
    }

    /**
     * setter for branchesCovered
     * @param branchesCovered the number of branches covered by tests
     */
    public void setBranchesCovered(Integer branchesCovered) {
        this.branchesCovered = branchesCovered;
    }



    /**
     * This methods returns a score on this JaCoCoMethod instance based on the instructions/branches covered/missed
     * The actual score is: exclude class (<init>) and static (<clinit>) constructors and native methods.
     * IMPORTANT NOTE: JACOCO DO NOT REPORT COVERAGE ON NATIVE METHODS. BETTER FOR US, BUT LOOKOUT IN CASE OF UPDATES!
     * Then a method should at least have 70% branch coverage for getting a positive score. Then, we rank by number
     * of instructions covered
     * @return a score indicating how much this method is covered by tests
     */
    public Integer calculateScore () {

        // if the method is a constructor or coverage of branches is not at least 70%
        if (    getMethodName().equalsIgnoreCase("<init>") ||
                getMethodName().equalsIgnoreCase("<clinit>") ||
                ((double) branchesCovered / (branchesCovered + branchesMissed) < 0.7)) {

            return 0;
        }
        else
            return instructionsCovered;
    }

    /**
     * Override the toString method to return the info about this instance
     * @return a string representing this instance
     */
    @Override
    public String toString () {

        return "Method " + getClassFQN() + "." + getMethodName() + getDesc() +
                ", instructions covered " + instructionsCovered + " and missed " + instructionsMissed +
                ", branches covered " + branchesCovered + " and missed " + branchesMissed +
                ", score: " + calculateScore();
    }
}
