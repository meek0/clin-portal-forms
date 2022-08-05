package bio.ferlab.clin.portal.forms.services;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.configurations.FhirConfiguration;
import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class CodesValuesServiceTest {
  
  private final FhirContext fhirContext = FhirContext.forR4();
  private final FhirClient fhirClient = Mockito.mock(FhirClient.class);
  private final FhirConfiguration fhirConfiguration = Mockito.mock(FhirConfiguration.class);
  private final CodesValuesService service =new CodesValuesService(fhirClient, fhirConfiguration);
  
  @BeforeEach
  void beforeEach() {
    final Bundle bundle = new Bundle();
    bundle.addEntry().setResource(new CodeSystem());
    bundle.addEntry().setResource(new CodeSystem());
    bundle.addEntry().setResource(new CodeSystem());
    bundle.addEntry().setResource(new CodeSystem());
    bundle.addEntry().setResource(new CodeSystem());
    bundle.addEntry().setResource(new ValueSet());
    when(fhirClient.getContext()).thenReturn(fhirContext);
    when(fhirClient.fetchCodesAndValues()).thenReturn(bundle);
    when(fhirConfiguration.getTypesWithDefault()).thenReturn(List.of());
    when(fhirConfiguration.getMultiValuesObservationCodes()).thenReturn(List.of());
  }
  
  @Test
  void getCodes() {
    final CodeSystem res = this.service.getCodes(CodesValuesService.ANALYSE_KEY);
    assertNotNull(res);
  }

  @Test
  void getValues() {
    final ValueSet res = this.service.getValues(CodesValuesService.AGE_KEY);
    assertNotNull(res);
  }


}