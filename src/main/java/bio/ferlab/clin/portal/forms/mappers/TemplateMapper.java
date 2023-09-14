package bio.ferlab.clin.portal.forms.mappers;

import bio.ferlab.clin.portal.forms.utils.DateUtils;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static bio.ferlab.clin.portal.forms.models.builders.ReflexBuilder.REFLEX_PANEL_PREFIX_EN;
import static bio.ferlab.clin.portal.forms.models.builders.ReflexBuilder.REFLEX_PANEL_PREFIX_FR;
import static bio.ferlab.clin.portal.forms.utils.FhirConst.*;

@Slf4j
@RequiredArgsConstructor
public class TemplateMapper {
  
  public static final String EMPTY = "-";

  private final CodeSystem analysisCodes;
  private final Locale locale;

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
      return getIdentifier(patient.getIdentifier(), CODE_MRN).map(r -> String.format("%s | %s",r.toUpperCase().replace("MRN-", ""), org)).orElse(EMPTY);
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

  private String handleError(Exception e) {
    // could implement a strict mode later
    log.warn(e.getMessage());
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
      full = StringUtils.capitalize(name.getPrefixAsSingleString()) + " " + full;
    }
    return full;
  }

  private Optional<String> getIdentifier(List<Identifier> identifiers, String code) {
    return identifiers.stream().filter(i -> i.getType().getCoding().stream().anyMatch(c -> code.equals(c.getCode()))).findFirst().map(Identifier::getValue);
  }
}
