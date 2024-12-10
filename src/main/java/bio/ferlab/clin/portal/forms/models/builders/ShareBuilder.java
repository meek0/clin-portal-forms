package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.utils.BundleExtractor;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static bio.ferlab.clin.portal.forms.utils.FhirConst.ANALYSIS_SERVICE_REQUEST;

@RequiredArgsConstructor
public class ShareBuilder {

  private final FhirClient fhirClient;
  private final String analysisId;
  private final List<String> shareRoles;
  private final String practitionerId;

  public ShareBuilder.Result build() {
    try {
      // find analysis
      var analysis = fhirClient.findServiceRequestById(analysisId);

      // check the profile
      if (!ANALYSIS_SERVICE_REQUEST.equals(analysis.getMeta().getProfile().stream().map(PrimitiveType::getValue).findFirst().orElse(null))) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "service request " + analysisId + " isn't an analysis");
      }

      // find all FHIR roles
      final var allPractitionerRoles = new BundleExtractor(fhirClient.getContext(), fhirClient.findAllPractitionerRoles()).getAllResourcesOfType(PractitionerRole.class);

      var userRoles = allPractitionerRoles.stream()
        .filter(r -> FhirUtils.extractId(r.getPractitioner()).equals(practitionerId))
        .toList();

      if (userRoles.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("practitioner %s has no role", practitionerId));
      }

      var validShareRoles = new ArrayList<String>();

      shareRoles.forEach(shareRole -> {
        var fhirRole = allPractitionerRoles.stream()
          .filter(r -> FhirUtils.extractId(r).equals(shareRole))
          .findFirst()
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "practitioner role " + shareRole + " is unknown"));
        var fhirRoleOrg = FhirUtils.extractId(fhirRole.getOrganization());
        if(!FhirUtils.isDoctor(fhirRole, fhirRoleOrg)) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("practitioner %s isn't a doctor at ep %s", shareRole, fhirRoleOrg));
        }
        validShareRoles.add(shareRole);
      });

      // we always override the previous shared roles
      this.removedPreviousSharedRoles(analysis);

      validShareRoles
        .stream()
        .map(r -> "PractitionerRole/" + r)
        .filter(r -> analysis.getMeta().getSecurity().stream().noneMatch(c -> c.getCode().equals(r)))
        .forEach(r -> analysis.getMeta().getSecurity().add(new Coding().setCode(r)));

      final var updatedAnalysis = this.fhirClient.shareWithRoles(analysis);

      return new ShareBuilder.Result(updatedAnalysis);
    } catch(ResourceNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "service request " + analysisId + " is unknown");
    }
  }

  private void removedPreviousSharedRoles(ServiceRequest analysis) {
    var previousTags = analysis.getMeta().getSecurity().stream()
      .map(Coding::getCode)
      .filter(c -> !c.contains("PractitionerRole/"))
      .toList();

    // add previous tags
    analysis.getMeta().getSecurity().clear();
    previousTags.forEach(t -> analysis.getMeta().addSecurity(new Coding().setCode(t)));
  }

  @AllArgsConstructor
  @Getter
  public static class Result {
    private final ServiceRequest analysis;
  }
}
