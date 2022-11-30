package bio.ferlab.clin.portal.forms.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

  @Test
  void indexOfAnyIgnoreCase() {
    assertFalse(Utils.isIndexOfAnyIgnoreCase(null, null));
    assertFalse(Utils.isIndexOfAnyIgnoreCase(null, ""));
    assertTrue(Utils.isIndexOfAnyIgnoreCase("F", "foo", "bar"));
    assertTrue(Utils.isIndexOfAnyIgnoreCase("f", "FOO", "BAR"));
    assertTrue(Utils.isIndexOfAnyIgnoreCase("foo", null, "FOO"));
  }

  @Test
  void removeSpaces() {
    assertNull(Utils.removeSpaces(null));
    assertEquals("", Utils.removeSpaces(" "));
    assertEquals("", Utils.removeSpaces("   "));
    assertEquals("foobar", Utils.removeSpaces(" foo   bar   "));
  }
}