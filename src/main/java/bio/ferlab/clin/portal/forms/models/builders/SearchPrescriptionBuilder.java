package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.mappers.FhirToSearchMapper;
import bio.ferlab.clin.portal.forms.models.search.SearchPrescription;
import bio.ferlab.clin.portal.forms.utils.BundleExtractor;
import bio.ferlab.clin.portal.forms.utils.FhirConst;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class SearchPrescriptionBuilder {
  
  private final FhirClient fhirClient;
  private final FhirToSearchMapper mapper;
  private final String practitionerId;
  private final String id;
  private final String ramq;
  
  public SearchPrescriptionBuilder validate() {
    if (StringUtils.isAllBlank(id, ramq)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ramq and id can't be both empty");
    }
    return this;
  }
  
  public Result build() {
    List<SearchPrescription> prescriptions = new ArrayList<>();
    
    if (StringUtils.isNotBlank(id)) {
      // first build the list of ep the user can access
      final PractitionerBuilder.Result roles = new PractitionerBuilder(fhirClient, practitionerId).build();
      final List<String> eps = roles.getPractitionerRoles().stream().map(r -> FhirUtils.extractId(r.getOrganization())).distinct().collect(Collectors.toList());

      final Bundle bundle = this.fhirClient.findServiceRequestWithDepsById(id);
      // extract analysis bundle
      final BundleExtractor extractor = new BundleExtractor(fhirClient.getContext(), bundle);
      final ServiceRequest analysis = extractor.getFirstResourcesOfType(ServiceRequest.class);
      final PractitionerRole practitionerRole = extractor.getFirstResourcesOfType(PractitionerRole.class);
      final Patient patient = extractor.getFirstResourcesOfType(Patient.class);
      // the following condition is important, if role exists and belongs to the user eps then we found one valid analysis
      if (isAnalysis(analysis) && isValidEp(practitionerRole, eps)) {
        final Bundle allBundle = this.fhirClient.fetchAdditionalPrescriptionData(FhirUtils.extractId(practitionerRole.getPractitioner()), patient.getIdElement().getIdPart());
        final BundleExtractor allExtractor = new BundleExtractor(this.fhirClient.getContext(), allBundle);
        final Practitioner practitioner = allExtractor.getFirstResourcesOfType(Practitioner.class);
        final Person person = allExtractor.getFirstResourcesOfType(Person.class);
        final RelatedPerson mother = allExtractor.getFirstResourcesOfType(RelatedPerson.class);
        prescriptions.add(mapper.mapToSearchPrescription(analysis, practitioner, practitionerRole, patient, person, mother));
      }
    } else if (StringUtils.isNotBlank(ramq)) {
      // find all patient linked to person with the ramq, we can have 2 patients with the same ramq in 2 EPs
      Bundle bundle = this.fhirClient.findPersonAndPatientByRamq(ramq);
      BundleExtractor bundleExtractor = new BundleExtractor(fhirClient.getContext(), bundle);
      final List<Patient> patients = bundleExtractor.getAllResourcesOfType(Patient.class);
      final List<String> patientIds = patients.stream().map(p -> p.getIdElement().getIdPart()).distinct().toList();
      // find all service requests for every patient id
      final Bundle allBundle = this.fhirClient.fetchServiceRequestsByPatientIds(patientIds);
      final List<ServiceRequest> serviceRequests = new BundleExtractor(fhirClient.getContext(), allBundle).getAllResourcesOfType(ServiceRequest.class);
      final List<String> serviceRequestIds = serviceRequests.stream().map(sr -> sr.getIdElement().getIdPart()).distinct().toList();
      // Easy way is to call SearchPrescriptionBuilder again for every IDs
      serviceRequestIds.forEach(srId -> prescriptions.addAll(new SearchPrescriptionBuilder(fhirClient, mapper, practitionerId, srId, null).validate().build().getPrescriptions()));
    }
    return new Result(prescriptions);
  }
  
  private boolean isValidEp(PractitionerRole role, List<String> eps) {
    return role != null  && eps.contains(FhirUtils.extractId(role.getOrganization()));
  }

  private boolean isAnalysis(ServiceRequest serviceRequest) {
    return serviceRequest != null && serviceRequest.getMeta().getProfile().stream().anyMatch(s -> FhirConst.ANALYSIS_SERVICE_REQUEST.equals(s.getValue()));
  }
  
  @Getter
  @AllArgsConstructor
  public static class Result {
    private final List<SearchPrescription> prescriptions; 
  }
}
