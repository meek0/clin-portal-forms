package bio.ferlab.clin.portal.forms.models.submit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalInfo {
  
  private Boolean isNewBorn;
  private Boolean isPrenatalDiagnosis;
  private String motherRamq;
  
  public void validate() {
    if (Boolean.TRUE.equals(isNewBorn) && Boolean.TRUE.equals(isPrenatalDiagnosis)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patient.additional_info is_new_born and isPrenatalDiagnosis can't be both true");
    }
    if (Boolean.TRUE.equals(isNewBorn) && StringUtils.isBlank(motherRamq)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patient.additional_info is_new_born is true but mother_ramq is empty");
    }
  }
}
