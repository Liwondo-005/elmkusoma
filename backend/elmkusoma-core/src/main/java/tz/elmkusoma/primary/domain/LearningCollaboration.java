package tz.elmkusoma.primary.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "learning_collaborations")
public class LearningCollaboration extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "collaboration_type", nullable = false, length = 30)
    private String collaborationType;

    @Column(name = "partner_name", length = 255)
    private String partnerName;

    @Column(name = "activity", columnDefinition = "TEXT")
    private String activity;

    @Column(name = "subject_name", length = 100)
    private String subjectName;

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    public LearningCollaboration() {
    }

    public UUID getStudentId() {
        return studentId;
    }

    public void setStudentId(UUID studentId) {
        this.studentId = studentId;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public String getCollaborationType() {
        return collaborationType;
    }

    public void setCollaborationType(String collaborationType) {
        this.collaborationType = collaborationType;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public void setPartnerName(String partnerName) {
        this.partnerName = partnerName;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted;
    }
}
