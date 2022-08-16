package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RequiredArgsConstructor
public class AnalysisBuilder {
  
  private final FhirClient fhirClient;
  private final SubmitToFhirMapper mapper;
  private final String panelCode;
  private final Patient patient;
  private final ClinicalImpression clinicalImpression;
  private final PractitionerRole practitionerRole;
  private final PractitionerRole supervisorRole;
  private final String comment;
  private Patient foetus;
  
  private String orderDetails;

  public Result build() {
    final ServiceRequest serviceRequest = mapper.mapToAnalysis(panelCode, patient, clinicalImpression, orderDetails, practitionerRole, supervisorRole, comment, foetus);
    return new Result(serviceRequest);
  }
  
  public AnalysisBuilder withFoetus(Patient foetus) {
    this.foetus = foetus;
    return this;
  }
  
  public AnalysisBuilder withReflex(boolean isReflex) {
    if (isReflex) {
      CodeSystem codes = this.fhirClient.findCodeSystemById("analysis-request-code");
      CodeSystem.ConceptDefinitionComponent code = codes.getConcept().stream().filter(c -> panelCode.equals(c.getCode())).findFirst()
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "panel code " + panelCode + " is unknown"));
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
