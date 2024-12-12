package bio.ferlab.clin.portal.forms.services;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.models.Prescription;
import bio.ferlab.clin.portal.forms.utils.BundleExtractor;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.TreeMap;

import static bio.ferlab.clin.portal.forms.utils.FhirConst.*;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

  private final FhirClient fhirClient;

  public Prescription fromAnalysisId(String id) {
    var prescription = new Prescription();

    var mainBundle = fhirClient.findServiceRequestWithDepsById(id);
    var mainBundleExtractor = new BundleExtractor(fhirClient.getContext(), mainBundle);
    var analysis = mainBundleExtractor.getFirstResourcesOfType(ServiceRequest.class);
    // if the user doesn't belong to the EP/LDM
    if (analysis == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prescription not found: " + id);
    // has to be analysis
    if (!analysis.getMeta().hasProfile(ANALYSIS_SERVICE_REQUEST)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prescription isn't an analysis: " + id);
    }
    var performer = mainBundleExtractor.getFirstResourcesOfType(Organization.class);
    // Assignation feature will attach several PractitionerRole insider performer BUT we want the one from requester
    var practitionerRole = mainBundleExtractor.getFirstResourcesById(PractitionerRole.class, FhirUtils.extractId(analysis.getRequester())); // null for imported batch
    var probandPatient = mainBundleExtractor.getFirstResourcesOfType(Patient.class);

    // DUO/TRIO ...
    var familyMembers = new TreeMap<String, Reference>();
    for(var member: analysis.getExtensionsByUrl(FAMILY_MEMBER)) {
      var parentPatientRef = ((Reference) member.getExtensionByUrl("parent").getValue());
      var parentRelation = ((CodeableConcept) member.getExtensionByUrl("parent-relationship").getValue()).getCodingFirstRep().getCode();
      familyMembers.put(parentRelation, parentPatientRef);
    }

    var detailsBundle = fhirClient.fetchPrescriptionDetails(analysis, practitionerRole, probandPatient, familyMembers);
    var detailsBundleExtractor = new BundleExtractor(fhirClient.getContext(), detailsBundle);
    var sequencings = detailsBundleExtractor.getAllResourcesOfType(ServiceRequest.class).stream()
      .filter(s -> s.getMeta().hasProfile(SEQUENCING_SERVICE_REQUEST)).toList();
    var patients = detailsBundleExtractor.getAllResourcesOfType(Patient.class);

    var persons = detailsBundleExtractor.getAllResourcesOfType(Person.class);

    var probandPerson = persons.stream()
      .filter(s -> s.getLink().stream().anyMatch(l -> l.getTarget().getReference().equals(analysis.getSubject().getReference())))
      .findFirst().orElseThrow(() -> new RuntimeException("Can't find person for analysis: " + analysis.getIdElement().getIdPart() + " and subject: " + analysis.getSubject().getReference()));

    var practitioner = detailsBundleExtractor.getFirstResourcesOfType(Practitioner.class);
    // bellow is null for imported batch cause requester is null and we use the requester to find the PractitionerRole in details request
    var organization = detailsBundleExtractor.getFirstResourcesById(Organization.class, FhirUtils.extractId(probandPatient.getManagingOrganization()));
    var impressions = detailsBundleExtractor.getAllResourcesOfType(ClinicalImpression.class);
    var observations = detailsBundleExtractor.getAllResourcesOfType(Observation.class);
    var familyHistories = detailsBundleExtractor.getAllResourcesOfType(FamilyMemberHistory.class);

    prescription.setAnalysis(analysis);
    prescription.setSequencings(sequencings);
    prescription.setPatients(patients);
    prescription.setProbandPatient(probandPatient);
    prescription.setPersons(persons);
    prescription.setProbandPerson(probandPerson);
    prescription.setFamilyMembers(familyMembers);
    prescription.setPractitioner(practitioner);
    prescription.setPractitionerRole(practitionerRole);
    prescription.setPerformer(performer);
    prescription.setOrganization(organization);
    prescription.setImpressions(impressions);
    prescription.setObservations(observations);
    prescription.setFamilyHistories(familyHistories);

    return prescription;
  }

}
