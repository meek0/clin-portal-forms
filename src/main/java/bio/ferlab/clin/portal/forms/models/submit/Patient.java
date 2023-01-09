package bio.ferlab.clin.portal.forms.models.submit;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
  @Valid
  @NotNull
  private AdditionalInfo additionalInfo = new AdditionalInfo();

  public Patient(Parent parent) {
    this.setRamq(parent.getRamq());
    this.setMrn(parent.getMrn());
    this.setEp(parent.getEp());
    this.setGender(parent.getGender());
    this.setBirthDate(parent.getBirthDate());
    this.setLastName(parent.getLastName());
    this.setFirstName(parent.getFirstName());
  }

  public Patient(String ramq, String mrn, String ep) {
    this.setRamq(ramq);
    this.setMrn(mrn);
    this.setEp(ep);
  }
}
