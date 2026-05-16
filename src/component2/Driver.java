package com.urbanride.component2;

import jakarta.persistence.*;
import com.urbanride.component1.User;

@Entity
@Table(name = "drivers")
public class Driver extends User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "driver_seq")
    @SequenceGenerator(name = "driver_seq", sequenceName = "driver_sequence", allocationSize = 1)
    private int userId;

    @Override
    public int getUserId() { return userId; }
    @Override
    public void setUserId(int id) { this.userId = id; }

    @Column(unique = true)
    // driverge ino
    private String licenseNo;
    private String status = "Offline";
    private String currentLocation;
    private Double rating = 5.0; // Start with 5 stars
    private Integer totalTrips = 0;
    private Double totalEarnings = 0.0;
    private Integer ratingCount = 0;
    private Double totalRatingPoints = 0.0;

    // Vehicle eke info
    private String vehiclePlate;
    private String vehicleModel;
    private String vehicleType;
    private String fuelType;

    public Driver() {}

    public Driver(int userId, String name, String email, String password, String mobile, String licenseNo) {
        super(userId, name, email, password, mobile, "driver");
        this.licenseNo = licenseNo;
    }

    public String getLicenseNo() { return licenseNo; }
    public void setLicenseNo(String licenseNo) { this.licenseNo = licenseNo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(String l) { this.currentLocation = l; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public Integer getTotalTrips() { return totalTrips; }
    public void setTotalTrips(Integer totalTrips) { this.totalTrips = totalTrips; }
    public String getVehiclePlate() { return vehiclePlate; }
    public void setVehiclePlate(String vehiclePlate) { this.vehiclePlate = vehiclePlate; }
    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }
    public Double getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(Double totalEarnings) { this.totalEarnings = totalEarnings; }
    public Integer getRatingCount() { return ratingCount; }
    public void setRatingCount(Integer count) { this.ratingCount = count; }
    public Double getTotalRatingPoints() { return totalRatingPoints; }
    public void setTotalRatingPoints(Double points) { this.totalRatingPoints = points; }

    public void goOnline() { this.status = "Available"; }
    public void goOffline() { this.status = "Offline"; }
    public void acceptTrip() { this.status = "OnTrip"; totalTrips++; }
    public void completeTrip() { this.status = "Available"; }
    
    public void addRating(double newStars) {
        if (this.ratingCount == null) this.ratingCount = 0;
        if (this.totalRatingPoints == null) this.totalRatingPoints = 0.0;
        
        this.ratingCount++;
        this.totalRatingPoints += newStars;
        this.rating = Math.round((this.totalRatingPoints / this.ratingCount) * 10.0) / 10.0;
    }

    @Override
    public boolean login(String email, String password) {
        return getEmail().equals(email) && getPasswordHash().equals(hashPassword(password));
    }

    @Override
    public void displayProfile() {
        System.out.println("Driver: " + getName() + " | Status: " + status + " | Rating: " + rating);
    }
}

