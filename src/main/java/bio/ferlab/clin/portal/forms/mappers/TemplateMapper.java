package bio.ferlab.clin.portal.forms.mappers;

import bio.ferlab.clin.portal.forms.utils.DateUtils;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static bio.ferlab.clin.portal.forms.models.builders.ReflexBuilder.REFLEX_PANEL_PREFIX_EN;
import static bio.ferlab.clin.portal.forms.models.builders.ReflexBuilder.REFLEX_PANEL_PREFIX_FR;
import static bio.ferlab.clin.portal.forms.utils.FhirConst.*;

@RequiredArgsConstructor
public class TemplateMapper {
  
  public static final String EMPTY = "-";

  private final CodeSystem analysisCodes;
  private final Locale locale;

  public String mapToName(Person person) {
    return formatName(person.getNameFirstRep(), false);
  }

  public String mapToRAMQ(Person person) {
    return getIdentifier(person.getIdentifier(), CODE_RAMQ).map(r -> String.format("%s %s %s", r.substring(0,4), r.substring(4,8), r.substring(8,12))).orElse(EMPTY);
  }

  public String mapToMRN(Patient patient) {
    return getIdentifier(patient.getIdentifier(), CODE_MRN).map(r -> String.format("%s | %s",r.replace("MRN-", ""), FhirUtils.extractId(patient.getManagingOrganization()))).orElse(EMPTY);
  }

  public String formatDate(Date date) {
    return DateUtils.FORMATTER_YYYYMMdd.format(DateUtils.toLocalDate(date));
  }

  public String mapToAuthor(Practitioner practitioner) {
    return formatDoctorName(practitioner);
  }

  public String mapToContact(Organization organization, String system) {
    if (organization == null) return EMPTY;
    return organization.getContactFirstRep().getTelecom().stream().filter(t -> system.equals(t.getSystem().toCode())).findFirst().map(ContactPoint::getValue).orElse(EMPTY);
  }

  public String mapToContact(PractitionerRole practitionerRole, PractitionerRole supervisorRole, String system) {
    PractitionerRole role = supervisorRole != null ? supervisorRole : practitionerRole;
    if (role == null) return EMPTY;
    return role.getTelecom().stream().filter(t -> system.equals(t.getSystem().toCode())).findFirst().map(ContactPoint::getValue).orElse(EMPTY);
  }

  public String mapToPerformer(Organization organization) {
    if (organization == null) return EMPTY;
    String name = "";
    if (organization.hasAlias()) {
      name += organization.getAlias().get(0) + " : ";
    }
    if (organization.hasName()) {
      name += organization.getName();
    }
    return name;
  }

  public String mapToAnalysis(ServiceRequest serviceRequest) {
    final var analysisCode = FhirUtils.findCode(serviceRequest, ANALYSIS_REQUEST_CODE).orElse(null);
    return analysisCodes.getConcept().stream().filter(c -> c.getCode().equals(analysisCode))
      .findFirst()
      .map(c -> FhirToConfigMapper.getDisplayForLang(c, locale.getLanguage()))
      .orElse(EMPTY);
  }

  public String mapToPanelReflex(ServiceRequest serviceRequest) {
    return serviceRequest.hasOrderDetail() ? serviceRequest.getOrderDetailFirstRep().getText()
      .replace(REFLEX_PANEL_PREFIX_FR, "")
      .replace(REFLEX_PANEL_PREFIX_EN, "") : EMPTY;
  }

  private String formatDoctorName(Practitioner practitioner) {
    if (practitioner == null)
      return EMPTY;
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
