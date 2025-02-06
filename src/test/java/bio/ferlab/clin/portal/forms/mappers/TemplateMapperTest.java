package bio.ferlab.clin.portal.forms.mappers;

import bio.ferlab.clin.portal.forms.services.CodesValuesService;
import bio.ferlab.clin.portal.forms.services.LogOnceService;
import bio.ferlab.clin.portal.forms.services.MessagesService;
import bio.ferlab.clin.portal.forms.services.TemplateService;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static bio.ferlab.clin.portal.forms.models.builders.ReflexBuilder.REFLEX_PANEL_PREFIX_EN;
import static bio.ferlab.clin.portal.forms.models.builders.ReflexBuilder.REFLEX_PANEL_PREFIX_FR;
import static bio.ferlab.clin.portal.forms.utils.FhirConst.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TemplateMapperTest {

  final MessagesService messagesService = Mockito.mock(MessagesService.class);
  final LogOnceService logOnceService = Mockito.mock(LogOnceService.class);
  final TemplateService templateService = Mockito.mock(TemplateService.class);
  final CodesValuesService codesValuesService = Mockito.mock(CodesValuesService.class);
  final CodeSystem codeSystem = new CodeSystem();
  final TemplateMapper mapper = new TemplateMapper("id", logOnceService, messagesService, templateService, codesValuesService, codeSystem, Locale.FRENCH);

  @Test
  void mapToBarcodeBase64() {
    when(templateService.generateBarcodeImage(any())).thenReturn(new BufferedImage(100,100, BufferedImage.TYPE_INT_RGB));
    when(templateService.convertToBase64(any())).thenReturn("dataBase64Image");
    assertEquals("dataBase64Image", mapper.mapToBarcodeBase64("1234"));
  }

  @Test
  void mapToComment() {
    var sr = new ServiceRequest();
    sr.getNoteFirstRep().setText("comment");
    assertEquals("comment", mapper.mapToComment(sr));
  }

  @Test
  void mapToSigns() {
    var o1 = new Observation();
    o1.getCode().getCodingFirstRep().setCode("PHEN");
    o1.getInterpretationFirstRep().getCodingFirstRep().setCode("POS");
    o1.setValue(new CodeableConcept());
    o1.getValueCodeableConcept().getCodingFirstRep().setCode("SIGN1");

    var o2 = new Observation();
    o2.getCode().getCodingFirstRep().setCode("PHEN");
    o2.getInterpretationFirstRep().getCodingFirstRep().setCode("POS");
    o2.setValue(new CodeableConcept());
    o2.getValueCodeableConcept().getCodingFirstRep().setCode("SIGN2");

    var o3 = new Observation();
    o3.getCode().getCodingFirstRep().setCode("PHEN");
    o3.getInterpretationFirstRep().getCodingFirstRep().setCode("POS");
    o3.setValue(new CodeableConcept());
    o3.getValueCodeableConcept().getCodingFirstRep().setCode("SIGN3");
    o3.addExtension(AGE_AT_ONSET_EXT, new Coding().setCode("age_code3"));
    var allAges = new ValueSet();
    allAges.getCompose().getIncludeFirstRep().addConcept().setCode("age_code3").getDesignationFirstRep().setLanguage("fr").setValue("age FR");
    when(codesValuesService.getValues(eq(CodesValuesService.AGE_KEY))).thenReturn(allAges);

    var o4 = new Observation();
    o4.getCode().getCodingFirstRep().setCode("PHEN");
    o4.getInterpretationFirstRep().getCodingFirstRep().setCode("NEG");
    o4.setValue(new CodeableConcept());
    o4.getValueCodeableConcept().getCodingFirstRep().setCode("SIGN4");

    var o1Concept = new ValueSet.ConceptReferenceComponent();
    o1Concept.setCode("SIGN1").getDesignationFirstRep().setLanguage("fr").setValue("sign 1 FR");
    when(codesValuesService.getHPOByCode(eq("SIGN1"))).thenReturn(o1Concept);
    var o2Concept = new CodeSystem.ConceptDefinitionComponent();
    o2Concept.setCode("SIGN2").getDesignationFirstRep().setLanguage("fr").setValue("sign 2 FR");
    when(codesValuesService.getHPOByCode(eq("SIGN2"))).thenReturn(o2Concept);
    when(codesValuesService.getHPOByCode(eq("SIGN3"))).thenReturn(null);

    var all = List.of(o1,o2,o3);

    assertEquals(List.of("Sign 1 FR (SIGN1)", "Sign 2 FR (SIGN2)", "(SIGN3) - age FR"), mapper.mapToSigns(all, "PHEN", "POS"));
  }

  @Test
  void mapToSign() {
    var o1 = new Observation();
    o1.getCode().getCodingFirstRep().setCode("stringType");
    o1.setValue(new StringType("foo"));

    var o2 = new Observation();
    o2.getCode().getCodingFirstRep().setCode("booleanType");
    o2.setValue(new BooleanType("true"));

    when(messagesService.get(eq("true"), any())).thenReturn("bar FR");

    var all = List.of(o1, o2);

    assertEquals("foo", mapper.mapToSign(all, "stringType", ""));
    assertEquals("bar FR", mapper.mapToSign(all, "booleanType", ""));
    assertEquals("", mapper.mapToSign(all, "missing", ""));
  }

  @Test
  void mapToEthnicity() {
    var o1 = new Observation();
    o1.getCode().getCodingFirstRep().setCode("NOT_ETH");

    var o2 = new Observation();
    o2.getCode().getCodingFirstRep().setCode("ETHN");
    o2.setValue(new CodeableConcept());
    o2.getValueCodeableConcept().getCodingFirstRep().setCode("ETH_VALUE");

    var ethCode = new CodeSystem.ConceptDefinitionComponent();
    ethCode.setCode("ETH_VALUE").getDesignationFirstRep().setLanguage("fr").setValue("eth FR");
    when(codesValuesService.getCodeSystemByKeyCode(eq(CodesValuesService.ETHNICITY_KEY), any())).thenReturn(ethCode);

    var all = List.of(o1, o2);

    assertEquals("eth FR", mapper.mapToEthnicity(all));
    verify(codesValuesService).getCodeSystemByKeyCode(eq(CodesValuesService.ETHNICITY_KEY), eq("ETH_VALUE"));
  }

  @Test
  void mapToEthnicity_no_display() {
    var o2 = new Observation();
    o2.getCode().getCodingFirstRep().setCode("ETHN");
    o2.setValue(new CodeableConcept());
    o2.getValueCodeableConcept().getCodingFirstRep().setCode("ETH_VALUE");

    when(codesValuesService.getCodeSystemByKeyCode(eq(CodesValuesService.ETHNICITY_KEY), any())).thenReturn(null);

    var all = List.of(o2);

    assertEquals("ETH_VALUE", mapper.mapToEthnicity(all));
  }

  @Test
  void mapToExams() {
    var o1 = new Observation();
    o1.getCategoryFirstRep().getCodingFirstRep().setCode("procedure");
    o1.getCode().getCodingFirstRep().setCode("code1");
    o1.getInterpretationFirstRep().getCodingFirstRep().setCode("A");

    var code1 = new CodeSystem.ConceptDefinitionComponent();
    code1.setCode("code1").getDesignationFirstRep().setLanguage("fr").setValue("code1 FR");
    when(codesValuesService.getCodeSystemByKeyCode(eq(CodesValuesService.OBSERVATION_KEY), eq("code1"))).thenReturn(code1);
    when(messagesService.get(eq("interpretation_A"), eq("fr"))).thenReturn("Abnormal");

    var o1Values = new CodeableConcept();
    o1Values.addCoding().setCode("o1_code1");
    var o1value1Concept = new ValueSet.ConceptReferenceComponent();
    o1value1Concept.setCode("o1_code1").getDesignationFirstRep().setLanguage("fr").setValue("o1 value1 FR");
    when(codesValuesService.getHPOByCode(eq("o1_code1"))).thenReturn(o1value1Concept);
    o1.setValue(o1Values);

    var o2 = new Observation();
    o2.getCode().getCodingFirstRep().setCode("code2");
    o2.getCategoryFirstRep().getCodingFirstRep().setCode("procedure");
    o2.addInterpretation(new CodeableConcept(new Coding().setCode("A")));
    var o2Values = new StringType("o2value");
    o2.setValue(o2Values);

    var o3 = new Observation();
    o3.getCategoryFirstRep().getCodingFirstRep().setCode("procedure");

    var o4 = new Observation();
    o4.getCategoryFirstRep().getCodingFirstRep().setCode("not_procedure");

    var all = List.of(o1,o2,o3);

    assertEquals("[Exam[name=code1 FR, comment=Abnormal : o1 value1 FR], Exam[name=code2, comment=Abnormal : o2value UI/L], Exam[name=, comment=]]", mapper.mapToExams(all).toString());
  }

  @Test
  void mapToFamilyHistory() {
    var fm1 = new FamilyMemberHistory();
    fm1.getNoteFirstRep().setText("fm1 text");
    fm1.getRelationship().getCodingFirstRep().setCode("fm1_code");
    var code1 = new CodeSystem.ConceptDefinitionComponent();
    code1.setCode("code1").getDesignationFirstRep().setLanguage("fr").setValue("code1 FR");
    when(codesValuesService.getCodeSystemByKeyCode(eq(CodesValuesService.PARENTAL_KEY), eq("fm1_code"))).thenReturn(code1);

    var fm2 = new FamilyMemberHistory();
    fm2.getNoteFirstRep().setText("fm2 text");
    fm2.getRelationship().getCodingFirstRep().setCode("fm2_code");
    when(codesValuesService.getCodeSystemByKeyCode(eq(CodesValuesService.PARENTAL_KEY), eq("fm2_code"))).thenReturn(null);

    var fm3 = new FamilyMemberHistory();

    var all = List.of(fm1, fm2, fm3);

    assertEquals("fm1 text (code1 FR), fm2 text (fm2_code)", mapper.mapToFamilyHistory(all));
  }

  @Test
  void mapToAddress() {
    assertEquals("", mapper.mapToAddress(null));
    Organization org = new Organization();
    assertEquals("", mapper.mapToAddress(org));
    org.getContactFirstRep().getAddress().setText("foo");
    assertEquals("foo", mapper.mapToAddress(org));
  }

  @Test
  void mapToName() {
    assertEquals("", mapper.mapToName(null));
    Person person = new Person();
    assertEquals("", mapper.mapToName(person));
    person.getNameFirstRep().setFamily("Doe").setGiven(List.of(new StringType("John")));
    assertEquals("DOE John", mapper.mapToName(person));
  }

  @Test
  void mapToRAMQ() {
    assertEquals("", mapper.mapToRAMQ(null));
    Person person = new Person();
    assertEquals("--", mapper.mapToRAMQ(person));
    person.addIdentifier(new Identifier().setValue("abcd12345678").setType(new CodeableConcept().addCoding(new Coding().setCode("JHN"))));
    assertEquals("ABCD 1234 5678", mapper.mapToRAMQ(person));
  }

  @Test
  void mapToMRN() {
    assertEquals("", mapper.mapToMRN(null));
    Patient patient = new Patient();
    assertEquals("--", mapper.mapToMRN(patient));
    patient.addIdentifier(new Identifier().setValue("mrn-foo-1234").setType(new CodeableConcept().addCoding(new Coding().setCode("MR"))));
    assertEquals("MRN-FOO-1234", mapper.mapToMRN(patient));
    patient.setManagingOrganization(new Reference("Organization/bar"));
    assertEquals("MRN-FOO-1234 | BAR", mapper.mapToMRN(patient));
  }

  @Test
  void formatDate() {
    assertEquals("", mapper.formatDate(null));
    assertNotNull(mapper.formatDate(new Date()));
  }

  @Test
  void mapToAuthor() {
    assertEquals("", mapper.mapToAuthor(null));
    Practitioner practitioner = new Practitioner();
    assertEquals("", mapper.mapToAuthor(practitioner));
    practitioner.getNameFirstRep().setFamily("Doe").setGiven(List.of(new StringType("John")));
    assertEquals("DOE John", mapper.mapToAuthor(practitioner));
    practitioner.getNameFirstRep().addPrefix("Dr");
    assertEquals("Dr. DOE John", mapper.mapToAuthor(practitioner));
  }

  @Test
  void mapToContact_organization() {
    assertEquals("", mapper.mapToContact(null, null));
    Organization organization = new Organization();
    assertEquals("", mapper.mapToContact(organization, null));
    organization.getContactFirstRep().addTelecom(new ContactPoint().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("123456789"));
    organization.getContactFirstRep().addTelecom(new ContactPoint().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue("foo@bar"));
    assertEquals("123456789", mapper.mapToContact(organization, "phone"));
    assertEquals("foo@bar", mapper.mapToContact(organization, "email"));
  }

  @Test
  void mapToContact_prescriber() {
    assertEquals("", mapper.mapToContact(null,null,null));
    PractitionerRole prescriber = new PractitionerRole();
    assertEquals("", mapper.mapToContact(prescriber, null, null));
    prescriber.addTelecom(new ContactPoint().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue("prescriber@mail"));
    assertEquals("prescriber@mail", mapper.mapToContact(prescriber,null, "email"));
    PractitionerRole supervisor = new PractitionerRole();
    assertEquals("prescriber@mail", mapper.mapToContact(prescriber,supervisor, "email"));
    supervisor.addTelecom(new ContactPoint().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue("supervisor@mail"));
    assertEquals("supervisor@mail", mapper.mapToContact(prescriber,supervisor, "email"));
  }

  @Test
  void mapToPerformer() {
    assertEquals("", mapper.mapToPerformer(null));
    Organization organization = new Organization();
    assertEquals("", mapper.mapToPerformer(organization));
    organization.setName("My Organization");
    assertEquals("My Organization", mapper.mapToPerformer(organization));
    organization.setAlias(List.of(new StringType("ORG")));
    assertEquals("ORG : My Organization", mapper.mapToPerformer(organization));
  }

  @Test
  void mapToAnalysis() {
    assertEquals("", mapper.mapToAnalysis(null));
    ServiceRequest serviceRequest = new ServiceRequest();
    assertEquals("", mapper.mapToAnalysis(serviceRequest));
    serviceRequest.getCode().addCoding(new Coding().setSystem(ANALYSIS_REQUEST_CODE).setCode("code"));
    assertEquals("", mapper.mapToAnalysis(serviceRequest));
    codeSystem.addConcept(new CodeSystem.ConceptDefinitionComponent().setCode("another_code").setDisplay("not_that_analysis"));
    assertEquals("", mapper.mapToAnalysis(serviceRequest));
    codeSystem.addConcept(new CodeSystem.ConceptDefinitionComponent().setCode("code").setDisplay("analysis"));
    assertEquals("analysis", mapper.mapToAnalysis(serviceRequest));
  }

  @Test
  void mapToPanelReflex() {
    assertEquals("", mapper.mapToPanelReflex(null));
    ServiceRequest serviceRequest = new ServiceRequest();
    assertEquals("--", mapper.mapToPanelReflex(serviceRequest));
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
    assertEquals("", mapper.mapToRole(role));
    role.getCode().clear(); // reset role
    role.addCode(new CodeableConcept().addCoding(new Coding().setCode("405277009")));
    assertEquals("(role)", mapper.mapToRole(role));
  }

  @Test
  void mapToMissingReason() {
    var o0 = new Observation();

    var o1 = new Observation();
    o1.getCategoryFirstRep().getCodingFirstRep().setCode("social-history");
    o1.getValueCodeableConcept().getCodingFirstRep().setSystem(SYSTEM_MISSING_PARENT);
    o1.getNoteFirstRep().setText("foo");

    var all = List.of(o0, o1);

    assertEquals("", mapper.mapToMissingReason(null));
    assertEquals("foo", mapper.mapToMissingReason(all));
  }

  @Test
  void mapToAffected() {
    var o0 = new Observation();

    var o1 = new Observation();
    o1.getCode().getCodingFirstRep().setCode("DSTA");
    o1.getInterpretationFirstRep().getCodingFirstRep().setCode("POS");
    o1.setValue(new CodeableConcept());
    o1.getValueCodeableConcept().getCodingFirstRep().setCode("SIGN1");

    var o2 = new Observation();
    o2.getCode().getCodingFirstRep().setCode("DSTA");
    o2.getInterpretationFirstRep().getCodingFirstRep().setCode("NEG");
    o2.setValue(new CodeableConcept());
    o2.getValueCodeableConcept().getCodingFirstRep().setCode("SIGN2");

    when(messagesService.get(eq("clinical_status_affected"), eq("fr"))).thenReturn("aff");
    when(messagesService.get(eq("clinical_status_not_affected"), eq("fr"))).thenReturn("not_aff");

    assertEquals("", mapper.mapToAffected(null));
    assertEquals("", mapper.mapToAffected(List.of(o0)));
    assertEquals("aff", mapper.mapToAffected(List.of(o1)));
    assertEquals("not_aff", mapper.mapToAffected(List.of(o2)));
  }

  @Test
  void mapToRelation() {

    assertEquals("", mapper.mapToRelation(null));
    assertEquals("MTH", mapper.mapToRelation("MTH"));

    var value = new ValueSet.ConceptReferenceComponent();
    value.getDesignationFirstRep().setLanguage("fr").setValue("Mother_fr");
    when(codesValuesService.getValueSetByKeyCode(any(), any())).thenReturn(value);

    assertEquals("Mother_fr", mapper.mapToRelation("MTH"));
    verify(codesValuesService, times(2)).getValueSetByKeyCode(eq(CodesValuesService.RELATION_KEY), eq("MTH"));
  }

}
