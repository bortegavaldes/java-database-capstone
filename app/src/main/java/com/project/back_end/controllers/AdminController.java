
package com.project.back_end.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.back_end.models.Admin;
import com.project.back_end.services.MainService;

@RestController
@RequestMapping("${api.path}admin")
public class AdminController {

    private final MainService service;

    public AdminController(MainService service) {
        this.service = service;
    }

     // Define the `adminLogin` Method:
    @PostMapping("/login") // Handles HTTP POST requests for admin login functionality
    public ResponseEntity<Map<String, Object>> adminLogin(@RequestBody Admin admin) {
        
        // Delegate authentication logic to the service layer, which now returns a ResponseEntity<String>
        ResponseEntity<String> serviceResponse = service.validateAdmin(
            admin.getUsername(), // Assuming Admin model has getUsername()
            admin.getPassword()  // Assuming Admin model has getPassword()
        );

        // Check the status of the ResponseEntity returned by the service
        if (serviceResponse.getStatusCode() == HttpStatus.OK) {
            // Success: The response body contains the JWT token
            String token = serviceResponse.getBody();
            
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("status", "success");
            responseBody.put("message", "Admin logged in successfully.");
            responseBody.put("token", token);
            
            // Return the custom response map with HTTP 200 OK
            return new ResponseEntity<>(responseBody, HttpStatus.OK);
            
        } else {
            // Failure: The service response has an error status (e.g., 401, 500)
            // and the body contains the error message (e.g., "Admin not found.")
            String errorMessage = serviceResponse.getBody();
            
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("status", "error");
            // Use the detailed error message from the service
            responseBody.put("message", errorMessage); 
            
            // Forward the original error status code from the service (e.g., 401 UNAUTHORIZED)
            return new ResponseEntity<>(responseBody, serviceResponse.getStatusCode());
        }
    }
}

// 1. Set Up the Controller Class:
//    - Annotate the class with `@RestController` to indicate that it's a REST controller, used to handle web requests and return JSON responses.
//    - Use `@RequestMapping("${api.path}admin")` to define a base path for all endpoints in this controller.
//    - This allows the use of an external property (`api.path`) for flexible configuration of endpoint paths.


// 2. Autowire Service Dependency:
//    - Use constructor injection to autowire the `Service` class.
//    - The service handles core logic related to admin validation and token checking.
//    - This promotes cleaner code and separation of concerns between the controller and business logic layer.


// 3. Define the `adminLogin` Method:
//    - Handles HTTP POST requests for admin login functionality.
//    - Accepts an `Admin` object in the request body, which contains login credentials.
//    - Delegates authentication logic to the `validateAdmin` method in the service layer.
//    - Returns a `ResponseEntity` with a `Map` containing login status or messages.

