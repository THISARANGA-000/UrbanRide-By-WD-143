package com.urbanride.component2;

import com.urbanride.component1.User;
import com.urbanride.component3.Booking;
import com.urbanride.component3.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/drivers")
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class DriverController {

    @Autowired
    private DriverRepository driverRepo;

    @Autowired
    private BookingRepository bookingRepo;

    @Autowired
    private com.urbanride.component1.FeedbackRepository feedbackRepo;

    // driverge application eka register karannawa
    @PostMapping("/register")
    public ResponseEntity<?> registerDriver(@RequestBody Map<String, Object> payload) {
        String email = (payload.get("email") != null) ? payload.get("email").toString() : null;
        String password = (payload.get("password") != null) ? payload.get("password").toString() : null;
        String name = (payload.get("name") != null) ? payload.get("name").toString() : null;

        if (email == null || password == null || name == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name, email and password are required"));
        }

        if (driverRepo.existsByEmailIgnoreCase(email.trim())) {
            return ResponseEntity.status(409).body(Map.of("error", "Email already registered as a driver"));
        }

        String nic = payload.get("nic") != null ? payload.get("nic").toString() : null;
        String lic = payload.get("licenseNo") != null ? payload.get("licenseNo").toString() : null;
        String finalLic = lic != null ? lic : (nic != null ? nic : "");

        if (finalLic != null && !finalLic.trim().isEmpty() && driverRepo.existsByLicenseNo(finalLic.trim())) {
            return ResponseEntity.status(409).body(Map.of("error", "NIC/License number already registered"));
        }

        Driver d = new Driver();
        d.setName(name);
        d.setEmail(email);
        d.setPasswordHash(User.hashPassword(password));
        d.setMobile(payload.getOrDefault("mobile", "").toString());
        d.setRole("driver");
        
        // 'nic' ekei 'licenseNo' ekei keys frontend eken handel karanawa
        d.setLicenseNo(finalLic);
        
        d.setVehiclePlate(payload.getOrDefault("vehiclePlate", "").toString());
        d.setVehicleModel(payload.getOrDefault("vehicleModel", "").toString());
        d.setVehicleType(payload.getOrDefault("vehicleType", "Car").toString());
        d.setFuelType(payload.getOrDefault("fuelType", "Gasoline").toString());
        d.setStatus("Available");

        Driver saved = driverRepo.save(d);
        
        // driverge files walata log wenawa
        try {
            logDriverToFile(saved);
        } catch (Exception e) {
            System.err.println("Non-critical error: Could not log driver to file: " + e.getMessage());
        }
        
        return ResponseEntity.ok(Map.of(
            "message", "Driver application submitted successfully",
            "driverId", saved.getUserId(),
            "name", saved.getName(),
            "role", "driver"
        ));
    }

    // okkoma driversla..
    @GetMapping("/all")
    public ResponseEntity<?> getAllDrivers() {
        java.util.List<java.util.Map<String, Object>> responseList = new java.util.ArrayList<>();
        for (Driver d : driverRepo.findAll()) {
            // booking table eken calc karanawa
            java.util.List<Booking> allBookingsForD = bookingRepo.findByDriverIdOrderByCreatedAtDesc(d.getUserId());
            long realTrips = allBookingsForD.stream()
                .filter(b -> "Completed".equalsIgnoreCase(b.getStatus()) || "Paid".equalsIgnoreCase(b.getStatus()))
                .count();
            double realEarnings = allBookingsForD.stream()
                .filter(b -> "Completed".equalsIgnoreCase(b.getStatus()) || "Paid".equalsIgnoreCase(b.getStatus()))
                .mapToDouble(Booking::getFare)
                .sum();

            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("driverId", d.getUserId());
            map.put("formattedId", String.format("D%03d", d.getUserId() % 100));
            map.put("name", d.getName() != null ? d.getName() : "Unknown");
            map.put("email", d.getEmail() != null ? d.getEmail() : "");
            map.put("mobile", d.getMobile() != null ? d.getMobile() : "");
            map.put("rating", d.getRating() != null ? d.getRating() : 5.0);
            map.put("totalTrips", realTrips);
            map.put("totalEarnings", realEarnings);
            map.put("vehiclePlate", d.getVehiclePlate() != null ? d.getVehiclePlate() : "");
            map.put("vehicleModel", d.getVehicleModel() != null ? d.getVehicleModel() : "");
            map.put("vehicleType", d.getVehicleType() != null ? d.getVehicleType() : "Car");
            map.put("status", d.getStatus() != null ? d.getStatus() : "Offline");
            map.put("memberSince", d.getCreatedAt());
            responseList.add(map);
        }
        return ResponseEntity.ok(responseList);
    }

    // login
    @PostMapping("/login")
    public ResponseEntity<?> loginDriver(@RequestBody Map<String, Object> payload) {
        try {
            String email = (payload.get("email") != null) ? payload.get("email").toString() : null;
            String password = (payload.get("password") != null) ? payload.get("password").toString() : null;

            if (email == null || password == null || password.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
            }

            Optional<Driver> opt = driverRepo.findByEmailIgnoreCase(email.trim());
            if (opt.isEmpty()) {
                System.out.println("Driver Login FAILED: User not found -> " + email);
                return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
            }

            Driver d = opt.get();
            
            if (d.getPasswordHash() == null) {
                return ResponseEntity.status(500).body(Map.of("error", "Driver account is corrupted (missing password)"));
            }

            if (!d.getPasswordHash().equals(User.hashPassword(password))) {
                System.out.println("Driver Login FAILED: Password mismatch for -> " + email);
                return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
            }

            // vehicle type eka validate karanawa
            String requestedVehicle = payload.get("vehicleType") != null ? payload.get("vehicleType").toString() : "";
            if (!requestedVehicle.isEmpty() && !requestedVehicle.equalsIgnoreCase(d.getVehicleType())) {
                return ResponseEntity.status(401).body(Map.of("error", "Selected vehicle type (" + requestedVehicle + ") does not match your registered type (" + d.getVehicleType() + ")."));
            }

            System.out.println("Driver Login SUCCESS: " + email);


            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("message", "Login successful");
            response.put("driverId", d.getUserId());
            response.put("formattedId", String.format("D%03d", d.getUserId() % 100));
            response.put("name", d.getName() != null ? d.getName() : "Unknown");
            response.put("email", d.getEmail() != null ? d.getEmail() : "");
            response.put("rating", d.getRating() != null ? d.getRating() : 5.0);
            response.put("totalTrips", d.getTotalTrips() != null ? d.getTotalTrips() : 0);
            response.put("vehiclePlate", d.getVehiclePlate() != null ? d.getVehiclePlate() : "");
            response.put("vehicleModel", d.getVehicleModel() != null ? d.getVehicleModel() : "");
            response.put("status", d.getStatus() != null ? d.getStatus() : "Offline");
            response.put("role", "driver");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            java.util.Map<String, String> err = new java.util.HashMap<>();
            err.put("error", "Server Error: " + (e.getMessage() != null ? e.getMessage() : "Unknown"));
            return ResponseEntity.status(500).body(err);
        }
    }


    @PutMapping("/{driverId}")
    public ResponseEntity<?> updateProfile(@PathVariable int driverId, @RequestBody Map<String, Object> payload) {
        return driverRepo.findById(driverId)
            .map(d -> {
                if (payload.containsKey("name")) d.setName(payload.get("name").toString());
                if (payload.containsKey("mobile")) d.setMobile(payload.get("mobile").toString());
                if (payload.containsKey("email")) d.setEmail(payload.get("email").toString());
                if (payload.containsKey("vehiclePlate")) d.setVehiclePlate(payload.get("vehiclePlate").toString());
                if (payload.containsKey("vehicleModel")) d.setVehicleModel(payload.get("vehicleModel").toString());
                /*
                logic eka .....
                if (payload.containsKey("driverId")) {
                int driverIdField = Integer.parseInt(payload.get("driverId").toString());
                Optional<Driver> drOpt = driverRepo.findById(driverIdField);
                if (drOpt.isPresent()) {
                if ("Pending Approval".equalsIgnoreCase(drOpt.get().getStatus())) {
                return ResponseEntity.status(403).body(Map.of("error", "Unapproved drivers cannot accept bookings."));
                }
                }
                }
                */

                driverRepo.save(d);
                return ResponseEntity.ok(Map.of("message", "Profile updated successfully", "name", d.getName()));
            })
            .orElse(ResponseEntity.notFound().build());
    }


    @GetMapping("/{driverId}")
    public ResponseEntity<?> getDriver(@PathVariable int driverId) {
        return driverRepo.findById(driverId)
            .map(d -> {
                // Calculate
                java.util.List<Booking> driverBookings = bookingRepo.findByDriverIdOrderByCreatedAtDesc(d.getUserId());
                long realTrips = driverBookings.stream()
                    .filter(b -> "Completed".equalsIgnoreCase(b.getStatus()) || "Paid".equalsIgnoreCase(b.getStatus()))
                    .count();
                double realEarnings = driverBookings.stream()
                    .filter(b -> "Completed".equalsIgnoreCase(b.getStatus()) || "Paid".equalsIgnoreCase(b.getStatus()))
                    .mapToDouble(Booking::getFare)
                    .sum();

                java.util.Map<String, Object> response = new java.util.HashMap<>();
                response.put("driverId", d.getUserId());
                response.put("name", d.getName() != null ? d.getName() : "Unknown");
                response.put("email", d.getEmail() != null ? d.getEmail() : "");
                response.put("mobile", d.getMobile() != null ? d.getMobile() : "");
                response.put("rating", d.getRating() != null ? d.getRating() : 5.0);
                response.put("totalTrips", realTrips);
                response.put("totalEarnings", realEarnings);
                response.put("vehiclePlate", d.getVehiclePlate() != null ? d.getVehiclePlate() : "");
                response.put("vehicleModel", d.getVehicleModel() != null ? d.getVehicleModel() : "");
                response.put("vehicleType", d.getVehicleType() != null ? d.getVehicleType() : "Car");
                response.put("status", d.getStatus() != null ? d.getStatus() : "Offline");
                response.put("memberSince", d.getCreatedAt());
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }


    @PutMapping("/{driverId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable int driverId, @RequestBody Map<String, Object> payload) {
        Optional<Driver> opt = driverRepo.findById(driverId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Driver d = opt.get();
        String newStatus = payload.get("status") != null ? payload.get("status").toString() : "";
        if ("online".equalsIgnoreCase(newStatus)) {
            /*
            logic eka ...
            if ("Pending Approval".equalsIgnoreCase(d.getStatus())) {
            return ResponseEntity.status(403).body(Map.of("error", "Your account is pending approval by admin."));
            }
            */
            d.goOnline();
        } else {
            d.goOffline();
        }

        driverRepo.save(d);
        return ResponseEntity.ok(Map.of("message", "Status updated", "status", d.getStatus()));
    }


    @DeleteMapping("/{driverId}")
    public ResponseEntity<?> deleteProfile(@PathVariable int driverId) {
        if (!driverRepo.existsById(driverId)) return ResponseEntity.notFound().build();
        driverRepo.deleteById(driverId);
        return ResponseEntity.ok(Map.of("message", "Driver account deleted successfully"));
    }

    @PutMapping("/{driverId}/rate")
    public ResponseEntity<?> rateDriver(@PathVariable int driverId, @RequestBody java.util.Map<String, Object> payload) {
        try {
            double stars = Double.parseDouble(payload.get("rating").toString());
            return driverRepo.findById(driverId)
                .map(d -> {
                    d.addRating(stars);
                    driverRepo.save(d);
                    
                    // feedback file eke store..
                    int passengerId = payload.get("passengerId") != null ? Integer.parseInt(payload.get("passengerId").toString()) : 0;
                    com.urbanride.component1.Feedback feedback = new com.urbanride.component1.Feedback(
                        passengerId, d.getUserId(), stars, payload.getOrDefault("comment", "No comment").toString()
                    );
                    feedbackRepo.save(feedback);

                    logFeedbackToFile(d.getUserId(), stars, payload.getOrDefault("comment", "No comment").toString());
                    
                    return ResponseEntity.ok(java.util.Map.of("message", "Rating submitted", "newRating", d.getRating()));
                })
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(400).body(java.util.Map.of("error", "Invalid rating request"));
        }
    }

    private void logDriverToFile(Driver d) {
        try (FileWriter fw = new FileWriter("drivers.txt", true);
             PrintWriter out = new PrintWriter(fw)) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            out.println(String.format("[%s] NEW DRIVER REGISTRATION", timestamp));
            out.println(String.format("   - ID: D%03d", d.getUserId() % 100));
            out.println(String.format("   - Name: %s", d.getName()));
            out.println(String.format("   - Email: %s", d.getEmail()));
            out.println(String.format("   - Mobile: %s", d.getMobile()));
            out.println(String.format("   - NIC: %s", d.getLicenseNo())); // LicenseNo is used for NIC in this context
            out.println(String.format("   - Vehicle: %s (%s)", d.getVehicleModel(), d.getVehicleType()));
            out.println(String.format("   - Plate: %s", d.getVehiclePlate()));
            out.println(String.format("   - Status: %s", d.getStatus()));
            out.println("--------------------------------------------------");
        } catch (Exception e) {
            System.err.println("Error writing to drivers.txt: " + e.getMessage());
        }
    }

    private void logFeedbackToFile(int driverId, double stars, String comment) {
        try (FileWriter fw = new FileWriter("feedback.txt", true);
             PrintWriter out = new PrintWriter(fw)) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            out.println(String.format("[%s] Driver ID: D%03d | Rating: %.1f stars | Comment: %s",
                timestamp, driverId % 100, stars, comment));
        } catch (Exception e) {
            System.err.println("Error writing to feedback.txt: " + e.getMessage());
        }
    }
}

