package apt.p8;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Test_TypeMapping extends apt.TestCase {

  @Test
  void book_books_rating() {
    assertTrue(
    check(make("./tmp/p8",
      "./test/apt/p8/Book.java",
      "./test/apt/p8/Books.java",
      "./test/apt/p8/Rating.java"
    )));
  }
}
