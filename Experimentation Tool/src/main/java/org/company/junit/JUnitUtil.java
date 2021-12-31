package org.company.junit;

import java.io.File;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.FileUtils;
import org.company.TestExecutor;
import org.company.asm.ASMMethod;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import static java.lang.Math.sqrt;
import static java.lang.System.exit;
import static org.company.App.logger;
import static org.company.junit.Const.*;
import static org.company.junit.JUnitTest.*;
import static org.company.xml.XMLUtil.removeUnicodeCharacter;


/**
 * This is a general purpose utility class for interacting with JUnit tests and reports
 */
public class JUnitUtil {


    /**
     * The JUnit console platform outputs two reports, one for JUnit 5 tests and
     * the other for JUnit <=4 tests. Since we do not know whether the tests were written with
     * JUnit 5 or <=4, we have to check both files and add tests from both files.
     * This method returns, for each test, the execution time and whether
     * the test succeeded or not
     * @param junitReport4ToParse the JUnit 4 XML report to parse
     * @param junitReport5ToParse the JUnit 5 XML report to parse
     * @return the JUnit tests and classes contained in the reports along with their execution time and outcome
     */
    public static JUnitClassesAndTestsBundle parseFromXML(File junitReport4ToParse, File junitReport5ToParse) throws Exception {

        // sometimes it happen that inside junit report files we find a damned unicode character.
        // since xml1.0 does not support these characters, we have to remove them
        String backspace = String.valueOf(( (char) 0x8 ));
        removeUnicodeCharacter(junitReport4ToParse, backspace);
        removeUnicodeCharacter(junitReport5ToParse, backspace);
        String ctrl_l = String.valueOf(( (char) 0xc ));
        removeUnicodeCharacter(junitReport4ToParse, ctrl_l);
        removeUnicodeCharacter(junitReport5ToParse, ctrl_l);
        String ctrl_f = String.valueOf(( (char) 0x6 ));
        removeUnicodeCharacter(junitReport4ToParse, ctrl_f);
        removeUnicodeCharacter(junitReport5ToParse, ctrl_f);

        JUnitClassesAndTestsBundle jUnit4ReportResults = JUnitUtil.parseFromXML(junitReport4ToParse);
        JUnitClassesAndTestsBundle jUnit5ReportResults = JUnitUtil.parseFromXML(junitReport5ToParse);

        ArrayList<JUnitTest> jUnit4Tests = new ArrayList<>(jUnit4ReportResults.getJunitTests());
        ArrayList<JUnitTest> jUnit5Tests = new ArrayList<>(jUnit5ReportResults.getJunitTests());
        jUnit4Tests.forEach(test ->test.setJunit5(false));
        jUnit5Tests.forEach(test ->test.setJunit5(true));
        ArrayList<JUnitTest> jUnitTestsToReturn = new ArrayList<>();
        jUnitTestsToReturn.addAll(jUnit4Tests);
        jUnitTestsToReturn.addAll(jUnit5Tests);

        ArrayList<JUnitClass> jUnit4Classes = new ArrayList<>(jUnit4ReportResults.getJUnitClasses());
        ArrayList<JUnitClass> jUnit5Classes = new ArrayList<>(jUnit5ReportResults.getJUnitClasses());
        jUnit4Classes.forEach(tempClass -> tempClass.setJunit5(false));
        jUnit5Classes.forEach(tempClass -> tempClass.setJunit5(true));
        ArrayList<JUnitClass> jUnitClasses = new ArrayList<>();
        jUnitClasses.addAll(jUnit4Classes);
        jUnitClasses.addAll(jUnit5Classes);

        String absPathJUnit4Tests = junitReport4ToParse.getAbsolutePath();
        String absPathJUnit5Tests = junitReport5ToParse.getAbsolutePath();

        int jUnit4TestsSize = jUnit4Tests.size();
        int jUnit5TestsSize = jUnit5Tests.size();

        // there are no tests, but this is not our error or fault
        if (jUnit4TestsSize == 0 && jUnit5TestsSize == 0)
            logger.error("[{}{}{}{}{}{} ", "JUnitUtil", " (" + "parseFromXML" + ")]: ",
                    "no tests were found in ", absPathJUnit4Tests, " and ",
                    absPathJUnit5Tests);

        // there are no tests in junit 4 report
        else if (jUnit4TestsSize == 0)
            logger.warn("[{}{}{}{} ", "JUnitUtil", " (" + "parseFromXML" + ")]: ",
                    "no tests were found in ", absPathJUnit4Tests);

        // there are no tests in junit 5 report
        else if (jUnit5TestsSize == 0)
            logger.warn("[{}{}{}{} ", "JUnitUtil", " (" + "parseFromXML" + ")]: ",
                    "no tests were found in ", absPathJUnit5Tests);

        return new JUnitClassesAndTestsBundle(jUnitTestsToReturn, jUnitClasses);
    }

