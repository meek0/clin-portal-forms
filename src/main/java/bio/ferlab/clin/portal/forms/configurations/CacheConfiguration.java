package bio.ferlab.clin.portal.forms.configurations;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Configuration
@ConfigurationProperties("cache")
@Slf4j
public class CacheConfiguration {
  
  public static final String CACHE_CODES_VALUES = "CACHE_CODES_VALUES";
  public static final String CACHE_ROLES = "CACHE_ROLES";
  public static final String CACHE_ORGANIZATIONS = "CACHE_ORGANIZATIONS";

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

  @Scheduled(fixedRateString = "${cache.eviction}", timeUnit = TimeUnit.SECONDS)
  @CacheEvict(value = CACHE_ORGANIZATIONS, allEntries = true)
  public void evictOrganizations() {
    log.debug("Evict organizations from cache");
  }

  /*
  The default implementation SimpleKeyGenerator generates cache key based on the method's param's type.
  This implementation can lead to conflict if methods use the same cache name and param's type.
  https://www.baeldung.com/spring-cache-custom-keygenerator
   */
  @Bean("customKeyGenerator")
  public KeyGenerator keyGenerator() {  // better implementation
    return (target, method, params) -> target.getClass().getSimpleName() + "_"
        + method.getName() + "_"
        + StringUtils.arrayToDelimitedString(params, "_");
  }
  
}
