package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.Test;

import static bio.ferlab.clin.portal.forms.utils.FhirConstants.SEQUENCING_SERVICE_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SequencingBuilderTest {
  
  @Test
  void build() {
    final Patient patient = new Patient();
    patient.setId("foo");
    final ServiceRequest analysis = new ServiceRequest();
    analysis.setId("foo");
    final PractitionerRole role = new PractitionerRole();
    role.setId("role");
    final SequencingBuilder.Result result = new SequencingBuilder(new SubmitToFhirMapper(), "code", patient, analysis, role).build();
    final ServiceRequest sr = result.getSequencing();
    
    assertNotNull(sr.getId());
    assertEquals(SEQUENCING_SERVICE_REQUEST, sr.getMeta().getProfile().get(0).getValue());
    assertEquals(ServiceRequest.ServiceRequestIntent.ORDER, sr.getIntent());
    assertEquals(FhirUtils.formatResource(patient), sr.getSubject().getReference());
    assertEquals(ServiceRequest.ServiceRequestStatus.ONHOLD, sr.getStatus());
    assertEquals(FhirUtils.formatResource(analysis), sr.getBasedOn().get(0).getReference());
    assertEquals("code", sr.getCode().getCodingFirstRep().getCode());
    assertEquals(FhirUtils.formatResource(role), sr.getRequester().getReference());
    assertNotNull(sr.getAuthoredOn());
  }

}