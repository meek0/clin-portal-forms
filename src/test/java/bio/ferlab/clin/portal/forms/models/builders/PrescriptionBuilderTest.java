package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.mappers.FhirToSearchMapper;
import bio.ferlab.clin.portal.forms.models.search.SearchPrescription;
import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PrescriptionBuilderTest {
  
  final FhirContext fhirContext = FhirContext.forR4();
  final FhirClient fhirClient = Mockito.mock(FhirClient.class);
  
  @BeforeEach
  void beforeEach() {
    when(fhirClient.getContext()).thenReturn(fhirContext);

    final PractitionerRole role1 = new PractitionerRole();
    role1.setOrganization(new Reference("Organization/org1"));

    final PractitionerRole role2 = new PractitionerRole();
    role2.setOrganization(new Reference("Organization/org2"));

    final Bundle bundleOrgs = new Bundle();
    bundleOrgs.addEntry().setResource(role1);
    bundleOrgs.addEntry().setResource(role2);

    when(fhirClient.findPractitionerRoleByPractitionerId(any())).thenReturn(bundleOrgs);
  }
  
  @Test
  void validate() {
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      new PrescriptionBuilder(null, null, null, null, null).validate();
    });
    assertEquals("ramq and id can't be both empty", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
  }
  
  @Test
  void build_no_matching_ep() {
    final Bundle bundle = new Bundle();
    bundle.addEntry().setResource(new PractitionerRole());
    when(fhirClient.findServiceRequestById(any())).thenReturn(bundle);
    final var result = new PrescriptionBuilder(fhirClient, new FhirToSearchMapper(), "practitionerId", "foo", "bar").build();
    assertEquals(0, result.getPrescriptions().size());
  }

  @Test
  void build_by_id() {
    final ServiceRequest analysis = new ServiceRequest();
    analysis.setId("id1");
    analysis.setAuthoredOn(new Date());
    analysis.getCode().addCoding().setCode("panel");
    final PractitionerRole role = new PractitionerRole();
    role.setOrganization(new Reference("Organization/org2"));
    final Patient patient = new Patient();
    patient.setId("patient");
    final Bundle serviceRequestBundle = new Bundle();
    serviceRequestBundle.addEntry().setResource(analysis);
    serviceRequestBundle.addEntry().setResource(role);
    serviceRequestBundle.addEntry().setResource(patient);
    when(fhirClient.findServiceRequestById(any())).thenReturn(serviceRequestBundle);
    final Practitioner practitioner = new Practitioner();
    when(fhirClient.findPractitionerById(any())).thenReturn(practitioner);
    final Person person = new Person();
    person.addName().setFamily("name");
    person.addIdentifier().setValue("ramq");
    final Bundle personBundle = new Bundle();
    personBundle.addEntry().setResource(person);
    when(fhirClient.findPersonByPatientId(any())).thenReturn(personBundle);
    final RelatedPerson relatedPerson = new RelatedPerson();
    relatedPerson.addIdentifier().setValue("ramq2");
    final Bundle relatedBundle = new Bundle();
    relatedBundle.addEntry().setResource(relatedPerson);
    when(fhirClient.findRelatedPersonByPatientId(any())).thenReturn(relatedBundle);
    
    final var result = new PrescriptionBuilder(fhirClient, new FhirToSearchMapper(), "practitionerId", "foo", null).build();

    assertEquals(1, result.getPrescriptions().size());
    SearchPrescription sp = result.getPrescriptions().get(0);
    assertEquals(analysis.getIdElement().getIdPart(), sp.getAnalysisId());
    assertNotNull(sp.getDate());
    assertEquals("patient", sp.getPatientId());
    assertEquals("panel", sp.getPanelCode());
    assertEquals("name", sp.getPatientName());
    assertEquals("ramq", sp.getPatientRamq());
    assertEquals("ramq2", sp.getMotherRamq());
    
  }

}