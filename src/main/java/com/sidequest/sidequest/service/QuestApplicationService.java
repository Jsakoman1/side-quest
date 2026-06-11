package com.sidequest.sidequest.service;

import com.sidequest.sidequest.dto.QuestApplicationRequestDTO;
import com.sidequest.sidequest.dto.QuestApplicationResponseDTO;
import com.sidequest.sidequest.mapper.QuestApplicationMgr;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.model.Quest;
import com.sidequest.sidequest.model.QuestApplication;
import com.sidequest.sidequest.model.QuestApplicationStatus;
import com.sidequest.sidequest.model.QuestStatus;
import com.sidequest.sidequest.repository.QuestApplicationRepository;
import com.sidequest.sidequest.repository.QuestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class QuestApplicationService {

    private final QuestApplicationRepository questApplicationRepository;
    private final QuestRepository questRepository;
    private final QuestApplicationMgr questApplicationMgr;

    public QuestApplicationService(
            QuestApplicationRepository questApplicationRepository,
            QuestRepository questRepository,
            QuestApplicationMgr questApplicationMgr
    ) {
        this.questApplicationRepository = questApplicationRepository;
        this.questRepository = questRepository;
        this.questApplicationMgr = questApplicationMgr;
    }

    public QuestApplicationResponseDTO applyForQuest(Long questId, QuestApplicationRequestDTO dto, AppUser currentUser) {
        Quest quest = findQuestOrThrow(questId);
        validateQuestIsOpen(quest);
        validateNotQuestCreator(quest, currentUser);
        validateNoDuplicateApplication(questId, currentUser.getId());

        QuestApplication application = questApplicationMgr.toEntity(dto, quest, currentUser);
        QuestApplication savedApplication = questApplicationRepository.save(application);

        return questApplicationMgr.toDto(savedApplication);
    }

    public List<QuestApplicationResponseDTO> getApplicationsForQuest(Long questId, AppUser currentUser) {
        Quest quest = findQuestOrThrow(questId);
        validateQuestOwner(quest, currentUser);

        return questApplicationRepository.findByQuestId(questId)
                .stream()
                .map(questApplicationMgr::toDto)
                .toList();
    }

    @Transactional
    public QuestApplicationResponseDTO acceptApplication(Long questId, Long applicationId, AppUser currentUser) {
        Quest quest = findQuestOrThrow(questId);
        validateQuestOwner(quest, currentUser);
        validateQuestIsOpen(quest);

        QuestApplication application = findPendingApplicationOrThrow(questId, applicationId);
        application.setStatus(QuestApplicationStatus.ACCEPTED);
        quest.setStatus(QuestStatus.ASSIGNED);

        rejectOtherPendingApplications(questId, applicationId);
        questRepository.save(quest);
        return questApplicationMgr.toDto(questApplicationRepository.save(application));
    }

    @Transactional
    public QuestApplicationResponseDTO rejectApplication(Long questId, Long applicationId, AppUser currentUser) {
        Quest quest = findQuestOrThrow(questId);
        validateQuestOwner(quest, currentUser);
        validateQuestIsOpen(quest);

        QuestApplication application = findPendingApplicationOrThrow(questId, applicationId);
        application.setStatus(QuestApplicationStatus.REJECTED);

        return questApplicationMgr.toDto(questApplicationRepository.save(application));
    }

    private Quest findQuestOrThrow(Long questId) {
        return questRepository.findById(questId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quest not found with id " + questId));
    }

    private QuestApplication findPendingApplicationOrThrow(Long questId, Long applicationId) {
        QuestApplication application = questApplicationRepository.findByIdAndQuestId(applicationId, questId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quest application not found with id " + applicationId));

        if (application.getStatus() != QuestApplicationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending applications can be updated");
        }

        return application;
    }

    private void rejectOtherPendingApplications(Long questId, Long acceptedApplicationId) {
        List<QuestApplication> pendingApplications = questApplicationRepository.findByQuestIdAndStatus(questId, QuestApplicationStatus.PENDING);
        for (QuestApplication application : pendingApplications) {
            if (!Objects.equals(application.getId(), acceptedApplicationId)) {
                application.setStatus(QuestApplicationStatus.REJECTED);
                questApplicationRepository.save(application);
            }
        }
    }

    private void validateQuestIsOpen(Quest quest) {
        if (quest.getStatus() != QuestStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Applications are only allowed for open quests");
        }
    }

    private void validateNotQuestCreator(Quest quest, AppUser currentUser) {
        if (quest.getCreator().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quest creator cannot apply to their own quest");
        }
    }

    private void validateNoDuplicateApplication(Long questId, Long applicantId) {
        if (questApplicationRepository.existsByQuestIdAndApplicantId(questId, applicantId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already applied for this quest");
        }
    }

    private void validateQuestOwner(Quest quest, AppUser currentUser) {
        if (!quest.getCreator().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to view these applications");
        }
    }
}
