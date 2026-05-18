package com.urbanride.component1;

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
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class UserController {

    @Autowired
    private PassengerRepository passengerRepo;

    @Autowired
    private BookingRepository bookingRepo;

    @PostMapping("/register")
    public ResponseEntity<?> registerPassenger(@RequestBody Map<String, Object> payload) {
        System.out.println("Registration attempt: " + payload);
        String email = payload.get("email") != null ? payload.get("email").toString() : null;
        String password = payload.get("password") != null ? payload.get("password").toString() : null;
        String name = payload.get("name") != null ? payload.get("name").toString() : null;
        String mobile = payload.get("mobile") != null ? payload.get("mobile").toString() : null;

        if (email == null || password == null || name == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name, email and password are required"));
        }

        if (passengerRepo.existsByEmailIgnoreCase(email)) {
            return ResponseEntity.status(409).body(Map.of("error", "Email already registered"));
        }

        Passenger p = new Passenger();
        p.setName(name);
        p.setEmail(email);
        p.setPasswordHash(User.hashPassword(password));
        p.setMobile(mobile != null ? mobile : "");
        p.setRole("passenger");
        p.setHomeAddress(payload.getOrDefault("homeAddress", "").toString());
        p.setWorkAddress(payload.getOrDefault("workAddress", "").toString());
        p.setPaymentMethod(payload.getOrDefault("paymentMethod", "Cash").toString());

        Passenger saved = passengerRepo.save(p);
        

        logUserToFile(saved);
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "Registration successful");
        response.put("userId", saved.getUserId());
        response.put("name", saved.getName());
        response.put("role", "passenger");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginPassenger(@RequestBody Map<String, Object> payload) {
        System.out.println("Passenger login attempt: " + payload.get("email"));
        String email = payload.get("email") != null ? payload.get("email").toString() : null;
        String password = payload.get("password") != null ? payload.get("password").toString() : null;

        if (email == null || password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
        }

        Optional<Passenger> opt = passengerRepo.findByEmailIgnoreCase(email.trim());
        if (opt.isEmpty()) {
            System.out.println("Login FAILED: User not found -> " + email);
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }

        Passenger p = opt.get();
        String hashedInput = User.hashPassword(password);

        // actually compare the hashed password
        if (!p.getPasswordHash().equals(hashedInput)) {
            System.out.println("Login FAILED: Password mismatch for -> " + email);
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }

        System.out.println("Login SUCCESS: " + email);

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "Login successful");
        response.put("userId", p.getUserId());
        response.put("formattedId", String.format("P%03d", p.getUserId() % 100));
        response.put("name", p.getName() != null ? p.getName() : "User");
        response.put("email", p.getEmail());
        response.put("mobile", p.getMobile() != null ? p.getMobile() : "");
        response.put("walletBalance", p.getWalletBalance() != null ? p.getWalletBalance() : 0.0);
        response.put("role", "passenger");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getPassenger(@PathVariable int userId) {
        return passengerRepo.findById(userId)
            .map(p -> {
                java.util.Map<String, Object> response = new java.util.HashMap<>();
                response.put("userId", p.getUserId());
                response.put("formattedId", String.format("P%03d", p.getUserId() % 100));
                response.put("name", p.getName() != null ? p.getName() : "Unknown");
                response.put("email", p.getEmail() != null ? p.getEmail() : "");
                response.put("mobile", p.getMobile() != null ? p.getMobile() : "");
                response.put("walletBalance", p.getWalletBalance() != null ? p.getWalletBalance() : 0.0);
                response.put("passengerType", p.getPassengerType() != null ? p.getPassengerType() : "Regular");
                
                // Count all bookings for this passenger from the real database
                long totalCount = bookingRepo.countByPassengerId(p.getUserId());
                response.put("totalRides", totalCount);
                
                response.put("memberSince", p.getCreatedAt());

                if (p.getCardNumber() != null && p.getCardNumber().length() >= 4) {
                    String full = p.getCardNumber().replaceAll("\\s+", "");
                    response.put("savedCardLast4", full.substring(full.length() - 4));
                    response.put("savedCardExpiry", p.getCardExpiry());
                }

                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> updatePassenger(@PathVariable int userId, @RequestBody Map<String, Object> payload) {
        try {
            return passengerRepo.findById(userId)
                .map(p -> {
                    if (payload.containsKey("name")) p.setName(payload.get("name").toString());
                    if (payload.containsKey("mobile")) p.setMobile(payload.get("mobile").toString());
                    if (payload.containsKey("email")) p.setEmail(payload.get("email").toString());

                    passengerRepo.save(p);
                    return ResponseEntity.ok(Map.of("message", "Profile updated successfully", "name", p.getName()));
                })
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to update user"));
        }
    }

    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<?> deletePassenger(@PathVariable int userId) {
        if (!passengerRepo.existsById(userId)) {
            return ResponseEntity.notFound().build();
        }
        passengerRepo.deleteById(userId);
        return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
    }

    @PutMapping("/{userId}/wallet")
    public ResponseEntity<?> topUpWallet(@PathVariable int userId, @RequestBody Map<String, Object> payload) {
        try {
            if (payload.get("amount") == null) return ResponseEntity.badRequest().body(Map.of("error", "Amount is required"));
            double amount = Double.parseDouble(payload.get("amount").toString());
            return passengerRepo.findById(userId)
                .map(p -> {
                    p.topUpWallet(amount);
                    
                    // Ã°Å¸â€™Â³ SAVE CARD DETAILS IF PROVIDED
                    if (payload.containsKey("cardNumber")) p.setCardNumber(payload.get("cardNumber").toString());
                    if (payload.containsKey("cardExpiry")) p.setCardExpiry(payload.get("cardExpiry").toString());
                    if (payload.containsKey("cardHolderName")) p.setCardHolderName(payload.get("cardHolderName").toString());
                    
                    passengerRepo.save(p);
                    
                    // LOG TO TOPUP FILE
                    logTopUpToFile(p.getUserId(), p.getName(), amount, p.getWalletBalance());
                    
                    return ResponseEntity.ok(Map.of(
                        "message", "Wallet topped up", 
                        "newBalance", p.getWalletBalance(),
                        "cardSaved", p.getCardNumber() != null
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            e.printStackTrace(); // Log the error to console
            return ResponseEntity.status(400).body(Map.of("error", "Invalid request: " + e.getMessage()));
        }
    }

    @PutMapping("/{userId}/wallet/deduct")
    public ResponseEntity<?> deductWallet(@PathVariable int userId, @RequestBody Map<String, Object> payload) {
        try {
            if (payload.get("amount") == null) return ResponseEntity.badRequest().body(Map.of("error", "Amount is required"));
            double amount = Double.parseDouble(payload.get("amount").toString());
            return passengerRepo.findById(userId)
                .map(p -> {
                    if (p.deductWallet(amount)) {
                        passengerRepo.save(p);
                        
                        // OG TO DEDUCTION FILE
                        logDeductionToFile(p.getUserId(), p.getName(), amount, p.getWalletBalance());
                        
                        return ResponseEntity.ok(Map.of("message", "Payment successful", "newBalance", p.getWalletBalance()));
                    } else {
                        return ResponseEntity.status(400).body(Map.of("error", "Insufficient wallet balance"));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", "Invalid request"));
        }
    }

    private void logDeductionToFile(int userId, String name, double amount, double newBalance) {
        try (FileWriter fw = new FileWriter("wallet_deductions.txt", true);
             PrintWriter out = new PrintWriter(fw)) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            out.println(String.format("[%s] Payment: User ID P%03d (%s) | Amount: LKR %.2f | Remaining Balance: LKR %.2f",
                timestamp, userId % 100, name, amount, newBalance));
        } catch (Exception e) {
            System.err.println("Error writing to wallet_deductions.txt: " + e.getMessage());
        }
    }

    private void logUserToFile(Passenger p) {
        try (FileWriter fw = new FileWriter("users.txt", true);
             PrintWriter out = new PrintWriter(fw)) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            out.println(String.format("[%s] New User: ID P%03d | Name: %s | Email: %s | Phone: %s",
                timestamp, p.getUserId() % 100, p.getName(), p.getEmail(), p.getMobile()));
        } catch (Exception e) {
            System.err.println("Error writing to users.txt: " + e.getMessage());
        }
    }

    private void logTopUpToFile(int userId, String name, double amount, double newBalance) {
        try (FileWriter fw = new FileWriter("wallet_topups.txt", true);
             PrintWriter out = new PrintWriter(fw)) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            out.println(String.format("[%s] Top-up: User ID P%03d (%s) | Amount: LKR %.2f | New Balance: LKR %.2f",
                timestamp, userId % 100, name, amount, newBalance));
        } catch (Exception e) {
            System.err.println("Error writing to wallet_topups.txt: " + e.getMessage());
        }
    }
}

