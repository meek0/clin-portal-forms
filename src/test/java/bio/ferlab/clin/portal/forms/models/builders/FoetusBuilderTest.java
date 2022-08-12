package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import bio.ferlab.clin.portal.forms.models.submit.AdditionalInfo;
import bio.ferlab.clin.portal.forms.models.submit.Patient;
import bio.ferlab.clin.portal.forms.utils.FhirConst;
import org.hl7.fhir.r4.model.Observation;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FoetusBuilderTest {
  
  final SubmitToFhirMapper mapper = new SubmitToFhirMapper();
  
  @Test
  void build_validate_foetus_gender() {
    final AdditionalInfo additionalInfo = new AdditionalInfo();
    additionalInfo.setIsPrenatalDiagnosis(true);
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      new FoetusBuilder(null, additionalInfo, null).build();
    });
    assertEquals("patient.additional_info foetus_gender is required if is_prenatal_diagnosis is true", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
  }

  @Test
  void build_validate_gestational_age() {
    final AdditionalInfo additionalInfo = new AdditionalInfo();
    additionalInfo.setIsPrenatalDiagnosis(true);
    additionalInfo.setFoetusGender(Patient.Gender.male);
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      new FoetusBuilder(null, additionalInfo, null).build();
    });
    assertEquals("patient.additional_info gestational_age is required if is_prenatal_diagnosis is true", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
  }

  @Test
  void build_validate_gestational_date_ddm() {
    this.build_validate_gestational_date_ddm(AdditionalInfo.GestationalAge.ddm, true);
  }

  @Test
  void build_validate_gestational_date_dpa() {
    this.build_validate_gestational_date_ddm(AdditionalInfo.GestationalAge.dpa, true);
  }

  @Test
  void build_validate_gestational_date_deceased() {
    this.build_validate_gestational_date_ddm(AdditionalInfo.GestationalAge.deceased, false);
  }
  
  void build_validate_gestational_date_ddm(AdditionalInfo.GestationalAge gestationalAge, boolean shouldThrowError) {
    final AdditionalInfo additionalInfo = new AdditionalInfo();
    additionalInfo.setIsPrenatalDiagnosis(true);
    additionalInfo.setFoetusGender(Patient.Gender.male);
    additionalInfo.setGestationalAge(gestationalAge);
    if (shouldThrowError) {
      ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
        new FoetusBuilder(null, additionalInfo, null).build();
      });
      assertEquals("patient.additional_info gestational_date is required if gestational_age is ddm or dpa", exception.getReason());
      assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    } else {
      new FoetusBuilder(mapper, additionalInfo, new org.hl7.fhir.r4.model.Patient()).build();
    }
  }
  
  @Test
  void build_deceased() {
    final AdditionalInfo additionalInfo = new AdditionalInfo();
    additionalInfo.setIsPrenatalDiagnosis(true);
    additionalInfo.setFoetusGender(Patient.Gender.other);
    additionalInfo.setGestationalAge(AdditionalInfo.GestationalAge.deceased);
    final org.hl7.fhir.r4.model.Patient mother = new org.hl7.fhir.r4.model.Patient();
    mother.setId("mother");
    FoetusBuilder.Result result = new FoetusBuilder(mapper, additionalInfo, mother).build();
    final org.hl7.fhir.r4.model.Patient foetus = result.getFoetus();
    assertNull(result.getObservation());
    assertEquals("Patient/mother", foetus.getLinkFirstRep().getOther().getReference());
    assertEquals("seealso", foetus.getLinkFirstRep().getType().toCode());
    assertEquals("other", foetus.getGender().toCode());
    assertEquals("BooleanType[true]", foetus.getDeceased().toString());
  }

  @Test
  void build_dpa() {
    final AdditionalInfo additionalInfo = new AdditionalInfo();
    additionalInfo.setIsPrenatalDiagnosis(true);
    additionalInfo.setFoetusGender(Patient.Gender.female);
    additionalInfo.setGestationalAge(AdditionalInfo.GestationalAge.dpa);
    LocalDate now = LocalDate.now();
    additionalInfo.setGestationalDate(now);
    final org.hl7.fhir.r4.model.Patient mother = new org.hl7.fhir.r4.model.Patient();
    mother.setId("mother");
    FoetusBuilder.Result result = new FoetusBuilder(mapper, additionalInfo, mother).build();
    final org.hl7.fhir.r4.model.Patient foetus = result.getFoetus();
    final Observation observation = result.getObservation();
    assertNotNull(foetus.getId());
    assertEquals("Patient/mother", foetus.getLinkFirstRep().getOther().getReference());
    assertEquals("seealso", foetus.getLinkFirstRep().getType().toCode());
    assertEquals("female", foetus.getGender().toCode());
    assertEquals("BooleanType[false]", foetus.getDeceased().toString());
    assertNotNull(observation.getId());
    assertEquals("final", observation.getStatus().toCode());
    assertEquals("Patient/mother", observation.getSubject().getReference());
    assertEquals("Patient/"+foetus.getId(), observation.getFocusFirstRep().getReference());
    assertEquals(mapper.mapToDate(now), observation.getValueDateTimeType().getValue());
    assertEquals(FhirConst.SYSTEM_DPA, observation.getCode().getCodingFirstRep().getSystem());
    assertEquals(FhirConst.CODE_DPA, observation.getCode().getCodingFirstRep().getCode());
  }

}