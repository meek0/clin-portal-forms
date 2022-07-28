package bio.ferlab.clin.portal.forms.configurations;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.algorithms.Algorithm;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Configuration;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class JwkConfiguration {

  private final JwkProvider jwkProvider;

  public JwkConfiguration(SecurityConfiguration securityConfiguration) throws MalformedURLException {
    final String jwkUrl = StringUtils.appendIfMissing(securityConfiguration.getIssuer(), "/") + "protocol/openid-connect/certs";
    this.jwkProvider = new JwkProviderBuilder(new URL(jwkUrl)).build(); // cached + rate limited by default
  }

  public Algorithm getAlgorithm(String keyId) throws JwkException {
    final Jwk jwk = jwkProvider.get(keyId);
    return Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
  }
}
