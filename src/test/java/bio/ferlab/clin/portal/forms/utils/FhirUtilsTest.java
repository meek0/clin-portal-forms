package bio.ferlab.clin.portal.forms.utils;

import bio.ferlab.clin.portal.forms.models.builders.ObservationsBuilder;
import bio.ferlab.clin.portal.forms.models.submit.Parent;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

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

  @Test
  void filterByTypes() {
    assertEquals(new ArrayList<>(), FhirUtils.filterByTypes(null, null));
    var refs = new ArrayList<Reference>();
    refs.add(new Reference("Organization/1"));
    refs.add(new Reference("PractitionerRole/1"));
    refs.add(new Reference("PractitionerRole/2"));
    refs.add(new Reference("Patient/1"));
    assertEquals(4, FhirUtils.filterByTypes(refs, null).size());
    assertEquals(2, FhirUtils.filterByTypes(refs, PractitionerRole.class).size());
    assertEquals("Organization/1", FhirUtils.filterByTypes(refs, PractitionerRole.class).get(0).getReference());
    assertEquals("Patient/1", FhirUtils.filterByTypes(refs, PractitionerRole.class).get(1).getReference());
  }

  @Test
  void findExtension() {
    var sr = new ServiceRequest();
    sr.addExtension("url1", new Reference("ref1"));
    sr.addExtension("url2", new Reference("ref2"));
    assertEquals("ref1", ((Reference) FhirUtils.findExtension(sr, "url1").get()).getReference());
    assertEquals("ref2", ((Reference) FhirUtils.findExtension(sr, "url2").get()).getReference());
    assertTrue(FhirUtils.findExtension(sr, "url3").isEmpty());
    assertTrue(FhirUtils.findExtension(sr, null).isEmpty());
    assertTrue(FhirUtils.findExtension((ServiceRequest) null, "url1").isEmpty());
    assertTrue(FhirUtils.findExtension(sr, "url0").isEmpty());
  }

  @Test
  void findIdentifier() {
    var p = new Practitioner();
    var id1 = new Identifier().setValue("value1").setType(new CodeableConcept().addCoding(new Coding().setCode("code1")));
    var id2 = new Identifier().setValue("value2").setType(new CodeableConcept().addCoding(new Coding().setCode("code2")));
    p.addIdentifier(id1);
    p.addIdentifier(id2);
    assertEquals("value1", FhirUtils.findIdentifier(p, "code1").get());
    assertEquals("value2", FhirUtils.findIdentifier(p, "code2").get());
    assertTrue(FhirUtils.findIdentifier(null, null).isEmpty());
    assertTrue(FhirUtils.findIdentifier(p, null).isEmpty());
    assertTrue(FhirUtils.findIdentifier(null, "code1").isEmpty());
    assertTrue(FhirUtils.findIdentifier(p, "code0").isEmpty());
  }

  @Test
  void findCode() {
    var sr = new ServiceRequest();
    sr.getCode().addCoding(new Coding().setSystem("system1").setCode("code1"));
    sr.getCode().addCoding(new Coding().setSystem("system2").setCode("code2"));
    assertEquals("code1", FhirUtils.findCode(sr, "system1").get());
    assertEquals("code2", FhirUtils.findCode(sr, "system2").get());
    assertTrue(FhirUtils.findCode(null, null).isEmpty());
    assertTrue(FhirUtils.findCode(sr, null).isEmpty());
    assertTrue(FhirUtils.findCode(null, "system1").isEmpty());
    assertTrue(FhirUtils.findCode(sr, "system0").isEmpty());
  }
}
