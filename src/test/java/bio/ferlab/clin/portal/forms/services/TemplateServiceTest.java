package bio.ferlab.clin.portal.forms.services;

import io.pebbletemplates.pebble.PebbleEngine;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TemplateServiceTest {

  final PebbleEngine engine = new PebbleEngine.Builder().build();
  final TemplateService service = new TemplateService(engine);

  @Test
  void parseTemplate() {
    final var result = service.parseTemplate("templates/test.peb", Map.of("value", "foo"), null);
    assertEquals("<p>foo</p>", result);
  }

  @Test
  void convert() {
    assertTrue(service.convert("<p>foo</p>").length > 0);
  }

  @Test
  void generateBarcodeImage() {
    assertNotNull(service.generateBarcodeImage("foo"));
  }

  @Test
  void convertToBase64() {
    assertTrue(!service.convertToBase64(service.generateBarcodeImage("foo")).isEmpty());
  }
}