package bio.ferlab.clin.portal.forms.mappers;

import bio.ferlab.clin.portal.forms.models.submit.Patient;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static bio.ferlab.clin.portal.forms.utils.FhirConstants.*;

@Component
public class SubmitToFhirMapper {
  
  private final ZoneId zoneId = ZoneId.systemDefault();
  
  public Date mapToDate(LocalDate localDate) {
    return Date.from(localDate.atStartOfDay(zoneId).toInstant());
  }
  
  public Enumerations.AdministrativeGender mapToGender(String gender) {
    return Enumerations.AdministrativeGender.fromCode(gender);
  }
  
  public Person mapToPerson(Patient patient, org.hl7.fhir.r4.model.Patient linkedPatient) {
    final Person p = new Person();
    p.setId(UUID.randomUUID().toString());
    this.updatePerson(patient, p, linkedPatient);
    return p;
  }
  
  public org.hl7.fhir.r4.model.Patient mapToPatient(Patient patient) {
    final Reference epRef = new Reference("Organization/"+patient.getEp());
    final org.hl7.fhir.r4.model.Patient p = new org.hl7.fhir.r4.model.Patient();
    p.setId(UUID.randomUUID().toString());
    this.updatePatient(patient, p);
    p.setGender(mapToGender(patient.getGender()));
    p.setManagingOrganization(epRef);
    return p;
  }
  
  public void updatePatient(Patient patient, org.hl7.fhir.r4.model.Patient res) {
    final Reference epRef = new Reference("Organization/"+patient.getEp());
    updateIdentifier(res.getIdentifier(), SYSTEM_MRN, CODE_MRN, patient.getMrn(), epRef);
  }
  
  public void updatePerson(Patient patient, Person person, org.hl7.fhir.r4.model.Patient linkedPatient) {
    updateIdentifier(person.getIdentifier(), SYSTEM_RAMQ, CODE_RAMQ, patient.getRamq(), null);
    person.setBirthDate(mapToDate(patient.getBirthDate()));
    person.setGender(mapToGender(patient.getGender()));
    person.getName().clear();
    person.addName().addGiven(patient.getFirstName()).setFamily(patient.getLastName());
    final String linkedPatientRef = "Patient/"+new IdType(linkedPatient.getId()).getIdPart();
    boolean isLinked = person.getLink().stream().anyMatch(l -> linkedPatientRef.equals(l.getTarget().getReference()));
    if(!isLinked){
      person.getLink().add(new Person.PersonLinkComponent(new Reference(linkedPatientRef)));
    }
  }
  
  private void updateIdentifier(List<Identifier> identifiers, String system, String code, String value, Reference assigner){
    if(StringUtils.isNotBlank(value)) {
      Identifier identifierToUpdate = null;
      for(Identifier identifier: identifiers) {
        if (identifier.hasType() && identifier.getType().hasCoding()) {
          for (Coding coding : identifier.getType().getCoding()) {
            if (system.equals(coding.getSystem()) && code.equals(coding.getCode())) {
              identifierToUpdate = identifier;
            }
          }
        }
      }
      if (identifierToUpdate != null) {
        if (StringUtils.isNotBlank(identifierToUpdate.getValue())) {
          if (!identifierToUpdate.getValue().equals(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patient " + code + " already set");
          }
        } else {
          identifierToUpdate.setValue(value);
        }
      } else {
        identifiers.add(new Identifier()
            .setValue(value)
            .setAssigner(assigner)
            .setType(new CodeableConcept().addCoding(new Coding().setSystem(system).setCode(code))));
      }
    }
  }
}
