package bio.ferlab.clin.portal.forms.controllers;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import ca.uhn.fhir.context.FhirContext;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import static bio.ferlab.clin.portal.forms.utils.FhirConst.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class RendererControllerTest {

  private final FhirContext fhirContext = FhirContext.forR4();

  @MockitoBean
  private FhirClient fhirClient;

  @Autowired
  private RendererController controller;

  @BeforeEach
  void beforeEach() {
    when(fhirClient.getContext()).thenReturn(fhirContext);
  }

  @Test
  void not_analysis() throws IOException {
    final var mainBundle = new Bundle();
    final var analysis = new ServiceRequest();
    mainBundle.addEntry(new Bundle.BundleEntryComponent().setResource(analysis));
    when(fhirClient.findServiceRequestWithDepsById(any())).thenReturn(mainBundle);
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      controller.render("1234", "html");
    });
    assertEquals(400, exception.getStatusCode().value());
    assertEquals("Prescription isn't an analysis: 1234", exception.getReason());
  }

  @Test
  void preBirth_html() throws IOException {
    this.preparePreBirth();
    ResponseEntity<String> html = (ResponseEntity<String>) controller.render("5678", "html");
    assertContent("snapshots/prenatal.html", html);
  }

  @Test
  void preBirth_pdf() throws IOException {
    this.preparePreBirth();
    ResponseEntity<ByteArrayResource> pdf = (ResponseEntity<ByteArrayResource>) controller.render("5678", "pdf");
    assertTrue(pdf.getBody().contentLength() > 0);
    var contentDisposition = pdf.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
    assertTrue(contentDisposition.startsWith("attachment; filename=5678_"));
    assertTrue(contentDisposition.endsWith(".pdf"));
  }

  @Test
  void postBirth_html() throws IOException {
    this.preparePostBirth();
    ResponseEntity<String> html = (ResponseEntity<String>) controller.render("1234", "html");
    assertContent("snapshots/post-birth.html", html);
  }

  @Test
  void postBirth_pdf() throws IOException {
    this.preparePostBirth();
    ResponseEntity<ByteArrayResource> pdf = (ResponseEntity<ByteArrayResource>) controller.render("1234", "pdf");
    assertTrue(pdf.getBody().contentLength() > 0);
    var contentDisposition = pdf.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
    assertTrue(contentDisposition.startsWith("attachment; filename=1234_"));
    assertTrue(contentDisposition.endsWith(".pdf"));
  }

  @Test
  void trio_html() throws IOException {
    this.prepareTrio();
    ResponseEntity<String> html = (ResponseEntity<String>) controller.render("2468", "html");
    assertTrue(html.getBody().contains("sequencingId"));
    assertTrue(html.getBody().contains("sequencingMotherId"));
    assertTrue(html.getBody().contains("sequencingFatherId"));
  }

  @Test
  void trio_pdf() throws IOException {
    this.prepareTrio();
    ResponseEntity<ByteArrayResource> pdf = (ResponseEntity<ByteArrayResource>) controller.render("2468", "pdf");
    assertTrue(pdf.getBody().contentLength() > 0);
    var contentDisposition = pdf.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
    assertTrue(contentDisposition.startsWith("attachment; filename=2468_"));
    assertTrue(contentDisposition.endsWith(".pdf"));
  }

  private void preparePreBirth() {
    var mainBundle = new Bundle();

    var analysis = new ServiceRequest();
    analysis.setId("ServiceRequest/analysisId");
    analysis.setSubject(new Reference("Patient/p1"));
    analysis.getMeta().addProfile(ANALYSIS_SERVICE_REQUEST);
    analysis.addCategory().addCoding().setCode(PRENATAL);

    var proband = new Patient();
    proband.setId("p1");

    mainBundle.addEntry(new Bundle.BundleEntryComponent().setResource(analysis));
    mainBundle.addEntry(new Bundle.BundleEntryComponent().setResource(proband));

    final var detailsBundle = new Bundle();
    final var sequencingDetailsBundle = new Bundle();

    final var patient = new Patient();
    patient.setId("p1");
    final var person = new Person();
    person.getLinkFirstRep().getTarget().setReference("Patient/p1");

    final var fetusPatient = new Patient();
    fetusPatient.setId("p2");

    final var sequencing = new ServiceRequest();
    sequencing.setId("ServiceRequest/sequencingId");
    sequencing.getMeta().addProfile(SEQUENCING_SERVICE_REQUEST);
    sequencing.setSubject(new Reference("Patient/p2"));
    sequencing.addCategory().addCoding().setCode(PRENATAL);
    sequencing.getBasedOn().add(new Reference("ServiceRequest/analysisId"));

    final var clinical = new ClinicalImpression();
    clinical.setSubject(new Reference("Patient/p1"));

    var obsIndic = new Observation();
    obsIndic.setId("obsIndicId");
    obsIndic.setSubject(new Reference("Patient/p1"));
    obsIndic.getCode().getCodingFirstRep().setCode("INDIC");
    obsIndic.setValue(new StringType("indication"));
    clinical.getInvestigationFirstRep().addItem(FhirUtils.toReference(obsIndic));

    detailsBundle.addEntry(new Bundle.BundleEntryComponent().setResource(patient));
    detailsBundle.addEntry(new Bundle.BundleEntryComponent().setResource(person));
    detailsBundle.addEntry(new Bundle.BundleEntryComponent().setResource(sequencing));
    detailsBundle.addEntry(new Bundle.BundleEntryComponent().setResource(clinical));
    detailsBundle.addEntry(new Bundle.BundleEntryComponent().setResource(obsIndic));

    sequencingDetailsBundle.addEntry(new Bundle.BundleEntryComponent().setResource(fetusPatient));

    final var codesAndValuesBundle = new Bundle();

    when(fhirClient.findServiceRequestWithDepsById(any())).thenReturn(mainBundle);
    when(fhirClient.fetchPrescriptionDetails(any(), any(), any(), any())).thenReturn(detailsBundle);
    when(fhirClient.fetchCodesAndValues()).thenReturn(codesAndValuesBundle);

    when(fhirClient.fetchFetusSequencingDetails(any())).thenReturn(sequencingDetailsBundle);
  }

  private void preparePostBirth() {
    var mainBundle = new Bundle();

    var analysis = new ServiceRequest();
    analysis.setId("analysisId");
    analysis.setSubject(new Reference("Patient/p1"));
    analysis.getMeta().addProfile(ANALYSIS_SERVICE_REQUEST);
    analysis.addPerformer().setReference("Organization/LDM-0001");

    var proband = new Patient();
    proband.setId("p1");
    proband.setManagingOrganization(new Reference("Organization/EP-0001"));

    var performer = new Organization();
    performer.setId("LDM-0001");

    mainBundle.addEntry(new Bundle.BundleEntryComponent().setResource(analysis));
    mainBundle.addEntry(new Bundle.BundleEntryComponent().setResource(proband));
    mainBundle.addEntry(new Bundle.BundleEntryComponent().setResource(performer));

    final var detailsBundle = new Bundle();

    final var patient = new Patient();
    patient.setId("p1");
    final var person = new Person();
    person.getLinkFirstRep().getTarget().setReference("Patient/p1");

    final var sequencing = new ServiceRequest();
    sequencing.setId("sequencingId");
    sequencing.getMeta().addProfile(SEQUENCING_SERVICE_REQUEST);
    sequencing.setSubject(new Reference("Patient/p1"));

    final var clinical = new ClinicalImpression();
    clinical.setSubject(new Reference("Patient/p1"));

    var ep = new Organization();
    ep.setId("EP-0001");
    ep.setName("EP");

    detailsBundle.addEntry(new Bundle.BundleEntryComponent().setResource(patient));
    detailsBundle.addEntry(new Bundle.BundleEntryComponent().setResource(person));
    detailsBundle.addEntry(new Bundle.BundleEntryComponent().setResource(sequencing));
    detailsBundle.addEntry(new Bundle.BundleEntryComponent().setResource(clinical));
    detailsBundle.addEntry(new Bundle.BundleEntryComponent().setResource(ep));

    final var codesAndValuesBundle = new Bundle();

    when(fhirClient.findServiceRequestWithDepsById(any())).thenReturn(mainBundle);
    when(fhirClient.fetchPrescriptionDetails(any(), any(), any(), any())).thenReturn(detailsBundle);
    when(fhirClient.fetchCodesAndValues()).thenReturn(codesAndValuesBundle);
  }

  private void prepareTrio() {
    var mainBundle = new Bundle();

    var analysis = new ServiceRequest();
    analysis.setId("analysisId");
    analysis.setSubject(new Reference("Patient/p1"));
    analysis.getMeta().addProfile(ANALYSIS_SERVICE_REQUEST);

    var proband = new Patient();
    proband.setId("p1");

    mainBundle.addEntry(new Bundle.BundleEntryComponent().setResource(analysis));
    mainBundle.addEntry(new Bundle.BundleEntryComponent().setResource(proband));

    final var detailsBundle = new Bundle();

    final var patient = new Patient();
    patient.setId("p1");
    final var person = new Person();
    person.getLinkFirstRep().getTarget().setReference("Patient/p1");

    final var patientMother = new Patient();
    patientMother.setId("pm");
    final var personMother = new Person();
    personMother.getLinkFirstRep().getTarget().setReference("Patient/pm");

    final var patientFather = new Patient();
    patientFather.setId("pf");
    final var personFather = new Person();
    personFather.getLinkFirstRep().getTarget().setReference("Patient/pf");

    final var sequencing = new ServiceRequest();
    sequencing.setId("sequencingId");
    sequencing.getMeta().addProfile(SEQUENCING_SERVICE_REQUEST);
    sequencing.setSubject(new Reference("Patient/p1"));

    final var sequencingMother = new ServiceRequest();
    sequencingMother.setId("sequencingMotherId");
    sequencingMother.getMeta().addProfile(SEQUENCING_SERVICE_REQUEST);
    sequencingMother.setSubject(new Reference("Patient/pm"));
    sequencingMother.getBasedOn().add(new Reference(analysis));

    final var sequencingFather = new ServiceRequest();
    sequencingFather.setId("sequencingFatherId");
    sequencingFather.getMeta().addProfile(SEQUENCING_SERVICE_REQUEST);
    sequencingFather.setSubject(new Reference("Patient/pf"));
    sequencingFather.getBasedOn().add(new Reference(analysis));

    final var clinical = new ClinicalImpression();
    clinical.setSubject(new Reference("Patient/p1"));

    final var clinicalMother = new ClinicalImpression();
    clinicalMother.setSubject(new Reference("Patient/pm"));

    final var clinicalFather = new ClinicalImpression();
    clinicalFather.setSubject(new Reference("Patient/pf"));

    this.addParentToAnalysis(analysis, clinicalMother, FAMILY_MEMBER_MOTHER_CODE);
    this.addParentToAnalysis(analysis, clinicalFather, FAMILY_MEMBER_FATHER_CODE);

    detailsBundle.addEntry(new Bundle.BundleEntryComponent().setResource(patient));
    detailsBundle.addEntry(new Bundle.BundleEntryComponent().setResource(patientMother));
    detailsBundle.addEntry(new Bundle.BundleEntryComponent().setResource(patientFather));
    detailsBundle.addEntry(new Bundle.BundleEntryComponent().setResource(person));
    detailsBundle.addEntry(new Bundle.BundleEntryComponent().setResource(personMother));
    detailsBundle.addEntry(new Bundle.BundleEntryComponent().setResource(personFather));
    detailsBundle.addEntry(new Bundle.BundleEntryComponent().setResource(sequencing));
    detailsBundle.addEntry(new Bundle.BundleEntryComponent().setResource(sequencingMother));
    detailsBundle.addEntry(new Bundle.BundleEntryComponent().setResource(sequencingFather));
    detailsBundle.addEntry(new Bundle.BundleEntryComponent().setResource(clinical));
    detailsBundle.addEntry(new Bundle.BundleEntryComponent().setResource(clinicalMother));
    detailsBundle.addEntry(new Bundle.BundleEntryComponent().setResource(clinicalFather));

    final var codesAndValuesBundle = new Bundle();

    when(fhirClient.findServiceRequestWithDepsById(any())).thenReturn(mainBundle);
    when(fhirClient.fetchPrescriptionDetails(any(), any(), any(), any())).thenReturn(detailsBundle);
    when(fhirClient.fetchCodesAndValues()).thenReturn(codesAndValuesBundle);
  }

  private void assertContent(String snapshotPath, ResponseEntity<String> response) throws IOException {
    var expected = IOUtils.resourceToString(snapshotPath, StandardCharsets.UTF_8, getClass().getClassLoader());
    assertEquals(sanitize(expected), sanitize(response.getBody()));
  }

  private void addParentToAnalysis(ServiceRequest analysis, ClinicalImpression clinical, String code) {
    final Extension familyMemberExt = new Extension(FAMILY_MEMBER);
    final Extension parentExt = new Extension("parent");
    parentExt.setValue(clinical.getSubject());
    final Extension parentRelationExt = new Extension("parent-relationship");
    final CodeableConcept cc = new CodeableConcept();
    cc.getCodingFirstRep().setSystem(SYSTEM_ROLE).setCode(code);
    parentRelationExt.setValue(cc);
    familyMemberExt.addExtension(parentExt);
    familyMemberExt.addExtension(parentRelationExt);
    analysis.addExtension(familyMemberExt);
  }

  private String sanitize(String file) {
    return Arrays.stream(file
        .replaceAll("\t", " ")
        .replaceAll("\\d{4}-\\d{2}-\\d{2}", "0000-00-00") // dates
        .split("\n"))
      .map(String::trim)
      .collect(Collectors.joining("\n"));
  }

}
