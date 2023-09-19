package bio.ferlab.clin.portal.forms.configurations;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest
class CacheConfigurationTest {

  @MockBean
  private FhirClient fhirClient;

  @Autowired
  @Qualifier("customKeyGenerator")
  private KeyGenerator keyGenerator;

  @Test
  void customKeyGenerator() {
    final Method method = Mockito.mock(Method.class);
    when(method.getName()).thenReturn("findFromFhir");
    final List<Object> params = new ArrayList<>();
    params.add("foo");params.add(null);
    params.add( new ServiceRequest().setId("bar"));
    assertEquals("FhirClient_findFromFhir_", keyGenerator.generate(fhirClient, method, null));
    assertEquals("FhirClient_findFromFhir_foo_null_ServiceRequest/bar", keyGenerator.generate(fhirClient, method, params.toArray()));
  }
}