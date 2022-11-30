package bio.ferlab.clin.portal.forms.utils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;

import java.util.ArrayList;
import java.util.List;

public class BundleExtractor {

  private final FhirContext fhirContext;
  private final Bundle bundle;
  private int currentIndex = 0;
  
  public BundleExtractor(FhirContext fhirContext, Bundle bundle)  {
    this.fhirContext = fhirContext;
    this.bundle = bundle;
  }

  public <T extends IBaseResource> List<T> getAllResourcesOfType(Class<T> clazz) {
    List<T> results = new ArrayList<>();
    List<IBaseResource> allRes = BundleUtil.toListOfResources(fhirContext, bundle);
    for(IBaseResource res : allRes) {
      if(res.getClass().equals(clazz)){
        results.add((T)res);
      } else if (res instanceof Bundle) { // search can return bundle inside bundle
        results.addAll(getAllResourcesOfTypeInBundle(clazz, (IBaseBundle) res));
      }
    }
    return results;
  }
  
  public synchronized <T extends IBaseResource> T getNextResourcesOfType(Class<T> clazz) {
    List<IBaseResource> allRes = BundleUtil.toListOfResources(fhirContext, bundle);
    if (allRes.size() > currentIndex) {
      IBaseResource res = allRes.get(currentIndex++);
      if(res.getClass().equals(clazz)){
        return (T)res;
      }
    }
    return null;
  }

  public <T extends IBaseResource> T getFirstResourcesOfType(Class<T> clazz) {
    List<IBaseResource> allRes = BundleUtil.toListOfResources(fhirContext, bundle);
    for(IBaseResource res : allRes) {
      if(res.getClass().equals(clazz)){
        return (T)res;
      } else if (res instanceof Bundle) { // search can return bundle inside bundle
        T fromBundle = getResourcesOfTypeInBundle(clazz, (IBaseBundle) res);
        if (fromBundle !=null) {
          return fromBundle;
        }
      }
    }
    return null;
  }
  
  public String extractFirstIdFromResponse(String type) {
    if (StringUtils.isNotBlank(type)) {
      return bundle.getEntry().stream()
        .map(Bundle.BundleEntryComponent::getResponse)
        .map(Bundle.BundleEntryResponseComponent::getLocation)
        .filter(StringUtils::isNotBlank)
        .filter(l -> l.startsWith(type + "/"))
        .findFirst()
        .map(FhirUtils::extractId).orElse(null);
    }
    return null;
  }

  private <T extends IBaseResource> List<T> getAllResourcesOfTypeInBundle(Class<T> clazz, IBaseBundle bundle) {
    List<T> results = new ArrayList<>();
    List<IBaseResource> allBundleRes = BundleUtil.toListOfResources(fhirContext, bundle);
    for (IBaseResource resBundle : allBundleRes) {
      if (resBundle.getClass().equals(clazz)) {
        results.add((T) resBundle);
      }
    }
    return results;
  }
  
  private <T extends IBaseResource> T getResourcesOfTypeInBundle(Class<T> clazz, IBaseBundle bundle) {
    List<T> all = getAllResourcesOfTypeInBundle(clazz, bundle);
    return all.isEmpty() ? null : all.get(0);
  }
}
