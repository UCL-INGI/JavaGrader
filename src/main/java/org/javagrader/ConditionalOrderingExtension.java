package org.javagrader;

import org.junit.jupiter.api.extension.*;

/**
 * Used for conditional ordering tests
 * As soon as one test fails, all remaining ones are skipped
 */
public class ConditionalOrderingExtension implements ExecutionCondition, TestWatcher {

    private static boolean disable = false;

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {
        boolean orderBeforeFinished = !disable;
        if (orderBeforeFinished) {
            return ConditionEvaluationResult.enabled("The test before succeeded");
        } else {
            return ConditionEvaluationResult.disabled("Test ignored as the last one failed");
        }
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        disable = true;
    }

    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(getClass(), context
                .getRequiredTestMethod()));
    }

}
