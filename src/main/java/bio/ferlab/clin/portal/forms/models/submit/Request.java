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
  private Analyse analyse;
  @Valid
  @NotNull
  private Patient patient;
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
