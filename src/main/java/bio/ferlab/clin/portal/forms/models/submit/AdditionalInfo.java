package bio.ferlab.clin.portal.forms.models.submit;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.EnumSet;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalInfo {
  
  public enum GestationalAge {
    ddm, dpa, deceased
  }
  
  private Boolean isNewBorn;
  private Boolean isPrenatalDiagnosis;
  private Patient.Gender foetusGender;
  private GestationalAge gestationalAge;
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate gestationalDate;
  private String motherRamq;
  
  public void validate() {
    if (Boolean.TRUE.equals(isNewBorn) && Boolean.TRUE.equals(isPrenatalDiagnosis)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patient.additional_info is_new_born and is_prenatal_diagnosis can't be both true");
    }
    if (Boolean.TRUE.equals(isNewBorn) && StringUtils.isBlank(motherRamq)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patient.additional_info is_new_born is true but mother_ramq is empty");
    }
    if (Boolean.TRUE.equals(isPrenatalDiagnosis)) {
      if (foetusGender == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patient.additional_info foetus_gender is required if is_prenatal_diagnosis is true");
      }
      if (gestationalAge == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patient.additional_info gestational_age is required if is_prenatal_diagnosis is true");
      } else if (EnumSet.of(GestationalAge.ddm, GestationalAge.dpa).contains(gestationalAge) && gestationalDate == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patient.additional_info gestational_date is required if gestational_age is ddm or dpa");
      }
    }
  }
}
