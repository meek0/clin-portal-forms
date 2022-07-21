package bio.ferlab.clin.portal.forms.utils;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Reference;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Slf4j
public class FhirUtils {
  
  public static void validate(FhirClient fhirClient, IBaseResource resource) {
    List<String> errors = new ArrayList<>();
    OperationOutcome oo;
    try {
      oo = (OperationOutcome) fhirClient.getGenericClient().validate().resource(resource).encodedJson().execute().getOperationOutcome();
    } catch(PreconditionFailedException | UnprocessableEntityException e) {
      oo = (OperationOutcome) e.getOperationOutcome();
    }
    for (OperationOutcome.OperationOutcomeIssueComponent nextIssue : oo.getIssue()) {
      if (EnumSet.of(OperationOutcome.IssueSeverity.ERROR, OperationOutcome.IssueSeverity.FATAL).contains(nextIssue.getSeverity())) {
        errors.add(nextIssue.getDiagnostics());
      } else if (OperationOutcome.IssueSeverity.WARNING.equals(nextIssue.getSeverity())){
        log.warn("Validation of resource {} : {}", formatResource(resource), nextIssue.getDiagnostics());
      }
    }

    if (!errors.isEmpty()) {
      throw new RuntimeException("Validation of resource "+ formatResource(resource)+" errors:\n" + StringUtils.join(errors, "\n"));
    }
  }
  
  public static void logDebug(FhirClient fhirClient, IBaseResource resource) {
    log.debug("JSON of {}\n{}", formatResource(resource), fhirClient.getContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(resource));
  }
  
  public static String formatResource(IBaseResource resource) {
    return String.format("%s/%s", resource.fhirType(), resource.getIdElement().getIdPart());
  }

  public static Reference toReference(IBaseResource resource) {
    return new Reference(formatResource(resource));
  }
}
