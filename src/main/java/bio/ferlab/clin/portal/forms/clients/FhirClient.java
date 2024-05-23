package bio.ferlab.clin.portal.forms.clients;

import bio.ferlab.clin.portal.forms.configurations.CacheConfiguration;
import bio.ferlab.clin.portal.forms.configurations.FhirConfiguration;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.PerformanceOptionsEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.List;
import java.util.TreeMap;

import static bio.ferlab.clin.portal.forms.utils.FhirConst.*;

@Component
@Getter
@Slf4j
public class FhirClient {

  private final FhirContext context;
  private final IGenericClient genericClient;
  private final FhirConfiguration fhirConfiguration;

  public FhirClient(FhirConfiguration configuration, FhirAuthInterceptor fhirAuthInterceptor) {
    context = FhirContext.forR4();

    context.setPerformanceOptions(PerformanceOptionsEnum.DEFERRED_MODEL_SCANNING);
    context.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);

    context.getRestfulClientFactory().setConnectTimeout(configuration.getTimeout());
    context.getRestfulClientFactory().setConnectionRequestTimeout(configuration.getTimeout());
    context.getRestfulClientFactory().setSocketTimeout(configuration.getTimeout());
    context.getRestfulClientFactory().setPoolMaxTotal(configuration.getPoolSize());
    context.getRestfulClientFactory().setPoolMaxPerRoute(configuration.getPoolSize());

    this.genericClient = context.newRestfulGenericClient(configuration.getUrl());
    this.fhirConfiguration = configuration;

