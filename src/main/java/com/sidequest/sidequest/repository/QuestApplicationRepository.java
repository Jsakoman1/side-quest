package com.sidequest.sidequest.repository;

import com.sidequest.sidequest.model.QuestApplication;
import com.sidequest.sidequest.model.QuestApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestApplicationRepository extends JpaRepository<QuestApplication, Long> {

    List<QuestApplication> findByQuestId(Long questId);

    boolean existsByQuestIdAndApplicantId(Long questId, Long applicantId);

    Optional<QuestApplication> findByIdAndQuestId(Long id, Long questId);

    List<QuestApplication> findByQuestIdAndStatus(Long questId, QuestApplicationStatus status);
}
