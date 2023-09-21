package bio.ferlab.clin.portal.forms.mappers;

import bio.ferlab.clin.portal.forms.services.LogOnceService;
import bio.ferlab.clin.portal.forms.services.MessagesService;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import static bio.ferlab.clin.portal.forms.models.builders.ReflexBuilder.REFLEX_PANEL_PREFIX_EN;
import static bio.ferlab.clin.portal.forms.models.builders.ReflexBuilder.REFLEX_PANEL_PREFIX_FR;
import static bio.ferlab.clin.portal.forms.utils.FhirConst.ANALYSIS_REQUEST_CODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class TemplateMapperTest {

  final MessagesService messagesService = Mockito.mock(MessagesService.class);
  final LogOnceService logOnceService = Mockito.mock(LogOnceService.class);
  final CodeSystem codeSystem = new CodeSystem();
  final TemplateMapper mapper = new TemplateMapper("id", logOnceService, messagesService, codeSystem, Locale.FRENCH);

  @Test
  void mapToName() {
    assertEquals("-", mapper.mapToName(null));
    Person person = new Person();
    assertEquals("-", mapper.mapToName(person));
    person.getNameFirstRep().setFamily("Doe").setGiven(List.of(new StringType("John")));
    assertEquals("DOE John", mapper.mapToName(person));
  }

  @Test
  void mapToRAMQ() {
    assertEquals("-", mapper.mapToRAMQ(null));
    Person person = new Person();
    assertEquals("-", mapper.mapToRAMQ(person));
    person.addIdentifier(new Identifier().setValue("abcd12345678").setType(new CodeableConcept().addCoding(new Coding().setCode("JHN"))));
    assertEquals("ABCD 1234 5678", mapper.mapToRAMQ(person));
  }

  @Test
  void mapToMRN() {
    assertEquals("-", mapper.mapToMRN(null));
    Patient patient = new Patient();
    assertEquals("-", mapper.mapToMRN(patient));
    patient.addIdentifier(new Identifier().setValue("mrn-foo-1234").setType(new CodeableConcept().addCoding(new Coding().setCode("MR"))));
    assertEquals("FOO-1234 | -", mapper.mapToMRN(patient));
    patient.setManagingOrganization(new Reference("Organization/bar"));
    assertEquals("FOO-1234 | BAR", mapper.mapToMRN(patient));
  }

  @Test
  void formatDate() {
    assertEquals("-", mapper.formatDate(null));
    assertNotNull(mapper.formatDate(new Date()));
  }

  @Test
  void mapToAuthor() {
    assertEquals("-", mapper.mapToAuthor(null));
    Practitioner practitioner = new Practitioner();
    assertEquals("-", mapper.mapToAuthor(practitioner));
    practitioner.getNameFirstRep().setFamily("Doe").setGiven(List.of(new StringType("John")));
    assertEquals("DOE John", mapper.mapToAuthor(practitioner));
    practitioner.getNameFirstRep().addPrefix("Dr");
    assertEquals("Dr. DOE John", mapper.mapToAuthor(practitioner));
  }

  @Test
  void mapToContact_organization() {
    assertEquals("-", mapper.mapToContact(null, null));
    Organization organization = new Organization();
    assertEquals("-", mapper.mapToContact(organization, null));
    organization.getContactFirstRep().addTelecom(new ContactPoint().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("123456789"));
    organization.getContactFirstRep().addTelecom(new ContactPoint().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue("foo@bar"));
    assertEquals("123456789", mapper.mapToContact(organization, "phone"));
    assertEquals("foo@bar", mapper.mapToContact(organization, "email"));
  }

  @Test
  void mapToContact_prescriber() {
    assertEquals("-", mapper.mapToContact(null,null,null));
    PractitionerRole prescriber = new PractitionerRole();
    assertEquals("-", mapper.mapToContact(prescriber, null, null));
    prescriber.addTelecom(new ContactPoint().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue("prescriber@mail"));
    assertEquals("prescriber@mail", mapper.mapToContact(prescriber,null, "email"));
    PractitionerRole supervisor = new PractitionerRole();
    assertEquals("prescriber@mail", mapper.mapToContact(prescriber,supervisor, "email"));
    supervisor.addTelecom(new ContactPoint().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue("supervisor@mail"));
    assertEquals("supervisor@mail", mapper.mapToContact(prescriber,supervisor, "email"));
  }

  @Test
  void mapToPerformer() {
    assertEquals("-", mapper.mapToPerformer(null));
    Organization organization = new Organization();
    assertEquals("-", mapper.mapToPerformer(organization));
    organization.setName("My Organization");
    assertEquals("My Organization", mapper.mapToPerformer(organization));
    organization.setAlias(List.of(new StringType("ORG")));
    assertEquals("ORG : My Organization", mapper.mapToPerformer(organization));
  }

  @Test
  void mapToAnalysis() {
    assertEquals("-", mapper.mapToAnalysis(null));
    ServiceRequest serviceRequest = new ServiceRequest();
    assertEquals("-", mapper.mapToAnalysis(serviceRequest));
    serviceRequest.getCode().addCoding(new Coding().setSystem(ANALYSIS_REQUEST_CODE).setCode("code"));
    assertEquals("-", mapper.mapToAnalysis(serviceRequest));
    codeSystem.addConcept(new CodeSystem.ConceptDefinitionComponent().setCode("another_code").setDisplay("not_that_analysis"));
    assertEquals("-", mapper.mapToAnalysis(serviceRequest));
    codeSystem.addConcept(new CodeSystem.ConceptDefinitionComponent().setCode("code").setDisplay("analysis"));
    assertEquals("analysis", mapper.mapToAnalysis(serviceRequest));
  }

  @Test
  void mapToPanelReflex() {
    assertEquals("-", mapper.mapToPanelReflex(null));
    ServiceRequest serviceRequest = new ServiceRequest();
    assertEquals("-", mapper.mapToPanelReflex(serviceRequest));
    serviceRequest.setOrderDetail(List.of(new CodeableConcept().setText(" reflex")));
    assertEquals("reflex", mapper.mapToPanelReflex(serviceRequest));
    serviceRequest.setOrderDetail(List.of(new CodeableConcept().setText(REFLEX_PANEL_PREFIX_FR + "reflex")));
    assertEquals("reflex", mapper.mapToPanelReflex(serviceRequest));
    serviceRequest.setOrderDetail(List.of(new CodeableConcept().setText(REFLEX_PANEL_PREFIX_EN + "reflex")));
    assertEquals("reflex", mapper.mapToPanelReflex(serviceRequest));
  }

  @Test
  void mapToGender() {
    assertEquals("", mapper.mapToGender(null));
    Person person = new Person();
    assertEquals("", mapper.mapToGender(person));
    person.setGender(Enumerations.AdministrativeGender.UNKNOWN);
    when(messagesService.get(any(), any())).thenReturn("gender");
    assertEquals("gender", mapper.mapToGender(person));
  }

  @Test
  void mapToRole() {
    assertEquals("", mapper.mapToRole(null));
    PractitionerRole role = new PractitionerRole();
    assertEquals("", mapper.mapToRole(role));
    role.addCode(new CodeableConcept().addCoding(new Coding().setCode("doctor")));
    when(messagesService.get(any(), any())).thenReturn("role");
    assertEquals("(role)", mapper.mapToRole(role));
  }

}