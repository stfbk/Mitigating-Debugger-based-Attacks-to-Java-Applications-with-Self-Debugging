package org.company.debug;

import org.company.App;

import java.io.*;
import java.util.*;

import static java.lang.System.exit;
import static org.company.debug.Const.*;

/**
 * utility class for running JUnit test cases under a list of JDB (i.e., Java) or GDB (i.e., native) debugging task units
 */
public class DebugUtil {


    /**
     * the path to the JUnit console
     */
    private final String jUnitConsolePath;

    /**
     * the path to the jar to protect
     */
    private final String jarToProtectPath;

    /**
     * the path to the jar containing the tests
     */
    private final String jarWithTestsPath;

    /**
     * Absolute path to the folder containing eventual native libraries
     */
    private final String pathOfFolderWithNativeLibraries;


    /**
     * simple constructor
     * @param jUnitConsolePath the path to the JUnit console
     * @param jarToProtectPath the path to the jar to protect
     * @param jarWithTestsPath the path to the jar containing the tests
     * @param pathOfFolderWithNativeLibraries Absolute path to the folder containing eventual native libraries, null if none
     */
    public DebugUtil(String jUnitConsolePath, String jarToProtectPath, String jarWithTestsPath, String pathOfFolderWithNativeLibraries) {

        this.jUnitConsolePath = jUnitConsolePath;
        this.jarToProtectPath = jarToProtectPath;
        this.jarWithTestsPath = jarWithTestsPath;
        this.pathOfFolderWithNativeLibraries = pathOfFolderWithNativeLibraries;
    }


