package bio.ferlab.clin.portal.forms.models.submit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Request {
  @Valid
  private Analyse analyse = new Analyse();
  @Valid
  private Patient patient = new Patient();
  @Valid
  private List<Observation> phenotypes = new ArrayList<>();
  
  private String observation;

  @Valid
  private List<Observation> exams = new ArrayList<>();
  
  private String investigation;
  
}
