package bio.ferlab.clin.portal.forms.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public class Utils {
  
  private Utils() {}

  public static String removeSpaces(String str) {
    return Optional.ofNullable(str).map(s -> s.replace(" ", "")).orElse(null);
  }
  
  public static boolean isIndexOfAnyIgnoreCase(String prefix, String...params){
    if (StringUtils.isNotBlank(prefix) && params != null) {
      for (String p : params) {
        if (org.apache.commons.lang3.StringUtils.isNotBlank(p)
            && p.toLowerCase().contains(prefix.toLowerCase())) {
          return true;
        }
      }
    }
    return false;
  }
}
