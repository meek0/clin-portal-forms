package bio.ferlab.clin.portal.forms.utils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.ArrayList;
import java.util.List;

public class BundleExtractor {

  private final FhirContext fhirContext;
  private final IBaseBundle bundle;
  private int currentIndex = 0;
  
  public BundleExtractor(FhirContext fhirContext, IBaseBundle bundle)  {
    this.fhirContext = fhirContext;
    this.bundle = bundle;
  }

  public <T extends IBaseResource> List<T> getAllResourcesOfType(Class<T> clazz) {
    List<T> results = new ArrayList<>();
    List<IBaseResource> allRes = BundleUtil.toListOfResources(fhirContext, bundle);
    for(IBaseResource res : allRes) {
      if(res.getClass().equals(clazz)){
        results.add((T)res);
      }
    }
    return results;
  }
  
  public <T extends IBaseResource> T getNextResourcesOfType(Class<T> clazz) {
    List<IBaseResource> allRes = BundleUtil.toListOfResources(fhirContext, bundle);
    if (allRes.size() > currentIndex) {
      IBaseResource res = allRes.get(currentIndex++);
      if(res.getClass().equals(clazz)){
        return (T)res;
      }
    }
    return null;
  }
}
