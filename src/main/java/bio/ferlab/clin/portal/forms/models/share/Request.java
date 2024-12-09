package bio.ferlab.clin.portal.forms.models.share;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class Request {
  @NotBlank
  private String analysisId;
  @NotNull
  private List<String> roles = new ArrayList<>();
}