    /**
     * given an array of JUnit tests, this method keeps only failed tests and returns
     * them in an hash map: the key of the map is the FQN of the class.
     * For each class, the array is composed of the (failed) tests the class contains
     * @param tests an array of JUnit tests
     * @return an hash map containing, for each class (key), the array (object) of failed JUnit tests
     */
    public static HashMap<String, ArrayList<JUnitTest>> getFailedTestsByClass(ArrayList<JUnitTest> tests) {

        HashMap<String, ArrayList<JUnitTest>> failedTests = new HashMap<>();

        for (JUnitTest jUnitTest : tests) {

            String classFQN = jUnitTest.getClassFQN();

            if (jUnitTest.getOutcome() == JUnitTest.FAILED) {
                failedTests.putIfAbsent(classFQN, new ArrayList<>());
                failedTests.get(classFQN).add(jUnitTest);
            }
        }
        return failedTests;
    }

    /**
     * given an array of JUnit tests, this method keeps only failed tests and returns them
     * @param tests an array of JUnit tests
     * @return the array containing only failed JUnit tests
     */
    public static ArrayList<JUnitTest> getFailedTests(ArrayList<JUnitTest> tests) {

        ArrayList<JUnitTest> failedTests = new ArrayList<>();

        for (JUnitTest jUnitTest : tests) {
            if (jUnitTest.getOutcome() == JUnitTest.FAILED)
                failedTests.add(jUnitTest);
        }
        return failedTests;
    }

    /**
     * given an array of JUnit tests, this method keeps only successful tests and returns
     * them in an hash map: the key of the map is the FQN of the class.
     * For each class, the array is composed of the (successful) tests the class contains
     * @param tests an array of JUnit tests
     * @return an hash map containing, for each class (key), the array (object) of successful JUnit tests
     */
    public static HashMap<String, ArrayList<JUnitTest>> getSuccessfulTestsByClass(ArrayList<JUnitTest> tests) {

        HashMap<String, ArrayList<JUnitTest>> successfulTests = new HashMap<>();

        for (JUnitTest jUnitTest : tests) {

            String classFQN = jUnitTest.getClassFQN();

            if (jUnitTest.getOutcome() == PASSED) {
                successfulTests.putIfAbsent(classFQN, new ArrayList<>());
                successfulTests.get(classFQN).add(jUnitTest);
            }
        }
        return successfulTests;
    }

    /**
     * given an array of JUnit tests, this method keeps only successful tests and returns them
     * @param tests an array of JUnit tests
     * @return the array containing only failed JUnit tests
     */
    public static ArrayList<JUnitTest> getSuccessfulTests(ArrayList<JUnitTest> tests) {

        ArrayList<JUnitTest> failedTests = new ArrayList<>();

        for (JUnitTest jUnitTest : tests) {
            if (jUnitTest.getOutcome() == PASSED)
                failedTests.add(jUnitTest);
        }
        return failedTests;
    }

    /**
     * given an array of JUnit tests, this method returns
     * them in an hash map: the key of the map is the FQN of the class.
     * For each class, the array is composed of the tests the class contains
     * @param tests an array of JUnit tests
     * @return an hash map containing, for each class (key), the array (object) of JUnit tests
     */
    public static HashMap<String, ArrayList<JUnitTest>> getTestsByClass(ArrayList<JUnitTest> tests) {

        HashMap<String, ArrayList<JUnitTest>> testByClass = new HashMap<>();

        for (JUnitTest jUnitTest : tests) {
            String classFQN = jUnitTest.getClassFQN();
            testByClass.putIfAbsent(classFQN, new ArrayList<>());
            testByClass.get(classFQN).add(jUnitTest);
        }
        return testByClass;
    }

