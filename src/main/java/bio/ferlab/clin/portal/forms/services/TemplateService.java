package bio.ferlab.clin.portal.forms.services;

import io.pebbletemplates.pebble.PebbleEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TemplateService {

  private final PebbleEngine engine;

  public String parseTemplate(String templateName, Map<String, Object> context) {
    try (StringWriter writer = new StringWriter()) {
      final var compiledTemplate = engine.getTemplate(templateName);
      compiledTemplate.evaluate(writer, context);
      return writer.toString();
    } catch (IOException e) {
      throw new RuntimeException("Failed to parse template", e);
    }
  }

  public byte[] convert(String html) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      ITextRenderer renderer = new ITextRenderer();
      renderer.setDocumentFromString(html);
      renderer.layout();
      renderer.createPDF(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("Failed to convert HTML to PDF", e);
    }
  }
}
