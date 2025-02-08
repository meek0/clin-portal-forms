package bio.ferlab.clin.portal.forms.controllers;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.mappers.TemplateMapper;
import bio.ferlab.clin.portal.forms.services.*;
import bio.ferlab.clin.portal.forms.utils.BundleExtractor;
import bio.ferlab.clin.portal.forms.utils.DateUtils;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
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
import java.util.*;

import static bio.ferlab.clin.portal.forms.utils.FhirConst.*;

@Controller
@RequestMapping("/render")
@RequiredArgsConstructor
public class RendererController {

  enum Format {
    html, pdf;
    boolean is(String format) {
      return this.name().equalsIgnoreCase(format);
    }
  }

  private final LocaleService localeService;
  private final LogOnceService logOnceService;
  private final FhirClient fhirClient;
  private final TemplateService templateService;
  private final CodesValuesService codesValuesService;
  private final MessagesService messagesService;
  private final PrescriptionService prescriptionService;

  @GetMapping(path = "/{id}")
  public ResponseEntity<?> render(@PathVariable String id, @RequestParam(defaultValue = "html") String format) {

    final var locale = LocaleService.DEFAULT_LOCALE; // localeService.getCurrentLocale();
    final var context =  prepareContext(id, locale);
    final var template = templateService.parseTemplate("index", context, locale);

    if (Format.html.is(format)) {
      return renderAsHtml(template);
    } else if (Format.pdf.is(format)) {
      return renderAsPdf(id, template);
    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported format: " + format);
    }
  }

