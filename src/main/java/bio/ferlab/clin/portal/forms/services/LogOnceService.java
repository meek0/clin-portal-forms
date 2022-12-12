package bio.ferlab.clin.portal.forms.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class LogOnceService {

  private final List<String> logs = new ArrayList<>();

  public synchronized void warn(String msg) {
    if (!logs.contains(msg)) {
      log.warn(msg);
      logs.add(msg);
    }
  }
}
