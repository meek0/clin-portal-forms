package bio.ferlab.clin.portal.forms.controllers;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.models.autocomplete.Supervisor;
import bio.ferlab.clin.portal.forms.models.builders.AutocompleteBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/autocomplete")
@RequiredArgsConstructor
public class AutocompleteController {
  
  private final FhirClient fhirClient;
  
  @GetMapping("/supervisor/{ep}/{prefix}")
  public List<Supervisor> autocomplete(@PathVariable String ep, @PathVariable String prefix) {
    return new AutocompleteBuilder(fhirClient, ep)
        .validateEp()
        .withSupervisor(prefix)
        .build().getSupervisors();
  }

}
