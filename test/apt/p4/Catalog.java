package apt.p4;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;

public class Catalog {

  @QueryMapping
  public Book bookById(@Argument Integer id) { return null; }

  @QueryMapping
  public List<Book> allBooks() { return null; }

  @SchemaMapping
  public Author author(Book book) { return null; }
  // resolves Book.author

}