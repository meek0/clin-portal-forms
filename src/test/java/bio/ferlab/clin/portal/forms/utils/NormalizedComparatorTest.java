package bio.ferlab.clin.portal.forms.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NormalizedComparatorTest {

  @Test
  void accents() {
    List<String> expected = List.of("0", "a", "b", "bar", "éa", "eb", "éc", "foo", "z");

    List<String> shuffled = new ArrayList<>(expected);
    Collections.shuffle(shuffled);

    Collections.sort(shuffled); // default sort put accents at the end
    assertEquals("0", shuffled.get(0));
    assertNotEquals("z", shuffled.get(shuffled.size()-1));  // BAD

    shuffled.sort(new NormalizedComparator());  // NormalizedComparator ignore accents
    assertEquals("0", shuffled.get(0));
    assertEquals("z", shuffled.get(shuffled.size()-1)); // GOOD
  }

}