package bio.ferlab.clin.portal.forms.mappers;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@Component
public class SubmitToFhirMapper {
  
  private ZoneId zoneId = ZoneId.systemDefault();
  
  public Date toDate(LocalDate localDate) {
    return Date.from(localDate.atStartOfDay(zoneId).toInstant());
  }
}
