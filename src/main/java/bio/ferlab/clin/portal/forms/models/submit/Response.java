package bio.ferlab.clin.portal.forms.models.submit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Response {
  private String id;
  private List<Patient> patients;

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Patient {
    private String id;
    private String familyMember;
  }
}
