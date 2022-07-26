package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.models.submit.Patient;
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
  private final Patient patient;
  
  private PractitionerRole supervisorRole;
  
  public PractitionerBuilder withSupervisor(String supervisor) {
    if (StringUtils.isNotBlank(supervisor)) {
      try {
        supervisorRole = this.fhirClient.getGenericClient().read().resource(PractitionerRole.class).withId(supervisor).encodedJson().execute();
      }catch(ResourceNotFoundException e){
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "supervisor " + supervisor + " is unknown");
      }
    }
    return this;
  }
  
  public Result build(){
    Bundle response = this.fhirClient.getGenericClient().search().forResource(PractitionerRole.class)
        .where(PractitionerRole.PRACTITIONER.hasId(practitionerId)).returnBundle(Bundle.class).encodedJson().execute();

    BundleExtractor bundleExtractor = new BundleExtractor(fhirClient.getContext(), response);
    List<PractitionerRole> practitionerRoles = bundleExtractor.getAllResourcesOfType(PractitionerRole.class);
    
    final String orgRef = FhirUtils.formatResource(new Organization().setId(patient.getEp()));
    PractitionerRole role = practitionerRoles.stream().filter(r -> orgRef.equals(r.getOrganization().getReference())).findFirst()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("can't find role for practitioner %s in ep %s", practitionerId, patient.getEp())));
    
    return new Result(role, supervisorRole);
  }
  
  @AllArgsConstructor
  @Getter
  public static class Result {
    private PractitionerRole practitionerRole;
    private PractitionerRole supervisorRole;
  }
}
