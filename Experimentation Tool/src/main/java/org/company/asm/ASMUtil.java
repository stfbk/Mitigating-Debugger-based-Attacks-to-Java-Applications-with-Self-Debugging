package org.company.asm;

import org.company.App;
import org.company.junit.JUnitTest;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static org.company.asm.MyClassVisitor.normalizeDesc;
import static org.company.junit.Const.*;

public class ASMUtil {

    public static ClassWriter addAnnotationToClass(InputStream fis, boolean isJunit5) throws IOException {

        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(fis);
        classReader.accept(classNode, 0);

        String annotationToAdd = isJunit5 ? kDisabledAnnotationFQN : kIgnoreAnnotationFQN;
        if (classNode.invisibleAnnotations == null)
            classNode.invisibleAnnotations = new ArrayList<>();
        if (classNode.visibleAnnotations == null)
            classNode.visibleAnnotations = new ArrayList<>();

        boolean annotationAlreadyPresent = false;
        AnnotationNode testAnnotationToRemove = null;
        for (AnnotationNode oldAnnotation : classNode.invisibleAnnotations) {
            if (oldAnnotation.desc.equals(annotationToAdd))
                annotationAlreadyPresent = true;
            if (oldAnnotation.desc.equals(kTestAnnotationFQN5) || oldAnnotation.desc.equals(kTestAnnotationFQN4))
                testAnnotationToRemove = oldAnnotation;
        }
        if (!annotationAlreadyPresent)
            classNode.invisibleAnnotations.add(new AnnotationNode(annotationToAdd));
        classNode.invisibleAnnotations.remove(testAnnotationToRemove);


        annotationAlreadyPresent = false;
        testAnnotationToRemove = null;
        for (AnnotationNode oldAnnotation : classNode.visibleAnnotations) {
            if (oldAnnotation.desc.equals(annotationToAdd))
                annotationAlreadyPresent = true;
            if (oldAnnotation.desc.equals(kTestAnnotationFQN5) || oldAnnotation.desc.equals(kTestAnnotationFQN4))
                testAnnotationToRemove = oldAnnotation;
        }
        if (!annotationAlreadyPresent)
            classNode.visibleAnnotations.add(new AnnotationNode(annotationToAdd));
        classNode.visibleAnnotations.remove(testAnnotationToRemove);

        //ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES); if I have the
        // ClassWriter.COMPUTE_FRAMES flag, I get a java.lang.TypeNotPresentException
        // (see https://stackoverflow.com/questions/26573945/classnotfoundexception-at-asm-objectwriter-getcommonsuperclass)
        // so let's try with the COMPUTE_MAXS flag
        //ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        //ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        // https://stackoverflow.com/questions/35145997/asm-5-0-3-with-java-1-8-incorrect-maxstack-with-java-lang-verifyerror-operand-s?noredirect=1#comment58021378_35145997
        ClassWriter classWriter = new ByteCodeWriter(classReader, App.class.getClassLoader(), ClassWriter.COMPUTE_FRAMES);

        classNode.accept(classWriter);
        return classWriter;
    }

    public static boolean thereAreNoMoreTests;


