package apt.p5;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Test_Enumeration extends apt.TestCase {

  @Test
  void day_days() {
    assertTrue(
    check(make("./tmp/p5",
      "./test/apt/p5/Day.java",
      "./test/apt/p5/Days.java"
    )));
  }
}
