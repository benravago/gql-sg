package apt.p5;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;

public class Days {

  @QueryMapping()
  public Integer dayOfWeek(@Argument Day day) { return null; }

  @QueryMapping()
  public Day today() { return null; }

}
