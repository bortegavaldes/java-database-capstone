package com.project.back_end.models;

import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Document(collection = "prescriptions")
public class Prescription {

    @Id
    private String id;

    @NotNull(message = "The patient name is required")
    @Size(min = 3, max = 100)
    private String patientName;

    @NotNull(message = "The appointment ID is required for the prescription")
    private Long appointmentId;

    @NotNull(message = "The medication name is required")
    @Size(min = 3, max = 100)
    private String medication;

    @NotNull(message = "Ensures that the dosage information is provided")
    private String dosage;

    @Size(max = 200)
    private String doctorNotes;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getMedication() {
        return medication;
    }

    public void setMedication(String medication) {
        this.medication = medication;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getDoctorNotes() {
        return doctorNotes;
    }

    public void setDoctorNotes(String doctorNotes) {
        this.doctorNotes = doctorNotes;
    }

}
