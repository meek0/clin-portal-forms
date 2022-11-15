package bio.ferlab.clin.portal.forms.models.builders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReflexBuilderTest {

  @Test
  void build() {
    assertEquals("Reflex Panel: Global Muscular diseases (MMG)", new ReflexBuilder("fr", true).build().getReflex());
    assertEquals("Panel Reflex: Maladies musculaires (MMG)", new ReflexBuilder(null, true).build().getReflex());
  }

}