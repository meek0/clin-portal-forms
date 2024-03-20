package bio.ferlab.clin.portal.forms.models.builders;

import bio.ferlab.clin.portal.forms.clients.FhirClient;
import bio.ferlab.clin.portal.forms.models.autocomplete.Supervisor;
import bio.ferlab.clin.portal.forms.utils.BundleExtractor;
import bio.ferlab.clin.portal.forms.utils.FhirUtils;
import bio.ferlab.clin.portal.forms.utils.Utils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class AutocompleteBuilder {

  private final FhirClient fhirClient;
  private final String ep; // assuming ep will be common to all auto-completes

  private List<Supervisor> supervisors;

  public AutocompleteBuilder withSupervisor(String prefix) {
    this.supervisors = new ArrayList<>();
    if (StringUtils.isNotBlank(prefix)) {
      final Bundle bundle = this.fhirClient.findPractitionerAndRoleByEp(ep);
      final BundleExtractor bundleExtractor = new BundleExtractor(fhirClient.getContext(), bundle);
      // from a performance point of view, it's easier to fetch all the practitioner + roles by ep
      // and filter here instead of asking FHIR to query filter in database.
      // considering findPractitionerAndRoleByEp can be cached it will be better
      final List<PractitionerRole> roles = bundleExtractor.getAllResourcesOfType(PractitionerRole.class)
        .stream().filter(r -> FhirUtils.isDoctor(r, ep)).toList();
      final List<Practitioner> practitioners = bundleExtractor.getAllResourcesOfType(Practitioner.class);
      // not a stream.filter(...) because un-readable
      for (Practitioner p : practitioners) {
        final String pRef = FhirUtils.formatResource(p);
        final HumanName name = p.getNameFirstRep();
        // all the fields we want to match
        if (Utils.isIndexOfAnyIgnoreCase(
            prefix,
            name.getNameAsSingleString(), // contains firstName and given names
            p.getIdentifierFirstRep().getValue())) {
          for (PractitionerRole r : roles) {
            if (pRef.equals(r.getPractitioner().getReference())) {
              final Supervisor s = new Supervisor();
              s.setName(getSupervisorFullName(name));
              s.setId(r.getIdElement().getIdPart());
              s.setLicense(p.getIdentifierFirstRep().getValue());
              supervisors.add(s);
            }
          }
        }
      }
    }
    return this;
  }

  public Result build() {
    return new Result(this.supervisors);
  }

  @Getter
  @AllArgsConstructor
  public static class Result {
    private final List<Supervisor> supervisors;
  }

  private String getSupervisorFullName(HumanName name) {
    final String givenName = name.getGivenAsSingleString();
    final String familyName = Optional.ofNullable(name.getFamily()).orElse("").toUpperCase();

    final String result = familyName.length() > 0 ? familyName + " " + givenName : givenName;

    return result.trim();
  }
}
