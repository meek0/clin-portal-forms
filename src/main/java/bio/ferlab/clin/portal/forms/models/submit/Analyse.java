package bio.ferlab.clin.portal.forms.models.submit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Analyse {
  @NotNull
  private String panelCode;
  private Boolean isReflex;
  private String observation;
  private String investigation;
  @NotNull
  private String indication;
  private String comment;
  private String residentSupervisor;
}
