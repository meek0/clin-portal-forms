package bio.ferlab.clin.portal.forms.services;

import bio.ferlab.clin.portal.forms.configurations.SecurityConfiguration;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;

@Service
@Slf4j
public class JwkService {
  
  private final SecurityConfiguration configuration;
  private final JwkProvider jwkProvider;
  
  public JwkService(SecurityConfiguration configuration) throws MalformedURLException {
    this.configuration = configuration;
    final String jwkUrl = StringUtils.appendIfMissing(configuration.getIssuer(), "/") + "protocol/openid-connect/certs";
    this.jwkProvider = new JwkProviderBuilder(new URL(jwkUrl)).build(); // cached + rate limited by default
  }

  public void checkToken(DecodedJWT jwt) {
    try {
      final Jwk jwk = jwkProvider.get(jwt.getKeyId());
      final Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
      JWT.require(algorithm)
          .withIssuer(configuration.getIssuer())
          .withAudience(configuration.getAudience())
          .acceptExpiresAt(configuration.getLeeway())
          .build().verify(jwt);
    } catch (JwkException e) {
      log.warn("Invalid token: {}", e.getMessage());
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid token");
    } catch( JWTVerificationException e) {
      log.warn("Token verification failed: {}", e.getMessage());
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }
}
