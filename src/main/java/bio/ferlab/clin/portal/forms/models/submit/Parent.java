package bio.ferlab.clin.portal.forms.models.submit;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Parent {

  public enum Moment {
    now,
    later,
    never,
  }

  public enum Status {
    affected,
    not_affected,
    unknown,
  }

  private String id;
  @NotNull
  private Moment parentEnterMoment;
  private String parentNoInfoReason;
  private Status parentClinicalStatus;
  private String ep;
  private String ramq;
  private String mrn;
  private String firstName;
  private String lastName;
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate birthDate;
  private Patient.Gender gender;
  @Valid
  @NotNull
  private List<Signs> signs = new ArrayList<>();
  private String comment;

  public void validate() {
    // this part is not optimal ... but if moment = now we need to manually validate the values are here
    // and if moment != null these values are allowed to be missing
    if (Moment.now.equals(parentEnterMoment)) {
      if (StringUtils.isAnyBlank(ep, lastName, firstName)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "[mother|father].[ep|lastName|firstName] can't be blank");
      }
      if (birthDate == null || gender == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "[mother|father].[birthDate|gender] can't be null");
      }
    }
    if (Status.affected.equals(parentClinicalStatus) && signs.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "[mother|father].signs can't be empty if [mother|father].parent_clinical_status = affected");
    }
  }
}
