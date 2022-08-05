package bio.ferlab.clin.portal.forms.mappers;

import bio.ferlab.clin.portal.forms.models.search.Search;
import bio.ferlab.clin.portal.forms.utils.FhirConst;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Person;
import org.springframework.stereotype.Component;

@Component
public class FhirToSearchMapper {
  
  public Search mapToSearch(org.hl7.fhir.r4.model.Person person, Patient patient) {
  final Search search = new Search();
  if (person != null ){
    if (person.hasGender()) {
      search.setGender(person.getGender().toCode());
    }
    if (person.hasName()) {
      search.setFirstName(person.getNameFirstRep().getGivenAsSingleString());
      search.setLastName(person.getNameFirstRep().getFamily());
    }
    if (person.hasBirthDate()) {
      search.setBirthDate(person.getBirthDate());
    }
    search.setRamq(getRamq(person));
  }
  if (patient != null) {
    search.setMrn(getMrn(patient));
    if (patient.hasManagingOrganization()) {
      search.setEp(FhirUtils.extractId(patient.getManagingOrganization()));
    }
  }
  return search;
  }
  
  private String getRamq(Person person) {
    return person.getIdentifier().stream().filter(i -> FhirConst.SYSTEM_RAMQ.equals(i.getType().getCodingFirstRep().getSystem()) && FhirConst.CODE_RAMQ.equals(i.getType().getCodingFirstRep().getCode())).map(Identifier::getValue).findFirst().orElse(null);
  }

  private String getMrn(Patient patient) {
    return patient.getIdentifier().stream().filter(i -> FhirConst.SYSTEM_MRN.equals(i.getType().getCodingFirstRep().getSystem()) && FhirConst.CODE_MRN.equals(i.getType().getCodingFirstRep().getCode())).map(Identifier::getValue).findFirst().orElse(null);
  }
}
