package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import bio.ferlab.clin.portal.forms.models.submit.Analyse;
import bio.ferlab.clin.portal.forms.models.submit.ClinicalSign;
import bio.ferlab.clin.portal.forms.models.submit.Exam;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
  
  public ObservationsBuilder validate() {
    for(int i=0; i<clinicalSigns.size();i++) {
      final ClinicalSign cs = clinicalSigns.get(i);
      if (cs.getIsObserved() && cs.getAgeCode() == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("age_code can't be empty for clinical_signs[%s].is_observed = true", i));
      }
    }
    for(int i=0; i<exams.size();i++) {
      final Exam ex = exams.get(i);
      if (Exam.Interpretation.abnormal.equals(ex.getInterpretation()) && ex.getValue() == null && ex.getValues().isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("value and values can't be both empty for paraclinical_exams[%s].interpretation = abnormal", i));
      }
    }
    return this;
  }
  
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
