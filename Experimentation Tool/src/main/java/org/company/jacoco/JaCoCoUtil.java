package org.company.jacoco;

import java.io.BufferedReader;
import java.io.File;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.company.App;
import org.company.asm.ASMMethod;
import org.company.junit.JUnitTest;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.lang.System.exit;
import static org.company.junit.Const.kInitializationError;


/**
 * This is a general purpose utility class for interacting with JaCoCo methods and reports
 */
public class JaCoCoUtil {

    /**
     * This methods takes as input a "jacoco.exec" file and converts it in XML with the JaCoCo CLI
     * @param directoryWhereToSaveOutput directory where to save output files (JaCoCoCLI output)
     * @param pathOfJaCoCoExecFile the absolute path to the .exec file to convert
     * @param pathOfJaCoCoXMLFile the absolute path to where to create the converted .xml
     * @param pathOfJaCoCoCLI the absolute path to the JaCoCo CLI jar
     * @param pathOfJarWithClasses the absolute path of the jar containing the classes on which the tests were executed
     */
    public static void convertJaCoCoExecToXML(File directoryWhereToSaveOutput, String pathOfJaCoCoExecFile,
                                              String pathOfJaCoCoXMLFile, String pathOfJaCoCoCLI, String pathOfJarWithClasses) {

        App.logger.info("[{}{}{}{}{}{}", "JaCoCoParser ", "(" + "convertJaCoCoExecToXML" + ")]: ",
                "converting JaCoCo exec file: ", pathOfJaCoCoXMLFile, " in XML file: ", pathOfJaCoCoXMLFile);

        // this process builder is needed to run the JaCoCo CLI jar and convert the exec report in XML
        ProcessBuilder pbJaCoCoCLI;

        pbJaCoCoCLI = new ProcessBuilder("java", "-jar", pathOfJaCoCoCLI, "report", pathOfJaCoCoExecFile,
                "--classfiles", pathOfJarWithClasses, "--xml", pathOfJaCoCoXMLFile);

        try {

            // where we the output of the JUnit console jar
            File jacocoCLIOutput = new File(directoryWhereToSaveOutput.getAbsolutePath() + "/jacocoCLIOutput.txt");

            if (jacocoCLIOutput.createNewFile()) {

                // redirect also error stream to read eventual errors
                pbJaCoCoCLI.directory(directoryWhereToSaveOutput).redirectErrorStream(true).redirectOutput(jacocoCLIOutput);
                Process pJaCoCoCLI = pbJaCoCoCLI.start();
                pJaCoCoCLI.waitFor();

                // check that the code is 0, otherwise there was an error
                int exitStatus = pJaCoCoCLI.exitValue();

                if (exitStatus != 0) {
                    App.logger.error("[{}{}{}{}{}{}{}", "JaCoCoParser ", "(" + "convertJaCoCoExecToXML" + ")]: ",
                            "JaCoCoCli process exited with code: ", exitStatus, " (check file ",
                            jacocoCLIOutput.getAbsolutePath()," for program output)");

                    exit(10);
                }
            }
            // this means that we were not able to create the file for the JUnit output
            else {

                App.logger.error("[{}{}{}", "JaCoCoParser ", "(" + "convertJaCoCoExecToXML" + ")]: ",
                        "Error while creating JaCoCo CLI output file");

                throw new IOException("Error while creating JaCoCo CLI output output file");
            }
        }
        catch (IOException e) {

            App.logger.error("[{}{}{}{}", "JaCoCoParser", "(" + "convertJaCoCoExecToXML" + ")]: ",
                    "IO Exception while converting JaCoCo report: ", e.getMessage());
            exit(2);
        }
        catch (InterruptedException e) {

            App.logger.error("[{}{}{}{}", "JaCoCoParser ", "(" + "convertJaCoCoExecToXML" + ")]: ",
                    "Exception while waiting for process to finish", e.getMessage());
            exit(5);
        }

    }

    /**
     * This method takes as input a .xml JaCoCo report and parses it to extract, for
     * each method, the class FQN and instructions/branches coverage
     * @param jacocoReportToParse the JaCoCo XML file to parse
     * @return an array of JaCoCoMethod along with their coverage (branch, instructions, ...)
     */
    public static ArrayList<JaCoCoMethod> parseFromXML(File jacocoReportToParse) throws Exception {

        ArrayList<JaCoCoMethod> arrayMethodsCoverage;

        App.logger.info("[{}{}{}{}", "JaCoCoXMLParser", " (" + "parseFromXML" + ")]: ",
                "Parsing JaCoCo XML report in file: ", jacocoReportToParse.getAbsolutePath());

        try {

            // by using the Simple API for XML, parse the JaCoCo report in an orderly
            // fashion and return the methods and coverage hash map
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();

            // instruct the parser to ignore DTD instructions and references
            saxParserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            saxParserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            SAXParser saxParser = saxParserFactory.newSAXParser();
            JaCoCoExecHandler jaCoCoExecHandler = new JaCoCoExecHandler();
            saxParser.parse(jacocoReportToParse, jaCoCoExecHandler);
            arrayMethodsCoverage = jaCoCoExecHandler.getMethodsAndCoverage();
        }
        // thrown when creating the new SAX parser
        catch (ParserConfigurationException | SAXException e) {

            App.logger.error("[{}{}{}{}", "JaCoCoXMLParser", " (" + "parseFromXML" + ")]: ",
                    "Parsing Exception while parsing JaCoCo XML report: ", e.getMessage());
            throw new Exception("Parsing Exception while parsing JaCoCo XML report: " + e.getMessage());
        }
        // thrown when parsing the file (saxParser.parse)
        catch (IOException e) {

            App.logger.error("[{}{}{}{}", "JaCoCoXMLParser", " (" + "parseFromXML" + ")]: ",
                    "IO Exception while parsing JaCoCo XML report: ", e.getMessage());
            throw new Exception("IO Exception while parsing JaCoCo XML report: " + e.getMessage());
        }

        return arrayMethodsCoverage;
    }


