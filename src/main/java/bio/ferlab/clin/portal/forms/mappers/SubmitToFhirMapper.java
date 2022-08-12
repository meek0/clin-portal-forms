package bio.ferlab.clin.portal.forms.mappers;

import bio.ferlab.clin.portal.forms.models.submit.Patient;
import bio.ferlab.clin.portal.forms.models.submit.*;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static bio.ferlab.clin.portal.forms.utils.FhirConst.*;

@Component
public class SubmitToFhirMapper {
  
  private final ZoneId zoneId = ZoneId.systemDefault();
  
  public Date mapToDate(LocalDate localDate) {
    return Date.from(localDate.atStartOfDay(zoneId).toInstant());
  }
  
  public long mapToAge(Date birthDate) {
    // ChronoUnit.YEARS doesn't work, in case you want to use it ...
    return ChronoUnit.DAYS.between(birthDate.toInstant(), Instant.now());
  }
  
  public Enumerations.AdministrativeGender mapToGender(Patient.Gender gender) {
    return Enumerations.AdministrativeGender.fromCode(gender.name());
  }
  
  public Person mapToPerson(Patient patient, org.hl7.fhir.r4.model.Patient linkedPatient) {
    final Person p = new Person();
    // don't use IdType.newRandomUuid() because it creates a conflict when looking for Person/urn:uuid:xxxx
    p.setId(UUID.randomUUID().toString());
    this.updatePerson(patient, p, linkedPatient);
    return p;
  }
  
  public org.hl7.fhir.r4.model.Patient mapToPatient(Patient patient) {
    final Reference epRef = FhirUtils.toReference(new Organization().setId(patient.getEp()));
    final org.hl7.fhir.r4.model.Patient p = new org.hl7.fhir.r4.model.Patient();
    p.setId(UUID.randomUUID().toString());
    p.setManagingOrganization(epRef);
    this.updatePatient(patient, p);
    return p;
  }
  
  public void updatePatient(Patient patient, org.hl7.fhir.r4.model.Patient res) {
    final Reference epRef = FhirUtils.toReference(new Organization().setId(patient.getEp()));
    res.setGender(mapToGender(patient.getGender()));
    updateIdentifier(res.getIdentifier(), SYSTEM_MRN, CODE_MRN, patient.getMrn(), epRef);
  }
  
  public void updatePerson(Patient patient, Person person, org.hl7.fhir.r4.model.Patient linkedPatient) {
    updateIdentifier(person.getIdentifier(), SYSTEM_RAMQ, CODE_RAMQ, patient.getRamq(), null);
    person.setBirthDate(mapToDate(patient.getBirthDate()));
    person.setGender(mapToGender(patient.getGender()));
    person.getName().clear();
    person.addName().addGiven(patient.getFirstName()).setFamily(patient.getLastName());
    final String linkedPatientRef = FhirUtils.formatResource(linkedPatient);
    boolean isLinked = person.getLink().stream().anyMatch(l -> linkedPatientRef.equals(l.getTarget().getReference()));
    if(!isLinked){
      person.getLink().add(new Person.PersonLinkComponent(new Reference(linkedPatientRef)));
    }
  }
  
