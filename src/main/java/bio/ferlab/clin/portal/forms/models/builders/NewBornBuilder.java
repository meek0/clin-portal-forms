package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.mappers.SubmitToFhirMapper;
import bio.ferlab.clin.portal.forms.models.submit.AdditionalInfo;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.RelatedPerson;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RequiredArgsConstructor
public class NewBornBuilder {
  
  private final SubmitToFhirMapper mapper;
  private final AdditionalInfo additionalInfo;
  private final Patient patient;
  
  public Result build() {
    RelatedPerson relatedPerson = null;
    if (Boolean.TRUE.equals(additionalInfo.getIsNewBorn())) {
      additionalInfo.validate();
      relatedPerson = mapper.mapToRelatedPerson(patient, additionalInfo.getMotherRamq());
    }
    return new Result(relatedPerson);
  }
  
  @Getter
  @AllArgsConstructor
  public static class Result {
    private final RelatedPerson relatedPerson;
  }
}