    genericClient.registerInterceptor(fhirAuthInterceptor);
  }

  public void validate(IBaseResource resource) {
    OperationOutcome oo;
    try {
      oo = (OperationOutcome) getGenericClient().validate().resource(resource).execute().getOperationOutcome();
    } catch(PreconditionFailedException | UnprocessableEntityException e) {
      oo = (OperationOutcome) e.getOperationOutcome();
    }
    boolean containsError = oo.getIssue().stream()
        .anyMatch(issue -> EnumSet.of(OperationOutcome.IssueSeverity.ERROR, OperationOutcome.IssueSeverity.FATAL).contains(issue.getSeverity()));
    if (containsError) {
      final String errors = toJson(oo);
      log.debug("Failed to validate resource:\n{}", errors);  // don't log in production <= sensitive data
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errors);
    }
  }

  public ServiceRequest assignPerformers(ServiceRequest serviceRequest) {
    log.info("Update service request {} with performers {}", serviceRequest.getIdElement().getIdPart(), serviceRequest.getPerformer().stream().map((Reference::getReference)).toList());
    final var outcome = this.genericClient.update().resource(serviceRequest).execute();
    return (ServiceRequest) outcome.getResource();
  }

  public Bundle submitForm(String personRef, String patientRef, Bundle bundle) {
    try {
      logDebug(bundle);
      if (fhirConfiguration.isValidate()) {
        validate(bundle);
      }
      log.info("Submit bundle for {} {} with {} entries",personRef, patientRef, bundle.getEntry().size());
      Bundle response = this.getGenericClient().transaction().withBundle(bundle).execute();
      logDebug(response);
      return response;
    } catch(PreconditionFailedException | UnprocessableEntityException | InvalidRequestException e) {  // FHIR Server custom validation chain failed
      final String errors = toJson(e.getOperationOutcome());
      log.debug("Failed to submit bundle:\n{}", errors);  // don't log in production <= sensitive data
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errors);
    }
  }

  @Cacheable(value = CacheConfiguration.CACHE_ROLES, sync = true, keyGenerator = "customKeyGenerator")
  public PractitionerRole findPractitionerRoleById(String id) {
    log.debug("Fetch practitioner role by id: {}", id);
    return this.getGenericClient().read().resource(PractitionerRole.class).withId(id).execute();
  }

  @Cacheable(value = CacheConfiguration.CACHE_ROLES, sync = true, keyGenerator = "customKeyGenerator")
  public Bundle findPractitionerAndRoleByRoleId(String id) {
    log.debug("Fetch practitioner and role by role id: {}", id);
    return this.getGenericClient().search().forResource(PractitionerRole.class)
      .where(PractitionerRole.RES_ID.exactly().code(id))
      .include(PractitionerRole.INCLUDE_PRACTITIONER)
      .returnBundle(Bundle.class).execute();
  }

  @Cacheable(value = CacheConfiguration.CACHE_ROLES, sync = true, keyGenerator = "customKeyGenerator")
  public Bundle findAllPractitionerRoles() {
    log.info("Fetch all practitioner roles");
    return this.getGenericClient().search().forResource(PractitionerRole.class)
      .count(Integer.MAX_VALUE)
      .returnBundle(Bundle.class).execute();
  }

  @Cacheable(value = CacheConfiguration.CACHE_ROLES, sync = true, keyGenerator = "customKeyGenerator")
  public Bundle findPractitionerRoleByPractitionerId(String practitionerId) {
    log.debug("Fetch practitioner roles by practitioner id: {}", practitionerId);
    return this.getGenericClient().search().forResource(PractitionerRole.class)
        .where(PractitionerRole.PRACTITIONER.hasId(practitionerId)).returnBundle(Bundle.class).execute();
  }

  @Cacheable(value = CacheConfiguration.CACHE_ROLES, sync = true, keyGenerator = "customKeyGenerator")
  public Bundle findPractitionerAndRoleByEp(String ep) {
    log.debug("Fetch practitioner and roles by ep: {}", ep);
    return this.getGenericClient().search().forResource(PractitionerRole.class)
        .where(PractitionerRole.ORGANIZATION.hasId(ep))
        .include(PractitionerRole.INCLUDE_PRACTITIONER)
        .count(Integer.MAX_VALUE)
        .returnBundle(Bundle.class).execute();
  }

  @Cacheable(value = CacheConfiguration.CACHE_FHIR, sync = true, keyGenerator = "customKeyGenerator")
  public Bundle fetchAdditionalPrescriptionData(String practitionerId, String patientId) {
    log.debug("Fetch additional prescription data practitionerId: {} patient: {}", practitionerId, patientId);
    final Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.BATCH);

    bundle.addEntry().getRequest()
        .setUrl("Practitioner/" + practitionerId)
        .setMethod(Bundle.HTTPVerb.GET);

    bundle.addEntry().getRequest()
        .setUrl("Person?link=" + patientId)
        .setMethod(Bundle.HTTPVerb.GET);

    bundle.addEntry().getRequest()
        .setUrl("RelatedPerson?patient=" + patientId)
        .setMethod(Bundle.HTTPVerb.GET);

    return this.getGenericClient().transaction().withBundle(bundle).execute();
  }

  @Cacheable(value = CacheConfiguration.CACHE_FHIR, sync = true, keyGenerator = "customKeyGenerator")
  public Bundle fetchServiceRequestsByPatientIds(List<String> patientIds) {
    log.debug("Fetch service request by patient ids: {}", patientIds);
    final Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.BATCH);

    patientIds.forEach(id -> {
      bundle.addEntry().getRequest()
          .setUrl("ServiceRequest?patient=" + id)
          .setMethod(Bundle.HTTPVerb.GET);
    });

    return this.getGenericClient().transaction().withBundle(bundle).execute();
  }

  @Cacheable(value = CacheConfiguration.CACHE_FHIR, sync = true, keyGenerator = "customKeyGenerator")
  public Bundle findServiceRequestWithDepsById(String id) {
    log.debug("Fetch service request with deps by id: {}", id);
    return this.getGenericClient().search().forResource(ServiceRequest.class)
        .where(ServiceRequest.RES_ID.exactly().code(id))
        .include(ServiceRequest.INCLUDE_REQUESTER)
        .include(ServiceRequest.INCLUDE_PERFORMER)
        .returnBundle(Bundle.class)
        .execute();
  }

  @Cacheable(value = CacheConfiguration.CACHE_FHIR, sync = true, keyGenerator = "customKeyGenerator")
  public ServiceRequest findServiceRequestById(String id) {
    log.debug("Fetch service request by id: {}", id);
    return this.getGenericClient().read().resource(ServiceRequest.class).withId(id).execute();
  }

  @Cacheable(value = CacheConfiguration.CACHE_FHIR, sync = true, keyGenerator = "customKeyGenerator")
  public Bundle findPersonAndPatientByRamq(String ramq) {
    log.debug("Fetch person and patient by ramq: {}", ramq);
    return this.getGenericClient().search()
        .forResource(Person.class)
        .where(Person.IDENTIFIER.exactly().code(ramq))
        .include(Person.INCLUDE_PATIENT)
        .returnBundle(Bundle.class)
        .execute();
  }

  @Cacheable(value = CacheConfiguration.CACHE_FHIR, sync = true, keyGenerator = "customKeyGenerator")
  public Bundle findPersonAndPatientByMrnAndEp(String mrn, String ep) {
    log.debug("Fetch person and patient by mrn and ep: {} {}", mrn, ep);
    return this.getGenericClient().search()
        .forResource(Patient.class)
        .where(Patient.IDENTIFIER.exactly().code(mrn))
        .and(Patient.ORGANIZATION.hasId(ep))
        .revInclude(Person.INCLUDE_PATIENT)
        .returnBundle(Bundle.class)
        .execute();
  }

  @Cacheable(value = CacheConfiguration.CACHE_FHIR, sync = true, keyGenerator = "customKeyGenerator")
  public Bundle fetchPrescriptionDetails(ServiceRequest analysis, PractitionerRole practitionerRole, TreeMap<String, Reference> familyMembers) {
    log.debug("Fetch prescription details for id: {}", analysis.getIdElement().getIdPart());
    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.BATCH);

    // Mandatory for FHIR Analysis has to be part of the response
    // cf: PrescriptionMasking and MetaTagPerson
    bundle.addEntry().getRequest()
      .setUrl(String.format(String.format("ServiceRequest/%s?_profile=%s", analysis.getIdElement().getIdPart(), ANALYSIS_SERVICE_REQUEST)))
      .setMethod(Bundle.HTTPVerb.GET);

    bundle.addEntry().getRequest()
      .setUrl(String.format("Patient/%s", FhirUtils.extractId(analysis.getSubject())))
      .setMethod(Bundle.HTTPVerb.GET);

    bundle.addEntry().getRequest()
      .setUrl(String.format("ServiceRequest?based-on=%s&_profile=%s", FhirUtils.formatResource(analysis), SEQUENCING_SERVICE_REQUEST))
      .setMethod(Bundle.HTTPVerb.GET);

    bundle.addEntry().getRequest()
      .setUrl(String.format("Person?link=%s", analysis.getSubject().getReference()))
      .setMethod(Bundle.HTTPVerb.GET);

    if (practitionerRole != null) {
      bundle.addEntry().getRequest()
        .setUrl(String.format("Organization/%s", FhirUtils.extractId(practitionerRole.getOrganization())))
        .setMethod(Bundle.HTTPVerb.GET);

      bundle.addEntry().getRequest()
        .setUrl(String.format("Practitioner/%s", FhirUtils.extractId(practitionerRole.getPractitioner())))
        .setMethod(Bundle.HTTPVerb.GET);
    }

    for (var ref : analysis.getSupportingInfo()) {
      if (ref.getReference().startsWith("ClinicalImpression")) {
        bundle.addEntry().getRequest()
          .setUrl(String.format("ClinicalImpression?_id=%s&_include=ClinicalImpression:investigation", FhirUtils.extractId(ref)))
          .setMethod(Bundle.HTTPVerb.GET);
      }
    }

    for(var familyMember : familyMembers.values()) {
      bundle.addEntry().getRequest()
        .setUrl(String.format("Patient/%s", FhirUtils.extractId(familyMember)))
        .setMethod(Bundle.HTTPVerb.GET);
      bundle.addEntry().getRequest()
        .setUrl(String.format("Person?link=%s", familyMember.getReference()))
        .setMethod(Bundle.HTTPVerb.GET);
    }

    return this.getGenericClient().transaction().withBundle(bundle).execute();
  }

  public Bundle fetchFetusSequencingDetails(ServiceRequest sequencing) {
    log.debug("Fetch sequencing details for id: {}", sequencing.getIdElement().getIdPart());
    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.BATCH);

    bundle.addEntry().getRequest()
      .setUrl(String.format("Patient/%s", FhirUtils.extractId(sequencing.getSubject())))
      .setMethod(Bundle.HTTPVerb.GET);

    return this.getGenericClient().transaction().withBundle(bundle).execute();
  }

  @Cacheable(value = CacheConfiguration.CACHE_CODES_VALUES, sync = true, keyGenerator = "customKeyGenerator")
  public Bundle fetchCodesAndValues() {
    log.info("Fetch codes and values from FHIR");

    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.BATCH);

    bundle.addEntry().getRequest()
        .setUrl("CodeSystem/analysis-request-code")
        .setMethod(Bundle.HTTPVerb.GET);

    bundle.addEntry().getRequest()
        .setUrl("CodeSystem/hp")
        .setMethod(Bundle.HTTPVerb.GET);

    bundle.addEntry().getRequest()
        .setUrl("CodeSystem/fmh-relationship-plus")
        .setMethod(Bundle.HTTPVerb.GET);

    bundle.addEntry().getRequest()
        .setUrl("CodeSystem/qc-ethnicity")
        .setMethod(Bundle.HTTPVerb.GET);

    bundle.addEntry().getRequest()
        .setUrl("CodeSystem/observation-code")
        .setMethod(Bundle.HTTPVerb.GET);

    bundle.addEntry().getRequest()
        .setUrl("ValueSet/age-at-onset")
        .setMethod(Bundle.HTTPVerb.GET);

    bundle.addEntry().getRequest()
      .setUrl("ValueSet/fmh-relationship")
      .setMethod(Bundle.HTTPVerb.GET);

    for(String byType: fhirConfiguration.getTypesWithDefault()) {
      bundle.addEntry().getRequest()
          .setUrl("ValueSet/" + byType + DEFAULT_HPO_SUFFIX)
          .setMethod(Bundle.HTTPVerb.GET);
      bundle.addEntry().getRequest()
          .setUrl("ValueSet/" + byType + DEFAULT_EXAM_SUFFIX)
          .setMethod(Bundle.HTTPVerb.GET);
    }

    for(String byType: fhirConfiguration.getMultiValuesObservationCodes()) {
      bundle.addEntry().getRequest()
          .setUrl("ValueSet/" + byType + ABNORMALITIES_SUFFIX)
          .setMethod(Bundle.HTTPVerb.GET);
    }

    return this.getGenericClient().transaction().withBundle(bundle).execute();
  }

  public String toJson(IBaseResource resource) {
    return getContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(resource);
  }

  public void logDebug(IBaseResource resource) {
    log.debug("JSON of {}\n{}", FhirUtils.formatResource(resource), toJson(resource));
  }

}
