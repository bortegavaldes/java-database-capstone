package com.project.back_end.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.back_end.models.Prescription;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.PrescriptionService;
import com.project.back_end.services.MainService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("${api.path}prescription")
public class PrescriptionController {
    PrescriptionService prescriptionService;
    MainService service;
    AppointmentService appointmentService;

    public PrescriptionController(PrescriptionService prescriptionService, MainService service,
            AppointmentService appointmentService) {
        this.prescriptionService = prescriptionService;
        this.service = service;
        this.appointmentService = appointmentService;
    }

    /**
     * Helper method to validate the token (copied from PatientController for consistency).
     * @param token The security token.
     * @param requiredRole The role needed for authorization (e.g., "doctor").
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

    // 3. Define the `savePrescription` Method:
    // PrescriptionService.savePrescription returns ResponseEntity<String> (201 or 500)
    // AppointmentService.changeStatus returns int (1 on success, -1 on failure/not found)
    @PostMapping("/save/{token}")
    public ResponseEntity<?> savePrescription(
            @Valid @RequestBody Prescription prescription, 
            @PathVariable String token) {

        // 1. Validate token for "doctor" role
        ResponseEntity<Map<String, Object>> authError = checkTokenValidity(token, "doctor");
        if (authError != null) {
            return authError;
        }

        // 2. Save the prescription
        ResponseEntity<String> saveResponse = prescriptionService.savePrescription(prescription);

        if (saveResponse.getStatusCode() == HttpStatus.CREATED) {
            // 3. Update the corresponding appointment status after successful prescription save
            Long appointmentId = prescription.getAppointmentId();
            int PRESCRIPTION_STATUS_COMPLETED = 1;
            // Assume "Prescribed" is the new status.
            appointmentService.changeStatus(appointmentId, PRESCRIPTION_STATUS_COMPLETED);

            // Full success
            return new ResponseEntity<>(
                Map.of("status", "success", "message", saveResponse.getBody()),
                HttpStatus.CREATED
            );
        } else {
            // Propagate error from PrescriptionService (likely 500 Internal Server Error)
            return new ResponseEntity<>(
                Map.of("status", "error", "message", saveResponse.getBody()),
                saveResponse.getStatusCode()
            );
        }
    }

    // 4. Define the `getPrescription` Method:
    // PrescriptionService.getPrescription returns ResponseEntity<Map<String, Object>> (200 OK, 404 NOT_FOUND, or 500 ERROR)
    @GetMapping("/{appointmentId}/{token}")
    public ResponseEntity<Map<String, Object>> getPrescription(
            @PathVariable Long appointmentId, 
            @PathVariable String token) {

        // 1. Validate token for "doctor" role
        ResponseEntity<Map<String, Object>> authError = checkTokenValidity(token, "doctor");
        if (authError != null) {
            return authError;
        }

        // 2. Fetch the prescription(s)
        ResponseEntity<Map<String, Object>> serviceResponse = prescriptionService.getPrescription(appointmentId);

        // Service response already contains status, message, and the prescription object(s)
        return serviceResponse;
    }

}

// 3. Define the `savePrescription` Method:
//    - Handles HTTP POST requests to save a new prescription for a given appointment.
//    - Accepts a validated `Prescription` object in the request body and a doctor’s token as a path variable.
//    - Validates the token for the `"doctor"` role.
//    - If the token is valid, updates the status of the corresponding appointment to reflect that a prescription has been added.
//    - Delegates the saving logic to `PrescriptionService` and returns a response indicating success or failure.


// 4. Define the `getPrescription` Method:
//    - Handles HTTP GET requests to retrieve a prescription by its associated appointment ID.
//    - Accepts the appointment ID and a doctor’s token as path variables.
//    - Validates the token for the `"doctor"` role using the shared service.
//    - If the token is valid, fetches the prescription using the `PrescriptionService`.
//    - Returns the prescription details or an appropriate error message if validation fails.
