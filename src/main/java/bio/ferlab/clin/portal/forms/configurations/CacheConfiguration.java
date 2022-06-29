package bio.ferlab.clin.portal.forms.configurations;

import bio.ferlab.clin.portal.forms.controllers.FormController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@ConfigurationProperties("cache")
@Slf4j
public class CacheConfiguration {
  
  public static final String CACHE_NAME = "default";
  
  @Autowired
  private FormController formController;

  @Scheduled(fixedRateString = "${cache.eviction}")
  public void clearCaches() {
    // synchronized the fetch and clear cache methods
    formController.clearCache();
  }
  
}
