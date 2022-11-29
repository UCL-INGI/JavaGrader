package org.javagrader;

import java.lang.annotation.*;

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(Allows.class)
public @interface Allow {

    String value();

}