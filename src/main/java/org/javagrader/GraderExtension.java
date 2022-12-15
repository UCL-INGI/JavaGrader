package org.javagrader;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.security.InvalidParameterException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.function.Executable;
import org.junit.platform.commons.util.ReflectionUtils;
import org.opentest4j.TestAbortedException;

import static org.javagrader.PrintConstants.*;
import static org.javagrader.TestResultStatus.SUCCESS;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

public class GraderExtension implements BeforeTestExecutionCallback,
        AfterTestExecutionCallback,
        TestWatcher,
        BeforeAllCallback,
        ExtensionContext.Store.CloseableResource,
        InvocationInterceptor {

    /** Gate keeper to prevent multiple Threads within the same routine */
    private static final Lock LOCK = new ReentrantLock();
    private static boolean started = false;
    private static final PrintStream originalStdOut = System.out;
    private PrintMode printMode = PrintMode.RST;
    private static Duration sumMaxCpuTimeout = Duration.ZERO;
    private static Duration sumMaxTimeout = Duration.ZERO;
    private static final String START_TIME = "start time";
    private static final String CPU_TIMEOUT = "cpu timeout";
    private static final String TIMEOUT_UNIT = "timeout units";
    private static Map<String, TestClassResult> testClassResult = new HashMap<>(); // TODO put in store context instead
    // otherwise this value will be updated for the same JVM and cannot be used to run test over the test suite itself
    
    public GraderExtension(GraderBuilder builder) {
        this.printMode = builder.printMode;
    }

    public GraderExtension() {

    }

    public static GraderBuilder builder() {
        return new GraderBuilder();
    }

    public static class GraderBuilder {
        
        private PrintMode printMode = PrintMode.RST;
        
        public GraderBuilder printMode(PrintMode printMode) {
            this.printMode = printMode;
            return this;
        }

        public GraderExtension build() {
            return new GraderExtension(this);
        }

    }

    public GraderExtension setPrintMode(PrintMode printMode) {
        this.printMode = printMode;
        return this;
    }

    /**
     * Gives the current cpu time in ns
     *
     * @return current cpu time in ns
     */
    private long getCpuTime() {
        return ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        // lock the access so only one Thread has access to it
        LOCK.lock();
        try {
            if (!started) {
                started = true;
                // The following line registers a callback hook when the root test context is shut down
                context.getRoot().getStore(GLOBAL).put("any unique name", this);
            }
        } finally {
            LOCK.unlock();
        }
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        Grade g = getBestAnnotationForTimeout(context);
        if (g != null) {
            getStore(context).put(CPU_TIMEOUT, g.cpuTimeout());
            getStore(context).put(TIMEOUT_UNIT, g.unit());
        } else {
            getStore(context).put(CPU_TIMEOUT, 0L);
            getStore(context).put(TIMEOUT_UNIT, null);
        }
        getStore(context).put(START_TIME, getCpuTime());
        updateSumTimeouts(context);
    }

    private Duration durationOf(long value, TimeUnit unit) {
        return Duration.ofNanos(unit.toNanos(value));
    }

    private void updateSumTimeouts(ExtensionContext context) {
        Grade g = getExistingGradeWithCpuTimeout(context);
        Duration wallClocktimeout = Duration.ZERO;
        if (g != null) {
            sumMaxCpuTimeout = sumMaxCpuTimeout.plus(durationOf(g.cpuTimeout(), g.unit()));
            wallClocktimeout = durationOf(g.cpuTimeout() * 3, g.unit());
        }
        Timeout t = getExistingGradeWithTimeout(context);
        if (t != null) {
            sumMaxTimeout = sumMaxTimeout.plus(durationOf(t.value(), t.unit()));
        } else if (!wallClocktimeout.isZero()) {
            sumMaxTimeout = sumMaxTimeout.plus(wallClocktimeout);
        }
    }

    /**
     * Provides the best annotation for timeout
     * If
     *  - no {@link Grade} is set on the method
     *  - {@link Grade#cpuTimeout()} is set to {@link Long#MAX_VALUE}
     * Gives the annotation from the class
     *
     * @param context
     * @return
     */
    private Grade getBestAnnotationForTimeout(ExtensionContext context) {
        Grade gm = context.getRequiredTestMethod().getAnnotation(Grade.class);
        Grade gc = context.getRequiredTestClass().getAnnotation(Grade.class);
        if (gm == null || gm.cpuTimeout() == Long.MAX_VALUE)
            return gc;
        return gm;
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        long startTime = getStore(context).remove(START_TIME, long.class);
        long duration = getCpuTime() - startTime;
        long cpuTimeout = getStore(context).remove(CPU_TIMEOUT, long.class);
        if (cpuTimeout > 0) {
            TimeUnit unit = getStore(context).remove(TIMEOUT_UNIT, TimeUnit.class);
            long excess = nanoSecondToTimeUnit(unit, duration) - cpuTimeout;
            if (excess > 0) {
                throw new java.util.concurrent.TimeoutException(
                        String.format("Execution exceeded CPU timeout of %s by %s",
                        getTimeoutMessage(unit, cpuTimeout),
                        getTimeoutMessage(unit, excess)));
            }
        }
    }

    private long nanoSecondToTimeUnit(TimeUnit unit, long value) {
        return unit.convert(value, TimeUnit.NANOSECONDS);
    }

    private static String getTimeoutMessage(TimeUnit unit, long value) {
        String label = unit.name().toLowerCase();
        if (value == 1 && label.endsWith("s")) {
            label = label.substring(0, label.length() - 1);
        }
        return value + " " + label;
    }

    private Store getStore(ExtensionContext context) {
        return context.getStore(Namespace.create(getClass(), context
                .getRequiredTestMethod()));
    }

    /* ----- reporting results ----- */

    @Override
    public void testSuccessful(ExtensionContext context) {
        addTestResult(context, SUCCESS);
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        addTestResult(context, TestResultStatus.ABORTED);
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        addTestResult(context, TestResultStatus.DISABLED);
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        String b = cause.getClass().toString();
        if (b.equals("class org.javagrader.CustomGradingResult")) { // cannot rely on instanceof because of the class loader changes
            Grade gradedMethod = context.getRequiredTestMethod().getAnnotation(Grade.class);
            Grade gradedClass = context.getRequiredTestClass().getAnnotation(Grade.class);
            if ((gradedClass != null && gradedClass.custom()) || (gradedMethod != null && gradedMethod.custom())) {
                CustomGradingResult custom = CustomGradingResult.fromSerialization(cause);
                if (custom != null)
                    addTestResult(context, custom.status, custom);
                else {
                    System.out.println("Failed to interpret the custom grading result " + cause);
                    addTestResult(context, TestResultStatus.FAIL);
                }
            } else {
                System.out.println("WARNING: Received a CustomGradingResult exception while not expecting one.");
                System.out.println("If you are trying to solve this exercise: sadly, there is a protection against this ;-)");
                System.out.println("If you are the exercise creator, you probably forgot to put custom=true inside @Grade.");
                addTestResult(context, TestResultStatus.FAIL);
            }
        } else {
            if (cause instanceof TimeoutException) {
                addTestResult(context, TestResultStatus.TIMEOUT);
            } else {
                addTestResult(context, TestResultStatus.FAIL);
            }
        }
    }

    private void addTestResult(ExtensionContext context, TestResultStatus status, CustomGradingResult customGradingResult) {
        Method m = context.getRequiredTestMethod();
        Class<?> c = context.getRequiredTestClass();
        boolean gradedClass = c.isAnnotationPresent(Grade.class);
        boolean gradedMethod = m.isAnnotationPresent(Grade.class);
        if (gradedClass || gradedMethod) { // only add if this is a graded test
            String methodName = getFactoryTestPrefix(context) + context.getDisplayName();
            String className = getClassDisplayName(context);
            TestMethodResult r = new TestMethodResult(methodName, m, status , customGradingResult);
            if (!testClassResult.containsKey(className)) {
                testClassResult.put(className, new TestClassResult(className, c));
            }
            testClassResult.get(className).addTestMethodResult(r);
        }
    }

    private void addTestResult(ExtensionContext context, TestResultStatus status) {
        addTestResult(context, status, null);
    }

    /**
     * Gets the display name of the class related to a context
     * Works for test, repeated tests and parametrized tests
     *
     * @return display name of the class related to a given context
     */
    private String getClassDisplayName(ExtensionContext context) {
        Optional<ExtensionContext> opt = context.getParent().get().getParent();
        if (opt.isPresent()) {
            return getClassDisplayName(context.getParent().get());
        }
        return context.getDisplayName();
    }

    private String getFactoryTestPrefix(ExtensionContext context) {
        Optional<ExtensionContext> opt = context.getParent().get().getParent();
        if (opt.isPresent() && opt.get().getParent().isPresent()) {
            return String.format("%s - ", context.getParent().get().getDisplayName());
        }
        return "";
    }

    @Override
    public void close() {
        if (printMode != PrintMode.NONE) {
            System.setOut(originalStdOut);
            printSumTimeouts();
            printTable();
        }
    }

    /**
     * Prints the sum of the max timeouts allowed according to the annotation (not the effective running time)
     */
    private void printSumTimeouts() {
        System.out.println("Max timeout = " + formatDuration(sumMaxTimeout));
        System.out.println("Max cpu timeout = " + formatDuration(sumMaxCpuTimeout));
    }

    /**
     * Format into HH:MM:SS
     *
     * @return readable duration
     */
    private String formatDuration(Duration d) {
        int partialMinutes = (int) (d.toMinutes() % 60L);
        int partialSeconds = (int) (d.getSeconds() % 60L);
        return String.format("%d:%02d:%02d", d.toHours(), partialMinutes, partialSeconds);
    }

    private void printTable() {
        System.out.println("--- GRADE ---");
        if (printMode == PrintMode.RST)
            System.out.println(RSTTableHeader);
        double grade = 0.;
        double maxGrade = 0.;
        double gradeWithoutAborted = 0.;
        double maxWithoutAborted = 0.;
        for (TestClassResult test: testClassResult.values()) {
            System.out.println(test.format(printMode));
            grade += test.grade();
            maxGrade += test.maxGrade();
            gradeWithoutAborted += test.grade();
            maxWithoutAborted += test.maxGradeWithoutAborted();
        }
        String prefix = globalPrefix(printMode);
        String sep = separator(printMode, SeparatorsType.CONTENT);
        switch (printMode) {
            case NONE: {break;}
            case NORMAL: {break;}
            case RST: {
                System.out.printf("%s%sTOTAL%s%s%s%s%s%s%n",
                        prefix, separator(printMode, SeparatorsType.CLASS_PREFIX),
                        separator(printMode, SeparatorsType.CLASS_SUFFIX), sep, sep,
                        RSTBold, formatGrade(grade, maxGrade), RSTBold);
                System.out.printf("%s%sTOTAL WITHOUT ABORTED%s%s%s%s%s%s%n%n",
                        prefix, separator(printMode, SeparatorsType.CLASS_PREFIX),
                        separator(printMode, SeparatorsType.CLASS_SUFFIX), sep, sep,
                        RSTBold, formatGrade(gradeWithoutAborted, maxWithoutAborted), RSTBold);
                break;
            }
            default: throw new IllegalArgumentException("Unrecognized printing mode " + printMode);
        }
        System.out.printf("TOTAL %s%n", formatGrade(grade, maxGrade));
        System.out.printf("TOTAL WITHOUT IGNORED %s%n", formatGrade(gradeWithoutAborted, maxWithoutAborted));
        System.out.println("--- END GRADE ---");
    }

    /* ----- sets wall clock timeout if not existing ----- */

    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        intercept(invocation, invocationContext, extensionContext);
    }

    @Override
    public <T> T interceptTestFactoryMethod(Invocation<T> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        // TODO
        return InvocationInterceptor.super.interceptTestFactoryMethod(invocation, invocationContext, extensionContext);
    }

    @Override
    public void interceptDynamicTest(Invocation<Void> invocation, DynamicTestInvocationContext invocationContext, ExtensionContext extensionContext) throws Throwable {
        // TODO
        InvocationInterceptor.super.interceptDynamicTest(invocation, invocationContext, extensionContext);
    }

    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        intercept(invocation, invocationContext, extensionContext);
        //InvocationInterceptor.super.interceptTestTemplateMethod(invocation, invocationContext, extensionContext);
    }

    private void intercept(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        Timeout cTimeout = extensionContext.getRequiredTestClass().getAnnotation(Timeout.class);
        Timeout mTimeout = extensionContext.getRequiredTestMethod().getAnnotation(Timeout.class);
        // TODO catch customgradingexception, use a context store and possibly set the test as success
        Grade g = null;
        if (cTimeout == null && mTimeout == null)
            g = getExistingGradeWithCpuTimeout(extensionContext);
        if (!areImportRestricted(extensionContext)) {
            // no need to override the class loader
            if (g != null) {
                long timeout = g.cpuTimeout() * 3;
                Timeout.ThreadMode threadMode = g.threadMode();
                Duration duration = Duration.ofNanos(g.unit().toNanos(timeout));
                switch (threadMode) {
                    case INFERRED: {
                        Assertions.assertTimeout(duration, invocation::proceed);
                        break;
                    }
                    case SAME_THREAD: {
                        Assertions.assertTimeout(duration, invocation::proceed);
                        break;
                    }
                    case SEPARATE_THREAD: {
                        Assertions.assertTimeoutPreemptively(duration, invocation::proceed);
                        break;
                    }
                }
            } else {
                invocation.proceed();
            }
        } else {
            // skip test execution and set the restricted class loader
            invocation.skip();
            Classpath classpath = Classpath.current();
            Set<String> forbids = getForbidden(extensionContext);
            Set<String> allows = getAllowed(extensionContext);
            RestrictedClassLoader modifiedClassLoader = classpath.newClassloader(forbids, allows);
            //ClassLoader modifiedClassLoader = new PermissiveClassLoader(Thread.currentThread().getContextClassLoader(), forbids, allows);
            ClassLoader currentThreadPreviousClassLoader = replaceCurrentThreadClassLoader(modifiedClassLoader);
            String className = extensionContext.getRequiredTestClass().getName();
            final Class<?> testClass;
            try {
                try {
                    testClass = modifiedClassLoader.loadClass(className);
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("Cannot load test class [" + className + "] from modified classloader, verify that you did not exclude a path containing the test", e);
                }
                final String methodName = extensionContext.getRequiredTestMethod().getName();
                Method m = extensionContext.getRequiredTestMethod();
                Object testInstance = ReflectionUtils.newInstance(testClass);
                List<Object> l = invocationContext.getArguments();
                Class<?>[] paramTypes = m.getParameterTypes();
                for (Class<?> c : paramTypes) {
                    if (modifiedClassLoader.isForbidden(c.getName())) {
                        throw new ClassNotFoundException("Failed to load the test instance as its contains a " + c.getName() + " parameter, which is forbidden");
                    }
                }
                // yes this is ugly, but it is needed for parametric tests to be imported correctly...
                final Optional<Method> method;
                try {
                    List<Method> ml = ReflectionUtils.findMethods(testInstance.getClass(), p -> {
                        if (!p.getName().equals(methodName))
                            return false;
                        Class<?>[] methodParams = p.getParameterTypes();
                        if (methodParams.length != paramTypes.length)
                            return false;
                        for (int i = 0; i < paramTypes.length; ++i) {
                            //if (!methodParams[i].toString().equals(paramTypes[i].toString()))
                            if (!isSameClass(methodParams[i], paramTypes[i]))
                                return false;
                        }
                        return true;
                    });
                    method = ReflectionUtils.findMethod(testInstance.getClass(), ml.get(0).getName(), ml.get(0).getParameterTypes());
                } catch (NoClassDefFoundError e) {
                    throw new ClassNotFoundException("Failed to load the test instance. It may contain a method with a forbidden parameter type", e);
                }
                List<Object> convertedArgs = new ArrayList<>();
                for (Object value : l) {
                    if (value == null) {
                        convertedArgs.add(null);
                    } else {
                        String toLoad = value.getClass().getName();
                        try {
                            Class<?> caster = modifiedClassLoader.loadClass(toLoad);
                            Object o = castObj(value, caster);
                            //Object o = modifiedClassLoader.convertInstance(toLoad, new Object().);
                            convertedArgs.add(o);
                        } catch (JsonIOException e) {
                            String message = String.format("Failed to provide argument of class %s (failed conversion to student's class loader using Gson. " +
                                    "Consider using @Grade(noSecurity = true), " +
                                    "use a simpler object type as input (perhaps you has cyclic references)" +
                                    " or refer to Gson doc to make it compatible)", toLoad);
                            throw new InvalidParameterException(message);
                        } catch (ClassNotFoundException e) {
                            String message;
                            if (forbids.contains(toLoad)) {
                                message = String.format("Failed to load class %s as it is forbidden", toLoad);
                            } else {
                                message = String.format("Failed to load class %s for unknown reason. " +
                                        "Consider using @Grade(noSecurity = true)", toLoad);
                            }
                            throw new ClassNotFoundException(message, e);
                        }
                    }
                }
                final Executable e = () -> ReflectionUtils.invokeMethod(
                        method.orElseThrow(() -> new IllegalStateException("No test method named " + methodName + " for class " + testClass)),
                        testInstance,
                        convertedArgs.toArray());

                if (g != null) {
                    long timeout = g.cpuTimeout() * 3;
                    Timeout.ThreadMode threadMode = g.threadMode();
                    Duration duration = Duration.ofNanos(g.unit().toNanos(timeout));
                    //Duration duration = Duration.of(timeout, g.unit().toChronoUnit());
                    BiConsumer<Duration, Executable> timeoutAssert = null;
                    switch (threadMode) {
                        case INFERRED: {
                            timeoutAssert = Assertions::assertTimeout;
                            break;
                        }
                        case SAME_THREAD: {
                            timeoutAssert = Assertions::assertTimeout;
                            break;
                        }
                        case SEPARATE_THREAD: {
                            timeoutAssert = Assertions::assertTimeoutPreemptively;
                            break;
                        }
                    }
                    timeoutAssert.accept(duration, e);
                } else {
                    e.execute();
                }

            } catch (Exception e1) {
                // determine if the exception is a TestAbortedException from the modified class loader
                Class<?> castedException = currentThreadPreviousClassLoader.loadClass(e1.getClass().getName());
                if (castedException == TestAbortedException.class) {
                    throw new TestAbortedException(e1.getMessage());
                }
                throw e1;
            } finally {
                Thread.currentThread().setContextClassLoader(currentThreadPreviousClassLoader);
            }
        }
    }

    /**
     * Retrieves the grade annotation responsible for the cpu timeout of this test
     * Null returned if the cpu timeout was not explicitly specified
     *
     * @param extensionContext
     * @return
     */
    private Grade getExistingGradeWithCpuTimeout(ExtensionContext extensionContext) {
        // first test for the method timeout
        Grade mGrade = extensionContext.getRequiredTestMethod().getAnnotation(Grade.class);
        if (mGrade != null && mGrade.cpuTimeout() != Long.MAX_VALUE)
            return mGrade;
        // no timeout configured for the method, testing the class
        Grade cGrade = extensionContext.getRequiredTestClass().getAnnotation(Grade.class);
        if (cGrade != null && cGrade.cpuTimeout() != Long.MAX_VALUE)
            return cGrade;
        // no timeout set on either the class or the method
        return null;
    }

    /**
     * Tells if the context should be run with restricted imports or not
     * This can be achieved by using {@code @Allow("all")} or setting {@link Grade#noRestrictedImport()} to true
     *
     * @param extensionContext
     * @return
     */
    private boolean areImportRestricted(ExtensionContext extensionContext) {
       Grade g = getExistingGradeWithoutRestrictedImport(extensionContext);
       if (g != null)
           return false;
        return !getAllowed(extensionContext).contains("all");
    }

    /**
     * Retrieves the grade annotation responsible for the restriction of imports
     * Null returned if the no import restriction was specified
     *
     * @param extensionContext
     * @return
     */
    private Grade getExistingGradeWithoutRestrictedImport(ExtensionContext extensionContext) {
        // first test for the noSecurity
        Grade mGrade = extensionContext.getRequiredTestMethod().getAnnotation(Grade.class);
        if (mGrade != null && mGrade.noRestrictedImport())
            return mGrade;
        // no noSecurity configured for the method, testing the class
        Grade cGrade = extensionContext.getRequiredTestClass().getAnnotation(Grade.class);
        if (cGrade != null && cGrade.noRestrictedImport())
            return cGrade;
        // no noSecurity set on either the class or the method
        return null;
    }

    /**
     * Retrieves the grade annotation responsible for the timeout of this test
     * Null returned if the timeout was not explicitly specified
     *
     * @param extensionContext
     * @return
     */
    private Timeout getExistingGradeWithTimeout(ExtensionContext extensionContext) {
        // first test for the method timeout
        Timeout mTimeout = extensionContext.getRequiredTestMethod().getAnnotation(Timeout.class);
        if (mTimeout != null)
            return mTimeout;
        // no timeout configured for the method, testing the class and possibly returning null
        return extensionContext.getRequiredTestClass().getAnnotation(Timeout.class);
    }

    private ClassLoader replaceCurrentThreadClassLoader(ClassLoader modifiedClassLoader) {
        ClassLoader currentThreadPreviousClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(modifiedClassLoader);
        return currentThreadPreviousClassLoader;
    }

    private Set<String> getForbidden(ExtensionContext extensionContext) {
        Forbid cForbid = extensionContext.getRequiredTestClass().getAnnotation(Forbid.class);
        Forbid mForbid = extensionContext.getRequiredTestMethod().getAnnotation(Forbid.class);
        Forbids cForbids = extensionContext.getRequiredTestClass().getAnnotation(Forbids.class);
        Forbids mForbids = extensionContext.getRequiredTestMethod().getAnnotation(Forbids.class);
        HashSet<String> forbidden = new HashSet<>();
        if (cForbid != null)
            forbidden.add(cForbid.value());
        if (mForbid != null)
            forbidden.add(mForbid.value());
        if (cForbids != null) {
            for (Forbid value: cForbids.value())
                forbidden.add(value.value());
        }
        if (mForbids != null) {
            for (Forbid value: mForbids.value())
                forbidden.add(value.value());
        }
        return forbidden;
    }

    private Set<String> getAllowed(ExtensionContext extensionContext) {
        Allow cAllow = extensionContext.getRequiredTestClass().getAnnotation(Allow.class);
        Allow mAllow = extensionContext.getRequiredTestMethod().getAnnotation(Allow.class);
        Allows cAllows = extensionContext.getRequiredTestClass().getAnnotation(Allows.class);
        Allows mAllows = extensionContext.getRequiredTestMethod().getAnnotation(Allows.class);
        HashSet<String> allowed = new HashSet<>();
        if (cAllow != null)
            allowed.add(cAllow.value());
        if (mAllow != null)
            allowed.add(mAllow.value());
        if (cAllows != null) {
            for (Allow value: cAllows.value())
                allowed.add(value.value());
        }
        if (mAllows != null) {
            for (Allow value : mAllows.value())
                allowed.add(value.value());
        }
        return allowed;
    }

    /**
     * Cast an object from one class loader to another by using {@link Gson}
     *
     * @return object loaded using the target class loader
     */
    private <T> T castObj(Object o, Class<T> target) throws JsonIOException {
        // Dear God, please forgive me for my sins
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(o), target);
    }

    /**
     * Modified version of isinstance, that works on objects across different class loaders
     *
     * @return true if the
     */
    private boolean isInstance(Object o, Class<?> target) {
        if (target.isInstance(o)) {
            return true;
        }
        return o.getClass().toString().equals(target.toString());
    }

    /**
     * Modified version of isinstance, that works on objects across different class loaders
     *
     * @return true if the
     */
    private <T> boolean isSameClass(Class<?> origin, Class<?> target) {
        if (origin.equals(target)) {
            return true;
        }
        return origin.toString().equals(target.toString());
    }

}