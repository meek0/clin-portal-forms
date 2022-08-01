package bio.ferlab.clin.portal.forms.utils;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
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
    final BundleExtractor extractor = new BundleExtractor(fhirContext, bundle);
    assertEquals(2, extractor.getAllResourcesOfType(Patient.class).size());
    assertEquals(0, extractor.getAllResourcesOfType(Organization.class).size());
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

}