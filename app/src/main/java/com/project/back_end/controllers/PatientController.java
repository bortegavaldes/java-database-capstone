package com.project.back_end.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Patient;
import com.project.back_end.services.PatientService;
import com.project.back_end.services.MainService;

@RestController
@RequestMapping("/patient")
public class PatientController {
    PatientService patientService;
    MainService service;
    
    public PatientController(PatientService patientService, MainService service) {
        this.patientService = patientService;
        this.service = service;
    }

    /**
     * Helper method to validate the token (assumed to exist in Service class).
     * @param token The security token.
     * @param requiredRole The role needed for authorization (e.g., "patient").
     * @return ResponseEntity with an error map if invalid, or null if the token is valid (HTTP 200 OK).
     */
    private ResponseEntity<Map<String, Object>> checkTokenValidity(String token, String requiredRole) {
        // Uses the general Service method for token validation
        ResponseEntity<String> validationResponse = service.validateToken(token, requiredRole);
        
        if (validationResponse.getStatusCode() != HttpStatus.OK) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("status", "error");
            errorBody.put("message", validationResponse.getBody()); 
            
            // Return the original error status code (e.g., 401 UNAUTHORIZED)
            return new ResponseEntity<>(errorBody, validationResponse.getStatusCode());
        }
        return null; // Token is valid
    }

    // 3. Define the `getPatient` Method:
    // Service method: Patient getPatientDetails(String token)
    @GetMapping("/{token}")
    public ResponseEntity<Map<String, Object>> getPatient(@PathVariable String token) {

        // Validate token for "patient" role
        ResponseEntity<Map<String, Object>> authError = checkTokenValidity(token, "patient");
        if (authError != null) {
            return authError;
        }

        // patientService.getPatientDetails is expected to extract ID/email from token
        Patient patient = patientService.getPatientDetails(token);

        if (patient == null) {
            return new ResponseEntity<>(
                Map.of("status", "error", "message", "Patient details not found after token validation."), 
                HttpStatus.NOT_FOUND
            );
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("patient", patient);
        return ResponseEntity.ok(response);
    }

    // 4. Define the `createPatient` Method:
    // PatientService.createPatient returns 1 (success) or 0 (error).
    // Service.validatePatient returns true (valid for registration) or false (already exists).
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> createPatient(@RequestBody Patient patient) {
        
        // 1. Check if patient already exists (using email/phone)
        if (!service.validatePatient(patient.getEmail(), patient.getPhone())) {
            return new ResponseEntity<>(
                Map.of("status", "error", "message", "Patient with this email or phone number already exists."),
                HttpStatus.CONFLICT // 409 Conflict
            );
        }

        // 2. Create patient
        int serviceResult = patientService.createPatient(patient);

        if (serviceResult == 1) {
            return new ResponseEntity<>(
                Map.of("status", "success", "message", "Patient registered successfully."), 
                HttpStatus.CREATED // 201 Created
            );
        } else {
            return new ResponseEntity<>(
                Map.of("status", "error", "message", "Failed to register patient due to an internal error."),
                HttpStatus.INTERNAL_SERVER_ERROR // 500 Internal Server Error
            );
        }
    }

    // 5. Define the `login` Method:
    // Service.validatePatientLogin returns token (String) on success or an error message (String) on failure.
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Login loginDto) {
        
        ResponseEntity<String> serviceResponse = service.validatePatientLogin(loginDto.getEmail(), loginDto.getPassword());

        if (serviceResponse.getStatusCode() == HttpStatus.OK) {
            String token = serviceResponse.getBody();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Patient login successful.");
            response.put("token", token);
            return ResponseEntity.ok(response);
        } else {
            // Propagate the error status and message from the service (e.g., 401 UNAUTHORIZED)
            return new ResponseEntity<>(
                Map.of("status", "error", "message", serviceResponse.getBody() != null ? serviceResponse.getBody() : "Login failed."),
                serviceResponse.getStatusCode()
            );
        }
    }

    // 6. Define the `getPatientAppointment` Method:
    // PatientService.getPatientAppointment returns List<AppointmentDTO> or empty list.
    // Patient ID is usually extracted from the token, but the request specifies patient ID as path variable.
    @GetMapping("/{patientId}/appointments/{role}/{token}")
    public ResponseEntity<?> getPatientAppointment(
            @PathVariable Long patientId,
            @PathVariable String role,
            @PathVariable String token) {

        // Validate the token (e.g., must be "patient" or potentially "doctor" accessing patient records)
        ResponseEntity<Map<String, Object>> authError = checkTokenValidity(token, role);
        if (authError != null) {
            return authError;
        }

        // The service method expects the Patient ID
        List<?> appointments = patientService.getPatientAppointment(patientId);

        if (appointments.isEmpty()) {
            return new ResponseEntity<>(
                Map.of("status", "info", "message", "No appointments found for patient ID " + patientId + "."),
                HttpStatus.NOT_FOUND
            );
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("appointments", appointments);
        return ResponseEntity.ok(response);
    }

    // 7. Define the `filterPatientAppointment` Method:
    // Service.filterPatient returns List<AppointmentDTO> or empty list.
    // patientId is assumed to be extracted inside the Service.filterPatient method via token.
    @GetMapping("/appointments/filter")
    public ResponseEntity<?> filterPatientAppointment(
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) String doctorName,
            @RequestParam String token) {

        // Validate token for "patient" role
        ResponseEntity<Map<String, Object>> authError = checkTokenValidity(token, "patient");
        if (authError != null) {
            return authError;
        }

        // Delegates filtering to the shared service, passing the token for patient identification.
        // The Service class will handle the logic using PatientService internally.
        List<?> filteredAppointments = service.filterPatient(token, condition, doctorName); 

        if (filteredAppointments.isEmpty()) {
            return new ResponseEntity<>(
                Map.of("status", "info", "message", "No appointments found matching the filter criteria."),
                HttpStatus.NOT_FOUND
            );
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("appointments", filteredAppointments);
        return ResponseEntity.ok(response);
    }

}

