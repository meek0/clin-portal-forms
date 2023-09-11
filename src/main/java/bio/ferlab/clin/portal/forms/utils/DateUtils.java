package bio.ferlab.clin.portal.forms.utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;

public class DateUtils {

  public static final ZoneId ZONE_ID_DEFAULT = ZoneId.systemDefault();
  public static final DateTimeFormatter FORMATTER_YYYYMMdd = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  public static final DateTimeFormatter FORMATTER_YYYYMMddTHHmmss = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

  public static Date toDate(LocalDate localDate) {
    return Optional.ofNullable(localDate).map(d -> Date.from(d.atStartOfDay(ZONE_ID_DEFAULT).toInstant())).orElse(null);
  }

  public static LocalDate toLocalDate(Date date) {
    return Optional.ofNullable(date).map(d -> date.toInstant().atZone(ZONE_ID_DEFAULT).toLocalDate()).orElse(null);
  }
}
