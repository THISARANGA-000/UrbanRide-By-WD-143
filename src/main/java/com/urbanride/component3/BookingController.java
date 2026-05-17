package com.urbanride.component3;

import com.urbanride.component2.Driver;
import com.urbanride.component2.DriverRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {

    @Autowired
    private BookingRepository bookingRepo;

    @Autowired
    private com.urbanride.component1.PassengerRepository passengerRepo;

    @Autowired
    private DriverRepository driverRepo;

    @PostMapping("/create")
    public ResponseEntity<?> createBooking(@RequestBody Map<String, Object> payload) {
        try {
            int passengerId = Integer.parseInt(payload.get("passengerId").toString());
            String pickup   = payload.get("pickupLocation") != null ? payload.get("pickupLocation").toString() : null;
            String dropoff  = payload.get("dropoffLocation") != null ? payload.get("dropoffLocation").toString() : null;
            String rideType = payload.get("rideType") != null ? payload.get("rideType").toString() : null;

            if (pickup == null || dropoff == null || rideType == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Data missing: pickup, dropoff, or rideType"));
            }

            Booking b = new Booking(passengerId, pickup, dropoff, rideType);

            if (payload.containsKey("fare")) {
                b.setFare(Double.parseDouble(payload.get("fare").toString()));
            } else {
                // Fallback for older versions or missing data
                double baseFare = 300.0;
                if (rideType != null) {
                    switch (rideType) {
                        case "Bike": baseFare = 200 + (Math.random() * 200); break;
                        case "Three-Wheel": baseFare = 350 + (Math.random() * 300); break;
                        case "Car": baseFare = 500 + (Math.random() * 800); break;
                        default: baseFare = 300 + (Math.random() * 400);
                    }
                }
                b.setFare(Math.round(baseFare * 100.0) / 100.0);
            }
            b.setStatus("Pending");

            Booking saved = bookingRepo.save(b);
            
            saveToFile(saved);
            
            return ResponseEntity.ok(Map.of(
                "message",   "Booking created successfully",
                "bookingId", saved.getBookingId(),
                "fare",      saved.getFare(),
                "status",    saved.getStatus()
            ));
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR IN CREATE BOOKING: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Internal Server Error: " + e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllBookings() {
        List<Booking> bookings = bookingRepo.findAllByOrderByCreatedAtDesc();
        java.util.List<java.util.Map<String, Object>> response = new java.util.ArrayList<>();
        for (Booking b : bookings) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("bookingId", b.getBookingId());
            map.put("passengerId", b.getPassengerId());
            map.put("pickupLocation", b.getPickupLocation());
            map.put("dropoffLocation", b.getDropoffLocation());
            map.put("status", b.getStatus());
            map.put("fare", b.getFare());
            map.put("rideType", b.getRideType());
            map.put("createdAt", b.getCreatedAt());
            map.put("driverId", b.getDriverId() > 0 ? b.getDriverId() : null);
            response.add(map);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingBookings() {
        LocalDateTime fiveMinsAgo = LocalDateTime.now().minusMinutes(5);
        List<Booking> pending = bookingRepo.findByStatusOrderByCreatedAtDesc("Pending");

        java.util.List<Booking> active = new java.util.ArrayList<>();
        for (Booking b : pending) {
            if ("Pending".equalsIgnoreCase(b.getStatus()) && b.getCreatedAt().isBefore(fiveMinsAgo)) {
                b.setStatus("Expired");
                bookingRepo.save(b);
            } else if ("Pending".equalsIgnoreCase(b.getStatus())) {
                active.add(b);
            }
        }
        
        java.util.List<java.util.Map<String, Object>> response = new java.util.ArrayList<>();
        for (Booking b : active) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("bookingId", b.getBookingId()); 
            map.put("displayId", b.getBookingId() % 100);
            map.put("passengerId", b.getPassengerId());
            map.put("pickupLocation", b.getPickupLocation());
            map.put("dropoffLocation", b.getDropoffLocation());
            map.put("status", b.getStatus());
            map.put("fare", b.getFare());
            map.put("rideType", b.getRideType());
            map.put("createdAt", b.getCreatedAt());
            response.add(map);
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/passenger/{passengerId}")
    public ResponseEntity<?> getPassengerBookings(@PathVariable int passengerId) {
        if (passengerId <= 0) return ResponseEntity.ok(java.util.Collections.emptyList());
        List<Booking> bookings = bookingRepo.findByPassengerIdOrderByCreatedAtDesc(passengerId);
        
        java.util.List<java.util.Map<String, Object>> response = new java.util.ArrayList<>();
        for (Booking b : bookings) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("bookingId", b.getBookingId());
            map.put("pickupLocation", b.getPickupLocation());
            map.put("dropoffLocation", b.getDropoffLocation());
            map.put("status", b.getStatus());
            map.put("fare", b.getFare());
            map.put("rideType", b.getRideType());
            map.put("createdAt", b.getCreatedAt());
            map.put("driverId", b.getDriverId());
            
            String dName = "Searching...";
            if (b.getDriverId() > 0) {
                Optional<Driver> d = driverRepo.findById(b.getDriverId());
                if (d.isPresent()) dName = d.get().getName();
                else dName = "Driver N/A";
            } else if ("Cancelled".equalsIgnoreCase(b.getStatus())) {
                dName = "N/A";
            } else if ("Expired".equalsIgnoreCase(b.getStatus())) {
                dName = "No Driver Found";
            }
            map.put("driverName", dName);
            map.put("paymentMethod", b.getPaymentMethod() != null ? b.getPaymentMethod() : "N/A");
            response.add(map);
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<?> getDriverBookings(@PathVariable int driverId) {
        if (driverId <= 0) return ResponseEntity.ok(java.util.Collections.emptyList());
        return ResponseEntity.ok(bookingRepo.findByDriverIdOrderByCreatedAtDesc(driverId));
    }

    @PutMapping("/{bookingId}/status")
    public synchronized ResponseEntity<?> updateStatus(@PathVariable int bookingId,
                                          @RequestBody Map<String, Object> payload) {
        Optional<Booking> bookingOpt = bookingRepo.findById(bookingId);
        
        if (bookingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Booking b = bookingOpt.get();
        String newStatus = payload.get("status") != null ? payload.get("status").toString() : null;

        if ("Accepted".equalsIgnoreCase(newStatus)) {
            if (!"Pending".equalsIgnoreCase(b.getStatus())) {
                return ResponseEntity.status(409).body(Map.of("error", "Ride has already been accepted by another driver or is no longer available."));
            }
        }

        if (payload.containsKey("driverId")) {
            int driverId = Integer.parseInt(payload.get("driverId").toString());
            Optional<Driver> drOpt = driverRepo.findById(driverId);
            if (drOpt.isPresent()) {
                Driver d = drOpt.get();
                b.setDriverId(driverId);
                
                d.setStatus("OnTrip");
                driverRepo.save(d);
            }
        }

        if (newStatus != null) {
            b.setStatus(newStatus);
            if ("Cancelled".equalsIgnoreCase(newStatus)) b.cancelBooking();
            else if ("Completed".equalsIgnoreCase(newStatus)) b.completeBooking();
        }

        if (payload.containsKey("paymentMethod")) {
            b.setPaymentMethod(payload.get("paymentMethod").toString());
        }

        if ("Paid".equalsIgnoreCase(newStatus)) {
            String method = payload.containsKey("paymentMethod") ? payload.get("paymentMethod").toString() : "";
            if ("Wallet".equalsIgnoreCase(method)) {
                Optional<com.urbanride.component1.Passenger> pOpt = passengerRepo.findById(b.getPassengerId());
                if (pOpt.isPresent()) {
                    com.urbanride.component1.Passenger p = pOpt.get();
                    if (!p.deductWallet(b.getFare())) {
                        return ResponseEntity.status(400).body(Map.of("error", "Insufficient wallet balance"));
                    }
                    passengerRepo.save(p);
                }
            }

            if (b.getDriverId() > 0) {
                driverRepo.findById(b.getDriverId()).ifPresent(d -> {
                    double currentEarnings = (d.getTotalEarnings() != null) ? d.getTotalEarnings() : 0.0;
                    int currentTrips = (d.getTotalTrips() != null) ? d.getTotalTrips() : 0;
                    
                    d.setTotalEarnings(currentEarnings + b.getFare());
                    d.setTotalTrips(currentTrips + 1);
                    d.setStatus("Available");
                    driverRepo.save(d);
                });
                savePaymentData(b);
            }
        }
        
        if ("Cancelled".equalsIgnoreCase(newStatus) && b.getDriverId() > 0) {
            driverRepo.findById(b.getDriverId()).ifPresent(d -> {
                d.setStatus("Available");
                driverRepo.save(d);
            });
        }

        bookingRepo.save(b);
        return ResponseEntity.ok(Map.of("message", "Booking updated", "status", b.getStatus()));
    }

    private void saveToFile(Booking b) {
        try (FileWriter fw = new FileWriter("bookings.txt", true);
             PrintWriter out = new PrintWriter(fw)) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            out.println(String.format("[%s] ID: B%03d | Passenger: P%03d | Route: %s -> %s | Fare: LKR %.2f | Status: %s",
                timestamp, b.getBookingId(), b.getPassengerId(), b.getPickupLocation(), b.getDropoffLocation(), b.getFare(), b.getStatus()));
        } catch (Exception e) {
            System.err.println("Error writing to bookings.txt: " + e.getMessage());
        }
    }

    private void savePaymentData(Booking b) {
        String filename = "payment_data.txt";
        try (FileWriter fw = new FileWriter(filename, true);
             PrintWriter out = new PrintWriter(fw)) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            out.println(String.format("[%s] Driver ID: D%03d | Passenger ID: P%03d | Booking ID: B%03d | Fare: LKR %.2f | Payment Method: %s | Status: %s",
                timestamp, b.getDriverId(), b.getPassengerId(), b.getBookingId(), b.getFare(), b.getPaymentMethod(), b.getStatus()));
        } catch (Exception e) {
            System.err.println("Error writing to " + filename + ": " + e.getMessage());
        }
    }
}
