package org.javagrader;

import java.lang.annotation.*;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Repeatable(Forbids.class)
public @interface Forbid {

    String value();

}
