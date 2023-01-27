package bio.ferlab.clin.portal.forms.models.submit;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthCondition {
  
  @NotBlank
  private String condition;
  @NotBlank
  private String parentalLink;
}
