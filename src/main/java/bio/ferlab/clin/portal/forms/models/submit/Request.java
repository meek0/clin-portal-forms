package bio.ferlab.clin.portal.forms.models.submit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Request {
  @Valid
  @NotNull
  private Analysis analysis;
  @Valid
  @NotNull
  private Patient patient;
  @Valid
  private Parent mother;
  @Valid
  private Parent father;
  @Valid
  private ClinicalSigns clinicalSigns;
  @Valid
  private ParaclinicalExams paraclinicalExams;
  @Valid
  private HistoryAndDiag historyAndDiagnosis;
}
