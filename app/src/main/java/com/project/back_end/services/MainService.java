package com.project.back_end.services;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Admin;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import org.springframework.stereotype.Service;

@Service
public class MainService {

    private final TokenService tokenService;
    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DoctorService doctorService;
    private final PatientService patientService;

    public MainService(TokenService tokenService, AdminRepository adminRepository, DoctorRepository doctorRepository,
            PatientRepository patientRepository, DoctorService doctorService, PatientService patientService) {
        this.tokenService = tokenService;
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.doctorService = doctorService;
        this.patientService = patientService;
    }

    // validateToken
    public ResponseEntity<String> validateToken(String token, String username) {
        try {
            boolean isValid = tokenService.validateToken(token, username);
            if (isValid) {
                return ResponseEntity.ok("Token is valid.");
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token.");
            }
        } catch (Exception e) {
            System.err.println("Token validation error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Token validation failed.");
        }
    }

    // validateAdmin
    public ResponseEntity<String> validateAdmin(String username, String password) {
        try {
            Optional<Admin> adminOpt = Optional.of(adminRepository.findByUsername(username));
            if (adminOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Admin not found.");
            }

            Admin admin = adminOpt.get();
            if (!admin.getPassword().equals(password)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid password.");
            }

            String token = tokenService.generateToken(admin.getId(), admin.getUsername());
            return ResponseEntity.ok(token);

        } catch (Exception e) {
            System.err.println("Admin login error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Login failed.");
        }
    }

    // filterDoctor
    public List<Doctor> filterDoctor(String name, String specialty, String timePeriod) {
        if (name != null && specialty != null && timePeriod != null) {
            return doctorService.filterDoctorsByNameSpecilityandTime(name, specialty, timePeriod);
        } else if (name != null && specialty != null) {
            return doctorService.filterDoctorByNameAndSpecility(name, specialty);
        } else if (name != null && timePeriod != null) {
            return doctorService.filterDoctorByNameAndTime(name, timePeriod);
        } else if (specialty != null && timePeriod != null) {
            return doctorService.filterDoctorByTimeAndSpecility(specialty, timePeriod);
        } else if (name != null) {
            return doctorService.findDoctorByName(name);
        } else if (specialty != null) {
            return doctorService.filterDoctorBySpecility(specialty);
        } else if (timePeriod != null) {
            return doctorService.filterDoctorsByTime(timePeriod);
        } else {
            return doctorService.getDoctors();
        }
    }

        // validateAppointment
    public int validateAppointment(Long doctorId, LocalDate date, LocalTime requestedTime) {
        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
        if (doctorOpt.isEmpty()) return -1;

        List<String> availableSlots = doctorService.getDoctorAvailability(doctorId, date);
        for (String slot : availableSlots) {
            try {
                LocalTime slotTime = LocalTime.parse(slot);
                if (slotTime.equals(requestedTime)) {
                    return 1;
                }
            } catch (DateTimeParseException ignored) {}
        }
        return 0;
    }

    // validatePatient
    public boolean validatePatient(String email, String phone) {
        boolean emailExists = patientRepository.findByEmail(email) != null;
        boolean phoneExists = patientRepository.findByEmailOrPhone(email,phone) != null;
        return !(emailExists || phoneExists);
    }

    // validatePatientLogin
    public ResponseEntity<String> validatePatientLogin(String email, String password) {
        try {
            Optional<Patient> patientOpt = Optional.of(patientRepository.findByEmail(email));
            if (patientOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Patient not found.");
            }

            Patient patient = patientOpt.get();
            if (!patient.getPassword().equals(password)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid password.");
            }

            String token = tokenService.generateToken(patient.getId(), patient.getEmail());
            return ResponseEntity.ok(token);

        } catch (Exception e) {
            System.err.println("Patient login error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Login failed.");
        }
    }

