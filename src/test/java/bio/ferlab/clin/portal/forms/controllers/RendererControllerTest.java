package bio.ferlab.clin.portal.forms.controllers;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import ca.uhn.fhir.context.FhirContext;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
  void postBirth() throws IOException {
    final var mainBundle = new Bundle();
    final var analysis = new ServiceRequest();
    mainBundle.addEntry(new Bundle.BundleEntryComponent().setResource(analysis));
    final var detailsBundle = new Bundle();
    final var codesAndValuesBundle = new Bundle();

    when(fhirClient.findServiceRequestWithDepsById(any())).thenReturn(mainBundle);
    when(fhirClient.fetchPrescriptionDetails(any(), any(), any())).thenReturn(detailsBundle);
    when(fhirClient.fetchCodesAndValues()).thenReturn(codesAndValuesBundle);

    ResponseEntity<String> html = (ResponseEntity<String>) controller.render("1234", "html");
    assertContent("snapshots/post-birth.html", html);
  }

  private void assertContent(String snapshotPath, ResponseEntity<String> response) throws IOException {
    var expected = IOUtils.resourceToString(snapshotPath, StandardCharsets.UTF_8, getClass().getClassLoader()).replaceAll(" ", "");
    assertEquals(sanitize(expected), sanitize(response.getBody()));
  }

  private String sanitize(String file) {
    return file.replaceAll("\t", "").replaceAll(" ", "");
  }

}