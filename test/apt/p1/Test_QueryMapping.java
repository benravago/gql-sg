package apt.p1;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Test_QueryMapping extends apt.TestCase {

  @Test
  void book_author_catalog() {
    assertTrue(
    check(make("./tmp/p1",
      "./test/apt/p1/Book.java",
      "./test/apt/p1/Author.java",
      "./test/apt/p1/Catalog.java"
    )));
  }
}
