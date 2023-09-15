package bio.ferlab.clin.portal.forms.clients;

import ca.uhn.fhir.rest.client.api.IHttpRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FhirAuthInterceptorTest {

  final HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
  final FhirAuthInterceptor fhirAuthInterceptor = new FhirAuthInterceptor(httpServletRequest);

  @Test
  void forwardRequestAuth() {
    final IHttpRequest request = Mockito.mock(IHttpRequest.class);
    when(httpServletRequest.getHeader(any())).thenReturn("Bearer foo");
    fhirAuthInterceptor.interceptRequest(request);
    verify(request).addHeader(eq(HttpHeaders.AUTHORIZATION), eq("Bearer foo"));
  }

  @Test
  void sanitizeAuth() {
    final IHttpRequest request = Mockito.mock(IHttpRequest.class);
    when(httpServletRequest.getHeader(any())).thenReturn("foo");
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      fhirAuthInterceptor.interceptRequest(request);
    });
    assertEquals("Token forwarded to FHIR is invalid: foo", exception.getMessage());
  }

}