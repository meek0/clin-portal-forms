package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static bio.ferlab.clin.portal.forms.utils.FhirConst.ANALYSIS_SERVICE_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AssignmentBuilderTest {

  final FhirContext fhirContext = FhirContext.forR4();
  final FhirClient fhirClient = Mockito.mock(FhirClient.class);

  @BeforeEach
  void beforeEach() {
    when(fhirClient.getContext()).thenReturn(fhirContext);
  }

  @Test
  void build_service_request_not_found() {
    when(fhirClient.findServiceRequestById(any())).thenThrow(new ResourceNotFoundException("foo"));
    final var builder = new AssignmentBuilder(fhirClient, "foo", null);
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, builder::build);
    assertEquals("service request foo is unknown", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
  }

  @Test
  void build_service_request_not_analysis() {
    when(fhirClient.findServiceRequestById(any())).thenReturn(new ServiceRequest());
    final var builder = new AssignmentBuilder(fhirClient, "foo", null);
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, builder::build);
    assertEquals("service request foo isn't an analysis", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
  }

  @Test
  void build_role_not_found() {
    final var analysis = new ServiceRequest();
    analysis.getMeta().addProfile(ANALYSIS_SERVICE_REQUEST);
    when(fhirClient.findServiceRequestById(any())).thenReturn(analysis);
    final var bundle = new Bundle();
    bundle.addEntry().setResource(new PractitionerRole().setId("1"));
    bundle.addEntry().setResource(new PractitionerRole().setId("2"));
    when(fhirClient.findAllPractitionerRoles()).thenReturn(bundle);
    final var builder = new AssignmentBuilder(fhirClient, "foo", List.of("1", "3"));
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, builder::build);
    assertEquals("practitioner role 3 is unknown", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
  }

  @Test
  void build() {
    final var analysis = new ServiceRequest();
    analysis.setId("foo");
    analysis.getMeta().addProfile(ANALYSIS_SERVICE_REQUEST);
    analysis.getPerformer().add(new Reference("Organization/1"));
    when(fhirClient.findServiceRequestById(any())).thenReturn(analysis);
    final var bundle = new Bundle();
    bundle.addEntry().setResource(new PractitionerRole().setId("1"));
    bundle.addEntry().setResource(new PractitionerRole().setId("2"));
    when(fhirClient.findAllPractitionerRoles()).thenReturn(bundle);
    final var builder = new AssignmentBuilder(fhirClient, "foo", List.of("1", "2"));
    when(fhirClient.assignPerformers(any())).thenReturn(analysis);
    final var res = builder.build();
    assertEquals("foo", res.getAnalysis().getIdElement().getIdPart());
    assertEquals(3, res.getAnalysis().getPerformer().size());
    assertEquals("Organization/1", res.getAnalysis().getPerformer().get(0).getReference());
    assertEquals("PractitionerRole/1", res.getAnalysis().getPerformer().get(1).getReference());
    assertEquals("PractitionerRole/2", res.getAnalysis().getPerformer().get(2).getReference());
  }

}