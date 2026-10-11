package com.kashi.portal.feedback;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    /** Loads the user together with each feedback row so listing a page does not run one query per row. */
    @Override
    @EntityGraph(attributePaths = "user")
    Page<Feedback> findAll(Pageable pageable);
}
