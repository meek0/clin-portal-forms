package bio.ferlab.clin.portal.forms.controllers;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.mappers.FhirToSearchMapper;
import bio.ferlab.clin.portal.forms.models.builders.PatientBuilder;
import bio.ferlab.clin.portal.forms.models.search.Search;
import bio.ferlab.clin.portal.forms.models.submit.Patient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {
  
  private final FhirClient fhirClient;
  private final FhirToSearchMapper mapper;
  
  @GetMapping("/patient")
  public Search search(@RequestParam(required = false) String ramq,
                       @RequestParam(required = false) String mrn,
                       @RequestParam String ep){
    final Patient patient = new Patient();
    patient.setRamq(ramq);
    patient.setMrn(mrn);
    patient.setEp(ep);
    final PatientBuilder.Result result = new PatientBuilder(fhirClient, null, patient)
        .validateRamqAndMrn()
        .validateEp()
        .findByMrn()
        .findByRamq()
        .build(false, false);
    return mapper.mapToSearch(result.getPerson(), result.getPatient());
  }
}
