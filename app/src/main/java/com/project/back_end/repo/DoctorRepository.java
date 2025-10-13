package com.project.back_end.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.back_end.models.Doctor;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long>{

//Custom Query Methods:
//    - **findByEmail**:
//      - This method retrieves a Doctor by their email.
//      - Return type: Doctor
//      - Parameters: String email
   Doctor findByEmail(String email);

//    - **findByNameLike**:
//      - This method retrieves a list of Doctors whose name contains the provided search string (case-sensitive).
//      - The `CONCAT('%', :name, '%')` is used to create a pattern for partial matching.
//      - Return type: List<Doctor>
//      - Parameters: String name
   @Query("SELECT d FROM Doctor d WHERE d.name LIKE CONCAT('%', :name, '%')")
   List<Doctor> findByNameLike(@Param("name") String name);

//    - **findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase**:
//      - This method retrieves a list of Doctors where the name contains the search string (case-insensitive) and the specialty matches exactly (case-insensitive).
//      - It combines both fields for a more specific search.
//      - Return type: List<Doctor>
//      - Parameters: String name, String specialty
   List<Doctor> findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(String name, String specialty);

//    - **findBySpecialtyIgnoreCase**:
//      - This method retrieves a list of Doctors with the specified specialty, ignoring case sensitivity.
//      - Return type: List<Doctor>
//      - Parameters: String specialty
   List<Doctor> findBySpecialtyIgnoreCase(String specialty);

}