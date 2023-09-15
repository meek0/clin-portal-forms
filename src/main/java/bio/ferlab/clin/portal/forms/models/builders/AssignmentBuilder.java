package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.utils.BundleExtractor;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static bio.ferlab.clin.portal.forms.utils.FhirConst.*;

@RequiredArgsConstructor
public class AssignmentBuilder {

  private final FhirClient fhirClient;
  private final String analysisId;
  private final List<String> assignments;

  public AssignmentBuilder withRoles(List<String> roles) {
    if (!roles.contains(USER_ROLE_GENETICIAN)){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user role '"+USER_ROLE_GENETICIAN+"' is required");
    }
    return this;
  }

  public AssignmentBuilder.Result build() {
    try {
      // find analysis
      final ServiceRequest analysis = fhirClient.findServiceRequestById(analysisId);
      // check the profile
      final String profile = analysis.getMeta().getProfile().stream().map(PrimitiveType::getValue).findFirst().orElse(null);
      if (!ANALYSIS_SERVICE_REQUEST.equals(profile)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "service request " + analysisId + " isn't an analysis");
      }
      // find roles
      final var roles = new BundleExtractor(fhirClient.getContext(), fhirClient.findAllPractitionerRoles()).getAllResourcesOfType(PractitionerRole.class);
      // check roles
      assignments.forEach(ass -> {
        final var role = roles.stream().filter(r -> r.getIdElement().getIdPart().equals(ass)).findFirst();
        if (role.isEmpty()) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "practitioner role " + ass + " is unknown");
        }
        final var code = role.get().getCode().stream().filter(c -> PRACTITIONER_ROLE_GENETICIAN_SYSTEM.equals(c.getCodingFirstRep().getSystem()))
          .findFirst().map(c -> c.getCodingFirstRep().getCode()).orElse(null);
        if (!GENETICIAN_CODE.equals(code)) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "practitioner role " + ass + " isn't a genetician");
        }
      });
      // build performer list
      final var updatedPerformers = new ArrayList<>(FhirUtils.filterByTypes(analysis.getPerformer(), PractitionerRole.class));
      assignments.forEach(ass -> {
        updatedPerformers.add(new Reference("PractitionerRole/"+ass));
      });
      analysis.setPerformer(updatedPerformers);
      // update analysis
      final var updatedAnalysis = this.fhirClient.assignPerformers(analysis);
      return new AssignmentBuilder.Result(updatedAnalysis);
    } catch(ResourceNotFoundException e) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "service request " + analysisId + " is unknown");
    }
  }

  @AllArgsConstructor
  @Getter
  public static class Result {
    private final ServiceRequest analysis;
  }
}
