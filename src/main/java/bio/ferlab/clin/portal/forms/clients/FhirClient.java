package bio.ferlab.clin.portal.forms.clients;

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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;

@Component
@Getter
@Slf4j
public class FhirClient {
  
  private final FhirContext context;
  private final IGenericClient genericClient;
  private final FhirConfiguration fhirConfiguration;
  
  public FhirClient(FhirConfiguration configuration, FhirAuthInterceptor fhirAuthInterceptor) {
    context = FhirContext.forR4();
    context.getRestfulClientFactory().setConnectTimeout(configuration.getTimeout());
    context.getRestfulClientFactory().setSocketTimeout(configuration.getTimeout());
    context.getRestfulClientFactory().setPoolMaxTotal(configuration.getPoolSize());
    context.getRestfulClientFactory().setPoolMaxPerRoute(configuration.getPoolSize());
    context.setPerformanceOptions(PerformanceOptionsEnum.DEFERRED_MODEL_SCANNING);
    context.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);

    this.genericClient = context.newRestfulGenericClient(configuration.getUrl());
    this.fhirConfiguration = configuration;
    
    genericClient.registerInterceptor(fhirAuthInterceptor);
  }

  public void validate(IBaseResource resource) {
    OperationOutcome oo;
    try {
      oo = (OperationOutcome) getGenericClient().validate().resource(resource).encodedJson().execute().getOperationOutcome();
    } catch(PreconditionFailedException | UnprocessableEntityException e) {
      oo = (OperationOutcome) e.getOperationOutcome();
    }
    boolean containsError = oo.getIssue().stream()
        .anyMatch(issue -> EnumSet.of(OperationOutcome.IssueSeverity.ERROR, OperationOutcome.IssueSeverity.FATAL).contains(issue.getSeverity()));
    if (containsError) {
      // no need to return to the user the errors as they most likely come from bad mapping inside this API
      throw new RuntimeException("Validation of resource contains error:\n"+ toJson(resource)+"\n" + toJson(oo));
    }
  }
  
  public void submitForm(String personRef, String patientRef, Bundle bundle) {
    try {
      logDebug(bundle);
      if (fhirConfiguration.isValidate()) {
        validate(bundle);
      }
      log.info("Submit bundle for {} {} with {} entries",personRef, patientRef, bundle.getEntry().size());
      Bundle response = this.getGenericClient().transaction().withBundle(bundle).encodedJson().execute();
      logDebug(response);
    } catch(PreconditionFailedException | UnprocessableEntityException | InvalidRequestException e) {  // FHIR Server custom validation chain failed
      final String errors = toJson(e.getOperationOutcome());
      log.error("Failed to submit bundle:\n{}\n{}", toJson(bundle), errors);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errors);
    }
  }
  
  public Organization findOrganizationById(String id) {
    return this.getGenericClient().read().resource(Organization.class).withId(id).encodedJson().execute();
  }

  public CodeSystem findCodeSystemById(String id) {
    return this.getGenericClient().read().resource(CodeSystem.class).withId(id).encodedJson().execute();
  }
  
  public PractitionerRole findPractitionerRoleById(String id) {
    return this.getGenericClient().read().resource(PractitionerRole.class).withId(id).encodedJson().execute();
  }
  
  public Bundle findPractitionerRoleByPractitionerId(String practitionerId) {
    return this.getGenericClient().search().forResource(PractitionerRole.class)
        .where(PractitionerRole.PRACTITIONER.hasId(practitionerId)).returnBundle(Bundle.class).encodedJson().execute();
  }
  
  public Bundle findPersonAndPatientByRamq(String ramq) {
    return this.getGenericClient().search()
        .forResource(Person.class)
        .where(Person.IDENTIFIER.exactly().code(ramq))
        .include(Person.INCLUDE_PATIENT)
        .returnBundle(Bundle.class)
        .encodedJson()
        .execute();
  }
  
  public Bundle findPersonAndPatientByMrnAndEp(String mrn, String ep) {
    return this.getGenericClient().search()
        .forResource(Patient.class)
        .where(Patient.IDENTIFIER.exactly().code(mrn))
        .and(Patient.ORGANIZATION.hasId(ep))
        .revInclude(Person.INCLUDE_PATIENT)
        .returnBundle(Bundle.class)
        .encodedJson()
        .execute();
  }

  public String toJson(IBaseResource resource) {
    return getContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(resource);
  }
  
  public void logDebug(IBaseResource resource) {
    log.debug("JSON of {}\n{}", FhirUtils.formatResource(resource), toJson(resource));
  }
  
}
