package bio.ferlab.clin.portal.forms.controllers;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.mappers.TemplateMapper;
import bio.ferlab.clin.portal.forms.services.CodesValuesService;
import bio.ferlab.clin.portal.forms.services.LocaleService;
import bio.ferlab.clin.portal.forms.services.TemplateService;
import bio.ferlab.clin.portal.forms.utils.BundleExtractor;
import bio.ferlab.clin.portal.forms.utils.DateUtils;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.*;
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
import java.util.Locale;
import java.util.Map;

import static bio.ferlab.clin.portal.forms.utils.FhirConst.SUPERVISOR_EXT;

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

  private final LocaleService localeService;
  private final FhirClient fhirClient;
  private final TemplateService templateService;
  private final CodesValuesService codesValuesService;

  @GetMapping(path = "/{id}")
  public ResponseEntity<?> render(@PathVariable String id, @RequestParam(defaultValue = "html") String format) {

    final Locale locale = localeService.getCurrentLocale();
    final String template = templateService.parseTemplate("index", prepareContext(id, locale), locale);

    if (Format.html.equals(format)) {
      return renderAsHtml(template);
    } else if (Format.pdf.equals(format)) {
      return renderAsPdf(id, template);
    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported format: " + format);
    }
  }

  private Map<String, Object> prepareContext(String id, Locale locale) {
    final var mainBundle = fhirClient.findServiceRequestWithDepsById(id);
    final var mainBundleExtractor = new BundleExtractor(fhirClient.getContext(), mainBundle);
    final var analysis = mainBundleExtractor.getFirstResourcesOfType(ServiceRequest.class);
    final var patient = mainBundleExtractor.getFirstResourcesOfType(Patient.class);
    final var performer = mainBundleExtractor.getFirstResourcesOfType(Organization.class);
    // Assignation feature will attach several PractitionerRole insider performer BUT we want the one from requester
    final var practitionerRole = mainBundleExtractor.getFirstResourcesById(PractitionerRole.class, FhirUtils.extractId(analysis.getRequester()));
    // if the user doesn't belong to the EP/LDM
    if (analysis == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prescription not found: " + id);

    final var detailsBundle = fhirClient.fetchPrescriptionDetails(analysis, patient, practitionerRole);
    final var detailsBundleExtractor = new BundleExtractor(fhirClient.getContext(), detailsBundle);
    final var sequencings = detailsBundleExtractor.getAllResourcesOfType(ServiceRequest.class);
    final var person = detailsBundleExtractor.getFirstResourcesOfType(Person.class);
    final var practitioner = detailsBundleExtractor.getFirstResourcesOfType(Practitioner.class);
    final var organization = detailsBundleExtractor.getFirstResourcesOfType(Organization.class);

    final var analysisCodes = codesValuesService.getCodes(CodesValuesService.ANALYSE_KEY);

    final Map<String, Object> context = new HashMap<>();
    context.put("id", analysis.getIdElement().getIdPart());
    context.put("analysis", analysis);
    context.put("sequencings", sequencings);
    context.put("performer", performer);
    context.put("practitionerRole", practitionerRole);
    context.put("patient", patient);
    context.put("person", person);
    context.put("practitioner", practitioner);
    context.put("organization", organization);
    context.put("mapper", new TemplateMapper(analysisCodes, locale));

    FhirUtils.findExtension(analysis, SUPERVISOR_EXT).ifPresent(r -> {
      final Bundle bundle = fhirClient.findPractitionerAndRoleByRoleId(FhirUtils.extractId((Reference)r));
      final var extractor = new BundleExtractor(fhirClient.getContext(), bundle);
      context.put("supervisor", extractor.getFirstResourcesOfType(Practitioner.class));
      context.put("supervisorRole", extractor.getFirstResourcesOfType(PractitionerRole.class));
    });

    context.put("idBarcodeBase64", templateService.convertToBase64(templateService.generateBarcodeImage(id)));

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
