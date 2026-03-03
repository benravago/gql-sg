package apt.p2;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Test_MutationMapping extends apt.TestCase {

  @Test
  void book_catalog() {
    assertTrue(
    check(make("./tmp/p2",
      "./test/apt/p2/Book.java",
      "./test/apt/p2/Catalog.java"
    )));
  }
}
