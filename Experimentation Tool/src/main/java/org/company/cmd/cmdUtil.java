package org.company.cmd;

import org.apache.commons.cli.CommandLine;

import java.io.File;

/**
 * This is a general purpose utility class for acquiring the command line options given to this program
 */
public class cmdUtil {

    /**
     * Acquires the given string and checks that is it neither null nor blank nor empty
     * @param cmd the command line object containing the program arguments
     * @param stringOptionToAcquire the key of the option to acquire
     * @param defaultValue the default value in case the option was not given. If both default value and
     *                     the given option are null, throw IllegalArgumentException
     * @return the option
     * @throws IllegalArgumentException if the default value is null and the given option is null or blank or empty
     */
    public static String acquireStringOption(CommandLine cmd, String stringOptionToAcquire, String defaultValue)
            throws IllegalArgumentException {

        String givenStringOption = cmd.getOptionValue(stringOptionToAcquire);
        givenStringOption = givenStringOption == null ? defaultValue : givenStringOption;

        if (givenStringOption == null || givenStringOption.equals("") || givenStringOption.isEmpty())
            throw new IllegalArgumentException("given path of jar " + stringOptionToAcquire + " is null");

        return givenStringOption;
    }


    /**
     * Acquires the given integer option and check that it is within the given boundaries
     * @param cmd the command line object containing the program arguments
     * @param integerOptionToAcquire the key of the option to acquire
     * @param defaultValue the default value in case the option was not given. If both default value and
     *                     the given option are null, throw IllegalArgumentException
     * @param minValue the minimum value the integer can have. If the given value is below, throw IllegalArgumentException
     * @param maxValue the maximum value the integer can have. If the given value is below, throw IllegalArgumentException
     * @return the option
     * @throws IllegalArgumentException if the default value is null and the given option is null or blank or empty
     */
    public static int acquireIntegerOption(CommandLine cmd, String integerOptionToAcquire, int defaultValue,
                                              int minValue, int maxValue) throws IllegalArgumentException {

        int valueToReturn = defaultValue;

        String givenIntegerOptionAsString = cmd.getOptionValue(integerOptionToAcquire);
        if (givenIntegerOptionAsString != null)
            valueToReturn = Integer.parseInt(givenIntegerOptionAsString);

        if (valueToReturn < minValue || valueToReturn > maxValue)
            throw new IllegalArgumentException("given number: " + valueToReturn + " is not between the given interval: " +
                    minValue + "-" + maxValue);

        return valueToReturn;
    }



    /**
     * Acquires the given path to a jar file and checks that the path is not null,
     * the file exists, the path is not a directory and it is indeed a jar file
     * @param cmd the command line object containing the program arguments
     * @param jarOptionToAcquire the key of the option to acquire
     * @param defaultValue the default value in case the option was not given. If both default value and
     *                     the given option are null, throw IllegalArgumentException
     * @return the option
     * @throws IllegalArgumentException if the option is null/does not exist/is a directory/it is not a jar file
     */
    public static String acquireJarPathOption(CommandLine cmd, String jarOptionToAcquire, String defaultValue)
            throws IllegalArgumentException {

        String givenJarOptionValue = cmd.getOptionValue(jarOptionToAcquire);
        givenJarOptionValue = givenJarOptionValue == null ? defaultValue : givenJarOptionValue;

        if (givenJarOptionValue != null) {

            if (!givenJarOptionValue.endsWith(".jar"))
                throw new IllegalArgumentException("given jar " + jarOptionToAcquire + " is not a jar file");

            File fileOfJarToProtect = new File(givenJarOptionValue);

            if (!fileOfJarToProtect.exists())
                throw new IllegalArgumentException("given jar " + fileOfJarToProtect.getAbsolutePath() + " does not exist ");

            if (fileOfJarToProtect.isDirectory())
                throw new IllegalArgumentException("given jar " + fileOfJarToProtect.getAbsolutePath() + " is a directory");

            givenJarOptionValue = new File(givenJarOptionValue).getAbsolutePath();
        }
        else
            throw new IllegalArgumentException("given path of jar " + jarOptionToAcquire + " is null");

        return givenJarOptionValue;
    }

    /**
     * Acquires the given path to a directory and checks that the path is not null. If the directory does not
     * exist, try to create it
     * @param cmd the command line object containing the program arguments
     * @param pathOptionToAcquire the key of the option to acquire
     * @param defaultValue the default value in case the option was not given. If both default value and
     *                     the given option are null, throw IllegalArgumentException
     * @return the option
     * @throws IllegalArgumentException if both default and given values are null OR if IOException while creating the folder
     */
    public static String acquireDirectoryPathOption(CommandLine cmd, String pathOptionToAcquire, String defaultValue)
            throws IllegalArgumentException {

        // get the path of the output folder and check that it exists. If not, try to create it
        String givenOutputFolderPath = cmd.getOptionValue(pathOptionToAcquire);

        if (givenOutputFolderPath != null) {
            File outputFolder = new File(givenOutputFolderPath);
            if (!outputFolder.exists()) {
                if (!outputFolder.mkdirs())
                    throw new IllegalArgumentException("given output folder did not exist and cannot be created");
            }
            givenOutputFolderPath = outputFolder.getAbsolutePath();
        }
        else {
            File outputFolder = new File(defaultValue);
            givenOutputFolderPath = outputFolder.getAbsolutePath();
        }

        return givenOutputFolderPath;
    }



}
