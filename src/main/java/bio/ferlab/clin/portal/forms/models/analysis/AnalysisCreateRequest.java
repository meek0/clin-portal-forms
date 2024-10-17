package bio.ferlab.clin.portal.forms.models.analysis;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AnalysisCreateRequest {

  private String analysisId;
  private AnalysisType type;
  private String analysisCode;
  private Boolean isReflex;
  private String residentSupervisorId;
  private String comment;
  private List<History> history;
  private String diagnosisHypothesis;
  private String ethnicityCode;
  private Boolean inbreeding;
  private List<String> sequencingTypes;
  private List<Patient> patients;

  public enum AnalysisType {
    GERMLINE, SOMATIC_TUMOR_ONLY
  }

  @Data
  public static class History {
    private String condition;
    private String parentalLinkCode;
  }

  public enum FamilyMember {
    PROBAND, MOTHER, FATHER, SIS, BRO
  }

  @Data
  public static class Patient {
    private String patientId;
    private String firstName;
    private String lastName;
    private String jhn;
    private String mrn;
    private Sex sex;
    private String birthDate;
    private String organizationId;
    private FamilyMember familyMember;
    private Foetus foetus;
    private ParentalStatus status;
    private String reason;
    private Boolean affected;
    private Clinical clinical;
    private ParaClinical paraClinical;
  }

  @Data
  public static class Foetus {
    private FoetusType type;
    private Sex sex;
    private GestationalMethod gestationalMethod;
    private String gestationalDate;
    private String motherJhn;
  }

  public enum FoetusType {
    NEW_BORN, PRENATAL
  }
  public enum Sex {
    MALE, FEMALE, OTHER, UNKNOWN
  }
  public enum GestationalMethod {
    DDM, DPA, DECEASED
  }

  public enum ParentalStatus {
    NOW, LATER, NEVER
  }

  @Data
  public static class Clinical{
    private List<Sign> signs = new ArrayList<>();
    private String comment;
  }

  @Data
  public static class Sign {
    private String code;
    private Boolean observed;
    private String ageCode;
  }

  @Data
  public static class ParaClinical {
    private List<Exam> exams = new ArrayList<>();
    private String other;
  }

  @Data
  public static class Exam {
    private String code;
    private ExamInterpretation interpretation;
    private List<String> values = new ArrayList<>();
  }

  public enum ExamInterpretation {
    ABNORMAL, NORMAL
  }

}
