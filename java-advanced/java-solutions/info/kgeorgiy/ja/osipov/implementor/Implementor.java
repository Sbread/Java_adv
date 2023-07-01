package info.kgeorgiy.ja.osipov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Class for generating classes.
 *
 * @author Daniil Osipov
 * @see info.kgeorgiy.java.advanced.implementor.Impler
 * @see info.kgeorgiy.java.advanced.implementor.JarImpler
 */
public class Implementor implements JarImpler {

    /**
     * ".java" string constant
     */
    private static final String JAVA = ".java";

    /**
     * "Impl" string constant
     */
    private static final String IMPL = "Impl";

    /**
     * "tab" string constant
     */
    private static final String TAB = "    ";


    /**
     * {@link System#lineSeparator()} string constant
     */
    private static final String LINE_SEPARATOR = System.lineSeparator();


    /**
     * {@link File#separatorChar} string constant
     */
    private static final char FILE_SEPARATOR = File.separatorChar;

    /**
     * Method generating class simpleName with Impl suffix
     *
     * @param aClass input class token
     * @return {@code aClass} {@link Class#getSimpleName()} with suffix Impl
     */
    private String getClassImplSimpleName(final Class<?> aClass) {
        return aClass.getSimpleName() + IMPL;
    }

    /**
     * Method generating {@code Path} for class without root
     *
     * @param aClass input class
     * @return {@code Path} for provided {@code aClass}
     */
    private Path getClassImplPath(final Class<?> aClass) {
        return Path.of(aClass.getPackageName().replace('.', FILE_SEPARATOR))
                .resolve(getClassImplSimpleName(aClass) + JAVA);
    }

    /**
     * Method checking {@code aClass} implement possibility.
     * <p>
     * Class is not valid if it's array, enum, primitive type,
     * {@link Modifier#isFinal(int)} or {@link Modifier#isPrivate(int)}
     *
     * @param aClass input class token
     * @return {@code true} if token is valid
     */
    private boolean cannotBeImplemented(final Class<?> aClass) {
        final int modifiers = aClass.getModifiers();
        return aClass.isArray()
                || aClass.isPrimitive()
                || aClass == Enum.class
                || Modifier.isFinal(modifiers)
                || Modifier.isPrivate(modifiers)
                || methodsAreNotImplementable(aClass);
    }

    private boolean methodsAreNotImplementable(final Class<?> aClass) {
        Map<CustomMethod, Method> allMethods = new HashMap<>();
        findAllMethods(aClass, allMethods);
        List<Method> methods = allMethods.entrySet()
                .stream()
                .filter(entry -> Modifier.isAbstract(entry.getKey().method().getModifiers()))
                .map(Map.Entry::getValue).toList();
        for (final Method method : methods) {
            if (methodIsNotImplementable(method)) {
                return true;
            }
        }
        return false;
    }

    private boolean methodIsNotImplementable(final Method method) {
        final Class<?>[] parametersTypes = method.getParameterTypes();
        for (final Class<?> argClass : parametersTypes) {
            if (Modifier.isPrivate(argClass.getModifiers())) {
                return true;
            }
        }
        return Modifier.isPrivate(method.getReturnType().getModifiers());
    }

    /**
     * Method tries creating parent directories for provided {@code path}
     *
     * @param path provided {@code Path}
     */
    private static void createDirectories(final Path path) {
        if (Objects.nonNull(path.getParent())) {
            try {
                Files.createDirectories(path.getParent());
            } catch (final IOException ignored) {
            }
        }
    }

    /**
     * Method for delete temporary directory
     * <p>
     * Uses {@link Files#walkFileTree(Path, FileVisitor)} and {@link DeleteFileVisitor}
     *
     * @param tmpDir            {@code Path} of the directory
     * @param deleteFileVisitor {@link DeleteFileVisitor} instance
     * @throws ImplerException if tmpDir doesn't exist
     */
    private static void deleteTmpDirectory(final Path tmpDir,
                                           final DeleteFileVisitor deleteFileVisitor) throws ImplerException {
        try {
            Files.walkFileTree(tmpDir, deleteFileVisitor);
        } catch (IOException e) {
            throw new ImplerException("Cannot clean temporary directory");
        }
    }

