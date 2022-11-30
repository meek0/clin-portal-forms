package bio.ferlab.clin.portal.forms.utils;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BundleExtractorTest {

  private final FhirContext fhirContext = FhirContext.forR4();

  @Test
  void getAllResourcesOfType() {
    final Bundle bundle = new Bundle();
    bundle.addEntry().setResource(new Patient());
    bundle.addEntry().setResource(new Patient());
    bundle.addEntry().setResource(new Bundle());
    bundle.addEntry().setResource(new Practitioner());
    final Bundle internalBundle = new Bundle();
    internalBundle.addEntry().setResource(new Practitioner().setId("id2"));
    bundle.addEntry().setResource(internalBundle);
    final BundleExtractor extractor = new BundleExtractor(fhirContext, bundle);
    assertEquals(2, extractor.getAllResourcesOfType(Patient.class).size());
    assertEquals(0, extractor.getAllResourcesOfType(Organization.class).size());
    assertEquals(2, extractor.getAllResourcesOfType(Practitioner.class).size());
  }
  
  @Test
  void getNextResourcesOfType() {
    final Bundle bundle = new Bundle();
    bundle.addEntry().setResource(new Patient().setId("id1"));
    bundle.addEntry().setResource(new Patient().setId("id2"));
    bundle.addEntry().setResource(new Patient().setId("id3"));
    bundle.addEntry().setResource(new Organization().setId("id1"));

    final BundleExtractor extractor = new BundleExtractor(fhirContext, bundle);
    assertEquals("id1", extractor.getNextResourcesOfType(Patient.class).getId());
    assertEquals("id2", extractor.getNextResourcesOfType(Patient.class).getId());
    assertEquals("id3", extractor.getNextResourcesOfType(Patient.class).getId());
    assertNull(extractor.getNextResourcesOfType(Patient.class));
  }
  
  @Test
  void getFirstResourcesOfType() {
    final Bundle bundle = new Bundle();
    bundle.addEntry().setResource(new Patient().setId("id1"));
    final Bundle internalBundle = new Bundle();
    internalBundle.addEntry().setResource(new Practitioner().setId("id2"));
    bundle.addEntry().setResource(internalBundle);
    final BundleExtractor extractor = new BundleExtractor(fhirContext, bundle);
    assertEquals("id1", extractor.getFirstResourcesOfType(Patient.class).getId());
    assertEquals("id2", extractor.getFirstResourcesOfType(Practitioner.class).getId());
    assertNull(extractor.getFirstResourcesOfType(Person.class));
  }
  
  @Test
  void extractFirstIdFromResponse() {
    final Bundle bundle = new Bundle();
    bundle.addEntry().setResponse(null);
    bundle.addEntry().setResponse(new Bundle.BundleEntryResponseComponent());
    bundle.addEntry().setResponse(new Bundle.BundleEntryResponseComponent().setLocation("Type1/foo"));
    bundle.addEntry().setResponse(new Bundle.BundleEntryResponseComponent().setLocation("Type2/bar"));
    final BundleExtractor extractor = new BundleExtractor(fhirContext, bundle);
    assertNull(extractor.extractFirstIdFromResponse(null));
    assertNull(extractor.extractFirstIdFromResponse(""));
    assertNull(extractor.extractFirstIdFromResponse("Type"));
    assertEquals("bar", extractor.extractFirstIdFromResponse("Type2"));
  }

}