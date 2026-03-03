package org.springframework.graphql.data.method.annotation;

import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.*;

@Retention(RUNTIME)
@Target(METHOD)
public @interface MutationMapping { // @SchemaMapping(typeName="Mutation")
  public String value() default "";
  public String name() default ""; // Alias for SchemaMapping.field().
}