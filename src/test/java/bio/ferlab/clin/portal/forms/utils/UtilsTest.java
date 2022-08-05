package bio.ferlab.clin.portal.forms.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

  @Test
  void indexOfAnyIgnoreCase() {
    assertFalse(Utils.indexOfAnyIgnoreCase(null, null));
    assertFalse(Utils.indexOfAnyIgnoreCase(null, ""));
    assertTrue(Utils.indexOfAnyIgnoreCase("F", "foo", "bar"));
    assertTrue(Utils.indexOfAnyIgnoreCase("f", "FOO", "BAR"));
    assertTrue(Utils.indexOfAnyIgnoreCase("foo", null, "FOO"));
  }
}