package bio.ferlab.clin.portal.forms.services;

import bio.ferlab.clin.portal.forms.configurations.FhirConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LocaleService {

  public static final Locale DEFAULT_LOCALE = Locale.FRENCH;
  public static final String DEFAULT_LANG = "fr";
  public static final String LANG_QUERY_PARAM = "lang";
  
  private final HttpServletRequest request;
  private final FhirConfiguration fhirConfiguration;

  public String getCurrentLang() {
    return getCurrentLocale().getLanguage();
  }

  public String getCurrentLangSupportedByFhir() {
    final String lang = getCurrentLang();
    return fhirConfiguration.getSupportedLangs().contains(lang) ? lang : DEFAULT_LANG;
  }
  
  public Locale getCurrentLocale() {
    return Optional.ofNullable(request.getParameter(LANG_QUERY_PARAM))
      .map(StringUtils::parseLocale)
      .orElse(DEFAULT_LOCALE);
  }
}