    // filterPatient
    public List<AppointmentDTO> filterPatient(String token, String condition, String doctorName) {
        try {
            String email = tokenService.extractEmail(token);
            Optional<Patient> patientOpt = Optional.of(patientRepository.findByEmail(email));
            if (patientOpt.isEmpty()) return Collections.emptyList();

            Long patientId = patientOpt.get().getId();

            if (condition != null && doctorName != null) {
                return patientService.filterByDoctorAndCondition(doctorName, patientId, condition);
            } else if (condition != null) {
                return patientService.filterByCondition(patientId, condition);
            } else if (doctorName != null) {
                return patientService.filterByDoctor(doctorName, patientId);
            } else {
                return patientService.getPatientAppointment(patientId);
            }

        } catch (Exception e) {
            System.err.println("Error filtering patient appointments: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public Long extractIdFromToken(String token) {
        String email = tokenService.extractEmail(token);
        Optional<Patient> patientOpt = Optional.of(patientRepository.findByEmail(email));
        if (patientOpt.isEmpty()){
            return null;
        }
        Long patientId = patientOpt.get().getId();
        return patientId;
    }
}

// 1. **@Service Annotation**
// The @Service annotation marks this class as a service component in Spring. This allows Spring to automatically detect it through component scanning
// and manage its lifecycle, enabling it to be injected into controllers or other services using @Autowired or constructor injection.
// 2. **Constructor Injection for Dependencies**
// The constructor injects all required dependencies (TokenService, Repositories, and other Services). This approach promotes loose coupling, improves testability,
// and ensures that all required dependencies are provided at object creation time.
// 3. **validateToken Method**
// This method checks if the provided JWT token is valid for a specific user. It uses the TokenService to perform the validation.
// If the token is invalid or expired, it returns a 401 Unauthorized response with an appropriate error message. This ensures security by preventing
// unauthorized access to protected resources.
// 4. **validateAdmin Method**
// This method validates the login credentials for an admin user.
// - It first searches the admin repository using the provided username.
// - If an admin is found, it checks if the password matches.
// - If the password is correct, it generates and returns a JWT token (using the admin’s username) with a 200 OK status.
// - If the password is incorrect, it returns a 401 Unauthorized status with an error message.
// - If no admin is found, it also returns a 401 Unauthorized.
// - If any unexpected error occurs during the process, a 500 Internal Server Error response is returned.
// This method ensures that only valid admin users can access secured parts of the system.
// 5. **filterDoctor Method**
// This method provides filtering functionality for doctors based on name, specialty, and available time slots.
// - It supports various combinations of the three filters.
// - If none of the filters are provided, it returns all available doctors.
// This flexible filtering mechanism allows the frontend or consumers of the API to search and narrow down doctors based on user criteria.
// 6. **validateAppointment Method**
// This method validates if the requested appointment time for a doctor is available.
// - It first checks if the doctor exists in the repository.
// - Then, it retrieves the list of available time slots for the doctor on the specified date.
// - It compares the requested appointment time with the start times of these slots.
// - If a match is found, it returns 1 (valid appointment time).
// - If no matching time slot is found, it returns 0 (invalid).
// - If the doctor doesn’t exist, it returns -1.
// This logic prevents overlapping or invalid appointment bookings.
// 7. **validatePatient Method**
// This method checks whether a patient with the same email or phone number already exists in the system.
// - If a match is found, it returns false (indicating the patient is not valid for new registration).
// - If no match is found, it returns true.
// This helps enforce uniqueness constraints on patient records and prevent duplicate entries.
// 8. **validatePatientLogin Method**
// This method handles login validation for patient users.
// - It looks up the patient by email.
// - If found, it checks whether the provided password matches the stored one.
// - On successful validation, it generates a JWT token and returns it with a 200 OK status.
// - If the password is incorrect or the patient doesn't exist, it returns a 401 Unauthorized with a relevant error.
// - If an exception occurs, it returns a 500 Internal Server Error.
// This method ensures only legitimate patients can log in and access their data securely.
// 9. **filterPatient Method**
// This method filters a patient's appointment history based on condition and doctor name.
// - It extracts the email from the JWT token to identify the patient.
// - Depending on which filters (condition, doctor name) are provided, it delegates the filtering logic to PatientService.
// - If no filters are provided, it retrieves all appointments for the patient.
// This flexible method supports patient-specific querying and enhances user experience on the client side.
