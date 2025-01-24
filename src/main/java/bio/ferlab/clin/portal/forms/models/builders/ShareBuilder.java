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
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ShareBuilder {

  private final PrescriptionService prescriptionService;
  private final FhirClient fhirClient;
  private final String analysisId;
  private final List<String> shareRoles;
  private final String practitionerId;

  public ShareBuilder.Result build() {
    final var prescription = prescriptionService.fromAnalysisId(analysisId);

    // find all FHIR roles (cached)
    final var allRoles = new BundleExtractor(fhirClient.getContext(), fhirClient.findAllPractitionerRoles()).getAllResourcesOfType(PractitionerRole.class);

    final var userRoles = this.getUserRoles(allRoles);
    this.validateShareRoles(prescription, allRoles, userRoles);

    var tagsToDelete = getTagsToDelete(prescription); // find shared roles that exist but arent shared anymore
    final var updatedResources = this.updateSecurityTags(prescription);

    log.info("Share service request {} with roles {} delete previous {}", analysisId, shareRoles, tagsToDelete);
    fhirClient.updateSharePractitionerRoles(updatedResources, tagsToDelete);

    return new ShareBuilder.Result(prescription.getAnalysis());
  }

  private List<String> getTagsToDelete(Prescription prescription) {
    var tagsToDelete = new ArrayList<String>();
    for(var resource : prescription.getAllResources()) {
      var previousTags = resource.getMeta().getSecurity().stream()
        .map(IBaseCoding::getCode)
        .filter(code -> code.startsWith("PractitionerRole/")) // ignore not role tags
        .filter(code -> shareRoles.stream().noneMatch(r -> r.equals(FhirUtils.extractId(code))))
        .toList();
      tagsToDelete.addAll(previousTags);
    }
    return tagsToDelete.stream().distinct().toList();
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
      if(!FhirUtils.isDoctor(role, roleOrg) && !FhirUtils.isGeneticCounselor(role, ep) && !FhirUtils.isResidentPhysician(role, ep)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("practitioner role: %s isn't a doctor|genetic counselor|resident physician at organization: %s", shareRole, roleOrg));
      }
      if (shareRoles.stream().anyMatch(r -> r.equals(FhirUtils.extractId(prescription.getAnalysis().getRequester())))) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("practitioner: %s can't share with the requester: %s", practitionerId, FhirUtils.extractId(prescription.getAnalysis().getRequester())));
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
