package bio.ferlab.clin.portal.forms.controllers;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import ca.uhn.fhir.rest.param.TokenParam;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/form")
public class FormController {
  
  @Autowired
  private FhirClient fhirClient;

  @GetMapping("/{formName}")
  public String config(@RequestParam(required = false) String lang, @PathVariable String formName) {
    final Locale locale = StringUtils.parseLocaleString(lang);
    Patient p = fhirClient.getClinClient().findPatientByMrn(new TokenParam(formName));
    return p.getIdBase();
  }

}
