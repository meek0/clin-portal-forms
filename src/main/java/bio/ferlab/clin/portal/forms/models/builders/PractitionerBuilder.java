package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.utils.BundleExtractor;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import bio.ferlab.clin.portal.forms.utils.JwtUtils;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static bio.ferlab.clin.portal.forms.utils.FhirConst.LDM_PREFIX;

@RequiredArgsConstructor
public class PractitionerBuilder {

  private final FhirClient fhirClient;
  private final String practitionerId;
  private final boolean checkRoles;

  private String ep;
  private PractitionerRole supervisorRole;

  public PractitionerBuilder withSupervisor(String supervisor, String ep) {
    if (StringUtils.isNotBlank(supervisor)) {
      try {
        supervisorRole = this.fhirClient.findPractitionerRoleById(supervisor);
        if (!FhirUtils.isDoctor(supervisorRole, ep)) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("practitioner %s isn't a doctor at ep %s", supervisor, ep));
        }
      }catch(ResourceNotFoundException e){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "supervisor " + supervisor + " is unknown");
      }
    }
    return this;
  }

  public PractitionerBuilder(FhirClient fhirClient, String practitionerId) {
    this.fhirClient = fhirClient;
    this.practitionerId = practitionerId;
    this.checkRoles = true;
  }

  public PractitionerBuilder withEp(String ep) {
    this.ep = ep;
    return this;
  }

  public Result build(){
    Bundle response = this.fhirClient.findPractitionerRoleByPractitionerId(practitionerId);

    BundleExtractor bundleExtractor = new BundleExtractor(fhirClient.getContext(), response);
    List<PractitionerRole> practitionerRoles = bundleExtractor.getAllResourcesOfType(PractitionerRole.class)
      .stream().filter(r -> !FhirUtils.extractId(r.getOrganization().getReference()).startsWith(LDM_PREFIX)).toList();

    if (this.checkRoles && practitionerRoles.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("practitioner %s has no roles", practitionerId));
    }

    PractitionerRole practitionerRole = null;
    if(StringUtils.isNotBlank(ep)) {
      final String orgRef = FhirUtils.formatResource(new Organization().setId(ep));
      practitionerRole = practitionerRoles.stream().filter(r -> orgRef.equals(r.getOrganization().getReference())).findFirst()
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("practitioner %s has no role in ep %s", practitionerId, ep)));

    }

    return new Result(practitionerRoles, practitionerRole, supervisorRole);
  }

  // similar as new PractitionerBuilder(...).withEp(...).build() + extract practitioner id from token
  public static void validateAccessToEp(FhirClient fhirClient, String authorization, String ep) {
    final String practitionerId = JwtUtils.getProperty(authorization, JwtUtils.FHIR_PRACTITIONER_ID);
    Result result = new PractitionerBuilder(fhirClient, practitionerId).withEp(ep).build();
    assert(result.practitionerRole != null); // just in case the exception line 59 isn't thrown
  }

  @AllArgsConstructor
  @Getter
  public static class Result {
    private List<PractitionerRole> practitionerRoles;
    private PractitionerRole practitionerRole;
    private PractitionerRole supervisorRole;
  }
}
