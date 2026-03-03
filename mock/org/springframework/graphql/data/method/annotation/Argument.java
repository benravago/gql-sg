package org.springframework.graphql.data.method.annotation;

import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.*;

@Retention(RUNTIME)
@Target(PARAMETER)
public @interface Argument {
  public String value() default "";
  public String name() default "";
}