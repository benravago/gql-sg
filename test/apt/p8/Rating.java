package apt.p8;

import tools.gqls.annotation.TypeMapping;

public class Rating {

  @TypeMapping(id=true)
  public Integer getId() { return null; }  // read-only

  public String getUser() { return null; } // read
  public void setUser(String user) {}      // write

  public Integer rating;
  public String comment;
}