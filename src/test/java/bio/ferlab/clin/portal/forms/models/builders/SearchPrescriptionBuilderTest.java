package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.mappers.FhirToSearchMapper;
import bio.ferlab.clin.portal.forms.models.search.SearchPrescription;
import bio.ferlab.clin.portal.forms.utils.FhirConst;
import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchPrescriptionBuilderTest {
  
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
      new SearchPrescriptionBuilder(null, null, null, null, null).validate();
    });
    assertEquals("ramq and id can't be both empty", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
  }
  
  @Test
  void build_no_matching_ep() {
    final Bundle bundle = new Bundle();
    bundle.addEntry().setResource(new PractitionerRole());
    when(fhirClient.findServiceRequestById(any())).thenReturn(bundle);
    final var result = new SearchPrescriptionBuilder(fhirClient, new FhirToSearchMapper(), "practitionerId", "foo", "bar").build();
    assertEquals(0, result.getPrescriptions().size());
  }
  
  @Test
  void build() {  // test both by id and by ramq
    final Patient p1 = new Patient();
    p1.setId("p1");
    p1.setManagingOrganization(new Reference("Organization/org1"));
    final Patient p2 = new Patient();
    p2.setId("p2");
    p2.setManagingOrganization(new Reference("Organization/org1"));
    final Patient p3 = new Patient();
    p3.setId("p3");
    p3.setManagingOrganization(new Reference("Organization/org2"));
    final Patient p4 = new Patient();
    p4.setId("p4");
    p4.setManagingOrganization(new Reference("Organization/org3"));
    final Bundle patientBundle = new Bundle();
    patientBundle.addEntry().setResource(p1);
    patientBundle.addEntry().setResource(p2);
    patientBundle.addEntry().setResource(p3);
    when(fhirClient.findPersonAndPatientByRamq(any())).thenReturn(patientBundle);
    
    final ServiceRequest s1 = new ServiceRequest();
    s1.setId("s1");s1.getMeta().addProfile(FhirConst.ANALYSIS_SERVICE_REQUEST);
    final ServiceRequest s1_1 = new ServiceRequest();
    s1_1.setId("s1_1");s1_1.getMeta().addProfile(FhirConst.SEQUENCING_SERVICE_REQUEST);
    final ServiceRequest s2 = new ServiceRequest();
    s2.setId("s2");s2.getMeta().addProfile(FhirConst.ANALYSIS_SERVICE_REQUEST);
    final ServiceRequest s3 = new ServiceRequest();
    s3.setId("s3");s3.getMeta().addProfile(FhirConst.ANALYSIS_SERVICE_REQUEST);
    final ServiceRequest s4 = new ServiceRequest();
    s4.setId("s4");s4.getMeta().addProfile(FhirConst.ANALYSIS_SERVICE_REQUEST);
    final ServiceRequest s5 = new ServiceRequest();
    s5.setId("s5");s5.getMeta().addProfile(FhirConst.ANALYSIS_SERVICE_REQUEST);
    final Bundle srbundle = new Bundle();
    final Bundle srOrg1Bundle = new Bundle();
    srOrg1Bundle.addEntry().setResource(s1);
    srOrg1Bundle.addEntry().setResource(s1_1);
    srOrg1Bundle.addEntry().setResource(s2);
    srbundle.addEntry().setResource(srOrg1Bundle);
    final Bundle srOrg2Bundle = new Bundle();
    srOrg2Bundle.addEntry().setResource(s3);
    srbundle.addEntry().setResource(srOrg2Bundle);
    srbundle.addEntry().setResource(s4);
    srbundle.addEntry().setResource(s5);
    
    when(fhirClient.fetchServiceRequestsByPatientIds(any())).thenReturn(srbundle);
    
    this.prepareResponse(p1, s1); // org1
    this.prepareResponse(p1, s1_1); // org1 ignored <- sequencing service request
    this.prepareResponse(p1, s2); // org1
    this.prepareResponse(p2, s3); // org1
    this.prepareResponse(p3, s4); // org2
    this.prepareResponse(p4, s5); // org3 ignored <- practitioner doesn't have that ep
    
    final var results = new SearchPrescriptionBuilder(fhirClient, new FhirToSearchMapper(), "practitionerId", null, "ramq").build().getPrescriptions();
    
    assertEquals(4, results.size());
    validateResult(p1, s1, results.get(0));
    validateResult(p1, s2, results.get(1));
    validateResult(p2, s3, results.get(2));
    validateResult(p3, s4, results.get(3));
    
    verify(fhirClient).findPersonAndPatientByRamq(eq("ramq"));
    verify(fhirClient).fetchServiceRequestsByPatientIds(eq(List.of("p1", "p2", "p3")));
    verify(fhirClient).findServiceRequestById(eq("s1"));
    verify(fhirClient).findServiceRequestById(eq("s2"));
    verify(fhirClient).findServiceRequestById(eq("s3"));
    verify(fhirClient).findServiceRequestById(eq("s4"));
    verify(fhirClient).findServiceRequestById(eq("s5"));
  }
  
  private void validateResult(Patient p, ServiceRequest sr, SearchPrescription sp) {
    assertEquals(sr.getIdElement().getIdPart(), sp.getId());
    assertNotNull(sp.getDate());
    assertNotNull(sp.getPrescriberId());
    assertEquals("dr given family", sp.getPrescriberName());
    assertEquals(p.getIdElement().getIdPart(), sp.getPatientId());
    assertEquals("panel", sp.getPanelCode());
    assertEquals("given family", sp.getPatientName());
    assertEquals("ramq", sp.getPatientRamq());
    assertEquals("mother_ramq", sp.getMotherRamq());
  }
  
  private void prepareResponse(Patient p, ServiceRequest sr) {
    sr.setAuthoredOn(new Date());
    sr.getCode().addCoding().setCode("panel");
    
    final PractitionerRole role = new PractitionerRole();
    role.setId(UUID.randomUUID().toString());
    role.setOrganization(p.getManagingOrganization());
    
    final Bundle serviceRequestBundle = new Bundle();
    serviceRequestBundle.addEntry().setResource(sr);
    serviceRequestBundle.addEntry().setResource(role);
    serviceRequestBundle.addEntry().setResource(p);
    
    // conditional based on service request id
    when(fhirClient.findServiceRequestById(eq(sr.getIdElement().getIdPart()))).thenReturn(serviceRequestBundle);
    
    final Practitioner practitioner = new Practitioner();
    practitioner.addName().setFamily("family").addGiven("given").addPrefix("dr");
    
    final Person person = new Person();
    person.addName().setFamily("family").addGiven("given");
    person.addIdentifier().setValue("ramq");
    
    final RelatedPerson relatedPerson = new RelatedPerson();
    relatedPerson.addIdentifier().setValue("mother_ramq");
    
    final Bundle allBundle = new Bundle();
    allBundle.addEntry().setResource(practitioner);
    allBundle.addEntry().setResource(person);
    allBundle.addEntry().setResource(relatedPerson);
    
    // no conditional we return always the same it's ok
    when(fhirClient.fetchAdditionalPrescriptionData(any(), any())).thenReturn(allBundle);
  }

}