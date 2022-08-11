package bio.ferlab.clin.portal.forms.controllers;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import bio.ferlab.clin.portal.forms.models.builders.*;
import bio.ferlab.clin.portal.forms.models.submit.Request;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import bio.ferlab.clin.portal.forms.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/form")
@RequiredArgsConstructor
public class SubmitController {

  private final FhirClient fhirClient;
  private final SubmitToFhirMapper mapper;

  @PostMapping
  public ResponseEntity<String> submit(@RequestHeader String authorization,
                                       @Valid @RequestBody Request request) {
  
    // The following code is for SOLO only

    final String practitionerId = JwtUtils.getProperty(authorization, JwtUtils.FHIR_PRACTITIONER_ID);
    final String panelCode = request.getAnalyse().getPanelCode();
    final String ep = request.getPatient().getEp();
 
    final PatientBuilder patientBuilder = new PatientBuilder(fhirClient, mapper, request.getPatient());
    PatientBuilder.Result pbr = patientBuilder
        .validateRamqAndMrn()
        .findByRamq()
        .findByMrn()
        .build(true, true);
    
    final NewBornBuilder newBornBuilder = new NewBornBuilder(mapper, request.getPatient().getAdditionalInfo(), pbr.getPatient());
    NewBornBuilder.Result nbr = newBornBuilder
        .build();
    
    final PractitionerBuilder practitionerBuilder = new PractitionerBuilder(fhirClient, practitionerId);
    PractitionerBuilder.Result roleBr = practitionerBuilder
        .withSupervisor(request.getAnalyse().getResidentSupervisor())
        .withEp(ep)
        .build();
    
    final ObservationsBuilder observationsBuilder = new ObservationsBuilder(mapper, panelCode, pbr.getPatient(), request.getAnalyse(),
        request.getClinicalSigns(), request.getParaclinicalExams(), request.getPatient().getEthnicity());
    ObservationsBuilder.Result obr = observationsBuilder
        .validate()
        .build();

    final ClinicalImpressionBuilder clinicalImpressionBuilder = new ClinicalImpressionBuilder(mapper, 
        pbr.getPerson(), pbr.getPatient(), obr.getObservations());
    ClinicalImpressionBuilder.Result cbr = clinicalImpressionBuilder
        .build();
    
    final AnalysisBuilder analysisBuilder = new AnalysisBuilder(fhirClient, mapper, panelCode, pbr.getPatient(), 
        cbr.getClinicalImpression(), roleBr.getPractitionerRole(), roleBr.getSupervisorRole(), request.getAnalyse().getComment());
    AnalysisBuilder.Result abr = analysisBuilder
        .withReflex(request.getAnalyse().getIsReflex())
        .build();

    final SequencingBuilder sequencingBuilder = new SequencingBuilder(mapper, panelCode, 
        pbr.getPatient(), abr.getAnalysis(), roleBr.getPractitionerRole());
    SequencingBuilder.Result sbr = sequencingBuilder
        .build();
    
    submit(pbr, nbr, abr, sbr, cbr, obr);
    
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }
  
  private void submit(PatientBuilder.Result pbr, 
                      NewBornBuilder.Result nbr,
                      AnalysisBuilder.Result abr, 
                      SequencingBuilder.Result sbr, 
                      ClinicalImpressionBuilder.Result cbr,
                      ObservationsBuilder.Result obr) {
    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.TRANSACTION);
    
    final String patientRef = FhirUtils.formatResource(pbr.getPatient());
    final String personRef = FhirUtils.formatResource(pbr.getPerson());

    bundle.addEntry()
        .setFullUrl(patientRef)
        .setResource(pbr.getPatient())
        .getRequest()
        .setUrl(patientRef) // full url with ID required if PUT
        .setMethod(pbr.isPatientNew() ? Bundle.HTTPVerb.POST: Bundle.HTTPVerb.PUT);

    bundle.addEntry()
        .setFullUrl(personRef)
        .setResource(pbr.getPerson())
        .getRequest()
        .setUrl(personRef) // full url with ID required if PUT
        .setMethod(pbr.isPersonNew() ? Bundle.HTTPVerb.POST: Bundle.HTTPVerb.PUT);
    
    if (nbr.getRelatedPerson() != null) {
      bundle.addEntry()
          .setFullUrl(FhirUtils.formatResource(nbr.getRelatedPerson()))
          .setResource(nbr.getRelatedPerson())
          .getRequest()
          .setUrl("RelatedPerson")
          .setMethod(Bundle.HTTPVerb.POST);
    }

    bundle.addEntry()
        .setFullUrl(FhirUtils.formatResource(abr.getAnalysis()))
        .setResource(abr.getAnalysis())
        .getRequest()
        .setUrl("ServiceRequest")
        .setMethod(Bundle.HTTPVerb.POST);

    bundle.addEntry()
        .setFullUrl(FhirUtils.formatResource(sbr.getSequencing()))
        .setResource(sbr.getSequencing())
        .getRequest()
        .setUrl("ServiceRequest")
        .setMethod(Bundle.HTTPVerb.POST);

    bundle.addEntry()
        .setFullUrl(FhirUtils.formatResource(cbr.getClinicalImpression()))
        .setResource(cbr.getClinicalImpression())
        .getRequest()
        .setUrl("ClinicalImpression")
        .setMethod(Bundle.HTTPVerb.POST);
    
    obr.getObservations().forEach(o ->
      bundle.addEntry()
          .setFullUrl(FhirUtils.formatResource(o))
          .setResource(o)
          .getRequest()
          .setUrl("Observation")
          .setMethod(Bundle.HTTPVerb.POST));
    
    fhirClient.submitForm(personRef, patientRef, bundle);
  }
  
}
