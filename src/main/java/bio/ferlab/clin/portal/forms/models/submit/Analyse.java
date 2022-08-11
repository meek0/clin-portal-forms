package bio.ferlab.clin.portal.forms.models.submit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Analyse {
  @NotBlank
  private String panelCode;
  private Boolean isReflex;
  @NotBlank
  private String indication;
  private String comment;
  private String residentSupervisor;
}
