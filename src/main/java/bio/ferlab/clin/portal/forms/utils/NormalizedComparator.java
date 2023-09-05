package bio.ferlab.clin.portal.forms.utils;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.Optional;

/**
 * Collections sorter ignore accents or special characters
 */
public class NormalizedComparator implements Comparator<String> {
  @Override
  public int compare(String o1, String o2) {
    return normalize(o1).compareTo(normalize(o2));
  }

  private String normalize(String str) {
    return Optional.ofNullable(str).map(s -> Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "")).orElse("");
  }
}
