package bio.ferlab.clin.portal.forms.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class LocaleService {
  
  public static final String DEFAULT_LOCALE = "fr";
  public static final List<String> SUPPORTED_LOCALES = List.of("fr");
  public static final String LANG_QUERY_PARAM = "lang";

  @Autowired
  private HttpServletRequest request; // current request
  
  public String getCurrentLocale() {
    String lang = Optional.ofNullable(request.getParameter(LANG_QUERY_PARAM))
        .orElse(request.getHeader(HttpHeaders.ACCEPT_LANGUAGE));
    return Optional.ofNullable(lang)
        .map(StringUtils::parseLocale)
        .map(Locale::getLanguage)
        .filter(SUPPORTED_LOCALES::contains)
        .orElse(DEFAULT_LOCALE);
  }
}
