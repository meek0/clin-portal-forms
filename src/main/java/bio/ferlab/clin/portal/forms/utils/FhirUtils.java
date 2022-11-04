package bio.ferlab.clin.portal.forms.utils;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Reference;

public class FhirUtils {
  
  private FhirUtils(){}
  
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
