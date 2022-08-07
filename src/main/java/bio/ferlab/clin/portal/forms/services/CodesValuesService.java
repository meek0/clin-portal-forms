package bio.ferlab.clin.portal.forms.services;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.configurations.FhirConfiguration;
import bio.ferlab.clin.portal.forms.utils.BundleExtractor;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ValueSet;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CodesValuesService {
  
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
  
  public CodeSystem getCodes(String key) {
    return (CodeSystem) this.buildCodesAndValues().get(key);
  }

  public ValueSet getValues(String key) {
    return (ValueSet) this.buildCodesAndValues().get(key);
  }
  
  private Map<String, IBaseResource> buildCodesAndValues() {
    final Bundle bundle = this.fhirClient.fetchCodesAndValues();
    
    BundleExtractor bundleExtractor = new BundleExtractor(fhirClient.getContext(), bundle);

    CodeSystem analyseCode = bundleExtractor.getNextResourcesOfType(CodeSystem.class);
    CodeSystem hp = bundleExtractor.getNextResourcesOfType(CodeSystem.class);
    CodeSystem parentalLinks = bundleExtractor.getNextResourcesOfType(CodeSystem.class);
    CodeSystem ethnicity = bundleExtractor.getNextResourcesOfType(CodeSystem.class);
    CodeSystem observation = bundleExtractor.getNextResourcesOfType(CodeSystem.class);
    ValueSet age = bundleExtractor.getNextResourcesOfType(ValueSet.class);

    Map<String, IBaseResource> codesAndValues = new HashMap<>();

    codesAndValues.put(ANALYSE_KEY, analyseCode);
    codesAndValues.put(HP_KEY, hp);
    codesAndValues.put(PARENTAL_KEY, parentalLinks);
    codesAndValues.put(ETHNICITY_KEY, ethnicity);
    codesAndValues.put(OBSERVATION_KEY, observation);
    codesAndValues.put(AGE_KEY, age);

    for(String byType: fhirConfiguration.getTypesWithDefault()) {
      ValueSet hpByType = bundleExtractor.getNextResourcesOfType(ValueSet.class);
      ValueSet obsByType = bundleExtractor.getNextResourcesOfType(ValueSet.class);
      codesAndValues.put(byType + HP_BY_TYPE_SUFFIX,hpByType);
      codesAndValues.put(byType + OBS_BY_TYPE_SUFFIX, obsByType);
    }

    for(String byType: fhirConfiguration.getMultiValuesObservationCodes()) {
      ValueSet abnormality = bundleExtractor.getNextResourcesOfType(ValueSet.class);
      codesAndValues.put(byType + MULTI_VALUES_SUFFIX, abnormality);
    }
    
    return codesAndValues;
  }
}
