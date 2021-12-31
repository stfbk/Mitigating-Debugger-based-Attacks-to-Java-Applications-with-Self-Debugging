package org.company;


import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

import static java.lang.System.exit;

/**
 * This class implements the methods for running test cases on a use case.
 * The class also collects relevant execution metrics (time, coverage, memory)
 */
public class TestExecutor {

    /**
     * The path of the jar containing the code to protect execute
     */
    private final String pathOfJarToExecute;

    /**
     * The path of the JUnit Console jar
     */
    private final String pathOfJUnitConsoleJar;

    /**
     * The path of the jar containing the test cases for the code to execute
     */
    private final String pathOfJarContainingTests;

    /**
     * The path of the JaCoCo agent jar for executing tests with coverage
     */
    private String pathOfJaCoCoAgent;

    /**
     * Flag for stating whether the tests should be executed with coverage or not
     */
    private boolean withCoverage = false;

    /**
     * Absolute path to the folder containing eventual native libraries
     */
    private final String pathOfFolderWithNativeLibraries;


    /**
     * The constructor accepts as arguments the path of the jar to execute and the path of the jar containing test files
     * @param pathOfJarToExecute Absolute path of the jar containing the code to protect execute
     * @param pathOfJarContainingTests Absolute path of the jar containing the test cases for the code to execute
     * @param pathOfJUnitConsoleJar Absolute path of the JUnit console jar to actually run the tests
     * @param pathOfFolderWithNativeLibraries Absolute path to the folder containing eventual native libraries, null if none
     */
    public TestExecutor (@NotNull String pathOfJarToExecute, @NotNull String pathOfJarContainingTests,
                         @NotNull String pathOfJUnitConsoleJar, String pathOfFolderWithNativeLibraries) {

        this.pathOfJarToExecute = pathOfJarToExecute;
        this.pathOfJarContainingTests = pathOfJarContainingTests;
        this.pathOfJUnitConsoleJar = pathOfJUnitConsoleJar;
        this.pathOfFolderWithNativeLibraries = pathOfFolderWithNativeLibraries;
    }


    /**
     * If invoked, the test executor will run tests with coverage (i.e., with JaCoCo). Remember that the execution
     * time given by JUnit is not precise if tests are run with coverage. The execution of the tests will produce
     * two files, jacoco.exec and jacoco.xml, containing the results of the methods coverage
     * @param pathOfJaCoCoAgent Absolute path of the JaCoCo agent jar for executing tests with coverage
     * @return this instance of TestExecutor
     */
    public TestExecutor withCoverage(String pathOfJaCoCoAgent) {

        this.pathOfJaCoCoAgent = pathOfJaCoCoAgent;

        withCoverage = true;

        return this;
    }

    /**
     * This function launches the tests on the code, eventually collecting execution metrics
     * Remember that the execution time given by JUnit is not precise if tests are run with JaCoCo
     * The function saves output files in the given folder. In detail, output files are
     * - "junitOutput.txt": the output of the JUnit console jar
     * - (if run with coverage): "jacoco.exec", "jacoco.xml": contains methods coverage by JaCoCo
     * - (if run without coverage): "TEST-junit-jupiter.xml": contains JUnit report and execution time
     * @param directoryWhereToSaveFiles directory where to save output files (JUnit and eventual JaCoCO reports)
     */
    public void runTests(@NotNull File directoryWhereToSaveFiles) {

        App.logger.info("[{}{}{}{}{}{}", "TestExecutor ", "(" + "runTests" + ")]: ", "Starting tests (" +
                        (withCoverage ? "with" : "without") + " methods coverage) contained in ",
                        pathOfJarContainingTests, " on ", pathOfJarToExecute);

        try {

            // where we the output of the JUnit console jar
            File junitOutput = new File(directoryWhereToSaveFiles.getAbsolutePath() + "/junitOutput.txt");

            if (junitOutput.createNewFile()) {

                // compose the command to launch the JUnit console
                ArrayList<String> junitConsoleCommand = new ArrayList<>();
                junitConsoleCommand.add("java");

                // if we have to run with coverage, include the JaCoCo java agent
                if (withCoverage)
                    junitConsoleCommand.add("-javaagent:" + pathOfJaCoCoAgent);

                if (pathOfFolderWithNativeLibraries != null)
                    junitConsoleCommand.add("-Djava.library.path=" + pathOfFolderWithNativeLibraries);

                // NOTE: the order of the jars in the classpath option IS IMPORTANT. The Java interpreter will
                // look for classes in the jars in the order they appear. Only if a class is not found in the first
                // jar will the interpreter look in the second jar
                junitConsoleCommand.addAll(Arrays.asList(
                        "-jar", pathOfJUnitConsoleJar,
                        "-cp", pathOfJarContainingTests + ":" + pathOfJarToExecute,
                        "--scan-classpath",
                        "--details=verbose",
                        "--reports-dir", "."));
                ProcessBuilder pb = new ProcessBuilder(junitConsoleCommand);

                if (pathOfFolderWithNativeLibraries != null) {
                    String oldPath = pb.environment().get("PATH");
                    String newPath = oldPath + ":" + pathOfFolderWithNativeLibraries;
                    pb.environment().put("PATH", newPath);
                }

                // set the directory where the process will run and redirect output (also error) to the output file
                pb.directory(directoryWhereToSaveFiles).redirectErrorStream(true).redirectOutput(junitOutput);

                App.logger.info("[{}{}{} ", "TestExecutor ", "(" + "runTests" + ")]: ", "executing command: " +
                        String.join(" ", junitConsoleCommand));

                Process p = pb.start();
                p.waitFor();

                // check that the code is 0, otherwise there was an error
                int exitStatus = p.exitValue();

                // "exitStatus != 0" because we want the process to be successful
                // "!(exitStatus == 1 && !withCoverage)" because the JUnit console will return 1 if everything is fine with
                // the tests execution but some tests failed. This is a failure we are willing to accept
                if (exitStatus == 0)
                    App.logger.info("[{}{}{}{}{} ", "TestExecutor ", "(" + "runTests" + ")]: ",
                            "JUnit ", (withCoverage ? "with JaCoCo agent" : ""), "exited with code: " + exitStatus);
                else if (exitStatus == 1)
                    App.logger.warn("[{}{}{}{}{}{} ", "TestExecutor ", "(" + "runTests" + ")]: ",
                            "JUnit ", (withCoverage ? "with JaCoCo agent" : ""), "exited with code: " + exitStatus,
                            " (probably some tests failed)");
                else {
                    App.logger.error("[{}{}{}{}{}{}{}{}", "TestExecutor ", "(" + "runTests" + ")]: ",
                            "JUnit ", (withCoverage ? "with JaCoCo agent" : ""), "exited with code: " + exitStatus,
                            " (check file ", junitOutput.getAbsolutePath()," for program output)");

                    exit(10);
                }
            }
            // this means that we were not able to create the file for the JUnit output
            else {

                App.logger.error("[{}{}{}", "TestExecutor ", "(" + "runTests" + ")]: ",
                        "Error while creating junit output file");

                throw new IOException("Error while creating junit output file");
            }

        }
        // thrown by ProcessBuilder.start
        catch (IOException e) {

            App.logger.error("[{}{}{}{}", "TestExecutor", "(" + "runTests" + ")]: ",
                    "IO Exception while executing the tests: ", e.getMessage());
            exit(2);
        }
        catch (InterruptedException e) {

            App.logger.error("[{}{}{}{}", "TestExecutor", "(" + "runTests" + ")]: ",
                    "Exception while waiting for process to finish", e.getMessage());
            exit(5);
        }
    }
}
