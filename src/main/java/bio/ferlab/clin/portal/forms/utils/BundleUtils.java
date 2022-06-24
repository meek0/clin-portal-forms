package bio.ferlab.clin.portal.forms.utils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BundleUtils {

  public static <T extends IBaseResource> List<T> toListOfResourcesOfType(FhirContext context, IBaseBundle bundle, Class<T> clazz) {
    List<T> results = new ArrayList<>();
    List<IBaseResource> allRes = BundleUtil.toListOfResources(context, bundle);
    for(IBaseResource res: allRes) {
      if(res instanceof Bundle) {
        results.addAll(toListOfResourcesOfType(context, (Bundle)res, clazz));
      } else if(res.getClass().equals(clazz)){
        results.add((T)res);
      }
    }
    return results;
  }

  public static <T extends IBaseResource> T toResourcesOfType(FhirContext context, IBaseBundle bundle, Class<T> clazz) {
    return toListOfResourcesOfType(context, bundle, clazz).stream().findFirst().orElse(null);
  }
  
}