    /**
     * wraps the given debugging task over the given tests method, executes it and log the outcome in the given folder
     * @param outputDirectory the directory in which output
     * @param executionDirectory the directory in which execute the processes. If null, the output directory will be used
     * @param debuggingTask the object containing the list of debugging task units to execute
     * @param classFQN the FQN of the class containing the test
     * @param testName the name of the test to execute
     * @param superClassFQN the FQN of the class for JDB
     * @return true if the task executed correctly, false otherwise
     */
    public boolean executeJDBDebugTask (File outputDirectory, File executionDirectory,
                                        DebuggingTask debuggingTask, String classFQN, String testName, String superClassFQN) {

        boolean executionOutcome = false;

        try {

            String debuggingTaskName = debuggingTask.getDebuggingTaskName();

            App.logger.info("[{}{}{}{}", "DebugUtil", "(" + "executeJDBDebugTask" + ")]: ",
                    "starting JDB debug task with name: " + debuggingTaskName + " on test: ", classFQN + "." + testName);

            executionDirectory = (executionDirectory == null ? outputDirectory : executionDirectory);

            String commandToExecute = "java " + "-jar " + jUnitConsolePath +
                    " --class-path " + jarWithTestsPath + ":" + jarToProtectPath +
                    " --select-method " + classFQN + "#" + testName;

            // define and create the files to contain the output of the processes
            File debuggerOutput = new File(outputDirectory.getAbsolutePath() +
                    "/JDB_" + debuggingTaskName + "_" + classFQN + "_" + testName + ".txt");

            if (debuggerOutput.createNewFile()) {

                // run the command with "suspend=y", i.e., the debuggee will wait for the debugger before running
                // note that we do not give a port for where to listen to the debugger connection,
                // it is decided randomly by the JVM based on the available ports
                List<String> commandToExecuteArray = new ArrayList<>(Arrays.asList(commandToExecute.split(" ")));

                if (pathOfFolderWithNativeLibraries != null)
                    commandToExecuteArray.add(1, "-Djava.library.path=" + pathOfFolderWithNativeLibraries);

                commandToExecuteArray.add(1, debugAgentLib);

                App.logger.info("[{}{}{}{}", "DebugUtil ", "(" + "executeJDBDebugTask" + ")]: ",
                        "executing JDB command: ", String.join(" ", commandToExecuteArray));

                ProcessBuilder pbDebuggee = new ProcessBuilder(commandToExecuteArray);

                pbDebuggee.directory(executionDirectory).redirectErrorStream(true);
                Process pDebuggee = pbDebuggee.start();

                // we read now the first line of the debuggee process output. We expect it to the something like:
                // 'Listening for transport dt_socket at address: xxxxx'. We check that the format matches and then
                // get the address port number
                BufferedReader pDebuggeeReader = new BufferedReader(new InputStreamReader(pDebuggee.getInputStream()));
                String debuggeeFirstOutput = pDebuggeeReader.readLine();
                int lastSpaceIndex = debuggeeFirstOutput.lastIndexOf(" ");
                String debuggeeFirstOutputFormat = debuggeeFirstOutput.substring(0, lastSpaceIndex);
                if (debuggeeFirstOutputFormat.equalsIgnoreCase(debuggeeFixedOutput)) {

                    String portNumber = debuggeeFirstOutput.substring(lastSpaceIndex + 1);

                    App.logger.info("[{}{}{}{}{}{}", "DebugUtil ", "(" + "executeJDBDebugTask" + ")]: ",
                            "executing JDB command to attach to the other process: ", "jdb ", "-attach ", portNumber);

                    // now setup the debugger process and attach it to the debuggee
                    ProcessBuilder pbDebugger = new ProcessBuilder("jdb", "-attach", portNumber);

                    pbDebugger.directory(executionDirectory).redirectErrorStream(true);
                    Process pJDB = pbDebugger.start();

                    // these two are the buffer for reading and writing to the debugger process. We will user
                    // the writer to write commands, while the reader will wait for the expected output
                    BufferedReader pDebuggerReader = new BufferedReader(new InputStreamReader(pJDB.getInputStream()));
                    BufferedWriter pDebuggerWriter = new BufferedWriter(new OutputStreamWriter(pJDB.getOutputStream()));

                    // start over and check that the output of the debugger process is the expected one
                    FileOutputStream fos = new FileOutputStream(debuggerOutput);

                    // after having setup the environments and processes, actually execute the debugging task
                    executionOutcome = executeDebugTask(pDebuggerReader, pDebuggerWriter,
                            fos, debuggingTask, classFQN + "." + testName, superClassFQN + "." + testName);
                }
                else {
                    App.logger.error("[{}{}{}", "DebugUtil ", "(" + "executeJDBDebugTask" + ")]: ",
                            "Error while starting debuggee process");

                    throw new IOException("Error while starting debuggee process");
                }
            }
            // this means that we were not able to create the file for the debuggee or debugger output
            else {

                App.logger.error("[{}{}{}", "DebugUtil ", "(" + "executeJDBDebugTask" + ")]: ",
                        "Error while creating output file");

                throw new IOException("Error while creating output file");
            }

        }
        catch (IOException e) {

            App.logger.error("[{}{}{}{}", "DebugUtil ", "(" + "executeJDBDebugTask" + ")]: ",
                    "IO Exception while executing preparing the debug task: ", e.getMessage());
            exit(2);
        }
        catch (Exception e) {
            App.logger.error("[{}{}{}{}", "DebugUtil ", "(" + "executeJDBDebugTask" + ")]: ",
                    "Generic exception: ", e.getMessage());
            exit(4);
        }
        return executionOutcome;
    }















