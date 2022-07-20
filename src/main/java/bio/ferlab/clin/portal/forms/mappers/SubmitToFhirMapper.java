package bio.ferlab.clin.portal.forms.mappers;

import bio.ferlab.clin.portal.forms.models.submit.Patient;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

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
    p.addIdentifier().setValue(patient.getRamq()).setType(new CodeableConcept().addCoding(new Coding().setSystem(SYSTEM_RAMQ).setCode(CODE_RAMQ)));
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
    res.getMeta().getSecurity().clear();
    res.getIdentifier().clear();  // TODO nope bad bad
    res.addIdentifier()
        .setValue(Optional.ofNullable(patient.getMrn()).orElse(UUID.randomUUID().toString()))
        .setAssigner(epRef)
        .setType(new CodeableConcept().addCoding(new Coding().setSystem(SYSTEM_MRN).setCode(CODE_MRN)));
  }
  
  public void updatePerson(Patient patient, Person person, org.hl7.fhir.r4.model.Patient linkedPatient) {
    person.getMeta().getSecurity().clear();
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
}
