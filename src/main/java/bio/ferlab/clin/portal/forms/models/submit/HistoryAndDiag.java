package bio.ferlab.clin.portal.forms.models.submit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
