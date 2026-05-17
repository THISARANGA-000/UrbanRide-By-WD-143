package com.urbanride.component1;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {
    List<Feedback> findByPassengerIdOrderByCreatedAtDesc(int passengerId);
    List<Feedback> findByDriverIdOrderByCreatedAtDesc(int driverId);
}
