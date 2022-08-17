package bio.ferlab.clin.portal.forms.models.submit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Exams {
  
  public enum Interpretation {
    abnormal,
    normal,
    not_done
  }
  
  @NotBlank
  private String code;
  @NotNull
  private Interpretation interpretation;
  private String value;
  @NotNull
  private List<String> values = new ArrayList<>();
  
}
