package info.kgeorgiy.ja.advanced.Maksonov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Implementor implements Impler {

    private final String ENTER = System.lineSeparator();
    private final String DOUBLE_ENTER = ENTER + ENTER;
    private final String TAB = "\t";

    public static void main(String[] args) {
        Implementor implementor = new Implementor();
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("Error: invalid arguments. Expected: <class> <path>.");
            return;
        }
        try {
            implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
        } catch (ImplerException | ClassNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        // check if we can implement given class
        if (token.isPrimitive()) {
            throw new ImplerException("Error: cannot implement primitive.");
        } else if (Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("Error: cannot implement private interfaces.");
        }
        // package and file name
        Path packagePath, filePath;
        try {
            packagePath = root.resolve(token.getPackageName().replace(".", File.separator));
            filePath = packagePath.resolve(token.getSimpleName() + "Impl" + ".java");
        } catch (InvalidPathException e) {
            throw new ImplerException("Error: invalid path. ", e);
        }
        // creating package
        try {
            Files.createDirectories(packagePath);
        } catch (IOException e) {
            throw new ImplerException("Error: cannot create package. ", e);
        }
        // write to file
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            writer.write(buildClass(token));
        } catch (IOException e) {
            throw new ImplerException("Error: cannot write to file.", e.getCause());
        }
    }

    //===================================================================//

    private String buildClass(Class<?> token) {
        StringBuilder stringBuilder = new StringBuilder();
        // header: package, name
        stringBuilder.
                append("package ").
                append(token.getPackageName()).
                append(";").
                append(DOUBLE_ENTER).
                append("public class ").
                append(token.getSimpleName()).append("Impl").append(" implements ").
                append(token.getCanonicalName()).append(" {").
                append(DOUBLE_ENTER);
        // body: methods
        Method[] methods = token.getMethods();
        for (Method method : methods) {
            stringBuilder.
                    append(buildMethod(method)).
                    append(DOUBLE_ENTER);
        }
        // tail: closing class
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    //===================================================================//

    private String buildMethod(Method method) {
        StringBuilder stringBuilder = new StringBuilder();
        //header: return type, name, parameters, exceptions
        stringBuilder.
                append(TAB).
                append("public ").
                append(method.getReturnType().getCanonicalName()).
                append(" ").
                append(method.getName()).
                append(buildMethodParameters(method.getParameters())).
                append(buildMethodThrows(method.getExceptionTypes())).
                append("{").
                append(ENTER);
        //body: return with default return type value
        stringBuilder.
                append(TAB + TAB).
                append(buildMethodBody(method.getReturnType())).
                append(ENTER);
        //tail: closing method
        stringBuilder.
                append(TAB).
                append("}");
        return stringBuilder.toString();
    }

    private String buildMethodParameters(Parameter[] parameters) {
        StringBuilder stringBuilder = new StringBuilder("(");
        int nom = 0;
        for (Parameter parameter : parameters) {
            stringBuilder.append(parameter.getType().getCanonicalName()).append(" ").append(parameter.getName());
            if (nom < parameters.length - 1) {
                stringBuilder.append(", ");
            }
            nom++;
        }
        return stringBuilder.append(") ").toString();
    }

    private String buildMethodThrows(Class<?>[] exceptionTypes) {
        if (exceptionTypes.length == 0) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder("throws ");
        int nom = 0;
        for (Class<?> exception : exceptionTypes) {
            stringBuilder.append(exception.getName());
            if (nom < exceptionTypes.length - 1) {
                stringBuilder.append(", ");
            }
            nom++;
        }
        return stringBuilder.append(" ").toString();
    }

    private String buildMethodBody(Class<?> returnType) {
        StringBuilder stringBuilder = new StringBuilder("return");
        String returnValue = "";
        if (returnType.isPrimitive()) {
            if (returnType.equals(Boolean.TYPE)) {
                returnValue = " false";
            } else if (!returnType.equals(Void.TYPE)) {
                returnValue = " 0";
            }
        } else {
            returnValue = " null";
        }
        return stringBuilder.append(returnValue).append(";").toString();
    }
}
