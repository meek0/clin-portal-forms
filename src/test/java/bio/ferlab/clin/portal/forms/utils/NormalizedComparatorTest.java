package bio.ferlab.clin.portal.forms.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class NormalizedComparatorTest {

  @Test
  void accents() {
    List<String> expected = List.of("0", "a", "b", "bar", "éa", "eb", "éc", "foo", "réf", "ret", "z");

    List<String> shuffled = new ArrayList<>(expected);
    Collections.shuffle(shuffled);

    List<String> sorted = new ArrayList<>(shuffled);
    sorted.sort(new NormalizedComparator());

    assertNotEquals(expected, shuffled);
    assertEquals(expected, sorted);
  }

}