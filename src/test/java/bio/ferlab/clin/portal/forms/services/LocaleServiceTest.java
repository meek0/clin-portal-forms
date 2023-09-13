package bio.ferlab.clin.portal.forms.services;

import bio.ferlab.clin.portal.forms.configurations.FhirConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class LocaleServiceTest {
  
  final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
  final FhirConfiguration configuration = Mockito.mock(FhirConfiguration.class);
  final LocaleService service = new LocaleService(request, configuration);
  
  @BeforeEach
  void setup() {
    when(configuration.getSupportedLangs()).thenReturn(List.of("fr", "en"));
    when(request.getParameter("lang")).thenReturn(null);
  }
  
  @Test
  void getCurrentLocale_default() {
    assertEquals("fr", service.getCurrentLang());
  }

  @Test
  void getCurrentLang_query_param() {
    when(request.getParameter("lang")).thenReturn("en-EN");
    assertEquals("en", service.getCurrentLang());
  }

  @Test
  void getCurrentLangSupportedByFhir_supported() {
    when(request.getParameter("lang")).thenReturn("fr-FR");
    assertEquals("fr", service.getCurrentLangSupportedByFhir());
  }

  @Test
  void getCurrentLangSupportedByFhir_unsupported() {
    when(request.getParameter("lang")).thenReturn("de");
    assertEquals("fr", service.getCurrentLangSupportedByFhir());
  }

  @Test
  void getCurrentLocale_query_param() {
    when(request.getParameter("lang")).thenReturn("fr-CA");
    assertEquals(Locale.CANADA_FRENCH, service.getCurrentLocale());
  }

}