package bio.ferlab.clin.portal.forms;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.io.IOUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonTestUtils {

  public static final ObjectMapper mapper = new ObjectMapper()
    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
    .registerModule(new JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  public static <T> T loadJsonResource(String resourcePath, Class<T> c) {
    try {
      var content = IOUtils.resourceToString(resourcePath, StandardCharsets.UTF_8, JsonTestUtils.class.getClassLoader());
      return mapper.readValue(content, c);
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> void assertJson(String resourcePath, T t) {
    try {
      var expected = loadJsonResource(resourcePath, t.getClass());
      assertEquals(formatToJson(expected), formatToJson(t));
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static String formatToJson(Object content) throws JsonProcessingException {
    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(content);
  }
}
