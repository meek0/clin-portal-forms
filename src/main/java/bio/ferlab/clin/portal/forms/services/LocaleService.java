package bio.ferlab.clin.portal.forms.services;

import bio.ferlab.clin.portal.forms.configurations.FhirConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LocaleService {
  
  public static final String DEFAULT_LOCALE = "fr";
  public static final String LANG_QUERY_PARAM = "lang";
  
  private final HttpServletRequest request;
  private final FhirConfiguration fhirConfiguration;
  
  public String getCurrentLocale() {
    return Optional.ofNullable(request.getParameter(LANG_QUERY_PARAM))
        .map(StringUtils::parseLocale)
        .map(Locale::getLanguage)
        .filter(fhirConfiguration.getSupportedLangs()::contains)
        .orElse(DEFAULT_LOCALE);
  }
}