// 1. Set Up the Controller Class:
//    - Annotate the class with `@RestController` to define it as a REST API controller for patient-related operations.
//    - Use `@RequestMapping("/patient")` to prefix all endpoints with `/patient`, grouping all patient functionalities under a common route.


// 2. Autowire Dependencies:
//    - Inject `PatientService` to handle patient-specific logic such as creation, retrieval, and appointments.
//    - Inject the shared `Service` class for tasks like token validation and login authentication.


// 3. Define the `getPatient` Method:
//    - Handles HTTP GET requests to retrieve patient details using a token.
//    - Validates the token for the `"patient"` role using the shared service.
//    - If the token is valid, returns patient information; otherwise, returns an appropriate error message.


// 4. Define the `createPatient` Method:
//    - Handles HTTP POST requests for patient registration.
//    - Accepts a validated `Patient` object in the request body.
//    - First checks if the patient already exists using the shared service.
//    - If validation passes, attempts to create the patient and returns success or error messages based on the outcome.


// 5. Define the `login` Method:
//    - Handles HTTP POST requests for patient login.
//    - Accepts a `Login` DTO containing email/username and password.
//    - Delegates authentication to the `validatePatientLogin` method in the shared service.
//    - Returns a response with a token or an error message depending on login success.


// 6. Define the `getPatientAppointment` Method:
//    - Handles HTTP GET requests to fetch appointment details for a specific patient.
//    - Requires the patient ID, token, and user role as path variables.
//    - Validates the token using the shared service.
//    - If valid, retrieves the patient's appointment data from `PatientService`; otherwise, returns a validation error.


// 7. Define the `filterPatientAppointment` Method:
//    - Handles HTTP GET requests to filter a patient's appointments based on specific conditions.
//    - Accepts filtering parameters: `condition`, `name`, and a token.
//    - Token must be valid for a `"patient"` role.
//    - If valid, delegates filtering logic to the shared service and returns the filtered result.
