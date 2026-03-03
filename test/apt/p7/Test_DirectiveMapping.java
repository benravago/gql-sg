package apt.p7;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Test_DirectiveMapping extends apt.TestCase {

  @Test
  void data_identity_note_oneof_question() {
    assertTrue(
    check(make("./tmp/p7",
      "./test/apt/p7/Data.java",
      "./test/apt/p7/Identity.java",
      "./test/apt/p7/Note.java",
      "./test/apt/p7/OneOf.java",
      "./test/apt/p7/Question.java"
    )));
  }
}
