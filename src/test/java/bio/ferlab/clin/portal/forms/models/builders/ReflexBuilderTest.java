package bio.ferlab.clin.portal.forms.models.builders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReflexBuilderTest {

  @Test
  void build() {
    assertEquals("Reflex Panel: Global Muscular diseases (MMG)", new ReflexBuilder(null, true).build().getReflex());
    assertEquals("Panel réflexe: Maladies musculaires globales (MMG)", new ReflexBuilder("fr", true).build().getReflex());
  }

}