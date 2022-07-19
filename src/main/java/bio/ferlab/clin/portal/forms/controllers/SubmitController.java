package bio.ferlab.clin.portal.forms.controllers;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import bio.ferlab.clin.portal.forms.models.submit.Patient;
import bio.ferlab.clin.portal.forms.models.submit.Request;
import bio.ferlab.clin.portal.forms.services.LocaleService;
import bio.ferlab.clin.portal.forms.utils.BundleExtractor;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/form")
public class SubmitController {
  
  private static final String SYSTEM_RAMQ = "http://terminology.hl7.org/CodeSystem/v2-0203";
  private static final String CODE_RAMQ = "JHN";

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
    
    final Person person = new Person();
    person.addIdentifier().setValue(request.getPatient().getRamq()).setType(new CodeableConcept().addCoding(new Coding().setSystem(SYSTEM_RAMQ).setCode(CODE_RAMQ)));
    person.setBirthDate(mapper.toDate(request.getPatient().getBirthDate()));
    person.setGender( Enumerations.AdministrativeGender.fromCode(request.getPatient().getGender()));
    person.addName().addGiven(request.getPatient().getFirstName()).setFamily(request.getPatient().getLastName());
    // add patient link
    
    boolean error = false;
    MethodOutcome outcome = fhirClient.getGenericClient().validate().resource(person).execute();
    OperationOutcome oo = (OperationOutcome) outcome.getOperationOutcome();
    for (OperationOutcome.OperationOutcomeIssueComponent nextIssue : oo.getIssue()) {
      if (EnumSet.of(OperationOutcome.IssueSeverity.ERROR, OperationOutcome.IssueSeverity.FATAL).contains(nextIssue.getSeverity())) {
        System.out.println(nextIssue.getDiagnostics());
        error = true;
      }
    }

    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.BATCH);

    bundle.addEntry().getRequest()
        .setUrl("Person?identifier=foo")
        .setMethod(Bundle.HTTPVerb.GET);

    Bundle response = fhirClient.getGenericClient().transaction().withBundle(bundle).execute();
    BundleExtractor bundleExtractor = new BundleExtractor(fhirClient.getContext(), response);
    List<Person> ps = bundleExtractor.getNextListOfResourcesOfType(Person.class);

    Optional<Person> existingPs2 = ps.stream().filter(i -> i.getIdentifierFirstRep().getType().getCoding().stream().anyMatch(c -> SYSTEM_RAMQ.equals(c.getSystem()))).findFirst();
    
    
    System.out.println("From bundle " + ps);
    
    if (!error && existingPs2.isEmpty()) {
      try {
        MethodOutcome res2 = fhirClient.getGenericClient().create().resource(person)
            //.conditional()
            //.where(Person.IDENTIFIER.exactly().systemAndIdentifier(SYSTEM_RAMQ, "foo"))
            //.conditionalByUrl("Person?identifier:of-type="+SYSTEM_RAMQ+"%7CJHN%7Cfoo")
            //.prettyPrint()
            //.encodedJson()
            .execute();
      }catch( PreconditionFailedException e1) {
        
      }catch (UnprocessableEntityException e2) {
        
      }
    

    
    }
    
    return ResponseEntity.ok("");
  }

}
