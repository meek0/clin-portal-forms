package bio.ferlab.clin.portal.forms.configurations;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.TimeUnit;

@Configuration
@ConfigurationProperties("cache")
@Slf4j
public class CacheConfiguration {
  
  public static final String CACHE_CODES_VALUES = "CACHE_CODES_VALUES";
  public static final String CACHE_ROLES = "CACHE_ROLES";

  @Scheduled(fixedRateString = "${cache.eviction}", timeUnit = TimeUnit.SECONDS)
  @CacheEvict(value = CACHE_CODES_VALUES, allEntries = true)
  public void evictCodesValues() {
    log.debug("Evict codes and values from cache");
  }

  @Scheduled(fixedRateString = "${cache.eviction}", timeUnit = TimeUnit.SECONDS)
  @CacheEvict(value = CACHE_ROLES, allEntries = true)
  public void evictRoles() {
    log.debug("Evict practitioner roles from cache");
  }
  
}
