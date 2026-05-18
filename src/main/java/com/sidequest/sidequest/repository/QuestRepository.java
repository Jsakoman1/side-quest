package com.sidequest.sidequest.repository;

import com.sidequest.sidequest.model.Quest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestRepository extends JpaRepository<Quest, Long> {
}