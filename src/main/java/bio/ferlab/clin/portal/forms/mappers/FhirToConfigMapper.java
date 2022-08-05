package bio.ferlab.clin.portal.forms.mappers;

import bio.ferlab.clin.portal.forms.models.config.Extra;
import bio.ferlab.clin.portal.forms.models.config.ExtraType;
import bio.ferlab.clin.portal.forms.models.config.ValueName;
import bio.ferlab.clin.portal.forms.models.config.ValueNameExtra;
import bio.ferlab.clin.portal.forms.services.LabelsService;
import bio.ferlab.clin.portal.forms.utils.FhirConstants;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.ValueSet;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FhirToConfigMapper {
  
  private final LabelsService labelsService;
  
  public Set<String> mapToAnalyseCodes( CodeSystem analyseCode) {
    return analyseCode.getConcept().stream().map(CodeSystem.ConceptDefinitionComponent::getCode).collect(Collectors.toSet());
  }
  
  public List<ValueName> mapToPrescribingInst(List<PractitionerRole> practitionerRoles) {
    return practitionerRoles.stream().map(r -> {
      String orgId = r.getOrganization().getReferenceElement().getIdPart();
      return ValueName.builder().name(orgId).value(orgId).build();
    }).collect(Collectors.toList());
  }
  
  public List<ValueName> mapToClinicalSigns(CodeSystem hp) {
    return hp.getConcept().stream()
        .skip(1)
        .limit(10)
        .map(c -> ValueName.builder().name(c.getDisplay()).value(c.getCode()).build()).collect(Collectors.toList());
  }

  public List<ValueName> mapToClinicalSigns(ValueSet hpByType) {
    return hpByType.getCompose().getIncludeFirstRep().getConcept().stream()
        .map(c -> ValueName.builder().name(c.getDisplay()).value(c.getCode()).build()).collect(Collectors.toList());
  }

  public List<ValueName> mapToOnsetAge(ValueSet age, String lang) {
    return extractValuesByLang(age, lang);
  }

  public List<ValueName> mapToParentalLinks(CodeSystem links, String lang) {
    return links.getConcept().stream()
        .map(c -> ValueName.builder().name(getDisplayForLang(c, lang)).value(c.getCode()).build()).collect(Collectors.toList());
  }

  public List<ValueName> mapToEthnicities(CodeSystem ethnicity, String lang) {
    return ethnicity.getConcept().stream()
        .map(c -> ValueName.builder().name(getDisplayForLang(c, lang)).value(c.getCode()).build()).collect(Collectors.toList());
  }

  public List<ValueNameExtra> mapToParaclinicalExams(CodeSystem observation, String lang, List<ValueSet> multiValues) {
    return observation.getConcept().stream()
        .map(c -> ValueNameExtra.builder()
            .name(getDisplayForLang(c, lang))
            .value(c.getCode())
            .extra(buildExtra(c.getCode(), lang, multiValues))
            .build())
        .collect(Collectors.toList());
  }

  public List<ValueNameExtra> mapToParaclinicalExams(ValueSet observation, String lang, List<ValueSet> multiValues) {
    return observation.getCompose().getIncludeFirstRep().getConcept().stream()
        .map(c -> ValueNameExtra.builder()
            .name(getDisplayForLang(c, lang))
            .value(c.getCode())
            .extra(buildExtra(c.getCode(), lang, multiValues))
            .build())
        .collect(Collectors.toList());
  }
  
  private Extra buildExtra(String code, String lang, List<ValueSet> multiValues) {
    Optional<ValueSet> byCode = multiValues.stream().filter(vs -> (code + FhirConstants.ABNORMALITIES_SUFFIX).equalsIgnoreCase(vs.getName())).findFirst();
    if(byCode.isPresent()) {
      return Extra.builder().label(getLabel(code, lang)).type(ExtraType.multi_select).options(extractValuesByLang(byCode.get(), lang)).build();
    } else {
      return Extra.builder().label(getLabel(code, lang)).type(ExtraType.string).build();
    }
  }
  
  private String getLabel(String code, String lang) {
    return labelsService.getLabel(code, lang);
  }
  
  private List<ValueName> extractValuesByLang(ValueSet valueSet, String lang) {
    return valueSet.getCompose().getIncludeFirstRep().getConcept().stream()
        .map(c -> ValueName.builder().name(getDisplayForLang(c, lang)).value(c.getCode()).build()).collect(Collectors.toList());
  }
  
  private String getDisplayForLang(ValueSet.ConceptReferenceComponent concept, String lang) {
    return concept.getDesignation().stream().filter(c -> StringUtils.isNotBlank(lang) && lang.equals(c.getLanguage()))
        .map(ValueSet.ConceptReferenceDesignationComponent::getValue)
        .findFirst().orElse(concept.getDisplay());
  }

  private String getDisplayForLang(CodeSystem.ConceptDefinitionComponent concept, String lang) {
    return concept.getDesignation().stream().filter(c -> StringUtils.isNotBlank(lang) && lang.equals(c.getLanguage()))
        .map(CodeSystem.ConceptDefinitionDesignationComponent::getValue)
        .findFirst().orElse(concept.getDisplay());
  }
}
