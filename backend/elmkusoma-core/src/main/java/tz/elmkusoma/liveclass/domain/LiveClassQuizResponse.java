package tz.elmkusoma.liveclass.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "live_class_quiz_responses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LiveClassQuizResponse extends BaseEntity {

    @Column(name = "quiz_id", nullable = false)
    private UUID quizId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "answer_text")
    private String answerText;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
}
