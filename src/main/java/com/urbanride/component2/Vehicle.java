package com.urbanride.component2;

public abstract class Vehicle {
    private int vehicleId;
    private String plateNo;
    private String model;
    private int year;
    private String type; 
    private int capacity;
    private String maintenanceDate;

    public Vehicle(int vehicleId, String plateNo, String model, int year, String type, int capacity) {
        this.vehicleId = vehicleId;
        this.plateNo = plateNo;
        this.model = model;
        this.year = year;
        this.type = type;
        this.capacity = capacity;
    }

    public int getVehicleId() { return vehicleId; }
    public String getPlateNo() { return plateNo; }
    public String getModel() { return model; }
    public int getYear() { return year; }
    public String getType() { return type; }
    public int getCapacity() { return capacity; }
    public String getMaintenanceDate() { return maintenanceDate; }
    public void setMaintenanceDate(String d) { this.maintenanceDate = d; }

    public abstract double calculateFuelCost(double distanceKm);
    public abstract String getFuelType();

    public void displayInfo() {
        System.out.println("Vehicle: " + model + " (" + plateNo + ") | Type: " + type + " | Fuel: " + getFuelType());
    }
}

