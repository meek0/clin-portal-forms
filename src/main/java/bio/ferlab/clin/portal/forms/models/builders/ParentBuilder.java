package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import bio.ferlab.clin.portal.forms.models.submit.Parent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Person;

@RequiredArgsConstructor
public class ParentBuilder {

  private final FhirClient fhirClient;
  private final SubmitToFhirMapper mapper;
  private final bio.ferlab.clin.portal.forms.models.submit.Parent parent;

  public ParentBuilder.Result build() {
    PatientBuilder.Result patientResult = null;
    if (parent != null) {
      parent.validate();
      if (Parent.Moment.now.equals(parent.getParentEnterMoment())){
        patientResult = PatientBuilder.findUpdateOrCreate(fhirClient, parent);
      }
    }
    return new Result(patientResult);
  }

  @AllArgsConstructor
  @Getter
  public static class Result {
    PatientBuilder.Result patientResult;

    public Person getPerson() {
      return patientResult != null ? patientResult.getPerson() : null;
    }
    public Patient getPatient() {
      return patientResult != null ? patientResult.getPatient() : null;
    }
  }
}
