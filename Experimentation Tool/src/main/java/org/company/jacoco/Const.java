package org.company.jacoco;

/**
 * simple class to hold generic constant values related to JaCoCo
 */
public class Const {


    /**
     * this is the name of the file we will obtain by converting the .exec report in XML
     */
    public static final String kJacocoXMLReportDefaultName = "jacoco.xml";

    /**
     * this is the default name of the file on which JaCoCo
     * writes the output of the methods coverage analysis
     */
    public static final String kJacocoReportDefaultName = "jacoco.exec";

    /**
     * the path of the jacoco agent that is used by default if the user does not provide one himself
     */
    public static final String kJacocoAgentDefaultPath = "lib/org.jacoco.agent-0.8.5-runtime.jar";

    /**
     * the path of the jacoco cli that is used by default if the user does not provide one himself
     */
    public static final String kJacocoCliDefaultPath = "lib/org.jacoco.cli-0.8.5-nodeps.jar";

}
