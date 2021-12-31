package org.company.debug;

/**
 * mainly keys of the JSON files containing the debugging tasks
 */
public class Const {


    // sample structure of JSON file with debugging tasks
    //
    // {
    //  "tasks": [{
    //    "name": "attachPriorRuntime",
    //    "units": [
    //      {
    //        "input":"set breakpoint in $methodFQN",
    //        "output":"Set uncaught java.lang.Throwable"
    //      },
    //      {
    // ...


    /**
     * key for the list (JSON array) of debugging tasks
     */
    public static final String kTasks = "tasks";

    /**
     * key for the name of the current debugging task
     */
    public static final String kName = "name";

    /**
     * key for the list (JSON array) of debugging tasks units
     */
    public static final String kUnits = "units";

    /**
     * key for the input of debugging task units
     */
    public static final String kInput = "input";

    /**
     * key for the output of debugging task units
     */
    public static final String kOutput = "output";

    /**
     * key for the option to repeat the debugging task unit
     */
    public static final String kRepeatUntil = "repeatUntil";

    /**
     * time (ms) to wait for the JDB output
     */
    public static final int kDebuggerOutputTimeout = 20;

    /**
     * placeholder in the input and output of debugging task units
     * This placeholder has to be replaced with the method against
     * which the debugging task is being run
     */
    public static final String kMethodFQNPlaceholder = "$methodFQN";

    /**
     * placeholder in the input and output of debugging task units
     * This placeholder has to be replaced with the name of the
     * method in the native library against which the debugging task
     * is being run
     */
    public static final String kMethodNativePlaceholder = "$methodNative";


    /**
     * placeholder in the input and output of debugging task units
     * This placeholder has to be replaced with the name of the
     * JUnit test with which the debugging task is being run
     */
    public static final String kJUnitTestPlaceholder = "$junitTest";


    /**
     * placeholder in the input and output of debugging task units
     * This placeholder has to be replaced with the class containing
     * the method against which the debugging task is being run
     */
    public static final String kClassFQNPlaceholder = "$classFQN";


    /**
     * maximum number of times to wait for the correct output
     */
    public static final int kDebuggerOutputMaxTries = 2500;

    /**
     * the string that the debuggee program should output is successfully launched with JDWP
     */
    public static final String debuggeeFixedOutput = "Listening for transport dt_socket at address:";

    /**
     * java program arguments to launch in debug mode
     */
    public static final String debugAgentLib = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y";
}
