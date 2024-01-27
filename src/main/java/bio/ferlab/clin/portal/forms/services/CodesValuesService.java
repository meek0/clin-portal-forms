package bio.ferlab.clin.portal.forms.services;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.configurations.CacheConfiguration;
import bio.ferlab.clin.portal.forms.configurations.FhirConfiguration;
import bio.ferlab.clin.portal.forms.utils.BundleExtractor;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ValueSet;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CodesValuesService {

  private final List<String> missingCodes = new ArrayList<>();

  public static final String ANALYSE_KEY = "analyse";
  public static final String HP_KEY = "hp";
  public static final String PARENTAL_KEY = "parental";
  public static final String ETHNICITY_KEY = "ethnicity";
  public static final String OBSERVATION_KEY = "observation";
  public static final String AGE_KEY = "age";
  public static final String HP_BY_TYPE_SUFFIX = "-hp";
  public static final String OBS_BY_TYPE_SUFFIX = "-observation";
  public static final String MULTI_VALUES_SUFFIX = "-multi-values";

  private final FhirClient fhirClient;
  private final FhirConfiguration fhirConfiguration;
  private final LogOnceService logOnceService;

  @Cacheable(value = CacheConfiguration.CACHE_CODES_VALUES, sync = true, keyGenerator = "customKeyGenerator")
  public CodeSystem getCodes(String key) {
    return (CodeSystem) this.buildCodesAndValues().get(key);
  }

  @Cacheable(value = CacheConfiguration.CACHE_CODES_VALUES, sync = true, keyGenerator = "customKeyGenerator")
  public ValueSet getValues(String key) {
    return (ValueSet) this.buildCodesAndValues().get(key);
  }

  public Object getHPOByCode(String code) {
    CodeSystem allHPOs = getCodes(CodesValuesService.HP_KEY);
    final List<ValueSet> hpByTypes = fhirConfiguration.getTypesWithDefault().stream()
      .map(t -> getValues(t + CodesValuesService.HP_BY_TYPE_SUFFIX)).toList();
    for(ValueSet hpByType: hpByTypes) {
      for(var concept : hpByType.getCompose().getIncludeFirstRep().getConcept()) {
        if (concept.getCode().equals(code)) {
          return concept;
        }
      }
    }
    if (allHPOs != null) {
      for (var concept : allHPOs.getConcept()) {
        if (concept.getCode().equals(code)) {
          return concept;
        }
      }
    }
    return null;
  }

  public CodeSystem.ConceptDefinitionComponent getObservationByCode(String code) {
    var allObs = getCodes(CodesValuesService.OBSERVATION_KEY);
    if (allObs != null) {
      for (var concept : allObs.getConcept()) {
        if (concept.getCode().equals(code)) {
          return concept;
        }
      }
    }
    return null;
  }

  public CodeSystem.ConceptDefinitionComponent getEthnicityByCode(String code) {
    var allObs = getCodes(CodesValuesService.ETHNICITY_KEY);
    if (allObs != null) {
      for (var concept : allObs.getConcept()) {
        if (concept.getCode().equals(code)) {
          return concept;
        }
      }
    }
    return null;
  }

  private Map<String, IBaseResource> buildCodesAndValues() {
    final Bundle bundle = this.fhirClient.fetchCodesAndValues();
    Map<String, IBaseResource> codesAndValues = new HashMap<>();

    BundleExtractor bundleExtractor = new BundleExtractor(fhirClient.getContext(), bundle);

    CodeSystem analyseCode = bundleExtractor.getNextResourcesOfType(CodeSystem.class);
    CodeSystem hp = bundleExtractor.getNextResourcesOfType(CodeSystem.class);
    CodeSystem parentalLinks = bundleExtractor.getNextResourcesOfType(CodeSystem.class);
    CodeSystem ethnicity = bundleExtractor.getNextResourcesOfType(CodeSystem.class);
    CodeSystem observation = bundleExtractor.getNextResourcesOfType(CodeSystem.class);
    ValueSet age = bundleExtractor.getNextResourcesOfType(ValueSet.class);

    codesAndValues.put(ANALYSE_KEY, analyseCode);
    codesAndValues.put(HP_KEY, hp);
    codesAndValues.put(PARENTAL_KEY, parentalLinks);
    codesAndValues.put(ETHNICITY_KEY, ethnicity);
    codesAndValues.put(OBSERVATION_KEY, observation);
    codesAndValues.put(AGE_KEY, age);

    for (String byType : fhirConfiguration.getTypesWithDefault()) {
      ValueSet hpByType = bundleExtractor.getNextResourcesOfType(ValueSet.class);
      ValueSet obsByType = bundleExtractor.getNextResourcesOfType(ValueSet.class);
      codesAndValues.put(byType + HP_BY_TYPE_SUFFIX, hpByType);
      codesAndValues.put(byType + OBS_BY_TYPE_SUFFIX, obsByType);
      validate(hp, hpByType);
      validate(observation, obsByType);
    }

    for (String byType : fhirConfiguration.getMultiValuesObservationCodes()) {
      ValueSet abnormality = bundleExtractor.getNextResourcesOfType(ValueSet.class);
      codesAndValues.put(byType + MULTI_VALUES_SUFFIX, abnormality);
    }

    return codesAndValues;
  }

  private void validate(CodeSystem codes, ValueSet values) {
    if (codes != null && values != null ) {
      for (ValueSet.ConceptReferenceComponent concept : values.getCompose().getIncludeFirstRep().getConcept()) {
        final String code = concept.getCode();
        Optional<CodeSystem.ConceptDefinitionComponent> res = codes.getConcept().stream().filter(c -> c.getCode().equals(code)).findFirst();
        if (res.isEmpty()) {
          logOnceService.warn(String.format("Missing CodeSystem for code: %s", code));
        }
      }
    }
  }

}
