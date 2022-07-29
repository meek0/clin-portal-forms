package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import bio.ferlab.clin.portal.forms.models.submit.Analyse;
import bio.ferlab.clin.portal.forms.models.submit.ClinicalSign;
import bio.ferlab.clin.portal.forms.models.submit.Exam;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

import static bio.ferlab.clin.portal.forms.utils.FhirConstants.*;
import static org.junit.jupiter.api.Assertions.*;

class ObservationsBuilderTest {
  
  final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandom();
  
  @Test
  void validate_empty_age_code() {
    final List<ClinicalSign> signs = random.objects(ClinicalSign.class, 5).collect(Collectors.toList());
    signs.get(2).setIsObserved(true);
    signs.get(2).setAgeCode(null);
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> { 
      new ObservationsBuilder(null, null, null, null, signs, List.of(), null).validate();
    });
    assertEquals("age_code can't be empty for clinical_signs[2].is_observed = true", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
  }

  @Test
  void validate_empty_values() {
    final List<Exam> exams = random.objects(Exam.class, 5).collect(Collectors.toList());
    exams.get(3).setInterpretation(Exam.Interpretation.abnormal);
    exams.get(3).setValue(null);
    exams.get(3).setValues(null);
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      new ObservationsBuilder(null, null, null, null, List.of(), exams,null).validate();
    });
    assertEquals("value and values can't be both empty for paraclinical_exams[3].interpretation = abnormal", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
  }
  
  @Test
  void build() {
    final Patient patient = new Patient();
    patient.setId("foo");
    final Analyse analyse = random.nextObject(Analyse.class);

    ClinicalSign cs1 = new ClinicalSign();
    cs1.setIsObserved(true);
    cs1.setAgeCode("age");
    cs1.setValue("sign");

    ClinicalSign cs2 = new ClinicalSign();
    cs2.setIsObserved(false);
    cs2.setValue("sign");
    
    final List<ClinicalSign> signs = List.of(cs1, cs2);
    
    Exam ex1 = new Exam();
    ex1.setCode("code1");
    ex1.setInterpretation(Exam.Interpretation.normal);

    Exam ex2 = new Exam();
    ex2.setCode("code2");
    ex2.setValue("value");
    ex2.setInterpretation(Exam.Interpretation.abnormal);

    Exam ex3 = new Exam();
    ex3.setCode("code3");
    ex3.setValues(List.of("value1", "value2"));
    ex3.setInterpretation(Exam.Interpretation.abnormal);
    
    final List<Exam> exams = List.of(ex1, ex2, ex3);
    
    final ObservationsBuilder.Result result = new ObservationsBuilder(new SubmitToFhirMapper(), "code", patient, analyse, signs, exams, "ethnicity")
        .build();
    
    final List<Observation> obs = result.getObservations();

    assertObservation(obs.get(0), patient, "DSTA", "exam", true, ANALYSIS_REQUEST_CODE, "code", true);
    assertObservation(obs.get(1), patient, "OBSG", "exam", null, null, analyse.getObservation(), true);
    assertObservation(obs.get(2), patient, "INVES", "exam", null, null, analyse.getInvestigation(), true);
    assertObservation(obs.get(3), patient, "ETHN", "exam", null, ETHNICITY_CODE, "ethnicity", true);
    assertObservation(obs.get(4), patient, "INDIC", "exam", null, null, analyse.getIndication(), true);

    assertObservation(obs.get(5), patient, "PHEN", "exam", true, HP_CODE, "sign", true);
    assertEquals("age", ((Coding)obs.get(5).getExtensionByUrl(AGE_AT_ONSET_EXT).getValue()).getCode());

    assertObservation(obs.get(6), patient, "PHEN", "exam", false, HP_CODE, "sign", true);
    assertNull(obs.get(6).getExtensionByUrl(AGE_AT_ONSET_EXT));

    assertObservation(obs.get(7), patient, "code1", "procedure", null, null, null, false);
    assertEquals(OBSERVATION_INTERPRETATION,obs.get(7).getInterpretationFirstRep().getCodingFirstRep().getSystem());
    assertEquals("N",obs.get(7).getInterpretationFirstRep().getCodingFirstRep().getCode());

    assertObservation(obs.get(8), patient, "code2", "procedure", null, null, "value", false);
    assertEquals(OBSERVATION_INTERPRETATION,obs.get(8).getInterpretationFirstRep().getCodingFirstRep().getSystem());
    assertEquals("A",obs.get(8).getInterpretationFirstRep().getCodingFirstRep().getCode());

    assertObservation(obs.get(9), patient, "code3", "procedure", null, null, null, false);
    assertEquals(OBSERVATION_INTERPRETATION,obs.get(9).getInterpretationFirstRep().getCodingFirstRep().getSystem());
    assertEquals("A",obs.get(9).getInterpretationFirstRep().getCodingFirstRep().getCode());
    assertEquals(HP_CODE,obs.get(9).getValueCodeableConcept().getCoding().get(0).getSystem());
    assertEquals("value1",obs.get(9).getValueCodeableConcept().getCoding().get(0).getCode());
    assertEquals(HP_CODE,obs.get(9).getValueCodeableConcept().getCoding().get(1).getSystem());
    assertEquals("value2",obs.get(9).getValueCodeableConcept().getCoding().get(1).getCode());
  }
  
  private void assertObservation(Observation obs, Patient patient, String code, String category, Boolean isObserved, String system, String value, boolean checkInterpretation) {
    assertNotNull(obs.getId());
    assertEquals(FhirUtils.formatResource(patient), obs.getSubject().getReference());
    assertEquals(OBSERVATION_CODE, obs.getCode().getCodingFirstRep().getSystem());
    assertEquals(code, obs.getCode().getCodingFirstRep().getCode());
    assertEquals(OBSERVATION_PROFILE, obs.getMeta().getProfile().get(0).getValue());
    assertEquals(Observation.ObservationStatus.FINAL, obs.getStatus());
    assertEquals(OBSERVATION_CATEGORY, obs.getCategoryFirstRep().getCodingFirstRep().getSystem());
    assertEquals(category, obs.getCategoryFirstRep().getCodingFirstRep().getCode());
    if (isObserved != null) {
      assertEquals(OBSERVATION_INTERPRETATION, obs.getInterpretationFirstRep().getCodingFirstRep().getSystem());
      assertEquals(isObserved ? "POS" : "NEG", obs.getInterpretationFirstRep().getCodingFirstRep().getCode());
    } else if (checkInterpretation) {
      assertEquals(0, obs.getInterpretation().size());
    }
    if (value != null) {
      if (system != null) {
        assertEquals(system, obs.getValueCodeableConcept().getCodingFirstRep().getSystem());
        assertEquals(value, obs.getValueCodeableConcept().getCodingFirstRep().getCode());
      } else {
        assertEquals(value, obs.getValueStringType().getValue());
      }
    }
  }
  
}