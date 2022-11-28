package org.javagrader;

import java.lang.annotation.*;

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(Forbids.class)
public @interface Forbid {

    String value();

}
