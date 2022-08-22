package bio.ferlab.clin.portal.forms.utils;

import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
  
  @Test
  void extractId() {
    assertNull(FhirUtils.extractId((Reference) null));
    assertNull(FhirUtils.extractId(new Reference()));
    assertNull(FhirUtils.extractId(new Reference().setReference("foo")));
    assertEquals("id", FhirUtils.extractId(new Reference().setReference("TypeResource/id")));
  }

  @Test
  void extractId_string() {
    assertNull(FhirUtils.extractId((String) null));
    assertNull(FhirUtils.extractId(""));
    assertNull(FhirUtils.extractId("foo"));
    assertEquals("100307", FhirUtils.extractId("Patient/100307/_history/2"));
  }

}