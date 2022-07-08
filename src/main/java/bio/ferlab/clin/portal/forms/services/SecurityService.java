package bio.ferlab.clin.portal.forms.services;

import bio.ferlab.clin.portal.forms.configurations.SecurityConfiguration;
import bio.ferlab.clin.portal.forms.utils.JwtUtils;
import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class SecurityService {

  private final SecurityConfiguration configuration;
  private final JwkService jwkService;
  
  public SecurityService(SecurityConfiguration configuration, JwkService jwkService){
    this.configuration = configuration;
    this.jwkService = jwkService;
  }

  public void checkAuthorization(String authorization) {
    if(configuration.isEnabled()) {
      if (StringUtils.isBlank(authorization)) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing token");
      }
      try {
        final String token = JwtUtils.removeBearerPrefix(authorization);
        DecodedJWT jwt = JWT.decode(token);
        this.jwkService.checkToken(jwt);
      } catch(JWTDecodeException e) {
        log.warn("Invalid token: {}", e.getMessage());  // hide from the user + log the reason
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid token");
      }
    }
  }

}
