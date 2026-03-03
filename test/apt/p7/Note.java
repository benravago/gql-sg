package apt.p7;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.*;
import java.lang.annotation.Target;

import tools.gqls.annotation.DirectiveMapping;

import static java.lang.annotation.ElementType.*;

import static graphql.introspection.Introspection.DirectiveLocation.*;

@Retention( RUNTIME )
@Target( TYPE )
@DirectiveMapping( location=OBJECT )
public @interface Note {
  String value();
}