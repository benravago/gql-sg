package tools.gqls.annotation;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.*;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;

@Retention( RUNTIME )
@Target({ TYPE, METHOD, FIELD, PARAMETER })
public @interface TypeMapping {
  public String name() default "";
  public boolean id() default false;
  public boolean ignore() default false;
  public boolean require() default false;
}