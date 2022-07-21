package bio.ferlab.clin.portal.forms.utils;

import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ServiceRequest;

@AllArgsConstructor
public class SequencingBuilder {
  
  private final SubmitToFhirMapper mapper;
  private final Patient patient;
  private final ServiceRequest analysis;
  
  public Result build() {
    final ServiceRequest serviceRequest = mapper.mapToSequencing(patient, analysis);
    return new Result(serviceRequest);
  }
  
  @AllArgsConstructor
  @Getter
  public static class Result {
    private final ServiceRequest sequencing;
  }
}
