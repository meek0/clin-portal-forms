package bio.ferlab.clin.portal.forms.mappers;

import bio.ferlab.clin.portal.forms.models.analysis.AnalysisCreateRequest;
import bio.ferlab.clin.portal.forms.models.submit.ClinicalSigns;
import bio.ferlab.clin.portal.forms.models.submit.Parent;
import bio.ferlab.clin.portal.forms.models.submit.Request;
import bio.ferlab.clin.portal.forms.models.submit.Signs;
import bio.ferlab.clin.portal.forms.utils.DateUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class QlinMeMapper {

  public AnalysisCreateRequest mapToAnalysisCreateRequest(Request submitRequest) {
    var analysis = new AnalysisCreateRequest();

    analysis.setType(AnalysisCreateRequest.AnalysisType.GERMLINE);
    analysis.setAnalysisCode(submitRequest.getAnalysis().getPanelCode());
    analysis.setResidentSupervisorId(submitRequest.getAnalysis().getResidentSupervisor());
    analysis.setIsReflex(submitRequest.getAnalysis().getIsReflex());
    analysis.setComment(submitRequest.getAnalysis().getComment());

    mapToDiagnosis(analysis, submitRequest);
    analysis.setHistory(mapToHistory(submitRequest));

    var patients = new ArrayList<AnalysisCreateRequest.Patient>();
    Optional.ofNullable(mapToProband(submitRequest)).ifPresent(patients::add);
    Optional.ofNullable(mapToParent(submitRequest.getMother(), AnalysisCreateRequest.FamilyMember.MOTHER)).ifPresent(patients::add);
    Optional.ofNullable(mapToParent(submitRequest.getFather(), AnalysisCreateRequest.FamilyMember.FATHER)).ifPresent(patients::add);
    if (!patients.isEmpty()) {
      analysis.setPatients(patients);
    }

    return analysis;
  }

  private List<AnalysisCreateRequest.History> mapToHistory(Request submitRequest) {
    var historyAndDiagnosis = submitRequest.getHistoryAndDiagnosis();
    if (historyAndDiagnosis != null && historyAndDiagnosis.getHealthConditions() != null && !historyAndDiagnosis.getHealthConditions().isEmpty()) {
      var result = new ArrayList<AnalysisCreateRequest.History>();
      historyAndDiagnosis.getHealthConditions().forEach(hc -> {
        var history = new AnalysisCreateRequest.History();
        history.setCondition(hc.getCondition());
        history.setParentalLinkCode(hc.getParentalLink());
        result.add(history);
      });
      return result;
    }
    return null;
  }

  private void mapToDiagnosis(AnalysisCreateRequest analysis, Request submitRequest) {
    var historyAndDiagnosis = submitRequest.getHistoryAndDiagnosis();
    if (historyAndDiagnosis != null) {
      analysis.setDiagnosisHypothesis(historyAndDiagnosis.getDiagnosticHypothesis());
      analysis.setEthnicityCode(historyAndDiagnosis.getEthnicity());
      analysis.setInbreeding(historyAndDiagnosis.getInbreeding());
    }
  }

  private AnalysisCreateRequest.Patient mapToProband(Request submitRequest) {
    var patient = submitRequest.getPatient();
    if (patient != null) {
      var proband = new AnalysisCreateRequest.Patient();
      if(patient.getId() != null) proband.setPatientId(patient.getId());
      proband.setFirstName(patient.getFirstName());
      proband.setLastName(patient.getLastName());
      proband.setJhn(patient.getRamq());
      proband.setMrn(patient.getMrn());
      if (patient.getGender() != null) {
        proband.setSex(AnalysisCreateRequest.Sex.valueOf(patient.getGender().name().toUpperCase()));
      }
      if (patient.getBirthDate() != null) {
        proband.setBirthDate(DateUtils.FORMATTER_YYYYMMdd.format(patient.getBirthDate()));
      }
      proband.setOrganizationId(patient.getEp());
      proband.setFamilyMember(AnalysisCreateRequest.FamilyMember.PROBAND);
      var additionalInfo = patient.getAdditionalInfo();
      if (additionalInfo != null && (Boolean.TRUE.equals(additionalInfo.getIsPrenatalDiagnosis()) || Boolean.TRUE.equals(additionalInfo.getIsNewBorn()))) {
        var foetus = new AnalysisCreateRequest.Foetus();
        if (additionalInfo.getGestationalAge() != null) {
          foetus.setGestationalMethod(AnalysisCreateRequest.GestationalMethod.valueOf(additionalInfo.getGestationalAge().name().toUpperCase()));
        }
        if (additionalInfo.getGestationalDate() != null) {
          foetus.setGestationalDate(DateUtils.FORMATTER_YYYYMMdd.format(additionalInfo.getGestationalDate()));
        }
        if (additionalInfo.getFoetusGender() != null) {
          foetus.setSex(AnalysisCreateRequest.Sex.valueOf(additionalInfo.getFoetusGender().name().toUpperCase()));
        }
        if (Boolean.TRUE.equals(additionalInfo.getIsNewBorn())) {
          foetus.setType(AnalysisCreateRequest.FoetusType.NEW_BORN);
        } else if (Boolean.TRUE.equals(additionalInfo.getIsPrenatalDiagnosis())) {
          foetus.setType(AnalysisCreateRequest.FoetusType.PRENATAL);
        }
        if (additionalInfo.getMotherRamq() != null) {
          foetus.setMotherJhn(additionalInfo.getMotherRamq());
        }
        proband.setFoetus(foetus);
      }
      proband.setClinical(mapToClinical(submitRequest.getClinicalSigns()));
      proband.setParaClinical(mapToParaclinical(submitRequest));
      return proband;
    }
    return null;
  }

  private AnalysisCreateRequest.Clinical mapToClinical(ClinicalSigns clinicalSigns) {
    if (clinicalSigns != null) {
      return mapToClinical(clinicalSigns.getSigns(), clinicalSigns.getComment());
    }
    return null;
  }

  private AnalysisCreateRequest.Clinical mapToClinical(List<Signs> signs, String comment) {
    if (signs != null && !signs.isEmpty()) {
      var result = new AnalysisCreateRequest.Clinical();
      result.setComment(comment);
      result.setSigns(new ArrayList<>());
      signs.forEach(s -> {
        var sign = new AnalysisCreateRequest.Sign();
        sign.setObserved(s.getIsObserved());
        sign.setCode(s.getValue());
        sign.setAgeCode(s.getAgeCode());
        result.getSigns().add(sign);
      });
      return result;
    }
    return null;
  }

  private AnalysisCreateRequest.ParaClinical mapToParaclinical(Request submitRequest) {
    var paraclinicalExams = submitRequest.getParaclinicalExams();
    if (paraclinicalExams != null) {
      var result = new AnalysisCreateRequest.ParaClinical();
      result.setOther(paraclinicalExams.getComment());
      if (paraclinicalExams.getExams() != null && !paraclinicalExams.getExams().isEmpty()) {
        result.setExams(new ArrayList<>());
        paraclinicalExams.getExams().forEach(e -> {
          var exam = new AnalysisCreateRequest.Exam();
          exam.setCode(e.getCode());
          if (e.getInterpretation() != null) {
            exam.setInterpretation(AnalysisCreateRequest.ExamInterpretation.valueOf(e.getInterpretation().name().toUpperCase()));
          }
          var values = new ArrayList<String>();
          if (e.getValue() != null) {
            values.add(e.getValue());
          }
          if (e.getValues() != null) {
            values.addAll(e.getValues());
          }
          exam.setValues(values);
          result.getExams().add(exam);
        });
      }
      return result;
    }
    return null;
  }

  private AnalysisCreateRequest.Patient mapToParent(Parent parent, AnalysisCreateRequest.FamilyMember familyMember) {
    if (parent != null) {
      var patient = new AnalysisCreateRequest.Patient();
      if(parent.getId() != null) patient.setPatientId(parent.getId());
      patient.setFirstName(parent.getFirstName());
      patient.setLastName(parent.getLastName());
      patient.setJhn(parent.getRamq());
      patient.setMrn(parent.getMrn());
      if (parent.getGender() != null) {
        patient.setSex(AnalysisCreateRequest.Sex.valueOf(parent.getGender().name().toUpperCase()));
      }
      if (parent.getBirthDate() != null) {
        patient.setBirthDate(DateUtils.FORMATTER_YYYYMMdd.format(parent.getBirthDate()));
      }
      patient.setOrganizationId(parent.getEp());
      patient.setFamilyMember(familyMember);
      patient.setClinical(mapToClinical(parent.getSigns(), parent.getComment()));

      if (parent.getParentEnterMoment() != null) {
        patient.setStatus(AnalysisCreateRequest.ParentalStatus.valueOf(parent.getParentEnterMoment().name().toUpperCase()));
      }
      patient.setReason(parent.getParentNoInfoReason());
      Boolean affected = Parent.Status.affected.equals(parent.getParentClinicalStatus()) ? Boolean.TRUE : Parent.Status.not_affected.equals(parent.getParentClinicalStatus()) ? Boolean.FALSE : null;
      patient.setAffected(affected);
      return patient;
    }
    return null;
  }

}
