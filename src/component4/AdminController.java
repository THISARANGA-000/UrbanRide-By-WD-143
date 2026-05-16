package com.urbanride.component4;

import com.urbanride.component1.PassengerRepository;
import com.urbanride.component2.Driver;
import com.urbanride.component2.DriverRepository;
import com.urbanride.component3.Booking;
import com.urbanride.component3.BookingRepository;
import com.urbanride.component1.User;
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
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private AdminRepository adminRepo;

    @Autowired
    private PassengerRepository passengerRepo;

    @Autowired
    private DriverRepository driverRepo;

    @Autowired
    private BookingRepository bookingRepo;

    @PostMapping("/register")
    public ResponseEntity<?> registerAdmin(@RequestBody Map<String, Object> payload) {
        String email = payload.get("email") != null ? payload.get("email").toString() : null;
        String password = payload.get("password") != null ? payload.get("password").toString() : null;
        String name = payload.get("name") != null ? payload.get("name").toString() : null;

        if (email == null || password == null || name == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name, email and password are required"));
        }

        if (adminRepo.existsByEmailIgnoreCase(email)) {
            return ResponseEntity.status(409).body(Map.of("error", "Email already registered as admin"));
        }

        Admin a = new Admin();
        a.setName(name);
        a.setEmail(email);
        a.setPasswordHash(User.hashPassword(password));
        a.setMobile(payload.getOrDefault("mobile", "").toString());
        a.setAdminRole(payload.getOrDefault("adminRole", "Support").toString());
        a.setActive(true);
        a.setRole("admin");

        Admin saved = adminRepo.save(a);


        logAdminToFile(saved);

        return ResponseEntity.ok(Map.of(
            "message", "Admin registered successfully",
            "adminId", saved.getUserId(),
            "name", saved.getName()
        ));
    }

    private void logAdminToFile(Admin a) {
        try (FileWriter fw = new FileWriter("admin.txt", true);
             PrintWriter out = new PrintWriter(fw)) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            out.println(String.format("[%s] New Admin: ID A%03d | Name: %s | Email: %s | Role: %s",
                timestamp, a.getUserId() - 1, a.getName(), a.getEmail(), a.getAdminRole()));
        } catch (Exception e) {
            System.err.println("Error writing to admin.txt: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginAdmin(@RequestBody Map<String, Object> payload) {
        String email = payload.get("email") != null ? payload.get("email").toString() : null;
        String password = payload.get("password") != null ? payload.get("password").toString() : null;

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
        }

        Optional<Admin> opt = adminRepo.findByEmailIgnoreCase(email.trim());
        if (opt.isEmpty()) {
            System.out.println("Admin Login FAILED: User not found -> " + email);
            return ResponseEntity.status(401).body(Map.of("error", "Invalid admin credentials"));
        }

        Admin a = opt.get();
        if (!a.getPasswordHash().equals(User.hashPassword(password))) {
            System.out.println("Admin Login FAILED: Password mismatch for -> " + email);
            return ResponseEntity.status(401).body(Map.of("error", "Invalid admin credentials"));
        }

        System.out.println("Admin Login SUCCESS: " + email);

        return ResponseEntity.ok(Map.of(
            "message", "Admin login successful",
            "adminId", a.getUserId(),
            "name", a.getName(),
            "role", "admin",
            "adminRole", a.getAdminRole()
        ));
    }

    @GetMapping("/passengers/all")
    public ResponseEntity<?> getAllPassengers() {
        return ResponseEntity.ok(passengerRepo.findAll());
    }

    @PostMapping("/drivers/approve/{id}")
    public ResponseEntity<?> approveDriver(@PathVariable int id) {
        Optional<Driver> opt = driverRepo.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Driver not found"));
        }
        Driver d = opt.get();
        d.setStatus("Offline");
        driverRepo.save(d);
        return ResponseEntity.ok(Map.of("message", "Driver approved successfully"));
    }

    @DeleteMapping("/drivers/delete/{id}")
    public ResponseEntity<?> deleteDriver(@PathVariable int id) {
        if (!driverRepo.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of("error", "Driver not found"));
        }

        
        driverRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Driver removed successfully"));
    }

    @DeleteMapping("/passengers/delete/{id}")
    public ResponseEntity<?> deletePassenger(@PathVariable int id) {
        if (!passengerRepo.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of("error", "Passenger not found"));
        }


        passengerRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Passenger removed successfully"));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        long totalPassengers = passengerRepo.count();
        long totalDrivers = driverRepo.count();
        

        long activeDrivers = driverRepo.countByStatus("Available") + driverRepo.countByStatus("OnTrip") + driverRepo.countByStatus("online");
        
        java.util.List<Booking> allBookings = bookingRepo.findAll();
        
        long totalTrips = allBookings.stream()
            .filter(b -> "Completed".equalsIgnoreCase(b.getStatus()) || "Paid".equalsIgnoreCase(b.getStatus()))
            .count();
            
        double totalRevenue = allBookings.stream()
            .filter(b -> "Completed".equalsIgnoreCase(b.getStatus()) || "Paid".equalsIgnoreCase(b.getStatus()))
            .mapToDouble(b -> b.getFare())
            .sum();

        return ResponseEntity.ok(Map.of(
            "totalPassengers", totalPassengers,
            "totalDrivers", totalDrivers,
            "totalUsers", (totalPassengers + totalDrivers),
            "activeDrivers", activeDrivers,
            "totalTrips", totalTrips,
            "totalRevenue", totalRevenue
        ));
    }
}
