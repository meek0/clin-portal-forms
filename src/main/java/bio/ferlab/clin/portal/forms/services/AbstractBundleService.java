package bio.ferlab.clin.portal.forms.services;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

public abstract class AbstractBundleService {

  private final String baseName;
  private final String fallBack;
  private final Map<String, ResourceBundle> labels = new HashMap<>();
  private final LogOnceService logOnceService;

  protected AbstractBundleService(String baseName, String fallBack, List<String> langs, LogOnceService logOnceService) {
    this.baseName = baseName;
    this.fallBack = fallBack;
    this.logOnceService = logOnceService;
    langs.forEach(lang -> {
      if (lang.isEmpty()) {
        this.labels.put(lang, ResourceBundle.getBundle(baseName));
      } else {
        this.labels.put(lang, ResourceBundle.getBundle(baseName, Locale.forLanguageTag(lang)));
      }
    });
  }

  public String get(String key, String lang) {
    String value = null;
    if (StringUtils.isNoneBlank(key, lang)) {
      final ResourceBundle bundle = labels.getOrDefault(lang, labels.get(fallBack));
      if (bundle.containsKey(key)) {
        value = bundle.getString(key);
      }
      if (StringUtils.isBlank(value)) {
        logOnceService.warn(String.format("Missing i18n in '%s' for key: %s and lang: %s", baseName, key, lang));
        value = null;
      }
    }
    return value;
  }

}
