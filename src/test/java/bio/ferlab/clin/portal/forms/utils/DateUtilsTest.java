package bio.ferlab.clin.portal.forms.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DateUtilsTest {

  @Test
  void convertDates() {
    assertNull(DateUtils.toDate(null));
    assertNull(DateUtils.toLocalDate(null));

    final var expectedLocalDate = LocalDate.of(2023,8, 31);
    final var convertedDate = DateUtils.toDate(expectedLocalDate);
    final var convertedLocalDate = DateUtils.toLocalDate(convertedDate);

    assertEquals(DateUtils.FORMATTER_YYYYMMdd.format(expectedLocalDate), DateUtils.FORMATTER_YYYYMMdd.format(DateUtils.toLocalDate(convertedDate)));
    assertEquals(expectedLocalDate, convertedLocalDate);
  }

}