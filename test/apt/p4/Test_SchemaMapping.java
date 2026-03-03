package apt.p4;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Test_SchemaMapping extends apt.TestCase {

  @Test
  void bool_author_catalog() {
    assertTrue(
    check(make("./tmp/p4",
      "./test/apt/p4/Book.java",
      "./test/apt/p4/Author.java",
      "./test/apt/p4/Catalog.java"
    )));
  }
}
