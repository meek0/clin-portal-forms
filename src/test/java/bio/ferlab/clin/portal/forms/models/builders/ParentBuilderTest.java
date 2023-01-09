package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import bio.ferlab.clin.portal.forms.models.submit.Parent;
import bio.ferlab.clin.portal.forms.models.submit.Patient;
import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ParentBuilderTest {

  final FhirContext fhirContext = FhirContext.forR4();
  final FhirClient fhirClient = Mockito.mock(FhirClient.class);
  final SubmitToFhirMapper mapper = new SubmitToFhirMapper();

  @BeforeEach
  void setup() {
    when(fhirClient.getContext()).thenReturn(fhirContext);
  }

  @Test
  void validate_now_ep_names() {
    final Parent parent = new Parent();
    parent.setParentEnterMoment(Parent.Moment.now);
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      new ParentBuilder(fhirClient, mapper, parent).build();
    });
    assertEquals("[mother|father].[ep|lastName|firstName] can't be blank", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
  }

  @Test
  void validate_now_gender_birthday() {
    final Parent parent = new Parent();
    parent.setParentEnterMoment(Parent.Moment.now);
    parent.setEp("ep");
    parent.setFirstName("firstName");
    parent.setLastName("lastName");
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      new ParentBuilder(fhirClient, mapper, parent).build();
    });
    assertEquals("[mother|father].[birthDate|gender] can't be null", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
  }

  @Test
  void validate_affected() {
    final Parent parent = new Parent();
    parent.setParentClinicalStatus(Parent.Status.affected);
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      new ParentBuilder(fhirClient, mapper, parent).build();
    });
    assertEquals("[mother|father].signs can't be empty if [mother|father].parent_clinical_status = affected", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
  }

  @Test
  void build() {
    final Parent parent = new Parent();
    parent.setParentEnterMoment(Parent.Moment.now);
    parent.setEp("ep");
    parent.setMrn("mrn");
    // no ramq to simplify the test
    parent.setBirthDate(LocalDate.now());
    parent.setGender(Patient.Gender.other);
    parent.setFirstName("firstname");
    parent.setLastName("lastNamae");

    when(fhirClient.findPersonAndPatientByMrnAndEp(any(), any())).thenReturn(new Bundle());

    final ParentBuilder builder = new ParentBuilder(fhirClient, mapper, parent);
    final ParentBuilder.Result result = builder.build();

    assertNotNull(result.getPatientResult().getPerson());
    assertNotNull(result.getPatientResult().getPatient());
    assertTrue(result.getPatientResult().isPersonNew());
    assertTrue(result.getPatientResult().isPatientNew());
    assertNotNull(result.getPatient());
  }

  @Test
  void build_ignored() {
    final ParentBuilder builder = new ParentBuilder(fhirClient, mapper, null);
    final ParentBuilder.Result result = builder.build();
    assertNull(result.getPatient());
  }

}