    /**
     * given an hash map of JUnit tests, this method parse them in ASM methods and returns
     * them in an hash map: the key of the map is the FQN of the class, the value the methods
     * defined in that class
     * @param tests an hash map of JUnit tests
     * @return an hash map containing, for each class (key), the array (object) of ASMMethod methods
     */
    public static HashMap<String, ArrayList<ASMMethod>> toASMMethods(HashMap<String, ArrayList<JUnitTest>>  tests) {
        HashMap<String, ArrayList<ASMMethod>> asmMethods = new HashMap<>();

        for (String key : tests.keySet()) {
            ArrayList<JUnitTest> currentTests = tests.get(key);
            asmMethods.put(key, toASMMethods(currentTests));
        }
        return asmMethods;
    }

    /**
     * given an array of JUnit tests, this method parses them in ASM methods
     * @param tests an array of JUnit tests
     * @return an array of ASM methods
     */
    public static ArrayList<ASMMethod> toASMMethods(ArrayList<JUnitTest> tests) {
        return new ArrayList<>(tests);
    }

    /**
     * given an array of JUnit tests, this method parse them in ASM methods and returns
     * them in an hash map: the key of the map is the FQN of the class, the value the methods
     * defined in that class
     * @param tests an hash map of JUnit tests
     * @return an hash map containing, for each class (key), the array (object) of ASMMethod methods
     */
    public static HashMap<String, ArrayList<ASMMethod>> toASMMethodByClass(ArrayList<JUnitTest> tests) {

        HashMap<String, ArrayList<ASMMethod>> asmMethods = new HashMap<>();

        for (JUnitTest jUnitTest : tests) {
            String classFQN = jUnitTest.getClassFQN();
            asmMethods.putIfAbsent(classFQN, new ArrayList<>());
            asmMethods.get(classFQN).add(jUnitTest);
        }

        return asmMethods;
    }


    /**
     * wrapper to run a test suite multiple times and collect the average results
     * @param repetitionNumber the number of times tests will be repeated
     * @param directoryWhereToSaveFiles the directory in which to save all output files. Note that this method will
     *                                  create a folder for each test repetition in which to save JUnit reports
     * @param testExecutor the test executor object with the jars containing the code to test and the tests
     * @return an array of JUnit tests containing the average results of the execution
     * @throws Exception internal exception
     */
    public static ArrayList<JUnitTest> runJUnitTestsMultipleTimes 
            (int repetitionNumber, @NotNull File directoryWhereToSaveFiles, TestExecutor testExecutor) throws Exception {

        ArrayList<JUnitTest> executionMetrics = null;

        // simply, we repeat the same procedure for the given number of times:
        //   first, we create a new folder for the execution of the tests;
        //   then, we execute the tests and parse the JUnit XML output file;
        //   finally, we aggregate the results by summing the execution time
        //   and checking that the test succeeded
        for (int i = 0; i < repetitionNumber; i++) {

            logger.info("[{}{}{}{}{}{} ", "JUnitUtil", " (" + "runJUnitTestsMultipleTimes" + ")]: ",
                    "Tests repetition ", (i+1), " of ", repetitionNumber);

            File testExecutionFolder = new File(directoryWhereToSaveFiles.getAbsolutePath() + "/" + (i+1) + "/");
            File jUnit4ReportFile = new File(testExecutionFolder.getAbsolutePath() + "/" + kJunit4ReportDefaultName);
            File jUnit5ReportFile = new File(testExecutionFolder.getAbsolutePath() + "/" + kJunit5ReportDefaultName);

            if (!testExecutionFolder.mkdirs()) {
                logger.error("[{}{}{}{} ", "JUnitUtil", " (" + "runJUnitTestsMultipleTimes" + ")]: ",
                        "error while creating folder for storing output of tests repetition number :", i+1);
                throw new IOException("Error while creating folder for storing output of tests repetition number " + (i+1));
            }

            boolean testRunWasKilled = false;
            ArrayList<JUnitTest> executionMetricsRepetition = null;
            testExecutor.runTests(testExecutionFolder);
            try {
                executionMetricsRepetition =
                        JUnitUtil.parseFromXML(jUnit4ReportFile, jUnit5ReportFile).getJunitTests();
            }
            // Sometimes it happens that the JUnit process aborts/gets killed. Therefore, try to repeat the run
            catch (FileNotFoundException e) {
                logger.warn("[{}{}{} ", "JUnitUtil", " (" + "runJUnitTestsMultipleTimes" + ")]: ",
                        "the test run was killed, trying again");
                testRunWasKilled = true;
            }
            if (!testRunWasKilled) {
                executionMetricsRepetition.sort(Comparator.comparing(JUnitTest::getTestFQNName));

                // after having executed the tests, aggregate the results
                if (executionMetrics == null) {
                    executionMetrics = executionMetricsRepetition;
                } else {

                    // for each test in both arrays, we sum the execution time and check that both were successful
                    for (int j = 0; j < executionMetrics.size(); j++) {

                        JUnitTest originalTest = executionMetrics.get(j);
                        JUnitTest repetitionTest = executionMetricsRepetition.get(j);

                        String originalTestFQNName = originalTest.getTestFQNName();
                        String repetitionTestFQNName = repetitionTest.getTestFQNName();

                        if (!originalTestFQNName.equals(repetitionTestFQNName)) {
                            logger.error("[{}{}{}{}{}{}{} ", "JUnitUtil", " (" + "runJUnitTestsMultipleTimes" + ")]: ",
                                    "mismatch in the name of the tests, (original test: ",
                                    originalTestFQNName, ", repetition test: ", repetitionTestFQNName, ")");
                            exit(8);
                        }

                        double newExecutionTime = originalTest.getExecutionTime() + repetitionTest.getExecutionTime();
                        boolean newIsSuccessful = originalTest.getOutcome() == PASSED && repetitionTest.getOutcome() == PASSED;

                        originalTest.setExecutionTime(newExecutionTime);
                        originalTest.addExecutionTime(repetitionTest.getExecutionTime());
                        originalTest.setOutcome(newIsSuccessful ? PASSED : FAILED);
                    }
                }
            }
            else {
                i = i - 1;
                FileUtils.deleteDirectory(testExecutionFolder);
            }
        }

        // the last step is to calculate the average of the execution time of the tests by dividing the
        // current stored execution time by the number of repetitions
        assert executionMetrics != null;
        executionMetrics.forEach(jUnitTest ->
                jUnitTest.setExecutionTime(jUnitTest.getExecutionTime()/repetitionNumber));

        for (JUnitTest jUnitTest : executionMetrics) {

            double average = jUnitTest.getExecutionTime();
            ArrayList<Double> executionTimes = jUnitTest.getExecutionTimes();
            double standardDeviation = 0;

            for (Double executionTime : executionTimes)
                standardDeviation = standardDeviation + (executionTime - average)*(executionTime - average);
            jUnitTest.setStandardDeviation(sqrt(standardDeviation));
        }
        return executionMetrics;   
    }

