package com.project.back_end.repo;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.project.back_end.models.Appointment;

@Repository
public interface AppointmentRepository  extends JpaRepository<Appointment, Long>{
//Custom Query Methods:

//    - **findByDoctorIdAndAppointmentTimeBetween**:
//      - This method retrieves a list of appointments for a specific doctor within a given time range.
//      - The doctor’s available times are eagerly fetched to avoid lazy loading.
//      - Return type: List<Appointment>
//      - Parameters: Long doctorId, LocalDateTime start, LocalDateTime end
//      - It uses a LEFT JOIN to fetch the doctor’s available times along with the appointments.
   @Query("SELECT a FROM Appointment a " +
           "LEFT JOIN FETCH a.doctor d " +
           //"LEFT JOIN FETCH d.availableTimes " +
           "WHERE d.id = :doctorId AND a.appointmentTime BETWEEN :start AND :end")
   List<Appointment> findByDoctorIdAndAppointmentTimeBetween(
        @Param("doctorId") Long doctorId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

//    - **findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween**:
//      - This method retrieves appointments for a specific doctor and patient name (ignoring case) within a given time range.
//      - It performs a LEFT JOIN to fetch both the doctor and patient details along with the appointment times.
//      - Return type: List<Appointment>
//      - Parameters: Long doctorId, String patientName, LocalDateTime start, LocalDateTime end
   @Query("SELECT a FROM Appointment a " +
           "LEFT JOIN FETCH a.doctor d " +
           "LEFT JOIN FETCH a.patient p " +
           "WHERE d.id = :doctorId " +
           "AND LOWER(p.name) LIKE LOWER(CONCAT('%', :patientName, '%')) " +
           "AND a.appointmentTime BETWEEN :start AND :end")
   List<Appointment> findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
        @Param("doctorId") Long doctorId,
        @Param("patientName") String patientName,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

//    - **deleteAllByDoctorId**:
//      - This method deletes all appointments associated with a particular doctor.
//      - It is marked as @Modifying and @Transactional, which makes it a modification query, ensuring that the operation is executed within a transaction.
//      - Return type: void
//      - Parameters: Long doctorId
   @Modifying
   @Transactional
   @Query("DELETE FROM Appointment a WHERE a.doctor.id = :doctorId")
   void deleteAllByDoctorId(@Param("doctorId") Long doctorId);

//    - **findByPatientId**:
//      - This method retrieves all appointments for a specific patient.
//      - Return type: List<Appointment>
//      - Parameters: Long patientId
   List<Appointment> findByPatientId(Long patientId);

//    - **findByPatient_IdAndStatusOrderByAppointmentTimeAsc**:
//      - This method retrieves all appointments for a specific patient with a given status, ordered by the appointment time.
//      - Return type: List<Appointment>
//      - Parameters: Long patientId, int status
   List<Appointment> findByPatient_IdAndStatusOrderByAppointmentTimeAsc(Long patientId, int status);

//    - **filterByDoctorNameAndPatientId**:
//      - This method retrieves appointments based on a doctor’s name (using a LIKE query) and the patient’s ID.
//      - Return type: List<Appointment>
//      - Parameters: String doctorName, Long patientId
   @Query("SELECT a FROM Appointment a " +
           "JOIN a.doctor d " +
           "WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :doctorName, '%')) " +
           "AND a.patient.id = :patientId")
    List<Appointment> filterByDoctorNameAndPatientId(
        @Param("doctorName") String doctorName,
        @Param("patientId") Long patientId
    );

//    - **filterByDoctorNameAndPatientIdAndStatus**:
//      - This method retrieves appointments based on a doctor’s name (using a LIKE query), patient’s ID, and a specific appointment status.
//      - Return type: List<Appointment>
//      - Parameters: String doctorName, Long patientId, int status
   @Query("SELECT a FROM Appointment a " +
           "JOIN a.doctor d " +
           "WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :doctorName, '%')) " +
           "AND a.patient.id = :patientId " +
           "AND a.status = :status")
   List<Appointment> filterByDoctorNameAndPatientIdAndStatus(
        @Param("doctorName") String doctorName,
        @Param("patientId") Long patientId,
        @Param("status") int status
   );

//    - **updateStatus**:
//      - This method updates the status of a specific appointment based on its ID.
//      - Return type: void
//      - Parameters: int status, long id
   @Modifying
   @Transactional
   @Query("UPDATE Appointment a SET a.status = :status WHERE a.id = :id")
   void updateStatus(@Param("status") int status, @Param("id") long id);


// 3. @Modifying and @Transactional annotations:
//    - The @Modifying annotation is used to indicate that the method performs a modification operation (like DELETE or UPDATE).
//    - The @Transactional annotation ensures that the modification is done within a transaction, meaning that if any exception occurs, the changes will be rolled back.

}