    /**
     * Method for generate jar file with implemented class
     * <p>
     * Implements provided {@code aClass}. Creates temporary directory, where .class files will be stored.
     * Compiles classes with {@link #compileClass(Class, Path)} and creates manifest for jar with
     * {@link #createJar(Class, Path, Path)}. Delete temporary directory with
     * {@link #deleteTmpDirectory(Path, DeleteFileVisitor)}
     *
     * @param aClass type token to create implementation for.
     * @param path   {@link Path} to store jar file.
     * @throws ImplerException if {@link #implement(Class, Path)} error occurred
     *                         or {@link IOException} occurred while creating temporary directory
     */
    @Override
    public void implementJar(Class<?> aClass, Path path) throws ImplerException {
        DeleteFileVisitor deleteFileVisitor = new DeleteFileVisitor();
        createDirectories(path);
        final Path tmpDir;
        try {
            tmpDir = Files.createTempDirectory(path.toAbsolutePath().getParent(), "tmp");
        } catch (IOException e) {
            throw new ImplerException("Cannot create temporary directory for class implementation");
        }
        try {
            implement(aClass, tmpDir);
            compileClass(aClass, tmpDir);
            createJar(aClass, tmpDir, path);
        } finally {
            deleteTmpDirectory(tmpDir, deleteFileVisitor);
        }
    }

    /**
     * Method for creating jar
     * <p>
     * Creates manifest with version 1.0 and puts .class files from {@code tmpDir} to jar file
     *
     * @param aClass super class or interface
     * @param tmpDir temporary directory where .class files will be stored
     * @param path   provided {@code Path} of jar file
     * @throws ImplerException if {@link IOException} occurred while writing to jar
     */
    private void createJar(final Class<?> aClass, final Path tmpDir, final Path path) throws ImplerException {
        final Manifest manifest = new Manifest();
        final Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (final JarOutputStream out = new JarOutputStream(Files.newOutputStream(path), manifest)) {
            final String className = aClass.getPackageName().replace('.', '/')
                    + "/" + getClassImplSimpleName(aClass) + ".class";
            out.putNextEntry(new JarEntry(className));
            Files.copy(tmpDir.resolve(className), out);
//            Files.copy(Paths.get(tmpDir.toString(), className), out);
        } catch (IOException e) {
            throw new ImplerException("jar write error " + e.getMessage());
        }
    }

