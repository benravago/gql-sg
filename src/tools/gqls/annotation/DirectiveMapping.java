package tools.gqls.annotation;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.*;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;

import graphql.introspection.Introspection.DirectiveLocation;

@Retention( RUNTIME )
@Target( ANNOTATION_TYPE )
public @interface DirectiveMapping {
  public String name() default "";
  public String description() default "";
  public boolean builtin() default false;
  public String instance() default "";
  public Class<?> wiring() default void.class;
  public DirectiveLocation[] location() default {};
  @Alias("name") String value() default "";
}