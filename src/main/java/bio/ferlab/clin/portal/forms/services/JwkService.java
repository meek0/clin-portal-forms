package bio.ferlab.clin.portal.forms.services;

import bio.ferlab.clin.portal.forms.configurations.JwkConfiguration;
import bio.ferlab.clin.portal.forms.configurations.SecurityConfiguration;
import com.auth0.jwk.JwkException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@RequiredArgsConstructor
public class JwkService {
  
  private final SecurityConfiguration configuration;
  private final JwkConfiguration jwkConfiguration;

  public void checkToken(DecodedJWT jwt) {
    try {
      final Algorithm algorithm = jwkConfiguration.getAlgorithm(jwt.getKeyId());
      JWT.require(algorithm)
          .withIssuer(configuration.getIssuer())
          .withAudience(configuration.getAudience())
          .acceptExpiresAt(configuration.getLeeway())
          .build().verify(jwt);
    } catch (JwkException e) {
      log.warn("Invalid token: {}", e.getMessage()); // hide from the user + log the reason
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid token");
    } catch( JWTVerificationException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }
}
