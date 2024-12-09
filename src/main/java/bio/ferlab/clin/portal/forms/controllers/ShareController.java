package bio.ferlab.clin.portal.forms.controllers;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.models.builders.ShareBuilder;
import bio.ferlab.clin.portal.forms.models.share.Request;
import bio.ferlab.clin.portal.forms.models.share.Response;
import bio.ferlab.clin.portal.forms.utils.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/share")
@RequiredArgsConstructor
public class ShareController {

  private final FhirClient fhirClient;

  @PostMapping
  public ResponseEntity<Response> share(@RequestHeader String authorization,
                                         @Valid @RequestBody Request request) {

    final String practitionerId = JwtUtils.getProperty(authorization, JwtUtils.FHIR_PRACTITIONER_ID);

    final var builder = new ShareBuilder(fhirClient, request.getAnalysisId(), request.getRoles(), practitionerId);
    final var result = builder
      .build();

    final Response res = new Response(result.getAnalysis().getIdElement().getIdPart(), filterRoles(result.getAnalysis()));
    return  ResponseEntity.ok(res);
  }

  private List<String> filterRoles(ServiceRequest serviceRequest) {
    return serviceRequest.getMeta().getSecurity().stream()
      .map(Coding::getCode)
      .filter(c -> c.startsWith("PractitionerRole/"))
      .map(c -> c.replace("PractitionerRole/", ""))
      .toList();
  }
}
