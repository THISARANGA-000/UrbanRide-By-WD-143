package com.urbanride.component3;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {
    List<Booking> findByPassengerIdOrderByCreatedAtDesc(int passengerId);
    List<Booking> findByDriverIdOrderByCreatedAtDesc(int driverId);
    List<Booking> findAllByOrderByCreatedAtDesc();
    List<Booking> findByStatusOrderByCreatedAtDesc(String status);
    long countByPassengerId(int passengerId);
    long countByDriverIdAndStatus(int driverId, String status);
}

