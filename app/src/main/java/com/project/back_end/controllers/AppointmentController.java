package com.project.back_end.controllers;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.MainService;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

// 2. Autowire Dependencies:
    private final AppointmentService appointmentService;
    private final MainService tokenService; // General service for shared utilities like token validation

    public AppointmentController(AppointmentService appointmentService, MainService tokenService) {
        this.appointmentService = appointmentService;
        this.tokenService = tokenService;
    }

    /**
     * Helper method to validate the token and return an error response map if invalid.
     * @param token The security token.
     * @param requiredRole The role needed for authorization (e.g., "doctor", "patient").
     * @return ResponseEntity with an error map if invalid, or null if the token is valid (HTTP 200 OK).
     */
    private ResponseEntity<Map<String, Object>> checkTokenValidity(String token, String requiredRole) {
        ResponseEntity<String> validationResponse = tokenService.validateToken(token, requiredRole);
        
        if (validationResponse.getStatusCode() != HttpStatus.OK) {
            // Token is invalid/expired/validation failed.
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("status", "error");
            // Use the error message returned in the body of the ResponseEntity from the service
            errorBody.put("message", validationResponse.getBody()); 
            
            // Return the original error status code (e.g., 401 UNAUTHORIZED)
            return new ResponseEntity<>(errorBody, validationResponse.getStatusCode());
        }
        return null; // Token is valid
    }

    // 3. Define the `getAppointments` Method:
    // Mapped to include doctor ID and date, required by the service method signature.
    @GetMapping("/doctor/{doctorId}/{date}/{patientName}/{token}")
    public ResponseEntity<?> getAppointments(
            @PathVariable Long doctorId, // Doctor ID now explicitly taken from path
            @PathVariable LocalDate date, // Requires conversion from String to LocalDate
            @PathVariable(required = false) String patientName,
            @PathVariable String token) {

        // Validate the token for the "doctor" role
        ResponseEntity<Map<String, Object>> authError = checkTokenValidity(token, "doctor");
        if (authError != null) {
            return authError;
        }
        
        // Ensure the doctor ID in the path matches the ID in the token (best practice check)
        // Note: For this exercise, we skip token ID extraction and trust the path variable, 
        // but in production, tokenService.extractIdFromToken(token) should be used.

        // Token is valid, proceed with business logic (service returns List<Appointment>)
        List<Appointment> appointments = appointmentService.getAppointments(doctorId, date, patientName);
        List<AppointmentDTO> appsDto = appointments.stream()
        .map(AppointmentDTO::new) // 1. Convert each Appointment to a new AppointmentDTO object
        .collect(Collectors.toList()); // 2. Collect the results back into a new List
        if (appointments.isEmpty()) {
            return new ResponseEntity<>("No appointments found.", HttpStatus.NOT_FOUND);
        }
        
        // Return the list of appointments with HTTP 200 OK
        return ResponseEntity.ok(appsDto);
    }

    // 4. Define the `bookAppointment` Method:
    @PostMapping("/{token}")
    public ResponseEntity<Map<String, Object>> bookAppointment(
            @RequestBody Appointment appointment,
            @PathVariable String token) {

        // Validate the token for the "patient" role
        ResponseEntity<Map<String, Object>> authError = checkTokenValidity(token, "patient");
        if (authError != null) {
            return authError;
        }

        // Token is valid, proceed with business logic (service returns int: 1 for success, 0 for failure)
        int successCode = appointmentService.bookAppointment(appointment);

        if (successCode == 1) {
            Map<String, Object> successBody = new HashMap<>();
            successBody.put("status", "success");
            successBody.put("message", "Appointment booked successfully.");
            // Note: The service doesn't return the saved Appointment object, so we omit it here.
            return new ResponseEntity<>(successBody, HttpStatus.CREATED); // 201 Created
        } else {
            // Assuming failure is due to data or availability issue (e.g., Doctor ID invalid, time conflict)
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("status", "error");
            errorBody.put("message", "Booking failed. Check doctor ID or time availability."); 
            return new ResponseEntity<>(errorBody, HttpStatus.BAD_REQUEST); // 400 Bad Request
        }
    }

    // 5. Define the `updateAppointment` Method:
    @PutMapping("/{id}/{token}") // Requires appointment ID in the path
    public ResponseEntity<Map<String, Object>> updateAppointment(
            @PathVariable Long id,
            @RequestBody Appointment updatedAppointment,
            @PathVariable String token) {

        // Validate the token for the "patient" role
        ResponseEntity<Map<String, Object>> authError = checkTokenValidity(token, "patient");
        if (authError != null) {
            return authError;
        }

        // Assume tokenService can extract the patientId from the token
        Long patientId;
        try {
            // This method needs to be implemented in your general Service class
            patientId = tokenService.extractIdFromToken(token); 
        } catch (Exception e) {
            return new ResponseEntity<>(Map.of("status", "error", "message", "Invalid token structure."), HttpStatus.UNAUTHORIZED);
        }

        // Token is valid, proceed with business logic (service returns status message String)
        String serviceMessage = appointmentService.updateAppointment(id, updatedAppointment, patientId);
        
        Map<String, Object> responseBody = new HashMap<>();

        if (serviceMessage.contains("successfully")) {
            responseBody.put("status", "success");
            responseBody.put("message", serviceMessage);
            // In a real app, you might fetch and return the final updated Appointment object here
            return new ResponseEntity<>(responseBody, HttpStatus.OK);
        } else if (serviceMessage.contains("Unauthorized")) {
            responseBody.put("status", "error");
            responseBody.put("message", serviceMessage);
            return new ResponseEntity<>(responseBody, HttpStatus.FORBIDDEN); // 403 Forbidden
        } else if (serviceMessage.contains("not found")) {
            responseBody.put("status", "error");
            responseBody.put("message", serviceMessage);
            return new ResponseEntity<>(responseBody, HttpStatus.NOT_FOUND); // 404 Not Found
        } else {
            // Default error for availability or other business logic failure
            responseBody.put("status", "error");
            responseBody.put("message", serviceMessage);
            return new ResponseEntity<>(responseBody, HttpStatus.BAD_REQUEST); // 400 Bad Request
        }
    }

    // 6. Define the `cancelAppointment` Method:
    @DeleteMapping("/{id}/{token}")
    public ResponseEntity<Map<String, Object>> cancelAppointment(
            @PathVariable Long id, // Appointment ID
            @PathVariable String token) {

        // Validate the token for the "patient" role
        ResponseEntity<Map<String, Object>> authError = checkTokenValidity(token, "patient");
        if (authError != null) {
            return authError;
        }

        // Assume tokenService can extract the patientId from the token
        Long patientId;
        try {
            // This method needs to be implemented in your general Service class
            patientId = tokenService.extractIdFromToken(token);
        } catch (Exception e) {
            return new ResponseEntity<>(Map.of("status", "error", "message", "Invalid token structure."), HttpStatus.UNAUTHORIZED);
        }

        // Token is valid, proceed with business logic (service returns status message String)
        String serviceMessage = appointmentService.cancelAppointment(id, patientId);
        
        Map<String, Object> responseBody = new HashMap<>();

        if (serviceMessage.contains("successfully")) {
            responseBody.put("status", "success");
            responseBody.put("message", serviceMessage);
            return new ResponseEntity<>(responseBody, HttpStatus.OK);
        } else if (serviceMessage.contains("Unauthorized")) {
            responseBody.put("status", "error");
            responseBody.put("message", serviceMessage);
            return new ResponseEntity<>(responseBody, HttpStatus.FORBIDDEN); // 403 Forbidden
        } else if (serviceMessage.contains("not found")) {
            responseBody.put("status", "error");
            responseBody.put("message", serviceMessage);
            return new ResponseEntity<>(responseBody, HttpStatus.NOT_FOUND); // 404 Not Found
        } else {
            responseBody.put("status", "error");
            responseBody.put("message", serviceMessage);
            return new ResponseEntity<>(responseBody, HttpStatus.INTERNAL_SERVER_ERROR); // Default for unexpected failure
        }
    }
}

