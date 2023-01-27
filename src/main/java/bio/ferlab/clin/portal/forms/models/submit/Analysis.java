package bio.ferlab.clin.portal.forms.models.submit;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Analysis {
  @NotBlank
  private String panelCode;
  private Boolean isReflex;
  private String comment;
  private String residentSupervisor;
}
