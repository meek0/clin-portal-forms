package bio.ferlab.clin.portal.forms.models.analysis;


import java.util.List;

public record AnalysisCreateResponse(String analysisId, List<Patient> patients) {
  public record Patient(String patientId, String familyMember) {}
}
