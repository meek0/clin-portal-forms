package bio.ferlab.clin.portal.forms.mappers;

import bio.ferlab.clin.portal.forms.services.CodesValuesService;
import bio.ferlab.clin.portal.forms.services.LogOnceService;
import bio.ferlab.clin.portal.forms.services.MessagesService;
import bio.ferlab.clin.portal.forms.services.TemplateService;
import bio.ferlab.clin.portal.forms.utils.DateUtils;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;

import java.util.*;

import static bio.ferlab.clin.portal.forms.models.builders.ReflexBuilder.REFLEX_PANEL_PREFIX_EN;
import static bio.ferlab.clin.portal.forms.models.builders.ReflexBuilder.REFLEX_PANEL_PREFIX_FR;
import static bio.ferlab.clin.portal.forms.utils.FhirConst.*;

@Slf4j
@RequiredArgsConstructor
public class TemplateMapper {

  public static final String EMPTY = "";

  private final String id;
  private final LogOnceService logOnceService;
  private final MessagesService messagesService;
  private final TemplateService templateService;
  private final CodesValuesService codesValuesService;
  private final CodeSystem analysisCodes;
  private final Locale locale;

  public String mapToBarcodeBase64(String value) {
    return templateService.convertToBase64(templateService.generateBarcodeImage(value));
  }

  public String mapToAddress(Organization organization) {
    try {
      var addr =  organization.getContactFirstRep().getAddress().getText();
      return Optional.ofNullable(addr).orElse(EMPTY);
    } catch (Exception e) {
      return handleError(e);
    }
  }

  public String mapToGender(Person person) {
    try {
      return i18n(person.getGender().toCode());
    } catch (Exception e) {
      return this.handleError(e);
    }
  }

  public String mapToRole(PractitionerRole role) {
    try {
      var code = role.getCodeFirstRep().getCodingFirstRep().getCode();
      var res = RESIDENT_PREFIX.equals(code) ? i18n(role.getCodeFirstRep().getCodingFirstRep().getCode()): "";
      return StringUtils.isNotBlank(res) ? "("+res+")" : EMPTY;
    }catch ( Exception e) {
      return this.handleError(e);
    }
  }

  public String mapToName(Person person) {
    try {
      return formatName(person.getNameFirstRep(), false);
    } catch (Exception e) {
      return this.handleError(e);
    }
  }

  public String mapToRAMQ(Person person) {
    try {
      return getIdentifier(person.getIdentifier(), CODE_RAMQ).map(r -> String.format("%s %s %s", r.substring(0,4), r.substring(4,8), r.substring(8,12)).toUpperCase()).orElse(EMPTY);
    } catch (Exception e) {
      return this.handleError(e);
    }
  }

  public String mapToMRN(Patient patient) {
    try {
      var org = Optional.ofNullable(FhirUtils.extractId(patient.getManagingOrganization())).orElse(EMPTY).toUpperCase();
      var mrn = getIdentifier(patient.getIdentifier(), CODE_MRN).map(r -> r.toUpperCase().replace("MRN-", "")).orElse(EMPTY);
      return StringUtils.isNotBlank(org) ? mrn + " | "+ org : mrn;
    } catch (Exception e) {
      return this.handleError(e);
    }
  }

  public String formatDate(Date date) {
    try {
      return DateUtils.FORMATTER_YYYYMMdd.format(DateUtils.toLocalDate(date));
    } catch (Exception e) {
      return this.handleError(e);
    }
  }

  public String mapToAuthor(Practitioner practitioner) {
    try {
      return formatDoctorName(practitioner);
    } catch (Exception e) {
      return this.handleError(e);
    }
  }

  public String mapToContact(Organization organization, String system) {
    try {
      return getTelecom(organization.getContactFirstRep().getTelecom(), system).orElse(EMPTY);
    } catch (Exception e) {
      return this.handleError(e);
    }
  }

  public String mapToContact(PractitionerRole practitionerRole, PractitionerRole supervisorRole, String system) {
    try {
      Optional<String> practitionerContact = Optional.ofNullable(practitionerRole).flatMap(r -> getTelecom(r.getTelecom(), system));
      Optional<String> supervisorContact = Optional.ofNullable(supervisorRole).flatMap(s -> getTelecom(s.getTelecom(), system));
      return supervisorContact.or(() -> practitionerContact).orElse(EMPTY);
    } catch (Exception e) {
      return this.handleError(e);
    }
  }

