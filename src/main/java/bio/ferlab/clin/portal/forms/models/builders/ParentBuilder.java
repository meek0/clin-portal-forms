package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import bio.ferlab.clin.portal.forms.models.submit.Parent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ParentBuilder {

  private final FhirClient fhirClient;
  private final SubmitToFhirMapper mapper;
  private final bio.ferlab.clin.portal.forms.models.submit.Parent parent;

  public ParentBuilder.Result build() {
    PatientBuilder.Result patient = null;
    if (parent != null) {
      parent.validate();
      if (Parent.Moment.now.equals(parent.getParentEnterMoment())){
        patient = PatientBuilder.findUpdateOrCreate(fhirClient, parent);
      }
    }
    return new Result(patient);
  }

  @AllArgsConstructor
  @Getter
  public static class Result {
    PatientBuilder.Result patient;
  }
}
