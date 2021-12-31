package org.company;

import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.company.asm.ASMMethod;
import org.company.asm.areMethodsDefinedInThisClassASMAdapter;
import org.company.debug.DebugUtil;
import org.company.debug.DebuggingTask;
import org.company.debug.DebuggingTaskUnit;
import org.company.jacoco.JaCoCoMethod;
import org.company.jacoco.JaCoCoUtil;
import org.company.jar.JarUtil;
import org.company.junit.JUnitClass;
import org.company.junit.JUnitClassesAndTestsBundle;
import org.company.junit.JUnitUtil;
import org.company.junit.JUnitTest;
import org.company.oblive.ObliveTransformator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.System.exit;
import static org.company.Const.*;
import static org.company.asm.Const.kAnnotatorDefaultPath;
import static org.company.cmd.Const.*;
import static org.company.cmd.cmdUtil.*;
import static org.company.debug.Const.kTasks;
import static org.company.jar.JarUtil.*;
import static org.company.junit.Const.*;
import static org.company.jacoco.Const.*;
import static org.company.junit.JUnitTest.FAILED;
import static org.company.junit.JUnitUtil.runJUnitTestsMultipleTimes;
import static org.company.oblive.Const.kObliveDefaultPath;
import static org.objectweb.asm.Opcodes.ASM8;

/**
 * The class containing the program entry point
 * Note: logs made through logback, see configuration file in classpath (target/classes/logback.xml)
 * Errors codes:
 * -  1: Wrong usage
 * -  2: Exception while IO operations
 * -  3: Given arguments are not valid
 * -  4: General exception (in inner methods, check logs)
 * -  5: Exception while waiting for shell process to finish
 * -  6: Exception while using ASM
 * -  7: Methods to protect are not of enough quality
 * -  8: Error while creating reports
 * -  9: Debugging task format not supported
 * - 10: SubProcess with non-zero exit code
 *-  11: Test cases are not of enough quality
 */
public class App {

    /**
     * The logger object through which logs are written
     */
    public static final Logger logger = LoggerFactory.getLogger(App.class);


    /**
     * The java home path.
     */
    public static String JAVA_HOME;