  private Map<String, Object> prepareContext(String id, Locale locale) {
    final var prescription = prescriptionService.fromAnalysisId(id);
    final var analysis = prescription.getAnalysis();
    final var sequencings = prescription.getSequencings();
    final var performer = prescription.getPerformer();
    final var practitioner = prescription.getPractitioner();
    final var practitionerRole = prescription.getPractitionerRole();
    final var organization = prescription.getOrganization();
    final var patients = prescription.getPatients();
    final var persons = prescription.getPersons();
    final var familyMembers = prescription.getFamilyMembers();
    final var impressions = prescription.getImpressions();
    final var observations = prescription.getObservations();
    final var familyHistories = prescription.getFamilyHistories();

    final var analysisCodes = codesValuesService.getCodes(CodesValuesService.ANALYSE_KEY);

    // following code could also be placed inside Prescription model for easier access
    final var probandPatient = prescription.getProbandPatient();
    final var probandPerson = prescription.getProbandPerson();

    final var probandImpression = impressions.stream()
      .filter(s -> analysis.getSubject().getReference().equals(s.getSubject().getReference()))
      .findFirst().orElseThrow(() -> new RuntimeException("Can't find clinical impression for analysis: " + analysis.getIdElement().getIdPart() + " and subject: " + analysis.getSubject().getReference()));
    final var probandObservations = observations.stream()
      .filter(s -> analysis.getSubject().getReference().equals(s.getSubject().getReference()))
      .toList();
    final var probandFamilyHistories = familyHistories.stream()
      .filter(s -> analysis.getSubject().getReference().equals(s.getPatient().getReference()))
      .toList();

    // extract family infos
    var probandFamily = new ArrayList<FamilyMember>();
    for(var relation : familyMembers.keySet()) {
      var refs = familyMembers.get(relation);
      for (var ref : refs) {
        final var patient = patients.stream()
          .filter(s -> s.getIdElement().getIdPart().equals(FhirUtils.extractId(ref)))
          .findFirst().orElseThrow(() -> new RuntimeException("Can't find patient (" + relation + ") for analysis: " + analysis.getIdElement().getIdPart() + " and parent: " + ref.getReference()));
        final var person = persons.stream()
          .filter(s -> s.getLink().stream().anyMatch(l -> l.getTarget().getReference().equals(ref.getReference())))
          .findFirst().orElseThrow(() -> new RuntimeException("Can't find person (" + relation + ") for analysis: " + analysis.getIdElement().getIdPart() + " and parent: " + ref.getReference()));
        var sequencing = sequencings.stream()
          .filter(s -> ref.getReference().equals(s.getSubject().getReference()))
          .findFirst().orElseThrow(() -> new RuntimeException("Can't find sequencing (" + relation + ") for analysis: " + analysis.getIdElement().getIdPart() + " and parent: " + ref.getReference()));
        final var impression = impressions.stream()
          .filter(s -> ref.getReference().equals(s.getSubject().getReference()))
          .findFirst().orElseThrow(() -> new RuntimeException("Can't find clinical impression (" + relation + ") for analysis: " + analysis.getIdElement().getIdPart() + " and parent: " + ref.getReference()));
        final var obs = observations.stream()
          .filter(s -> ref.getReference().equals(s.getSubject().getReference()))
          .toList();
        final var histories = familyHistories.stream()
          .filter(s -> ref.getReference().equals(s.getPatient().getReference()))
          .toList();
        probandFamily.add(new FamilyMember(null, ref, relation, patient, person, sequencing, impression, obs, histories, null));
      }
    }

    var missingReasons = probandObservations.stream()
      .filter(o -> "social-history".equals(o.getCategoryFirstRep().getCodingFirstRep().getCode()))
      .filter(o -> SYSTEM_MISSING_PARENT.equals(o.getValueCodeableConcept().getCodingFirstRep().getSystem()))
      .toList();
    for(var missingReason : missingReasons) {
      var reason = missingReason.getNoteFirstRep().getText();
      var relation = StringUtils.substring(missingReason.getCode().getCodingFirstRep().getCode(), 1);
      if (StringUtils.isNoneBlank(reason, relation)) {
        probandFamily.add(new FamilyMember(reason, null, relation, null, null, null, null, null, null, missingReason.getValueCodeableConcept().getCodingFirstRep().getCode()));
      }
    }

    final Map<String, Object> context = new HashMap<>();

    context.put("analysis", analysis);

    context.put("performer", performer);
    context.put("practitionerRole", practitionerRole);
    context.put("practitioner", practitioner);
    context.put("organization", organization);

    if (!PRENATAL.equalsIgnoreCase(analysis.getCategoryFirstRep().getCodingFirstRep().getCode())) {
      final var probandSequencing = sequencings.stream()
        .filter(s -> analysis.getSubject().getReference().equals(s.getSubject().getReference()))
        .findFirst().orElseThrow(() -> new RuntimeException("Can't find sequencing for analysis: " + analysis.getIdElement().getIdPart() + " and subject: " + analysis.getSubject().getReference()));

      context.put("probandSequencing", probandSequencing);
    } else {
      final var foetusSequencing = sequencings.stream()
        .filter(s -> PRENATAL.equalsIgnoreCase(s.getCategoryFirstRep().getCodingFirstRep().getCode())).findFirst().orElse(null);

      if (foetusSequencing != null) {
        final Bundle fetusBundle = fhirClient.fetchFetusSequencingDetails(foetusSequencing);
        final var fetusExtractor = new BundleExtractor(fhirClient.getContext(), fetusBundle);
        final var fetus = fetusExtractor.getFirstResourcesOfType(Patient.class);

        context.put("probandSequencing", foetusSequencing);
        context.put("fetus", fetus);
      }
    }

    context.put("probandPatient", probandPatient);
    context.put("probandPerson", probandPerson);
    context.put("probandImpression", probandImpression);
    context.put("probandObservations", probandObservations);
    context.put("probandFamilyHistories", probandFamilyHistories);

    context.put("probandFamilyMembers", probandFamily);

    // don't know how thread-safe is Pebble renderer, let's instance a new mapper instead of having a singleton
    var mapper = new TemplateMapper(id, logOnceService, messagesService, templateService, codesValuesService, analysisCodes, locale);
    context.put("mapper", mapper);
    context.put("isPrenatalAnalysisCategory", analysis.hasCategory() && PRENATAL.equalsIgnoreCase(analysis.getCategoryFirstRep().getCodingFirstRep().getCode()));
    context.put("now", new Date());
    context.put("version", "1.0");
    context.put("totalPages", 2 + mapper.nonMissingFamilyMembers(probandFamily).size());

    FhirUtils.findExtension(analysis, SUPERVISOR_EXT).ifPresent(r -> {
      final Bundle bundle = fhirClient.findPractitionerAndRoleByRoleId(FhirUtils.extractId((Reference)r));
      final var extractor = new BundleExtractor(fhirClient.getContext(), bundle);
      context.put("supervisor", extractor.getFirstResourcesOfType(Practitioner.class));
      context.put("supervisorRole", extractor.getFirstResourcesOfType(PractitionerRole.class));
    });


    //context.put("analysisBarcodeBase64", templateService.convertToBase64(templateService.generateBarcodeImage(analysis.getIdElement().getIdPart())));
    //context.put("sequencingBarcodeBase64", templateService.convertToBase64(templateService.generateBarcodeImage(sequencing.getIdElement().getIdPart())));

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
      .contentType(MediaType.APPLICATION_PDF)
      .body(resource);
  }

  public record FamilyMember(String missingReason, Reference ref, String relation, Patient patient, Person person, ServiceRequest sequencing, ClinicalImpression clinicalImpression, List<Observation> observations, List<FamilyMemberHistory> familyHistories, String missingReasonType){}
}
