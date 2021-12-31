package org.company.junit;

import org.junit.Ignore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * simple class to hold generic constant values related to JUnit
 */
public class Const {

    /**
     * the path of the junit console that is used by default if the user does not provide one himself
     */
    public static final String kJUnitConsoleDefaultPath = "lib/junit-platform-console-standalone-1.6.2.jar";

    /**
     * this is the default name of the file on which JUnit 5
     * writes the output of the tests execution
     */
    public static final String kJunit5ReportDefaultName = "TEST-junit-jupiter.xml";

    /**
     * this is the default name of the file on which JUnit 4
     * writes the output of the tests execution
     */
    public static final String kJunit4ReportDefaultName = "TEST-junit-vintage.xml";


    /**
     * this is the default number of times test are going
     * to be execution to collect accurate execution metrics
     */
    public static final int kTestsRepetitionDefaultNumber = 100;

    /**
     * this is the default number of tests on which to
     * run debugging tasks
     */
    public static final int kDefaultNumberOfTestsOnWhichToExecuteDebuggingTasks = 10;


    /**
     * this is the min number of times test are going
     * to be execution to collect accurate execution metrics
     */
    public static final int kTestsRepetitionMinValue = 1;

    /**
     * this is the max number of times test are going
     * to be execution to collect accurate execution metrics
     */
    public static final int kTestsRepetitionMaxValue = 1000;

    /**
     * if in the testcase node we have a "initializationError" name attribute, it means
     * that something went wrong while executing the test
     */
    public static final String kInitializationError = "initializationError";


    /**
     * the FQN descriptor for the @Test annotation of JUnit 5
     */
    public static final String kTestAnnotationFQN5 = "Lorg/junit/jupiter/api/Test;";

    /**
     * the FQN descriptor for the @Test annotation of JUnit 4
     */
    public static final String kTestAnnotationFQN4 = "Lorg/junit/Test;";

    /**
     * the FQN descriptor for the @Ignore annotation of JUnit
     */
    public static final String kIgnoreAnnotationFQN = "Lorg/junit/Ignore;";

    /**
     * the FQN descriptor for the @Disabled annotation of JUnit
     */
    public static final String kDisabledAnnotationFQN = "Lorg/junit/jupiter/api/Disabled;";

    /**
     * Test annotation class
     */
    public static final Class<Test> kTestAnnotation = Test.class;

    /**
     * Disabled annotation class
     */
    public static final Class<Disabled> kDisabledAnnotation = Disabled.class;

    /**
     *  Ignore annotation class
     */
    public static final Class<Ignore> kIgnoreAnnotation = Ignore.class;
}
