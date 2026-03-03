package tools.gqls.annotation;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.*;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;

@Retention( RUNTIME )
@Target( TYPE ) // ANNOTATION_TYPE
public @interface ScalarMapping {
  public String name() default "";
  public String description() default "";
  public boolean builtin() default false;
  public String instance() default "";
  public Class<?> coercing() default void.class;
  public Class<?> type() default void.class;
  @Alias("name") String value() default "";
}