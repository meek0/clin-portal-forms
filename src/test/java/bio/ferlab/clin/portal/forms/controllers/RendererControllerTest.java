package bio.ferlab.clin.portal.forms.controllers;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import ca.uhn.fhir.context.FhirContext;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ClinicalImpression;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
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

  @MockBean
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
  void foetus() throws IOException {
    final var mainBundle = new Bundle();
    final var analysis = new ServiceRequest();
    analysis.getMeta().addProfile(ANALYSIS_SERVICE_REQUEST);
    analysis.addCategory().setText(PRENATAL);
    mainBundle.addEntry(new Bundle.BundleEntryComponent().setResource(analysis));
    when(fhirClient.findServiceRequestWithDepsById(any())).thenReturn(mainBundle);
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      controller.render("1234", "html");
    });
    assertEquals(501, exception.getStatusCode().value());
    assertEquals("Prescription for foetus isn't implemented: 1234", exception.getReason());
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

  private void preparePostBirth() {
    var mainBundle = new Bundle();

    var analysis = new ServiceRequest();
    analysis.setId("analysisId");
    analysis.setSubject(new Reference("Patient/p1"));
    analysis.getMeta().addProfile(ANALYSIS_SERVICE_REQUEST);

    mainBundle.addEntry(new Bundle.BundleEntryComponent().setResource(analysis));

    final var detailsBundle = new Bundle();

    final var sequencing = new ServiceRequest();
    sequencing.setId("sequencingId");
    sequencing.getMeta().addProfile(SEQUENCING_SERVICE_REQUEST);
    sequencing.setSubject(new Reference("Patient/p1"));

    final var clinical = new ClinicalImpression();
    clinical.setSubject(new Reference("Patient/p1"));

    detailsBundle.addEntry(new Bundle.BundleEntryComponent().setResource(sequencing));
    detailsBundle.addEntry(new Bundle.BundleEntryComponent().setResource(clinical));

    final var codesAndValuesBundle = new Bundle();

    when(fhirClient.findServiceRequestWithDepsById(any())).thenReturn(mainBundle);
    when(fhirClient.fetchPrescriptionDetails(any(), any())).thenReturn(detailsBundle);
    when(fhirClient.fetchCodesAndValues()).thenReturn(codesAndValuesBundle);
  }

  private void assertContent(String snapshotPath, ResponseEntity<String> response) throws IOException {
    var expected = IOUtils.resourceToString(snapshotPath, StandardCharsets.UTF_8, getClass().getClassLoader());
    assertEquals(sanitize(expected), sanitize(response.getBody()));
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
