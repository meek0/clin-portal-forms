package bio.ferlab.clin.portal.forms.models.builders;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ReflexBuilder {

  /*private final CodesValuesService codesValuesService;
  private final FhirToConfigMapper fhirToConfigMapper;*/
  private final String lang;
  private final Boolean isReflex;

  public Result build() {
    String reflex = null;
    if (isReflex) {
      if ("fr".equals(lang)) {
        reflex = String.format("Panel réflexe: %s (%s)", "Maladies musculaires globales", "MMG");
      } else {
        reflex = String.format("Reflex Panel: %s (%s)", "Global Muscular diseases", "MMG");
      }
      /*final CodeSystem analyse = codesValuesService.getCodes(CodesValuesService.ANALYSE_KEY);
      reflex = analyse.getConcept().stream()
        .filter(c -> "MMG".equals(c.getCode()))
        .findFirst()
        .map(c -> fhirToConfigMapper.getDisplayForLang(c, lang))
        .orElse(String.format("Reflex Panel: %s (%s)", "Global Muscular diseases", "MMG"));*/
      // Reflex Panel: Global Muscular diseases (MMG)
    }
    return new Result(reflex);
  }

  @Getter
  @AllArgsConstructor
  public static class Result {
    private String reflex;
  }

}
