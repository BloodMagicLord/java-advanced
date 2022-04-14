package info.kgeorgiy.ja.Maksonov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

public class Implementor implements Impler, JarImpler {

    private static final String ENTER = System.lineSeparator();
    private static final String DOUBLE_ENTER = ENTER + ENTER;
    private static final String TAB = "\t";

    private static final String FILE_SEPARATOR = File.separator;
    private static final char FILE_SEPARATOR_CHAR = File.separatorChar;
    private static final String FILE_PATH_SEPARATOR = File.pathSeparator;

    private static final Path TEMP_PATH = Paths.get("./temp");
    private static final String ENCODING_STRING = "UTF8";
    private static final String JAVA_EXTENSION = ".java";
    private static final String CLASS_EXTENSION = ".class";
    private static final String JAR_EXTENSION = ".jar";

    private static final JavaCompiler COMPILER = ToolProvider.getSystemJavaCompiler();

    //===================================================================//

    /**
     * Runs implemeting alrorithms with given {@code args}.
     * <p>
     * Runs {@code implementJar()} if {@code args} can be applied as "-jar class path".
     * Runs {@code implement()} if  {@code args} can be applied as "class path".
     * Otherwise, saying why arguments is invalid
     *
     * @param args arguments for running from terminal.
     */
    public static void main(String[] args) {
        final String expectedArgs = "Expected [-jar] <class> <path>.";
        Implementor implementor = new Implementor();

        try {
            if (args != null) {
                if (args.length == 2) {
                    implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
                } else if (args.length == 3 && args[0].equals("-jar")) {
                    implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
                }
            }
            System.err.println("Error: invalid foramt of args. " + expectedArgs);
        } catch (ClassNotFoundException e) {
            System.err.println("Error: invalid class argument. " + expectedArgs);
        } catch (InvalidPathException e) {
            System.err.println("Error: invalid path argument. " + expectedArgs);
        } catch (ImplerException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Implement {@code token} and save it to {@code root}
     * <p>
     * @param token type token to create implementation for.
     * @param root root directory.
     * @throws ImplerException if {@param token} is private or primitive.
     *                         if {@param root} is invalid.
     *                         if any other error occurred.
     * @see java.lang.Class
     * @see java.nio.file.Path
     */
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
            packagePath = root.resolve(buildPackageName(token.getPackageName()));
            filePath = packagePath.resolve(buildFileName(token.getSimpleName()) + JAVA_EXTENSION);
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

    /**
     * Implement {@code token} and save it to {@code jarFile} as <var>.jar</var> file.
     * <p>
     * Generated class name will be same as classes name of the type token with <var>Impl</var> suffix added.
     *
     * @param token type token to create implementation for.
     * @param jarFile target <var>.jar</var> file.
     * @throws ImplerException if error occures while implementing.
     *                         if something gone wrong while working with files.
     *
     * @see java.lang.Class
     * @see java.nio.file.Path
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        //preparing before creating
        if (COMPILER == null) {
            throw new ImplerException("Error: compiler not found.");
        }

        //creating temporary directory
        try {
            if (!Files.exists(TEMP_PATH)) {
                Files.createDirectory(TEMP_PATH);
            }
        } catch (IOException e) {
            throw new ImplerException("Error: cannot create temp directory.", e);
        }

        //working
        implement(token, TEMP_PATH);
        Path classPath;
        String className, pathJavaFile, classPathString;

        className = Paths
                .get(buildPackageName(token.getPackageName()))
                .resolve(buildFileName(token.getSimpleName()))
                .toString();
        classPath = Paths
                .get(TEMP_PATH.resolve(className) + ".class");
        pathJavaFile = TEMP_PATH
                .resolve(className) + JAVA_EXTENSION;

        try {
            classPathString = Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (URISyntaxException e) {
            throw new ImplerException("Error: cannot get class path.", e);
        }

        // compiling
        //System.out.println(className + "\n" + pathJavaFile + "\n" + classPathString);
        String[] args = {"-cp", classPathString, pathJavaFile, "-encoding", ENCODING_STRING};
        if (COMPILER.run(null, null, null, args) != 0) {
            throw new ImplerException("Error: compile error.");
        }

        // creating jar
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "Implementor");
        manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VENDOR, "Maksonov Artem");

        try (final JarOutputStream writer = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            writer.putNextEntry(
                    new ZipEntry(className.replace(FILE_SEPARATOR_CHAR, '/') + CLASS_EXTENSION)
            );
            Files.copy(classPath, writer);
            writer.closeEntry();
        } catch (IOException e) {
            throw new ImplerException("Error: cannot write to .jar file.", e);
        }

        // deleting temporary directory
        if (!recursiveDelete(TEMP_PATH.toFile())) {
            System.err.println("Error: some temporary files was not deleted.");
        }
    }

    //==================================================================================//
    /**
     * Returns true if {@code file} was deleted succesfully. False otherwise.
     * <p>
     * If {@code file} is directory, deletes this directory recursivly with all content in it.
     * Otherwise, deletes single file {@code file}.
     *
     * @param file File or directory to delete.
     * @return true if {@code file} was deleted succesfully, false otherwise.
     *
     * @see java.io.File
     */
    private boolean recursiveDelete(File file) {
        File[] content = file.listFiles();
        if (content != null) {
            for (File f : content) {
                recursiveDelete(f);
            }
        }
        return file.delete();
    }

    /**
     * Returns {java.lang.StringBuilder} with realization of class {@code token}.
     *
     * @param token class to implement.
     * @return String with {@param token} realization.
     *
     * @see java.lang.Class
     */
    private String buildClass(Class<?> token) {
        StringBuilder stringBuilder = new StringBuilder();
        // header: package, name
        String packageName = token.getPackageName();
        if (!packageName.equals("")) {
            stringBuilder.
                    append("package ").
                    append(token.getPackageName()).
                    append(";").
                    append(DOUBLE_ENTER).
                    append("public class ").
                    append(token.getSimpleName()).append("Impl").append(" implements ").
                    append(token.getCanonicalName()).append(" {").
                    append(DOUBLE_ENTER);
        }
        // body: methods
        Method[] methods = token.getMethods();
        for (Method method : methods) {
            if (Modifier.isPublic(method.getModifiers())) {
                buildMethod(method, stringBuilder);
                stringBuilder.append(DOUBLE_ENTER);
            }
        }
        // tail: closing class
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    /**
     * Appends to the {@code stringBuilder} {@code method} default realization
     *
     * @param method method to build
     * @param stringBuilder java.lang.StringBuilder in which should return statement be appended
     *
     * @see java.lang.reflect.Method
     * @see java.lang.StringBuilder
     */
    private void buildMethod(Method method, StringBuilder stringBuilder) {
        //header: return type, name, parameters, exceptions
        stringBuilder.
                append(TAB).
                append("public ").
                append(method.getReturnType().getCanonicalName()).
                append(" ").
                append(method.getName());
        buildMethodParameters(method.getParameters(), stringBuilder);
        buildMethodThrows(method.getExceptionTypes(), stringBuilder);
        stringBuilder.
                append(" {").
                append(ENTER);
        //body: return with default return type value
        stringBuilder.append(TAB).append(TAB);
        buildMethodBody(method.getReturnType(), stringBuilder);
        stringBuilder.append(ENTER);
        //tail: closing method
        stringBuilder.
                append(TAB).
                append("}");
    }

    /**
     * Appends to the {@code stringBuilder} all parameters divided by "," in "()".
     * <p>
     * If {@code parameters} is empty, appends only "()".
     *
     * @param parameters all parameters of the method
     * @param stringBuilder java.lang.StringBuilder in which should return statement be appended
     *
     * @see java.lang.reflect.Parameter
     * @see java.lang.StringBuilder
     */
    private void buildMethodParameters(Parameter[] parameters, StringBuilder stringBuilder) {
        stringBuilder.append("(");
        int nom = 0;
        for (Parameter parameter : parameters) {
            stringBuilder.append(parameter.getType().getCanonicalName()).append(" ").append(parameter.getName());
            if (nom < parameters.length - 1) {
                stringBuilder.append(", ");
            }
            nom++;
        }
        stringBuilder.append(")");
    }

    /**
     * Appends to the {@code stringBuilder} "throws " + all exceptions from {@code exceptionTypes} + " ".
     * <p>
     * If {@code exceptionTypes} is empty, nothing happens.
     *
     * @param exceptionTypes all exception for the method
     * @param stringBuilder java.lang.StringBuilder in which should return statement be appended
     *
     * @see java.lang.Class
     * @see java.lang.StringBuilder
     */
    private void buildMethodThrows(Class<?>[] exceptionTypes, StringBuilder stringBuilder) {
        if (exceptionTypes.length == 0) {
            return;
        }
        stringBuilder.append(" throws ");
        int nom = 0;
        for (Class<?> exception : exceptionTypes) {
            stringBuilder.append(exception.getName());
            if (nom < exceptionTypes.length - 1) {
                stringBuilder.append(", ");
            }
            nom++;
        }
    }

    /**
     * Appends to the {@code stringBuilder} "return " + default return value for {@code returnType}.
     *
     * @param returnType return type of the method.
     * @param stringBuilder java.lang.StringBuilder in which should return statement be appended.
     *
     * @see java.lang.Class
     * @see java.lang.StringBuilder
     */
    private void buildMethodBody(Class<?> returnType, StringBuilder stringBuilder) {
        stringBuilder.append("return");
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
        stringBuilder.append(returnValue).append(";");
    }

    /**
     * Concat two strings: {@code simpleName} and "Impl"
     *
     * @param simpleName simple name of class
     * @return name of file + "Impl"
     */
    private String buildFileName(String simpleName) {
        return simpleName + "Impl";
    }

    /**
     * Replaces all '.' in {@code packageName} on {@link Implementor#FILE_SEPARATOR_CHAR}.
     *
     * @param packageName name of package where '.' should be replaced
     * @return result string
     *
     */
    private String buildPackageName(String packageName) {
        return packageName.replace('.', FILE_SEPARATOR_CHAR);
    }
}
