package bio.ferlab.clin.portal.forms.utils;

import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FhirUtilsTest {
  
  @Test
  void formatResource() {
    final Resource patient = new Patient().setId("foo");
    assertEquals("Patient/foo", FhirUtils.formatResource(patient));
  }

  @Test
  void toReference() {
    final Resource patient = new Patient().setId("foo");
    final String expected = new Reference("Patient/foo").getReference();
    assertEquals(expected, FhirUtils.toReference(patient).getReference());
  }

}