package bio.ferlab.clin.portal.forms.filters;

import bio.ferlab.clin.portal.forms.configurations.SecurityConfiguration;
import bio.ferlab.clin.portal.forms.services.SecurityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityFilterTest {

  final SecurityConfiguration configuration = Mockito.mock(SecurityConfiguration.class);
  final SecurityService service = Mockito.mock(SecurityService.class);
  final SecurityFilter filter = new SecurityFilter(service, configuration);

  @BeforeEach
  void beforeEach() {
    when(configuration.getPublics()).thenReturn(List.of("/foo"));
  }

  @Test
  void doFilterInternal() throws ServletException, IOException {
    final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    when(request.getHeader(any())).thenReturn("token");
    final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    final FilterChain chain = Mockito.mock(FilterChain.class);
    filter.doFilterInternal(request, response, chain);
    verify(request).getHeader(eq("Authorization"));
    verify(service).checkAuthorization(eq("token"));
    verify(chain).doFilter(eq(request), eq(response));
  }

  @Test
  void shouldNotFilter_options() throws ServletException {
    final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    when(request.getMethod()).thenReturn(HttpMethod.OPTIONS.name());
    assertTrue(filter.shouldNotFilter(request));
  }

  @Test
  void shouldNotFilter_publics() throws ServletException {
    final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    when(request.getMethod()).thenReturn(HttpMethod.GET.name());
    when(request.getRequestURI()).thenReturn("/foo/abc");
    assertTrue(filter.shouldNotFilter(request));
  }

  @Test
  void shouldNotFilter_default() throws ServletException {
    final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    when(request.getMethod()).thenReturn(HttpMethod.GET.name());
    when(request.getRequestURI()).thenReturn("/bar");
    assertFalse(filter.shouldNotFilter(request));
  }

}