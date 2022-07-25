package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import bio.ferlab.clin.portal.forms.models.submit.Observation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ObservationsBuilder {
  
  private final SubmitToFhirMapper mapper;
  private final String panelCode;
  private final List<Observation> phenotypes;
  private final String observation;
  private final List<Observation> exams;
  private final String investigation;
  
  public Result build() {
    List<org.hl7.fhir.r4.model.Observation> obs = mapper.mapToObservations(panelCode, phenotypes, observation, exams, investigation);
    return new Result(obs);
  }
  
  @AllArgsConstructor
  @Getter
  public static class Result {
    List<org.hl7.fhir.r4.model.Observation> observations;
  }
}
