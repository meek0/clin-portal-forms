package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import bio.ferlab.clin.portal.forms.models.submit.Patient;
import bio.ferlab.clin.portal.forms.utils.FhirConsts;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PatientBuilderTest {
  
  final FhirContext fhirContext = FhirContext.forR4();
  final FhirClient fhirClient = Mockito.mock(FhirClient.class);
  
  @BeforeEach
  void setup() {
    when(fhirClient.getContext()).thenReturn(fhirContext);
  }
  
  @Test
  void validateRamqAndMrn() {
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      final Patient patient = new Patient();
      final PatientBuilder builder = new PatientBuilder(fhirClient, new SubmitToFhirMapper(), patient);
      builder.validateRamqAndMrn();
    });
    assertEquals("patient.ramq and patient.mrn can't be both empty", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
  }

  @Test
  void validateEp() {
    when(fhirClient.findOrganizationById(any())).thenThrow(new ResourceNotFoundException("Organization not found"));
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      final Patient patient = new Patient();
      patient.setEp("foo");
      final PatientBuilder builder = new PatientBuilder(fhirClient, new SubmitToFhirMapper(), patient);
      builder.validateEp();
    });
    assertEquals("patient.ep foo is unknown", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
  }
  
  @Test
  void findByRamq() {
    final Bundle bundle = new Bundle();
    bundle.addEntry().setResource(new Person().setId("p1"));
    bundle.addEntry().setResource(new org.hl7.fhir.r4.model.Patient().setManagingOrganization(FhirUtils.toReference(new Organization().setId("ep1"))).setId("p1"));
    bundle.addEntry().setResource(new org.hl7.fhir.r4.model.Patient().setManagingOrganization(FhirUtils.toReference(new Organization().setId("ep2"))).setId("p2"));
    when(fhirClient.findPersonAndPatientByRamq(any())).thenReturn(bundle);
    final Patient patient = new Patient();
    patient.setRamq("foo");
    patient.setEp("ep2");
    final PatientBuilder builder = new PatientBuilder(fhirClient, new SubmitToFhirMapper(), patient);
    PatientBuilder.Result result = builder.findByRamq().build(false, false);
    assertEquals("p1", result.getPerson().getId());
    assertEquals("p2", result.getPatient().getId());
  }

  @Test
  void findByMrn() {
    final Bundle bundle = new Bundle();
    bundle.addEntry().setResource(new org.hl7.fhir.r4.model.Patient().setId("p1"));
    bundle.addEntry().setResource(new Person().setId("p1"));
    when(fhirClient.findPersonAndPatientByMrnAndEp(any(), any())).thenReturn(bundle);
    final Patient patient = new Patient();
    patient.setMrn("foo");
    patient.setEp("bar");
    final PatientBuilder builder = new PatientBuilder(fhirClient, new SubmitToFhirMapper(), patient);
    PatientBuilder.Result result = builder.findByMrn().build(false, false);
    assertEquals("p1", result.getPerson().getId());
    assertEquals("p1", result.getPatient().getId());
  }
  
  @Test
  void findByRamqOrMrn() {
    final Bundle bundleByRamq = new Bundle();
    bundleByRamq.addEntry().setResource(new Person().setId("p1"));
    final Bundle bundleByMrn = new Bundle();
    bundleByMrn.addEntry().setResource(new org.hl7.fhir.r4.model.Patient().setId("p1"));

    final Patient patient = new Patient();
    patient.setMrn("mrn");
    patient.setRamq("ramq");
    patient.setEp("ep");

    when(fhirClient.findPersonAndPatientByRamq(any())).thenReturn(bundleByRamq);
    when(fhirClient.findPersonAndPatientByMrnAndEp(any(), any())).thenReturn(bundleByMrn);

    final PatientBuilder builder = new PatientBuilder(fhirClient, new SubmitToFhirMapper(), patient);
    PatientBuilder.Result result = builder
        .findByRamq()
        .findByMrn()
        .build(false, false);
    
    assertEquals("p1", result.getPerson().getId());
    assertFalse(result.isPersonNew());
    assertEquals("p1", result.getPatient().getId());
    assertFalse(result.isPatientNew());
  }
  
  @Test
  void createIfMissing() {
    final Patient patient = new Patient();
    patient.setGender(Patient.Gender.male);
    patient.setBirthDate(LocalDate.now());
    patient.setEp("ep");
    patient.setRamq("ramq");
    patient.setMrn("mrn");
    patient.setFirstName("firstname");
    patient.setLastName("lastname");
    
    final PatientBuilder builder = new PatientBuilder(fhirClient, new SubmitToFhirMapper(), patient);
    PatientBuilder.Result result = builder.build(true, false);
    
    assertNotNull(result.getPerson());
    assertNotNull(result.getPerson().getId());
    assertEquals(Enumerations.AdministrativeGender.MALE, result.getPerson().getGender());
    assertEquals("ramq", result.getPerson().getIdentifierFirstRep().getValue());
    assertEquals("firstname", result.getPerson().getNameFirstRep().getGivenAsSingleString());
    assertEquals("lastname", result.getPerson().getNameFirstRep().getFamily());
    assertEquals("Patient/"+result.getPatient().getId(), result.getPerson().getLinkFirstRep().getTarget().getReference());
    
    assertNotNull(result.getPatient());
    assertNotNull(result.getPatient().getId());
    assertEquals(Enumerations.AdministrativeGender.MALE, result.getPatient().getGender());
    assertEquals("Organization/ep", result.getPatient().getManagingOrganization().getReference());
    assertEquals("mrn", result.getPatient().getIdentifierFirstRep().getValue());
  }

  @Test
  void update_identifier() {
    final Patient patient = new Patient();
    patient.setGender(Patient.Gender.male);
    patient.setBirthDate(LocalDate.now());
    patient.setRamq("new_ramq");

    final Bundle bundleByRamq = new Bundle();
    final Person person = new Person();
    person.getIdentifierFirstRep().setValue("ramq").getType().getCodingFirstRep().setCode(FhirConsts.CODE_RAMQ).setSystem(FhirConsts.SYSTEM_RAMQ);
    bundleByRamq.addEntry().setResource(person);
    
    when(fhirClient.findPersonAndPatientByRamq(any())).thenReturn(bundleByRamq);

    final PatientBuilder builder = new PatientBuilder(fhirClient, new SubmitToFhirMapper(), patient);
    PatientBuilder.Result result = builder.findByRamq().build(true, true);
    
    assertEquals("new_ramq", result.getPerson().getIdentifierFirstRep().getValue());

  }

}