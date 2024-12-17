package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.models.Prescription;
import bio.ferlab.clin.portal.forms.services.PrescriptionService;
import bio.ferlab.clin.portal.forms.utils.BundleExtractor;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ShareBuilder {

  public static final List<String> RESOURCES_WITH_TAGS = List.of("ServiceRequest", "Patient", "Observation", "ClinicalImpression");

  private final PrescriptionService prescriptionService;
  private final FhirClient fhirClient;
  private final String analysisId;
  private final List<String> shareRoles;
  private final String practitionerId;

  // remember all the previously shared roles because we want to delete them first from meta
  private final List<String> previousRoles = new ArrayList<>();

  public ShareBuilder.Result build() {
    final var prescription = prescriptionService.fromAnalysisId(analysisId);

    // find all FHIR roles (cached)
    final var allRoles = new BundleExtractor(fhirClient.getContext(), fhirClient.findAllPractitionerRoles()).getAllResourcesOfType(PractitionerRole.class);

    final var userRoles = this.getUserRoles(allRoles);
    this.validateShareRoles(prescription, allRoles, userRoles);

    final var updatedResources = this.updateSecurityTags(prescription);

    log.info("Share service request {} with roles {}", analysisId, shareRoles);
    fhirClient.updateSharePractitionerRoles(updatedResources, previousRoles.stream().filter(r -> r.startsWith("PractitionerRole/")).distinct().toList());

    return new ShareBuilder.Result(prescription.getAnalysis());
  }

  private List<PractitionerRole> getUserRoles(List<PractitionerRole> allRoles) {
    var userRoles = allRoles.stream()
      .filter(r -> FhirUtils.extractId(r.getPractitioner()).equals(practitionerId))
      .toList();

    if (userRoles.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("practitioner: %s has no roles", practitionerId));
    }
    return userRoles;
  }

  private void validateShareRoles(Prescription prescription, List<PractitionerRole> allRoles, List<PractitionerRole> userRoles) {
    var ep = FhirUtils.extractId(prescription.getOrganization());
    shareRoles.forEach(shareRole -> {
      var role = allRoles.stream().filter(r -> FhirUtils.extractId(r).equals(shareRole)).findFirst()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "practitioner role: " + shareRole + " is unknown"));
      var roleOrg = FhirUtils.extractId(role.getOrganization());
      if (!roleOrg.equals(ep)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("practitioner role: %s belongs to another organization: %s than the prescription: %s", shareRole, roleOrg, ep));
      }
      if(!FhirUtils.isDoctor(role, roleOrg)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("practitioner role: %s isn't a doctor at organization: %s", shareRole, roleOrg));
      }
      if (userRoles.stream().anyMatch(r -> FhirUtils.extractId(r).equals(shareRole))) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("practitioner: %s can't share with themself: %s", practitionerId, shareRole));
      }
    });
  }

  private List<IBaseResource> updateSecurityTags(Prescription prescription) {
    final var resources = new ArrayList<IBaseResource>();
    prescription.getAllResources().forEach(r -> {
      if (r instanceof ServiceRequest sr) {
        this.addTagCode(sr, true);
        resources.add(sr);
      } else if (r instanceof Patient p) {
        this.addTagCode(p, false);  // Could be shared with multiple prescriptions
        resources.add(p);
      } else if (r instanceof Observation o) {
        this.addTagCode(o, true);
        resources.add(o);
      } else if (r instanceof ClinicalImpression ci) {
        this.addTagCode(ci, true);
        resources.add(ci);
      }
    });
    return resources;
  }

  private void addTagCode(Resource resource, boolean erasePrevious) {
    // previous roles to delete from meta
    resource.getMeta().getSecurity().forEach(c -> previousRoles.add(c.getCode()));

    if (erasePrevious) {
      var previousTags = resource.getMeta().getSecurity().stream()
        .map(Coding::getCode)
        .filter(c -> !c.contains("PractitionerRole/"))
        .toList();
      resource.getMeta().getSecurity().clear();
      previousTags.forEach(t -> resource.getMeta().addSecurity(new Coding().setCode(t)));
    }
    shareRoles.forEach(role -> addTagCode(resource, "PractitionerRole/" + role));
  }

  private void addTagCode(Resource resource, String code) {
    if (StringUtils.isNotBlank(code) && resource.getMeta().getSecurity().stream().noneMatch(c -> c.getCode().equals(code))) {
      resource.getMeta().addSecurity(new Coding().setCode(code));
    }
  }

  @AllArgsConstructor
  @Getter
  public static class Result {
    private final ServiceRequest analysis;
  }
}
