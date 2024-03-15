package bio.ferlab.clin.portal.forms.models.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Extra {
  private ExtraType type;
  private String label;
  private String unit;
  private boolean required;
  private List<ValueName> options = new ArrayList<>();
}
