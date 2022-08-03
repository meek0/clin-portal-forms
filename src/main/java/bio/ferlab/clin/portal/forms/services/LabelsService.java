package bio.ferlab.clin.portal.forms.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

@Service
@Slf4j
public class LabelsService {
  
  private static final String LABELS_BUNDLE_NAME = "labels";
  private final Map<String, ResourceBundle> labels = new HashMap<>();
  
  public LabelsService() {
    this.labels.put("fr", ResourceBundle.getBundle(LABELS_BUNDLE_NAME, Locale.forLanguageTag("fr")));
  }

  public String getLabel(String code, String lang) {
    String label = null;
    if (StringUtils.isNoneBlank(code, lang)) {
      final ResourceBundle bundle = labels.getOrDefault(lang, labels.get("fr"));  // fr by default
      if (bundle.containsKey(code)) {
        label = bundle.getString(code);
      }
      if (StringUtils.isBlank(label)) {
        log.warn("Missing label for code: {} and lang: {}", code, lang);
        label = null;
      }
    }
    return label;
  }
  
  
}
