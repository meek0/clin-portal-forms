package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import bio.ferlab.clin.portal.forms.models.submit.Exam;
import bio.ferlab.clin.portal.forms.models.submit.Phenotype;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Patient;

import java.util.List;

@RequiredArgsConstructor
public class ObservationsBuilder {
  
  private final SubmitToFhirMapper mapper;
  private final String panelCode;
  private final Patient patient;
  private final List<Phenotype> phenotypes;
  private final String observation;
  private final List<Exam> exams;
  private final String investigation;
  private final String ethnicity;
  private final String indication;
  
  public Result build() {
    List<org.hl7.fhir.r4.model.Observation> obs = mapper.mapToObservations(panelCode, patient, phenotypes, observation, exams, investigation, ethnicity, indication);
    return new Result(obs);
  }
  
  @AllArgsConstructor
  @Getter
  public static class Result {
    List<org.hl7.fhir.r4.model.Observation> observations;
  }
}
