package apt.p8;

import java.util.Collection;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;

import tools.gqls.annotation.TypeMapping;

public class Books {

  @QueryMapping()
  public
    Collection<Book> books()
    { return null; }

  @QueryMapping
  public
    Book bookById(

      @Argument
      @TypeMapping(id=true)
      Integer id

    ) { return null; }

  @MutationMapping
  public
    @TypeMapping(require=true)
    Book addBook(

      @Argument
      @TypeMapping(require=true)
      Book book

    ) { return null; }

  @MutationMapping
  public
    @TypeMapping(require=true)
    Book updateBook(

      @Argument
      @TypeMapping(require=true, id=true)
      Integer id,

      @Argument
      @TypeMapping(require=true)
      Book book

    ) { return null; }

  @MutationMapping
  public
    @TypeMapping(require=true)
    Book deleteBook(

      @Argument
      @TypeMapping(require=true, id=true)
      Integer id

    ) { return null; }

  @MutationMapping
  public
    Rating addRating(

      @Argument
      @TypeMapping(require=true)
      Integer bookId,

      @Argument
      @TypeMapping(require=true)
      Rating rating

    ) { return null; }

}
