package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.mappers.FhirToSearchMapper;
import bio.ferlab.clin.portal.forms.models.search.SearchPrescription;
import bio.ferlab.clin.portal.forms.utils.BundleExtractor;
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
public class PrescriptionBuilder {
  
  private final FhirClient fhirClient;
  private final FhirToSearchMapper mapper;
  private final String practitionerId;
  private final String id;
  private final String ramq;
  
  public PrescriptionBuilder validate() {
    if (StringUtils.isAllBlank(id, ramq)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ramq and id can't be both empty");
    }
    return this;
  }
  
  public Result build() {
    List<SearchPrescription> prescriptions = new ArrayList<>();

    final PractitionerBuilder.Result roles = new PractitionerBuilder(fhirClient, practitionerId).build();
    final List<String> eps = roles.getPractitionerRoles().stream().map(r -> FhirUtils.extractId(r.getOrganization())).distinct().collect(Collectors.toList());
 
    if (StringUtils.isNotBlank(id)) {
      final Bundle bundle = this.fhirClient.findServiceRequestById(id);
      // extract analysis bundle
      final BundleExtractor extractor = new BundleExtractor(fhirClient.getContext(), bundle);
      final ServiceRequest analysis = extractor.getFirstResourcesOfType(ServiceRequest.class);
      final PractitionerRole practitionerRole = extractor.getFirstResourcesOfType(PractitionerRole.class);
      final Patient patient = extractor.getFirstResourcesOfType(Patient.class);
      // the following condition is important, if role exists and belongs to the user eps then we found one valid analysis
      if (practitionerRole != null  && eps.contains(FhirUtils.extractId(practitionerRole.getOrganization()))) {
        // find practitioner from the role
        final Practitioner practitioner = this.fhirClient.findPractitionerById(FhirUtils.extractId(practitionerRole.getPractitioner()));
        // find person + mother from patient
        final Bundle personBundle = this.fhirClient.findPersonByPatientId(patient.getIdElement().getIdPart());
        final Person person = new BundleExtractor(fhirClient.getContext(), personBundle).getFirstResourcesOfType(Person.class);
        final Bundle motherBundle = this.fhirClient.findRelatedPersonByPatientId(patient.getIdElement().getIdPart());
        final RelatedPerson mother = new BundleExtractor(fhirClient.getContext(), motherBundle).getFirstResourcesOfType(RelatedPerson.class);

        prescriptions.add(mapper.mapToSearchPrescription(analysis, practitioner, patient, person, mother));
      }
    } else if (StringUtils.isNotBlank(ramq)) {
      throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "search prescription by ramq isn't implemented yet");
    }
    return new Result(prescriptions);
  }
  
  @Getter
  @AllArgsConstructor
  public static class Result {
    private final List<SearchPrescription> prescriptions; 
  }
}
