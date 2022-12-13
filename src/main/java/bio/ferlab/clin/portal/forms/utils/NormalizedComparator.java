package bio.ferlab.clin.portal.forms.utils;

import java.text.Normalizer;
import java.util.Comparator;

/**
 * Collections sorter ignore accents or special characters
 */
public class NormalizedComparator implements Comparator<String> {
  @Override
  public int compare(String o1, String o2) {
    o1 = Normalizer.normalize(o1, Normalizer.Form.NFD);
    o2 = Normalizer.normalize(o2, Normalizer.Form.NFD);
    return o1.compareTo(o2);
  }
}
