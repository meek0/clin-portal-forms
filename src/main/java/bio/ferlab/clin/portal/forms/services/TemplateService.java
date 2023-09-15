package bio.ferlab.clin.portal.forms.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.util.XRLog;
import io.pebbletemplates.pebble.PebbleEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TemplateService {

  static {
    // com.openhtmltopdf
    XRLog.setLoggingEnabled(false);
  }

  private final PebbleEngine engine;

  public String parseTemplate(String templateName, Map<String, Object> context, Locale locale) {
    try (StringWriter writer = new StringWriter()) {
      final var compiledTemplate = engine.getTemplate(templateName);
      compiledTemplate.evaluate(writer, context, locale);
      return writer.toString();
    } catch (IOException e) {
      throw new RuntimeException("Failed to parse template", e);
    }
  }

  public byte[] convert(String html) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      PdfRendererBuilder builder = new PdfRendererBuilder();
      builder.withHtmlContent(html, null);
      builder.toStream(out);
      builder.run();
      return out.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("Failed to convert HTML to PDF", e);
    }
  }

  public BufferedImage generateBarcodeImage(String barcodeText) {
    try {
      Code128Writer barcodeWriter = new Code128Writer();
      BitMatrix bitMatrix = barcodeWriter.encode(barcodeText, BarcodeFormat.CODE_128, 160, 50);
      return MatrixToImageWriter.toBufferedImage(bitMatrix);
    } catch (Exception e) {
      throw new RuntimeException("Failed to generate barcode of: " + barcodeText, e);
    }
  }

  public String convertToBase64(BufferedImage bufferedImage) {
    try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      ImageIO.write(bufferedImage, "png", baos);
      return Base64.getEncoder().encodeToString(baos.toByteArray());
    } catch (IOException e) {
      throw new RuntimeException("Failed to convert image into base64", e);
    }
  }
}
