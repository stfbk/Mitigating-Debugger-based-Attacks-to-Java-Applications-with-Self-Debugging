package org.company.jar;


import org.company.App;
import org.company.asm.*;
import org.company.jacoco.JaCoCoMethod;
import org.company.junit.JUnitClass;
import org.company.junit.JUnitTest;
import org.company.junit.JUnitUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.*;
import java.util.*;

import static java.lang.System.exit;
import static org.objectweb.asm.Opcodes.ASM8;

/**
 * This is a general purpose utility class for interacting with jar files
 */
public class JarUtil {

    /**
     * Extract all files from a jar archive in the given output directory. If the output directory does not
     * already exist, the method creates it. Note that extracted files with the same name override already present files
     * @param outputDirectory where to extract all files
     * @param pathOfJarFile the jar archive from which to extract all files
     * @throws IOException if the creation of the output directory or the extraction of files failed
     */
    public static void extractAllFilesFromJar(File outputDirectory, String pathOfJarFile) throws IOException {

        App.logger.info("[{}{}{}{}{}{} ", "FileUtil", "(" + "extractAllFilesFromJar" + ")]: ", "extracting all " +
                        "files from jar ", pathOfJarFile, " into directory ", outputDirectory.getAbsolutePath());

        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdirs())
                throw new IOException("given output folder did not exist and cannot be created");
        }
        else if (!outputDirectory.isDirectory())
            throw new IOException("given output folder is not a folder");

        try {

            ProcessBuilder pbJarXF;

            pbJarXF = new ProcessBuilder("jar", "xf", pathOfJarFile);
            pbJarXF.directory(outputDirectory).redirectErrorStream(true);
            Process pJarXF = pbJarXF.start();

            // here we must NOT wait for the process to finish through "p.waitFor();"
            // because the output of the process could be too big for the buffer size
            // provided by the underlying platform. So we "drain" the buffer instead,
            // that in the end has the same effect of p.waitFor();
            BufferedReader jarXFReader = new BufferedReader(new InputStreamReader(pJarXF.getInputStream()));

            String temp;

            while ((temp = jarXFReader.readLine()) != null) {

                // if the output from the extraction contains the word "Exception", it means that something went wrong
                if (temp.toLowerCase().contains("Exception".toLowerCase())) {

                    App.logger.error("[{}{}{}{}", "FileUtil ", "(" + "extractAllFilesFromJar" + ")]: ",
                            "exception while extracting files from jar: ", temp);

                    throw new IOException("exception while extracting files from jar: " + temp);
                }
            }

            pJarXF.waitFor();

            // check also that the code is 0, otherwise there was an error
            int exitStatus = pJarXF.exitValue();

            if (exitStatus != 0) {
                App.logger.error("[{}{}{}{}", "FileUtil ", "(" + "extractAllFilesFromJar" + ")]: ",
                        "jar process exited with code: ", exitStatus);

                exit(10);
            }
        }
        catch (InterruptedException e) {

            App.logger.error("[{}{}{}{}", "FileUtil ", "(" + "extractAllFilesFromJar" + ")]: ",
                    "Exception while waiting for process to finish", e.getMessage());
            exit(5);
        }
    }

    /**
     * Create a jar file with all the files in the given directory
     * @param directoryContainingFiles directory containing files to add in the new jar
     * @param manifest the manifest file to be included. If not given, it will be assumed to be
     *                 'directoryContainingFiles/META-INF/MANIFEST.MF'
     * @param jarAbsolutePath the path for the jar file that will be created
     * @throws IOException if the creation of the jar failed
     */
    public static void createJar(File directoryContainingFiles, File manifest, String jarAbsolutePath) throws IOException {

        App.logger.info("[{}{}{}{}{}{} ", "FileUtil", "(" + "createJar" + ")]: ", "create jar with name: ",
                jarAbsolutePath, " from files contained in ", directoryContainingFiles.getAbsolutePath());

        try {

            if (manifest == null)
                manifest = new File(directoryContainingFiles.getAbsolutePath() + "/META-INF/MANIFEST.MF");

            ProcessBuilder pbJarCF;

            pbJarCF = new ProcessBuilder("jar", "cmf", manifest.getAbsolutePath(), jarAbsolutePath, "./");
            pbJarCF.directory(directoryContainingFiles).redirectErrorStream(true);
            Process pJarCF = pbJarCF.start();

            BufferedReader jarXFReader = new BufferedReader(new InputStreamReader(pJarCF.getInputStream()));
            String temp;

            // here we must NOT wait for the process to finish through "p.waitFor();"
            // because the output of the process could be too big for the buffer size
            // provided by the underlying platform. So we "drain" the buffer instead,
            // that in the end has the same effect of p.waitFor();
            while ((temp = jarXFReader.readLine()) != null) {

                // if the output from the extraction contains the word "Exception", it means that something went wrong
                if (temp.toLowerCase().contains("Exception".toLowerCase())) {

                    App.logger.error("[{}{}{}{}", "FileUtil ", "(" + "createJar" + ")]: ",
                            "exception while creating jar: ", temp);

                    throw new IOException("exception while creating jar: " + temp);
                }
            }

            pJarCF.waitFor();

            // check also that the code is 0, otherwise there was an error
            int exitStatus = pJarCF.exitValue();

            if (exitStatus != 0) {
                App.logger.error("[{}{}{}{}", "FileUtil ", "(" + "createJar" + ")]: ",
                        "jar process exited with code: ", exitStatus);

                exit(10);
            }
        }
        catch (InterruptedException e) {

            App.logger.error("[{}{}{}{}", "FileUtil ", "(" + "createJar" + ")]: ",
                    "Exception while waiting for process to finish", e.getMessage());
            exit(5);
        }
    }

    /**
     * get the jar name from a path to the jar on file system
     * example: c://Users/stefy/java/program.jar => program
     * @param jarPath the path in the file system to the jar file
     * @return the name of the jar (without extension)
     */
    public static String getJarNameFromPath(String jarPath) {
        return jarPath.substring(jarPath.lastIndexOf('/') + 1, jarPath.lastIndexOf('.'));
    }









    /**
     * This method extracts all class files from the given jar. Then, it visits all classes to
     * add the @Ignore (JUnit 4) or @Disabled (Junit 5) annotation to the given tests. Finally,
     * it creates the new jar with the given name in the given directory
     * @param testsToIgnore the hash map containing the JUnit tests methods to ignore
     * @param classesToIgnore the array of the classes to ignore
     * @param pathOfJarFileToVisit the absolute path of the jar file to visit
     * @param outputFolderPath the path of the folder that will contain the output of this method
     * @param newJarName the name that the visited jar will have (without ".jar" extension)
     * @param requireAllTestsToBeProcessed true if all methods must be found and processed
     * @param classesNotToExclude classes not to exclude from the tests
     * @return the new jar visited file
     */
    public static File createJarWithIgnoredTests(HashMap<String, ArrayList<JUnitTest>> testsToIgnore,
                                                 ArrayList<JUnitClass> classesToIgnore,
                                                 String pathOfJarFileToVisit, String outputFolderPath, String newJarName,
                                                 boolean requireAllTestsToBeProcessed,
                                                 Set<String> classesNotToExclude) throws IOException {


        App.logger.info("[{}{}{}{}{}", "JarUtil", " (" + "createJarWithIgnoredTests" + ")]: ",
                "start parsing jar", pathOfJarFileToVisit, " to add annotations to given tests and classes");

        // extract all files from the Jar in the given folder
        File outputFolder = new File(outputFolderPath);
        extractAllFilesFromJar(outputFolder, pathOfJarFileToVisit);
        HashSet<File> classFiles = new HashSet<>();
        getAllFilesFromDirectoryWithExtension(outputFolder, "class", classFiles);

        // we have to visit all classes at least twice. This is because some tests that we think are used
        // by a class could actually be defined in one of the superclasses. Since we do not know it, we
        // first have to parse all classes and determine whether a method used by a class is defined by
        // that class or not. If not, we recursively check the superclass(es) until we arrive at the
        // definition of the method. Then, we can actually start applying the visitor to the methods.

        boolean doWeHaveMoreTestsToProcess = true;

        while (doWeHaveMoreTestsToProcess) {

            doWeHaveMoreTestsToProcess = false;

            for (File classFile : classFiles) {

                // calculate the FQN of the class (e.g., "org.example.className") from the path of
                // the class file (e.g., "C://users/bin/jars/org/example/className.class")
                String classFQN = classFile.getAbsolutePath()
                        .replace(outputFolderPath + "/", "")
                        .replace("/", ".")
                        .replace(".class", "");

                // this means that we have some tests to process that are (supposedly) defined in this class
                if (testsToIgnore.containsKey(classFQN)) {

                    FileInputStream classFileInputStream = new FileInputStream(classFile);

                    ArrayList<JUnitTest> arrayOfTestsToIgnore = testsToIgnore.get(classFQN);

                    // check which methods are defined in the class are which are defined in a superclass
                    areMethodsDefinedInThisClassASMAdapter classVisitor =
                            new areMethodsDefinedInThisClassASMAdapter(ASM8, JUnitUtil.toASMMethods(arrayOfTestsToIgnore));
                    ClassReader classReader = new ClassReader(classFileInputStream);
                    classReader.accept(classVisitor, 0);
                    classFileInputStream.close();

                    ArrayList<ASMMethod> testsThatWereNotFound = classVisitor.getMethodsThatWereNotFound();

                    if (testsThatWereNotFound.size() != 0) {

                        String superClassName = classVisitor.getSuperClassName().replace("/", ".");

                        if (!superClassName.equals("java.lang.Object")) {

                            App.logger.info("[{}{}{}{}{}{}{}", "JarUtil", " (" + "createJarWithIgnoredTests" + ")]: ",
                                    "some tests were not found (one per line below), moving them from class ", classFQN,
                                    " to superclass ", superClassName, ": ");
                            testsThatWereNotFound.forEach(asmMethod -> App.logger.info("    {}", asmMethod.toString()));

                            // note that the superclass may not be present as key in the "testsToIgnore" map,
                            testsToIgnore.putIfAbsent(superClassName, new ArrayList<>());
                            ArrayList<JUnitTest> superclassMethods = testsToIgnore.get(superClassName);

                            // then, we add the methods that were not found to the superclass, if not already there
                            for (ASMMethod method : testsThatWereNotFound) {

                                JUnitTest test = (JUnitTest) method;
                                int indexOf = superclassMethods.indexOf(test);
                                if (indexOf < 0) {

                                    // this is the step in which we actually remove the test from the class that use
                                    // it and assign the test to the class that defines it
                                    if (!arrayOfTestsToIgnore.remove(test)) {
                                        App.logger.error("[{}{}{}", "JarUtil", " (" + "createJarWithIgnoredTests" + ")]: ",
                                                "method were not found to remove, this (should be) impossible");
                                        exit(6);
                                    }
                                    test.setClassFQN(superClassName);
                                    superclassMethods.add(test);
                                }
                            }

                            doWeHaveMoreTestsToProcess = true;
                        }
                        else {
                            if (requireAllTestsToBeProcessed) {
                                App.logger.error("[{}{}{}{}{}{}", "JarUtil", " (" + "createJarWithIgnoredTests" + ")]: ",
                                        "some methods were not found (one per line below), ",
                                        "requireAllMethodsToBeProcessed flag is true but the superclass of class ",
                                        classFQN, " is \"java.lang.Object\"");
                                testsThatWereNotFound.forEach(method -> App.logger.error("    {}", method));
                                exit(6);
                            }
                        }
                    }
                }
            }
        }


        // then, after that we moved all tests to the class that defines them, we can now process them
        App.logger.info("[{}{}{}", "JarUtil", " (" + "createJarWithIgnoredTests" + ")]: ",
                "after we moved all tests to the superclasses, we can process them");


        boolean someMethodsWereNotProcessed = false;

        // for each class file, we first check whether there it contains a test to ignore
        // if not, we do not modify the class. Otherwise, we go through the class with ASM
        // and visit it with the given class visitor
        for (File classFile : classFiles) {

            // calculate the FQN of the class (e.g., "org.example.className") from the path of
            // the class file (e.g., "C://users/bin/jars/org/example/className.class")
            String classFQN = classFile.getAbsolutePath()
                    .replace(outputFolderPath + "/", "")
                    .replace("/", ".")
                    .replace(".class", "");

            // we acquire the stream pointing to the class file. Then, we pass this stream to the class
            // reader that will read the bytecode and trigger the events for the class visitor
            FileInputStream classFileInputStream = new FileInputStream(classFile);

            ClassWriter classWriter = null;
            boolean classIsToOverWrite = true;


            // check whether we have the class to add the annotation to
            ArrayList<JUnitClass> setOfClassesToIgnore = new ArrayList<>();
            for (JUnitClass tempClass : classesToIgnore) {

                // because we may have something like "org.apache.avro.TestSchemaNormalization$TestFingerprint"
                // but since we take the name of the class from the file, we have to match it with  the whole class,
                // not inner classes => "org.apache.avro.TestSchemaNormalization"
                String completeClassFQN = tempClass.getClassFQN();
                int eventualIndexOfInnerClass = completeClassFQN.indexOf("$");
                String classFQNWithoutInternalClasses = (eventualIndexOfInnerClass == -1) ?
                        completeClassFQN :
                        completeClassFQN.substring(0, eventualIndexOfInnerClass);

                if (classFQNWithoutInternalClasses.equals(classFQN))
                    setOfClassesToIgnore.add(tempClass);
            }

            if (!setOfClassesToIgnore.isEmpty()) {

                App.logger.info("[{}{}{}{}{}", "JarUtil", " (" + "createJarWithIgnoredTests" + ")]: ",
                        "whole class ", classFQN, " is to be filtered");

                classWriter = ASMUtil.addAnnotationToClass(classFileInputStream, setOfClassesToIgnore.get(0).isJunit5());
                setOfClassesToIgnore.forEach(classesToIgnore::remove);

                ArrayList<JUnitTest> temp = new ArrayList<>();
                JUnitTest tempTest = new JUnitTest("<because we annotate all>");
                tempTest.setDesc("<because we annotate all>");
                temp.add(tempTest);

                // just to be sure and avoid errors, we ignore also all tests within the class
                classWriter = ASMUtil.addAnnotationToIgnoreTests(
                        new ByteArrayInputStream(classWriter.toByteArray()), temp, true);

                // just to be sure
                testsToIgnore.remove(classFQN);
            }
            else if (testsToIgnore.containsKey(classFQN)) {

                App.logger.info("[{}{}{}{}{}", "JarUtil", " (" + "createJarWithIgnoredTests" + ")]: ",
                        "visiting class ", classFQN, " to add annotation to ignore tests");

                ArrayList<JUnitTest> arrayMethods = testsToIgnore.get(classFQN);

                App.logger.info("[{}{}{}{}{}", "JarUtil", " (" + "createJarWithIgnoredTests" + ")]: ",
                        "Class ", classFQN, " has " + arrayMethods.size() + " tests to ignore");

                classWriter = ASMUtil.addAnnotationToIgnoreTests(classFileInputStream, arrayMethods, false);

                // true if there are still methods to process
                if (arrayMethods.size() != 0) {
                    someMethodsWereNotProcessed = true;
                    App.logger.info("[{}{}{}", "JarUtil", " (" + "createJarWithIgnoredTests" + ")]: ",
                            "the following methods were not processed:");
                    arrayMethods.forEach(jUnitTest -> App.logger.info("    {}", jUnitTest.toString()));
                }

                // IF THE CLASS IS NOT IN THE SET OF CLASSES TO KEEP,
                // we have to say to JUnit to ignore this class, otherwise
                // JUnit will try to execute the tests in the class (but
                // since the class has no tests, JUnit would throw an error)
                boolean contain = false;
                for (String tempClass: classesNotToExclude) {
                    if (tempClass.equals(classFQN)) {
                        contain = true;
                        break;
                    }
                }

                if (ASMUtil.thereAreNoMoreTests && !contain) {

                    App.logger.info("[{}{}{}", "JarUtil", " (" + "createJarWithIgnoredTests" + ")]: ",
                            "we filtered out all tests from the class, so we ignore the whole class " + classFQN + " as well");

                    // add to the class both the @Ignore and the @Disabled annotation
                    classWriter = ASMUtil.addAnnotationToClass(new ByteArrayInputStream(classWriter.toByteArray()), true);
                    classWriter = ASMUtil.addAnnotationToClass(new ByteArrayInputStream(classWriter.toByteArray()), false);
                }

                testsToIgnore.remove(classFQN);
            }
            else {
                // too verbose
//                App.logger.info("[{}{}{}{}{}", "JarUtil", " (" + "createJarWithIgnoredTests" + ")]: ",
//                        "class ", classFQN, " does not contain anything that has to be processed");
                classIsToOverWrite = false;
            }


            if (classIsToOverWrite) {
                OutputStream classFileOutputStream = new FileOutputStream(classFile);
                classFileOutputStream.write(classWriter.toByteArray());
                classFileOutputStream.flush();
                classFileOutputStream.close();
            }

            classFileInputStream.close();
        }


        if (requireAllTestsToBeProcessed && (someMethodsWereNotProcessed || !classesToIgnore.isEmpty())) {
            App.logger.error("[{}{}{}{}{}{}", "JarUtil", " (" + "createJarWithIgnoredTests" + ")]: ",
                    someMethodsWereNotProcessed ? "not all tests" : "",
                    someMethodsWereNotProcessed && !classesToIgnore.isEmpty() ? " and " : "",
                    !classesToIgnore.isEmpty() ? "not all classes" : "", " were processed. Unfortunately, it was required.");
            exit(6);
        }

        // now we can create the jar archive back
        String visitedJarPath = outputFolderPath + "/" + newJarName + ".jar";
        createJar(outputFolder, null, visitedJarPath);
        return new File(visitedJarPath);
    }









    /**
     * This method extracts all class files from the given jar. Then, it visits all classes to
     * find the method to protect and add the "assert false" instruction. Finally,
     * it creates the new jar with the given name in the given directory
     * @param methodToProtect the method to protect
     * @param pathOfJarToVisit the path of the jar to visit
     * @param outputFolderPath the path of the folder that will contain the output of this method
     * @param newJarName the name that the visited jar will have (without ".jar" extension)
     * @return the new jar visited file
     */
    public static File createJarWithAssertFalseAnnotation (JaCoCoMethod methodToProtect, String pathOfJarToVisit,
                                                          String outputFolderPath, String newJarName) throws IOException {

        // extract all files from the Jar in the given folder
        File outputFolder = new File(outputFolderPath);
        extractAllFilesFromJar(outputFolder, pathOfJarToVisit);
        HashSet<File> classFiles = new HashSet<>();
        getAllFilesFromDirectoryWithExtension(outputFolder, "class", classFiles);

        boolean stillToFindClassThatDefinesMethod = true;
        boolean didWeFindMethod;

        while (stillToFindClassThatDefinesMethod) {

            didWeFindMethod = false;

            String classFQN = methodToProtect.getClassFQN();
            String classFQNToFind = outputFolderPath + "/"
                    + classFQN.replace(".", "/")
                    + ".class";

            if (classFiles.removeIf(file -> file.getAbsolutePath().equals(classFQNToFind))) {

                File classFile = new File(classFQNToFind);
                FileInputStream classFileInputStream = new FileInputStream(classFile);
                didWeFindMethod = true;

                // visit the class to check if the method is defined
                areMethodsDefinedInThisClassASMAdapter classVisitor =
                        new areMethodsDefinedInThisClassASMAdapter(ASM8, methodToProtect);
                ClassReader classReader = new ClassReader(classFileInputStream);
                classReader.accept(classVisitor, 0);
                classFileInputStream.close();

                if (classVisitor.getMethodsThatWereFound().size() == 1) {
                    stillToFindClassThatDefinesMethod = false;

                    App.logger.info("[{}{}{}{}{}{}", "JarUtil", " (" + "createJarWithAssertFalseAnnotation" + ")]: ",
                            "visiting class ", classFQN, " to add 'assert false' to method ",
                            methodToProtect.getMethodName());

                    // https://stackoverflow.com/questions/35145997/asm-5-0-3-with-java-1-8-incorrect-maxstack-with-java-lang-verifyerror-operand-s?noredirect=1#comment58021378_35145997
                    ClassWriter classWriterToAddAssertFalse = new ByteCodeWriter(classReader, App.class.getClassLoader(), ClassWriter.COMPUTE_FRAMES);

                    AddAssertFalseAdapterClass classVisitorToAddAssertFalse =
                            new AddAssertFalseAdapterClass(ASM8, classWriterToAddAssertFalse, methodToProtect);

                    FileInputStream classFileInputStreamToAddAssertFalse = new FileInputStream(classFile);
                    ClassReader classReaderToAddAssertFalse = new ClassReader(classFileInputStreamToAddAssertFalse);
                    classReaderToAddAssertFalse.accept(classVisitorToAddAssertFalse, 0);
                    classFileInputStream.close();

                    OutputStream classFileOutputStreamToAddAssertFalse = new FileOutputStream(classFile);
                    classFileOutputStreamToAddAssertFalse.write(classWriterToAddAssertFalse.toByteArray());
                    classFileOutputStreamToAddAssertFalse.flush();
                    classFileOutputStreamToAddAssertFalse.close();
                }
                else {
                    String superClassName = classVisitor.getSuperClassName().replace("/", ".");

                    if (superClassName.equals("java.lang.Object"))
                        didWeFindMethod = false;
                    else {
                        App.logger.info("[{}{}{}{}{}{}{}", "JarUtil", " (" + "createJarWithAssertFalseAnnotation" + ")]: ",
                                "the method to protect was not found, moving it from class ", classFQN,
                                " to superclass ", superClassName, ": ");
                        methodToProtect.setClassFQN(superClassName);
                    }
                }
            }

            if (!didWeFindMethod) {
                App.logger.error("[{}{}{}", "JarUtil", " (" + "createJarWithAssertFalseAnnotation" + ")]: ",
                        "the method to protect was not found in any class");
                exit(6);
            }
        }

        // now we can create the jar archive back
        String visitedJarPath = outputFolderPath + "/" + newJarName + ".jar";
        createJar(outputFolder, null, visitedJarPath);
        return new File(visitedJarPath);
    }




    /**
     * Utility method to recursively get a pointer to all files inside the given directory
     * @param filesDirectory the folder where to search for the files
     * @param extension the extension of the files to retrieve
     * @param setOfFiles the set in which all files will be inserted
     */
    public static void getAllFilesFromDirectoryWithExtension(final File filesDirectory, String extension, HashSet<File> setOfFiles) {

        if (filesDirectory.exists()) {
            try {

                // for each file in the directory, we check whether it is a folder (in that case, we recursively invoke
                // this function) or an actual file (in that case, we check whether it is a file to retrieve or not)
                for (final File potentialFile : Objects.requireNonNull(filesDirectory.listFiles())) {

                    if (potentialFile.isDirectory())
                        getAllFilesFromDirectoryWithExtension(potentialFile, extension, setOfFiles);

                    else {
                        if (potentialFile.getAbsolutePath().endsWith(extension))
                            setOfFiles.add(potentialFile);
                    }
                }
            }
            // catch an eventual null pointer exception, it may be thrown if our process does not have
            // enough privileges to read the folder
            catch (NullPointerException e) {

                App.logger.error("[{}{}{}{}{}{}", "FileUtil", " (" + "getAllFiles" + ")]: ",
                        "the directory ", filesDirectory.getAbsolutePath(),
                        " could not be accessed (probable too low permissions):", e.getMessage());
                exit(2);
            }
        }
    }

}