  public ServiceRequest mapToAnalysis(String panelCode, org.hl7.fhir.r4.model.Patient patient,
                                      ClinicalImpression clinicalImpression, String orderDetails,
                                      PractitionerRole practitionerRole, PractitionerRole supervisorRole,
                                      String comment, org.hl7.fhir.r4.model.Patient foetus) {
    final ServiceRequest serviceRequest = new ServiceRequest();
    serviceRequest.setId(UUID.randomUUID().toString());
    if (foetus != null) {
      serviceRequest.addCategory().setText("Prenatal");
    }
    serviceRequest.getMeta().addProfile(ANALYSIS_SERVICE_REQUEST);
    serviceRequest.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
    serviceRequest.setSubject(FhirUtils.toReference(patient));
    serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.ONHOLD);
    serviceRequest.addSupportingInfo(FhirUtils.toReference(clinicalImpression));
    serviceRequest.setCode(new CodeableConcept().addCoding(new Coding().setSystem(ANALYSIS_REQUEST_CODE).setCode(panelCode)));
    serviceRequest.setAuthoredOn(new Date());
    serviceRequest.setRequester(FhirUtils.toReference(practitionerRole));
    String sanitizedComment = StringUtils.isNotBlank(comment) ? comment : "";
    serviceRequest.addNote(new Annotation().setText(sanitizedComment).setTime(new Date()).setAuthor(practitionerRole.getPractitioner()));
    if (StringUtils.isNotBlank(orderDetails)) {
      serviceRequest.addOrderDetail(new CodeableConcept().setText(orderDetails));
    }
    if(supervisorRole != null) {
      serviceRequest.addExtension(SUPERVISOR_EXT, FhirUtils.toReference(supervisorRole));
    }
    return serviceRequest;
  }

  public ServiceRequest mapToSequencing(String panelCode, org.hl7.fhir.r4.model.Patient patient, ServiceRequest analysis, PractitionerRole practitionerRole, org.hl7.fhir.r4.model.Patient foetus) {
    final ServiceRequest serviceRequest = new ServiceRequest();
    serviceRequest.setId(UUID.randomUUID().toString());
    serviceRequest.getMeta().addProfile(SEQUENCING_SERVICE_REQUEST);
    serviceRequest.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
    if (foetus != null) {
      serviceRequest.addCategory().setText("Prenatal");
      serviceRequest.setSubject(FhirUtils.toReference(foetus));
    } else {
      serviceRequest.setSubject(FhirUtils.toReference(patient));
    }
    serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.ONHOLD);
    serviceRequest.addBasedOn(FhirUtils.toReference(analysis));
    serviceRequest.setRequester(FhirUtils.toReference(practitionerRole));
    serviceRequest.setCode(new CodeableConcept().addCoding(new Coding().setSystem(ANALYSIS_REQUEST_CODE).setCode(panelCode)));
    serviceRequest.setAuthoredOn(new Date());
    return serviceRequest;
  }
  
  public ClinicalImpression mapToClinicalImpression(Person person, org.hl7.fhir.r4.model.Patient patient, List<Observation> observations) {
    final ClinicalImpression clinicalImpression = new ClinicalImpression();
    clinicalImpression.setId(UUID.randomUUID().toString());
    clinicalImpression.setSubject(FhirUtils.toReference(patient));
    clinicalImpression.setStatus(ClinicalImpression.ClinicalImpressionStatus.COMPLETED);
    clinicalImpression.addExtension(AGE_AT_EVENT_EXT, new Age().setSystem(Enumerations.AgeUnits.D.getSystem()).setCode(Enumerations.AgeUnits.D.toCode()).setValue(mapToAge(person.getBirthDate())));
    observations.forEach(o -> clinicalImpression.addInvestigation(new ClinicalImpression.ClinicalImpressionInvestigationComponent(new CodeableConcept().setText("Examination / signs")).addItem(FhirUtils.toReference(o))));
    return clinicalImpression;
  }
  
  public List<Observation> mapToObservations(String panelCode,
                                             org.hl7.fhir.r4.model.Patient patient,
                                             Analyse analyse,
                                             ClinicalSigns signs,
                                             ParaclinicalExams exams,
                                             String ethnicity) {
    
    List<Observation> all = new ArrayList<>();

    Observation dsta = createObservation(patient, "DSTA", "exam",true, ANALYSIS_REQUEST_CODE, panelCode);
    all.add(dsta);
    
    if(StringUtils.isNotBlank(signs.getComment())) {
      Observation obsg = createObservation(patient, "OBSG", "exam",null, null, signs.getComment());
      all.add(obsg);
    }

    if(StringUtils.isNotBlank(exams.getComment())) {
      Observation obsg = createObservation(patient, "INVES", "exam", null, null, exams.getComment());
      all.add(obsg);
    }
    
    if(StringUtils.isNotBlank(ethnicity)) {
      Observation obsg = createObservation(patient, "ETHN", "exam",null, ETHNICITY_CODE, ethnicity);
      all.add(obsg);
    }

    Observation indic = createObservation(patient, "INDIC", "exam",null, null, analyse.getIndication());
    all.add(indic);

    all.addAll(signs.getSigns().stream().map(o -> {
      Observation obs = createObservation(patient, "PHEN", "exam",o.getIsObserved(), HP_CODE, o.getValue());
      if(o.getAgeCode() != null) {
        obs.addExtension(AGE_AT_ONSET_EXT, new Coding().setCode(o.getAgeCode()));
      }
      return obs;
    }).collect(Collectors.toList()));

    all.addAll(exams.getExams().stream().map(o -> {
      Observation obs = createObservation(patient, o.getCode(), "procedure", null, null, o.getValue());
      obs.addInterpretation(new CodeableConcept(new Coding().setSystem(OBSERVATION_INTERPRETATION).setCode(getInterpretationCode(o.getInterpretation()))));
      o.getValues().forEach(v -> obs.getValueCodeableConcept().addCoding(new Coding().setSystem(HP_CODE).setCode(v)));
      return obs;
    }).collect(Collectors.toList()));
    
    return all;
  }
  
  public RelatedPerson mapToRelatedPerson(org.hl7.fhir.r4.model.Patient patient, String motherRamq) {
    final RelatedPerson relatedPerson = new RelatedPerson();
    relatedPerson.setId(UUID.randomUUID().toString());
    this.updateIdentifier(relatedPerson.getIdentifier(), SYSTEM_RAMQ, CODE_RAMQ, motherRamq, null);
    relatedPerson.setPatient(FhirUtils.toReference(patient));
    relatedPerson.addRelationship().setText("Mother").addCoding().setSystem(SYSTEM_MOTHER).setCode(CODE_MOTHER);
    relatedPerson.setActive(true);
    return relatedPerson;
  }
  
  public org.hl7.fhir.r4.model.Patient mapToFoetus(AdditionalInfo additionalInfo, org.hl7.fhir.r4.model.Patient mother) {
    final org.hl7.fhir.r4.model.Patient foetus = new org.hl7.fhir.r4.model.Patient();
    foetus.setId(UUID.randomUUID().toString());
    // foetus.addExtension().setValue(new CodeableConcept(new Coding().setSystem(SYSTEM_GESTATIONAL_AGE).setCode(CODE_GESTATIONAL_AGE)).setText(additionalInfo.getGestationalAge().name()));
    foetus.setDeceased(new BooleanType(AdditionalInfo.GestationalAge.deceased.equals(additionalInfo.getGestationalAge())));
    foetus.setGender(mapToGender(additionalInfo.getFoetusGender()));
    final var link = new org.hl7.fhir.r4.model.Patient.PatientLinkComponent();
    link.setOther(FhirUtils.toReference(mother));
    link.setType(org.hl7.fhir.r4.model.Patient.LinkType.SEEALSO);
    foetus.addLink(link);
    return foetus;
  }
  
  public Observation mapToObservation(AdditionalInfo additionalInfo, org.hl7.fhir.r4.model.Patient mother, org.hl7.fhir.r4.model.Patient foetus) {
    final Observation observation = new Observation();
    observation.setId(UUID.randomUUID().toString());
    observation.setStatus(Observation.ObservationStatus.FINAL);
    observation.setSubject(FhirUtils.toReference(mother));
    observation.setFocus(List.of(FhirUtils.toReference(foetus)));
    switch (additionalInfo.getGestationalAge()) {
      case ddm:
        observation.setCode(new CodeableConcept(new Coding().setSystem(SYSTEM_DDM).setCode(CODE_DDM)));
        break;
      case dpa:
        observation.setCode(new CodeableConcept(new Coding().setSystem(SYSTEM_DPA).setCode(CODE_DPA)));
        break;
      default:
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patient.additional_info gestational_age should be ddm or dpa");
    }
    observation.getValueDateTimeType().setValue(mapToDate(additionalInfo.getGestationalDate()));
    return observation;
  }
  
  private String getInterpretationCode(Exams.Interpretation interpretation) {
    switch (interpretation){
      case abnormal:
        return "A";
      case normal:
        return "N";
      default:
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid interpretation value: " + interpretation);
    }
  }
  
  private Observation createObservation(org.hl7.fhir.r4.model.Patient patient, String code, String category, Boolean isObserved, String system, String value) {
    Observation observation = new Observation();
    observation.setSubject(FhirUtils.toReference(patient));
    observation.setId(UUID.randomUUID().toString());
    observation.getMeta().addProfile(OBSERVATION_PROFILE);
    observation.setStatus(Observation.ObservationStatus.FINAL);
    observation.setCode(new CodeableConcept(new Coding().setSystem(OBSERVATION_CODE).setCode(code)));
    observation.addCategory(new CodeableConcept(new Coding().setSystem(OBSERVATION_CATEGORY).setCode(category)));
    if(isObserved != null) {
      observation.addInterpretation(new CodeableConcept(new Coding().setSystem(OBSERVATION_INTERPRETATION).setCode(isObserved ? "POS" : "NEG")));
    }
    if (StringUtils.isNotBlank(value)) {
      if (StringUtils.isNotBlank(system)) {
        observation.setValue(new CodeableConcept(new Coding().setSystem(system).setCode(value)));
      } else {
        observation.setValue(new StringType(value));
      }
    }
    return observation;
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
        final String previousValue = identifierToUpdate.getValue();
        if (StringUtils.isBlank(previousValue) || !previousValue.equals(value)) {
          identifierToUpdate.setValue(value); // FHIR Server will check if modifying MRN/RAMQ is allowed or not
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
