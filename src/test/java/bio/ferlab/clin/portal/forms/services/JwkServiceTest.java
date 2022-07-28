package bio.ferlab.clin.portal.forms.services;

import bio.ferlab.clin.portal.forms.configurations.JwkConfiguration;
import bio.ferlab.clin.portal.forms.configurations.SecurityConfiguration;
import com.auth0.jwk.JwkException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class JwkServiceTest {

  private final Algorithm algorithm = Algorithm.HMAC256("secret");

  final SecurityConfiguration configuration = Mockito.mock(SecurityConfiguration.class);
  final JwkConfiguration jwkConfiguration = Mockito.mock(JwkConfiguration.class);
  final JwkService service = new JwkService(configuration, jwkConfiguration);

  @BeforeEach
  void setup() throws JwkException {
    when(configuration.getIssuer()).thenReturn("http://localhost:8080/auth");
    when(configuration.getAudience()).thenReturn("audience");
    when(configuration.getLeeway()).thenReturn(5L);
    when(jwkConfiguration.getAlgorithm(any())).thenReturn(algorithm);
  }
  
  @Test
  void checkToken() {
    String token = JWT.create()
        .withClaim("iss", "http://localhost:8080/auth")
        .withClaim("aud", "audience")
        .withExpiresAt(Date.from(Instant.now().plusSeconds(60)))
        .sign(algorithm);
    DecodedJWT decoded = JWT.decode(token);
    service.checkToken(decoded);
  }

  @Test
  void checkToken_bad_signature() throws JwkException {
    when(jwkConfiguration.getAlgorithm(any())).thenReturn(Algorithm.HMAC256("another_secret"));
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      String token = JWT.create()
          .withClaim("iss", "http://localhost:8080/auth")
          .withClaim("aud", "audience")
          .withExpiresAt(Date.from(Instant.now().plusSeconds(60)))
          .sign(algorithm);
      DecodedJWT decoded = JWT.decode(token);
      service.checkToken(decoded);
    });
    assertEquals("The Token's Signature resulted invalid when verified using the Algorithm: HmacSHA256", exception.getReason());
    assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
  }

  @Test
  void checkToken_invalid_issuer() {
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      String token = JWT.create()
          .withClaim("iss", "foo")
          .sign(algorithm);
      DecodedJWT decoded = JWT.decode(token);
      service.checkToken(decoded);
    });
    assertEquals("The Claim 'iss' value doesn't match the required issuer.", exception.getReason());
    assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
  }

  @Test
  void checkToken_invalid_audience() {
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      String token = JWT.create()
          .withClaim("iss", "http://localhost:8080/auth")
          .withClaim("aud", "foo")
          .sign(algorithm);
      DecodedJWT decoded = JWT.decode(token);
      service.checkToken(decoded);
    });
    assertEquals("The Claim 'aud' value doesn't contain the required audience.", exception.getReason());
    assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
  }

  @Test
  void checkToken_expired() {
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      String token = JWT.create()
          .withClaim("iss", "http://localhost:8080/auth")
          .withClaim("aud", "audience")
          .withExpiresAt(Date.from(Instant.now().minusSeconds(10)))
          .sign(algorithm);
      DecodedJWT decoded = JWT.decode(token);
      service.checkToken(decoded);
    });
    assertTrue(exception.getReason().startsWith("The Token has expired on"));
    assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());

  }

  @Test
  void checkToken_leeway() {
    String token = JWT.create()
        .withClaim("iss", "http://localhost:8080/auth")
        .withClaim("aud", "audience")
        .withExpiresAt(Date.from(Instant.now().minusSeconds(3)))  // we allow 5 seconds
        .sign(algorithm);
    DecodedJWT decoded = JWT.decode(token);
    service.checkToken(decoded);
  }

}