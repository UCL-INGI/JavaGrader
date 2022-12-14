package org.javagrader;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Inherited
public @interface Allows {

    Allow[] value();

}
