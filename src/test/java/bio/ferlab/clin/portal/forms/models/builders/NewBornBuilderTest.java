package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import bio.ferlab.clin.portal.forms.models.submit.AdditionalInfo;
import bio.ferlab.clin.portal.forms.utils.FhirConst;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.RelatedPerson;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class NewBornBuilderTest {
  
  @Test
  void build_missing_mother_ramq() {
    final AdditionalInfo additionalInfo = new AdditionalInfo();
    additionalInfo.setIsNewBorn(true);
    final Patient patient = new Patient();
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      new NewBornBuilder(new SubmitToFhirMapper(), additionalInfo, patient).build();
    });
    assertEquals("patient.additional_info is_new_born is true but mother_ramq is empty", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
  }

  @Test
  void build_foetus_and_new_born() {
    final AdditionalInfo additionalInfo = new AdditionalInfo();
    additionalInfo.setIsNewBorn(true);
    additionalInfo.setIsPrenatalDiagnosis(true);
    final Patient patient = new Patient();
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      new NewBornBuilder(new SubmitToFhirMapper(), additionalInfo, patient).build();
    });
    assertEquals("patient.additional_info is_new_born and isPrenatalDiagnosis can't be both true", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
  }

  @Test
  void build_not_new_born() {
    final AdditionalInfo additionalInfo = new AdditionalInfo();
    additionalInfo.setIsNewBorn(false);
    final Patient patient = new Patient();
    NewBornBuilder.Result result = new NewBornBuilder(new SubmitToFhirMapper(), additionalInfo, patient).build();
    assertNull(result.getRelatedPerson());
  }

  @Test
  void build() {
    final AdditionalInfo additionalInfo = new AdditionalInfo();
    additionalInfo.setIsNewBorn(true);
    additionalInfo.setMotherRamq("mother_ramq");
    final Patient patient = new Patient();
    patient.setId("mother_id");
    
    NewBornBuilder.Result result = new NewBornBuilder(new SubmitToFhirMapper(), additionalInfo, patient).build();
    final RelatedPerson relatedPerson = result.getRelatedPerson();
    
    assertNotNull(relatedPerson.getId());
    assertEquals("Patient/mother_id", relatedPerson.getPatient().getReference());
    assertTrue(relatedPerson.getActive());
    
    final Identifier identifier = relatedPerson.getIdentifierFirstRep();
    assertEquals("mother_ramq", identifier.getValue());
    assertEquals(FhirConst.SYSTEM_RAMQ, identifier.getType().getCodingFirstRep().getSystem());
    assertEquals(FhirConst.CODE_RAMQ, identifier.getType().getCodingFirstRep().getCode());
    
    final CodeableConcept relation = relatedPerson.getRelationshipFirstRep();
    assertEquals("Mother", relation.getText());
    assertEquals(FhirConst.SYSTEM_MOTHER, relation.getCodingFirstRep().getSystem());
    assertEquals(FhirConst.CODE_MOTHER, relation.getCodingFirstRep().getCode());
  }

}