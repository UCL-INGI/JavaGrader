package org.javagrader;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

import org.apiguardian.api.API;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.annotation.Testable;

/**
 * A test to run, with its grading value and a potential cpu/wall-clock timeout
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Testable // allows for repeatable and parametrized tests
@Documented
@Inherited
@Tag("grade") // for filtering
@ExtendWith(GraderExtension.class)
public @interface Grade {

    /**
     * Value for the test
     */
    double value() default 1.0;

    /**
     * CPU timeout in the defined time unit {@link Grade#unit()}
     *
     * @return
     */
    long cpuTimeout() default Long.MAX_VALUE;

    /**
     * Time units related to the timeout {@link Grade#cpuTimeout()}
     *
     * @return
     */
    TimeUnit unit() default TimeUnit.SECONDS;

    Timeout.ThreadMode threadMode() default Timeout.ThreadMode.SAME_THREAD;

    /**
     * Expects a CustomGradingResult?
     * If false, CustomGradingResult will be considered as a standard error
     */
    boolean custom() default false;

    /**
     * Should run without the security from {@link Allow} and {@link Forbid}?
     * If yes, does not override the {@link ClassLoader} from the tests, therefore allowing every class to be loaded
     *
     * @return
     */
    boolean noSecurity() default false;

}
