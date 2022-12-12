package bio.ferlab.clin.portal.forms.configurations;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties("fhir")
@Data
public class FhirConfiguration {
  
  private String url;
  private int timeout;
  private int poolSize;
  private boolean validate;
  private List<String> supportedLangs = new ArrayList<>();
  private List<String> typesWithDefault = new ArrayList<>();
  private Map<String, String> sameTypes = new HashMap<>();
  private List<String> multiValuesObservationCodes = new ArrayList<>();
  
}
