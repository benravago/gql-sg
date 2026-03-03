package apt.p9;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Test_Primitives extends apt.TestCase {

  @Test
  void builtins() {
    assertTrue(
    check(make("./tmp/p9",
      "./test/apt/p9/Builtins.java"
    )));
  }
}
