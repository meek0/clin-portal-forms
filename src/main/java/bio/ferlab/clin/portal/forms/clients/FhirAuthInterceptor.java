package bio.ferlab.clin.portal.forms.clients;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
public class FhirAuthInterceptor implements IClientInterceptor {

  @Autowired
  private HttpServletRequest request; // current request
  
  @Override
  public void interceptRequest(IHttpRequest fhirRequest) {
    fhirRequest.addHeader(HttpHeaders.AUTHORIZATION, request.getHeader(HttpHeaders.AUTHORIZATION));
  }

  @Override
  public void interceptResponse(IHttpResponse fhirResponse) throws IOException {
    // Nothing to do here
  }
}
