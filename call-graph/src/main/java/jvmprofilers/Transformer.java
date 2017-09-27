package jvmprofilers;

import javassist.*;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class Transformer implements ClassFileTransformer {

    // packages=com.foo,gr.bar.foo;output=/tmp/call-graph.dot
    final static String PACKAGES = "packages";
    static String[] packages = null;

    final static String OUTPUT = "output";
    static String output = null;

    public static void premain(String argument, Instrumentation instrumentation) {
        if (argument == null) {
            err("Missing arguments (" + PACKAGES + ";" + OUTPUT + ")");
            return;
        }

        String[] tokens = argument.split(";");
        if (tokens.length < 1) {
            err("Missing delimiter ';' + (" + argument + ")");
            return;
        }

        for (String token : tokens) {
            String[] args = token.split("=");
            if (args.length < 2) {
                err("Missing argument delimiter '=' (" + token + ")");
                return;
            }

            String argtype = args[0];
            if (argtype.equalsIgnoreCase(PACKAGES)) {
                packages = args[1].split(",");
            } else if (argtype.equalsIgnoreCase(OUTPUT)) {
                output = args[1];
            } else {
                err("Wrong argument (" + argtype + ")");
                return;
            }
        }

        try {
            if (null == output) output = "/tmp/call-graph.dot";
            CallStack.dot = new BufferedWriter(new FileWriter(output), 8192);
            CallStack.dot.write("digraph CallGraph {\n");
            CallStack.dot.flush();

            instrumentation.addTransformer(new Transformer());
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] bytes) throws IllegalClassFormatException {
        String name = className.replace("/", ".");
        if (name.equalsIgnoreCase("jvmprofilers.CallStack")) return bytes;

        if (packages == null) return transformClass(bytes);

        for (String p : packages) {
            if (name.startsWith(p)) return transformClass(bytes);
        }

        return bytes;
    }

    private byte[] transformClass(byte[] bytes) {
        ClassPool pool = ClassPool.getDefault();
        CtClass clazz = null;

        try {
            clazz = pool.makeClass(new ByteArrayInputStream(bytes));
            if (!clazz.isInterface()) {
                for (CtBehavior m : clazz.getDeclaredBehaviors()) {
                    if (m.isEmpty()) continue;
                    transformMethod(m, clazz.getName());
                }
                bytes = clazz.toBytecode();
            }
        } catch (CannotCompileException e) {
            e.printStackTrace();
            err("Cannot compile: " + e.getMessage());
        } catch (NotFoundException e) {
            e.printStackTrace();
            err("Cannot find: " + e.getMessage());
        } catch (IOException e) {
            err("Error writing: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (clazz != null) {
                clazz.detach();
            }
        }
        return bytes;
   }

    private void transformMethod(CtBehavior method, String className) throws NotFoundException, CannotCompileException {
        String name = className.substring(className.lastIndexOf('.') + 1, className.length());
        name = name.replace('$', '_');

        String methodName = method.getName();
        methodName = methodName.replace('$', '_');

        method.insertBefore("jvmprofilers.CallStack.push(\"" + name + "." + methodName + "\");");
        method.insertAfter("jvmprofilers.CallStack.pop();");
    }

    private static void err(String msg) {
        System.err.println("[CALL-GRAPH ERROR]: " + msg);
    }
}
