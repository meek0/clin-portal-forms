package bio.ferlab.clin.portal.forms.mappers;

import bio.ferlab.clin.portal.forms.models.config.ValueName;
import bio.ferlab.clin.portal.forms.services.LabelsService;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class FhirToConfigMapperTest {

  final LabelsService labelsService = Mockito.mock(LabelsService.class);
  final FhirToConfigMapper mapper = new FhirToConfigMapper(labelsService);
  
  @BeforeEach
  void beforeEach() {
    when(labelsService.get(any(), any())).thenReturn("label");
  }
  
  @Test
  void mapToAnalyseCodes() {
    final CodeSystem codeSystem = new CodeSystem();
    codeSystem.addConcept(new CodeSystem.ConceptDefinitionComponent().setCode("1"));
    codeSystem.addConcept(new CodeSystem.ConceptDefinitionComponent().setCode("2"));
    codeSystem.addConcept(new CodeSystem.ConceptDefinitionComponent().setCode("3"));
    var result = mapper.mapToAnalyseCodes(codeSystem);
    assertEquals("1 2 3", StringUtils.join(result, " "));
  }
  
  @Test
  void mapToPrescribingInst() {
    final List<PractitionerRole> roles = new ArrayList<>();
    Stream.iterate(0, n -> n +1).limit(2).forEach(n -> {
        PractitionerRole role = new PractitionerRole();
        role.setOrganization(FhirUtils.toReference(new Organization().setId(n.toString())));
        roles.add(role);
    });
    var result = mapper.mapToPrescribingInst(roles);
    assertEquals("ValueName(name=0, value=0) ValueName(name=1, value=1)", StringUtils.join(result, " "));
  }

  @Test
  void mapToClinicalSigns() {
    final CodeSystem codeSystem = new CodeSystem();
    Stream.iterate(0, n -> n +1).limit(20).forEach(n -> {
      codeSystem.getConcept().add(new CodeSystem.ConceptDefinitionComponent().setDisplay("display"+n).setCode(n.toString()));
    });
    var result = mapper.mapToClinicalSigns(codeSystem, "fr");
    assertEquals(10, result.size());  // we keep 10 items
    for(int i=1;i <result.size();i++) { // we skip the 1 item
      final ValueName v = result.get(i-1);
      assertEquals("display"+i, v.getName());
      assertEquals(String.valueOf(i), v.getValue());
    }
  }
  
  @Test
  void mapToClinicalSigns_values() {
    final ValueSet valueSet = new ValueSet();
    Stream.iterate(0, n -> n +1).limit(2).forEach(n -> {
      valueSet.getCompose().getIncludeFirstRep().getConcept().add(new ValueSet.ConceptReferenceComponent().setDisplay("display"+n).setCode(n.toString()));
    });
    var result = mapper.mapToClinicalSigns(valueSet,"fr");
    assertEquals("ValueName(name=display0, value=0) ValueName(name=display1, value=1)", StringUtils.join(result, " "));
  }
  
  @Test
  void mapToOnsetAge() {
    final ValueSet valueSet = new ValueSet();
    Stream.iterate(0, n -> n +1).limit(3).forEach(n -> {
      var concept = new ValueSet.ConceptReferenceComponent().setDisplay("display"+n).setCode(n.toString());
      if (n.equals(1)) {  // add 1 translation
        concept.getDesignation().add(new ValueSet.ConceptReferenceDesignationComponent().setLanguage("fr").setValue("fr" + n));
      }
      valueSet.getCompose().getIncludeFirstRep().getConcept().add(concept);
    });
    var result = mapper.mapToOnsetAge(valueSet, "fr");
    assertEquals("ValueName(name=display0, value=0) ValueName(name=fr1, value=1) ValueName(name=display2, value=2)", StringUtils.join(result, " "));
  }
  
  @Test
  void mapToParentalLinks() {
    final CodeSystem codeSystem = new CodeSystem();
    Stream.iterate(0, n -> n +1).limit(2).forEach(n -> {
      codeSystem.getConcept().add(new CodeSystem.ConceptDefinitionComponent().setDisplay("display"+n).setCode(n.toString()));
    });
    var result = mapper.mapToParentalLinks(codeSystem, null);
    assertEquals("ValueName(name=display0, value=0) ValueName(name=display1, value=1)", StringUtils.join(result, " "));
  }

  @Test
  void mapToEthnicities() {
    final CodeSystem codeSystem = new CodeSystem();
    Stream.iterate(0, n -> n +1).limit(2).forEach(n -> {
      codeSystem.getConcept().add(new CodeSystem.ConceptDefinitionComponent().setDisplay("display"+n).setCode(n.toString()));
    });
    var result = mapper.mapToEthnicities(codeSystem, null);
    assertEquals("ValueName(name=display0, value=0) ValueName(name=display1, value=1)", StringUtils.join(result, " "));
  }
  
  @Test
  void mapToParaclinicalExams() {
    final CodeSystem codeSystem = new CodeSystem();
    Stream.iterate(0, n -> n +1).limit(2).forEach(n -> {
      var concept = new CodeSystem.ConceptDefinitionComponent().setDisplay("display"+n).setCode(n.toString());
      if (n.equals(1)) {  // add 1 translation
        concept.getDesignation().add(new CodeSystem.ConceptDefinitionDesignationComponent().setLanguage("fr").setValue("fr" + n));
      }
      codeSystem.getConcept().add(concept);
    });

    var multiValue = new ValueSet().setName("1-abnormalities");
    multiValue.getCompose().getIncludeFirstRep().getConceptFirstRep().setCode("code1").getDesignation().add(new ValueSet.ConceptReferenceDesignationComponent().setLanguage("fr").setValue("fr1"));
    
    var result = mapper.mapToParaclinicalExams(codeSystem, "fr", List.of(multiValue));
    assertEquals("ValueNameExtra(name=display0, value=0, extra=Extra(type=string, label=label, options=null)) ValueNameExtra(name=fr1, value=1, extra=Extra(type=multi_select, label=label, options=[ValueName(name=fr1, value=code1)]))", StringUtils.join(result, " "));
  }
  
  @Test
  void mapToParaclinicalExams_values() {
    final ValueSet valueSet = new ValueSet();
    Stream.iterate(0, n -> n +1).limit(2).forEach(n -> {
      var concept = new ValueSet.ConceptReferenceComponent().setDisplay("display"+n).setCode(n.toString());
      if (n.equals(0)) {  // add 1 translation
        concept.getDesignation().add(new ValueSet.ConceptReferenceDesignationComponent().setLanguage("fr").setValue("fr" + n));
      }
      valueSet.getCompose().getIncludeFirstRep().addConcept(concept);
    });

    var multiValue = new ValueSet().setName("0-abnormalities");
    multiValue.getCompose().getIncludeFirstRep().getConceptFirstRep().setCode("code0").getDesignation().add(new ValueSet.ConceptReferenceDesignationComponent().setLanguage("fr").setValue("fr0"));

    var result = mapper.mapToParaclinicalExams(valueSet, "fr", List.of(multiValue));
    assertEquals("ValueNameExtra(name=fr0, value=0, extra=Extra(type=multi_select, label=label, options=[ValueName(name=fr0, value=code0)])) ValueNameExtra(name=display1, value=1, extra=Extra(type=string, label=label, options=null))", StringUtils.join(result, " "));

  }
}