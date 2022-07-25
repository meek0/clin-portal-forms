package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.*;

@RequiredArgsConstructor
public class AnalysisBuilder {
  
  private final FhirClient fhirClient;
  private final SubmitToFhirMapper mapper;
  private final String panelCode;
  private final Patient patient;
  private final ClinicalImpression clinicalImpression;
  
  private String orderDetails;

  public Result build() {
    final ServiceRequest serviceRequest = mapper.mapToAnalysis(panelCode, patient, clinicalImpression, orderDetails);
    return new Result(serviceRequest);
  }
  
  public AnalysisBuilder withReflex(boolean isReflex) {
    if (isReflex) {
      CodeSystem codes = this.fhirClient.getGenericClient().read().resource(CodeSystem.class).withId("analysis-request-code").encodedJson().execute();
      CodeSystem.ConceptDefinitionComponent code = codes.getConcept().stream().filter(c -> panelCode.equals(c.getCode())).findFirst().get();
      this.orderDetails = String.format("Reflex Panel: %s (%s)", code.getDisplay(), code.getCode());
    }
    return this;
  }

  @AllArgsConstructor
  @Getter
  public static class Result {
    private final ServiceRequest analysis;
  }
}
