package bio.ferlab.clin.portal.forms.models.share;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class Response {
  private String analysisId;
  @NotNull
  private List<String> roles = new ArrayList<>();
}
