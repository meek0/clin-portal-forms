package bio.ferlab.clin.portal.forms.controllers;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.services.TemplateService;
import bio.ferlab.clin.portal.forms.utils.DateUtils;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/render")
@RequiredArgsConstructor
public class RendererController {

  enum Format {
    html, pdf;
    boolean equals(String format) {
      return this.name().equalsIgnoreCase(format);
    }
  }

  private final FhirClient fhirClient;
  private final TemplateService templateService;

  @GetMapping(path = "/{id}")
  public ResponseEntity<?> render(@PathVariable String id, @RequestParam(defaultValue = "html") String format) {

    final String template = templateService.parseTemplate("index", prepareContext(id));

    if (Format.html.equals(format)) {
      return renderAsHtml(template);
    } else if (Format.pdf.equals(format)) {
      return renderAsPdf(id, template);
    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported format: " + format);
    }
  }

  private Map<String, Object> prepareContext(String id) {
    final var serviceRequest = fhirClient.findServiceRequestById(id);
    final Map<String, Object> context = new HashMap<>();
    context.put("id", FhirUtils.extractId(serviceRequest.getId()));
    context.put("lastUpdated", serviceRequest.getMeta().getLastUpdated());
    return context;
  }

  private ResponseEntity<String> renderAsHtml(String template) {
    return ResponseEntity.ok()
      .contentType(MediaType.TEXT_HTML)
      .body(template);
  }

  private ResponseEntity<ByteArrayResource> renderAsPdf(String id, String template) {
    byte[] pdf = templateService.convert(template);
    ByteArrayResource resource = new ByteArrayResource(pdf);
    final String fileName = String.format("%s_%s.pdf", id, LocalDateTime.now().format(DateUtils.FORMATTER_YYYYMMddTHHmmss));
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
      .contentLength(resource.contentLength())
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .body(resource);
  }
}
