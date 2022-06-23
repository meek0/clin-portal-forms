package bio.ferlab.clin.portal.forms.configurations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {
  
  @Autowired
  private SecurityConfiguration securityConfiguration;

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
        .allowedOrigins(securityConfiguration.getCors().toArray(String[]::new))
        .allowedMethods("*")
        .allowedHeaders("*")  // request allowed headers
        .exposedHeaders("*")  // response allowed headers
        .allowCredentials(true) // authorization header
        .maxAge(3600);
  }
}