// 1. Set Up the Controller Class:
//    - Annotate the class with `@RestController` to define it as a REST API controller.
//    - Use `@RequestMapping("/appointments")` to set a base path for all appointment-related endpoints.
//    - This centralizes all routes that deal with booking, updating, retrieving, and canceling appointments.


// 2. Autowire Dependencies:
//    - Inject `AppointmentService` for handling the business logic specific to appointments.
//    - Inject the general `Service` class, which provides shared functionality like token validation and appointment checks.


// 3. Define the `getAppointments` Method:
//    - Handles HTTP GET requests to fetch appointments based on date and patient name.
//    - Takes the appointment date, patient name, and token as path variables.
//    - First validates the token for role `"doctor"` using the `Service`.
//    - If the token is valid, returns appointments for the given patient on the specified date.
//    - If the token is invalid or expired, responds with the appropriate message and status code.


// 4. Define the `bookAppointment` Method:
//    - Handles HTTP POST requests to create a new appointment.
//    - Accepts a validated `Appointment` object in the request body and a token as a path variable.
//    - Validates the token for the `"patient"` role.
//    - Uses service logic to validate the appointment data (e.g., check for doctor availability and time conflicts).
//    - Returns success if booked, or appropriate error messages if the doctor ID is invalid or the slot is already taken.


// 5. Define the `updateAppointment` Method:
//    - Handles HTTP PUT requests to modify an existing appointment.
//    - Accepts a validated `Appointment` object and a token as input.
//    - Validates the token for `"patient"` role.
//    - Delegates the update logic to the `AppointmentService`.
//    - Returns an appropriate success or failure response based on the update result.


// 6. Define the `cancelAppointment` Method:
//    - Handles HTTP DELETE requests to cancel a specific appointment.
//    - Accepts the appointment ID and a token as path variables.
//    - Validates the token for `"patient"` role to ensure the user is authorized to cancel the appointment.
//    - Calls `AppointmentService` to handle the cancellation process and returns the result.