  public String mapToPerformer(Organization organization) {
    try {
      String name = "";
      if (organization.hasAlias()) {
        name += organization.getAlias().get(0) + " : ";
      }
      if (organization.hasName()) {
        name += organization.getName();
      }
      return name.isEmpty() ? EMPTY : name;
    } catch (Exception e) {
      return this.handleError(e);
    }
  }

  public String mapToAnalysis(ServiceRequest serviceRequest) {
    try {
      final var analysisCode = FhirUtils.findCode(serviceRequest, ANALYSIS_REQUEST_CODE).orElse(null);
      return analysisCodes.getConcept().stream().filter(c -> c.getCode().equals(analysisCode))
        .findFirst()
        .map(c -> FhirToConfigMapper.getDisplayForLang(c, getLang()))
        .orElse(EMPTY);
    } catch (Exception e) {
      return this.handleError(e);
    }
  }

  public String mapToPanelReflex(ServiceRequest serviceRequest) {
    try {
      return serviceRequest.hasOrderDetail() ? serviceRequest.getOrderDetailFirstRep().getText()
        .replace(REFLEX_PANEL_PREFIX_FR, "")
        .replace(REFLEX_PANEL_PREFIX_EN, "").trim() : EMPTY;
    } catch (Exception e) {
      return this.handleError(e);
    }
  }

  public String mapToComment(ServiceRequest serviceRequest) {
    try {
      return serviceRequest.getNoteFirstRep().getText();
    } catch (Exception e) {
      return this.handleError(e);
    }
  }

  public List<String> mapToSigns(List<Observation> obs, String code, String interpretation) {
    var signs = new ArrayList<String>();
    try {
      var filtered = obs.stream()
        .filter(o -> o.getCode().getCodingFirstRep().getCode().equals(code))
        .filter(o -> StringUtils.isBlank(interpretation) || o.getInterpretationFirstRep().getCodingFirstRep().getCode().equals(interpretation))
        .toList();
      for (var sign: filtered) {
        var signCode = sign.getValue();
        var signAge = mapToI18nAgeAtOnset(sign);
        if (signCode instanceof CodeableConcept v) {
          var hpoCode = v.getCodingFirstRep().getCode();
          var hpoName = mapToI18nHPOName(hpoCode); //arrangerClient.getHPONameByPrefix(hpoCode);
          String signStr = "";
          if (StringUtils.isNotBlank(hpoName)) {
            signStr += StringUtils.capitalize(hpoName);
          }
          signStr += " ("+hpoCode+")";
          if (StringUtils.isNotBlank(signAge)) {
            signStr += " - " + signAge;
          }
          signs.add(signStr);
        } else if (signCode instanceof StringType v) {
          signs.add(v.asStringValue());
        } else if (signCode instanceof BooleanType v) {
          signs.add(i18n(v.asStringValue()));
        }
      }
    } catch (Exception e) {
      this.handleError(e);
    }
    return signs;
  }

  public String mapToSign(List<Observation> obs, String code, String interpretation) {
    var signs = mapToSigns(obs, code, interpretation);
    return signs.isEmpty() ? EMPTY : signs.get(0);
  }

  public String mapToEthnicity(List<Observation> obs) {
    var code = mapToSign(obs, "ETHN", "").replace("(","").replace(")", "").trim();
    var eth = codesValuesService.getValueByKeyCode(CodesValuesService.ETHNICITY_KEY, code);
    if (eth != null) {
      return FhirToConfigMapper.getDisplayForLang(eth, getLang());
    } else {
      return code;
    }
  }

