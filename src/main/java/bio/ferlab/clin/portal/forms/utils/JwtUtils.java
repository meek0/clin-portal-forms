package bio.ferlab.clin.portal.forms.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JwtUtils {
  
  public static final String BEARER_PREFIX = "Bearer ";
  public static final String FHIR_PRACTITIONER_ID = "fhir_practitioner_id";
  
  public Optional<String> getProperty(String token, String attr) {
    DecodedJWT jwt = JWT.decode(removeBearerPrefix(token));
    return Optional.ofNullable(jwt.getClaim(attr)).map(Claim::asString);
  }
  
  public String removeBearerPrefix(String token) {
    return token.replace(BEARER_PREFIX, "");
  }
  
}
