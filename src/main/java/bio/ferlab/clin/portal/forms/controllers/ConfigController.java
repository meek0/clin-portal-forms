package bio.ferlab.clin.portal.forms.controllers;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.configurations.FhirConfiguration;
import bio.ferlab.clin.portal.forms.mappers.FhirToConfigMapper;
import bio.ferlab.clin.portal.forms.models.builders.PractitionerBuilder;
import bio.ferlab.clin.portal.forms.models.config.Form;
import bio.ferlab.clin.portal.forms.services.CodesValuesService;
import bio.ferlab.clin.portal.forms.services.LocaleService;
import bio.ferlab.clin.portal.forms.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ValueSet;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static bio.ferlab.clin.portal.forms.utils.FhirConst.DEFAULT_EXAM_SUFFIX;
import static bio.ferlab.clin.portal.forms.utils.FhirConst.DEFAULT_HPO_SUFFIX;

@RestController
@RequestMapping("/form")
@RequiredArgsConstructor
public class ConfigController {

  private final FhirConfiguration fhirConfiguration;
  private final FhirClient fhirClient;
  private final LocaleService localeService;
  private final FhirToConfigMapper fhirToConfigMapper;
  private final CodesValuesService codesValuesService;

  @GetMapping("/{panelCode}")
  public Form config(@RequestHeader String authorization,
                     @PathVariable String panelCode) {
    
    final String lang = localeService.getCurrentLocale();
    final String practitionerId = JwtUtils.getProperty(authorization, JwtUtils.FHIR_PRACTITIONER_ID);

    PractitionerBuilder.Result roles = new PractitionerBuilder(fhirClient, practitionerId).build();
    
    CodeSystem analyseCode = codesValuesService.getCodes(CodesValuesService.ANALYSE_KEY);
    CodeSystem hp = codesValuesService.getCodes(CodesValuesService.HP_KEY);
    CodeSystem parentalLinks = codesValuesService.getCodes(CodesValuesService.PARENTAL_KEY);
    CodeSystem ethnicity = codesValuesService.getCodes(CodesValuesService.ETHNICITY_KEY);
    CodeSystem observation = codesValuesService.getCodes(CodesValuesService.OBSERVATION_KEY);
    ValueSet age = codesValuesService.getValues(CodesValuesService.AGE_KEY);
    
    // validate the form's type is supported
    if (analyseCode.getConcept().stream().noneMatch(c -> panelCode.equals(c.getCode()))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("unsupported form panel code: %s available codes: %s",
          panelCode, fhirToConfigMapper.mapToAnalyseCodes(analyseCode)));
    }
    
    // return form's config
    Form form = new Form();
    form.getConfig().getPrescribingInstitutions().addAll(fhirToConfigMapper.mapToPrescribingInst(roles.getPractitionerRoles()));
    form.getConfig().getClinicalSigns().getOnsetAge().addAll(fhirToConfigMapper.mapToOnsetAge(age, lang));
    form.getConfig().getHistoryAndDiagnosis().getParentalLinks().addAll(fhirToConfigMapper.mapToParentalLinks(parentalLinks, lang));
    form.getConfig().getHistoryAndDiagnosis().getEthnicities().addAll(fhirToConfigMapper.mapToEthnicities(ethnicity, lang));
    
    // use form default or generic values
    final List<ValueSet> hpByTypes = fhirConfiguration.getTypesWithDefault().stream()
        .map(t -> codesValuesService.getValues(t + CodesValuesService.HP_BY_TYPE_SUFFIX)).collect(Collectors.toList());
    final List<ValueSet> obsByTypes = fhirConfiguration.getTypesWithDefault().stream()
        .map(t -> codesValuesService.getValues(t + CodesValuesService.OBS_BY_TYPE_SUFFIX)).collect(Collectors.toList());
    final List<ValueSet> multiValues = fhirConfiguration.getMultiValuesObservationCodes().stream()
        .map(t -> codesValuesService.getValues(t + CodesValuesService.MULTI_VALUES_SUFFIX)).collect(Collectors.toList());
    this.applyFormHpByTypeOrDefault(panelCode, form, hp, hpByTypes);
    this.applyFormObservationByTypeOrDefault(panelCode, form, lang, observation, obsByTypes, multiValues);
    
    return form;
  }
  
  private void applyFormHpByTypeOrDefault(String formType, Form form, CodeSystem all, List<ValueSet> byTypes) {
    Optional<ValueSet> byType = byTypes.stream().filter(vs -> (formType + DEFAULT_HPO_SUFFIX).equalsIgnoreCase(vs.getName())).findFirst();
    if (byType.isPresent()) {
      form.getConfig().getClinicalSigns().getDefaultList().addAll(fhirToConfigMapper.mapToClinicalSigns(byType.get()));
    } else {
      form.getConfig().getClinicalSigns().getDefaultList().addAll(fhirToConfigMapper.mapToClinicalSigns(all));
    }
  }

  private void applyFormObservationByTypeOrDefault(String formType, Form form, String lang, CodeSystem all, List<ValueSet> byTypes, List<ValueSet> multiValues) {
    Optional<ValueSet> byType = byTypes.stream().filter(vs -> (formType + DEFAULT_EXAM_SUFFIX).equalsIgnoreCase(vs.getName())).findFirst();
    if (byType.isPresent()) {
      form.getConfig().getParaclinicalExams().getDefaultList().addAll(fhirToConfigMapper.mapToParaclinicalExams(byType.get(), lang, multiValues));
    } else {
      form.getConfig().getParaclinicalExams().getDefaultList().addAll(fhirToConfigMapper.mapToParaclinicalExams(all, lang, multiValues));
    }
  }
}
