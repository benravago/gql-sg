package apt.p3;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Test_SubscriptionMapping extends apt.TestCase {

  @Test
  void book_bookprice_catalog() {
    assertTrue(
    check(make("./tmp/p3",
      "./test/apt/p3/Book.java",
      "./test/apt/p3/BookPrice.java",
      "./test/apt/p3/Catalog.java"
    )));
  }
}
