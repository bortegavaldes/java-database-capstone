package com.project.back_end.mvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.project.back_end.services.MainService;

@Controller
public class DashboardController {

    @Autowired
    private final MainService tokenService; // Rename 'Service' for better clarity if needed

    public DashboardController(MainService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * Handles requests to the Admin Dashboard, validating the provided token.
     *
     * @param token The security token provided as a path variable.
     * @return The path to the Thymeleaf template if valid, or a redirect to the login page otherwise.
     */
    // Define the adminDashboard method:
    @GetMapping("/adminDashboard/{token}")
    public String adminDashboard(@PathVariable String token) {
        // Call validateToken, which returns a ResponseEntity<String>
        ResponseEntity<String> validationResponse = tokenService.validateToken(token, "admin");

        // FIX: Check if the returned HTTP status is OK (200) for a valid token.
        if (validationResponse.getStatusCode() == HttpStatus.OK) {
            // Token is valid → return the admin dashboard view.
            return "admin/adminDashboard";
        }

        // Token is invalid/expired → redirect to the root URL (login/home page).
        return "redirect:/";
    }

    /**
     * Handles requests to the Doctor Dashboard, validating the provided token.
     *
     * @param token The security token provided as a path variable.
     * @return The path to the Thymeleaf template if valid, or a redirect to the login page otherwise.
     */
    // Define the doctorDashboard method:
    @GetMapping("/doctorDashboard/{token}")
    public String doctorDashboard(@PathVariable String token) {
        // Call validateToken, which returns a ResponseEntity<String>
        ResponseEntity<String> validationResponse = tokenService.validateToken(token, "doctor");

        // FIX: Check if the returned HTTP status is OK (200) for a valid token.
        if (validationResponse.getStatusCode() == HttpStatus.OK) {
            // Token is valid → return the doctor dashboard view.
            return "doctor/doctorDashboard";
        }

        // Token is invalid/expired → redirect to the root URL (login/home page).
        return "redirect:/";
    }

}
