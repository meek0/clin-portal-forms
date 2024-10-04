package bio.ferlab.clin.portal.forms.configurations;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("qlin-me")
@Data
public class QlinMeConfiguration {

  private String url;
  private Boolean enabled;
  private int timeout;
}
