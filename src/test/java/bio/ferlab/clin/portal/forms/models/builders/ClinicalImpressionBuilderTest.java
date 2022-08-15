package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static bio.ferlab.clin.portal.forms.utils.FhirConst.AGE_AT_EVENT_EXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ClinicalImpressionBuilderTest {
  
  @Test
  void build() {
    final Patient patient = new Patient();
    patient.setId("foo");
    final Person person = new Person();
    person.setBirthDate(Date.from(Instant.now().minus(5, ChronoUnit.DAYS)));
    final Observation ob1 = new Observation();
    ob1.setId("ob1");
    final Observation ob2 = new Observation();
    ob2.setId("ob2");
    final List<Observation> obs = List.of(ob1, ob2);
    final Observation foetusObservation = new Observation();
    foetusObservation.setId("foetusObs");
    
    ClinicalImpressionBuilder.Result result = new ClinicalImpressionBuilder(new SubmitToFhirMapper(), person, patient, obs, foetusObservation)
        .build();
    
    final ClinicalImpression ci = result.getClinicalImpression();
    
    assertNotNull(ci.getId());
    assertEquals("Patient/foo", ci.getSubject().getReference());
    assertEquals(ClinicalImpression.ClinicalImpressionStatus.COMPLETED, ci.getStatus());
    assertEquals(3, ci.getInvestigation().size());
    assertEquals(5, ((Age)ci.getExtensionByUrl(AGE_AT_EVENT_EXT).getValue()).getValue().longValue());
    assertEquals("Observation/ob1", ci.getInvestigation().get(0).getItemFirstRep().getReference());
    assertEquals("Observation/ob2", ci.getInvestigation().get(1).getItemFirstRep().getReference());
    assertEquals("Observation/foetusObs", ci.getInvestigation().get(2).getItemFirstRep().getReference());
  }

}