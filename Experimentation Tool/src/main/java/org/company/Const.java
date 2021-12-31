package org.company;

/**
 * simple class to hold generic constant values
 */
public class Const {

    /**
     * the path of the output folder path that is used by default if the user does not provide one himself
     */
    public static final String kOutputFolderDefaultPath = "./experimentation_tool_output";

    /**
     * folder to contain the output of JUnit tests functional correctness (part of phase 1)
     */
    public static final String kTestsCorrectnessFolderName = "1_1_tests_correctness";

    /**
     * folder to contain the output of JaCoCo methods coverage (part of phase 1)
     */
    public static final String kCoverageFolderName = "1_2_coverage";

    /**
     * folder to (eventually) contain the jar file with the code to protect stripped of eventual tests
     */
    public static final String kTestsFilteredOutFolderName = "original_jar_stripped_of_tests";

    /**
     * folder to (eventually) contain the tests jar file containing only tests that succeed
     */
    public static final String kFailedTestsFilteredOutFolderName = "failed_tests_filtered_out";

    /**
     * folder to (eventually) contain the elaborated jar file containing only tests that execute the methods to protect
     */
    public static final String kIrrelevantTestsFilteredFolderName = "irrelevant_tests_filtered_out";

    /**
     * folder to contain further computation on tests
     */
    public static final String kUltimateTestsFolderName = "ultimate_tests";

    /**
     * folder to contain the jar file to protect in which methods to protect are instrumented with 'assert false'
     * FYI, this is to be able to run again tests and filter out tests that do not execute the methods to protect
     */
    public static final String kMethodsInstrumentedFolderName = "methods_instrumented";

    /**
     * folder to contain the jar file to protect in which methods to protect are annotated
     * FYI, this is to allow Oblive to actually transform such methods
     */
    public static final String kAnnotatorFolderName = "2_1_annotator";

    /**
     * folder to contain whatever Oblive outputs, included the protected jar file
     */
    public static final String kObliveProtectedJarFolderName = "2_2_oblive_protected_jar";

    /**
     * folder to contain the results of JUnit execution metrics on the original jar
     */
    public static final String kExecutionMetricsOriginalFolderName = "3_1_executionMetrics_original_jar";

    /**
     * folder to contain the results of JUnit execution metrics on the protected jar
     */
    public static final String kExecutionMetricsProtectedFolderName = "3_2_executionMetrics_protected_jar";

    /**
     * folder to contain the results of the execution of the debugging tasks on the original jar
     */
    public static final String kDebuggingTasksOriginalFolderName = "4_1_debuggingTasks_original_jar";

    /**
     * folder to contain the results of the execution of the debugging tasks on the protected jar
     */
    public static final String kDebuggingTasksProtectedFolderName = "4_2_debuggingTasks_protected_jar";

    /**
     * folder to contain the final reports of everything
     */
    public static final String kFinalReportsFolderName = "final_reports";

    /**
     * the name of the file that will contain the full signature (classFQN.methodName.desc,
     * ASM style) of the method to protect with Oblive
     */
    public static final String kMethodToProtectFileName = "method_to_protect.txt";

    /**
     * the name of the file that will contain the output of the annotator tool
     */
    public static final String kAnnotatorOutputFileName = "annotatorOutput.txt";

    /**
     * this is the name of the file that will contain the results of the tests run for the execution metrics
     * (i.e., for each test, the execution time on original and protected jars)
     */
    public static final String kExecutionMetricsReportFileName = "execution_metrics_report.csv";

    /**
     * this is the name of the file that will contain the results of the JDB debugging tasks on the original jar
     * (i.e., for each test, whether the debugging task succeeded or not)
     */
    public static final String kDebuggingTaskJDBOriginalReportFileName = "debugging_tasks_JDB_original_report.csv";

    /**
     * this is the name of the file that will contain the results of the GDB debugging tasks on the original jar
     * (i.e., for each test, whether the debugging task succeeded or not)
     */
    public static final String kDebuggingTaskGDBOriginalReportFileName = "debugging_tasks_GDB_original_report.csv";

    /**
     * this is the name of the file that will contain the results of the jDB debugging tasks on the protected jar
     * (i.e., for each test, whether the debugging task succeeded or not)
     */
    public static final String kDebuggingTaskJDBProtectedReportFileName = "debugging_tasks_JDB_protected_report.csv";

    /**
     * this is the name of the file that will contain the results of the GDB debugging tasks on the protected jar
     * (i.e., for each test, whether the debugging task succeeded or not)
     */
    public static final String kDebuggingTaskGDBProtectedReportFileName = "debugging_tasks_GDB_protected_report.csv";



    /**
     * the JSON file in the resources folder that contain the JDB debugging tasks
     */
    public static final String kDebuggingTasksJdbDefaultPath = "debuggingTasks/debuggingTasksJDB.json";

    /**
     * the JSON file in the resources folder that contain the GDB debugging tasks
     */
    public static final String kDebuggingTasksGdbDefaultPath = "debuggingTasks/debuggingTasksGDB.json";

}
