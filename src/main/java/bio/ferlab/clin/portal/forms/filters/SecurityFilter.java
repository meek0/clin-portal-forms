package bio.ferlab.clin.portal.forms.filters;

import bio.ferlab.clin.portal.forms.services.SecurityService;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(1)
public class SecurityFilter extends OncePerRequestFilter {
  
  private final SecurityService securityService;
  
  public SecurityFilter(SecurityService securityService) {
    this.securityService = securityService;
  }

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
    if (request.getRequestURI().startsWith("/actuator")) {
      return true;
    } else {
      return super.shouldNotFilter(request);
    }
  }
}
