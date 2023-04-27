package bio.ferlab.clin.portal.forms.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static bio.ferlab.clin.portal.forms.utils.FhirConst.TOKEN_ATTR_REALM_ACCESS;
import static bio.ferlab.clin.portal.forms.utils.FhirConst.USER_ROLES_CLIN_PREFIX;

public class JwtUtils {
  
  private JwtUtils() {}
  
  public static final String BEARER_PREFIX = "Bearer ";
  public static final String FHIR_PRACTITIONER_ID = "fhir_practitioner_id";
  public static final String AUTHORIZED_PARTY = "azp";
  
  public static String getProperty(String token, String attr) {
    DecodedJWT jwt = JWT.decode(removeBearerPrefix(token));
    return getProperty(jwt, attr);
  }

  public static String getProperty(DecodedJWT jwt, String attr) {
    return Optional.ofNullable(jwt.getClaim(attr)).map(Claim::asString)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing " + attr + " in token"));
  }

  public static List<String> getUserRoles(String token) {
    final var jwt = JWT.decode(removeBearerPrefix(token));

    final List<String> roles = new ArrayList<>();

    roles.addAll(Optional.ofNullable(jwt.getClaim(TOKEN_ATTR_REALM_ACCESS))
      .map(c -> c.as(RealmAccess.class))
      .map(c -> c.roles)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing " + TOKEN_ATTR_REALM_ACCESS)));

    // ignore all roles that aren't clin
    return roles.stream().filter(r -> r.startsWith(USER_ROLES_CLIN_PREFIX)).collect(Collectors.toList());
  }

  public static class RealmAccess {
    public List<String> roles = new ArrayList();
  }
  
  public static String removeBearerPrefix(String token) {
    return token.replace(BEARER_PREFIX, "");
  }
  
}
