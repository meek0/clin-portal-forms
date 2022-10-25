package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.ClinicalImpression;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.ServiceRequest;

@RequiredArgsConstructor
public class AnalysisBuilder {

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
      this.orderDetails = String.format("Reflex Panel: %s (%s)", "Global Muscular diseases", "MMG");
    }
    return this;
  }

  @AllArgsConstructor
  @Getter
  public static class Result {
    private final ServiceRequest analysis;
  }
}
