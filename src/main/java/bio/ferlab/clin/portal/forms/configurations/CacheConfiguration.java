package bio.ferlab.clin.portal.forms.configurations;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@ConfigurationProperties("cache")
@Slf4j
public class CacheConfiguration {
  
  public static final String CACHE_NAME = "default";

  @Scheduled(fixedRateString = "${cache.eviction}")
  @CacheEvict(allEntries = true , cacheNames = CACHE_NAME)
  public void clearCache() {
    log.info("Evict all entries from cache");
  }
  
}
