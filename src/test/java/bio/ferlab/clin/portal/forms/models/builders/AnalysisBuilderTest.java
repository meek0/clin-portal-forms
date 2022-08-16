package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static bio.ferlab.clin.portal.forms.utils.FhirConst.ANALYSIS_SERVICE_REQUEST;
import static bio.ferlab.clin.portal.forms.utils.FhirConst.SUPERVISOR_EXT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AnalysisBuilderTest {
  
  final FhirClient fhirClient = Mockito.mock(FhirClient.class);

  @Test
  void withReflex_panel_unknown() {
    final CodeSystem cs = new CodeSystem();
    when(fhirClient.findCodeSystemById(any())).thenReturn(cs);
    
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      new AnalysisBuilder(fhirClient, null, "code", null, null, null, null, null)
          .withReflex(true);
    });
    assertEquals("panel code code is unknown", exception.getReason());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
  }

  @Test
  void build() {
    final Patient patient = new Patient();
    patient.setId("foo");
    
    final ClinicalImpression clinicalImpression = new ClinicalImpression();
    clinicalImpression.setId("foo");
    
    final Practitioner practitioner = new Practitioner();
    practitioner.setId("foo");
    
    final PractitionerRole role = new PractitionerRole();
    role.setId("foo");
    role.setPractitioner(FhirUtils.toReference(practitioner));
    
    final PractitionerRole supervisor = new PractitionerRole();
    supervisor.setId("foo");

    final CodeSystem cs = new CodeSystem();
    cs.addConcept().setCode("code").setDisplay("display");
    when(fhirClient.findCodeSystemById(any())).thenReturn(cs);
    
    final AnalysisBuilder.Result result = new AnalysisBuilder(fhirClient, new SubmitToFhirMapper(), "code", patient, clinicalImpression, role, supervisor, "comment")
        .withFoetus(new Patient())
        .withReflex(true)
        .build();
    final ServiceRequest sr = result.getAnalysis();

    assertNotNull(sr.getId());
    assertEquals(ANALYSIS_SERVICE_REQUEST, sr.getMeta().getProfile().get(0).getValue());
    assertEquals(ServiceRequest.ServiceRequestIntent.ORDER, sr.getIntent());
    assertEquals(FhirUtils.formatResource(patient), sr.getSubject().getReference());
    assertEquals(ServiceRequest.ServiceRequestStatus.ONHOLD, sr.getStatus());
    assertEquals(FhirUtils.formatResource(clinicalImpression), sr.getSupportingInfoFirstRep().getReference());
    assertEquals("code", sr.getCode().getCodingFirstRep().getCode());
    assertNotNull(sr.getAuthoredOn());
    assertEquals(FhirUtils.formatResource(role), sr.getRequester().getReference());
    assertEquals(FhirUtils.formatResource(supervisor), ((Reference)sr.getExtensionByUrl(SUPERVISOR_EXT).getValue()).getReference());
    assertEquals("Reflex Panel: display (code)", sr.getOrderDetailFirstRep().getText());
    final Annotation note = sr.getNoteFirstRep();
    assertNotNull(note.getTime());
    assertEquals("comment", note.getText());
    assertEquals(FhirUtils.formatResource(practitioner), ((Reference)note.getAuthor()).getReference());
    assertEquals("Prenatal", sr.getCategoryFirstRep().getText());
  }
}