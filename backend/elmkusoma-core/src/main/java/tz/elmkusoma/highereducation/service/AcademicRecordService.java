package tz.elmkusoma.highereducation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.highereducation.domain.AcademicRecord;
import tz.elmkusoma.highereducation.dto.AcademicRecordDTO;
import tz.elmkusoma.highereducation.repository.AcademicRecordRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AcademicRecordService {

    private final AcademicRecordRepository repository;

    public AcademicRecordDTO getLatestRecord(UUID studentId) {
        return repository.findTopByStudentIdAndIsDeletedFalseOrderByCreatedAtDesc(studentId)
                .map(this::toDTO)
                .orElse(null);
    }

    public List<AcademicRecordDTO> getStudentRecords(UUID studentId) {
        return repository.findByStudentIdAndIsDeletedFalse(studentId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public AcademicRecordDTO getRecord(UUID studentId, String semester, String academicYear) {
        return repository.findByStudentIdAndSemesterAndAcademicYearAndIsDeletedFalse(studentId, semester, academicYear)
                .map(this::toDTO)
                .orElse(null);
    }

    public AcademicRecordDTO create(AcademicRecordDTO dto) {
        AcademicRecord entity = AcademicRecord.builder()
                .studentId(dto.getStudentId())
                .programmeId(dto.getProgrammeId())
                .academicYear(dto.getAcademicYear())
                .semester(dto.getSemester())
                .totalCreditHours(dto.getTotalCreditHours())
                .earnedCreditHours(dto.getEarnedCreditHours())
                .semesterGpa(dto.getSemesterGpa())
                .cumulativeGpa(dto.getCumulativeGpa())
                .totalCourses(dto.getTotalCourses())
                .completedCourses(dto.getCompletedCourses())
                .failedCourses(dto.getFailedCourses())
                .academicStanding(dto.getAcademicStanding())
                .classRank(dto.getClassRank())
                .totalStudentsInClass(dto.getTotalStudentsInClass())
                .institutionId(dto.getInstitutionId())
                .build();
        return toDTO(repository.save(entity));
    }

    public AcademicRecordDTO update(UUID id, AcademicRecordDTO dto) {
        AcademicRecord entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicRecord", "id", id));
        entity.setSemesterGpa(dto.getSemesterGpa());
        entity.setCumulativeGpa(dto.getCumulativeGpa());
        entity.setTotalCreditHours(dto.getTotalCreditHours());
        entity.setEarnedCreditHours(dto.getEarnedCreditHours());
        entity.setCompletedCourses(dto.getCompletedCourses());
        entity.setFailedCourses(dto.getFailedCourses());
        entity.setAcademicStanding(dto.getAcademicStanding());
        entity.setClassRank(dto.getClassRank());
        return toDTO(repository.save(entity));
    }

    private AcademicRecordDTO toDTO(AcademicRecord e) {
        return AcademicRecordDTO.builder()
                .id(e.getId())
                .studentId(e.getStudentId())
                .programmeId(e.getProgrammeId())
                .academicYear(e.getAcademicYear())
                .semester(e.getSemester())
                .totalCreditHours(e.getTotalCreditHours())
                .earnedCreditHours(e.getEarnedCreditHours())
                .semesterGpa(e.getSemesterGpa())
                .cumulativeGpa(e.getCumulativeGpa())
                .totalCourses(e.getTotalCourses())
                .completedCourses(e.getCompletedCourses())
                .failedCourses(e.getFailedCourses())
                .academicStanding(e.getAcademicStanding())
                .classRank(e.getClassRank())
                .totalStudentsInClass(e.getTotalStudentsInClass())
                .institutionId(e.getInstitutionId())
                .build();
    }
}
