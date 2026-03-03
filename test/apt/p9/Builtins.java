package apt.p9;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;

import tools.gqls.annotation.TypeMapping;

public class Builtins {

  @QueryMapping
  public int int32(@Argument int si32) { return 0; }

  @QueryMapping
  public float fp32(@Argument float spfp) { return 0; }

  @QueryMapping
  public double fp64(@Argument double dpfp) { return 0; }

  @QueryMapping
  public boolean bool(@Argument boolean bit) { return false; }

  @QueryMapping
  public String chars(@Argument String text) { return null; }

  @QueryMapping
  public
    @TypeMapping(id=true)
    String opaque(
      @Argument
      @TypeMapping(id=true)
      String text
    )
    { return null; }

}