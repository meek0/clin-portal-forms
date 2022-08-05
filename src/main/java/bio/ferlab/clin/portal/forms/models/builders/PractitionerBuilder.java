package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.utils.BundleExtractor;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
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

@RequiredArgsConstructor
public class PractitionerBuilder {
  
  private final FhirClient fhirClient;
  private final String practitionerId;
  
  private String ep;
  private PractitionerRole supervisorRole;
  
  public PractitionerBuilder withSupervisor(String supervisor) {
    if (StringUtils.isNotBlank(supervisor)) {
      try {
        supervisorRole = this.fhirClient.findPractitionerRoleById(supervisor);
      }catch(ResourceNotFoundException e){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "supervisor " + supervisor + " is unknown");
      }
    }
    return this;
  }
  
  public PractitionerBuilder withEp(String ep) {
    this.ep = ep;
    return this;
  }
  
  public Result build(){
    Bundle response = this.fhirClient.findPractitionerRoleByPractitionerId(practitionerId);

    BundleExtractor bundleExtractor = new BundleExtractor(fhirClient.getContext(), response);
    List<PractitionerRole> practitionerRoles = bundleExtractor.getAllResourcesOfType(PractitionerRole.class);

    if (practitionerRoles.isEmpty()) {
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
  
  @AllArgsConstructor
  @Getter
  public static class Result {
    private List<PractitionerRole> practitionerRoles;
    private PractitionerRole practitionerRole;
    private PractitionerRole supervisorRole;
  }
}
