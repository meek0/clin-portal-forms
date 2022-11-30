package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.ServiceRequest;

@RequiredArgsConstructor
public class SequencingBuilder {
  
  private final SubmitToFhirMapper mapper;
  private final String panelCode;
  private final Patient patient;
  private final ServiceRequest analysis;
  private final PractitionerRole practitionerRole;
  private Patient foetus;
  
  public SequencingBuilder withFoetus(Patient foetus) {
    this.foetus = foetus;
    return this;
  }
  
  public Result build() {
    ServiceRequest serviceRequest = null;
    if (patient !=null) {
      serviceRequest = mapper.mapToSequencing(panelCode, patient, analysis, practitionerRole, foetus);
    }
    return new Result(serviceRequest);
  }
  
  @AllArgsConstructor
  @Getter
  public static class Result {
    private final ServiceRequest sequencing;
  }
}
