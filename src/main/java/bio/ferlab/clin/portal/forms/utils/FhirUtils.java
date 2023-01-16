package bio.ferlab.clin.portal.forms.utils;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;

import static bio.ferlab.clin.portal.forms.utils.FhirConst.DOCTOR_PREFIX;

public class FhirUtils {
  
  private FhirUtils(){}

  public static boolean isDoctor(PractitionerRole role, String ep) {
    final String orgRef = FhirUtils.formatResource(new Organization().setId(ep));
    return role != null && DOCTOR_PREFIX.equals(role.getCodeFirstRep().getCodingFirstRep().getCode())
      && orgRef.equals(role.getOrganization().getReference());
  }
  
  public static String formatResource(IBaseResource resource) {
    return String.format("%s/%s", resource.fhirType(), resource.getIdElement().getIdPart());
  }

  public static Reference toReference(IBaseResource resource) {
    return new Reference(formatResource(resource));
  }
  
  public static String extractId(String url) {
    return extractId(new Reference(url));
  }
  
  public static String extractId(Reference reference) {
    if (reference != null) {
      final String ref = reference.getReference();
      if (StringUtils.isNotBlank(ref) && ref.contains("/")) {
        return ref.split("/")[1];
      }
    }
    return null;
  }
  
  public static String sanitizeNoteComment(String comment) {
    return StringUtils.isNotBlank(comment) ? comment : "--";
  }
}
