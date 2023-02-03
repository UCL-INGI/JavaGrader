package org.javagrader;

import org.junit.jupiter.api.extension.*;

import javax.swing.text.html.Option;
import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

/**
 * Used for conditional ordering tests
 * As soon as one test fails, all remaining ones are skipped
 */
public class ConditionalOrderingExtension implements ExecutionCondition,
        TestWatcher, BeforeTestExecutionCallback{

    private static boolean disable = false;
    // TODO disable at class level

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {
        //boolean orderBeforeFinished = !disable;
        Class<?> annotated = getAnnotatedClass(extensionContext);
        boolean orderBeforeFinished = !extensionContext.getRoot().getStore(GLOBAL).getOrDefault(annotated, Boolean.class, false);
        if (orderBeforeFinished) {
            return ConditionEvaluationResult.enabled("The test before succeeded");
        } else {
            return ConditionEvaluationResult.disabled("Test ignored as the last one failed");
        }
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        disable = true;
        Class<?> annotated = getAnnotatedClass(context);
        if (annotated != null) {
            context.getRoot().getStore(GLOBAL).put(annotated, true);
        }
    }

    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(getClass(), context
                .getRequiredTestMethod()));
    }

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {
        // stores the context of the annotated class
        Class<?> annotated = getAnnotatedClass(extensionContext);
    }

    private Class<?> getAnnotatedClass(ExtensionContext context) {
        Class<?> testClass = context.getRequiredTestClass();
        Optional<Method> testMethod = context.getTestMethod();
        ExtendWith em = null;
        if (testMethod.isPresent()) {
            em = testMethod.get().getAnnotation(ExtendWith.class);
        }
        ExtendWith ec = testClass.getAnnotation(ExtendWith.class);
        if (ec == null && em == null) {
            return getAnnotatedClass(testClass.getSuperclass());
        } else {
            return testClass;
        }
    }

    private Class<?> getAnnotatedClass(Class<?> clazz) {
        if (clazz == null)
            return null;
        ExtendWith ec = clazz.getAnnotation(ExtendWith.class);
        if (ec == null) {
            return getAnnotatedClass(clazz.getSuperclass());
        } else {
            return clazz;
        }
    }

}
