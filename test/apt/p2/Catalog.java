package apt.p2;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;

public class Catalog {

  @MutationMapping
  public Book createBook(@Argument Book book) { return null; }

  @MutationMapping
  public Book updateBook(@Argument Integer id, @Argument Book book) { return null; }

  @QueryMapping
  public Book bookById(@Argument Integer id) { return null; }

}