    /**
     * This method takes as input a .xml JUnit report and parses to extract, for
     * each test, the execution time and whether the test succeeded or not
     * Note that parametrized tests are returned only once, and the worst-case (i.e., failed test) is considered
     * @param junitReportToParse the JUnit XML report to parse
     * @return the JUnit tests and classes contained in the report along with their execution time and outcome
     */
    private static JUnitClassesAndTestsBundle parseFromXML(File junitReportToParse) throws Exception {

        ArrayList<JUnitTest> executedTests;
        ArrayList<JUnitClass> classesWithErrors;

        logger.info("[{}{}{}{}", "JUnitParser", " (" + "parseFromXML" + ")]: ",
                "Parsing JUnit XML report in file: ", junitReportToParse.getAbsolutePath());

        try {

            // by using the Simple API for XML, parse the JUnit report in an orderly
            // fashion and return the execution time and outcome
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();

            // instruct the parser to ignore DTD instructions and references
            saxParserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            saxParserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            SAXParser saxParser = saxParserFactory.newSAXParser();
            JUnitHandler jUnitHandler = new JUnitHandler();
            saxParser.parse(junitReportToParse, jUnitHandler);
            executedTests = jUnitHandler.getTestsAndOutcome();
            classesWithErrors = jUnitHandler.getClassesWithErrors();
        }
        // thrown when creating the new SAX parser
        catch (ParserConfigurationException | SAXException e) {

            logger.error("[{}{}{}{}", "JUnitParser", " (" + "parseFromXML" + ")]: ",
                    "Parsing Exception while parsing JUnit XML report: ", e.getMessage());

            throw new Exception(e.getMessage());

        }
        // thrown when parsing the file
        catch (IOException e) {

            logger.error("[{}{}{}{}", "JUnitParser", " (" + "parseFromXML" + ")]: ",
                    "IO Exception while parsing JUnit XML report: ", e.getMessage());

            throw new Exception(e.getMessage());
        }

        return new JUnitClassesAndTestsBundle(executedTests, classesWithErrors);
    }

