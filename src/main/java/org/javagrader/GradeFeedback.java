package org.javagrader;

import java.lang.annotation.*;

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(GradeFeedbacks.class)
public @interface GradeFeedback {

    /**
     * Message to give as a feedback
     *
     * @return
     */
    String message();

    /**
     * Condition under which the feedback must be given
     *
     * @return
     */
    TestResultStatus on() default TestResultStatus.FAIL;

}
