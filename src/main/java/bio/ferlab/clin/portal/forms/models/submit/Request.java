package bio.ferlab.clin.portal.forms.models.submit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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
  @NotNull
  private ClinicalSigns clinicalSigns;
  @Valid
  @NotNull
  private ParaclinicalExams paraclinicalExams;
  @Valid
  @NotNull
  private HistoryAndDiag historyAndDiagnosis;
}
