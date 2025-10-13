package com.project.back_end.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;

@Service
public class DoctorService {

    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;
    private final DoctorRepository doctorRepository;

    public DoctorService(AppointmentRepository appointmentRepository, TokenService tokenService,
            DoctorRepository doctorRepository) {
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
        this.doctorRepository = doctorRepository;
    }

    public List<String> getDoctorAvailability(Long doctorId, LocalDate date) {
        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
        if (doctorOpt.isEmpty()) {
            return Collections.emptyList();
        }

        Doctor doctor = doctorOpt.get();
        List<String> availableSlots = doctor.getAvailableTimes();
        if (availableSlots == null || availableSlots.isEmpty()) {
            return Collections.emptyList();
        }

        List<Appointment> bookedAppointments = appointmentRepository
                .findByDoctorIdAndAppointmentTimeBetween(doctorId, date.atStartOfDay(), date.atTime(LocalTime.MAX));

        Set<LocalTime> bookedHours = bookedAppointments.stream()
                .map(Appointment::getAppointmentTime)
                .map(LocalDateTime::toLocalTime)
                .map(time -> time.truncatedTo(ChronoUnit.HOURS))
                .collect(Collectors.toSet());

        List<String> available = new ArrayList<>();

        for (String slot : availableSlots) {
            String[] parts = slot.split("-");
            if (parts.length != 2) {
                continue;
            }
            try {
                LocalTime start = LocalTime.parse(parts[0].trim());
                LocalTime end = LocalTime.parse(parts[1].trim());

                LocalTime current = start;
                while (!current.isAfter(end.minusHours(1))) {
                    if (!bookedHours.contains(current)) {
                        available.add(current.toString()); // e.g., "09:00"
                    }
                    current = current.plusHours(1);
                }
            } catch (DateTimeParseException ignored) {
            }
        }

        return available;
    }

