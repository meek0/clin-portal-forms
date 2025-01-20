package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.services.PrescriptionService;
import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static bio.ferlab.clin.portal.forms.utils.FhirConst.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ShareBuilderTest {

  final FhirContext fhirContext = FhirContext.forR4();
  final FhirClient fhirClient = Mockito.mock(FhirClient.class);
  final PrescriptionService prescriptionService = new PrescriptionService(fhirClient);

  @BeforeEach
  void beforeEach() {
    Mockito.reset(fhirClient);
    when(fhirClient.getContext()).thenReturn(fhirContext);
  }

  @Test
  void prescription_not_found() {
    when(fhirClient.findServiceRequestWithDepsById(any())).thenReturn(new Bundle());
    var builder = new ShareBuilder(prescriptionService, fhirClient, "analysis_id", List.of(), "PR01");
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, builder::build);
    assertEquals("Prescription not found: analysis_id", exception.getReason());
    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
  }

  private ServiceRequest buildAnalysis() {
    var analysis = new ServiceRequest();
    analysis.getMeta().addProfile(ANALYSIS_SERVICE_REQUEST);
    analysis.getMeta().addSecurity().setCode("EP1");
    analysis.setSubject(new Reference("Patient/PAT01"));
    return analysis;
  }

  private Patient buildPatient1() {
    var p = new Patient();
    p.setId("PAT01");
    p.setManagingOrganization(new Reference("Organization/ORG01"));
    return p;
  }

  private Person buildPerson1() {
    var p = new Person();
    p.setId("PER01");
    p.addLink().setTarget(new Reference("Patient/PAT01"));
    return p;
  }

  private PractitionerRole buildPractitionerRole1() {
    var pr = new PractitionerRole();
    pr.setId("PRR01");
    pr.getPractitioner().setReference("Practitioner/PR01");
    pr.setOrganization(new Reference("Organization/ORG01"));
    pr.getCodeFirstRep().getCodingFirstRep().setSystem(PRACTITIONER_ROLE_GENETICIAN_SYSTEM).setCode(RESIDENT_PHYSICIAN_PREFIX);
    return pr;
  }

  private PractitionerRole buildPractitionerRole2() {
    var pr = new PractitionerRole();
    pr.setId("PRR02");
    pr.getPractitioner().setReference("Practitioner/PR02");
    pr.setOrganization(new Reference("Organization/ORG02"));
    pr.getCodeFirstRep().getCodingFirstRep().setSystem(PRACTITIONER_ROLE_GENETICIAN_SYSTEM).setCode(DOCTOR_PREFIX);
    return pr;
  }

  private Organization buildOrganization1() {
    var o = new Organization();
    o.setId("ORG01");
    return o;
  }

  @Test
  void practitioner_no_roles() {
    var analysis = buildAnalysis();
    var patient1 = buildPatient1();
    var bundleAnalysis = new Bundle();
    bundleAnalysis.addEntry().setResource(analysis);
    bundleAnalysis.addEntry().setResource(patient1);
    when(fhirClient.findServiceRequestWithDepsById(any())).thenReturn(bundleAnalysis);

    var person1 = buildPerson1();
    var bundleDetails = new Bundle();
    bundleDetails.addEntry().setResource(patient1);
    bundleDetails.addEntry().setResource(person1);
    when(fhirClient.fetchPrescriptionDetails(any(), any(), any(), any())).thenReturn(bundleDetails);

    var bundleRoles = new Bundle();
    when(fhirClient.findAllPractitionerRoles()).thenReturn(bundleRoles);

    var builder = new ShareBuilder(prescriptionService, fhirClient, "analysis_id", List.of(), "PR01");
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, builder::build);
    assertEquals("practitioner: PR01 has no roles", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
  }

  @Test
  void unknown_share_role() {
    var patient1 = buildPatient1();
    var analysis = buildAnalysis();
    var bundleAnalysis = new Bundle();
    bundleAnalysis.addEntry().setResource(analysis);
    bundleAnalysis.addEntry().setResource(patient1);
    when(fhirClient.findServiceRequestWithDepsById(any())).thenReturn(bundleAnalysis);

    var bundleDetails = new Bundle();
    var person1 = buildPerson1();
    bundleDetails.addEntry().setResource(patient1);
    bundleDetails.addEntry().setResource(person1);
    when(fhirClient.fetchPrescriptionDetails(any(), any(), any(), any())).thenReturn(bundleDetails);

    var pr1 = buildPractitionerRole1();
    var bundleRoles = new Bundle();
    bundleRoles.addEntry().setResource(pr1);
    when(fhirClient.findAllPractitionerRoles()).thenReturn(bundleRoles);

    var builder = new ShareBuilder(prescriptionService, fhirClient, "analysis_id", List.of("PRR02"), "PR01");
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, builder::build);
    assertEquals("practitioner role: PRR02 is unknown", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
  }

  @Test
  void share_role_another_ep() {
    var patient1 = buildPatient1();
    var analysis = buildAnalysis();
    var bundleAnalysis = new Bundle();
    bundleAnalysis.addEntry().setResource(analysis);
    bundleAnalysis.addEntry().setResource(patient1);
    when(fhirClient.findServiceRequestWithDepsById(any())).thenReturn(bundleAnalysis);

    var bundleDetails = new Bundle();
    var person1 = buildPerson1();
    var org1 = buildOrganization1();
    bundleDetails.addEntry().setResource(patient1);
    bundleDetails.addEntry().setResource(person1);
    bundleDetails.addEntry().setResource(org1);
    when(fhirClient.fetchPrescriptionDetails(any(), any(), any(), any())).thenReturn(bundleDetails);

    var pr1 = buildPractitionerRole1();
    var pr2 = buildPractitionerRole2();
    var bundleRoles = new Bundle();
    bundleRoles.addEntry().setResource(pr1);
    bundleRoles.addEntry().setResource(pr2);
    when(fhirClient.findAllPractitionerRoles()).thenReturn(bundleRoles);

    var builder = new ShareBuilder(prescriptionService, fhirClient, "analysis_id", List.of("PRR02"), "PR01");
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, builder::build);
    assertEquals("practitioner role: PRR02 belongs to another organization: ORG02 than the prescription: ORG01", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
  }

  @Test
  void not_a_doctor() {
    var patient1 = buildPatient1();
    var analysis = buildAnalysis();
    var bundleAnalysis = new Bundle();
    bundleAnalysis.addEntry().setResource(analysis);
    bundleAnalysis.addEntry().setResource(patient1);
    when(fhirClient.findServiceRequestWithDepsById(any())).thenReturn(bundleAnalysis);

    var bundleDetails = new Bundle();
    var person1 = buildPerson1();
    var org1 = buildOrganization1();
    bundleDetails.addEntry().setResource(patient1);
    bundleDetails.addEntry().setResource(person1);
    bundleDetails.addEntry().setResource(org1);
    when(fhirClient.fetchPrescriptionDetails(any(), any(), any(), any())).thenReturn(bundleDetails);

    var pr1 = buildPractitionerRole1();
    pr1.getCode().clear();  // removed doctor role
    var pr2 = buildPractitionerRole2();
    var bundleRoles = new Bundle();
    bundleRoles.addEntry().setResource(pr1);
    bundleRoles.addEntry().setResource(pr2);
    when(fhirClient.findAllPractitionerRoles()).thenReturn(bundleRoles);

    var builder = new ShareBuilder(prescriptionService, fhirClient, "analysis_id", List.of("PRR01"), "PR01");
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, builder::build);
    assertEquals("practitioner role: PRR01 isn't a doctor|genetic counselor|resident physician at organization: ORG01", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
  }

  @Test
  void share_with_themself() {
    var patient1 = buildPatient1();
    var analysis = buildAnalysis();
    var bundleAnalysis = new Bundle();
    bundleAnalysis.addEntry().setResource(analysis);
    bundleAnalysis.addEntry().setResource(patient1);
    when(fhirClient.findServiceRequestWithDepsById(any())).thenReturn(bundleAnalysis);

    var bundleDetails = new Bundle();
    var person1 = buildPerson1();
    var org1 = buildOrganization1();
    bundleDetails.addEntry().setResource(patient1);
    bundleDetails.addEntry().setResource(person1);
    bundleDetails.addEntry().setResource(org1);
    when(fhirClient.fetchPrescriptionDetails(any(), any(), any(), any())).thenReturn(bundleDetails);

    var pr1 = buildPractitionerRole1();
    var pr2 = buildPractitionerRole2();
    var bundleRoles = new Bundle();
    bundleRoles.addEntry().setResource(pr1);
    bundleRoles.addEntry().setResource(pr2);
    when(fhirClient.findAllPractitionerRoles()).thenReturn(bundleRoles);

    var builder = new ShareBuilder(prescriptionService, fhirClient, "analysis_id", List.of("PRR01"), "PR01");
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, builder::build);
    assertEquals("practitioner: PR01 can't share with themself: PRR01", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
  }

  @Test
  void keep_or_replace_previous_shares() {
    var patient1 = buildPatient1();
    patient1.getMeta().addSecurity().setCode("PractitionerRole/PRR01");
    var analysis = buildAnalysis();
    var bundleAnalysis = new Bundle();
    bundleAnalysis.addEntry().setResource(analysis);
    bundleAnalysis.addEntry().setResource(patient1);
    when(fhirClient.findServiceRequestWithDepsById(any())).thenReturn(bundleAnalysis);

    var bundleDetails = new Bundle();
    var person1 = buildPerson1();
    var org1 = buildOrganization1();
    bundleDetails.addEntry().setResource(patient1);
    bundleDetails.addEntry().setResource(person1);
    bundleDetails.addEntry().setResource(org1);

    var observation1 = new Observation();
    observation1.setId("OBS01");
    observation1.getMeta().addSecurity().setCode("PractitionerRole/PRR01"); // will be removed
    var clinicalImpression1 = new ClinicalImpression();
    clinicalImpression1.setId("CLI01");
    clinicalImpression1.getMeta().addSecurity().setCode("PractitionerRole/PRR01"); // will be removed
    bundleDetails.addEntry().setResource(observation1);
    bundleDetails.addEntry().setResource(clinicalImpression1);

    when(fhirClient.fetchPrescriptionDetails(any(), any(), any(), any())).thenReturn(bundleDetails);

    var pr1 = buildPractitionerRole1();
    var pr2 = buildPractitionerRole2();
    pr2.setOrganization(new Reference("Organization/ORG01"));
    var bundleRoles = new Bundle();
    bundleRoles.addEntry().setResource(pr1);
    bundleRoles.addEntry().setResource(pr2);
    when(fhirClient.findAllPractitionerRoles()).thenReturn(bundleRoles);

    var builder = new ShareBuilder(prescriptionService, fhirClient, "analysis_id", List.of("PRR02"), "PR01");
    builder.build();

    assertEquals("[EP1, PractitionerRole/PRR02]", analysis.getMeta().getSecurity().stream().map(Coding::getCode).toList().toString());
    assertEquals("[PractitionerRole/PRR01, PractitionerRole/PRR02]", patient1.getMeta().getSecurity().stream().map(Coding::getCode).toList().toString());
    assertEquals("[PractitionerRole/PRR02]", observation1.getMeta().getSecurity().stream().map(Coding::getCode).toList().toString());
    assertEquals("[PractitionerRole/PRR02]", clinicalImpression1.getMeta().getSecurity().stream().map(Coding::getCode).toList().toString());
  }

  @Test
  void ignore_duplicates() {
    var patient1 = buildPatient1();
    patient1.getMeta().addSecurity().setCode("PractitionerRole/PRR02");
    var analysis = buildAnalysis();
    analysis.getMeta().addSecurity().setCode("PractitionerRole/PRR02");
    var bundleAnalysis = new Bundle();
    bundleAnalysis.addEntry().setResource(analysis);
    bundleAnalysis.addEntry().setResource(patient1);
    when(fhirClient.findServiceRequestWithDepsById(any())).thenReturn(bundleAnalysis);

    var bundleDetails = new Bundle();
    var person1 = buildPerson1();
    var org1 = buildOrganization1();
    bundleDetails.addEntry().setResource(patient1);
    bundleDetails.addEntry().setResource(person1);
    bundleDetails.addEntry().setResource(org1);
    when(fhirClient.fetchPrescriptionDetails(any(), any(), any(), any())).thenReturn(bundleDetails);

    var pr1 = buildPractitionerRole1();
    var pr2 = buildPractitionerRole2();
    pr2.setOrganization(new Reference("Organization/ORG01"));
    var bundleRoles = new Bundle();
    bundleRoles.addEntry().setResource(pr1);
    bundleRoles.addEntry().setResource(pr2);
    when(fhirClient.findAllPractitionerRoles()).thenReturn(bundleRoles);

    var builder = new ShareBuilder(prescriptionService, fhirClient, "analysis_id", List.of("PRR02", "PRR02"), "PR01");
    builder.build();

    assertEquals("[EP1, PractitionerRole/PRR02]", analysis.getMeta().getSecurity().stream().map(Coding::getCode).toList().toString());
    assertEquals("[PractitionerRole/PRR02]", patient1.getMeta().getSecurity().stream().map(Coding::getCode).toList().toString());
  }

}
