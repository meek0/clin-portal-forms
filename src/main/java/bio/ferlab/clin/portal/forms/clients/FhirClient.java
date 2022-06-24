package bio.ferlab.clin.portal.forms.clients;

import bio.ferlab.clin.portal.forms.configurations.FhirConfiguration;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.PerformanceOptionsEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
@Getter
public class FhirClient {
  
  private final FhirContext context;
  private final IClinFhirClient clinClient;
  private final IGenericClient genericClient;
  
  public FhirClient(FhirConfiguration configuration, FhirAuthInterceptor fhirAuthInterceptor) {
    context = FhirContext.forR4();
    context.getRestfulClientFactory().setConnectTimeout(configuration.getTimeout());
    context.getRestfulClientFactory().setSocketTimeout(configuration.getTimeout());
    context.getRestfulClientFactory().setPoolMaxTotal(configuration.getPoolSize());
    context.getRestfulClientFactory().setPoolMaxPerRoute(configuration.getPoolSize());
    context.setPerformanceOptions(PerformanceOptionsEnum.DEFERRED_MODEL_SCANNING);
    context.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        
    this.clinClient = context.newRestfulClient(IClinFhirClient.class, configuration.getUrl());
    this.genericClient = context.newRestfulGenericClient(configuration.getUrl());
    
    clinClient.registerInterceptor(fhirAuthInterceptor);
    genericClient.registerInterceptor(fhirAuthInterceptor);
  }
  
}
