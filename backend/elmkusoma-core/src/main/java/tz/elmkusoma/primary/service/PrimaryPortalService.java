package tz.elmkusoma.primary.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.academic.domain.Subject;
import tz.elmkusoma.academic.repository.SubjectRepository;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learner.domain.LearnerNotification;
import tz.elmkusoma.learner.repository.LearnerNotificationRepository;
import tz.elmkusoma.primary.domain.*;
import tz.elmkusoma.primary.dto.*;
import tz.elmkusoma.primary.repository.*;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.domain.TeacherAssignment;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PrimaryPortalService {

    private final PrimaryPortalRepository primaryPortalRepository;
    private final PortfolioItemRepository portfolioItemRepository;
    private final StudentBadgeRepository studentBadgeRepository;
    private final StudentStreakRepository studentStreakRepository;
    private final CurriculumTopicRepository curriculumTopicRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final LearnerNotificationRepository learnerNotificationRepository;
    private final LearningProfileRepository learningProfileRepository;
    private final DiscoveryEntryRepository discoveryEntryRepository;
    private final ReadingAdventureRepository readingAdventureRepository;
    private final LiveClassActivityRepository liveClassActivityRepository;
    private final LiveClassResponseRepository liveClassResponseRepository;
    private final LearningEvidenceRepository learningEvidenceRepository;
    private final LearningPassportRepository learningPassportRepository;
    private final QuestChallengeRepository questChallengeRepository;
    private final LearningCollaborationRepository learningCollaborationRepository;
    private final MistakeLabEntryRepository mistakeLabEntryRepository;
    private final ELmkusomaLabRepository elmkusomaLabRepository;
    private final SpeakingActivityRepository speakingActivityRepository;
    private final RealWorldMissionRepository realWorldMissionRepository;

    public PrimaryPortalService(PrimaryPortalRepository primaryPortalRepository,
                                 PortfolioItemRepository portfolioItemRepository,
                                 StudentBadgeRepository studentBadgeRepository,
                                 StudentStreakRepository studentStreakRepository,
                                 CurriculumTopicRepository curriculumTopicRepository,
                                 TeacherRepository teacherRepository,
                                 SubjectRepository subjectRepository,
                                 UserRepository userRepository,
                                 LearnerNotificationRepository learnerNotificationRepository,
                                 LearningProfileRepository learningProfileRepository,
                                 DiscoveryEntryRepository discoveryEntryRepository,
                                 ReadingAdventureRepository readingAdventureRepository,
                                 LiveClassActivityRepository liveClassActivityRepository,
                                 LiveClassResponseRepository liveClassResponseRepository,
                                 LearningEvidenceRepository learningEvidenceRepository,
                                 LearningPassportRepository learningPassportRepository,
                                 QuestChallengeRepository questChallengeRepository,
                                 LearningCollaborationRepository learningCollaborationRepository,
                                 MistakeLabEntryRepository mistakeLabEntryRepository,
                                 ELmkusomaLabRepository elmkusomaLabRepository,
                                 SpeakingActivityRepository speakingActivityRepository,
                                 RealWorldMissionRepository realWorldMissionRepository) {
        this.primaryPortalRepository = primaryPortalRepository;
        this.portfolioItemRepository = portfolioItemRepository;
        this.studentBadgeRepository = studentBadgeRepository;
        this.studentStreakRepository = studentStreakRepository;
        this.curriculumTopicRepository = curriculumTopicRepository;
        this.teacherRepository = teacherRepository;
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
        this.learnerNotificationRepository = learnerNotificationRepository;
        this.learningProfileRepository = learningProfileRepository;
        this.discoveryEntryRepository = discoveryEntryRepository;
        this.readingAdventureRepository = readingAdventureRepository;
        this.liveClassActivityRepository = liveClassActivityRepository;
        this.liveClassResponseRepository = liveClassResponseRepository;
        this.learningEvidenceRepository = learningEvidenceRepository;
        this.learningPassportRepository = learningPassportRepository;
        this.questChallengeRepository = questChallengeRepository;
        this.learningCollaborationRepository = learningCollaborationRepository;
        this.mistakeLabEntryRepository = mistakeLabEntryRepository;
        this.elmkusomaLabRepository = elmkusomaLabRepository;
        this.speakingActivityRepository = speakingActivityRepository;
        this.realWorldMissionRepository = realWorldMissionRepository;
    }

    public List<TeacherInfoResponse> getStudentTeachers(UUID studentId, UUID institutionId) {
        List<TeacherAssignment> assignments = primaryPortalRepository.findTeacherAssignmentsForStudent(studentId, institutionId);

        return assignments.stream()
                .map(ta -> {
                    Teacher teacher = teacherRepository.findById(ta.getTeacherId()).orElse(null);
                    if (teacher == null) {
                        return null;
                    }
                    User user = userRepository.findById(teacher.getUserId()).orElse(null);
                    Subject subject = subjectRepository.findById(ta.getSubjectId()).orElse(null);

                    TeacherInfoResponse response = new TeacherInfoResponse();
                    response.setId(teacher.getId());
                    response.setFirstName(user != null ? user.getFirstName() : null);
                    response.setLastName(user != null ? user.getLastName() : null);
                    response.setEmail(user != null ? user.getEmail() : null);
                    response.setSubjectName(subject != null ? subject.getName() : null);
                    response.setSpecialization(teacher.getSpecialization());
                    response.setProfileImageUrl(user != null ? user.getProfileImageUrl() : null);
                    return response;
                })
                .filter(r -> r != null)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<PortfolioItemResponse> getStudentPortfolio(UUID studentId, UUID institutionId) {
        List<PortfolioItem> items = portfolioItemRepository
                .findByStudentIdAndInstitutionIdAndIsDeletedFalseOrderByDisplayOrderAscCreatedAtDesc(studentId, institutionId);
        return items.stream().map(this::toPortfolioResponse).collect(Collectors.toList());
    }

    @Transactional
    public PortfolioItemResponse addPortfolioItem(UUID studentId, UUID institutionId, PortfolioItemRequest request) {
        PortfolioItem item = new PortfolioItem();
        item.setStudentId(studentId);
        item.setInstitutionId(institutionId);
        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setFileUrl(request.getFileUrl());
        item.setPortfolioType(request.getPortfolioType());
        item.setSubjectName(request.getSubjectName());
        item.setDisplayOrder(0);
        item.setIsFeatured(false);

        PortfolioItem saved = portfolioItemRepository.save(item);
        return toPortfolioResponse(saved);
    }

    @Transactional
    public void deletePortfolioItem(UUID studentId, UUID itemId) {
        PortfolioItem item = portfolioItemRepository.findByIdAndStudentIdAndIsDeletedFalse(itemId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("PortfolioItem", "id", itemId));
        item.setIsDeleted(true);
        portfolioItemRepository.save(item);
    }

    public List<BadgeResponse> getStudentBadges(UUID studentId, UUID institutionId) {
        List<StudentBadge> badges = studentBadgeRepository.findByStudentIdAndInstitutionIdAndIsDeletedFalse(studentId, institutionId);
        return badges.stream().map(this::toBadgeResponse).collect(Collectors.toList());
    }

    public StreakResponse getStudentStreak(UUID studentId, UUID institutionId) {
        StudentStreak streak = studentStreakRepository.findByStudentIdAndInstitutionIdAndIsDeletedFalse(studentId, institutionId)
                .orElse(null);

        StreakResponse response = new StreakResponse();
        if (streak != null) {
            response.setCurrentStreak(streak.getCurrentStreak());
            response.setLongestStreak(streak.getLongestStreak());
            response.setTotalPoints(streak.getTotalPoints());
            response.setLastActivityDate(streak.getLastActivityDate());
        } else {
            response.setCurrentStreak(0);
            response.setLongestStreak(0);
            response.setTotalPoints(0);
            response.setLastActivityDate(null);
        }
        return response;
    }

    public List<CurriculumTopic> getCurriculumTopics(UUID subjectId) {
        return curriculumTopicRepository.findBySubjectIdAndIsDeletedFalseOrderBySortOrderAsc(subjectId);
    }

    public List<LearnerNotification> getMyNotifications(UUID studentId, UUID institutionId) {
        return learnerNotificationRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(studentId);
    }

    public LearningProfileResponse getLearningProfile(UUID studentId) {
        LearningProfile profile = learningProfileRepository.findByStudentIdAndIsDeletedFalse(studentId)
                .orElse(null);

        if (profile == null) {
            profile = new LearningProfile();
            profile.setStudentId(studentId);
            profile.setLearningStyle("VISUAL");
            profile.setStrengths("[]");
            profile.setInterests("[]");
            profile.setGoals("");
            profile.setTotalPoints(0);
            profile.setLevel(1);
            profile = learningProfileRepository.save(profile);
        }

        return toLearningProfileResponse(profile);
    }

    @Transactional
    public LearningProfileResponse updateLearningProfile(UUID studentId, LearningProfileRequest request) {
        LearningProfile profile = learningProfileRepository.findByStudentIdAndIsDeletedFalse(studentId)
                .orElse(new LearningProfile());
        profile.setStudentId(studentId);
        profile.setLearningStyle(request.getLearningStyle());
        profile.setStrengths(request.getStrengths());
        profile.setInterests(request.getInterests());
        profile.setGoals(request.getGoals());

        LearningProfile saved = learningProfileRepository.save(profile);
        return toLearningProfileResponse(saved);
    }

    public List<DiscoveryEntryResponse> getDiscoveryEntries(UUID studentId, UUID institutionId) {
        List<DiscoveryEntry> entries = discoveryEntryRepository
                .findByStudentIdAndInstitutionIdAndIsDeletedFalseOrderByCreatedAtDesc(studentId, institutionId);
        return entries.stream().map(this::toDiscoveryEntryResponse).collect(Collectors.toList());
    }

    @Transactional
    public DiscoveryEntryResponse addDiscoveryEntry(UUID studentId, UUID institutionId, DiscoveryEntryRequest request) {
        DiscoveryEntry entry = new DiscoveryEntry();
        entry.setStudentId(studentId);
        entry.setInstitutionId(institutionId);
        entry.setTitle(request.getTitle());
        entry.setQuestion(request.getQuestion());
        entry.setDiscoveryType(request.getDiscoveryType());
        entry.setSubjectName(request.getSubjectName());
        entry.setIsResolved(false);

        DiscoveryEntry saved = discoveryEntryRepository.save(entry);
        return toDiscoveryEntryResponse(saved);
    }

    public List<ReadingAdventureResponse> getReadingAdventures(UUID studentId, UUID institutionId) {
        List<ReadingAdventure> adventures = readingAdventureRepository
                .findByStudentIdAndInstitutionIdAndIsDeletedFalseOrderByCreatedAtDesc(studentId, institutionId);
        return adventures.stream().map(this::toReadingAdventureResponse).collect(Collectors.toList());
    }

    @Transactional
    public ReadingAdventureResponse markReadingComplete(UUID studentId, UUID adventureId) {
        ReadingAdventure adventure = readingAdventureRepository.findByIdAndStudentIdAndIsDeletedFalse(adventureId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("ReadingAdventure", "id", adventureId));
        adventure.setTimesRead(adventure.getTimesRead() + 1);
        ReadingAdventure saved = readingAdventureRepository.save(adventure);
        return toReadingAdventureResponse(saved);
    }

    @Transactional
    public ReadingAdventureResponse toggleFavoriteReading(UUID studentId, UUID adventureId) {
        ReadingAdventure adventure = readingAdventureRepository.findByIdAndStudentIdAndIsDeletedFalse(adventureId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("ReadingAdventure", "id", adventureId));
        adventure.setIsFavorite(!adventure.getIsFavorite());
        ReadingAdventure saved = readingAdventureRepository.save(adventure);
        return toReadingAdventureResponse(saved);
    }

    public List<LiveClassActivityResponse> getLiveClassActivities(UUID liveClassId) {
        List<LiveClassActivity> activities = liveClassActivityRepository
                .findByLiveClassIdAndIsDeletedFalseOrderByOrderIndexAsc(liveClassId);
        return activities.stream().map(this::toLiveClassActivityResponse).collect(Collectors.toList());
    }

    @Transactional
    public LiveClassActivityResponse createLiveClassActivity(UUID liveClassId, LiveClassActivityRequest request, UUID createdById) {
        LiveClassActivity activity = new LiveClassActivity();
        activity.setLiveClassId(liveClassId);
        activity.setCreatedById(createdById);
        activity.setActivityType(request.getActivityType());
        activity.setTitle(request.getTitle());
        activity.setOptions(request.getOptions());
        activity.setCorrectAnswer(request.getCorrectAnswer());
        activity.setIsPublished(false);
        activity.setOrderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 0);

        LiveClassActivity saved = liveClassActivityRepository.save(activity);
        return toLiveClassActivityResponse(saved);
    }

    @Transactional
    public LiveClassResponseDTO respondToActivity(UUID activityId, UUID studentId, String selectedAnswer) {
        LiveClassActivity activity = liveClassActivityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("LiveClassActivity", "id", activityId));

        LiveClassResponse response = new LiveClassResponse();
        response.setLiveClassId(activity.getLiveClassId());
        response.setStudentId(studentId);
        response.setActivityType(activity.getActivityType());
        response.setSelectedAnswer(selectedAnswer);
        response.setRespondedAt(LocalDateTime.now());

        boolean isCorrect = activity.getCorrectAnswer() != null
                && activity.getCorrectAnswer().equalsIgnoreCase(selectedAnswer);
        response.setScore(isCorrect ? 1 : 0);

        LiveClassResponse saved = liveClassResponseRepository.save(response);
        return toLiveClassResponseDTO(saved);
    }

    public List<LearningEvidenceResponse> getLearningEvidence(UUID studentId, UUID institutionId) {
        List<LearningEvidence> evidence = learningEvidenceRepository
                .findByStudentIdAndInstitutionIdAndIsDeletedFalseOrderByCreatedAtDesc(studentId, institutionId);
        return evidence.stream().map(this::toLearningEvidenceResponse).collect(Collectors.toList());
    }

    @Transactional
    public LearningEvidenceResponse addLearningEvidence(UUID studentId, UUID institutionId, LearningEvidenceRequest request) {
        LearningEvidence evidence = new LearningEvidence();
        evidence.setStudentId(studentId);
        evidence.setInstitutionId(institutionId);
        evidence.setTitle(request.getTitle());
        evidence.setEvidenceType(request.getEvidenceType());
        evidence.setDescription(request.getDescription());
        evidence.setEvidenceUrl(request.getEvidenceUrl());
        evidence.setSubjectName(request.getSubjectName());
        evidence.setPoints(request.getPoints() != null ? request.getPoints() : 0);

        LearningEvidence saved = learningEvidenceRepository.save(evidence);
        return toLearningEvidenceResponse(saved);
    }

    public LearningPassportResponse getLearningPassport(UUID studentId, UUID institutionId) {
        LearningPassport passport = learningPassportRepository
                .findByStudentIdAndInstitutionIdAndIsDeletedFalse(studentId, institutionId)
                .orElse(null);

        if (passport == null) {
            passport = new LearningPassport();
            passport.setStudentId(studentId);
            passport.setInstitutionId(institutionId);
            passport.setStampsEarned(0);
            passport.setTotalStamps(100);
            passport.setCurrentCountry("Tanzania");
            passport.setLastActivity(LocalDateTime.now());
            passport = learningPassportRepository.save(passport);
        }

        return toLearningPassportResponse(passport);
    }

    public List<QuestChallengeResponse> getQuestChallenges(UUID studentId, UUID institutionId) {
        List<QuestChallenge> quests = questChallengeRepository
                .findByStudentIdAndInstitutionIdAndIsDeletedFalseOrderByCreatedAtDesc(studentId, institutionId);
        return quests.stream().map(this::toQuestChallengeResponse).collect(Collectors.toList());
    }

    @Transactional
    public QuestChallengeResponse completeQuest(UUID studentId, UUID questId, Integer score) {
        QuestChallenge quest = questChallengeRepository.findByIdAndStudentIdAndIsDeletedFalse(questId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("QuestChallenge", "id", questId));
        quest.setIsCompleted(true);
        quest.setScore(score);
        quest.setCompletedAt(LocalDateTime.now());

        QuestChallenge saved = questChallengeRepository.save(quest);
        return toQuestChallengeResponse(saved);
    }

    public List<MistakeLabEntryResponse> getMistakeLabEntries(UUID studentId, UUID institutionId) {
        List<MistakeLabEntry> entries = mistakeLabEntryRepository
                .findByStudentIdAndInstitutionIdAndIsDeletedFalseOrderByCreatedAtDesc(studentId, institutionId);
        return entries.stream().map(this::toMistakeLabEntryResponse).collect(Collectors.toList());
    }

    @Transactional
    public MistakeLabEntryResponse addMistakeLabEntry(UUID studentId, UUID institutionId, MistakeLabEntryRequest request) {
        MistakeLabEntry entry = new MistakeLabEntry();
        entry.setStudentId(studentId);
        entry.setInstitutionId(institutionId);
        entry.setQuestion(request.getQuestion());
        entry.setWrongAnswer(request.getWrongAnswer());
        entry.setCorrectAnswer(request.getCorrectAnswer());
        entry.setExplanation(request.getExplanation());
        entry.setSubjectName(request.getSubjectName());
        entry.setIsReviewed(false);

        MistakeLabEntry saved = mistakeLabEntryRepository.save(entry);
        return toMistakeLabEntryResponse(saved);
    }

    public List<LearningCollaboration> getCollaborations(UUID studentId, UUID institutionId) {
        return learningCollaborationRepository
                .findByStudentIdAndInstitutionIdAndIsDeletedFalseOrderByCreatedAtDesc(studentId, institutionId);
    }

    public List<ELmkusomaLabResponse> getLabs(UUID studentId, UUID institutionId) {
        List<ELmkusomaLab> labs = elmkusomaLabRepository
                .findByStudentIdAndInstitutionIdAndIsDeletedFalseOrderByCreatedAtDesc(studentId, institutionId);
        return labs.stream().map(this::toELmkusomaLabResponse).collect(Collectors.toList());
    }

    @Transactional
    public ELmkusomaLabResponse attemptLab(UUID studentId, UUID labId, ELmkusomaLabAttemptRequest request) {
        ELmkusomaLab lab = elmkusomaLabRepository.findByIdAndStudentIdAndIsDeletedFalse(labId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("ELmkusomaLab", "id", labId));
        lab.setIsAttempted(true);
        lab.setStudentNotes(request.getStudentNotes());
        lab.setScore(request.getScore());

        ELmkusomaLab saved = elmkusomaLabRepository.save(lab);
        return toELmkusomaLabResponse(saved);
    }

    public List<SpeakingActivityResponse> getSpeakingActivities(UUID studentId, UUID institutionId) {
        List<SpeakingActivity> activities = speakingActivityRepository
                .findByStudentIdAndInstitutionIdAndIsDeletedFalseOrderByCreatedAtDesc(studentId, institutionId);
        return activities.stream().map(this::toSpeakingActivityResponse).collect(Collectors.toList());
    }

    @Transactional
    public SpeakingActivityResponse addSpeakingActivity(UUID studentId, UUID institutionId, SpeakingActivityRequest request) {
        SpeakingActivity activity = new SpeakingActivity();
        activity.setStudentId(studentId);
        activity.setInstitutionId(institutionId);
        activity.setActivityType(request.getActivityType());
        activity.setTitle(request.getTitle());
        activity.setDescription(request.getDescription());
        activity.setSubjectName(request.getSubjectName());
        activity.setIsCompleted(false);

        SpeakingActivity saved = speakingActivityRepository.save(activity);
        return toSpeakingActivityResponse(saved);
    }

    @Transactional
    public SpeakingActivityResponse completeSpeakingActivity(UUID studentId, UUID activityId) {
        SpeakingActivity activity = speakingActivityRepository.findByIdAndStudentIdAndIsDeletedFalse(activityId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("SpeakingActivity", "id", activityId));
        activity.setIsCompleted(true);

        SpeakingActivity saved = speakingActivityRepository.save(activity);
        return toSpeakingActivityResponse(saved);
    }

    public List<RealWorldMissionResponse> getRealWorldMissions(UUID studentId, UUID institutionId) {
        List<RealWorldMission> missions = realWorldMissionRepository
                .findByStudentIdAndInstitutionIdAndIsDeletedFalseOrderByCreatedAtDesc(studentId, institutionId);
        return missions.stream().map(this::toRealWorldMissionResponse).collect(Collectors.toList());
    }

    @Transactional
    public RealWorldMissionResponse completeMission(UUID studentId, UUID missionId, String evidence) {
        RealWorldMission mission = realWorldMissionRepository.findByIdAndStudentIdAndIsDeletedFalse(missionId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("RealWorldMission", "id", missionId));
        mission.setIsCompleted(true);
        mission.setEvidence(evidence);

        RealWorldMission saved = realWorldMissionRepository.save(mission);
        return toRealWorldMissionResponse(saved);
    }

    public String getAIResponse(UUID studentId, String question) {
        String q = question.toLowerCase().trim();
        if (q.contains("fraction")) return "Fractions represent parts of a whole! For example, 1/2 means one part out of two equal parts. Think of a pizza cut into 2 slices — if you eat 1, you ate 1/2 of the pizza. Would you like me to give you a practice problem?";
        if (q.contains("water cycle")) return "The water cycle is how water moves around our planet! Evaporation → Condensation → Precipitation → Collection. It never stops!";
        if (q.contains("what should i study") || q.contains("next")) return "Check your Learning Map to see what topics are next. Try a practice quiz to test yourself!";
        if (q.contains("practice") || q.contains("quiz")) return "Great idea! Go to your Assignments or Assessments page to try a practice quiz. Learning by doing is the best way!";
        if (q.contains("writing") || q.contains("essay")) return "Writing tips: 1) Start with a clear main idea. 2) Use describing words. 3) Read your work out loud. 4) Check your spelling.";
        if (q.contains("today") || q.contains("learn")) return "Check your Learning Evidence page to see what you've accomplished today!";
        if (q.contains("hello") || q.contains("hi")) return "Hello! I'm your AI Learning Guide. Ask me about any subject!";
        if (q.contains("math")) return "Math is about patterns and problem-solving. Start with what you know and work step by step!";
        if (q.contains("science")) return "Science is about asking questions and finding answers through experiments. What topic are you curious about?";
        return "That's a great question! Try asking me about a specific subject like Math, Science, or English, or ask me to explain a topic or give you a practice problem.";
    }

    private PortfolioItemResponse toPortfolioResponse(PortfolioItem item) {
        PortfolioItemResponse response = new PortfolioItemResponse();
        response.setId(item.getId());
        response.setTitle(item.getTitle());
        response.setDescription(item.getDescription());
        response.setFileUrl(item.getFileUrl());
        response.setThumbnailUrl(item.getThumbnailUrl());
        response.setPortfolioType(item.getPortfolioType());
        response.setSubjectName(item.getSubjectName());
        response.setCreatedAt(item.getCreatedAt());
        return response;
    }

    private BadgeResponse toBadgeResponse(StudentBadge badge) {
        BadgeResponse response = new BadgeResponse();
        response.setId(badge.getId());
        response.setBadgeName(badge.getBadgeName());
        response.setBadgeType(badge.getBadgeType());
        response.setDescription(badge.getDescription());
        response.setIconUrl(badge.getIconUrl());
        response.setPoints(badge.getPoints());
        response.setAwardedAt(badge.getAwardedAt());
        return response;
    }

    private LearningProfileResponse toLearningProfileResponse(LearningProfile profile) {
        LearningProfileResponse response = new LearningProfileResponse();
        response.setId(profile.getId());
        response.setLearningStyle(profile.getLearningStyle());
        response.setStrengths(profile.getStrengths());
        response.setInterests(profile.getInterests());
        response.setGoals(profile.getGoals());
        response.setTotalPoints(profile.getTotalPoints());
        response.setLevel(profile.getLevel());
        return response;
    }

    private DiscoveryEntryResponse toDiscoveryEntryResponse(DiscoveryEntry entry) {
        DiscoveryEntryResponse response = new DiscoveryEntryResponse();
        response.setId(entry.getId());
        response.setTitle(entry.getTitle());
        response.setQuestion(entry.getQuestion());
        response.setDiscoveryType(entry.getDiscoveryType());
        response.setSubjectName(entry.getSubjectName());
        response.setResult(entry.getResult());
        response.setIsResolved(entry.getIsResolved());
        response.setEvidence(entry.getEvidence());
        response.setCreatedAt(entry.getCreatedAt());
        return response;
    }

    private ReadingAdventureResponse toReadingAdventureResponse(ReadingAdventure adventure) {
        ReadingAdventureResponse response = new ReadingAdventureResponse();
        response.setId(adventure.getId());
        response.setTitle(adventure.getTitle());
        response.setContent(adventure.getContent());
        response.setSubjectName(adventure.getSubjectName());
        response.setReadingLevel(adventure.getReadingLevel());
        response.setWordCount(adventure.getWordCount());
        response.setReadTimeMinutes(adventure.getReadTimeMinutes());
        response.setTimesRead(adventure.getTimesRead());
        response.setIsFavorite(adventure.getIsFavorite());
        response.setCoverColor(adventure.getCoverColor());
        response.setCreatedAt(adventure.getCreatedAt());
        return response;
    }

    private LiveClassActivityResponse toLiveClassActivityResponse(LiveClassActivity activity) {
        LiveClassActivityResponse response = new LiveClassActivityResponse();
        response.setId(activity.getId());
        response.setActivityType(activity.getActivityType());
        response.setTitle(activity.getTitle());
        response.setOptions(activity.getOptions());
        response.setCorrectAnswer(activity.getCorrectAnswer());
        response.setIsPublished(activity.getIsPublished());
        return response;
    }

    private LiveClassResponseDTO toLiveClassResponseDTO(LiveClassResponse resp) {
        LiveClassResponseDTO response = new LiveClassResponseDTO();
        response.setId(resp.getId());
        response.setActivityType(resp.getActivityType());
        response.setSelectedAnswer(resp.getSelectedAnswer());
        response.setScore(resp.getScore());
        response.setRespondedAt(resp.getRespondedAt());
        return response;
    }

    private LearningEvidenceResponse toLearningEvidenceResponse(LearningEvidence evidence) {
        LearningEvidenceResponse response = new LearningEvidenceResponse();
        response.setId(evidence.getId());
        response.setTitle(evidence.getTitle());
        response.setEvidenceType(evidence.getEvidenceType());
        response.setDescription(evidence.getDescription());
        response.setEvidenceUrl(evidence.getEvidenceUrl());
        response.setSubjectName(evidence.getSubjectName());
        response.setPoints(evidence.getPoints());
        response.setCreatedAt(evidence.getCreatedAt());
        return response;
    }

    private LearningPassportResponse toLearningPassportResponse(LearningPassport passport) {
        LearningPassportResponse response = new LearningPassportResponse();
        response.setId(passport.getId());
        response.setStampsEarned(passport.getStampsEarned());
        response.setTotalStamps(passport.getTotalStamps());
        response.setCurrentCountry(passport.getCurrentCountry());
        response.setLastActivity(passport.getLastActivity());
        return response;
    }

    private QuestChallengeResponse toQuestChallengeResponse(QuestChallenge quest) {
        QuestChallengeResponse response = new QuestChallengeResponse();
        response.setId(quest.getId());
        response.setTitle(quest.getTitle());
        response.setDescription(quest.getDescription());
        response.setQuestType(quest.getQuestType());
        response.setDifficulty(quest.getDifficulty());
        response.setSubjectName(quest.getSubjectName());
        response.setIsCompleted(quest.getIsCompleted());
        response.setScore(quest.getScore());
        response.setTotalPoints(quest.getTotalPoints());
        response.setCompletedAt(quest.getCompletedAt());
        return response;
    }

    private MistakeLabEntryResponse toMistakeLabEntryResponse(MistakeLabEntry entry) {
        MistakeLabEntryResponse response = new MistakeLabEntryResponse();
        response.setId(entry.getId());
        response.setQuestion(entry.getQuestion());
        response.setWrongAnswer(entry.getWrongAnswer());
        response.setCorrectAnswer(entry.getCorrectAnswer());
        response.setExplanation(entry.getExplanation());
        response.setSubjectName(entry.getSubjectName());
        response.setIsReviewed(entry.getIsReviewed());
        return response;
    }

    private ELmkusomaLabResponse toELmkusomaLabResponse(ELmkusomaLab lab) {
        ELmkusomaLabResponse response = new ELmkusomaLabResponse();
        response.setId(lab.getId());
        response.setStudentId(lab.getStudentId());
        response.setInstitutionId(lab.getInstitutionId());
        response.setLabTitle(lab.getLabTitle());
        response.setLabType(lab.getLabType());
        response.setHypothesis(lab.getHypothesis());
        response.setMaterialsList(lab.getMaterialsList());
        response.setSteps(lab.getSteps());
        response.setExpectedResult(lab.getExpectedResult());
        response.setStudentNotes(lab.getStudentNotes());
        response.setIsAttempted(lab.getIsAttempted());
        response.setScore(lab.getScore());
        return response;
    }

    private SpeakingActivityResponse toSpeakingActivityResponse(SpeakingActivity activity) {
        SpeakingActivityResponse response = new SpeakingActivityResponse();
        response.setId(activity.getId());
        response.setStudentId(activity.getStudentId());
        response.setInstitutionId(activity.getInstitutionId());
        response.setActivityType(activity.getActivityType());
        response.setTitle(activity.getTitle());
        response.setDescription(activity.getDescription());
        response.setAudioUrl(activity.getAudioUrl());
        response.setImageUrl(activity.getImageUrl());
        response.setSubjectName(activity.getSubjectName());
        response.setIsCompleted(activity.getIsCompleted());
        response.setDurationSeconds(activity.getDurationSeconds());
        return response;
    }

    private RealWorldMissionResponse toRealWorldMissionResponse(RealWorldMission mission) {
        RealWorldMissionResponse response = new RealWorldMissionResponse();
        response.setId(mission.getId());
        response.setStudentId(mission.getStudentId());
        response.setInstitutionId(mission.getInstitutionId());
        response.setMissionTitle(mission.getMissionTitle());
        response.setMissionType(mission.getMissionType());
        response.setDescription(mission.getDescription());
        response.setLocation(mission.getLocation());
        response.setInstructions(mission.getInstructions());
        response.setEvidence(mission.getEvidence());
        response.setIsCompleted(mission.getIsCompleted());
        response.setPoints(mission.getPoints());
        return response;
    }
}
