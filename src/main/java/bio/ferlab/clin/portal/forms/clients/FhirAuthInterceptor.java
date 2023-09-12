package bio.ferlab.clin.portal.forms.clients;

import bio.ferlab.clin.portal.forms.utils.JwtUtils;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class FhirAuthInterceptor implements IClientInterceptor {

  private final HttpServletRequest request; // current request

  @Override
  public void interceptRequest(IHttpRequest fhirRequest) {
    // FHIR will validate the token's authorizations
    fhirRequest.addHeader(HttpHeaders.AUTHORIZATION, sanitizeAuth(request.getHeader(HttpHeaders.AUTHORIZATION)));
  }

  // Neutralization of CRLF Sequences in HTTP Headers
  private String sanitizeAuth(String auth) {
    if (!StringUtils.isBlank(auth) && !auth.startsWith(JwtUtils.BEARER_PREFIX)) {
      throw new RuntimeException("Token forwarded to FHIR is invalid: " + StringUtils.abbreviate(auth, 20));
    }
    return auth;
  }

  @Override
  public void interceptResponse(IHttpResponse fhirResponse) throws IOException {
    // Nothing to do here
  }
}
