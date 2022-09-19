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
    final FamilyMemberHistory fm1 = new FamilyMemberHistory();
    fm1.setId("fm1");
    final List<FamilyMemberHistory> histories = List.of(fm1);
    
    ClinicalImpressionBuilder.Result result = new ClinicalImpressionBuilder(new SubmitToFhirMapper(), person, patient, obs, histories)
        .build();
    
    final ClinicalImpression ci = result.getClinicalImpression();
    
    assertNotNull(ci.getId());
    assertEquals("Patient/foo", ci.getSubject().getReference());
    assertEquals(ClinicalImpression.ClinicalImpressionStatus.COMPLETED, ci.getStatus());
    assertEquals(3, ci.getInvestigationFirstRep().getItem().size());
    assertEquals(5, ((Age)ci.getExtensionByUrl(AGE_AT_EVENT_EXT).getValue()).getValue().longValue());
    assertEquals("Examination / signs", ci.getInvestigationFirstRep().getCode().getText());
    assertEquals("Observation/ob1", ci.getInvestigationFirstRep().getItem().get(0).getReference());
    assertEquals("Observation/ob2", ci.getInvestigationFirstRep().getItem().get(1).getReference());
    assertEquals("FamilyMemberHistory/fm1", ci.getInvestigationFirstRep().getItem().get(2).getReference());
  }

}