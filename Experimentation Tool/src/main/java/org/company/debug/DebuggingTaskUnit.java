package org.company.debug;

import org.company.App;
import org.company.jacoco.JaCoCoMethod;
import org.json.JSONObject;

import static java.lang.System.exit;
import static org.company.debug.Const.*;

/**
 * this class represents a debugging task unit, i.e., a couple
 * of strings input-output. The input is a debugger command like "threads"
 * or "where", while the output is the expected output from the debugger after
 * the command is executed.
 */
public class DebuggingTaskUnit {

    /**
     * the command input to fed to the debugger
     */
    private final String commandInput;

    /**
     * the output expected by the debugger after having written the command
     */
    private final String expectedOutput;

    /**
     * if set, the debugging task unit will be repeated until the value of repeatUntil is found and consumed
     */
    private final String repeatUntil;


    /**
     * create a new debugging task unit
     * @param unitJSON the JSON object containing two keys, 'input' and 'output'
     * @param methodToProtect the method against which the debugging task runs to
     *                        this is needed because some debugging tasks units are
     *                        parametrized (e.g., set breakpoint in method)
     */
    public DebuggingTaskUnit(JSONObject unitJSON, JaCoCoMethod methodToProtect) {

        // now we parse the signature of the method to make it acceptable for the JDB. Indeed, JDB does not accept
        // a FQN like "org.apache.commons.lang3.math.NumberUtils.createNumber(Ljava/lang/String;)Ljava/lang/Number;"
        // but instead "org.apache.commons.lang3.math.NumberUtils.createNumber(java.lang.String)"

        // so originalDesc now contain something like "(Ljava/lang/String;)"
        String originalDesc = methodToProtect.getDescWithoutReturningType();
        StringBuilder normalizedDesc = new StringBuilder();

        StringBuilder ifArray = new StringBuilder();

        char firstLetter = originalDesc.charAt(0);
        originalDesc = originalDesc.substring(1);

        while (firstLetter != ')') {

            switch (firstLetter) {
                case '(':
                    normalizedDesc.append("(");
                    break;
                case '[':
                    ifArray.append("[]");
                    originalDesc = originalDesc.substring(1);
                    break;
                case 'Z':
                    normalizedDesc.append("boolean").append(ifArray).append(", ");
                    ifArray = new StringBuilder();
                    originalDesc = originalDesc.substring(1);
                    break;
                case 'B':
                    normalizedDesc.append("byte").append(ifArray).append(", ");
                    ifArray = new StringBuilder();
                    originalDesc = originalDesc.substring(1);
                    break;
                case 'C':
                    normalizedDesc.append("char").append(ifArray).append(", ");
                    ifArray = new StringBuilder();
                    originalDesc = originalDesc.substring(1);
                    break;
                case 'S':
                    normalizedDesc.append("short").append(ifArray).append(", ");
                    ifArray = new StringBuilder();
                    originalDesc = originalDesc.substring(1);
                    break;
                case 'I':
                    normalizedDesc.append("int").append(ifArray).append(", ");
                    ifArray = new StringBuilder();
                    originalDesc = originalDesc.substring(1);
                    break;
                case 'J':
                    normalizedDesc.append("long").append(ifArray).append(", ");
                    ifArray = new StringBuilder();
                    originalDesc = originalDesc.substring(1);
                    break;
                case 'F':
                    normalizedDesc.append("float").append(ifArray).append(", ");
                    ifArray = new StringBuilder();
                    originalDesc = originalDesc.substring(1);
                    break;
                case 'D':
                    normalizedDesc.append("double").append(ifArray).append(", ");
                    ifArray = new StringBuilder();
                    originalDesc = originalDesc.substring(1);
                    break;
                case 'L':
                    int indexOfSemiColon = originalDesc.indexOf(";");
                    String currentArgument = (originalDesc.substring(1, indexOfSemiColon));
                    currentArgument = currentArgument.replaceAll("/", ".");
                    normalizedDesc.append(currentArgument).append(ifArray).append(", ");
                    ifArray = new StringBuilder();
                    originalDesc = originalDesc.substring(indexOfSemiColon + 1);
                    break;
                default:
                    App.logger.error("[{}{}{}{}", "MyClassVisitor", " (" + "normalizeDesc" + ")]: ",
                            "character not supported: ", firstLetter);
                    exit(6);
            }

            firstLetter = originalDesc.charAt(0);
        }
        if (normalizedDesc.length() != 1)
            normalizedDesc.delete(normalizedDesc.length() - 2, normalizedDesc.length());
        normalizedDesc.append(")");

        String methodASMSignature = methodToProtect.getClassFQNForASM()
                + "."
                + methodToProtect.getMethodName()
                + normalizedDesc.toString();
        String methodASMNameInNative = "Java_" +
                methodToProtect.getClassFQN().replace(".", "_")
                + "_"
                + methodToProtect.getMethodName();
        methodASMNameInNative = methodASMNameInNative.replace("$", "_00024");

        String methodASMClassFQN = methodToProtect.getClassFQNForASM();

        // the input and the output are always strings, either null or with an actual value
        commandInput = (unitJSON.get(kInput) == JSONObject.NULL) ?
                null :
                unitJSON.getString(kInput)
                        .replace(kMethodNativePlaceholder, methodASMNameInNative)
                        .replace(kMethodFQNPlaceholder, methodASMSignature)
                        .replace(kClassFQNPlaceholder, methodASMClassFQN);

        expectedOutput = (unitJSON.get(kOutput) == JSONObject.NULL) ?
                null :
                unitJSON.getString(kOutput)
                        .replace(kMethodNativePlaceholder, methodASMNameInNative)
                        .replace(kMethodFQNPlaceholder, methodASMSignature)
                        .replace(kClassFQNPlaceholder, methodASMClassFQN);
        repeatUntil = (unitJSON.get(kRepeatUntil) == JSONObject.NULL) ?
                null :
                unitJSON.getString(kRepeatUntil)
                        .replace(kMethodNativePlaceholder, methodASMNameInNative)
                        .replace(kMethodFQNPlaceholder, methodASMSignature)
                        .replace(kClassFQNPlaceholder, methodASMClassFQN);
    }


    /**
     * simple constructor
     * @param commandInput the command input to fed to the debugger
     * @param expectedOutput the output expected by the debugger after having written the command
     * @param repeatUntil if set, the debugging task unit will be repeated until this value is found and consumed
     */
    public DebuggingTaskUnit(String commandInput, String expectedOutput, String repeatUntil) {

        this.commandInput = commandInput;
        this.expectedOutput = expectedOutput;
        this.repeatUntil = repeatUntil;
    }

    /**
     * getter for the command input
     * @return the command input
     */
    public String getCommandInput() {
        return commandInput;
    }

    /**
     * getter for the expected output
     * @return the expected output
     */
    public String getExpectedOutput() {
        return expectedOutput;
    }

    /**
     * getter for the repeat until string
     * @return the repeat until string
     */
    public String getRepeatUntil() {
        return repeatUntil;
    }
}