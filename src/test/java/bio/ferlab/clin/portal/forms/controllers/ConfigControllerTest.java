package bio.ferlab.clin.portal.forms.controllers;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.models.builders.FoetusBuilder;
import bio.ferlab.clin.portal.forms.models.config.Form;
import bio.ferlab.clin.portal.forms.services.LogOnceService;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import bio.ferlab.clin.portal.forms.utils.JwtUtils;
import ca.uhn.fhir.context.FhirContext;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class ConfigControllerTest {

  final FhirContext fhirContext = FhirContext.forR4();
  final Algorithm algorithm = Algorithm.HMAC256("secret");
  final String token = JWT.create().withClaim(JwtUtils.FHIR_PRACTITIONER_ID, "practitionerId").sign(algorithm);

  @MockBean
  private FhirClient fhirClient;

  @MockBean
  private LogOnceService logOnceService;

  @Autowired
  private ConfigController controller;

  @BeforeEach
  void beforeEach() {
    when(fhirClient.getContext()).thenReturn(fhirContext);
    when(fhirClient.findPractitionerRoleByPractitionerId(any())).thenReturn(buildOrganizationsAndRoles());
    when(fhirClient.fetchCodesAndValues()).thenReturn(buildCodesAndValues());
  }

  @Test
  void mmg() {
    Form form = controller.config("Bearer " + token, "MMG");
    assertEquals("org1", form.getConfig().getPrescribingInstitutions().get(0).getValue());
    // validate sort by name
    assertEquals("HP:A", form.getConfig().getClinicalSigns().getDefaultList().get(0).getValue());
    assertEquals("HP:X", form.getConfig().getClinicalSigns().getDefaultList().get(1).getValue());
    // validate missing code
    verify(logOnceService, atLeast(1)).warn("Missing CodeSystem for code: HP:X");
  }

  @Test
  void unsupported() {
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      controller.config("Bearer " + token, "foo");
    });
    assertEquals("unsupported form panel code: foo available codes: [MMG]", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
  }

  private Bundle buildOrganizationsAndRoles() {
    Bundle bundle = new Bundle();

    Organization org1 = new Organization();
    org1.setId("org1");

    PractitionerRole pr1 = new PractitionerRole();
    pr1.setId("pr1");
    pr1.setOrganization(FhirUtils.toReference(org1));

    bundle.addEntry().setResource(pr1);
    return bundle;
  }

  private Bundle buildCodesAndValues() {
    Bundle bundle = new Bundle();

    CodeSystem panels = new CodeSystem();
    panels.getConcept().add(new CodeSystem.ConceptDefinitionComponent(new CodeType("MMG")));
    bundle.addEntry().setResource(panels);

    CodeSystem hp = new CodeSystem();
    hp.getConcept().add(new CodeSystem.ConceptDefinitionComponent(new CodeType("HP:A")).setDisplay("Display HP:A"));
    hp.getConcept().add(new CodeSystem.ConceptDefinitionComponent(new CodeType("HP:B")).setDisplay("Display HP:B"));
    hp.getConcept().add(new CodeSystem.ConceptDefinitionComponent(new CodeType("HP:C")).setDisplay("Display HP:C"));
    bundle.addEntry().setResource(hp);

    bundle.addEntry().setResource(new CodeSystem());
    bundle.addEntry().setResource(new CodeSystem());
    bundle.addEntry().setResource(new CodeSystem());
    bundle.addEntry().setResource(new ValueSet());

    ValueSet hpMMG = new ValueSet();
    hpMMG.setName("mmg-default-hpo");
    hpMMG.getCompose().getIncludeFirstRep().addConcept(new ValueSet.ConceptReferenceComponent(new CodeType("HP:X")).setDisplay("Display HP:X"));
    hpMMG.getCompose().getIncludeFirstRep().addConcept(new ValueSet.ConceptReferenceComponent(new CodeType("HP:A")).setDisplay("Display HP:A"));
    bundle.addEntry().setResource(hpMMG);

    bundle.addEntry().setResource(new ValueSet());
    bundle.addEntry().setResource(new ValueSet());
    bundle.addEntry().setResource(new ValueSet());
    bundle.addEntry().setResource(new ValueSet());
    bundle.addEntry().setResource(new ValueSet());

    bundle.addEntry().setResource(new ValueSet());
    bundle.addEntry().setResource(new ValueSet());

    return bundle;
  }

}