    /**
     * given an array of JaCoCo methods, this method orders them by class and return
     * them in an hash map: the key of the map is the FQN of the class.
     * For each class, the array is composed of the methods the class contains
     * @param methods an array of JaCoCo methods
     * @return an hash map containing, for each class (key), the array (object) of JaCoCO methods
     */
    public static HashMap<String, ArrayList<JaCoCoMethod>> getMethodsByClass (List<JaCoCoMethod> methods) {

        HashMap<String, ArrayList<JaCoCoMethod>> methodsByClass = new HashMap<>();

        for (JaCoCoMethod jaCoCoMethod : methods) {

            String classFQN = jaCoCoMethod.getClassFQN();

            methodsByClass.putIfAbsent(classFQN, new ArrayList<>());
            methodsByClass.get(classFQN).add(jaCoCoMethod);
        }
        return methodsByClass;
    }

    /**
     * given an hash map of JaCoCoMethod methods, this method parse them in ASMMethod and returns
     * them in an hash map: the key of the map is the FQN of the class.
     * For each class, the array is composed of the methods the class contains
     * @param methods an array of JaCoCoMethod methods
     * @return an hash map containing, for each class (key), the array (object) of ASMMethod tests
     */
    public static HashMap<String, ArrayList<ASMMethod>> toASMMethod (HashMap<String, ArrayList<JaCoCoMethod>> methods) {

        HashMap<String, ArrayList<ASMMethod>> asmMethods = new HashMap<>();

        for (String key : methods.keySet()) {
            ArrayList<JaCoCoMethod> currentTests = methods.get(key);
            asmMethods.putIfAbsent(key, new ArrayList<>());
            for (JaCoCoMethod method : currentTests)
                asmMethods.get(key).add(method);
        }
        return asmMethods;
    }

    /**
     * given a JaCoCo method, this method creates and return
     * an hash map: the key of the map is the FQN of the class,
     * the value is the given method wrapped in an array list
     * @param method the JaCoCo method
     * @return an hash map: the key is the FQN of the class, the value is the given method wrapped in an array list
     */
    public static HashMap<String, ArrayList<JaCoCoMethod>> getMethodsByClass (JaCoCoMethod method) {

        HashMap<String, ArrayList<JaCoCoMethod>> methodByClass = new HashMap<>();

        String classFQN = method.getClassFQN();

        methodByClass.putIfAbsent(classFQN, new ArrayList<>());
        methodByClass.get(classFQN).add(method);

        return methodByClass;
    }

    /**
     * Custom visitor for the XML document. For each method, saves the class FQN, the method
     * name and the coverage (# of missed and covered) instructions/branches
     */
    static class JaCoCoExecHandler extends DefaultHandler {

        /**
         * To collect the methods and the percentage coverage of instructions and branches
         */
        private final ArrayList<JaCoCoMethod> methodsAndCoverage = new ArrayList<>();

        /**
         * since the XML file is visited in order, this variable contains the class
         * element currently being examined
         */
        String currentClass = null;

        /**
         * since the XML file is visited in order, this variable contains the method
         * element currently being examined
         */
        JaCoCoMethod currentMethod = null;

        // Invoked when an XML element is encountered
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {

            // the first element we are interested in is the name of the class
            if (qName.equalsIgnoreCase("class"))
                currentClass = (attributes.getValue("name").replace('/', '.'));

            // we analyze one method at time.
            // For each method, create a JaCoCo method to save coverage data
            else if (qName.equalsIgnoreCase("method") && currentClass != null) {

                currentMethod = new JaCoCoMethod(attributes.getValue("name"));
                currentMethod.setDesc(attributes.getValue("desc"));
                currentMethod.setClassFQN(currentClass);
            }

            // for each counter, save in the current method the coverage
            else if (qName.equalsIgnoreCase("counter") && currentMethod != null) {

                Integer missed = Integer.valueOf(attributes.getValue("missed"));
                Integer covered = Integer.valueOf(attributes.getValue("covered"));

                switch (attributes.getValue("type")) {

                    case "INSTRUCTION":
                        currentMethod.setInstructionsCovered(covered);
                        currentMethod.setInstructionsMissed(missed);
                        break;

                    case "BRANCH":
                        currentMethod.setBranchesCovered(covered);
                        currentMethod.setBranchesMissed(missed);
                        break;
                }
            }
        }


        @Override
        public void endElement(String uri, String localName, String qName) {

            // when we finish to analyze a method, save it in the array list to return
            if (qName.equalsIgnoreCase("method")) {

                methodsAndCoverage.add(currentMethod);

                // to warn that, currently, we are parsing no method
                currentMethod = null;
            }
            else if (qName.equalsIgnoreCase("class")) {

                // to warn that, currently, we are parsing no class
                currentClass = null;
            }
        }


        /**
         * getter for methodsAndCoverage
         * @return methodsAndCoverage
         */
        public ArrayList<JaCoCoMethod> getMethodsAndCoverage() {
            return methodsAndCoverage;
        }
    }

}


