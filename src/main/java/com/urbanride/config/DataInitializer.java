package com.urbanride.config;

import com.urbanride.component3.Booking;
import com.urbanride.component2.Driver;
import com.urbanride.component1.Passenger;
import com.urbanride.component1.User;
import com.urbanride.component3.BookingRepository;
import com.urbanride.component2.DriverRepository;
import com.urbanride.component1.PassengerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedDatabase(
            PassengerRepository passengerRepo,
            DriverRepository driverRepo,
            BookingRepository bookingRepo) {

        return args -> {

            // â”€â”€ SEED TEST USER (Passenger) â”€â”€
            if (!passengerRepo.existsByEmail("testuser@urbanride.com")) {
                Passenger p = new Passenger();
                p.setName("Test User");
                p.setEmail("testuser@urbanride.com");
                p.setPasswordHash(User.hashPassword("Test@1234"));
                p.setMobile("+94 77 999 0001");
                p.setRole("passenger");
                p.setWalletBalance(2500.00);
                p.setPassengerType("Premium");
                p.setPaymentMethod("Wallet");
                passengerRepo.save(p);
                System.out.println("âœ…  Test User seeded â†’ testuser@urbanride.com / Test@1234");
            }

            // â”€â”€ SEED TEST DRIVER â”€â”€
            if (!driverRepo.existsByEmail("testdriver@urbanride.com")) {
                Driver d = new Driver();
                d.setName("Test Driver");
                d.setEmail("testdriver@urbanride.com");
                d.setPasswordHash(User.hashPassword("Test@1234"));
                d.setMobile("+94 77 999 0002");
                d.setRole("driver");
                d.setLicenseNo("DL-2024-TDR001");
                d.setVehiclePlate("TDR-0001");
                d.setVehicleModel("Toyota Prius");
                d.setVehicleType("Car");
                d.setFuelType("Hybrid");
                d.setRating(4.9);
                d.setTotalTrips(1247);
                d.setStatus("Available");
                driverRepo.save(d);
                System.out.println("âœ…  Test Driver seeded â†’ testdriver@urbanride.com / Test@1234");
            }

            // â”€â”€ SEED SAMPLE BOOKINGS (only if empty) â”€â”€
            if (bookingRepo.count() == 0) {
                Passenger p = passengerRepo.findByEmail("testuser@urbanride.com").orElse(null);
                Driver d = driverRepo.findByEmail("testdriver@urbanride.com").orElse(null);
                if (p != null && d != null) {
                    Booking b1 = new Booking(p.getUserId(), "Colombo 03", "Dehiwala", "Car");
                    b1.setDriverId(d.getUserId());
                    b1.setStatus("Completed");
                    b1.setFare(1250.00);
                    b1.setPaymentMethod("Card");
                    bookingRepo.save(b1);

                    Booking b2 = new Booking(p.getUserId(), "Mount Lavinia", "Fort", "Car");
                    b2.setDriverId(d.getUserId());
                    b2.setStatus("Cancelled");
                    b2.setFare(0);
                    bookingRepo.save(b2);

                    Booking b3 = new Booking(p.getUserId(), "Nugegoda", "Colombo 01", "Bike");
                    b3.setDriverId(d.getUserId());
                    b3.setStatus("Completed");
                    b3.setFare(750.00);
                    b3.setPaymentMethod("Wallet");
                    bookingRepo.save(b3);

                    System.out.println("âœ…  Sample bookings seeded (3 records)");
                }
            }

            System.out.println("ðŸš€  UrbanRide DB ready  |  H2 Console â†’ http://localhost:8080/h2-console");
        };
    }
}
