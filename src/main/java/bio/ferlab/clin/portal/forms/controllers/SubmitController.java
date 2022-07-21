package bio.ferlab.clin.portal.forms.controllers;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import bio.ferlab.clin.portal.forms.models.submit.Request;
import bio.ferlab.clin.portal.forms.services.LocaleService;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import bio.ferlab.clin.portal.forms.utils.PatientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/form")
@Slf4j
public class SubmitController {

  private final FhirClient fhirClient;
  private final SubmitToFhirMapper mapper;
  private final LocaleService localeService;
  
  public SubmitController(FhirClient fhirClient, SubmitToFhirMapper mapper, LocaleService localeService) {
    this.fhirClient = fhirClient;
    this.mapper = mapper;
    this.localeService = localeService;
  }

  @PostMapping("/{type}")
  public ResponseEntity<String> submit(@RequestHeader String authorization,
                               @PathVariable String type,
                               @Valid @RequestBody Request request) {
    /*
    1- create/get Person DONE
    2- create/get Patient DONE
    3- create servicerequest analysis
    4- create servicerequest sequencing
    5 - create specimen
    5 -create clinicalimpressoin 
    6 create obsetrvations X 
     */
    
    final PatientBuilder patientBuilder = new PatientBuilder(fhirClient, mapper, request.getPatient());
    PatientBuilder.PatientBuilderResult pbr = patientBuilder.
      validateEp().
      validateRamqAndMrn().
      findByRamq().
      findByMrn().
      build();
    
     submit(pbr);
    
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }
  
  private void submit(PatientBuilder.PatientBuilderResult patientBuilderResult) {
    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.TRANSACTION);

    bundle.addEntry()
        .setFullUrl("Patient/"+patientBuilderResult.getPatient().getIdElement().getIdPart())
        .setResource(patientBuilderResult.getPatient())
        .getRequest()
        .setUrl("Patient/"+patientBuilderResult.getPatient().getIdElement().getIdPart())
        .setMethod(patientBuilderResult.isPatientNew() ? Bundle.HTTPVerb.POST: Bundle.HTTPVerb.PUT);

    bundle.addEntry()
        .setFullUrl("Person/"+patientBuilderResult.getPerson().getIdElement().getIdPart())
        .setResource(patientBuilderResult.getPerson())
        .getRequest()
        .setUrl("Person/"+patientBuilderResult.getPerson().getIdElement().getIdPart())
        .setMethod(patientBuilderResult.isPersonNew() ? Bundle.HTTPVerb.POST: Bundle.HTTPVerb.PUT);
    
    for(Bundle.BundleEntryComponent entry : bundle.getEntry()) {
      log.info(entry.getRequest().getMethod() + " " + entry.getFullUrl());
    }

    FhirUtils.logDebug(fhirClient, bundle);
    
    FhirUtils.validate(fhirClient, bundle);
    
    Bundle response = this.fhirClient.getGenericClient().transaction().withBundle(bundle).encodedJson().execute();
    FhirUtils.logDebug(fhirClient, response);
  }
  
}
