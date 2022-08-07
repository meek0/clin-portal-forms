package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AutocompleteBuilderTest {
  
  final FhirContext fhirContext = FhirContext.forR4();
  final FhirClient fhirClient = Mockito.mock(FhirClient.class);

  @BeforeEach
  void beforeEach() {
    when(fhirClient.getContext()).thenReturn(fhirContext);
  }
  
  @Test
  void supervisor_should_find_one_match() {
    
    when(fhirClient.findPractitionerAndRoleByEp(any())).thenReturn(prepareTestBundle());
    
    AutocompleteBuilder.Result result = new AutocompleteBuilder(fhirClient, null)
        .withSupervisor("p1")
        .build();

    assertEquals(1, result.getSupervisors().size());
    assertEquals("r1", result.getSupervisors().get(0).getId());
  }

  @Test
  void supervisor_should_find_more_match() {

    when(fhirClient.findPractitionerAndRoleByEp(any())).thenReturn(prepareTestBundle());

    AutocompleteBuilder.Result result = new AutocompleteBuilder(fhirClient, null)
        .withSupervisor("p")
        .build();

    assertEquals(3, result.getSupervisors().size());
    assertEquals("r1", result.getSupervisors().get(0).getId());
    assertEquals("r2", result.getSupervisors().get(1).getId());
    assertEquals("r3", result.getSupervisors().get(2).getId());
  }

  @Test
  void supervisor_should_find_nothing() {

    when(fhirClient.findPractitionerAndRoleByEp(any())).thenReturn(prepareTestBundle());

    AutocompleteBuilder.Result result = new AutocompleteBuilder(fhirClient, null)
        .withSupervisor("x")
        .build();

    assertEquals(0, result.getSupervisors().size());
  }

  @Test
  void supervisor_should_find_by_name_ignore_case() {

    when(fhirClient.findPractitionerAndRoleByEp(any())).thenReturn(prepareTestBundle());

    AutocompleteBuilder.Result result = new AutocompleteBuilder(fhirClient, null)
        .withSupervisor("NA")
        .build();

    assertEquals(1, result.getSupervisors().size());
    assertEquals("r1", result.getSupervisors().get(0).getId());
    assertEquals("name", result.getSupervisors().get(0).getName());
  }
  
  private Bundle prepareTestBundle() {
    final Practitioner p1 = new Practitioner();
    p1.setId("p1");
    p1.getNameFirstRep().setFamily("name");

    final Practitioner p2 = new Practitioner();
    p2.setId("p2");

    final Practitioner p3 = new Practitioner();
    p3.setId("p3");

    final PractitionerRole r1 = new PractitionerRole();
    r1.setId("r1");
    r1.setPractitioner(FhirUtils.toReference(p1));

    final PractitionerRole r2 = new PractitionerRole();
    r2.setId("r2");
    r2.setPractitioner(FhirUtils.toReference(p2));

    final PractitionerRole r3 = new PractitionerRole();
    r3.setId("r3");
    r3.setPractitioner(FhirUtils.toReference(p3));

    final Bundle bundle = new Bundle();
    bundle.addEntry().setResource(p1);
    bundle.addEntry().setResource(p2);
    bundle.addEntry().setResource(p3);
    bundle.addEntry().setResource(r1);
    bundle.addEntry().setResource(r2);
    bundle.addEntry().setResource(r3);
    
    return bundle;
  }
  
}