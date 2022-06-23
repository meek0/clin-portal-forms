package bio.ferlab.clin.portal.forms.configurations;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties("security")
@Data
public class SecurityConfiguration {
  
  private boolean enabled;
  private String audience;
  private String issuer;
  private List<String> cors = new ArrayList<>();
  
}
