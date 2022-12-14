package org.javagrader;

import java.lang.annotation.*;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(Allows.class)
@Inherited
public @interface Allow {

    String value();

}