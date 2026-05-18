package com.urbanride.component6;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FeedbackManager {
    // This represents the rating and feedback logic from the running project (DriverController)
    public static void logFeedbackToFile(int driverId, double stars, String comment) {
        try (FileWriter fw = new FileWriter("feedback.txt", true);
             PrintWriter out = new PrintWriter(fw)) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            out.println(String.format("[%s] Driver ID: D%03d | Rating: %.1f stars | Comment: %s",
                timestamp, driverId, stars, comment));
        } catch (Exception e) {
            System.err.println("Error writing to feedback.txt: " + e.getMessage());
        }
    }
}