    /**
     * wraps the given debugging task over the given tests method, executes it and log the outcome in the given folder
     * @param outputDirectory the directory in which output
     * @param executionDirectory the directory in which execute the processes. If null, the output directory will be used
     * @param debuggingTask the object containing the list of debugging task units to execute
     * @param classFQN the FQN of the class containing the test
     * @param testName the name of the test to execute
     * @return true if the task executed correctly, false otherwise
     */
    public boolean executeGDBDebugTask (File outputDirectory, File executionDirectory,
                                        DebuggingTask debuggingTask, String classFQN, String testName) {
        boolean executionOutcome = false;

        try {

            String debuggingTaskName = debuggingTask.getDebuggingTaskName();

            App.logger.info("[{}{}{}{}", "DebugUtil", "(" + "executeGDBDebugTask" + ")]: ",
                    "starting GDB debug task with name: " + debuggingTaskName + " on test: ", classFQN + "." + testName);

            executionDirectory = (executionDirectory == null ? outputDirectory : executionDirectory);

            String commandToExecute = "gdb " + "--args " +
                    "java " + "-jar " + jUnitConsolePath +
                    " --class-path " + jarWithTestsPath + ":" + jarToProtectPath +
                    " --select-method " + classFQN + "#" + testName;

            // define and create the files to contain the output of the processes
            File outputGDB = new File(outputDirectory.getAbsolutePath() +
                    "/GDB_" + debuggingTaskName + "_" + classFQN + "_" + testName + ".txt");

            if (outputGDB.createNewFile()) {

                List<String> commandToExecuteArray = new ArrayList<>(Arrays.asList(commandToExecute.split(" ")));

                if (pathOfFolderWithNativeLibraries != null)
                    commandToExecuteArray.add(3, "-Djava.library.path=" + pathOfFolderWithNativeLibraries);

                App.logger.info("[{}{}{}{}", "DebugUtil ", "(" + "executeGDBDebugTask" + ")]: ",
                        "executing GDB command: ", String.join(" ", commandToExecuteArray));

                // now setup the GDB process and attach it to the debuggee
                ProcessBuilder pbGDB = new ProcessBuilder(commandToExecuteArray);
                pbGDB.directory(executionDirectory).redirectErrorStream(true);
                Process pGDB = pbGDB.start();

                // these two are the buffer for reading and writing to the GDB process. We will user
                // the writer to write commands, while the reader will wait for the expected output
                BufferedReader pGDBReader = new BufferedReader(new InputStreamReader(pGDB.getInputStream()));
                BufferedWriter pGDBWriter = new BufferedWriter(new OutputStreamWriter(pGDB.getOutputStream()));

                // start over and check that the output of the GDB process is the expected one
                FileOutputStream fos = new FileOutputStream(outputGDB);

                // after having setup the environments and processes, actually execute the debugging task
                executionOutcome = executeDebugTask(pGDBReader, pGDBWriter,
                        fos, debuggingTask, classFQN + "." + testName, "useless");

            }
            // this means that we were not able to create the file for the GDB output
            else {

                App.logger.error("[{}{}{}", "DebugUtil ", "(" + "executeGDBDebugTask" + ")]: ",
                        "Error while creating output file");

                throw new IOException("Error while creating output file");
            }

        }
        catch (IOException e) {

            App.logger.error("[{}{}{}{}", "DebugUtil ", "(" + "executeGDBDebugTask" + ")]: ",
                    "IO Exception while executing preparing the debug task: ", e.getMessage());
            exit(2);
        }
        catch (Exception e) {
            App.logger.error("[{}{}{}{}", "DebugUtil ", "(" + "executeGDBDebugTask" + ")]: ",
                    "Generic exception: ", e.getMessage());
            exit(4);
        }
        return executionOutcome;
    }