    /**
     * Custom visitor for the XML document. For each method, saves the execution time and outcome
     */
    static class JUnitHandler extends DefaultHandler {


        /**
         * To collect the methods and the percentage coverage of instructions and branches
         */
        private final ArrayList<JUnitTest> testsAndOutcome = new ArrayList<>();

        /**
         * To collect the classes that contained tests that were not executed because of
         * (usually initialization) errors
         */
        private final ArrayList<JUnitClass> classesWithErrors = new ArrayList<>();

        /**
         * since the XML file is visited in order, this variable contains the test
         * element currently being examined
         */
        JUnitTest currentTest = null;

        /**
         * true if the current XML node we are parsing is a class, false if it is a test
         */
        private boolean weAreParsingAClass;

        // Invoked when an XML element is encountered
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {

            // the first element we are interested in is the test
            if (qName.equalsIgnoreCase("testcase")) {

                // for instance: "testName(String, ...)[1]"
                String testFullSignature = attributes.getValue("name");
                String testName;
                String testDesc;

                // happens when test is not a method but a class instead
                // e.g., <testcase name="soot.toolkits.purity.PurityTest" classname="soot.toolkits.purity.PurityTest" time="0">
                // or if the test has "initializationError" as name
                if (testFullSignature.contains(".") || testFullSignature.equals(kInitializationError)) {
                    weAreParsingAClass = true;

                    currentTest = new JUnitTest("class");
                    currentTest.setClassFQN(attributes.getValue("classname"));
                }
                else {
                    weAreParsingAClass = false;

                    // we parse the test full signature name to get the name and the desc, separately
                    int indexOfOpenParenthesis = testFullSignature.indexOf("(");
                    int indexOfCloseParenthesis = testFullSignature.indexOf(")");
                    if (indexOfOpenParenthesis != -1 && indexOfCloseParenthesis != -1) {
                        testName = testFullSignature.substring(0, indexOfOpenParenthesis);
                        testDesc = testFullSignature.substring(indexOfOpenParenthesis, indexOfCloseParenthesis + 1);
                    } else {
                        // if there are no parenthesis, either that's it OR we have something like
                        // "  prefix[0: format "%z" --> ""]  ". So, we get rid of everything after '[' (included)
                        int indexOfBracket = testFullSignature.indexOf('[');
                        if (indexOfBracket != -1)
                            testName = testFullSignature.substring(0, testFullSignature.indexOf('['));
                        else
                            testName = testFullSignature;
                        testDesc = "()";
                    }

                    currentTest = new JUnitTest(testName);
                    currentTest.setDesc(testDesc);
                    double executionTime = Double.valueOf(attributes.getValue("time"));
                    currentTest.setExecutionTime(executionTime);
                    currentTest.addExecutionTime(executionTime);
                    currentTest.setClassFQN(attributes.getValue("classname"));
                    currentTest.setOutcome(PASSED);
                }
            }

            // if we get to a failure or error node, it means that the current test has failed or something went wrong
            else if (qName.equalsIgnoreCase("failure") || qName.equalsIgnoreCase("error"))
                currentTest.setOutcome(FAILED);

            if (qName.equalsIgnoreCase("error")) {
                if (attributes.getValue("type").equals("java.lang.VerifyError")) {
                    currentTest.setOutcome(UNUSABLE);
                }
            }

        }





        @Override
        public void endElement(String uri, String localName, String qName) {

            // when we finish to analyze a test, save it in the array list to return
            // note that we exclude parametrized tests
            if (qName.equalsIgnoreCase("testcase")) {

                if (weAreParsingAClass)
                    classesWithErrors.add(new JUnitClass(currentTest.getClassFQN()));
                else {
                    // remember that the "contains" function is implemented based on the "indexOf" function
                    int indexOf = testsAndOutcome.indexOf(currentTest);
                    if (indexOf >= 0) {
                        testsAndOutcome.get(indexOf).setOutcome(currentTest.getOutcome());
                    } else
                        testsAndOutcome.add(currentTest);
                }
            }
        }

        /**
         * getter for testsAndOutcome
         * @return testsAndOutcome
         */
        public ArrayList<JUnitTest> getTestsAndOutcome() {
            return testsAndOutcome;
        }

        /**
         * getter for classesWithErrors
         * @return classesWithErrors
         */
        public ArrayList<JUnitClass> getClassesWithErrors() {
            return classesWithErrors;
        }
    }
}