  public List<Exam> mapToExams(List<Observation> obs) {
    var exams = new ArrayList<Exam>();
    try {
      var filtered = obs.stream()
        .filter(o -> o.getCategoryFirstRep().getCodingFirstRep().getCode().equals("procedure")).toList();
      for (var exam: filtered) {
        var code = exam.getCode().getCodingFirstRep().getCode();
        var name = codesValuesService.getValueByKeyCode(CodesValuesService.OBSERVATION_KEY, code);

        String examName = name != null ? FhirToConfigMapper.getDisplayForLang(name, getLang()) : code;
        String examComment = EMPTY;

        var interpretation = exam.getInterpretationFirstRep().getCodingFirstRep().getCode();
        if (StringUtils.isNotBlank(interpretation)) {
          examComment += i18n("interpretation_"+interpretation);
        }

        var value = exam.getValue();
        if (value instanceof CodeableConcept v) {
          var allHPOs = new ArrayList<>();
          for(var coding: v.getCoding()) {
            var hpoCode = coding.getCode();
            var hpoName = mapToI18nHPOName(hpoCode);
            if (StringUtils.isNotBlank(hpoName)) {
              allHPOs.add(hpoName);
            }
          }
          if (!allHPOs.isEmpty()) {
            examComment += " : " + StringUtils.join(allHPOs, ", ");
          }
        } else if (value instanceof StringType v) {
          examComment += " : "+v.asStringValue();
        }
        if ("A".equals(interpretation)) {
          examComment += " UI/L";
        }
        exams.add(new Exam(examName, examComment));
      }
    } catch (Exception e) {
      this.handleError(e);
    }
    return exams;
  }

  public record Exam(String name, String comment){}

  public String mapToFamilyHistory(List<FamilyMemberHistory> histories) {
    var all = new ArrayList<String>();
    try {
      for(var history: histories) {
        String str = history.getNoteFirstRep().getText();
        var code = history.getRelationship().getCodingFirstRep().getCode();
        var value = codesValuesService.getValueByKeyCode(CodesValuesService.PARENTAL_KEY, code);
        if (value != null) {
          var name = FhirToConfigMapper.getDisplayForLang(value, getLang());
          str+= " ("+name+")";
        } else {
          str+= " ("+code+")";
        }
        all.add(str);
      }
    } catch (Exception e) {
      this.handleError(e);
    }
    return StringUtils.join(all, ", ");
  }

  private String mapToI18nAgeAtOnset(Observation o) {
    ValueSet allAges = codesValuesService.getValues(CodesValuesService.AGE_KEY);
    return FhirUtils.findExtension(o, AGE_AT_ONSET_EXT).map(e -> ((Coding) e).getCode())
      .flatMap(code -> allAges.getCompose().getIncludeFirstRep().getConcept().stream().filter(c -> c.getCode().equals(code)).findFirst())
      .map(code -> FhirToConfigMapper.getDisplayForLang(code, getLang()))
      .orElse(EMPTY);
  }

  private String mapToI18nHPOName(String hpoCode) {
    var hpo = codesValuesService.getHPOByCode(hpoCode);
    var name = EMPTY;
    if (hpo instanceof  ValueSet.ConceptReferenceComponent c) {
      name = FhirToConfigMapper.getDisplayForLang(c, getLang());
    } else if (hpo instanceof  CodeSystem.ConceptDefinitionComponent c) {
      name = FhirToConfigMapper.getDisplayForLang(c, getLang());
    }
    return name;
  }

  private String i18n(String key) {
    return Optional.ofNullable(this.messagesService.get(key, locale.getLanguage())).orElse("");
  }

  private String handleError(Exception e) {
    logOnceService.warn(String.format("Template[%s]: %s", id, e.getMessage()));
    return EMPTY;
  }

  private Optional<String> getTelecom(List<ContactPoint> contactPoints, String system) {
    return contactPoints.stream().filter(t -> system.equals(t.getSystem().toCode())).findFirst().map(ContactPoint::getValue);
  }

  private String getLang() {
    return Optional.ofNullable(locale).map(Locale::getLanguage).orElse(null);
  }

  private String formatDoctorName(Practitioner practitioner) {
    final var name = formatName(practitioner.getNameFirstRep(), true);
    final var md = FhirUtils.findIdentifier(practitioner, MEDICAL_LICENSE_CODE);
    return md.map(s -> name + " - " + s).orElse(name);
  }

  private String formatName(HumanName name, boolean withPrefix) {
    var full = String.format("%s %s", name.getFamily().toUpperCase(), StringUtils.capitalize(name.getGivenAsSingleString()));
    if (withPrefix && name.hasPrefix()) {
      full = StringUtils.capitalize(name.getPrefixAsSingleString()) + ". " + full;
    }
    return full;
  }

  private Optional<String> getIdentifier(List<Identifier> identifiers, String code) {
    return identifiers.stream().filter(i -> i.getType().getCoding().stream().anyMatch(c -> code.equals(c.getCode()))).findFirst().map(Identifier::getValue);
  }
}
