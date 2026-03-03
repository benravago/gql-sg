package apt.p1;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;

public class Catalog {

  @QueryMapping
  public Book bookById(@Argument Integer id) { return null; }

  @QueryMapping
  public List<Book> allBooks() { return null; }

}
