package bio.ferlab.clin.portal.forms.models.submit;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Patient {
  
  public enum Gender {
    male,
    female,
    other,
    unknown
  }
  
  @NotBlank
  private String ep;
  private String ramq;
  private String mrn;
  @NotBlank
  private String firstName;
  @NotBlank
  private String lastName;
  @NotNull
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate birthDate;
  @NotNull
  private Gender gender;
  private String ethnicity;
  @Valid
  @NotNull
  private AdditionalInfo additionalInfo = new AdditionalInfo();
}
