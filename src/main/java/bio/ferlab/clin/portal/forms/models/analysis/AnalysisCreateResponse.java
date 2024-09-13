package bio.ferlab.clin.portal.forms.models.analysis;

import java.util.List;

public record AnalysisCreateResponse(String analysisId, List<SequencingCreated> sequencings) {
  public record SequencingCreated(String sequencingId, PatientCreated patient) {}
  public record PatientCreated(String patientId, AnalysisCreateRequest.FamilyMember familyMember) {}
}
