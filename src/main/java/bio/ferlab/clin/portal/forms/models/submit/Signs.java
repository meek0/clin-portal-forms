package bio.ferlab.clin.portal.forms.models.submit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

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
