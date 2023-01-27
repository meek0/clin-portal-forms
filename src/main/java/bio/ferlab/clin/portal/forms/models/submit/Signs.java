package bio.ferlab.clin.portal.forms.models.submit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Signs {
  @NotBlank
  private String value;
  @NotNull
  private Boolean isObserved;
  private String ageCode;
}
