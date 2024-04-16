package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.Test;

import static bio.ferlab.clin.portal.forms.utils.FhirConst.SEQUENCING_SERVICE_REQUEST;
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
    assertEquals("http://fhir.cqgc.ferlab.bio/CodeSystem/analysis-request-code", sr.getCode().getCodingFirstRep().getSystem());
    assertEquals("75020", sr.getCode().getCoding().get(1).getCode());
    assertEquals("http://fhir.cqgc.ferlab.bio/CodeSystem/sequencing-request-code", sr.getCode().getCoding().get(1).getSystem());
    assertEquals(FhirUtils.formatResource(role), sr.getRequester().getReference());
    assertNotNull(sr.getAuthoredOn());
  }

  @Test
  void build_foetus() {
    final Patient patient = new Patient();
    patient.setId("foo");
    final ServiceRequest analysis = new ServiceRequest();
    analysis.setId("foo");
    final PractitionerRole role = new PractitionerRole();
    role.setId("role");
    final Patient foetus = new Patient();
    foetus.setId("foetus");
    foetus.setDeceased(new BooleanType(false));
    final SequencingBuilder.Result result = new SequencingBuilder(new SubmitToFhirMapper(), "code", patient, analysis, role)
        .withFoetus(foetus)
        .build();
    final ServiceRequest sr = result.getSequencing();

    assertNotNull(sr.getId());
    assertEquals(SEQUENCING_SERVICE_REQUEST, sr.getMeta().getProfile().get(0).getValue());
    assertEquals(ServiceRequest.ServiceRequestIntent.ORDER, sr.getIntent());
    assertEquals(ServiceRequest.ServiceRequestStatus.ONHOLD, sr.getStatus());
    assertEquals(FhirUtils.formatResource(analysis), sr.getBasedOn().get(0).getReference());
    assertEquals("code", sr.getCode().getCodingFirstRep().getCode());
    assertEquals("http://fhir.cqgc.ferlab.bio/CodeSystem/analysis-request-code", sr.getCode().getCodingFirstRep().getSystem());
    assertEquals("75020", sr.getCode().getCoding().get(1).getCode());
    assertEquals("http://fhir.cqgc.ferlab.bio/CodeSystem/sequencing-request-code", sr.getCode().getCoding().get(1).getSystem());
    assertEquals(FhirUtils.formatResource(role), sr.getRequester().getReference());
    assertNotNull(sr.getAuthoredOn());
    assertEquals("Prenatal", sr.getCategoryFirstRep().getCodingFirstRep().getCode());
    assertEquals("Patient/foetus", sr.getSubject().getReference());
    assertEquals(ServiceRequest.ServiceRequestPriority.ASAP, sr.getPriority());
  }

}