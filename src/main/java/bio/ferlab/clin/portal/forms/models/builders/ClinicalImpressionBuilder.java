package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.ClinicalImpression;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Person;

import java.util.List;

@RequiredArgsConstructor
public class ClinicalImpressionBuilder {
  
  private final SubmitToFhirMapper mapper;
  private final Person person;
  private final Patient patient;
  private final List<Observation> observations;
  
  public Result build() {
    final ClinicalImpression clinicalImpression = mapper.mapToClinicalImpression(person, patient, observations);
    return new Result(clinicalImpression);
  }
  
  @AllArgsConstructor
  @Getter
  public static class Result {
    private final ClinicalImpression clinicalImpression;
  }
}
