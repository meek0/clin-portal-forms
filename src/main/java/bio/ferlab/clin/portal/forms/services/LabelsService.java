package bio.ferlab.clin.portal.forms.services;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LabelsService extends AbstractBundleService{

  public LabelsService(LogOnceService logOnceService) {
    super("labels", "fr", List.of("fr"), logOnceService);
  }
}
