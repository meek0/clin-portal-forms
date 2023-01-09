package bio.ferlab.clin.portal.forms.models.submit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParaclinicalExams {

  @Valid
  @NotNull
  List<Exams> exams = new ArrayList<>();
  private String comment;
}
