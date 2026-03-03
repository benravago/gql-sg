package org.springframework.graphql.data.method.annotation;

import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.*;

@Retention(RUNTIME)
@Target({TYPE,METHOD})
public @interface SchemaMapping {
  public String value() default "";
  public String field() default ""; // Customize the name of the GraphQL field to bind to.
  public String typeName() default ""; // Customizes the name of the source/parent type for the GraphQL field.
}