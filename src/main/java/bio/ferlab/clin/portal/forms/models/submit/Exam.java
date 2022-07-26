package bio.ferlab.clin.portal.forms.models.submit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Exam {
  
  public enum Interpretation {
    abnormal,
    normal
  }
  
  @NotNull
  private String code;
  @NotNull
  private Interpretation interpretation;
  private String value;
  private List<String> values= new ArrayList<>();
  
}
