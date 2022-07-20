package bio.ferlab.clin.portal.forms.controllers;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import bio.ferlab.clin.portal.forms.models.config.Form;

import bio.ferlab.clin.portal.forms.models.submit.Request;
import bio.ferlab.clin.portal.forms.services.LocaleService;
import bio.ferlab.clin.portal.forms.utils.BundleExtractor;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static bio.ferlab.clin.portal.forms.utils.FhirConstants.*;

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
    1- create/get Person
    2- create/get Patient
    3- create servicerequest analysis
    4- create servicerequest sequencing
    5 -create clinicalimpressoin 
    6 create obsetrvations X 
     */
    
    this.handlePatient(request.getPatient());
    
    return ResponseEntity.noContent().build();
  }
  
  private void handlePatient(bio.ferlab.clin.portal.forms.models.submit.Patient patient) {
    var pairs = findByRamqOrMrn(patient.getEp(), patient.getRamq(), patient.getMrn());
    this.createOrUpdate(patient, pairs.getLeft(), pairs.getRight());
  }
  
  private Pair<Optional<Person>, Optional<Patient>> findByRamqOrMrn(String ep, String ramq, String mrn) {
    if(StringUtils.isNotBlank(ramq)) {
      Bundle bundle = this.fhirClient.getGenericClient().search()
          .forResource(Person.class)
          .where(Person.IDENTIFIER.exactly().code(ramq))
          .include(Person.INCLUDE_PATIENT)
          .returnBundle(Bundle.class)
          .execute();

      BundleExtractor bundleExtractor = new BundleExtractor(fhirClient.getContext(), bundle);
      final Person person = bundleExtractor.getNextResourcesOfType(Person.class);
      final List<Patient> patients = bundleExtractor.getAllResourcesOfType(Patient.class);
      final Optional<Patient> patient = patients.stream().filter(p -> p.getManagingOrganization()
          .getReference().equals("Organization/"+ep)).findFirst();
      
      return Pair.of(Optional.ofNullable(person), patient);
    } else if(StringUtils.isNotBlank(mrn)) {
      Bundle bundle = this.fhirClient.getGenericClient().search()
          .forResource(Patient.class)
          .where(Patient.IDENTIFIER.exactly().code(mrn))
          .and(Patient.ORGANIZATION.hasId(ep))
          .revInclude(Person.INCLUDE_PATIENT)
          .returnBundle(Bundle.class)
          .execute();

      BundleExtractor bundleExtractor = new BundleExtractor(fhirClient.getContext(), bundle);
      final Patient patient = bundleExtractor.getNextResourcesOfType(Patient.class);
      final Person person = bundleExtractor.getNextResourcesOfType(Person.class);

      return Pair.of(Optional.ofNullable(person), Optional.ofNullable(patient));
    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patient.ramq and patient.mrn can't be both null");
    }
  }
  
  private void createOrUpdate(bio.ferlab.clin.portal.forms.models.submit.Patient patient, Optional<Person> existingPerson, Optional<Patient> existingPatient) {
    // update existing patient
    existingPatient.ifPresent(p -> mapper.updatePatient(patient, p));
    // keep existing or create new patient
    final Patient newOrUpdatedPatient = existingPatient.orElse(mapper.mapToPatient(patient));
    // update existing person
    existingPerson.ifPresent(p -> mapper.updatePerson(patient, p, newOrUpdatedPatient));
    // keep existing or create new person
    final Person newOrUpdatedPerson = existingPerson.orElse(mapper.mapToPerson(patient, newOrUpdatedPatient));

    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.TRANSACTION);

    bundle.addEntry()
        .setFullUrl("Patient/"+newOrUpdatedPatient.getIdElement().getIdPart())
        .setResource(newOrUpdatedPatient)
        .getRequest()
        .setUrl("Patient/"+newOrUpdatedPatient.getIdElement().getIdPart())
        .setMethod(existingPatient.isPresent() ? Bundle.HTTPVerb.PUT: Bundle.HTTPVerb.POST);

    bundle.addEntry()
        .setFullUrl("Person/"+newOrUpdatedPerson.getIdElement().getIdPart())
        .setResource(newOrUpdatedPerson)
        .getRequest()
        .setUrl("Person/"+newOrUpdatedPerson.getIdElement().getIdPart())
        .setMethod(existingPerson.isPresent() ? Bundle.HTTPVerb.PUT: Bundle.HTTPVerb.POST);
    
    log.debug(bundle.getEntry().get(0).getRequest().getMethod() + " " + bundle.getEntry().get(0).getFullUrl());
    log.debug(bundle.getEntry().get(1).getRequest().getMethod() + " " + bundle.getEntry().get(1).getFullUrl());

    log.debug("\n" + fhirClient.getContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle));
    
    List<String> bundleErrors = this.validateResource(bundle);
    if(!bundleErrors.isEmpty()) {
      throw new RuntimeException("Failed to validate form bundle:\n" + StringUtils.join(bundleErrors, "\n"));
    }
    
    Bundle response = this.fhirClient.getGenericClient().transaction().withBundle(bundle).execute();
    log.debug("\n" + fhirClient.getContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(response));
  }
  
  private List<String> validateResource(Resource resource) {
    List<String> errors = new ArrayList<>();
    MethodOutcome outcome = fhirClient.getGenericClient().validate().resource(resource).execute();
    OperationOutcome oo = (OperationOutcome) outcome.getOperationOutcome();
    for (OperationOutcome.OperationOutcomeIssueComponent nextIssue : oo.getIssue()) {
      if (EnumSet.of(OperationOutcome.IssueSeverity.ERROR, OperationOutcome.IssueSeverity.FATAL).contains(nextIssue.getSeverity())) {
        errors.add(nextIssue.getDiagnostics());
      }
    }
    return errors;
  }

}
