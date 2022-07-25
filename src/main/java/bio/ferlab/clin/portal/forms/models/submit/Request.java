package bio.ferlab.clin.portal.forms.models.submit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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
  private List<Phenotype> phenotypes = new ArrayList<>();
  
  private String observation;

  @Valid
  private List<Exam> exams = new ArrayList<>();
  
  private String investigation;
  
  private String ethnicity;
  
  @NotNull
  private String indication;
  
  private String comment;
  
  private String residentSupervisor;
  
}
