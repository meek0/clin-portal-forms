package bio.ferlab.clin.portal.forms.configurations;

import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class CacheConfiguration {

  public static final String CACHE_FHIR = "CACHE_FHIR";
  public static final String CACHE_CODES_VALUES = "CACHE_CODES_VALUES";
  public static final String CACHE_ROLES = "CACHE_ROLES";

  @Scheduled(fixedRateString = "${fhir.cache}", timeUnit = TimeUnit.MILLISECONDS)
  @CacheEvict(value = CACHE_FHIR, allEntries = true)
  public void evictFhirCache() {
    // log.debug("Evict FHIR cache");
  }

  @Scheduled(fixedRateString = "${cache.eviction}", timeUnit = TimeUnit.SECONDS)
  @CacheEvict(value = CACHE_CODES_VALUES, allEntries = true)
  public void evictCodesValues() {
    log.debug("Evict codes and values from cache");
  }

  @Scheduled(fixedRateString = "${cache.short-eviction}", timeUnit = TimeUnit.SECONDS)
  @CacheEvict(value = CACHE_ROLES, allEntries = true)
  public void evictRoles() {
    log.debug("Evict practitioner roles from cache");
  }

  /*
  The default implementation SimpleKeyGenerator generates cache key based on the method's param's type.
  This implementation can lead to conflict if methods use the same cache name and param's type.
  https://www.baeldung.com/spring-cache-custom-keygenerator
   */
  @Bean("customKeyGenerator")
  public KeyGenerator keyGenerator() {  // better implementation
    return (target, method, params) -> {
      final String key = target.getClass().getSimpleName() + "_"
        + method.getName() + "_"
        + StringUtils.arrayToDelimitedString(mapFhirReferences(params), "_");
      log.debug("KeyGenerator: {}", key);
      return key;
    };
  }

  private Object[] mapFhirReferences(Object ... params) {
    if (params != null) {
      return Arrays.stream(params).map(o -> {
        if (o instanceof IBaseResource r) {
          return FhirUtils.formatResource(r);
        }
        return o;
      }).toArray();
    }
    return params;
  }

}
