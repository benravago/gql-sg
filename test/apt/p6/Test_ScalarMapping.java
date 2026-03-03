package apt.p6;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Test_ScalarMapping extends apt.TestCase {

  @Test
  void identity_identitycoercing_question() {
    assertTrue(
    check(make("./tmp/p6",
      "./test/apt/p6/Identity.java",
      "./test/apt/p6/IdentityCoercing.java",
      "./test/apt/p6/Question.java"
    )));
  }
}
