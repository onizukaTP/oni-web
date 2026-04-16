package com.onizuka.framework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)  // Available at runtime -> required to scan using reflection
@Target(ElementType.METHOD)  // can be used on methods -> restricts usage to methods
public @interface GET {
    String value() default "/"; // path
}
