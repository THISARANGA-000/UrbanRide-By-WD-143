package com.urbanride.component1;

import com.urbanride.component2.Driver;
import com.urbanride.component2.DriverRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedbacks")
@CrossOrigin(origins = "*")
public class FeedbackController {

    @Autowired
    private FeedbackRepository feedbackRepo;

    @Autowired
    private DriverRepository driverRepo;

    @GetMapping("/passenger/{passengerId}")
    public ResponseEntity<?> getPassengerFeedbacks(@PathVariable String passengerId) {
        try {
            int pId = Integer.parseInt(passengerId.replaceAll("[^\\d]", ""));
            List<Feedback> feedbacks = feedbackRepo.findByPassengerIdOrderByCreatedAtDesc(pId);
            List<Map<String, Object>> responseList = new ArrayList<>();

            // --- DUMMY DATA FOR DEMO ---
            if (feedbacks.isEmpty()) {
                responseList.add(createMockFeedback(pId, 1, 5.0, "Excellent driver! Very safe and punctual."));
                responseList.add(createMockFeedback(pId, 2, 4.0, "Good ride, but the vehicle could be cleaner."));
                responseList.add(createMockFeedback(pId, 3, 5.0, "Super friendly driver, highly recommended."));
                return ResponseEntity.ok(responseList);
            }
            // ---------------------------

            for (Feedback f : feedbacks) {
                Map<String, Object> map = new HashMap<>();
                map.put("feedbackId", f.getFeedbackId());
                map.put("driverId", f.getDriverId());
                map.put("rating", f.getRating());
                map.put("comment", f.getComment());
                map.put("createdAt", f.getCreatedAt());

                String driverName = driverRepo.findById(f.getDriverId())
                                             .map(Driver::getName)
                                             .orElse("UrbanRide Driver");
                map.put("driverName", driverName);
                
                responseList.add(map);
            }
            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Server error: " + e.getMessage()));
        }
    }

    private Map<String, Object> createMockFeedback(int pId, int dId, double stars, String comment) {
        Map<String, Object> map = new HashMap<>();
        map.put("feedbackId", 1000 + dId);
        map.put("driverId", dId);
        map.put("rating", stars);
        map.put("comment", comment);
        map.put("createdAt", LocalDateTime.now().minusDays(dId));
        
        String driverName = driverRepo.findById(dId).map(Driver::getName).orElse("UrbanRide Driver #" + dId);
        map.put("driverName", driverName);
        return map;
    }

    @PutMapping("/{feedbackId}")
    public ResponseEntity<?> updateFeedback(@PathVariable int feedbackId, @RequestBody Map<String, Object> updates) {
        try {
            // Mock update for dummy data
            if (feedbackId >= 1000) {
                return ResponseEntity.ok(Map.of("message", "Mock feedback updated successfully"));
            }

            Feedback feedback = feedbackRepo.findById(feedbackId)
                    .orElseThrow(() -> new RuntimeException("Feedback not found"));

            if (updates.containsKey("rating")) {
                feedback.setRating(((Number) updates.get("rating")).doubleValue());
            }
            if (updates.containsKey("comment")) {
                feedback.setComment((String) updates.get("comment"));
            }

            feedbackRepo.save(feedback);
            return ResponseEntity.ok(Map.of("message", "Feedback updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Update failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{feedbackId}")
    public ResponseEntity<?> deleteFeedback(@PathVariable int feedbackId) {
        try {
            // Mock delete for dummy data
            if (feedbackId >= 1000) {
                return ResponseEntity.ok(Map.of("message", "Mock feedback deleted successfully"));
            }
            
            if (!feedbackRepo.existsById(feedbackId)) {
                return ResponseEntity.notFound().build();
            }
            feedbackRepo.deleteById(feedbackId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Feedback deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Delete failed: " + e.getMessage()));
        }
    }
}
