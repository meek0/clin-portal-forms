package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PractitionerBuilderTest {

  final FhirContext fhirContext = FhirContext.forR4();
  final FhirClient fhirClient = Mockito.mock(FhirClient.class);

  @BeforeEach
  void setup() {
    when(fhirClient.getContext()).thenReturn(fhirContext);
  }

  @Test
  void withSupervisor() {
    final PractitionerRole practitionerRole = new PractitionerRole();
    practitionerRole.setId("foo");
    final Bundle bundle = new Bundle();
    bundle.addEntry().setResource(new PractitionerRole().setOrganization(FhirUtils.toReference(new Organization().setId("ep1"))).setId("bar1"));
    bundle.addEntry().setResource(new PractitionerRole().setOrganization(FhirUtils.toReference(new Organization().setId("ep2"))).setId("bar2"));

    when(fhirClient.findPractitionerRoleById(any())).thenReturn(practitionerRole);
    when(fhirClient.findPractitionerRoleByPractitionerId(any())).thenReturn(bundle);
    
    final PractitionerBuilder builder = new PractitionerBuilder(fhirClient, "practitioner");
    final PractitionerBuilder.Result result = builder
        .withEp("ep2")
        .withSupervisor("supervisor")
        .build();

    assertEquals(2, result.getPractitionerRoles().size());
    assertEquals("bar2", result.getPractitionerRole().getId());
    assertEquals("foo", result.getSupervisorRole().getId());
  }

  @Test
  void role_not_found() {
    final Bundle bundle = new Bundle();
    bundle.addEntry().setResource(new PractitionerRole().setOrganization(FhirUtils.toReference(new Organization().setId("ep1"))).setId("bar1"));
    bundle.addEntry().setResource(new PractitionerRole().setOrganization(FhirUtils.toReference(new Organization().setId("ep2"))).setId("bar2"));
    
    when(fhirClient.findPractitionerRoleByPractitionerId(any())).thenReturn(bundle);

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      new PractitionerBuilder(fhirClient, "p").withEp("ep3").build();
    });
    assertEquals("practitioner p has no role in ep ep3", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
  }

  @Test
  void no_roles() {
    final Bundle bundle = new Bundle();

    when(fhirClient.findPractitionerRoleByPractitionerId(any())).thenReturn(bundle);

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      new PractitionerBuilder(fhirClient, "p").build();
    });
    assertEquals("practitioner p has no roles", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
  }

  @Test
  void withSupervisor_not_found() {
    when(fhirClient.findPractitionerRoleById(any())).thenThrow(new ResourceNotFoundException("role not found"));
    
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      new PractitionerBuilder(fhirClient, null).withSupervisor("sup");
    });
    assertEquals("supervisor sup is unknown", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
  }

}