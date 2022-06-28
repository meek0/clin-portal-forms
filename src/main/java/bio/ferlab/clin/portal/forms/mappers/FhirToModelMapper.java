package bio.ferlab.clin.portal.forms.mappers;

import bio.ferlab.clin.portal.forms.models.ValueName;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.ValueSet;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class FhirToModelMapper {
  
  public Set<String> mapToAnalyseCodes( CodeSystem analyseCode) {
    return analyseCode.getConcept().stream().map(CodeSystem.ConceptDefinitionComponent::getCode).collect(Collectors.toSet());
  }
  
  public List<ValueName> mapToPrescribingInst(List<PractitionerRole> practitionerRoles) {
    return practitionerRoles.stream().map(r -> {
      String orgId = r.getOrganization().getReferenceElement().getIdPart();
      return new ValueName(orgId, orgId);
    }).collect(Collectors.toList());
  }
  
  public List<ValueName> mapClinicalSigns(CodeSystem hp) {
    return hp.getConcept().stream()
        .skip(1)
        .limit(10)
        .map(c -> new ValueName(c.getDisplay(), c.getCode())).collect(Collectors.toList());
  }

  public List<ValueName> mapToOnsetAge(ValueSet age, String lang) {
    return age.getCompose().getIncludeFirstRep().getConcept().stream()
        .map(c -> new ValueName(getDisplayForLang(c, lang), c.getCode())).collect(Collectors.toList());
  }
  
  private String getDisplayForLang(ValueSet.ConceptReferenceComponent concept, String lang) {
    return concept.getDesignation().stream().filter(c -> lang.equals(c.getLanguage()))
        .map(ValueSet.ConceptReferenceDesignationComponent::getValue)
        .findFirst().orElse(concept.getDisplay());
  }
}
