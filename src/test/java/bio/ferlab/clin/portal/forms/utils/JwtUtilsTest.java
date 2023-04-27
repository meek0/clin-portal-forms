package bio.ferlab.clin.portal.forms.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilsTest {

  private final Algorithm algorithm = Algorithm.HMAC256("secret");
  
  @Test
  void getProperty() {
    String token = JWT.create()
        .withClaim(JwtUtils.FHIR_PRACTITIONER_ID, "foo")
        .sign(algorithm);
    assertEquals("foo", JwtUtils.getProperty(token, JwtUtils.FHIR_PRACTITIONER_ID));
  }

  @Test
  void getProperty_not_found() {
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      String token = JWT.create()
          .sign(algorithm);
      JwtUtils.getProperty(token, JwtUtils.FHIR_PRACTITIONER_ID);
    });
    assertEquals("missing fhir_practitioner_id in token", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
  }

  @Test
  void removeBearerPrefix() {
    assertEquals("foo", JwtUtils.removeBearerPrefix("Bearer foo"));
  }

  @Test
  void getUserRoles() {
    String bearer = JWT.create()
      .withClaim("realm_access", Map.of("roles", List.of("foo", "bar", "clin-1", "clin_2")))
      .sign(Algorithm.HMAC256("secret"));
    List<String> roles = JwtUtils.getUserRoles("Bearer " + bearer);
    assertEquals(2, roles.size());
    assertEquals("clin-1", roles.get(0));
    assertEquals("clin_2", roles.get(1));
  }
}