package bio.ferlab.clin.portal.forms.controllers;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.mappers.FhirToModelMapper;
import bio.ferlab.clin.portal.forms.models.Form;
import bio.ferlab.clin.portal.forms.services.LocaleService;
import bio.ferlab.clin.portal.forms.utils.BundleExtractor;
import bio.ferlab.clin.portal.forms.utils.JwtUtils;
import io.undertow.util.BadRequestException;
import org.hl7.fhir.r4.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/form")
public class FormController {
  
  private final FhirClient fhirClient;
  private final JwtUtils jwtUtils;
  private final LocaleService localeService;
  private final FhirToModelMapper fhirToModelMapper;
  
  public FormController(FhirClient fhirClient, 
                        JwtUtils jwtUtils,
                        LocaleService localeService,
                        FhirToModelMapper fhirToModelMapper) {
    this.fhirClient = fhirClient;
    this.jwtUtils = jwtUtils;
    this.localeService = localeService;
    this.fhirToModelMapper = fhirToModelMapper;
  }

  @GetMapping("/{type}")
  public Form config(@RequestHeader String authorization,
                       @PathVariable String type) throws BadRequestException {
    
    final String lang = localeService.getCurrentLocale();
    final String practitionerId = jwtUtils.getProperty(authorization, JwtUtils.FHIR_PRACTITIONER_ID);
    
    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.BATCH);
    
    bundle.addEntry().getRequest()
        .setUrl("CodeSystem/analysis-request-code")
        .setMethod(Bundle.HTTPVerb.GET);

    bundle.addEntry().getRequest()
        .setUrl("CodeSystem/hp")  // really big!!!!!!!
        .setMethod(Bundle.HTTPVerb.GET);
    
    bundle.addEntry().getRequest()
        .setUrl("ValueSet/age-at-onset")
        .setMethod(Bundle.HTTPVerb.GET);

    bundle.addEntry().getRequest()
        .setUrl("CodeSystem/fmh-relationship-plus")
        .setMethod(Bundle.HTTPVerb.GET);

    bundle.addEntry().getRequest()
        .setUrl("CodeSystem/qc-ethnicity")
        .setMethod(Bundle.HTTPVerb.GET);

    bundle.addEntry().getRequest()
        .setUrl("PractitionerRole?practitioner="+practitionerId)
        .setMethod(Bundle.HTTPVerb.GET);
    
    Bundle response = fhirClient.getGenericClient().transaction().withBundle(bundle).execute();
    BundleExtractor bundleExtractor = new BundleExtractor(fhirClient.getContext(), response);

    CodeSystem analyseCode = bundleExtractor.getNextResourcesOfType(CodeSystem.class);
    CodeSystem hp = bundleExtractor.getNextResourcesOfType(CodeSystem.class);
    ValueSet age = bundleExtractor.getNextResourcesOfType(ValueSet.class);
    CodeSystem parentalLinks = bundleExtractor.getNextResourcesOfType(CodeSystem.class);
    CodeSystem ethnicity = bundleExtractor.getNextResourcesOfType(CodeSystem.class);
    
    if (analyseCode.getConcept().stream().noneMatch(c -> type.equals(c.getCode()))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Unsupported form type: '%s' available types: %s",
          type, fhirToModelMapper.mapToAnalyseCodes(analyseCode)));
    }
    
    List<PractitionerRole> practitionerRoles = bundleExtractor.getNextListOfResourcesOfType(PractitionerRole.class);

    Form form = new Form();
    form.getConfig().getPrescribingInstitutions().addAll(fhirToModelMapper.mapToPrescribingInst(practitionerRoles));
    form.getConfig().getClinicalSigns().getDefaultList().addAll(fhirToModelMapper.mapToClinicalSigns(hp));
    form.getConfig().getClinicalSigns().getOnsetAge().addAll(fhirToModelMapper.mapToOnsetAge(age, lang));
    form.getConfig().getHistoryAndDiagnosis().getParentalLinks().addAll(fhirToModelMapper.mapToParentalLinks(parentalLinks, lang));
    form.getConfig().getHistoryAndDiagnosis().getEthnicities().addAll(fhirToModelMapper.mapToEthnicities(ethnicity, lang));
    
    return form;
  }
  
  

}
