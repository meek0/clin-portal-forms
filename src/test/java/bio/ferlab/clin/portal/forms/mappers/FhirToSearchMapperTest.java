package bio.ferlab.clin.portal.forms.mappers;

import bio.ferlab.clin.portal.forms.models.search.SearchPatient;
import bio.ferlab.clin.portal.forms.utils.FhirConst;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Person;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FhirToSearchMapperTest {
  
  final FhirToSearchMapper mapper = new FhirToSearchMapper();
  
  @Test
  void mapToSearch() {
    final Patient patient = new Patient();
    patient.getIdentifierFirstRep().setValue("mrn").getType().getCodingFirstRep().setSystem(FhirConst.SYSTEM_MRN).setCode(FhirConst.CODE_MRN);
    patient.setManagingOrganization(new Reference().setReference("Organization/foo"));
    
    final Person person = new Person();
    person.getIdentifierFirstRep().setValue("ramq").getType().getCodingFirstRep().setSystem(FhirConst.SYSTEM_RAMQ).setCode(FhirConst.CODE_RAMQ);
    final Date now = new Date();
    person.setBirthDate(now);
    person.getNameFirstRep().setFamily("lastname").addGiven("firstname");
    
    final SearchPatient search = mapper.mapToSearch(person, patient);
    
    assertEquals("firstname", search.getFirstName());
    assertEquals("foo", search.getEp());
    assertEquals("lastname", search.getLastName());
    assertEquals("foo", search.getEp());
    assertEquals(now, search.getBirthDate());
    assertEquals("ramq", search.getRamq());
    assertEquals("mrn", search.getMrn());
  }

}