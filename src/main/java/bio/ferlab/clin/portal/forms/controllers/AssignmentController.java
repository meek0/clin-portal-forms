package bio.ferlab.clin.portal.forms.controllers;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.models.assignment.Request;
import bio.ferlab.clin.portal.forms.models.assignment.Response;
import bio.ferlab.clin.portal.forms.models.builders.AssignmentBuilder;
import bio.ferlab.clin.portal.forms.utils.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/assignment")
@RequiredArgsConstructor
public class AssignmentController {

  private final FhirClient fhirClient;

  @PostMapping
  public ResponseEntity<Response> submit(@RequestHeader String authorization,
                                         @Valid @RequestBody Request request) {

    final List<String> roles = JwtUtils.getUserRoles(authorization);
    final var builder = new AssignmentBuilder(fhirClient, request.getAnalysisId(), request.getAssignments());
    final var result = builder.build();

    final Response res = new Response(result.getAnalysis().getIdElement().getIdPart(), filterPerformers(result.getAnalysis()));
    return  ResponseEntity.ok(res);
  }

  private List<String> filterPerformers(ServiceRequest serviceRequest) {
    return serviceRequest.getPerformer().stream()
      .filter(p -> p.getReference().startsWith(PractitionerRole.class.getSimpleName()))
      .map(p -> p.getReferenceElement().getIdPart())
      .collect(Collectors.toList());
  }
}
