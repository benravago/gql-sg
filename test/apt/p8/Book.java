package apt.p8;

import tools.gqls.annotation.TypeMapping;

public class Book {

  @TypeMapping(id=true)
  public Integer getId() { return null; } // read-only

  public String name;
  public String author;
  public String publisher;
  public Double price;
}