    /**
     * actually executes the debugging task, independently of the debugger (either JDB or GBD). For each debugging
     * task unit, it sends the input to the debugger and wait for the expected output. The method will wait
     * kDebuggerOutputTimeout milliseconds for maximum kDebuggerOutputMaxTries times (set values in Const class).
     * If the expected output is not found, the method returns false. Otherwise, it proceeds to the next task
     * until the last one.
     * @param pDebuggerReader buffered reader to read the output of the debugger process
     * @param pDebuggerWriter buffered writer to send commands to te debugger process
     * @param fos file output stream toward a file where to save the output of the debugger
     * @param debuggingTask the debugging task to execute
     * @param testFQN the name of the method for logging purposes
     * @param superClassFQN the name of the method for junit parametrization
     * @return true if the tak succeeds, false otherwise
     */
    private boolean executeDebugTask(BufferedReader pDebuggerReader, BufferedWriter pDebuggerWriter, FileOutputStream fos,
                                  DebuggingTask debuggingTask, String testFQN, String superClassFQN) {

        String debuggingTaskName = debuggingTask.getDebuggingTaskName();

        DebuggingTaskUnit currentDebuggingTaskUnit;
        boolean executionOutcome = false;
        boolean foundExpectedOutput = true;

        try {

            // feed all commands to the debugger process and check that the output matches the expected one
            // note: the command may take some time to be run, or the debuggee process needs to execute some
            //       instructions first. Therefore, for a while, the debugger process may not output what we expect
            //       in this case, we will wait for some time before aborting the operation
            //       If, after the timeout, the debugger process did not output what we expected,
            //       we close everything and declare failure
            while (debuggingTask.areThereMoreDebuggingTasksUnits()) {

                // if there is no current debugging unit, it means that either this is the first iteration
                // of the loop OR the previous unit completed successfully
                // in both cases, take the next task, if any
                currentDebuggingTaskUnit = debuggingTask.next();

                // get the input to feed to the debugger and the expected output
                String commandInput = currentDebuggingTaskUnit.getCommandInput(); if (commandInput != null) { commandInput = commandInput.replace(kJUnitTestPlaceholder, superClassFQN); }
                String expectedOutput = currentDebuggingTaskUnit.getExpectedOutput(); if (expectedOutput != null) { expectedOutput = expectedOutput.replace(kJUnitTestPlaceholder, superClassFQN); }
                String repeatUntil = currentDebuggingTaskUnit.getRepeatUntil(); if (repeatUntil != null) { repeatUntil = repeatUntil.replace(kJUnitTestPlaceholder, superClassFQN); }

                App.logger.info("[{}{}{}{}{}{}{}{}{} ", "DebugUtil", "(" + "executeDebugTask" + ")]: ",
                        "next debugging task unit: input is: ", commandInput, " , expected output is: ",
                        expectedOutput, ", repeat until ", repeatUntil ,")");

                // if any, send the command to the the debugger process
                if (commandInput != null) {
                    pDebuggerWriter.write(commandInput + "\n");
                    fos.write(("\n[LOG] command sent is: \"" + commandInput + "\"\n").getBytes());
                } else {
                    fos.write(("\n[LOG] command is null\n").getBytes());
                }
                if (expectedOutput != null) {
                    fos.write(("\n[LOG] expected output is: \"" + expectedOutput + "\"\n").getBytes());
                } else {
                    fos.write(("\n[LOG] expected output is null\n").getBytes());
                }
                if (repeatUntil != null) {
                    fos.write(("\n[LOG] repeat until is: \"" + repeatUntil + "\"\n").getBytes());
                } else {
                    fos.write(("\n[LOG] repeat until is null\n").getBytes());
                }

                pDebuggerWriter.flush();
                fos.flush();

                int currentTryNumber = 0;
                foundExpectedOutput = false;
                StringBuilder debuggerOutputBuilder = new StringBuilder();

                while (currentTryNumber < kDebuggerOutputMaxTries && !foundExpectedOutput) {

                    Thread.sleep(kDebuggerOutputTimeout);
                    currentTryNumber++;
                    char newReadByte;

                    // to make an effective polling, we have to check whether
                    // the reader is "ready", i.e., there are bytes to read
                    // If true, then read all bytes available and add them to
                    // the current string (and of course we do not have found our output already)
                    // then, check if the string read from the buffered reader is
                    // the expected output. If true, go on to the next task unit
                    // If not, continue to wait for other bytes to read until either
                    // the debugger outputs the expected output or we reach the maximum number of tries
                    // Note: if the byte read is the new line char, save the line in
                    // the output file and clear the string builder.
                    while (pDebuggerReader.ready() && !foundExpectedOutput) {

                        // if there is at least a byte to read, then read it and add it to the current line
                        // contained in the string builder.
                        newReadByte = (char) pDebuggerReader.read();
                        debuggerOutputBuilder.append(newReadByte);
                        String debuggerOutput = debuggerOutputBuilder.toString();

                        boolean didWeWriteAlready = false;

                        if (debuggerOutput.toLowerCase().contains(expectedOutput.toLowerCase())) {

                            if (repeatUntil == null)
                                foundExpectedOutput = true;
                            // we need to repeat the debugging task unit
                            // therefore, reset the counter and send again
                            // the command
                            else {
                                currentTryNumber = 0;
                                if (commandInput != null) {
                                    pDebuggerWriter.write(commandInput + "\n");
                                    fos.write(("\n[LOG] repeatUntil is not null, so repeat the command\n").getBytes());
                                    fos.write(("\n[LOG] command sent is: \"" + commandInput + "\"\n").getBytes());
                                }
                                pDebuggerWriter.flush();
                            }

                            fos.write((debuggerOutput).getBytes());
                            didWeWriteAlready = true;

                            debuggerOutputBuilder = new StringBuilder();
                        }
                        if (repeatUntil != null && debuggerOutput.toLowerCase().contains(repeatUntil.toLowerCase())) {
                            foundExpectedOutput = true;

                            if (!didWeWriteAlready) {
                                fos.write((debuggerOutput).getBytes());
                                didWeWriteAlready = true;
                            }
                            debuggerOutputBuilder = new StringBuilder();
                        }

                        // If the character is the '\n' line terminator, then
                        // save the string in the output file and reset the string builder
                        if (newReadByte == '\n') {
                            if (!didWeWriteAlready) {
                                fos.write((debuggerOutput).getBytes());
                            }
                            debuggerOutputBuilder = new StringBuilder();
                        }
                    }
                }

                // if we did not find the expected output, alas, the debugging task unit failed
                if (!foundExpectedOutput) {
                    fos.write((debuggerOutputBuilder.toString()).getBytes());
                    App.logger.warn("[{}{}{}{}{}{}{} ", "DebugUtil", "(" + "executeDebugTask" + ")]: ",
                            "did not found the expected output (expected output was: ", expectedOutput,
                            ", string builder contains:", debuggerOutputBuilder, ")");
                    break;
                }
            }


            if (!debuggingTask.areThereMoreDebuggingTasksUnits() && foundExpectedOutput) {
                App.logger.info("[{}{}{}{}{}{}{}", "DebugUtil", "(" + "executeDebugTask" + ")]: ",
                        "task: ", debuggingTaskName, " on test: ", testFQN, " successfully executed");
                executionOutcome = true;
            } else
                App.logger.warn("[{}{}{}{}{}{}{} ", "DebugUtil", "(" + "executeDebugTask" + ")]: ", "error in task: ",
                        debuggingTaskName, " on test: ", testFQN, ": output finished but there were still task units");

            // since the process at this point is already closed, do not invoke these methods
            // writer.flush();
            // writer.close();

            fos.flush();
            fos.close();
            // do not check for the exit code, our AD protection may break the debugger
        }
        // thrown by ProcessBuilder.start
        catch (IOException e) {

            App.logger.error("[{}{}{}{}", "DebugUtil", "(" + "executeDebugTask" + ")]: ",
                    "IO Exception while executing debug task: ", e.getMessage());
            exit(2);
        }
        catch (InterruptedException e) {

            App.logger.error("[{}{}{}{}", "DebugUtil", "(" + "executeDebugTask" + ")]: ",
                    "Exception while waiting for process to finish", e.getMessage());
            exit(5);
        }
        catch (Exception e) {
            App.logger.error("[{}{}{}{}", "DebugUtil", "(" + "executeDebugTask" + ")]: ",
                    "Generic exception: ", e.getMessage());
            e.printStackTrace();
            exit(4);
        }

        return executionOutcome;
    }
}