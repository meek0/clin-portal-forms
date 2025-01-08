package bio.ferlab.clin.portal.forms.models;

import lombok.Data;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Data
public class Prescription {
  private ServiceRequest analysis;
  private List<ServiceRequest> sequencings;
  private List<Patient> patients;
  private Patient probandPatient;
  private List<Person> persons;
  private Person probandPerson;
  private Map<String, List<Reference>> familyMembers;
  private Practitioner practitioner;
  private PractitionerRole practitionerRole;
  private Organization performer;
  private Organization organization;
  private List<ClinicalImpression> impressions;
  private List<Observation> observations;
  private List<FamilyMemberHistory> familyHistories;

  public List<IBaseResource> getAllResources() {
    var allResources = new ArrayList<IBaseResource>();
    allResources.add(analysis);
    allResources.addAll(sequencings);
    allResources.addAll(patients);
    allResources.addAll(persons);
    allResources.add(practitioner);
    allResources.add(practitionerRole);
    allResources.add(performer);
    allResources.add(organization);
    allResources.addAll(impressions);
    allResources.addAll(observations);
    allResources.addAll(familyHistories);
    return allResources.stream().filter(Objects::nonNull).toList();
  }
}
