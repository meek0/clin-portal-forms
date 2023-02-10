package bio.ferlab.clin.portal.forms.controllers;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import bio.ferlab.clin.portal.forms.models.builders.*;
import bio.ferlab.clin.portal.forms.models.submit.ClinicalSigns;
import bio.ferlab.clin.portal.forms.models.submit.Request;
import bio.ferlab.clin.portal.forms.models.submit.Response;
import bio.ferlab.clin.portal.forms.services.LocaleService;
import bio.ferlab.clin.portal.forms.utils.BundleExtractor;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import bio.ferlab.clin.portal.forms.utils.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/form")
@RequiredArgsConstructor
public class SubmitController {

  private final FhirClient fhirClient;
  private final SubmitToFhirMapper mapper;
  private final LocaleService localeService;

  @PostMapping
  public ResponseEntity<Response> submit(@RequestHeader String authorization,
                                         @Valid @RequestBody Request request,
                                         @RequestParam(required = false, name = "lang") String queryLang) {

    final String practitionerId = JwtUtils.getProperty(authorization, JwtUtils.FHIR_PRACTITIONER_ID);
    final String panelCode = request.getAnalysis().getPanelCode();
    final String ep = request.getPatient().getEp();
    final String lang = localeService.getLocale(queryLang);
 
    final PatientBuilder patientBuilder = new PatientBuilder(fhirClient, mapper, request.getPatient());
    PatientBuilder.Result pbr = patientBuilder
        .validateRamqAndMrn()
        .findByRamq()
        .findByMrn()
        .build(true, true);
    
    final ParentBuilder motherBuilder = new ParentBuilder(fhirClient, mapper, request.getMother());
    ParentBuilder.Result motherResult = motherBuilder.build();

    final ParentBuilder fatherBuilder = new ParentBuilder(fhirClient, mapper, request.getFather());
    ParentBuilder.Result fatherResult = fatherBuilder.build();
    
    final NewBornBuilder newBornBuilder = new NewBornBuilder(mapper, request.getPatient().getAdditionalInfo(), pbr.getPatient());
    NewBornBuilder.Result nbr = newBornBuilder
        .build();
    
    final FoetusBuilder foetusBuilder = new FoetusBuilder(mapper, request.getPatient().getAdditionalInfo(), pbr.getPatient());
    FoetusBuilder.Result fbr = foetusBuilder
        .build();
    
    final PractitionerBuilder practitionerBuilder = new PractitionerBuilder(fhirClient, practitionerId);
    PractitionerBuilder.Result roleBr = practitionerBuilder
        .withSupervisor(request.getAnalysis().getResidentSupervisor(), ep)
        .withEp(ep)
        .build();
    
    final FamilyMemberHistoryBuilder familyMemberHistoryBuilder = new FamilyMemberHistoryBuilder(mapper, request.getHistoryAndDiagnosis(), pbr.getPatient());
    FamilyMemberHistoryBuilder.Result fmhr = familyMemberHistoryBuilder.build();
    
    final ObservationsBuilder observationsBuilder = new ObservationsBuilder(mapper, panelCode, pbr.getPatient(), request.getHistoryAndDiagnosis(),
        request.getClinicalSigns(), request.getParaclinicalExams());
    ObservationsBuilder.Result obr = observationsBuilder
        .withFoetus(fbr.getObservation())
        .withMother(request.getMother())
        .withFather(request.getFather())
        .validate()
        .build();

    final ObservationsBuilder observationsMotherBuilder = new ObservationsBuilder(mapper, panelCode, motherResult.getPatient(),
      null, new ClinicalSigns(request.getMother()), null);
    ObservationsBuilder.Result obmr = observationsMotherBuilder
      .withAffected(request.getMother().getParentClinicalStatus())
      .validate()
      .build();

    final ObservationsBuilder observationsFatherBuilder = new ObservationsBuilder(mapper, panelCode, fatherResult.getPatient(),
      null, new ClinicalSigns(request.getFather()), null);
    ObservationsBuilder.Result obfr = observationsFatherBuilder
      .withAffected(request.getFather().getParentClinicalStatus())
      .validate()
      .build();

    final ClinicalImpressionBuilder clinicalImpressionBuilder = new ClinicalImpressionBuilder(mapper, 
        pbr.getPerson(), pbr.getPatient(), obr.getObservations(), fmhr.getHistories());
    ClinicalImpressionBuilder.Result cbr = clinicalImpressionBuilder
        .build();

    final ClinicalImpressionBuilder clinicalImpressionMotherBuilder = new ClinicalImpressionBuilder(mapper,
      motherResult.getPerson(), motherResult.getPatient(), obmr.getObservations(), List.of());
    ClinicalImpressionBuilder.Result cbmr = clinicalImpressionMotherBuilder.build();

    final ClinicalImpressionBuilder clinicalImpressionFatherBuilder = new ClinicalImpressionBuilder(mapper,
      fatherResult.getPerson(), fatherResult.getPatient(), obfr.getObservations(), List.of());
    ClinicalImpressionBuilder.Result cbfr = clinicalImpressionFatherBuilder.build();

    final ReflexBuilder reflexBuilder = new ReflexBuilder(lang, request.getAnalysis().getIsReflex());
    ReflexBuilder.Result rbr = reflexBuilder.build();
    
    final AnalysisBuilder analysisBuilder = new AnalysisBuilder(mapper, panelCode, pbr.getPatient(),
        cbr.getClinicalImpression(), roleBr.getPractitionerRole(), roleBr.getSupervisorRole(), request.getAnalysis().getComment());
    AnalysisBuilder.Result abr = analysisBuilder
        .withFoetus(fbr.getFoetus())
        .withMother(cbmr.getClinicalImpression())
        .withFather(cbfr.getClinicalImpression())
        .withReflex(rbr.getReflex())
        .build();

    final SequencingBuilder sequencingBuilder = new SequencingBuilder(mapper, panelCode, 
        pbr.getPatient(), abr.getAnalysis(), roleBr.getPractitionerRole());
    SequencingBuilder.Result sbr = sequencingBuilder
        .withFoetus(fbr.getFoetus())
        .build();

    final SequencingBuilder sequencingMotherBuilder = new SequencingBuilder(mapper, panelCode, motherResult.getPatient(), abr.getAnalysis(), roleBr.getPractitionerRole());
    SequencingBuilder.Result sbmr = sequencingMotherBuilder
      .build();

    final SequencingBuilder sequencingFatherBuilder = new SequencingBuilder(mapper, panelCode, fatherResult.getPatient(), abr.getAnalysis(), roleBr.getPractitionerRole());
    SequencingBuilder.Result sbfr = sequencingFatherBuilder
      .build();
    
    final Response res = new Response(submit(pbr, motherResult, fatherResult, nbr, fbr, abr, sbr, sbmr, sbfr, cbr, cbmr, cbfr, obr, obmr, obfr, fmhr));
    
    return ResponseEntity.ok(res);
  }
  
