package tz.elmkusoma.primary.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "discovery_entries")
public class DiscoveryEntry extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "question", columnDefinition = "TEXT", nullable = false)
    private String question;

    @Column(name = "discovery_type", nullable = false, length = 30)
    private String discoveryType;

    @Column(name = "subject_name", length = 100)
    private String subjectName;

    @Column(name = "result", columnDefinition = "TEXT")
    private String result;

    @Column(name = "is_resolved", nullable = false)
    private Boolean isResolved = false;

    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence;

    public DiscoveryEntry() {
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getDiscoveryType() {
        return discoveryType;
    }

    public void setDiscoveryType(String discoveryType) {
        this.discoveryType = discoveryType;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Boolean getIsResolved() {
        return isResolved;
    }

    public void setIsResolved(Boolean isResolved) {
        this.isResolved = isResolved;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }
}
