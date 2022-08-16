package bio.ferlab.clin.portal.forms.models.submit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoryAndDiag {
  
  @NotBlank
  private String diagnosticHypothesis;
  private String ethnicity;
  private Boolean inbreeding;
  @NotNull
  @Valid
  private List<HealthCondition> healthConditions = new ArrayList<>();
}
