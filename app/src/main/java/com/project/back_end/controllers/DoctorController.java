package com.project.back_end.controllers;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.back_end.DTO.DoctorDTO;
import com.project.back_end.DTO.Login;
import com.project.back_end.models.Doctor;
import com.project.back_end.services.DoctorService;
import com.project.back_end.services.MainService;

@RestController
@RequestMapping("${api.path}doctor")
public class DoctorController {
    
    DoctorService doctorService;
    MainService service;

    public DoctorController(DoctorService doctorService, MainService service) {
        this.doctorService = doctorService;
        this.service = service;
    }

    /**
     * Helper method to validate the token and return an error response map if invalid.
     * @param token The security token.
     * @param requiredRole The role needed for authorization (e.g., "doctor", "admin").
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

    // 3. Define the `getDoctorAvailability` Method:
    // Service method: List<String> getDoctorAvailability(Long doctorId, LocalDate date)
    @GetMapping("/availability/{userType}/{doctorId}/{date}/{token}")
    public ResponseEntity<?> getDoctorAvailability(
            @PathVariable String userType,
            @PathVariable Long doctorId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String token) {

        // Validate the token against the user type (e.g., "patient", "doctor", "admin")
        ResponseEntity<Map<String, Object>> authError = checkTokenValidity(token, userType);
        if (authError != null) {
            return authError;
        }

        // doctorService.getDoctorAvailability returns List<String> of available times
        List<String> availability = doctorService.getDoctorAvailability(doctorId, date);
        
        if (availability.isEmpty()) {
             return new ResponseEntity<>(
                Map.of("status", "info", "message", "Doctor not found or no available times for this date."), 
                HttpStatus.NOT_FOUND
            );
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("availableTimes", availability);
        return ResponseEntity.ok(response);
    }

    // 4. Define the `getDoctor` Method (Retrieves all doctors):
    // Service method: List<Doctor> getDoctors()
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllDoctors() {
        // doctorService.getDoctors() returns List<Doctor>
        List<Doctor> doctors = doctorService.getDoctors(); // Corrected service call name
        
        Map<String, Object> response = new HashMap<>();
        response.put("doctors", doctors);
        
        return ResponseEntity.ok(response);
    }

    // 5. Define the `saveDoctor` Method:
    // Service method: int saveDoctor(Doctor doctor) returns 1 (success), 0 (error), -1 (conflict)
    @PostMapping("/save/{token}")
    public ResponseEntity<Map<String, Object>> saveDoctor(
            @RequestBody Doctor doctor, 
            @PathVariable String token) {
        
        // Validate token for "admin" role
        ResponseEntity<Map<String, Object>> authError = checkTokenValidity(token, "admin");
        if (authError != null) {
            return authError;
        }

        int serviceResult = doctorService.saveDoctor(doctor);

        switch (serviceResult) {
            case 1 -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Doctor registered successfully.");
                // Note: service does not return the saved doctor, so we omit 'doctor' field in response
                return new ResponseEntity<>(response, HttpStatus.CREATED); // 201 Created
            }
            case -1 -> {
                // Conflict: Doctor with this email already exists
                return new ResponseEntity<>(
                        Map.of("status", "error", "message", "Doctor already exists or data conflict."),
                        HttpStatus.CONFLICT // 409 Conflict
                );
            }
            default -> {
                // General error (0)
                return new ResponseEntity<>(
                        Map.of("status", "error", "message", "Failed to register doctor due to an internal error."),
                        HttpStatus.INTERNAL_SERVER_ERROR // 500 Internal Server Error
                );
            }
        }
    }

    // 6. Define the `doctorLogin` Method:
    // Service method: String validateDoctor(String email, String password) returns token or error message
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> doctorLogin(@RequestBody Login loginDto) {
        // Use the validateDoctor method from DoctorService
        String serviceResult = doctorService.validateDoctor(loginDto.getEmail(), loginDto.getPassword());
        
        if (!serviceResult.contains("not found") && !serviceResult.contains("Invalid password")) {
            // Assume the result is the JWT token (success)
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Doctor login successful.");
            response.put("token", serviceResult);
            return ResponseEntity.ok(response);
        } else {
            // Result is an error message
            return new ResponseEntity<>(
                Map.of("status", "error", "message", serviceResult),
                HttpStatus.UNAUTHORIZED // 401 Unauthorized
            );
        }
    }

    // 7. Define the `updateDoctor` Method:
    // Service method: int updateDoctor(Long doctorId, Doctor updatedDoctor) returns 1 (success), -1 (not found)
    @PutMapping("/update/{doctorId}/{token}") // Added doctorId to path to identify target
    public ResponseEntity<Map<String, Object>> updateDoctor(
            @PathVariable Long doctorId,
            @RequestBody Doctor updatedDoctor, 
            @PathVariable String token) {
        
        // Validate token for "admin" role
        ResponseEntity<Map<String, Object>> authError = checkTokenValidity(token, "admin");
        if (authError != null) {
            return authError;
        }

        int serviceResult = doctorService.updateDoctor(doctorId, updatedDoctor);

        switch (serviceResult) {
            case 1 -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Doctor ID " + doctorId + " updated successfully.");
                // In a real app, you might fetch and return the updated Doctor object here
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            case -1 -> {
                return new ResponseEntity<>(
                        Map.of("status", "error", "message", "Doctor ID " + doctorId + " not found."),
                        HttpStatus.NOT_FOUND // 404 Not Found
                );
            }
            default -> {
                return new ResponseEntity<>(
                        Map.of("status", "error", "message", "Failed to update doctor."),
                        HttpStatus.INTERNAL_SERVER_ERROR // 500 Internal Server Error
                );
            }
        }
    }

    // 8. Define the `deleteDoctor` Method:
    // Service method: int deleteDoctor(Long doctorId) returns 1 (success), -1 (not found)
    @DeleteMapping("/{doctorId}/{token}")
    public ResponseEntity<Map<String, Object>> deleteDoctor(
            @PathVariable Long doctorId, 
            @PathVariable String token) {

        // Validate token for "admin" role
        ResponseEntity<Map<String, Object>> authError = checkTokenValidity(token, "admin");
        if (authError != null) {
            return authError;
        }

        int serviceResult = doctorService.deleteDoctor(doctorId);

        switch (serviceResult) {
            case 1 -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Doctor ID " + doctorId + " deleted successfully.");
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            case -1 -> {
                return new ResponseEntity<>(
                        Map.of("status", "error", "message", "Doctor ID " + doctorId + " not found."),
                        HttpStatus.NOT_FOUND // 404 Not Found
                );
            }
            default -> {
                return new ResponseEntity<>(
                        Map.of("status", "error", "message", "Failed to delete doctor due to an internal error."),
                        HttpStatus.INTERNAL_SERVER_ERROR // 500 Internal Server Error
                );
            }
        }
    }

    // 9. Define the `filter` Method:
    // Service method: List<Doctor> filterDoctor(String name, String specialty, String timePeriod)
    @GetMapping("/filter")
    public ResponseEntity<Map<String, Object>> filter(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String time,
            @RequestParam(required = false) String specialty) {
        
        // Calls the filterDoctor method in the general Service class (as seen in Service.java)
        List<Doctor> filteredDoctors = service.filterDoctor(name, specialty, time); 

        Map<String, Object> response = new HashMap<>();
        response.put("doctors", filteredDoctors);
        
        return ResponseEntity.ok(response);
    }

    // Define the `getDoctor` Method:
    // Service method: Doctor getDoctorDetails(String token)
    @GetMapping("/{token}")
    public ResponseEntity<Map<String, Object>> getDoctor(@PathVariable String token) {

        // Validate token for "doctor" role
        ResponseEntity<Map<String, Object>> authError = checkTokenValidity(token, "doctor");
        if (authError != null) {
            return authError;
        }

        // doctorService.getDoctorDetails is expected to extract ID/email from token
        Doctor doctor = doctorService.getDoctorDetails(token);

        if (doctor == null) {
            return new ResponseEntity<>(
                Map.of("status", "error", "message", "Doctor details not found after token validation."), 
                HttpStatus.NOT_FOUND
            );
        }
        DoctorDTO doctorDTO = new DoctorDTO(doctor.getId(),doctor.getName(),doctor.getEmail());
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("doctor", doctorDTO);
        return ResponseEntity.ok(response);
    }
}

// 1. Set Up the Controller Class:
//    - Annotate the class with `@RestController` to define it as a REST controller that serves JSON responses.
//    - Use `@RequestMapping("${api.path}doctor")` to prefix all endpoints with a configurable API path followed by "doctor".
//    - This class manages doctor-related functionalities such as registration, login, updates, and availability.


// 2. Autowire Dependencies:
//    - Inject `DoctorService` for handling the core logic related to doctors (e.g., CRUD operations, authentication).
//    - Inject the shared `Service` class for general-purpose features like token validation and filtering.


// 3. Define the `getDoctorAvailability` Method:
//    - Handles HTTP GET requests to check a specific doctor’s availability on a given date.
//    - Requires `user` type, `doctorId`, `date`, and `token` as path variables.
//    - First validates the token against the user type.
//    - If the token is invalid, returns an error response; otherwise, returns the availability status for the doctor.


// 4. Define the `getDoctor` Method:
//    - Handles HTTP GET requests to retrieve a list of all doctors.
//    - Returns the list within a response map under the key `"doctors"` with HTTP 200 OK status.


// 5. Define the `saveDoctor` Method:
//    - Handles HTTP POST requests to register a new doctor.
//    - Accepts a validated `Doctor` object in the request body and a token for authorization.
//    - Validates the token for the `"admin"` role before proceeding.
//    - If the doctor already exists, returns a conflict response; otherwise, adds the doctor and returns a success message.


// 6. Define the `doctorLogin` Method:
//    - Handles HTTP POST requests for doctor login.
//    - Accepts a validated `Login` DTO containing credentials.
//    - Delegates authentication to the `DoctorService` and returns login status and token information.


// 7. Define the `updateDoctor` Method:
//    - Handles HTTP PUT requests to update an existing doctor's information.
//    - Accepts a validated `Doctor` object and a token for authorization.
//    - Token must belong to an `"admin"`.
//    - If the doctor exists, updates the record and returns success; otherwise, returns not found or error messages.


// 8. Define the `deleteDoctor` Method:
//    - Handles HTTP DELETE requests to remove a doctor by ID.
//    - Requires both doctor ID and an admin token as path variables.
//    - If the doctor exists, deletes the record and returns a success message; otherwise, responds with a not found or error message.


// 9. Define the `filter` Method:
//    - Handles HTTP GET requests to filter doctors based on name, time, and specialty.
//    - Accepts `name`, `time`, and `speciality` as path variables.
//    - Calls the shared `Service` to perform filtering logic and returns matching doctors in the response.
