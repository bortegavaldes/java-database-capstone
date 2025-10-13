package com.project.back_end.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;

    // Constructor Injection
    public AppointmentService(AppointmentRepository appointmentRepository,
            DoctorRepository doctorRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
    }

    // 4. Book Appointment
    @Transactional
    public int bookAppointment(Appointment appointment) {
        try {
            appointmentRepository.save(appointment);
            return 1;
        } catch (Exception e) {
            // Log the error if needed
            return 0;
        }
    }

    // Update Appointment
    @Transactional
    public String updateAppointment(Long appointmentId, Appointment updatedAppointment, Long patientId) {
        Optional<Appointment> existingOpt = appointmentRepository.findById(appointmentId);
        if (existingOpt.isEmpty()) {
            return "Appointment not found.";
        }

        Appointment existing = existingOpt.get();
        if (!existing.getPatient().getId().equals(patientId)) {
            return "Unauthorized: Patient ID mismatch.";
        }

        if (!isDoctorAvailable(updatedAppointment.getDoctor().getId(), updatedAppointment.getAppointmentTime())) {
            return "Doctor is not available at the selected time.";
        }

        existing.setAppointmentTime(updatedAppointment.getAppointmentTime());
        existing.setDoctor(updatedAppointment.getDoctor());
        existing.setStatus(updatedAppointment.getStatus());

        appointmentRepository.save(existing);
        return "Appointment updated successfully.";
    }

    // Helper method to check doctor availability
    private boolean isDoctorAvailable(Long doctorId, LocalDateTime appointmentTime) {
        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
        if (doctorOpt.isEmpty()) {
            return false;
        }

        List<String> availableTimes = doctorOpt.get().getAvailableTimes();
        LocalTime appointmentLocalTime = appointmentTime.toLocalTime();

        for (String timeSlot : availableTimes) {
            String[] parts = timeSlot.split("-");
            if (parts.length != 2) {
                continue;
            }

            try {
                LocalTime start = LocalTime.parse(parts[0].trim());
                LocalTime end = LocalTime.parse(parts[1].trim());

                if (!appointmentLocalTime.isBefore(start) && !appointmentLocalTime.isAfter(end)) {
                    return true;
                }
            } catch (DateTimeParseException e) {
                // Log or handle invalid time format
            }
        }

        return false;
    }

    // Cancel Appointment
    @Transactional
    public String cancelAppointment(Long appointmentId, Long patientId) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isEmpty()) {
            return "Appointment not found.";
        }

        Appointment appointment = appointmentOpt.get();
        if (!appointment.getPatient().getId().equals(patientId)) {
            return "Unauthorized: Patient ID mismatch.";
        }

        appointmentRepository.delete(appointment);
        return "Appointment cancelled successfully.";
    }

    // Get Appointments
    @Transactional(readOnly = true)
    public List<Appointment> getAppointments(Long doctorId, LocalDate date, String patientName) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        if (patientName != null && !patientName.isBlank() && !"0".equals(patientName.trim())) {
            return appointmentRepository.findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
                    doctorId, patientName, start, end);
        } else {
            return appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(doctorId, start, end);
        }
    }

    // Change Status
    @Transactional
    public String changeStatus(Long appointmentId, int status) {
        try {
            appointmentRepository.updateStatus(status, appointmentId);
            return "Status updated successfully.";
        } catch (Exception e) {
            return "Failed to update status.";
        }
    }

}

// 1. **Add @Service Annotation**:
//    - To indicate that this class is a service layer class for handling business logic.
//    - The `@Service` annotation should be added before the class declaration to mark it as a Spring service component.
//    - Instruction: Add `@Service` above the class definition.
// 2. **Constructor Injection for Dependencies**:
//    - The `AppointmentService` class requires several dependencies like `AppointmentRepository`, `Service`, `TokenService`, `PatientRepository`, and `DoctorRepository`.
//    - These dependencies should be injected through the constructor.
//    - Instruction: Ensure constructor injection is used for proper dependency management in Spring.
// 3. **Add @Transactional Annotation for Methods that Modify Database**:
//    - The methods that modify or update the database should be annotated with `@Transactional` to ensure atomicity and consistency of the operations.
//    - Instruction: Add the `@Transactional` annotation above methods that interact with the database, especially those modifying data.
// 4. **Book Appointment Method**:
//    - Responsible for saving the new appointment to the database.
//    - If the save operation fails, it returns `0`; otherwise, it returns `1`.
//    - Instruction: Ensure that the method handles any exceptions and returns an appropriate result code.
// 5. **Update Appointment Method**:
//    - This method is used to update an existing appointment based on its ID.
//    - It validates whether the patient ID matches, checks if the appointment is available for updating, and ensures that the doctor is available at the specified time.
//    - If the update is successful, it saves the appointment; otherwise, it returns an appropriate error message.
//    - Instruction: Ensure proper validation and error handling is included for appointment updates.
// 6. **Cancel Appointment Method**:
//    - This method cancels an appointment by deleting it from the database.
//    - It ensures the patient who owns the appointment is trying to cancel it and handles possible errors.
//    - Instruction: Make sure that the method checks for the patient ID match before deleting the appointment.
// 7. **Get Appointments Method**:
//    - This method retrieves a list of appointments for a specific doctor on a particular day, optionally filtered by the patient's name.
//    - It uses `@Transactional` to ensure that database operations are consistent and handled in a single transaction.
//    - Instruction: Ensure the correct use of transaction boundaries, especially when querying the database for appointments.
// 8. **Change Status Method**:
//    - This method updates the status of an appointment by changing its value in the database.
//    - It should be annotated with `@Transactional` to ensure the operation is executed in a single transaction.
//    - Instruction: Add `@Transactional` before this method to ensure atomicity when updating appointment status.

