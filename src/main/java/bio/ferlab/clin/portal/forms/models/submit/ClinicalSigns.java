package bio.ferlab.clin.portal.forms.models.submit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalSigns {

  @Valid
  @NotNull
  private List<Signs> signs = new ArrayList<>();
  private String comment;

  public ClinicalSigns(Parent parent) {
    if (parent != null) {
      this.signs = parent.getSigns();
      this.comment = parent.getComment();
    }
  }

}
