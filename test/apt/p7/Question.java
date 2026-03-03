package apt.p7;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;

public class Question {

  @QueryMapping
  public String nameOf(@Argument Identity id) { return null; }

  @QueryMapping
  public Integer numberOf(@Argument Identity id) { return null; }

  @MutationMapping
  public Identity identify(@Argument String name, @Argument Integer id) { return null; }

  @MutationMapping
  public Data noise(@Argument Integer x) { return null; }

}