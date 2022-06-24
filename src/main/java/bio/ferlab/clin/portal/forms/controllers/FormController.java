package bio.ferlab.clin.portal.forms.controllers;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.models.Config;
import bio.ferlab.clin.portal.forms.models.Form;
import bio.ferlab.clin.portal.forms.models.User;
import bio.ferlab.clin.portal.forms.utils.BundleUtils;
import bio.ferlab.clin.portal.forms.utils.JwtUtils;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.util.BundleUtil;
import io.undertow.util.BadRequestException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/form")
public class FormController {
  
  private final FhirClient fhirClient;
  private final JwtUtils jwtUtils;
  private final BundleUtils bundleUtils;
  
  public FormController(FhirClient fhirClient, JwtUtils jwtUtils, BundleUtils bundleUtils) {
    this.fhirClient = fhirClient;
    this.jwtUtils = jwtUtils;
    this.bundleUtils = bundleUtils;
  }

  @GetMapping("/{type}")
  public Form config(@RequestHeader(required = true) String authorization, 
                       //@RequestParam(required = false) String lang, 
                       @PathVariable String type) throws BadRequestException {
    //final Locale locale = StringUtils.parseLocaleString(lang);
    
    final String practitionerId = jwtUtils.getProperty(authorization, JwtUtils.FHIR_PRACTITIONER_ID)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing '" + JwtUtils.FHIR_PRACTITIONER_ID + "' in token"));
    
    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.BATCH);

    bundle.addEntry().getRequest()
        .setUrl("/PractitionerRole?practitioner="+practitionerId)
        .setMethod(Bundle.HTTPVerb.GET);

    bundle.addEntry().getRequest()
        .setUrl("/Practitioner/"+practitionerId)
        .setMethod(Bundle.HTTPVerb.GET);
    
    Bundle response = fhirClient.getGenericClient().transaction().withBundle(bundle).execute();

    List<PractitionerRole> fhirRoles = BundleUtils.toListOfResourcesOfType(fhirClient.getContext(), response, PractitionerRole.class);
    Practitioner fhirPractitioner = BundleUtils.toResourcesOfType(fhirClient.getContext(), response, Practitioner.class);
    //fhirClient.getClinClient().findPractitionerRole(new TokenParam(practitionerId));
    List<String> roles = fhirRoles.stream().map(r -> r.getCodeFirstRep().getCodingFirstRep().getCode()).distinct().collect(Collectors.toList());
   
    return Form.builder()
        .config(Config.builder()
            .user(User.builder()
                .name(fhirPractitioner.getName().get(0).getNameAsSingleString())
                .roles(roles)
                .build())
            .build())
        .build();
  }

}
