package bio.ferlab.clin.portal.forms.utils;

import bio.ferlab.clin.portal.forms.models.builders.ObservationsBuilder;
import bio.ferlab.clin.portal.forms.models.submit.Parent;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;

import java.util.Optional;

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

  public static ObservationsBuilder.Affected toAffected(Parent.Status status) {
    return Optional.ofNullable(status).map((s) -> switch (s) {
      case affected -> ObservationsBuilder.Affected.POS;
      case not_affected -> ObservationsBuilder.Affected.NEG;
      case unknown -> ObservationsBuilder.Affected.IND;
    }).orElse(null);
  }

  public static ObservationsBuilder.Affected toAffected(Boolean isObserved) {
    if (Boolean.TRUE.equals(isObserved)) {
      return ObservationsBuilder.Affected.POS;
    } else if (Boolean.FALSE.equals(isObserved)) {
      return ObservationsBuilder.Affected.NEG;
    } else {
      return null;
    }
  }
}
