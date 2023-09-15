package bio.ferlab.clin.portal.forms.services;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessagesService extends AbstractBundleService{

  public MessagesService(LogOnceService logOnceService) {
    super("messages", "", List.of("", "fr"), logOnceService);
  }

}
