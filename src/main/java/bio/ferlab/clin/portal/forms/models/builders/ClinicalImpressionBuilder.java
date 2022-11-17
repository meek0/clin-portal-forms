package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.*;

import java.util.List;

@RequiredArgsConstructor
public class ClinicalImpressionBuilder {
  
  private final SubmitToFhirMapper mapper;
  private final Person person;
  private final Patient patient;
  private final List<Observation> observations;
  private final List<FamilyMemberHistory> histories;
  
  public Result build() {
    ClinicalImpression clinicalImpression = null;
    if (person != null && patient != null) {
      clinicalImpression = mapper.mapToClinicalImpression(person, patient, observations, histories);
    }
    return new Result(clinicalImpression);
  }
  
  @AllArgsConstructor
  @Getter
  public static class Result {
    private final ClinicalImpression clinicalImpression;
  }
}