    // 5. saveDoctor
    public int saveDoctor(Doctor doctor) {
        try {
            if (doctorRepository.findByEmail(doctor.getEmail()) == null) {
                return -1;
            }
            doctorRepository.save(doctor);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

     // 6. updateDoctor
    public int updateDoctor(Long doctorId, Doctor updatedDoctor) {
        Optional<Doctor> existingOpt = doctorRepository.findById(doctorId);
        if (existingOpt.isEmpty()) return -1;

        Doctor existing = existingOpt.get();
        existing.setName(updatedDoctor.getName());
        existing.setEmail(updatedDoctor.getEmail());
        existing.setPassword(updatedDoctor.getPassword());
        existing.setSpecialty(updatedDoctor.getSpecialty());
        existing.setAvailableTimes(updatedDoctor.getAvailableTimes());

        doctorRepository.save(existing);
        return 1;
    }

     // 7. getDoctors
    @Transactional(readOnly = true)
    public List<Doctor> getDoctors() {
        List<Doctor> doctors = doctorRepository.findAll();
        doctors.forEach(d -> d.getAvailableTimes().size()); // Force eager loading
        return doctors;
    }

    // 8. deleteDoctor
    @Transactional
    public int deleteDoctor(Long doctorId) {
        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
        if (doctorOpt.isEmpty()) return -1;

        appointmentRepository.deleteAllByDoctorId(doctorId);
        doctorRepository.deleteById(doctorId);
        return 1;
    }

    // 9. validateDoctor
    public String validateDoctor(String email, String password) {
        Optional<Doctor> doctorOpt = Optional.of(doctorRepository.findByEmail(email));
        if (doctorOpt.isEmpty()) return "Doctor not found.";

        Doctor doctor = doctorOpt.get();
        if (!doctor.getPassword().equals(password)) return "Invalid password.";

        return tokenService.generateToken(doctor.getId(), doctor.getEmail());
    }

     // 10. findDoctorByName
    @Transactional(readOnly = true)
    public List<Doctor> findDoctorByName(String name) {
        List<Doctor> doctors = doctorRepository.findByNameLike(name);
        doctors.forEach(d -> d.getAvailableTimes().size());
        return doctors;
    }

    // 11. filterDoctorsByNameSpecilityandTime
    public List<Doctor> filterDoctorsByNameSpecilityandTime(String name, String specialty, String period) {
        List<Doctor> doctors = doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(name, specialty);
        return filterDoctorByTime(doctors, period);
    }

    // 12. filterDoctorByTime
    public List<Doctor> filterDoctorByTime(List<Doctor> doctors, String period) {
        return doctors.stream()
                .filter(d -> isAvailableDuringPeriod(d.getAvailableTimes(), period))
                .collect(Collectors.toList());
    }

     // Helper: Check if any slot matches AM/PM
    private boolean isAvailableDuringPeriod(List<String> slots, String period) {
        for (String slot : slots) {
            String[] parts = slot.split("-");
            if (parts.length != 2) continue;
            try {
                LocalTime start = LocalTime.parse(parts[0].trim());
                LocalTime end = LocalTime.parse(parts[1].trim());
                if ("AM".equalsIgnoreCase(period) && start.isBefore(LocalTime.NOON)) return true;
                if ("PM".equalsIgnoreCase(period) && end.isAfter(LocalTime.NOON)) return true;
            } catch (DateTimeParseException ignored) {}
        }
        return false;
    }

    // 13. filterDoctorByNameAndTime
    public List<Doctor> filterDoctorByNameAndTime(String name, String period) {
        List<Doctor> doctors = doctorRepository.findByNameLike(name);
        return filterDoctorByTime(doctors, period);
    }

    // 14. filterDoctorByNameAndSpecility
    public List<Doctor> filterDoctorByNameAndSpecility(String name, String specialty) {
        return doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(name, specialty);
    }

    // 15. filterDoctorByTimeAndSpecility
    public List<Doctor> filterDoctorByTimeAndSpecility(String specialty, String period) {
        List<Doctor> doctors = doctorRepository.findBySpecialtyIgnoreCase(specialty);
        return filterDoctorByTime(doctors, period);
    }

    // 16. filterDoctorBySpecility
    public List<Doctor> filterDoctorBySpecility(String specialty) {
        return doctorRepository.findBySpecialtyIgnoreCase(specialty);
    }

    // 17. filterDoctorsByTime
    public List<Doctor> filterDoctorsByTime(String period) {
        List<Doctor> doctors = doctorRepository.findAll();
        return filterDoctorByTime(doctors, period);
    }

    // getDoctorDetails
    public Doctor getDoctorDetails(String token) {
        try {
            String email = tokenService.extractEmail(token);
            Doctor doctor = doctorRepository.findByEmail(email);
            if (doctor == null)
                throw new RuntimeException("Doctor not found for email: " + email);
            else
                return doctor;
        } catch (RuntimeException e) {
            System.err.println("Error retrieving patient details: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Error retrieving patient details: " + e.getMessage());
            return null;
        }
    }

}

// 1. **Add @Service Annotation**:
//    - This class should be annotated with `@Service` to indicate that it is a service layer class.
//    - The `@Service` annotation marks this class as a Spring-managed bean for business logic.
//    - Instruction: Add `@Service` above the class declaration.
// 2. **Constructor Injection for Dependencies**:
//    - The `DoctorService` class depends on `DoctorRepository`, `AppointmentRepository`, and `TokenService`.
//    - These dependencies should be injected via the constructor for proper dependency management.
//    - Instruction: Ensure constructor injection is used for injecting dependencies into the service.
// 3. **Add @Transactional Annotation for Methods that Modify or Fetch Database Data**:
//    - Methods like `getDoctorAvailability`, `getDoctors`, `findDoctorByName`, `filterDoctorsBy*` should be annotated with `@Transactional`.
//    - The `@Transactional` annotation ensures that database operations are consistent and wrapped in a single transaction.
//    - Instruction: Add the `@Transactional` annotation above the methods that perform database operations or queries.
// 4. **getDoctorAvailability Method**:
//    - Retrieves the available time slots for a specific doctor on a particular date and filters out already booked slots.
//    - The method fetches all appointments for the doctor on the given date and calculates the availability by comparing against booked slots.
//    - Instruction: Ensure that the time slots are properly formatted and the available slots are correctly filtered.
// 5. **saveDoctor Method**:
//    - Used to save a new doctor record in the database after checking if a doctor with the same email already exists.
//    - If a doctor with the same email is found, it returns `-1` to indicate conflict; `1` for success, and `0` for internal errors.
//    - Instruction: Ensure that the method correctly handles conflicts and exceptions when saving a doctor.
// 6. **updateDoctor Method**:
//    - Updates an existing doctor's details in the database. If the doctor doesn't exist, it returns `-1`.
//    - Instruction: Make sure that the doctor exists before attempting to save the updated record and handle any errors properly.
// 7. **getDoctors Method**:
//    - Fetches all doctors from the database. It is marked with `@Transactional` to ensure that the collection is properly loaded.
//    - Instruction: Ensure that the collection is eagerly loaded, especially if dealing with lazy-loaded relationships (e.g., available times). 
// 8. **deleteDoctor Method**:
//    - Deletes a doctor from the system along with all appointments associated with that doctor.
//    - It first checks if the doctor exists. If not, it returns `-1`; otherwise, it deletes the doctor and their appointments.
//    - Instruction: Ensure the doctor and their appointments are deleted properly, with error handling for internal issues.
// 9. **validateDoctor Method**:
//    - Validates a doctor's login by checking if the email and password match an existing doctor record.
//    - It generates a token for the doctor if the login is successful, otherwise returns an error message.
//    - Instruction: Make sure to handle invalid login attempts and password mismatches properly with error responses.
// 10. **findDoctorByName Method**:
//    - Finds doctors based on partial name matching and returns the list of doctors with their available times.
//    - This method is annotated with `@Transactional` to ensure that the database query and data retrieval are properly managed within a transaction.
//    - Instruction: Ensure that available times are eagerly loaded for the doctors.
// 11. **filterDoctorsByNameSpecilityandTime Method**:
//    - Filters doctors based on their name, specialty, and availability during a specific time (AM/PM).
//    - The method fetches doctors matching the name and specialty criteria, then filters them based on their availability during the specified time period.
//    - Instruction: Ensure proper filtering based on both the name and specialty as well as the specified time period.
// 12. **filterDoctorByTime Method**:
//    - Filters a list of doctors based on whether their available times match the specified time period (AM/PM).
//    - This method processes a list of doctors and their available times to return those that fit the time criteria.
//    - Instruction: Ensure that the time filtering logic correctly handles both AM and PM time slots and edge cases.
// 13. **filterDoctorByNameAndTime Method**:
//    - Filters doctors based on their name and the specified time period (AM/PM).
//    - Fetches doctors based on partial name matching and filters the results to include only those available during the specified time period.
//    - Instruction: Ensure that the method correctly filters doctors based on the given name and time of day (AM/PM).
// 14. **filterDoctorByNameAndSpecility Method**:
//    - Filters doctors by name and specialty.
//    - It ensures that the resulting list of doctors matches both the name (case-insensitive) and the specified specialty.
//    - Instruction: Ensure that both name and specialty are considered when filtering doctors.
// 15. **filterDoctorByTimeAndSpecility Method**:
//    - Filters doctors based on their specialty and availability during a specific time period (AM/PM).
//    - Fetches doctors based on the specified specialty and filters them based on their available time slots for AM/PM.
//    - Instruction: Ensure the time filtering is accurately applied based on the given specialty and time period (AM/PM).
// 16. **filterDoctorBySpecility Method**:
//    - Filters doctors based on their specialty.
//    - This method fetches all doctors matching the specified specialty and returns them.
//    - Instruction: Make sure the filtering logic works for case-insensitive specialty matching.
// 17. **filterDoctorsByTime Method**:
//    - Filters all doctors based on their availability during a specific time period (AM/PM).
//    - The method checks all doctors' available times and returns those available during the specified time period.
//    - Instruction: Ensure proper filtering logic to handle AM/PM time periods.
