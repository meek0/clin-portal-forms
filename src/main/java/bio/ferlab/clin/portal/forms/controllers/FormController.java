package bio.ferlab.clin.portal.forms.controllers;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.models.Config;
import bio.ferlab.clin.portal.forms.models.Form;
import bio.ferlab.clin.portal.forms.models.Role;
import bio.ferlab.clin.portal.forms.models.User;
import bio.ferlab.clin.portal.forms.utils.JwtUtils;
import ca.uhn.fhir.rest.param.TokenParam;
import io.undertow.util.BadRequestException;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/form")
public class FormController {
  
  private final FhirClient fhirClient;
  private final JwtUtils jwtUtils;
  
  public FormController(FhirClient fhirClient, JwtUtils jwtUtils) {
    this.fhirClient = fhirClient;
    this.jwtUtils = jwtUtils;
  }

  @GetMapping("/{type}")
  public Form config(@RequestHeader(required = true) String authorization, 
                       //@RequestParam(required = false) String lang, 
                       @PathVariable String type) throws BadRequestException {
    //final Locale locale = StringUtils.parseLocaleString(lang);
    
    final String practitionerId = jwtUtils.getProperty(authorization, JwtUtils.FHIR_PRACTITIONER_ID)
        .orElseThrow(() -> new BadRequestException("Missing '" + JwtUtils.FHIR_PRACTITIONER_ID + "' in token"));
    
    List<PractitionerRole> fhirRoles = fhirClient.getClinClient().findPractitionerRole(new TokenParam(practitionerId));
    
    List<Role> roles = fhirRoles.stream().map(r -> new Role(r.getOrganization().getReference(), r.getCodeFirstRep().getCodingFirstRep().getCode())).collect(Collectors.toList());
   
    return Form.builder()
        .config(Config.builder()
            .user(User.builder()
                .roles(roles)
                .build())
            .build())
        .build();
  }

}
