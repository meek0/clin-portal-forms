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

import static bio.ferlab.clin.portal.forms.utils.FhirConst.*;

@RequiredArgsConstructor
public class TemplateMapper {

  private final CodeSystem analysisCodes;
  private final Locale locale;

  public String mapToName(Person person) {
    return formatName(person.getNameFirstRep());
  }

  public String mapToRAMQ(Person person) {
    return getIdentifier(person.getIdentifier(), CODE_RAMQ).map(r -> String.format("%s %s %s", r.substring(0,4), r.substring(4,8), r.substring(8,12))).orElse("");
  }

  public String mapToMRN(Patient patient) {
    return getIdentifier(patient.getIdentifier(), CODE_MRN).map(r -> String.format("%s | %s",r.replace("MRN-", ""), FhirUtils.extractId(patient.getManagingOrganization()))).orElse("");
  }

  public String formatDate(Date date) {
    return DateUtils.FORMATTER_YYYYMMdd.format(DateUtils.toLocalDate(date));
  }

  public String mapToAuthor(Practitioner practitioner) {
    final var name = practitioner.getNameFirstRep();
    final var full = formatName(name);
    if (name.hasPrefix()) {
      return  StringUtils.capitalize(name.getPrefixAsSingleString()) + " " + full;
    }
    return full;
  }

  public String mapToAnalysis(ServiceRequest serviceRequest) {
    final var analysisCode = serviceRequest.getCode().getCoding().stream().filter(c -> ANALYSIS_REQUEST_CODE.equals(c.getSystem()))
      .findFirst()
      .map(Coding::getCode).orElse(null);
    return analysisCodes.getConcept().stream().filter(c -> c.getCode().equals(analysisCode))
      .findFirst()
      .map(c -> FhirToConfigMapper.getDisplayForLang(c, locale.getLanguage()))
      .orElse("");
  }

  public String mapToPanelReflex(ServiceRequest serviceRequest) {
    return serviceRequest.hasOrderDetail() ? serviceRequest.getOrderDetailFirstRep().getText() : "";
  }

  private String formatName(HumanName name) {
    return String.format("%s %s", name.getFamily().toUpperCase(), StringUtils.capitalize(name.getGivenAsSingleString()));
  }

  private Optional<String> getIdentifier(List<Identifier> identifiers, String code) {
    return identifiers.stream().filter(i -> i.getType().getCoding().stream().anyMatch(c -> code.equals(c.getCode()))).findFirst().map(Identifier::getValue);
  }
}
