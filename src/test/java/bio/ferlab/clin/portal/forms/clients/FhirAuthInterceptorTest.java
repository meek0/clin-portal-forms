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

  final HttpServletRequest httpServletRequest = Mockito.mock(MockHttpServletRequest.class);
  final FhirAuthInterceptor fhirAuthInterceptor = new FhirAuthInterceptor(httpServletRequest);

  @Test
  void forwardRequestAuth() {
    final IHttpRequest request = Mockito.mock(MockIHttpRequest.class);
    when(httpServletRequest.getHeader(any())).thenReturn("Bearer foo");
    fhirAuthInterceptor.interceptRequest(request);
    verify(request).addHeader(eq(HttpHeaders.AUTHORIZATION), eq("Bearer foo"));
  }

  @Test
  void sanitizeAuth() {
    final IHttpRequest request = Mockito.mock(MockIHttpRequest.class);
    when(httpServletRequest.getHeader(any())).thenReturn("foo");
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      fhirAuthInterceptor.interceptRequest(request);
    });
    assertEquals("Token forwarded is invalid: foo", exception.getMessage());
  }

  // dont ask me why ... mockito doesnt like to mock interface
  private static abstract class MockIHttpRequest implements IHttpRequest {}
  private static abstract class MockHttpServletRequest implements HttpServletRequest {}
}
