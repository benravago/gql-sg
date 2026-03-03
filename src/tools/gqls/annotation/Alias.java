package tools.gqls.annotation;

import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.*;

@Retention(RUNTIME)
@Target({TYPE,METHOD,FIELD,PARAMETER})
public @interface Alias {
  public String value() default "";
}