package com.onizuka.framework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME) // Available at Runtime
@Target(ElementType.PARAMETER) // restricts usage to parameters
public @interface PathParam {
    String value(); // path parameter
}
