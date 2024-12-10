package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static bio.ferlab.clin.portal.forms.utils.FhirConst.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ShareBuilderTest {

  final FhirContext fhirContext = FhirContext.forR4();
  final FhirClient fhirClient = Mockito.mock(FhirClient.class);

  @BeforeEach
  void beforeEach() {
    when(fhirClient.getContext()).thenReturn(fhirContext);
  }

  @Test
  void build_no_errors() {
    final var analysis = new ServiceRequest();
    analysis.getMeta().addProfile(ANALYSIS_SERVICE_REQUEST);
    analysis.getMeta().addSecurity().setCode("EP1");  // will be kept
    analysis.getMeta().addSecurity().setCode("PractitionerRole/PRR03"); // will be removed

    when(fhirClient.findServiceRequestById(any())).thenReturn(analysis);

    final var bundle = new Bundle();
    final var pr1 = new PractitionerRole();
    pr1.setId("PRR01");
    pr1.getPractitioner().setReference("Practitioner/PR01");
    pr1.setOrganization(new Reference("Organization/ORG01"));
    final var pr2 = new PractitionerRole();
    pr2.setId("PRR02");
    pr2.getPractitioner().setReference("Practitioner/PR02");
    pr2.setOrganization(new Reference("Organization/ORG02"));
    pr2.getCodeFirstRep().getCodingFirstRep().setSystem(PRACTITIONER_ROLE_GENETICIAN_SYSTEM).setCode(DOCTOR_PREFIX);
    bundle.addEntry().setResource(pr1);
    bundle.addEntry().setResource(pr2);
    when(fhirClient.findAllPractitionerRoles()).thenReturn(bundle);

    when(fhirClient.shareWithRoles(any())).thenReturn(analysis);

    final var builder = new ShareBuilder(fhirClient, "", List.of("PRR02", "PRR02"), "PR01");  // PRR02 duplicated on purpose
    final var result = builder.build();

    assertEquals("[EP1, PractitionerRole/PRR02]", result.getAnalysis().getMeta().getSecurity().stream().map(Coding::getCode).toList().toString());

  }

}
