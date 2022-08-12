package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import bio.ferlab.clin.portal.forms.models.submit.AdditionalInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;

import java.util.EnumSet;

@RequiredArgsConstructor
public class FoetusBuilder {
  
  private final SubmitToFhirMapper mapper;
  private final AdditionalInfo additionalInfo;
  private final Patient mother;
  
  public Result build() {
    Patient foetus = null;
    Observation observation = null;
    if (Boolean.TRUE.equals(additionalInfo.getIsPrenatalDiagnosis())) {
      additionalInfo.validate();
      foetus = mapper.mapToFoetus(additionalInfo, mother);
      if (EnumSet.of(AdditionalInfo.GestationalAge.ddm, AdditionalInfo.GestationalAge.dpa).contains(additionalInfo.getGestationalAge())) {
        observation = mapper.mapToObservation(additionalInfo, mother, foetus);
      }
    }
    return new Result(foetus, observation);
  }
  
  @Getter
  @AllArgsConstructor
  public static class Result {
    private final Patient foetus;
    private final Observation observation;
  }
}
