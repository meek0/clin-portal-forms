package bio.ferlab.clin.portal.forms.filters;

import bio.ferlab.clin.portal.forms.configurations.SecurityConfiguration;
import bio.ferlab.clin.portal.forms.services.SecurityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {
  
  private final SecurityService securityService;
  private final SecurityConfiguration securityConfiguration;

  @Override
  public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    if (!HttpMethod.OPTIONS.name().equals(request.getMethod())) { // we don't check OPTIONS
      String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
      securityService.checkAuthorization(authorization);
    }
    filterChain.doFilter(request, response);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
    return this.securityConfiguration.getPublics().stream().anyMatch(p -> request.getRequestURI().startsWith(p)) || super.shouldNotFilter(request);
  }
}