    /**
     * The main method takes as input two jars containing, respectively, the code to protect and the test cases. Also,
     * it takes as input an annotation as a string: this is the annotation that will be added to the method to protect
     * and that defines the protection to implement. If you want to test more kind of protections, invoke this program
     * multiple times. Then, following the defined methodology it executes the tests on the code to protect to collect
     * first coverage metrics. Then, it decides which method (yes, only one method) to annotate. Then, the main method
     * invokes Oblive on the jar with the annotated method and creates the new protected jar. The main method executes
     * functional tests on both jars (unprotected and protected) to collect execution time (and possibly other metrics
     * in the future). Finally, it wraps functional tests within the debugger tasks and executes them on both the jars
     * so to assert that the AD protection actually works against debuggers.
     * @param args run with -h to get the list of supported args
     */
    public static void main(String[] args) {

        System.out.println("" +
                " _____ _     _ _         _____         _   \n" +
                "|  _  | |   | (_)       |_   _|       | |  \n" +
                "| | | | |__ | |___   _____| | ___  ___| |_ \n" +
                "| | | | '_ \\| | \\ \\ / / _ \\ |/ _ \\/ __| __|\n" +
                "\\ \\_/ / |_) | | |\\ V /  __/ |  __/\\__ \\ |_ \n" +
                " \\___/|_.__/|_|_| \\_/ \\___\\_/\\___||___/\\__|\n" +
                "                                           \n" +
                "                                           ");


        // first thing, get the current time. This will be used to measure the execution time
        LocalDateTime initialDate = LocalDateTime.now();

        // the path of the jar containing the code to protect
        // note that, during the experimentation, the jar with the code to protect may be subject to some
        // processing. Therefore, this variable points to the processed jar that will be later protected
        final String jarToProtectPath;

        // the path of the jar, given as parameter, containing the code to protect
        String tempJarToProtectPath;

        // the path of the jar protected by Oblive (defined later in the
        // program, it depends also on the output folder given by the user)
        final String jarProtectedPath;

        // the path of the jar containing the test cases for the code to protect.
        // note that, during the experimentation, some tests (e.g., failed tests or tests that do not execute on
        // the method to protect) will be ignored. Therefore, this variable points to the jar containing only
        // the tests that will be used for the experimentation
        final String jarWithTestsPath;

        // the path of the jar, given as parameter, containing the tests
        String tempJarWithTestsPath;

        // the annotation, given as parameter as string for the annotator tool (so check the annotator tool for
        // the format), that defines the AD protection to implement
        final String annotationToApply;

        // the number of times that tests have to be repeated to collect execution metrics
        final int testsRepetitionNumber;

        // this is the default directory where the tool writes the output of the analysis (e.g. execution times)
        final String outputFolderPath;

        // the path in the file system pointing to the jar of the JUnit5 standalone console
        // the console will be used to run functional and debugging tests and collect execution time
        final String jUnitConsolePath;

        // the path in the file system pointing to the jar of the JaCoCo agent
        // the JaCoCo agent will be used to generate a report on methods coverage while executing tests
        final String jaCoCoAgentPath;

        // the path in the file system pointing to the jar of the JaCoCo CLI
        // the JaCoCo CLI will be used to convert the report produced by JaCoCo agent from .exec to .xml
        final String jaCoCoCLIPath;

        // the path in the file system pointing to the jar of the annotator
        // the annotator will be used to annotate the method to be protected by Oblive
        final String annotatorPath;

        // the path in the file system pointing to the jar of the Oblive software
        // Oblive will be used to apply AD protections and transform the jar to protect
        final String oblivePath;

        // the number of tests on which to execute the debugging tasks. To save time, we
        // do not execute all debugging tasks on all tests...
        int numberOfTestsOnWhichToExecuteDebuggingTasks;


        // ===== ===== ===== ===== PARAMETERS ACQUISITION ===== ===== ===== =====

        Options options = new Options();

        Option pathOfJarToProtectOption = new Option("j", kJarToProtectOptionKey, true,
                "Path to the .jar file containing the Java code on which the experimentation will run");
        pathOfJarToProtectOption.setRequired(true);
        options.addOption(pathOfJarToProtectOption);

        Option antiDebuggingProtectionOption = new Option("p", kAnnotationOptionKey, true,
                "The annotation, as string, that defines the AD protection to implement [one between" +
                        "antidebugtime, antidebugself, native]");
        antiDebuggingProtectionOption.setRequired(true);
        options.addOption(antiDebuggingProtectionOption);

        Option pathOfJarWithTestsOption = new Option("t", kJarTestCasesOptionKey, true,
                "Path to the .jar file containing the Java tests to run on the given jar to protect\n" +
                         "If not given, tests will be assumed to be in the jarToProtect");
        pathOfJarWithTestsOption.setRequired(false);
        options.addOption(pathOfJarWithTestsOption);

        Option pathOfJUnitConsoleOption = new Option("u", kJUnitConsoleOptionKey, true,
                "Path in the file system pointing to the jar of JUnit5 standalone console\n" +
                         "If not given, the internal .jar, inside the lib folder, will be used");
        pathOfJUnitConsoleOption.setRequired(false);
        options.addOption(pathOfJUnitConsoleOption);

        Option pathOfJaCoCoAgentOption = new Option("a", kJaCoCoAgentOptionKey, true,
                "Path in the file system pointing to the jar of the JaCoCo agent\n" +
                        "If not given, the internal .jar, inside the lib folder, will be used");
        pathOfJaCoCoAgentOption.setRequired(false);
        options.addOption(pathOfJaCoCoAgentOption);

        Option pathOfJaCoCoCLIOption = new Option("c", kJaCoCoCLIOptionKey, true,
                "Path in the file system pointing to the jar of the JaCoCo CLI\n" +
                        "If not given, the internal .jar, inside the lib folder, will be used");
        pathOfJaCoCoCLIOption.setRequired(false);
        options.addOption(pathOfJaCoCoCLIOption);

        Option pathOfAnnotatorOption = new Option("r", kAnnotatorPathOptionKey, true,
                "Path in the file system pointing to the jar of the annotator\n" +
                        "If not given, the internal .jar, inside the lib folder, will be used");
        pathOfAnnotatorOption.setRequired(false);
        options.addOption(pathOfAnnotatorOption);

        Option pathOfObliveOption = new Option("b", kOblivePathOptionKey, true,
                "Path in the file system pointing to the jar of the Oblive software\n" +
                        "If not given, the internal .jar, inside the lib folder, will be used");
        pathOfObliveOption.setRequired(false);
        options.addOption(pathOfObliveOption);

        Option pathOfJavaHome = new Option("h", kJavaHomePathOptionKey, true,
                "Path in the file system pointing to JAVA_HOME");
        pathOfObliveOption.setRequired(true);
        options.addOption(pathOfJavaHome);

        Option pathOfOutputFolderOption = new Option("o", kOutputFolderOptionKey, true,
                "Path in the file system pointing to the directory where to write output files\n" +
                         "If not given, the current working directory will be used");
        pathOfOutputFolderOption.setRequired(false);
        options.addOption(pathOfOutputFolderOption);

        Option testsRepetitionNumberOption = new Option("n", kTestsRepetitionNumberOptionKey, true,
                "How many times tests have to be repeated to collect execution metrics\n" +
                        "The number must be in the interval " + kTestsRepetitionMinValue + "-" + kTestsRepetitionMaxValue + "\n" +
                        "If not given, the default value (" + kTestsRepetitionDefaultNumber + ") will be used\n");
        testsRepetitionNumberOption.setRequired(false);
        options.addOption(testsRepetitionNumberOption);

        Option numberOfTestsOnWhichToExecuteDebuggingTasksOption = new Option(
                "z", KNumberOfTestsOnWhichToExecuteDebuggingTasksKey, true,
                "the number of tests on which to execute the debugging tasks. " +
                        "The number must be strictly positive. If there are less tests than the specified number, " +
                        "then a warning message will be issued\n" +
                        "If not given, the default value (" + kDefaultNumberOfTestsOnWhichToExecuteDebuggingTasks + ") will be used\n");
        numberOfTestsOnWhichToExecuteDebuggingTasksOption.setRequired(false);
        options.addOption(numberOfTestsOnWhichToExecuteDebuggingTasksOption);


        // check that the arguments are actually valid, i.e., the paths lead to .jar files
        try {

            CommandLine cmd = new DefaultParser().parse(options, args);

            // acquire the paths of the jars and check that they are not null, they
            // point to existing files (not directories) and that they are jar files
            tempJarToProtectPath = acquireJarPathOption (cmd, kJarToProtectOptionKey,   null);
            tempJarWithTestsPath = acquireJarPathOption (cmd, kJarTestCasesOptionKey,   tempJarToProtectPath);
            jUnitConsolePath     = acquireJarPathOption (cmd, kJUnitConsoleOptionKey, kJUnitConsoleDefaultPath);
            jaCoCoAgentPath      = acquireJarPathOption (cmd, kJaCoCoAgentOptionKey, kJacocoAgentDefaultPath);
            jaCoCoCLIPath        = acquireJarPathOption (cmd, kJaCoCoCLIOptionKey, kJacocoCliDefaultPath);
            annotatorPath        = acquireJarPathOption (cmd, kAnnotatorPathOptionKey, kAnnotatorDefaultPath);
            oblivePath           = acquireJarPathOption (cmd, kOblivePathOptionKey, kObliveDefaultPath);

            // acquire the annotation option and check that it is not null, blank or empty
            annotationToApply = acquireStringOption(cmd, kAnnotationOptionKey, null);

            // acquire the java home path
            JAVA_HOME = acquireStringOption(cmd, kJavaHomePathOptionKey, null);

            // acquire the path to the output folder. If the directory does not exist, try to create it
            outputFolderPath = acquireDirectoryPathOption(cmd, kOutputFolderOptionKey, kOutputFolderDefaultPath);

            // acquire the number of repetition of tests. If not given, use the default value
            testsRepetitionNumber = acquireIntegerOption(cmd, kTestsRepetitionNumberOptionKey,
                    kTestsRepetitionDefaultNumber, kTestsRepetitionMinValue, kTestsRepetitionMaxValue);

            // acquire the number of tests on which to execute debugging tasks. If not given, use the default value
            numberOfTestsOnWhichToExecuteDebuggingTasks = acquireIntegerOption(cmd,
                    KNumberOfTestsOnWhichToExecuteDebuggingTasksKey,
                    kDefaultNumberOfTestsOnWhichToExecuteDebuggingTasks, 1, Integer.MAX_VALUE);


            LocalDateTime timeAcquisitionParameters = LocalDateTime.now();
            logger.info("[{}{}{}{}", "App", " (" + "main" + ")]: ", "parameters acquisition completed in ",
                    getElapsedTime(initialDate, timeAcquisitionParameters));


            // ===== ===== ===== ===== STARTING ANALYSIS ===== ===== ===== =====

            String nameOfJarToProtect = getJarNameFromPath(tempJarToProtectPath);
            logger.info("[{}{}{}{}{}{} ", "App", " (" + "main" + ")]: ", "starting analysis on jar: ", nameOfJarToProtect,
                    " to apply protection: ", annotationToApply);

            // This is the structure of the folders
            //
            // <output folder>/
            // ├── <name of the jar>/
            // │   ├── 1_1_tests_correctness/                           | contains JUnit reports for checking functional correctness of tests over original jar
            // │       ├── original_jar_stripped_of_tests/              | contains the .jar of the code to protect stripped of eventual tests
            // │       ├── failed_tests_filtered_out/                   | contains .jar file containing only tests that succeed (so ignoring failed tests)
            // │   ├── 1_2_coverage/                                    | contains JaCoCo reports for coverage over original jar
            // │       ├── methods_instrumented/                        | contains .jar file containing original jar to protect with methods instrumented with 'assert false'
            // │       ├── irrelevant_tests_filtered_out/               | contains the elaboration of the tests .jar file so to ignore tests that do not execute on the method to protect
            // │   ├── 2_1_annotator/                                   | contains the jar to protect with the method annotated
            // │       ├── method_to_protect.txt                        | the method to protect (signature in ASM style)
            // |       ├── annotator_output.txt                         | the output of the annotator
            // │   ├── 2_2_oblive_protected_jar/                        | contains the jar protected by Oblive
            // │   ├── 3_1_executionMetrics_original_jar/               | contains JUnit reports for execution metrics of (filtered) tests (i.e., time) over original jar
            // │   ├── 3_2_executionMetrics_protected_jar/              | contains JUnit reports for execution metrics of (filtered) tests (i.e., time) over protected jar
            // │   ├── 4_1_debuggingTasks_original_jar/                 | contains the debugger outputs when JDB and GDB tasks are executed against the original jar
            // │   ├── 4_2_debuggingTasks_protected_jar/                | contains the debugger outputs when JDB and GDB tasks are executed against the protected jar
            // │   ├── final_reports/                                   | contains final reports summarizing the experimentation
            // │       ├── execution_metrics_report.csv                 | CSV file summarizing execution metrics of (filtered) tests (i.e., time) over original and protected jar
            // │       ├── debugging_tasks_JDB_original_report.csv      | CSV file summarizing the outcome of the JDB debugging tasks (i.e., true/false) over original jar
            // │       ├── debugging_tasks_JDB_protected_report.csv     | CSV file summarizing the outcome of the JDB debugging tasks (i.e., true/false) over protected jar
            // │       ├── debugging_tasks_GDB_original_report.csv      | CSV file summarizing the outcome of the GDB debugging tasks (i.e., true/false) over original jar
            // │       ├── debugging_tasks_GDB_protected_report.csv     | CSV file summarizing the outcome of the GDB debugging tasks (i.e., true/false) over protected jar
            // ├── <other use cases>
            // ...

            // create now the necessary folders and files to store the output of the analysis
            String analysisFolderPath           = outputFolderPath + "/" + nameOfJarToProtect + "/" ;
            File analysisOutputFolder           = new File(analysisFolderPath);

            String testsCorrectnessFolderPath   = analysisFolderPath + kTestsCorrectnessFolderName + "/";
            File testsCorrectnessFolder         = new File(testsCorrectnessFolderPath);

            String coverageFolderPath           = analysisFolderPath + kCoverageFolderName + "/";
            File coverageFolder                 = new File(coverageFolderPath);

            String assertFalseJarFolderPath     = coverageFolderPath + kMethodsInstrumentedFolderName;
            File assertFalseJarFolder           = new File(assertFalseJarFolderPath);

            String relevantTestsJarFolderPath   = coverageFolderPath + kIrrelevantTestsFilteredFolderName;
            File relevantTestsJarFolder         = new File(relevantTestsJarFolderPath);

            String ultimateTestsJarFolderPath   = coverageFolderPath + kUltimateTestsFolderName;
            File ultimateTestsJarFolder         = new File(ultimateTestsJarFolderPath);

            String annotatorFolderPath          = analysisFolderPath + kAnnotatorFolderName + "/";
            File annotatorFolder                = new File(annotatorFolderPath);

            String obliveFolderPath             = analysisFolderPath + kObliveProtectedJarFolderName + "/";
            File obliveFolder                   = new File(obliveFolderPath);

            String originalMetricsFolderPath    = analysisFolderPath + kExecutionMetricsOriginalFolderName + "/";
            File originalMetricsFolder          = new File(originalMetricsFolderPath);

            String protectedMetricsFolderPath   = analysisFolderPath + kExecutionMetricsProtectedFolderName + "/";
            File protectedMetricsFolder         = new File(protectedMetricsFolderPath);

            String originalDebugFolderPath      = analysisFolderPath + kDebuggingTasksOriginalFolderName + "/";
            File originalDebugFolder            = new File(originalDebugFolderPath);

            String protectedDebugFolderPath     = analysisFolderPath + kDebuggingTasksProtectedFolderName + "/";
            File protectedDebugFolder           = new File(protectedDebugFolderPath);

            String finalReportsFolderPath       = analysisFolderPath + kFinalReportsFolderName + "/";
            File finalReportsFolder             = new File(finalReportsFolderPath);

            String metricsFilePath              = finalReportsFolder.getAbsolutePath() + "/" + kExecutionMetricsReportFileName;
            File metricsFile                    = new File(metricsFilePath);

            String annotatorOutputFilePath      = annotatorFolderPath + "/" + kAnnotatorOutputFileName;
            File annotatorOutputFile            = new File(annotatorOutputFilePath);

            String debuggingTaskJDBOriginalReportFileName = finalReportsFolder.getAbsolutePath() + "/" + kDebuggingTaskJDBOriginalReportFileName;
            File debuggingTaskJDBOriginalReportFile       = new File(debuggingTaskJDBOriginalReportFileName);

            //String debuggingTaskGDBOriginalReportFileName = finalReportsFolder.getAbsolutePath() + "/" + kDebuggingTaskGDBOriginalReportFileName;
            //File debuggingTaskGDBOriginalReportFile       = new File(debuggingTaskGDBOriginalReportFileName);

            String debuggingTaskJDBProtectedReportFileName = finalReportsFolder.getAbsolutePath() + "/" + kDebuggingTaskJDBProtectedReportFileName;
            File debuggingTaskJDBProtectedReportFile       = new File(debuggingTaskJDBProtectedReportFileName);

            String debuggingTaskGDBProtectedReportFileName = finalReportsFolder.getAbsolutePath() + "/" + kDebuggingTaskGDBProtectedReportFileName;
            File debuggingTaskGDBProtectedReportFile       = new File(debuggingTaskGDBProtectedReportFileName);

            String methodToProtectFilePath     = annotatorFolder.getAbsolutePath() + "/" + kMethodToProtectFileName;
            File methodToProtectFile           = new File(methodToProtectFilePath);

            // in case the output folder already exists, we delete it
            if (analysisOutputFolder.exists()) {
                if (!deleteDirectoryRecursively(analysisOutputFolder)) {
                    logger.error("[{}{}{}{} ", "App", " (" + "main" + ")]: ",
                            "not able to overwrite directory ", analysisOutputFolder.getAbsolutePath());
                    exit(2);
                }
            }


            // create again the structure of folders
            if (!analysisOutputFolder.mkdirs()                               ||
                    !testsCorrectnessFolder.mkdir()                          ||
                    !coverageFolder.mkdir()                                  ||
                        !assertFalseJarFolder.mkdir()                        ||
                        !relevantTestsJarFolder.mkdir()                      ||
                    !annotatorFolder.mkdir()                                 ||
                        !methodToProtectFile.createNewFile()                 ||
                        !annotatorOutputFile.createNewFile()                 ||
                    !originalMetricsFolder.mkdir()                           ||
                    !protectedMetricsFolder.mkdir()                          ||
                    !originalDebugFolder.mkdir()                             ||
                    !protectedDebugFolder.mkdir()                            ||
                    !obliveFolder.mkdir()                                    ||
                    !finalReportsFolder.mkdir()                              ||
                        !metricsFile.createNewFile()                         ||
                        !debuggingTaskJDBOriginalReportFile.createNewFile()  ||
                        //!debuggingTaskGDBOriginalReportFile.createNewFile()  ||
                        !debuggingTaskJDBProtectedReportFile.createNewFile() ||
                        !debuggingTaskGDBProtectedReportFile.createNewFile())
                throw new IOException("Exception while creating directory or files to contain results");


            // take now a pointer to important files that we need later, so that in case
            // of any error the analysis stops immediately here and we waste no time
            InputStream debuggingTasksJDB = App.class.getClassLoader().getResourceAsStream(kDebuggingTasksJdbDefaultPath);
            InputStream debuggingTasksGDB = App.class.getClassLoader().getResourceAsStream(kDebuggingTasksGdbDefaultPath);
            assert debuggingTasksJDB != null;
            assert debuggingTasksGDB != null;


            LocalDateTime timeSetup = LocalDateTime.now();
            logger.info("[{}{}{}{}", "App", " (" + "main" + ")]: ", "setup completed in ",
                    getElapsedTime(timeAcquisitionParameters, timeSetup));


            // ===== ===== ===== ===== 1: the first step is to collect coverage execution metrics (Analysis)
            //                            we first execute tests once to check that all tests succeed       (step 1.1)
            //                            If this is not the case, we use ASM to ignore failed tests        (step 1.2)
            //                            Then, we can actually run the tests to get coverage metrics       (step 1.3)
            //                            Finally, we also ignore tests that do not execute
            //                            on method that has been chosen to be protected                    (step 1.4)

            logger.info("[{}{}{}{} ", "App", " (" + "main" + ")]: ",
                    "Running tests for verifying functional correctness on original jar: ", nameOfJarToProtect);


            // ===== ===== Step 1.1
            //             to determine whether tests succeed or not, we run them all.
            //             Through the JUnit report, we collect the name of failed tests
            //             and store them in an hash map (the variable named "jUnitTestsCorrectnessFailed")

            new TestExecutor(tempJarToProtectPath, tempJarWithTestsPath, jUnitConsolePath, null)
                    .runTests(testsCorrectnessFolder);

            // jUnitTestsCorrectness contains successful tests, failed tests and also classes errors
            JUnitClassesAndTestsBundle junitCorrectness =
                    JUnitUtil.parseFromXML(
                            new File(testsCorrectnessFolderPath + kJunit4ReportDefaultName),
                            new File(testsCorrectnessFolderPath + kJunit5ReportDefaultName));
            ArrayList<JUnitTest> jUnitTestsCorrectness = junitCorrectness.getJunitTests();
            ArrayList<JUnitClass> jUnitClassCorrectness = junitCorrectness.getJUnitClasses();

            int initialNumberOfTests = jUnitTestsCorrectness.size();

            logger.info("[{}{}{}{} ", "App", " (" + "main" + ")]: ", "initial number of tests: ", initialNumberOfTests);

            if (initialNumberOfTests == 0) {
                logger.error("[{}{}{} ", "App", " (" + "main" + ")]: ", "there are no tests! Exiting...");
                exit(11);
            }

            // Just to be sure, we strip the jar with the code to protect of any JUnit test
            logger.info("[{}{}{} ", "App", " (" + "main" + ")]: ", "stripping the jar to protect of eventual JUnit tests");

            // an hash map containing, for each class (key), the array (object) of JUnit tests
            HashMap<String, ArrayList<JUnitTest>> jUnitTests =
                    JUnitUtil.getTestsByClass(jUnitTestsCorrectness);

            // this removes eventual tests from the jar to protect. Note that this new
            // jar file will be used for the rest of the execution instead of the original jar file
            jarToProtectPath = createJarWithIgnoredTests(
                    jUnitTests,
                    jUnitClassCorrectness,
                    tempJarToProtectPath,
                    testsCorrectnessFolderPath + kTestsFilteredOutFolderName,
                    nameOfJarToProtect + "_" +  kTestsFilteredOutFolderName,
                    false,
                    new HashSet<>()).getAbsolutePath();



            // ===== ===== Step 1.2
            //             if at least a test failed, we know we have to process
            //             the jar containing the tests to remove failed tests

            // an hash map containing, for each class (key), the array (object) of failed JUnit tests
            ArrayList<JUnitTest> jUnitTestsCorrectnessFailed =
                    JUnitUtil.getFailedTests(jUnitTestsCorrectness);

            int initialNumberOfFailedTests = jUnitTestsCorrectnessFailed.size();
            int initialNumberOfSuccessfulTests = initialNumberOfTests-initialNumberOfFailedTests;
            logger.info("[{}{}{}{}{}{}{} ", "App", " (" + "main" + ")]: ", "initial number of failed tests: ",
                    initialNumberOfFailedTests, ", so there remain ", initialNumberOfSuccessfulTests, " tests");

            if (initialNumberOfFailedTests != 0) {

                if (initialNumberOfSuccessfulTests == 0) {
                    logger.error("[{}{}{} ", "App", " (" + "main" + ")]: ", "all tests failed! Exiting...");
                    exit(11);
                }
                else {

                    if (jUnitTestsCorrectnessFailed.size() > 0) {
                        logger.info("[{}{}{}{} ", "App", " (" + "main" + ")]: ", "the following tests",
                                " failed (one per line in the next logs) and will be ignored");
                        jUnitTestsCorrectnessFailed.forEach(junitTest -> logger.info("    {}", junitTest));
                    }
                    else
                        logger.info("[{}{}{} ", "App", " (" + "main" + ")]: ", "no tests failed");

                    if (jUnitClassCorrectness.size() > 0) {
                        logger.info("[{}{}{}{} ", "App", " (" + "main" + ")]: ", "the following classes",
                                " give errors (one per line in the next logs) and will be ignored");
                        jUnitClassCorrectness.forEach(junitClass -> logger.info("    {}", junitClass));
                    }
                    else
                        logger.info("[{}{}{} ", "App", " (" + "main" + ")]: ", "no classes will be ignored");

                    // this makes the jar with tests path be now the jar with tests but without the failed tests.
                    tempJarWithTestsPath = createJarWithIgnoredTests(
                            JUnitUtil.getTestsByClass(jUnitTestsCorrectnessFailed),
                            jUnitClassCorrectness,
                            tempJarWithTestsPath,
                            testsCorrectnessFolderPath + kFailedTestsFilteredOutFolderName,
                            nameOfJarToProtect + "_" + kFailedTestsFilteredOutFolderName,
                            true,
                            new HashSet<>()).getAbsolutePath();
                }
            }
            else {
                logger.info("[{}{}{} ", "App", " (" + "main" + ")]: ", "all tests executed successfully");
            }

            LocalDateTime timeStep11 = LocalDateTime.now();
            logger.info("[{}{}{}{}", "App", " (" + "main" + ")]: ", "filtered out failed tests in ",
                    getElapsedTime(timeSetup, timeStep11));



            // ===== ===== Step 1.3
            //             after having filtered out failed tests, we can execute them to get coverage metrics

            logger.info("[{}{}{}{} ", "App", " (" + "main" + ")]: ",
                    "collecting coverage metrics for jar: ", nameOfJarToProtect);

            // run the tests through the TestExecutor class with coverage (JaCoCo)
            // note that the execution of JUnit tests with coverage will create a
            // 'jacoco.exec' file in the folder given as arguments when running tests
            new TestExecutor(jarToProtectPath, tempJarWithTestsPath, jUnitConsolePath, null)
                    .withCoverage(jaCoCoAgentPath)
                    .runTests(coverageFolder);

            String coverageReportXML = coverageFolderPath + kJacocoXMLReportDefaultName;

            // convert the .exec JaCoCo coverage file in .xml and parse it to extract coverage information
            JaCoCoUtil.convertJaCoCoExecToXML(coverageFolder, coverageFolderPath + kJacocoReportDefaultName,
                    coverageReportXML, jaCoCoCLIPath, jarToProtectPath);
            ArrayList<JaCoCoMethod> methodsAndCoverage = JaCoCoUtil.parseFromXML(new File(coverageReportXML));

            // now we have an array with all methods and the related missed and covered instructions and branches
            // we sort the methods based on their score. The top method will be protected with Oblive.
            // Check the calculateScore method to see how the score is calculated
            methodsAndCoverage.sort(Comparator.comparing(JaCoCoMethod::calculateScore).reversed());
            JaCoCoMethod methodToProtect = methodsAndCoverage.get(0);

            // if the score of the method to protect is 0, it means that
            // there are not enough quality methods and we should abort
            if (methodToProtect.calculateScore() <= 0) {

                logger.error("[{}{}{}", "App", " (" + "main" + ")]: ", "the method to protect does not match the score criteria");

                exit(7);
            }
            else
                logger.info("[{}{}{}{} ", "App", " (" + "main" + ")]: ", "the following method was chosen to be protected: ",
                        methodToProtect);

            LocalDateTime timeStep13 = LocalDateTime.now();
            logger.info("[{}{}{}{}", "App", " (" + "main" + ")]: ", "execution metrics and coverage collected in ",
                    getElapsedTime(timeStep11, timeStep13));



            // ===== ===== Step 1.4
            //             now we modify the jar to protect to instrument the method chosen to be protected. Then
            //             we run the tests again and exclude tests that do NOT execute on the instrumented method
            //             the reason is that, in the rest of the program, we want to base our metrics on
            //             relevant tests only

            String jarToProtectPathWithAssertFalse = createJarWithAssertFalseAnnotation(
                    methodToProtect,
                    jarToProtectPath,
                    assertFalseJarFolderPath,
                    nameOfJarToProtect + "_with_assert_false").getAbsolutePath();

            // we now run the tests again to understand which tests execute on the method to protect so to remove the
            // other tests from the experimentation
            new TestExecutor(jarToProtectPathWithAssertFalse, tempJarWithTestsPath, jUnitConsolePath, null)
                    .runTests(relevantTestsJarFolder);

            JUnitClassesAndTestsBundle jUnitOnInstrumentedMethod = JUnitUtil.parseFromXML(
                            new File(relevantTestsJarFolderPath + "/" + kJunit4ReportDefaultName),
                            new File(relevantTestsJarFolderPath + "/" + kJunit5ReportDefaultName));

            ArrayList<JUnitTest> jUnitTestsOnInstrumentedMethod = jUnitOnInstrumentedMethod.getJunitTests();
            ArrayList<JUnitClass> jUnitClassesOnInstrumentedMethod = jUnitOnInstrumentedMethod.getJUnitClasses();

            // an hash map containing, for each class (key), the array (object) of successful JUnit tests
            // note that a 'successful' test mean that the test was NOT executed on the method to protect
            // therefore, we now remove such tests
            ArrayList<JUnitTest> jUnitTestsOnInstrumentedMethodSuccessful =
                    JUnitUtil.getSuccessfulTests(jUnitTestsOnInstrumentedMethod);

            // we keep the tests that failed when executed with the instrumented ('assert_false')
            // jar to protect, since these tests are the one that execute on the method to protect
            jUnitTestsOnInstrumentedMethod.removeIf(jUnitTest -> jUnitTest.getOutcome() != FAILED);

            int numberOfConsideredTests = jUnitTestsOnInstrumentedMethod.size();
            logger.info("[{}{}{}{}{}{}{}{}{}{} ", "App", " (" + "main" + ")]: ", "number of tests that execute on the method ",
                    "to protect (and that will therefore be used for experimentation): ", numberOfConsideredTests,
                    " over ", initialNumberOfSuccessfulTests, " successful tests over ", initialNumberOfTests, " initial tests (one per line below)");
            jUnitTestsOnInstrumentedMethod.forEach(junitTest -> logger.info("    {}", junitTest));


            if (!jUnitTestsOnInstrumentedMethodSuccessful.isEmpty()) {

                if (numberOfConsideredTests == 0) {
                    logger.error("[{}{}{}{} ", "App", " (" + "main" + ")]: ", "no test executes on the method to protect. ",
                            "This probably means that tests fail because of a java errors (ASM?). Check JUnit reports." +
                            " Exiting...");
                    exit(11);
                }

                    logger.info("[{}{}{}{} ", "App", " (" + "main" + ")]: ", "the following tests",
                            " (one per line in the next logs) do not execute the method to protect and will be removed");
                    jUnitTestsOnInstrumentedMethodSuccessful.forEach(junitTest -> logger.info("    {}", junitTest));

                    // this makes the jar with tests path be now the jar without irrelevant tests. Note that this new
                    // jar file will be used for the rest of the execution instead of the original jar file
                    jarWithTestsPath = createJarWithIgnoredTests(
                            JUnitUtil.getTestsByClass(jUnitTestsOnInstrumentedMethodSuccessful),
                            jUnitClassesOnInstrumentedMethod,
                            tempJarWithTestsPath,
                            relevantTestsJarFolderPath,
                            nameOfJarToProtect + "_irrelevant_test_filtered",
                            true,
                            JUnitUtil.getTestsByClass(jUnitTestsOnInstrumentedMethod).keySet()).getAbsolutePath();
            }
            // if (weird) all tests execute on the method to protect, just keep the old jar
            else {
                jarWithTestsPath = tempJarWithTestsPath;
            }


            LocalDateTime timeStep14 = LocalDateTime.now();
            logger.info("[{}{}{}{}", "App", " (" + "main" + ")]: ", "filtered out irrelevant tests in ",
                    getElapsedTime(timeStep13, timeStep14));











            // ===== ===== ===== ===== 2: the second step is to annotate the method to protect
            //                            and transform the code with Oblive (Annotation and Transformation)
            //                            first, we get the method to protect and save them in a file       (step 2.1)
            //                            then, we invoke the annotator tool to add the annotations         (step 2.2)
            //                            finally, we invoke Oblive to transform the annotated method to add
            //                            the AD protection and create the protected jar                   (step 2.3)


            // ===== ===== Step 2.1
            //             save the signature of the method to annotate in a file that will be fed to the annotator
            FileOutputStream outputStreamForMethodToProtectFile = new FileOutputStream(methodToProtectFile);
            outputStreamForMethodToProtectFile.write(methodToProtect.getMethodForASM().getBytes());
            outputStreamForMethodToProtectFile.flush();
            outputStreamForMethodToProtectFile.close();

            // ===== ===== Step 2.2
            //             invoke the annotator tool to add annotations to the method to protect
            logger.info("[{}{}{} ", "App", " (" + "main" + ")]: ", "starting the annotation of the method to protect");
            JarUtil.extractAllFilesFromJar(annotatorFolder, jarToProtectPath);
            ProcessBuilder pbAnnotator = new ProcessBuilder("java", "-jar", annotatorPath,
                    annotatorFolder.getAbsolutePath(), methodToProtectFile.getAbsolutePath(), annotationToApply);

            pbAnnotator.directory(annotatorFolder).redirectErrorStream(true).redirectOutput(annotatorOutputFile);

            Process pAnnotator = pbAnnotator.start();
            pAnnotator.waitFor();

            // check that the code is 0, otherwise there was an error
            int exitStatus = pAnnotator.exitValue();
            if (exitStatus != 0) {
                App.logger.error("[{}{}{}{}", "App ", "(" + "main" + ")]: ",
                        "Annotator process exited with code: ", exitStatus);
                exit(10);
            }
            else
                App.logger.info("[{}{}{}", "App ", "(" + "main" + ")]: ",
                        "Annotator process was successful");



            String annotatedJarToProtectPath = annotatorFolder.getAbsolutePath() + "/" + nameOfJarToProtect + "_annotated.jar";
            JarUtil.createJar(annotatorFolder, null, annotatedJarToProtectPath);



            // ===== ===== Step 2.3
            //             invoke Oblive to transform the annotated method to add
            //             the AD protection and create the protected jar

            // the path of the jar produced by Oblive, containing the protected code
            jarProtectedPath = obliveFolder.getAbsolutePath() + "/" + nameOfJarToProtect + "_" + annotationToApply + ".jar";

            ObliveTransformator obliveTransformator = new ObliveTransformator(oblivePath);
            obliveTransformator.applyADProtections(obliveFolder, annotatedJarToProtectPath,
                    jarProtectedPath, "oblive_" + annotationToApply);


            LocalDateTime timeStep2 = LocalDateTime.now();
            logger.info("[{}{}{}{}", "App", " (" + "main" + ")]: ", "Oblive applied protections to jar in ",
                    getElapsedTime(timeStep14, timeStep2));





            // ===== ===== ===== ===== 3: the third step is to ensure functional correctness of both original and
            //                            protected jar and collect execution metrics (Functional Correctness)
            //                            first, we run the tests on the original jar                        (step 3.1)
            //                            second, we run the tests on the protected jar                      (step 3.2)
            //                            finally, we merge the results in a CSV file                        (step 3.3)

            // note: we have to run tests multiple times to have accurate execution metrics
            // JUnit5 has interesting annotation, "@RepeatedTest(X)" to repeat a test X number of times
            // Unfortunately, JUnit4< tests do not have it. Therefore, the simplest (and maybe only) solution
            // to preserve the compatibility with JUnit4< is to invoke the JUnit console multiple times
            // and then aggregate the results




            // ===== ===== Step 3.1
            //             run the tests on the original jar
            logger.info("[{}{}{}{} ", "App", " (" + "main" + ")]: ",
                    "Running tests for collecting metrics (execution time) on original jar: ", nameOfJarToProtect);
            TestExecutor testExecutorMetricsOriginalJar =
                    new TestExecutor(jarToProtectPath, jarWithTestsPath, jUnitConsolePath, null);
            ArrayList<JUnitTest> executionMetricsOnOriginalJar = runJUnitTestsMultipleTimes(
                    testsRepetitionNumber, originalMetricsFolder, testExecutorMetricsOriginalJar);





            // ===== ===== Step 3.2
            //             run the tests on the protected jar
            logger.info("[{}{}{}{} ", "App", " (" + "main" + ")]: ",
                    "Running tests for collecting metrics (execution time) on protected jar: ", nameOfJarToProtect);

            TestExecutor testExecutorMetricsProtectedJar =
                    new TestExecutor(jarProtectedPath, jarWithTestsPath, jUnitConsolePath, obliveFolderPath);
            ArrayList<JUnitTest> executionMetricsOnProtectedJar = runJUnitTestsMultipleTimes(
                    testsRepetitionNumber, protectedMetricsFolder, testExecutorMetricsProtectedJar);




            // ===== ===== Step 3.3
            //             merge the results in a CSV file
            //             below you find an example of the structure of the file
            //
            // test_name | original_jar_average_execution_time | original_jar_standard_deviation |  original_jar_is_successful |
            // __________|_____________________________________|_________________________________|_____________________________|
            // testName1 |                1.57                 |                2.29             |            true             |
            // testName2 |                4.85                 |                9.46             |            true             |
            // testName3 |                2.75                 |                5.99             |            true             |
            // ...
            //
            // I (Stefano) chose CSV because easier to handle with R to plot the results, but of course we can change if you want

            logger.info("[{}{}{}{} ", "App", " (" + "main" + ")]: ",
                    "Writing CSV report for metrics (execution time): ", nameOfJarToProtect);

            FileOutputStream outputStreamForExecutionMetricsReport = new FileOutputStream(metricsFile);
            StringBuilder executionMetricsCSV = new StringBuilder();
            executionMetricsCSV.append("test_name," +
                    "original_jar_average_execution_time,original_jar_standard_deviation,original_jar_is_successful," +
                    "protected_jar_average_execution_time,protected_jar_standard_deviation,protected_jar_is_successful," +
                    "\n");
            outputStreamForExecutionMetricsReport.write(executionMetricsCSV.toString().getBytes());

            for (int i = 0; i < executionMetricsOnOriginalJar.size(); i++) {

                JUnitTest testOnOriginalJar = executionMetricsOnOriginalJar.get(i);
                String testNameOriginalJar = testOnOriginalJar.getTestFQNName();

                JUnitTest testOnProtectedJar = executionMetricsOnProtectedJar.get(i);
                String testNameProtectedJar = testOnProtectedJar.getTestFQNName();

                if (!testNameOriginalJar.equals(testNameProtectedJar)) {

                    logger.error("[{}{}{}{}{}{}{} ", "App", " (" + "main" + ")]: ",
                            "mismatch in the name of the tests, (test on original jar: ",
                            testNameOriginalJar, ", test on protected jar: ", testNameProtectedJar, ")");

                    exit(8);
                } else {
                    executionMetricsCSV = new StringBuilder();
                    executionMetricsCSV.append(testNameProtectedJar).append(",")
                            .append(testOnOriginalJar.getExecutionTime()).append(",")
                            .append(testOnOriginalJar.getStandardDeviation()).append(",")
                            .append(testOnOriginalJar.getOutComeAsString()).append(",")
                            .append(testOnProtectedJar.getExecutionTime()).append(",")
                            .append(testOnProtectedJar.getStandardDeviation()).append(",")
                            .append(testOnProtectedJar.getOutComeAsString()).append("\n");
                    outputStreamForExecutionMetricsReport.write(executionMetricsCSV.toString().getBytes());
                }
            }
            outputStreamForExecutionMetricsReport.flush();
            outputStreamForExecutionMetricsReport.close();


            LocalDateTime timeStep3 = LocalDateTime.now();
            logger.info("[{}{}{}{}", "App", " (" + "main" + ")]: ", "functional correctness after having applied " +
                            " protections completed in ", getElapsedTime(timeStep2, timeStep3));


            // ===== ===== ===== ===== 4: the fourth step is to ensure the effectiveness of the applied protections by
            //                            running debugging tasks on original and protected jars (Protections Correctness)
            //                            first, we read and parse the debugging tasks from the JSON files    (step 4.1)
            //                            then, we setup stream to write on log files and some utils          (step 4.2)
            //                            finally, for each test, we run all debugging tasks on original
            //                            and protected jars                                                  (step 4.3)

            // ===== ===== Step 4.1
            //             we read and parse the JDB and GDB debugging tasks from the JSON files

            StringBuilder debuggingTasksJDBHeaderCSV = new StringBuilder().append("test_name, ");
            StringBuilder debuggingTasksGDBHeaderCSV = new StringBuilder().append("test_name, ");

            // convert JDB tasks in a JSON object
            JSONObject debuggingTasksJDBJSON = new JSONObject(new String(IOUtils.toByteArray(debuggingTasksJDB)));
            JSONArray debuggingTasksJDBJSONArray = debuggingTasksJDBJSON.getJSONArray(kTasks);

            // convert GDB tasks in a JSON object
            JSONObject debuggingTasksGDBJSON = new JSONObject(new String(IOUtils.toByteArray(debuggingTasksGDB)));
            JSONArray debuggingTasksGDBJSONArray = debuggingTasksGDBJSON.getJSONArray(kTasks);

            // we now convert the JDB and GDB debugging tasks from JSON objects to Java objects
            // the array list will contain the debugging tasks converted from
            // JSON and parametrized with the method to protect
            ArrayList<DebuggingTask> parametrizedDebuggingTasksJDB = new ArrayList<>();
            debuggingTasksJDBJSONArray.forEach(debuggingTaskJDBJSON -> {

                // get the JSON object corresponding to a debugging task. The object contains the name of the
                // debugging task and the debugging task units. Note that the input and output of the debugging
                // tasks may be parametrized with respect to the method to protect
                JSONObject currentDebuggingTaskJSON = (JSONObject) debuggingTaskJDBJSON;
                DebuggingTask parametrizedDebuggingTask = new DebuggingTask(currentDebuggingTaskJSON, methodToProtect);
                parametrizedDebuggingTasksJDB.add(parametrizedDebuggingTask);
                debuggingTasksJDBHeaderCSV.append(parametrizedDebuggingTask.getDebuggingTaskName()).append(", ");
            });

            ArrayList<DebuggingTask> parametrizedDebuggingTasksGDB = new ArrayList<>();
            debuggingTasksGDBJSONArray.forEach(debuggingTaskGDBJSON -> {

                JSONObject currentDebuggingTaskJSON = (JSONObject) debuggingTaskGDBJSON;
                DebuggingTask parametrizedDebuggingTask = new DebuggingTask(currentDebuggingTaskJSON, methodToProtect);

                // NOTE: because the JVM uses the SEGV signal internally for a few different things (NullPointerException,
                // safepoints, ...) we have to ignore it. Therefore, the first debugging task unit is to supply to the debugger
                // the command 'handle SIGSEGV pass noprint nostop' to tell gdb to let the application handle the SEGV signal
                // https://stackoverflow.com/questions/44533670/jdk9-hotspot-debug-using-gdb-causing-sigsegv-segmentation-fault-in-eclipse-ubun
                // http://mail.openjdk.java.net/pipermail/hotspot-dev/2007-October/000179.html
                DebuggingTaskUnit noSIGSEGV = new DebuggingTaskUnit(
                        "handle SIGSEGV pass noprint nostop",
                        "SIGSEGV",
                        null);
                parametrizedDebuggingTask.addDebuggingTaskUnit(0, noSIGSEGV);



                parametrizedDebuggingTasksGDB.add(parametrizedDebuggingTask);
                debuggingTasksGDBHeaderCSV.append(parametrizedDebuggingTask.getDebuggingTaskName()).append(", ");
            });




            // ===== ===== Step 4.2
            //             we setup stream to write on log files and some utils
            //             first, we create output stream for the 4 log files (JDB original, JDB protected,
            //             GDB original, GDB protected), then we write the header of the CSV files, that is
            //             the list of the debugging tasks. Note that debugging tasks may differ from JDB and GDB.
            //             Then, we create the debugging utils to run the debugging tasks

            // we collected the name of all JDB debugging tasks (that compose the header of the report files)
            FileOutputStream osForDebuggingTasksJDBOriginalReport  = new FileOutputStream(debuggingTaskJDBOriginalReportFile);
            FileOutputStream osForDebuggingTasksJDBProtectedReport = new FileOutputStream(debuggingTaskJDBProtectedReportFile);
            debuggingTasksJDBHeaderCSV.append("\n");
            osForDebuggingTasksJDBOriginalReport.write(debuggingTasksJDBHeaderCSV.toString().getBytes());
            osForDebuggingTasksJDBProtectedReport.write(debuggingTasksJDBHeaderCSV.toString().getBytes());

            //FileOutputStream osForDebuggingTasksGDBOriginalReport  = new FileOutputStream(debuggingTaskGDBOriginalReportFile);
            FileOutputStream osForDebuggingTasksGDBProtectedReport = new FileOutputStream(debuggingTaskGDBProtectedReportFile);
            debuggingTasksGDBHeaderCSV.append("\n");
            //osForDebuggingTasksGDBOriginalReport.write(debuggingTasksGDBHeaderCSV.toString().getBytes());
            osForDebuggingTasksGDBProtectedReport.write(debuggingTasksGDBHeaderCSV.toString().getBytes());


            // instantiate the debug util that will run the debugging tasks. Note that we pass two different jars
            // as the second argument, i.e., the original and the protected jar
            DebugUtil debugUtilTestOriginalJarJDB =  new DebugUtil(jUnitConsolePath, jarToProtectPath, jarWithTestsPath, null);
            DebugUtil debugUtilTestProtectedJarJDB = new DebugUtil(jUnitConsolePath, jarProtectedPath, jarWithTestsPath, obliveFolderPath);

            //DebugUtil debugUtilTestOriginalJarGDB =  new DebugUtil(jUnitConsolePath, jarToProtectPath, jarWithTestsPath, null);
            DebugUtil debugUtilTestProtectedJarGDB = new DebugUtil(jUnitConsolePath, jarProtectedPath, jarWithTestsPath, obliveFolderPath);



            // ===== ===== Step 4.3
            //             we limit the analysis to 10 tests. For each test, we run all debugging tasks on
            //             original and protected jars and then save the output in 3 CSV files
            //             (original/protected JDB + protected GDB).
            //             Below, you find an example of the output file
            // test_name | debuggingTask1 | debuggingTask2 | debuggingTask2 | ...
            // __________|________________|________________|________________|
            // testName1 |      true      |      true      |      true      |
            // testName2 |      true      |      true      |      true      |
            // testName3 |      true      |      true      |      true      |


            // TODO START OF EXPERIMENTAL PART
            // The tests in "jUnitTestsOnInstrumentedMethod" may have been defined in a test superclass, not in the
            // test class they seem to belong to. Therefore, we need to ensure that the tests are actually there, or
            // otherwise look for their superclass.


            logger.info("[{}{}{}{} ", "App", " (" + "main" + ")]: ", "checking that tests are defined ",
                "in the class they should be defined.");

            /*ArrayList<JUnitTest> jUnitTestsOnInstrumentedMethodDEEPCOPY = new ArrayList<>();
            for (JUnitTest test: jUnitTestsOnInstrumentedMethod) {
                JUnitTest clonedTest = new JUnitTest(test.getMethodName());
                clonedTest.setExecutionTime(test.getExecutionTime());
                clonedTest.setClassFQN(test.getClassFQN());
                clonedTest.setJunit5(test.isJunit5());
                clonedTest.setOutcome(test.getOutcome());
                clonedTest.setDesc(test.getDesc());
                clonedTest.setStandardDeviation(clonedTest.getStandardDeviation());
                jUnitTestsOnInstrumentedMethodDEEPCOPY.add(clonedTest);
            }*/

            HashMap<String, ArrayList<JUnitTest>> jUnitTestsOnInstrumentedMethodByClass =
                    JUnitUtil.getTestsByClass(jUnitTestsOnInstrumentedMethod);

            App.logger.info("EXPERIMENTAL: below, the tests before the experimentation: ");
            jUnitTestsOnInstrumentedMethod.forEach(test -> App.logger.info("    {}", test));

            App.logger.info("EXPERIMENTAL: below, the classes FQNs of the tests: ");
            jUnitTestsOnInstrumentedMethodByClass.keySet().forEach(fqn -> App.logger.info("    {}", fqn));

            // extract all files from the Jar in the folder
            extractAllFilesFromJar(ultimateTestsJarFolder, jarWithTestsPath);
            HashSet<File> classFiles = new HashSet<>();
            getAllFilesFromDirectoryWithExtension(ultimateTestsJarFolder, "class", classFiles);

            boolean doWeHaveMoreTestsToProcess = true;

            while (doWeHaveMoreTestsToProcess) {

                doWeHaveMoreTestsToProcess = false;

                for (File classFile : classFiles) {

                    String classFQN = classFile.getAbsolutePath()
                            .replace(ultimateTestsJarFolderPath + "/", "")
                            .replace("/", ".")
                            .replace(".class", "");

                    App.logger.info("EXPERIMENTAL: analyzing class " + classFQN);

                    if (jUnitTestsOnInstrumentedMethodByClass.containsKey(classFQN)) {

                        App.logger.info("EXPERIMENTAL: some tests are referred to class" + classFQN);

                        FileInputStream classFileInputStream = new FileInputStream(classFile);

                        ArrayList<JUnitTest> arrayOfTests = jUnitTestsOnInstrumentedMethodByClass.get(classFQN);

                        // check which tests are defined in the class are which are defined in a superclass
                        areMethodsDefinedInThisClassASMAdapter classVisitor =
                                new areMethodsDefinedInThisClassASMAdapter(ASM8, JUnitUtil.toASMMethods(arrayOfTests));
                        ClassReader classReader = new ClassReader(classFileInputStream);
                        classReader.accept(classVisitor, 0);
                        classFileInputStream.close();

                        ArrayList<ASMMethod> testsThatWereNotFound = classVisitor.getMethodsThatWereNotFound();

                        if (testsThatWereNotFound.size() != 0) {

                            String superClassName = classVisitor.getSuperClassName().replace("/", ".");

                            if (!superClassName.equals("java.lang.Object")) {

                                App.logger.info("[{}{}{}{}{}{}{}", "MAIN", " (" + "EXPERIMENTAL" + ")]: ",
                                        "some tests were not found (one per line below), moving them from class ", classFQN,
                                        " to superclass ", superClassName, ": ");
                                testsThatWereNotFound.forEach(asmMethod -> App.logger.info("    {}", asmMethod.toString()));

                                // note that the superclass may not be present as key in the "testsToIgnore" map,
                                jUnitTestsOnInstrumentedMethodByClass.putIfAbsent(superClassName, new ArrayList<>());
                                ArrayList<JUnitTest> superclassMethods = jUnitTestsOnInstrumentedMethodByClass.get(superClassName);

                                // then, we add the methods that were not found to the superclass, if not already there
                                for (ASMMethod method : testsThatWereNotFound) {

                                    JUnitTest test = (JUnitTest) method;
                                    int indexOf = superclassMethods.indexOf(test);
                                    if (indexOf < 0) {

                                        // this is the step in which we actually remove the test from the class that use
                                        // it and assign the test to the class that defines it
                                        if (!arrayOfTests.remove(test)) {
                                            App.logger.error("[{}{}{}", "MAIN", " (" + "EXPERIMENTAL" + ")]: ",
                                                    "method were not found to remove, this (should be) impossible");
                                            exit(6);
                                        }
                                        test.setSuperclassFQN(superClassName);
                                        superclassMethods.add(test);
                                    }
                                }

                                doWeHaveMoreTestsToProcess = true;
                            }
                            else {
                                App.logger.error("[{}{}{}{}{}{}", "MAIN", " (" + "EXPERIMENTAL" + ")]: ",
                                        "some methods were not found (one per line below), ",
                                        "requireAllMethodsToBeProcessed flag is true but the superclass of class ",
                                        classFQN, " is \"java.lang.Object\"");
                                testsThatWereNotFound.forEach(method -> App.logger.error("    {}", method));
                                exit(6);
                            }
                        }
                        else {
                            App.logger.info("[{}{}{}{}{}", "MAIN", " (" + "EXPERIMENTAL" + ")]: ",
                                    "all tests in class ", classFQN, " where found ");
                        }
                    }
                    else {
                        App.logger.info("EXPERIMENTAL: no tests are referred to class " + classFQN);
                    }
                }
            }

            App.logger.info("EXPERIMENTAL: below, the tests after the experimentation: ");
            jUnitTestsOnInstrumentedMethod.forEach(test -> App.logger.info("    {}", test));

            App.logger.info("EXPERIMENTAL: the NEW classes FQNs of the tests: ");
            jUnitTestsOnInstrumentedMethodByClass.keySet().forEach(fqn -> App.logger.info("    {}", fqn));



            // TODO END OF EXPERIMENTAL PART






            List<JUnitTest> jUnitTestsToRunDebuggingTasks = jUnitTestsOnInstrumentedMethod
                    .stream()
                    .limit(numberOfTestsOnWhichToExecuteDebuggingTasks)
                    .collect(Collectors.toList());

            // for each method, we will execute ALL debugging tasks on the original and the protected jar
            jUnitTestsToRunDebuggingTasks.forEach(jUnitTest -> {

                String classFQN = jUnitTest.getClassFQN();
                String classSuperFQN = jUnitTest.getSuperclassFQN() == null ? classFQN : jUnitTest.getSuperclassFQN();
                String testName = jUnitTest.getMethodName();

                // this is for the report: the first string is the name of the test
                final StringBuilder debuggingTasksJDBOriginalCSV  =
                        new StringBuilder().append(classFQN).append(".").append(testName).append(", ");
                final StringBuilder debuggingTasksJDBProtectedCSV =
                        new StringBuilder().append(classFQN).append(".").append(testName).append(", ");
                //final StringBuilder debuggingTasksGDBOriginalCSV  =
                //        new StringBuilder().append(classFQN).append(".").append(testName).append(", ");
                final StringBuilder debuggingTasksGDBProtectedCSV =
                        new StringBuilder().append(classFQN).append(".").append(testName).append(", ");



                // now, we execute each debugging task twice, on the original and on the protected jar
                // we expect the tasks to succeed on the original jar and fail on the protected jar
                parametrizedDebuggingTasksJDB.forEach(currentTask -> {

                    logger.info("[{}{}{}{}{}{}{}{} ", "App", " (" + "main" + ")]: ", "executing JDB debugging task: ",
                            currentTask.getDebuggingTaskName(), " with test: ", classFQN, ".", testName);

                    boolean executionOutcomeOnOriginalJar = debugUtilTestOriginalJarJDB.executeJDBDebugTask(
                            originalDebugFolder, null, currentTask, classFQN, testName, classSuperFQN);
                    debuggingTasksJDBOriginalCSV.append(executionOutcomeOnOriginalJar).append(", ");
                    currentTask.reset();

                    boolean executionOutcomeOnProtectedJar = debugUtilTestProtectedJarJDB.executeJDBDebugTask(
                            protectedDebugFolder, null, currentTask, classFQN, testName, classSuperFQN);
                    currentTask.reset();
                    debuggingTasksJDBProtectedCSV.append(executionOutcomeOnProtectedJar).append(", ");
                });

                // try to write down the results on the report files
                // we have to catch here because we are in a lambda
                try {
                    debuggingTasksJDBOriginalCSV.append("\n");
                    debuggingTasksJDBProtectedCSV.append("\n");
                    osForDebuggingTasksJDBOriginalReport.write(debuggingTasksJDBOriginalCSV.toString().getBytes());
                    osForDebuggingTasksJDBProtectedReport.write(debuggingTasksJDBProtectedCSV.toString().getBytes());
                }
                catch (IOException e) {

                    logger.error("[{}{}{}{} ", "App", " (" + "main" + ")]: ", "Exception while writing CSV report" +
                            "for JDB debugging tasks: ", e);
                    exit(2);
                }


                parametrizedDebuggingTasksGDB.forEach(currentTask -> {

                    logger.info("[{}{}{}{}{}{}{}{} ", "App", " (" + "main" + ")]: ", "executing GDB debugging task: ",
                            currentTask.getDebuggingTaskName(), " with test: ", classFQN, ".", testName);

                    //boolean executionOutcomeOnOriginalJar = debugUtilTestOriginalJarGDB.executeGDBDebugTask(
                    //        originalDebugFolder, null, currentTask, classFQN, testName);
                    //debuggingTasksGDBOriginalCSV.append(executionOutcomeOnOriginalJar).append(", ");
                    //currentTask.reset();

                    boolean executionOutcomeOnProtectedJar = debugUtilTestProtectedJarGDB.executeGDBDebugTask(
                            protectedDebugFolder, null, currentTask, classFQN, testName);
                    currentTask.reset();
                    debuggingTasksGDBProtectedCSV.append(executionOutcomeOnProtectedJar).append(", ");
                });

                try {
                    //debuggingTasksGDBOriginalCSV.append("\n");
                    debuggingTasksGDBProtectedCSV.append("\n");
                    //osForDebuggingTasksGDBOriginalReport.write(debuggingTasksGDBOriginalCSV.toString().getBytes());
                    osForDebuggingTasksGDBProtectedReport.write(debuggingTasksGDBProtectedCSV.toString().getBytes());
                }
                catch (IOException e) {

                    logger.error("[{}{}{}{} ", "App", " (" + "main" + ")]: ", "Exception while writing CSV report" +
                            "for GDB debugging tasks: ", e);
                    exit(2);
                }
            });

            osForDebuggingTasksJDBProtectedReport.flush();
            osForDebuggingTasksJDBProtectedReport.close();
            osForDebuggingTasksJDBOriginalReport.flush();
            osForDebuggingTasksJDBOriginalReport.close();

            osForDebuggingTasksGDBProtectedReport.flush();
            osForDebuggingTasksGDBProtectedReport.close();
            //osForDebuggingTasksGDBOriginalReport.flush();
            //osForDebuggingTasksGDBOriginalReport.close();


            LocalDateTime timeStep4 = LocalDateTime.now();
            logger.info("[{}{}{}{}", "App", " (" + "main" + ")]: ", "debugging tasks executed in ",
                    getElapsedTime(timeStep3, timeStep4));

            logger.info("[{}{}{}{}", "App", " (" + "main" + ")]: ", "whole analysis completed in ",
                    getElapsedTime(initialDate, timeStep4));

        }
        // exception thrown for wrong usage
        catch (ParseException e) {

            logger.error("[{}{}{}{} ", "App", " (" + "main" + ")]: ", "Wrong usage or arguments: ", e);

            String header = "\nThis tool automatize the experimentation phase for the AD protections application\n\n";
            String footer = "\nPlease report issues to sberlato@fbk.eu";
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Experimentation Tool", header, options, footer, true);

            exit(1);
        }
        // if the user provided wrong parameters
        catch (IllegalArgumentException e ) {

            logger.error("[{}{}{}{} ", "App", " (" + "main" + ")]: ", "Given arguments are not valid: ", e);

            exit(3);
        }
        // if IO operations (e.g., creation of files) went wrong
        catch (IOException e) {

            logger.error("[{}{}{}{} ", "App", " (" + "main" + ")]: ", "Exception while creating file: ", e);
            exit(2);
        }
        // general handler (if here, it means that something went wrong in inner methods)
        catch (Exception e) {

            logger.error("[{}{}{}{} ", "App", " (" + "main" + ")]: ", "General exception: ", e);
            e.printStackTrace();
            exit(4);
        }
    }


    /**
     * print a human readable interval between two local date time
     * @param firstDate the first local date time
     * @param secondDate the second local date time (should be after the first)
     * @return a string representing a human readable interval between two local date time
     */
    private static String getElapsedTime(LocalDateTime firstDate, LocalDateTime secondDate) {
        Duration now = Duration.between(firstDate, secondDate);
        return now.toDays() + " days, " +
                now.toHours() + " hours, " +
                now.toMinutes() + " minutes" +
                " (to string) " + now.toString();
    }


    /**
     * this method recursively deletes a directory and all its content
     * @param currentJarOutputFolder entry point
     * @return true if operation was successful
     */
    private static boolean deleteDirectoryRecursively(File currentJarOutputFolder) {

        File[] allContents = currentJarOutputFolder.listFiles();
        if (allContents != null) {
            for (File file : allContents)
                deleteDirectoryRecursively(file);
        }
        return currentJarOutputFolder.delete();
    }




}
