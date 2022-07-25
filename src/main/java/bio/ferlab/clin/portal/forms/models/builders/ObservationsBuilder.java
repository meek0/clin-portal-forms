package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import bio.ferlab.clin.portal.forms.models.submit.Analyse;
import bio.ferlab.clin.portal.forms.models.submit.ClinicalSign;
import bio.ferlab.clin.portal.forms.models.submit.Exam;
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
  private final Analyse analyse;
  private final List<ClinicalSign> clinicalSigns;
  private final List<Exam> exams;
  private final String ethnicity;
  
  public Result build() {
    List<org.hl7.fhir.r4.model.Observation> obs = mapper.mapToObservations(panelCode, patient, analyse, clinicalSigns, exams, ethnicity);
    return new Result(obs);
  }
  
  @AllArgsConstructor
  @Getter
  public static class Result {
    List<org.hl7.fhir.r4.model.Observation> observations;
  }
}
