package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import bio.ferlab.clin.portal.forms.models.submit.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class ObservationsBuilder {
  
  private final SubmitToFhirMapper mapper;
  private final String panelCode;
  private final Patient patient;
  private final HistoryAndDiag historyAndDiag;
  private final ClinicalSigns signs;
  private final ParaclinicalExams exams;
  private Observation foetusObservation;
  private Parent mother;
  private Parent father;
  
  public ObservationsBuilder withFoetus(Observation o) {
    this.foetusObservation = o;
    return this;
  }

  public ObservationsBuilder withMother(Parent mother) {
    this.mother = mother;
    return this;
  }

  public ObservationsBuilder withFather(Parent father) {
    this.father = father;
    return this;
  }
  
  public ObservationsBuilder validate() {
    if (signs != null) {
      for (int i = 0; i < signs.getSigns().size(); i++) {
        final Signs cs = signs.getSigns().get(i);
        if (cs.getIsObserved() && cs.getAgeCode() == null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("age_code can't be empty for clinical_signs[%s].is_observed = true", i));
        }
      }
    }
    if (exams != null) {
      for (int i = 0; i < exams.getExams().size(); i++) {
        final Exams ex = exams.getExams().get(i);
        if (Exams.Interpretation.abnormal.equals(ex.getInterpretation()) && ex.getValue() == null && CollectionUtils.isEmpty(ex.getValues())) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("value and values can't be both empty for paraclinical_exams[%s].interpretation = abnormal", i));
        }
      }
    }
    return this;
  }
  
  public Result build() {
    List<org.hl7.fhir.r4.model.Observation> obs = new ArrayList<>();
    if (patient != null) {
      obs = mapper.mapToObservations(panelCode, patient, mother, father, historyAndDiag, signs, exams);
      if (foetusObservation != null) {
        obs.add(foetusObservation);
      }
    }
    return new Result(obs);
  }
  
  @AllArgsConstructor
  @Getter
  public static class Result {
    List<org.hl7.fhir.r4.model.Observation> observations;
  }
}
