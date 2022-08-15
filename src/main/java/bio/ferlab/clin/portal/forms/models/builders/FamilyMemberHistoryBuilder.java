package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import bio.ferlab.clin.portal.forms.models.submit.HistoryAndDiag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.FamilyMemberHistory;
import org.hl7.fhir.r4.model.Patient;

import java.util.List;

@RequiredArgsConstructor
public class FamilyMemberHistoryBuilder {
  
  private final SubmitToFhirMapper mapper;
  private final HistoryAndDiag historyAndDiag;
  private final Patient patient;
  
  public Result build() {
    final List<FamilyMemberHistory> histories = this.mapper.mapToFamilyMemberHistory(historyAndDiag, patient);
    return new Result(histories);
  }
  
  @Getter
  @AllArgsConstructor
  public static class Result {
    private final List<FamilyMemberHistory> histories;
  }
}
