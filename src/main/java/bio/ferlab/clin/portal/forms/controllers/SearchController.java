package bio.ferlab.clin.portal.forms.controllers;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.mappers.FhirToSearchMapper;
import bio.ferlab.clin.portal.forms.models.builders.PatientBuilder;
import bio.ferlab.clin.portal.forms.models.builders.PractitionerBuilder;
import bio.ferlab.clin.portal.forms.models.builders.SearchPrescriptionBuilder;
import bio.ferlab.clin.portal.forms.models.search.SearchPatient;
import bio.ferlab.clin.portal.forms.models.search.SearchPrescription;
import bio.ferlab.clin.portal.forms.models.submit.Patient;
import bio.ferlab.clin.portal.forms.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {
  
  private final FhirClient fhirClient;
  private final FhirToSearchMapper mapper;
  
  @GetMapping("/patient/{ep}")
  public SearchPatient patient(@PathVariable String ep,
                               @RequestHeader String authorization,
                               @RequestParam(required = false) String ramq,
                               @RequestParam(required = false) String mrn){

    PractitionerBuilder.validateAccessToEp(fhirClient, authorization, ep);
    
    final Patient patient = new Patient();
    patient.setRamq(ramq);
    patient.setMrn(mrn);
    patient.setEp(ep);
    
    final PatientBuilder.Result result = new PatientBuilder(fhirClient, null, patient)
        .validateRamqAndMrn()
        .findByMrn()
        .findByRamq()
        .build(false, false);
    
    return mapper.mapToSearch(result.getPerson(), result.getPatient());
  }

  @GetMapping("/prescription")
  public List<SearchPrescription> prescription(@RequestHeader String authorization,
                                         @RequestParam(required = false) String id,
                                         @RequestParam(required = false) String ramq){

    final String practitionerId = JwtUtils.getProperty(authorization, JwtUtils.FHIR_PRACTITIONER_ID);

    final SearchPrescriptionBuilder.Result result = new SearchPrescriptionBuilder(fhirClient, mapper, practitionerId, id, ramq)
      .validate()
      .build();
    
    return result.getPrescriptions();
  }
}
