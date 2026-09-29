package tz.elmkusoma.learning.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Assignment extends BaseEntity {

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "class_group_id", nullable = false)
    private UUID classGroupId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "total_marks", nullable = false)
    private Integer totalMarks;

    @Column(name = "attachments")
    private String attachments;

    @Column(name = "assignment_type", length = 50)
    private String assignmentType;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "status", length = 20)
    private String status = "PUBLISHED";

    /** Optional Lesson link (Lesson ↔ Assignment connection, nullable). */
    @Column(name = "lesson_id")
    private UUID lessonId;

    /** When the assignment opens for submissions (nullable). */
    @Column(name = "open_date")
    private LocalDateTime openDate;

    /** Hard close date; after this, submissions are rejected unless late allowed. */
    @Column(name = "close_date")
    private LocalDateTime closeDate;

    /** Whether submissions are accepted after due_date (marked late). */
    @Column(name = "allow_late_submission")
    private Boolean allowLateSubmission = false;
}