    /**
     * Method for getting class path from {@code aClass}
     *
     * @param aClass super class or interface
     * @return class path of provided {@code aClass}
     * @throws ImplerException if token couldn't be converted to URI
     */
    private String getClassPath(final Class<?> aClass) throws ImplerException {
        try {
            return Path.of(aClass.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (URISyntaxException e) {
            throw new ImplerException("Invalid class for URI:" + aClass.getSimpleName() + e.getMessage());
        }
    }

    /**
     * Method for compiling class
     *
     * @param aClass class
     * @param tmpDir directory where will be generated jar file
     * @throws ImplerException if java compiler is not provided or its exit code not equals to 0
     */
    private void compileClass(final Class<?> aClass, final Path tmpDir) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (Objects.isNull(compiler)) {
            throw new ImplerException("Cannot find compiler");
        }
        final String classPath = getClassPath(aClass);
        final Path filePath = tmpDir.resolve(getClassImplPath(aClass));
        createDirectories(filePath);
        final String[] args = Stream.of(
                        filePath.toString(),
                        "-encoding",
                        "UTF-8",
                        "-cp",
                        String.join(
                                File.pathSeparator,
                                tmpDir.toString(),
                                classPath))
                .toArray(String[]::new);

        final int exitCode = compiler.run(null, null, null, args);
        if (exitCode != 0) {
            throw new ImplerException("Compiler exit code:" + exitCode);
        }
    }

    /**
     * Implementation of {@link Impler#implement(Class, Path)}.
     * <p>
     * Invokes {@link #generatePackage(Class)}, {@link #generateClassSignature(Class)},
     * {@link #generateConstructors(Class)}, {@link #generateMethods(Class)} of provided
     * {@code aClass} and write generated implementation by {@link BufferedWriter} {@code writer}
     * in {@code classImplPath}
     *
     * @param aClass type token to create implementation for.
     * @param path   root directory.
     * @throws ImplerException if this {@code classPath} of {@code token} and {@code root} is not supported
     *                         or provided {@code token} is not valid
     */
    @Override
    public void implement(final Class<?> aClass, final Path path) throws ImplerException {
        if (cannotBeImplemented(aClass)) {
            throw new ImplerException("Class " + aClass.getCanonicalName() + " cannot be implemented");
        }
        final Path classImplPath = path.resolve(getClassImplPath(aClass));
        createDirectories(classImplPath);

        try (final BufferedWriter writer = Files.newBufferedWriter(classImplPath, StandardCharsets.UTF_8)) {
            writer.write(String.join(
                    LINE_SEPARATOR,
                    generatePackage(aClass),
                    generateClassSignature(aClass),
                    generateConstructors(aClass),
                    generateMethods(aClass),
                    "}"));
        } catch (final IOException e) {
            throw new ImplerException("writer error " + e.getMessage());
        }
    }

    /**
     * Method for format string padding
     *
     * @param level nesting level
     * @param str   string to format
     * @return string formatted by provided by level {@link #TAB} tabs
     */
    private String formatPadding(final int level, final String str) {
        return TAB.repeat(level) + str;
    }

    /**
     * Method for generating package of generated class {@code aClass}.
     * <p>
     * The result will be string "package <i>package of aClass;</i>"
     * or empty string if there is no information about package in {@code aClass}
     *
     * @param aClass super class or interface for future generated class
     * @return generated code with information about the package
     */
    private static String generatePackage(final Class<?> aClass) {
        final String classPackage = aClass.getPackageName();
        return classPackage.isEmpty() ? "" : "package " + classPackage + ";";
    }

    /**
     * Method for generating class signature
     * <p>
     * The result will be string "public class <i>aClassImpl</i> implements/extends {@code aClass}"
     * depending on {@code aClass} is interface or class
     *
     * @param aClass super class or interface for future generated class
     * @return generated code for class signature
     */
    private String generateClassSignature(final Class<?> aClass) {
        return String.join(
                " ",
                "public",
                "class",
                getClassImplSimpleName(aClass),
                aClass.isInterface() ? "implements" : "extends",
                aClass.getCanonicalName(),
                "{"
        );
    }

    /**
     * Method for generating exceptions for executable
     * <p>
     * Generate string "throws Exception1, Exception2, ..."
     * or empty string if {@code exceptions} is empty
     *
     * @param exceptions exceptions to throw
     * @return string of thrown exceptions
     */
    private static String generateExceptions(final Class<?>[] exceptions) {
        if (exceptions.length == 0) {
            return "";
        }
        return "throws " +
                Arrays.stream(exceptions)
                        .map(Class::getCanonicalName)
                        .collect(Collectors.joining(", "));
    }

    /**
     * Method to generate string of parameter with type
     *
     * @param parameter given {@link Parameter}
     * @return "parameter_type parameter_name"
     */
    private String getParameterWithType(final Parameter parameter) {
        return String.join(" ", parameter.getType().getCanonicalName(), parameter.getName());
    }

    /**
     * Method for generating arguments for executable
     * <p>
     * Generating arguments types depends on {@code function} converting function
     *
     * @param arguments executable arguments
     * @param function  converting function
     * @return generated code of arguments for provided {@code executable}
     */
    private String generateExecutableArguments(final Parameter[] arguments,
                                               final Function<Parameter, String> function) {
        return Arrays.stream(arguments)
                .map(function)
                .collect(Collectors.joining(", "));
    }

    /**
     * Method for generating signatures for constructors and methods
     * <p>
     * Generates string
     * "public {@code name} (<i>params with types</i>) throws <i>Exception1, Exception2, ...</i>"
     * with {@link #generateExecutableArguments(Parameter[], Function)}  and {@link #generateExceptions(Class[])}
     *
     * @param executable     method or constructor
     * @param executableName string with return type and name of method for methods
     *                       or without return type for constructors
     * @return generated code for signature of provided {@code executable}
     */
    private String generateExecutableSignature(final Executable executable,
                                               final String executableName) {
        return formatPadding(1,
                String.join(
                        " ",
                        "public",
                        executableName,
                        "(",
                        generateExecutableArguments(executable.getParameters(), this::getParameterWithType),
                        ")",
                        generateExceptions(executable.getExceptionTypes()),
                        "{"));
    }

    /**
     * Method of generating {@code constructor} body realization
     *
     * @param constructor which body will be generated
     * @return string of generated {@code constructor} body
     */
    private String generateConstructorBody(final Constructor<?> constructor) {
        return formatPadding(2, "super" + "(" + generateExecutableArguments(constructor.getParameters(), Parameter::getName) +
                ")" + ";");
    }

    /**
     * Method for generating code for constructor
     * <p>
     * Generates string "public <i>aClassImpl</i>(<i>params</i>) {super(params);}"
     * with {@link #generateExecutableSignature(Executable, String)}
     * and {@link #generateConstructorBody}
     *
     * @param constructor provided constructor of the super class
     * @param aClass      class
     * @return generated code for provided {@code constructor} and {@code aClass}
     */
    private String generateConstructor(final Constructor<?> constructor, final Class<?> aClass) {
        return String.join(
                LINE_SEPARATOR,
                generateExecutableSignature(constructor, getClassImplSimpleName(aClass)),
                generateConstructorBody(constructor),
                formatPadding(1, "}"));
    }

    /**
     * Method for generating one non-private constructor
     * <p>
     * If {@code token} is interface, than returns empty string
     *
     * @param aClass super class or interface for future generated class
     * @return code for one non-private constructor of class or empty string if interface
     * @throws ImplerException if {@code aClass} is class and there is not {@code public} constructors in it
     */
    private String generateConstructors(final Class<?> aClass) throws ImplerException {
        if (aClass.isInterface()) {
            return "";
        }
        final Optional<Constructor<?>> constructor = Arrays.stream(aClass.getDeclaredConstructors())
                .filter(c -> !Modifier.isPrivate(c.getModifiers()))
                .findFirst();

        if (constructor.isPresent()) {
            return generateConstructor(constructor.get(), aClass);
        } else {
            throw new ImplerException("No public constructors for class: " + aClass.getSimpleName());
        }
    }

    /**
     * BiFunction returns method with more narrow return type
     */
    private final BiFunction<Method, Method, Method> narrowestMethod = (m1, m2) ->
            m1.getReturnType().isAssignableFrom(m2.getReturnType()) ? m2 : m1;

    /**
     * Method for finding all distinct abstract and non-final methods of inheritance tree
     * <p>
     * Gets all methods of this {@code aClass} and its super class with {@link Class#getDeclaredMethods()},
     * {@link Class#getMethods()}. Merge found methods in map by key {@link CustomMethod}, value {@link Method},
     * and merging biFunction {@link #narrowestMethod} and recursion.
     *
     * @param aClass class
     * @param map    map that contains found methods
     */
    private void findAllMethods(final Class<?> aClass, final Map<CustomMethod, Method> map) {
        if (aClass == null) {
            return;
        }
        final ArrayList<Method> methods = new ArrayList<>(List.of(aClass.getMethods()));
        methods.addAll(List.of(aClass.getDeclaredMethods()));
        methods.forEach(method -> map.merge(new CustomMethod(method), method, narrowestMethod));
        findAllMethods(aClass.getSuperclass(), map);
    }

    /**
     * Method returns default value of {@code aClass}
     * <p>
     * Generates <ul>
     * <li>"null" for {@code Object}</li>
     * <li>"true" for {@code boolean}</li>
     * <li>empty string for {@code void}</li>
     * <li>"0" for primitive types</li>
     * </ul>
     *
     * @param aClass type for return default value
     * @return default value of provided {@code aClass}
     */
    private String getDefaultReturnValue(final Class<?> aClass) {
        if (!aClass.isPrimitive()) {
            return "null";
        }
        if (aClass == void.class) {
            return "";
        }
        if (aClass == boolean.class) {
            return "true";

        }
        return "0";
    }

    /**
     * Method generates default return value of {@code method}
     *
     * @param method method which return statement will be generated
     * @return return statement with {@link #getDefaultReturnValue(Class)}
     */
    private String generateReturn(final Method method) {
        return formatPadding(2,
                String.join(" ", "return", getDefaultReturnValue(method.getReturnType()), ";"));
    }

    /**
     * Method generates {@code method} return type and name
     *
     * @param method which return type and name will be generated
     * @return string generated {@code method} return type and name
     */
    private String generateMethodReturnTypeAndName(final Method method) {
        return String.join(" ", method.getReturnType().getCanonicalName(), method.getName());
    }

    /**
     * Method for generating method
     * <p>
     * Generates code for method with {@link #generateExecutableSignature(Executable, String)}
     * with body "return" {@link #getDefaultReturnValue(Class)}
     *
     * @param method method for generating
     * @return generated code for provided {@code method}
     */

    private String generateMethod(final Method method) {
        return String.join(
                LINE_SEPARATOR,
                generateExecutableSignature(method, generateMethodReturnTypeAndName(method)),
                generateReturn(method),
                formatPadding(1, "}"));
    }

    /**
     * Method for generating methods of the implementing class
     * <p>
     * Finds all abstract methods in inheritance tree with {@link #findAllMethods(Class, Map)} ,
     * leaves all non-final, abstract methods in parent ({@code aClass}), than generates methods with
     * {@link #generateMethod(Method)}
     *
     * @param aClass given class
     * @return generated realization for all non-final abstract methods of {@code aClass}
     */
    private String generateMethods(final Class<?> aClass) {
        Map<CustomMethod, Method> methods = new HashMap<>();
        findAllMethods(aClass, methods);
        return methods.entrySet()
                .stream()
                .filter(entry -> Modifier.isAbstract(entry.getKey().method().getModifiers()))
                .map(Map.Entry::getValue)
                .map(this::generateMethod)
                .collect(Collectors.joining(LINE_SEPARATOR));
    }

    /**
     * Method for executing {@code Implementor} from command line
     *
     * @param args should be of type
     *             "<i>full_super_class_name path_for_directory_of_future_generated_java_file</i>" for implement or
     *             "<i>-jar full_super_class_name path_for_directory_of_future_generated_java_file</i>" for implementing jar
     */
    public static void main(String[] args) {
        if (Objects.isNull(args) || (args.length != 2 && args.length != 3)) {
            System.err.println("Expected two args: class name and path, or three args: -jar, class name and path");
            return;
        }
        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("All args must be non-null");
            return;
        }
        try {
            Implementor implementor = new Implementor();
            if (args.length == 2) {
                implementor.implement(Class.forName(args[0]), Path.of(args[1]));
            } else if (args[0].equals("-jar")) {
                implementor.implementJar(Class.forName(args[1]), Path.of(args[2]));
            }
        } catch (InvalidPathException e) {
            System.err.println("Invalid path:" + args[1] + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Invalid class name: " + args[0] + e.getMessage());
        } catch (ImplerException e) {
            System.err.println("Implement exception. " + e.getMessage());
        }
    }
}
