package bio.ferlab.clin.portal.forms.models.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValueNameExtra {
  private String name;
  private String value;
  private String tooltip;
  private Extra extra;

  public ValueNameExtra formatWithTooltip() {
    String tooltip = StringUtils.substringBetween(name, "(", ")");
    if (StringUtils.isNotBlank(tooltip)) {
      this.name = StringUtils.remove(name, "("+tooltip+")").trim();
      this.tooltip = StringUtils.capitalize(tooltip).trim();
    }
    return this;
  }
}
