package apt.p6;

import graphql.schema.Coercing;
import tools.gqls.annotation.ScalarMapping;

@ScalarMapping
public class IdentityCoercing implements Coercing<Object,Identity> {
  // actual implementation here
}