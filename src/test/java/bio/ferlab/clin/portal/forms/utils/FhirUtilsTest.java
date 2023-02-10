package bio.ferlab.clin.portal.forms.utils;

import bio.ferlab.clin.portal.forms.models.builders.ObservationsBuilder;
import bio.ferlab.clin.portal.forms.models.submit.Parent;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FhirUtilsTest {
  
  @Test
  void formatResource() {
    final Resource patient = new Patient().setId("foo");
    assertEquals("Patient/foo", FhirUtils.formatResource(patient));
  }

  @Test
  void isDoctor() {
    var role = new PractitionerRole();
    role.getCodeFirstRep().getCodingFirstRep().setCode("doctor");
    role.getOrganization().setReference("Organization/ep1");
    assertTrue(FhirUtils.isDoctor(role, "ep1"));
    role.getCodeFirstRep().getCodingFirstRep().setCode("foo");
    assertFalse(FhirUtils.isDoctor(role, "ep2"));
    assertFalse(FhirUtils.isDoctor(null, null));
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

  @Test
  void toAffected_status() {
    assertNull(FhirUtils.toAffected((Parent.Status) null));
    assertEquals("POS", FhirUtils.toAffected(Parent.Status.affected).name());
    assertEquals("NEG", FhirUtils.toAffected(Parent.Status.not_affected).name());
    assertEquals("IND", FhirUtils.toAffected(Parent.Status.unknown).name());
  }

  @Test
  void toAffected_boolean() {
    assertNull(FhirUtils.toAffected((Boolean) null));
    assertEquals(ObservationsBuilder.Affected.POS, FhirUtils.toAffected(Boolean.TRUE));
    assertEquals(ObservationsBuilder.Affected.NEG, FhirUtils.toAffected(Boolean.FALSE));
  }

}