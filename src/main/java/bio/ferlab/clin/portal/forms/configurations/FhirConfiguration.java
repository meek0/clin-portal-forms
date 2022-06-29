package bio.ferlab.clin.portal.forms.configurations;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties("fhir")
@Data
public class FhirConfiguration {
  
  private String url;
  private int timeout;
  private int poolSize;
  private List<String> typesWithDefault = new ArrayList<>();
  
}
