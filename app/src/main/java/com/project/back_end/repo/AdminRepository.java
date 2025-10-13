package com.project.back_end.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.back_end.models.Admin;

@Repository 
public interface AdminRepository extends JpaRepository<Admin, Long>{
    
// 2. Custom Query Method:
//    - **findByUsername**:
//      - This method allows you to find an Admin by their username.
//      - Return type: Admin
//      - Parameter: String username
//      - It will return an Admin entity that matches the provided username.
//      - If no Admin is found with the given username, it returns null.
    Admin findByUsername(String username);
}