    public static ClassWriter addAnnotationToIgnoreTests(InputStream fis, ArrayList<JUnitTest> tests, boolean annotateAllMethods)
            throws IOException {

        App.logger.info("[{}{}{}", "ASMUtil", " (" + "addAnnotationToIgnoreTests" + ")]: ",
                "adding annotations to tests to ignore (there are " + tests.size() + " tests to ignore)");


        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(fis);
        classReader.accept(classNode, 0);

        ArrayList<JUnitTest> testsWithNewAnnotation = new ArrayList<>();
        thereAreNoMoreTests = true;

        for (MethodNode method : classNode.methods) {

            if (!method.name.equals("<init>") && !method.name.equals("<clinit>")) {

                String name = method.name + normalizeDesc(method.desc);
                boolean annotationAlreadyPresent;

                if (method.invisibleAnnotations == null)
                    method.invisibleAnnotations = new ArrayList<>();
                if (method.visibleAnnotations == null)
                    method.visibleAnnotations = new ArrayList<>();


                boolean thisMethodIsATest = false;
                for (AnnotationNode oldAnnotation : method.invisibleAnnotations) {
                    if (oldAnnotation.desc.equals(kTestAnnotationFQN5) || oldAnnotation.desc.equals(kTestAnnotationFQN4)) {
                        thisMethodIsATest = true;
                        break;
                    }
                }
                for (AnnotationNode oldAnnotation : method.visibleAnnotations) {
                    if (oldAnnotation.desc.equals(kTestAnnotationFQN5) || oldAnnotation.desc.equals(kTestAnnotationFQN4)) {
                        thisMethodIsATest = true;
                        break;
                    }
                }

                boolean thisTestWasProcessed = false;
                for (JUnitTest test: tests) {

                    if (name.equals(test.getMethodName() + test.getDescWithoutReturningType()) || annotateAllMethods) {

                        App.logger.info("[{}{}{}{}", "ASMUtil", " (" + "addAnnotationToIgnoreTests" + ")]: ",
                                "processing annotations of test ", name);
                        thisTestWasProcessed = true;

                        AnnotationNode testAnnotationToRemove = null;
                        String annotationToAdd = test.isJunit5() ? kDisabledAnnotationFQN : kIgnoreAnnotationFQN;

                        for (AnnotationNode oldAnnotation : method.invisibleAnnotations) {
                            if (oldAnnotation.desc.equals(annotationToAdd))
                                annotationAlreadyPresent = true;
                            if (oldAnnotation.desc.equals(kTestAnnotationFQN5) || oldAnnotation.desc.equals(kTestAnnotationFQN4))
                                testAnnotationToRemove = oldAnnotation;
                        }
                        // add annotation in visible annotations
//                        if (!annotationAlreadyPresent) {
//                            method.invisibleAnnotations.add(new AnnotationNode(annotationToAdd));
//                            App.logger.info("[{}{}{}{}{}{}", "ASMUtil", " (" + "addAnnotationToIgnoreTests" + ")]: ",
//                                    "annotating method ", name, " with invisible annotation ",  annotationToAdd);
//                        }
//                        else
//                            App.logger.info("[{}{}{}{}{}", "ASMUtil", " (" + "addAnnotationToIgnoreTests" + ")]: ",
//                                    "annotation ",  annotationToAdd, " is already present in invisible annotations");

                        if (testAnnotationToRemove != null) {
                            method.invisibleAnnotations.remove(testAnnotationToRemove);
                            App.logger.info("[{}{}{}", "ASMUtil", " (" + "addAnnotationToIgnoreTests" + ")]: ",
                                    "@Test annotation was present in invisible annotations, now removed");
                        } else
                            App.logger.info("[{}{}{}", "ASMUtil", " (" + "addAnnotationToIgnoreTests" + ")]: ",
                                    "@Test annotation was not present in invisible annotations");


                        testAnnotationToRemove = null;
                        annotationAlreadyPresent = false;

                        for (AnnotationNode oldAnnotation : method.visibleAnnotations) {
                            if (oldAnnotation.desc.equals(annotationToAdd))
                                annotationAlreadyPresent = true;
                            if (oldAnnotation.desc.equals(kTestAnnotationFQN5) || oldAnnotation.desc.equals(kTestAnnotationFQN4))
                                testAnnotationToRemove = oldAnnotation;
                        }

                        if (!annotationAlreadyPresent) {
                            method.visibleAnnotations.add(new AnnotationNode(annotationToAdd));
                            App.logger.info("[{}{}{}{}{}{}", "ASMUtil", " (" + "addAnnotationToIgnoreTests" + ")]: ",
                                    "annotating method ", name, " with visible annotation ", annotationToAdd);
                        } else
                            App.logger.info("[{}{}{}{}{}", "ASMUtil", " (" + "addAnnotationToIgnoreTests" + ")]: ",
                                    "annotation ", annotationToAdd, " is already present in visible annotations");

                        if (testAnnotationToRemove != null) {

                            method.visibleAnnotations.remove(testAnnotationToRemove);
                            App.logger.info("[{}{}{}", "ASMUtil", " (" + "addAnnotationToIgnoreTests" + ")]: ",
                                    "@Test annotation was present in visible annotations, now removed");
                        } else
                            App.logger.info("[{}{}{}", "ASMUtil", " (" + "addAnnotationToIgnoreTests" + ")]: ",
                                    "@Test annotation was not present in visible annotations");

                        testsWithNewAnnotation.add(test);
                    }
                }

                // if this method was a test, but we did not process it, it means that
                // the class will still contain at least one test
                if (thisMethodIsATest && !thisTestWasProcessed)
                    thereAreNoMoreTests = false;
            }
        }

        tests.removeAll(testsWithNewAnnotation);

        //ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES); if I have the
        // ClassWriter.COMPUTE_FRAMES flag, I get a java.lang.TypeNotPresentException
        // (see https://stackoverflow.com/questions/26573945/classnotfoundexception-at-asm-objectwriter-getcommonsuperclass)
        // so let's try with the COMPUTE_MAXS flag
        //ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        //ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        // https://stackoverflow.com/questions/35145997/asm-5-0-3-with-java-1-8-incorrect-maxstack-with-java-lang-verifyerror-operand-s?noredirect=1#comment58021378_35145997
        ClassWriter classWriter = new ByteCodeWriter(classReader, App.class.getClassLoader(), ClassWriter.COMPUTE_FRAMES);


        classNode.accept(classWriter);
        return classWriter;
    }
}
