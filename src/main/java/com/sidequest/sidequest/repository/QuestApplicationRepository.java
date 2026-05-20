package com.sidequest.sidequest.repository;

import com.sidequest.sidequest.model.QuestApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestApplicationRepository extends JpaRepository<QuestApplication, Long> {

    List<QuestApplication> findByQuestId(Long questId);
    
}