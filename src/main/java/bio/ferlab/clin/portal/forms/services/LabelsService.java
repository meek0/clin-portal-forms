package bio.ferlab.clin.portal.forms.services;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LabelsService {
  
  private static final String LABELS_BUNDLE_NAME = "labels";
  private final Map<String, ResourceBundle> labels = new HashMap<>();
  private final LogOnceService logOnceService;

  public LabelsService(LogOnceService logOnceService) {
    this.logOnceService = logOnceService;
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
        logOnceService.warn(String.format("Missing label for code: %s and lang: %s", code, lang));
        label = null;
      }
    }
    return label;
  }

}
