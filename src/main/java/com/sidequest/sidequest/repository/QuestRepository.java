package com.sidequest.sidequest.repository;

import com.sidequest.sidequest.model.Quest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface QuestRepository extends JpaRepository<Quest, Long> {

    @Query("select q from Quest q join fetch q.creator")
    List<Quest> findAllWithCreator();

    @Query("select q from Quest q join fetch q.creator where q.id = :id")
    Optional<Quest> findByIdWithCreator(Long id);
}
