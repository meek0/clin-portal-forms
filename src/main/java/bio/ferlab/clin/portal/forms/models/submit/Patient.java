package bio.ferlab.clin.portal.forms.models.submit;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Patient {
  @NotNull
  private String ep;
  private String ramq;
  private String mrn;
  @NotNull
  private String firstName;
  @NotNull
  private String lastName;
  @NotNull
  @JsonFormat(pattern = "dd/MM/yyyy")
  private LocalDate birthDate;
  @NotNull
  private String gender;
  private String ethnicity;
}
