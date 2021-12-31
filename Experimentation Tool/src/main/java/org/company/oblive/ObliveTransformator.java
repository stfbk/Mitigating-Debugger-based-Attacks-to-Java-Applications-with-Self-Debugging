package org.company.oblive;

import org.company.App;

import java.io.File;
import java.io.IOException;

import static java.lang.System.exit;
import static org.company.App.JAVA_HOME;

/**
 * Apply the transformations by Oblive
 */
public class ObliveTransformator {

    /**
     * the path in the file system to the Oblive jar
     */
    String obliveJarPath;


    /**
     * Simple constructor
     * @param obliveJarPath the path in the file system to the Oblive jar
     */
    public ObliveTransformator(String obliveJarPath) {

        this.obliveJarPath = new File(obliveJarPath).getAbsolutePath();
    }


    /**
     * This methods takes as input a jar file annotated and apply the AD protections through Oblive
     * @param executionDirectory the directory in which to execute Oblive
     * @param jarToProtectPath the path of the annotated jar to protect
     * @param protectedJarPath the path of the file in which to save the jar
     * @param nativeLibraryName the name of the native library that will contain the protected code (name only, no "lib" and ".so")
     */
    public void applyADProtections(File executionDirectory, String jarToProtectPath, String protectedJarPath, String nativeLibraryName) {

        App.logger.info("[{}{}{}{}{}{}", "ObliveTransformator ", "(" + "applyADProtections" + ")]: ",
                "apply AD protections to jar: ", jarToProtectPath, " in jar: ", protectedJarPath);


        try {

            // where we the output of the Oblive jar
            File obliveOutput = new File(executionDirectory.getAbsolutePath() + "/oblive.txt");

            if (obliveOutput.createNewFile()) {

                // this process builder is needed to run the Oblive jar
                ProcessBuilder pbOblive;

                pbOblive = new ProcessBuilder("java", "-jar",
                        obliveJarPath, jarToProtectPath, protectedJarPath, nativeLibraryName);

                pbOblive.environment().put("JAVA_HOME", JAVA_HOME);

                // redirect also error stream to read eventual errors
                pbOblive.directory(executionDirectory).redirectErrorStream(true).redirectOutput(obliveOutput);
                Process pOblive = pbOblive.start();
                pOblive.waitFor();

                // check also that the code is 0, otherwise there was an error
                int exitStatus = pOblive.exitValue();

                if (exitStatus != 0) {
                    App.logger.error("[{}{}{}{}", "ObliveTransformator ", "(" + "applyADProtections" + ")]: ",
                            "Oblive process exited with code: ", exitStatus);

                    exit(10);
                }
            }
            // this means that we were not able to create the file for the Oblive output
            else {

                App.logger.error("[{}{}{}", "ObliveTransformator ", "(" + "applyADProtections" + ")]: ",
                        "Error while creating Oblive output file");

                throw new IOException("Error while creating Oblive output file");
            }
        }
        catch (IOException e) {

            App.logger.error("[{}{}{}{}", "ObliveTransformator", "(" + "applyADProtections" + ")]: ",
                    "IO Exception while starting Oblive process: ", e.getMessage());
            exit(2);
        }
        catch (InterruptedException e) {

            App.logger.error("[{}{}{}{}", "ObliveTransformator ", "(" + "applyADProtections" + ")]: ",
                    "Exception while waiting for process to finish", e.getMessage());
            exit(5);
        }
    }
}
