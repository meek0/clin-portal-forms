package bio.ferlab.clin.portal.forms.services;

import bio.ferlab.clin.portal.forms.configurations.SecurityConfiguration;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class SecurityServiceTest {
  
  private final Algorithm algorithm = Algorithm.HMAC256("secret");
  
  final SecurityConfiguration configuration = Mockito.mock(SecurityConfiguration.class);
  final JwkService jwkService = Mockito.mock(JwkService.class);
  final SecurityService service = new SecurityService(configuration, jwkService);
  
  @BeforeEach
  void setup() {
    when(configuration.isEnabled()).thenReturn(true);
  }
  
  @Test
  void checkAuthorization_mising_token() {
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      service.checkAuthorization(null);
    });
    assertEquals("missing token", exception.getReason());
    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
  }

  @Test
  void checkAuthorization_invalid_token() {
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      service.checkAuthorization("yolotoken");
    });
    assertEquals("invalid token", exception.getReason());
    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
  }

  @Test
  void checkAuthorization_ok_token() {
    final String token = JWT.create().sign(algorithm);
    service.checkAuthorization("Bearer " + token);
  }

}