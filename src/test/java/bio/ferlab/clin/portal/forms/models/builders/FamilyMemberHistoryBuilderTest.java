package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import bio.ferlab.clin.portal.forms.models.submit.HealthCondition;
import bio.ferlab.clin.portal.forms.models.submit.HistoryAndDiag;
import bio.ferlab.clin.portal.forms.utils.FhirConst;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.hl7.fhir.r4.model.FamilyMemberHistory;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FamilyMemberHistoryBuilderTest {

  final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandom();
  final SubmitToFhirMapper mapper =new SubmitToFhirMapper();
  
  @Test
  void build() {
    final HistoryAndDiag historyAndDiag = new HistoryAndDiag();
    historyAndDiag.setHealthConditions(random.objects(HealthCondition.class, 2).collect(Collectors.toList()));
    final Patient patient = new Patient();
    patient.setId("foo");
    
    final FamilyMemberHistoryBuilder.Result result = new FamilyMemberHistoryBuilder(mapper, historyAndDiag, patient).build();
    
    assertEquals(2, result.getHistories().size());

    final HealthCondition hc1 = historyAndDiag.getHealthConditions().get(0);
    final FamilyMemberHistory h1 = result.getHistories().get(0);
    assertEquals("Patient/foo", h1.getPatient().getReference());
    assertEquals(FhirConst.SYSTEM_RELATIONSHIP, h1.getRelationship().getCodingFirstRep().getSystem());
    assertEquals(hc1.getParentalLink(), h1.getRelationship().getCodingFirstRep().getCode());
    assertEquals(hc1.getCondition(), h1.getNote().get(0).getText());
    assertEquals("completed", h1.getStatus().toCode());

    final HealthCondition hc2 = historyAndDiag.getHealthConditions().get(1);
    final FamilyMemberHistory h2 = result.getHistories().get(1);
    assertEquals("Patient/foo", h2.getPatient().getReference());
    assertEquals(FhirConst.SYSTEM_RELATIONSHIP, h2.getRelationship().getCodingFirstRep().getSystem());
    assertEquals(hc2.getParentalLink(), h2.getRelationship().getCodingFirstRep().getCode());
    assertEquals(hc2.getCondition(), h2.getNote().get(0).getText());
    assertEquals("completed", h2.getStatus().toCode());
  }

}