  private String submit(PatientBuilder.Result pbr, 
                      ParentBuilder.Result motherResult,
                      ParentBuilder.Result fatherResult,
                      NewBornBuilder.Result nbr,
                      FoetusBuilder.Result fbr,
                      AnalysisBuilder.Result abr, 
                      SequencingBuilder.Result sbr,
                      SequencingBuilder.Result sbmr,
                      SequencingBuilder.Result sbfr,
                      ClinicalImpressionBuilder.Result cbr,
                      ClinicalImpressionBuilder.Result cbmr,
                      ClinicalImpressionBuilder.Result cbfr,
                      ObservationsBuilder.Result obr,
                      ObservationsBuilder.Result obmr,
                      ObservationsBuilder.Result obfr,
                      FamilyMemberHistoryBuilder.Result fmhr) {
    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.TRANSACTION);

    final String patientRef = FhirUtils.formatResource(pbr.getPatient());
    final String personRef = FhirUtils.formatResource(pbr.getPerson());
    
    this.addPatientToBundle(bundle, pbr);
    
    if (motherResult.getPatientResult() != null) {
      this.addPatientToBundle(bundle, motherResult.getPatientResult());
    }

    if (fatherResult.getPatientResult() != null) {
      this.addPatientToBundle(bundle, fatherResult.getPatientResult());
    }

