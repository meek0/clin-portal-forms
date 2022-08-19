package bio.ferlab.clin.portal.forms.models.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchPrescription {
  private String analysisId;
  private String panelCode;
  private String prescriber;
  private String date;
  private String patientId;
  private String patientName;
  private String patientRamq;
  private String motherRamq;
}
