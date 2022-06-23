package bio.ferlab.clin.portal.forms.clients;

import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.client.api.IBasicClient;
import ca.uhn.fhir.rest.param.TokenParam;
import org.hl7.fhir.r4.model.Patient;

public interface IClinFhirClient extends IBasicClient {

  @Search(type= Patient.class)
  Patient findPatientByMrn(@RequiredParam(name = Patient.SP_IDENTIFIER) TokenParam theId);
}
