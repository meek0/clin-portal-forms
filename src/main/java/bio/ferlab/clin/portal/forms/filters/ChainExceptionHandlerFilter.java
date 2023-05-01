package bio.ferlab.clin.portal.forms.filters;

import bio.ferlab.clin.portal.forms.configurations.SecurityConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
@Order(0)
@Slf4j
public class ChainExceptionHandlerFilter extends OncePerRequestFilter {

  @Autowired
  @Qualifier("handlerExceptionResolver")
  private HandlerExceptionResolver resolver;
  @Autowired
  private SecurityConfiguration securityConfiguration;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    final long start = System.currentTimeMillis();
    try {
      filterChain.doFilter(request, response);
    } catch (Exception e) {
      // this little 'astuce' allows to throw exceptions in filters and resolve them like any Controller exceptions.
      resolver.resolveException(request, response, null, e);
    } finally {
      if (this.securityConfiguration.getPublics().stream().noneMatch(p -> request.getRequestURI().startsWith(p))) {
        log.info("{} {} in {} ms", request.getMethod(), request.getRequestURI(), System.currentTimeMillis() - start);
      }
    }
  }
}