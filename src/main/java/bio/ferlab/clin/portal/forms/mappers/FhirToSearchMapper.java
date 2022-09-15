package bio.ferlab.clin.portal.forms.mappers;

import bio.ferlab.clin.portal.forms.models.search.SearchPatient;
import bio.ferlab.clin.portal.forms.models.search.SearchPrescription;
import bio.ferlab.clin.portal.forms.utils.FhirConst;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import bio.ferlab.clin.portal.forms.utils.Utils;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

@Component
public class FhirToSearchMapper {
  public SearchPatient mapToSearch(org.hl7.fhir.r4.model.Person person, Patient patient) {
    final SearchPatient search = new SearchPatient();
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
  
  public SearchPrescription mapToSearchPrescription(ServiceRequest analysis,
                                                    Practitioner practitioner,
                                                    PractitionerRole role,
                                                    Patient patient, Person person, RelatedPerson mother) {
    final SearchPrescription prescription = new SearchPrescription();
    prescription.setId(analysis.getIdElement().getIdPart());
    prescription.setEp(FhirUtils.extractId(role.getOrganization()));
    prescription.setPrescriberId(role.getIdElement().getIdPart());
    if (practitioner.hasName()) {
      prescription.setPrescriberName(practitioner.getNameFirstRep().getNameAsSingleString());
    }
    if (analysis.hasAuthoredOn()) {
      prescription.setDate(Utils.getDateFormat().format(analysis.getAuthoredOn()));
    }
    if (person.hasName()) {
      prescription.setPatientName(person.getNameFirstRep().getNameAsSingleString());
    }
    if (person.hasIdentifier()) {
      prescription.setPatientRamq(person.getIdentifierFirstRep().getValue());
    }
    prescription.setPatientId(patient.getIdElement().getIdPart());
    if (analysis.hasCode()) {
      prescription.setPanelCode(analysis.getCode().getCodingFirstRep().getCode());
    }
    if (mother != null && mother.hasIdentifier()) {
      prescription.setMotherRamq(mother.getIdentifierFirstRep().getValue());
    }
    return prescription;
  }  
  
  private String getRamq(Person person) {
    return person.getIdentifier().stream().filter(i -> FhirConst.SYSTEM_RAMQ.equals(i.getType().getCodingFirstRep().getSystem()) && FhirConst.CODE_RAMQ.equals(i.getType().getCodingFirstRep().getCode())).map(Identifier::getValue).findFirst().orElse(null);
  }

  private String getMrn(Patient patient) {
    return patient.getIdentifier().stream().filter(i -> FhirConst.SYSTEM_MRN.equals(i.getType().getCodingFirstRep().getSystem()) && FhirConst.CODE_MRN.equals(i.getType().getCodingFirstRep().getCode())).map(Identifier::getValue).findFirst().orElse(null);
  }
}