    // First ServiceRequest is always the Patient
    // we extract ID from submit response
    bundle.addEntry()
        .setFullUrl(FhirUtils.formatResource(abr.getAnalysis()))
        .setResource(abr.getAnalysis())
        .getRequest()
        .setUrl("ServiceRequest")
        .setMethod(Bundle.HTTPVerb.POST);
    
    if (nbr.getRelatedPerson() != null) {
      bundle.addEntry()
          .setFullUrl(FhirUtils.formatResource(nbr.getRelatedPerson()))
          .setResource(nbr.getRelatedPerson())
          .getRequest()
          .setUrl("RelatedPerson")
          .setMethod(Bundle.HTTPVerb.POST);
    }
    
    if (fbr.getFoetus() != null) {
      bundle.addEntry()
          .setFullUrl(FhirUtils.formatResource(fbr.getFoetus()))
          .setResource(fbr.getFoetus())
          .getRequest()
          .setUrl("Patient")
          .setMethod(Bundle.HTTPVerb.POST);
    }

    bundle.addEntry()
        .setFullUrl(FhirUtils.formatResource(sbr.getSequencing()))
        .setResource(sbr.getSequencing())
        .getRequest()
        .setUrl("ServiceRequest")
        .setMethod(Bundle.HTTPVerb.POST);

    if (sbmr.getSequencing() != null) {
      bundle.addEntry()
        .setFullUrl(FhirUtils.formatResource(sbmr.getSequencing()))
        .setResource(sbmr.getSequencing())
        .getRequest()
        .setUrl("ServiceRequest")
        .setMethod(Bundle.HTTPVerb.POST);
    }

    if (sbfr.getSequencing() != null) {
      bundle.addEntry()
        .setFullUrl(FhirUtils.formatResource(sbfr.getSequencing()))
        .setResource(sbfr.getSequencing())
        .getRequest()
        .setUrl("ServiceRequest")
        .setMethod(Bundle.HTTPVerb.POST);
    }

    bundle.addEntry()
        .setFullUrl(FhirUtils.formatResource(cbr.getClinicalImpression()))
        .setResource(cbr.getClinicalImpression())
        .getRequest()
        .setUrl("ClinicalImpression")
        .setMethod(Bundle.HTTPVerb.POST);

    if (cbmr.getClinicalImpression() != null) {
      bundle.addEntry()
        .setFullUrl(FhirUtils.formatResource(cbmr.getClinicalImpression()))
        .setResource(cbmr.getClinicalImpression())
        .getRequest()
        .setUrl("ClinicalImpression")
        .setMethod(Bundle.HTTPVerb.POST);
    }

    if (cbfr.getClinicalImpression() != null) {
      bundle.addEntry()
        .setFullUrl(FhirUtils.formatResource(cbfr.getClinicalImpression()))
        .setResource(cbfr.getClinicalImpression())
        .getRequest()
        .setUrl("ClinicalImpression")
        .setMethod(Bundle.HTTPVerb.POST);
    }
    
    obr.getObservations().forEach(o ->
      bundle.addEntry()
          .setFullUrl(FhirUtils.formatResource(o))
          .setResource(o)
          .getRequest()
          .setUrl("Observation")
          .setMethod(Bundle.HTTPVerb.POST));

    obmr.getObservations().forEach(o ->
      bundle.addEntry()
        .setFullUrl(FhirUtils.formatResource(o))
        .setResource(o)
        .getRequest()
        .setUrl("Observation")
        .setMethod(Bundle.HTTPVerb.POST));

    obfr.getObservations().forEach(o ->
      bundle.addEntry()
        .setFullUrl(FhirUtils.formatResource(o))
        .setResource(o)
        .getRequest()
        .setUrl("Observation")
        .setMethod(Bundle.HTTPVerb.POST));
    
    fmhr.getHistories().forEach(h ->  
      bundle.addEntry()
        .setFullUrl(FhirUtils.formatResource(h))
        .setResource(h)
        .getRequest()
        .setUrl("FamilyMemberHistory")
        .setMethod(Bundle.HTTPVerb.POST));
    
   final Bundle response = fhirClient.submitForm(personRef, patientRef, bundle);
   return new BundleExtractor(fhirClient.getContext(), response).extractFirstIdFromResponse("ServiceRequest");
  }
  
  private void addPatientToBundle(Bundle bundle, PatientBuilder.Result pbr) {
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
  }
  
}
