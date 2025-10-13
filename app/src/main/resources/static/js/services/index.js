//import openModal from "../components/modals.js";
import {API_BASE_URL} from "../config/config.js";

const ADMIN_API = API_BASE_URL + '/admin/login';
const DOCTOR_API = API_BASE_URL + '/doctor/login';

/*
window.onload = function () {
  const adminBtn = document.getElementById('adminLogin');
  if (adminBtn) {
    adminBtn.addEventListener('click', () => {
      openModal('adminLogin');
    });
  }
}*/

window.adminLoginHandler = async function () {
  // Step 1: Get the entered username and password from the input fields
  const usernameInput = document.getElementById('username');
  const passwordInput = document.getElementById('password');

  if (!usernameInput || !passwordInput) {
    console.error("Login input fields not found. Ensure elements with IDs 'username' and 'password' exist.");
    alert("A critical error occurred: Login form elements are missing.");
    return;
  }

  const username = usernameInput.value;
  const password = passwordInput.value;

  // Step 2: Create an admin object with these credentials
  const adminCredentials = {
    username: username,
    password: password
  };

  // Step 6: Wrap everything in a try-catch to handle network or server errors
  try {
    console.log("Attempting admin login...");

    // Step 3: Use fetch() to send a POST request to the ADMIN_API endpoint
    const response = await fetch(ADMIN_API, {
      method: 'POST', // Set method to POST
      headers: {
        'Content-Type': 'application/json' // Add headers
      },
      // Convert the admin object to JSON and send in the body
      body: JSON.stringify(adminCredentials)
    });

    if (response.ok) { // Checks if status code is 200-299
      // Step 4: If the response is successful:

      // Parse the JSON response to get the token
      const data = await response.json();
      const token = data.token; // Assumes the token is in a field named 'token'

      if (token) {
        // Store the token in localStorage
        localStorage.setItem('token', token);

        // Call selectRole('admin') to proceed with admin-specific behavior
        selectRole('admin');
        alert("Admin login successful!");

      } else {
        // Successful response, but missing token (unlikely but good to check)
        console.error("Server responded successfully but did not provide a token.", data);
        alert("Login failed: Authentication token is missing from the server response.");
      }

    } else {
      // Step 5: If login fails or credentials are invalid:

      // Try to get a specific error message from the server response body
      let errorMessage = "Login failed. Please check your username and password.";
      try {
        const errorData = await response.json();
        if (errorData.message) {
          errorMessage = errorData.message; // Use server-provided message
        }
      } catch (e) {
        // Ignore if response body isn't JSON or message isn't present
        console.warn("Could not parse specific error message from server response.");
      }

      // Show an alert with an error message
      alert(`Login Failed: ${errorMessage}`);
      console.error(`Login failed with status ${response.status}:`, response);
    }

  } catch (error) {
    // Step 6 (cont.): Show a generic error message if something goes wrong
    console.error("Network or server error during admin login:", error);
    alert("A network error occurred. Please try again later or contact support.");
  }
};

window.doctorLoginHandler = async function () {
  // Step 1: Get the entered username and password from the input fields
  const emailInput = document.getElementById('email');
  const passwordInput = document.getElementById('password');

  if (!emailInput || !passwordInput) {
    console.error("Login input fields not found. Ensure elements with IDs 'email' and 'password' exist.");
    alert("A critical error occurred: Login form elements are missing.");
    return;
  }

  const email = emailInput.value;
  const password = passwordInput.value;

  // Step 2: Create an admin object with these credentials
  const doctorCredentials = {
    email: email,
    password: password
  };

  // Step 6: Wrap everything in a try-catch to handle network or server errors
  try {
    console.log("Attempting admin login...");

    // Step 3: Use fetch() to send a POST request to the DOCTOR_API endpoint
    const response = await fetch(DOCTOR_API, {
      method: 'POST', // Set method to POST
      headers: {
        'Content-Type': 'application/json' // Add headers
      },
      // Convert the doctor object to JSON and send in the body
      body: JSON.stringify(doctorCredentials)
    });

    if (response.ok) { // Checks if status code is 200-299
      // Step 4: If the response is successful:

      // Parse the JSON response to get the token
      const data = await response.json();
      const token = data.token; // Assumes the token is in a field named 'token'

      if (token) {
        // Store the token in localStorage
        localStorage.setItem('token', token);

        // Call selectRole('admin') to proceed with admin-specific behavior
        selectRole('doctor');
        alert("Doctor login successful!");
      } else {
        // Successful response, but missing token (unlikely but good to check)
        console.error("Server responded successfully but did not provide a token.", data);
        alert("Login failed: Authentication token is missing from the server response.");
      }

    } else {
      // Step 5: If login fails or credentials are invalid:

      // Try to get a specific error message from the server response body
      let errorMessage = "Login failed. Please check your email and password.";
      try {
        const errorData = await response.json();
        if (errorData.message) {
          errorMessage = errorData.message; // Use server-provided message
        }
      } catch (e) {
        // Ignore if response body isn't JSON or message isn't present
        console.warn("Could not parse specific error message from server response.");
      }

      // Show an alert with an error message
      alert(`Login Failed: ${errorMessage}`);
      console.error(`Login failed with status ${response.status}:`, response);
    }

  } catch (error) {
    // Step 6 (cont.): Show a generic error message if something goes wrong
    console.error("Network or server error during admin login:", error);
    alert("A network error occurred. Please try again later or contact support.");
  }
};

/*
  Import the openModal function to handle showing login popups/modals
  Import the base API URL from the config file
  Define constants for the admin and doctor login API endpoints using the base URL

  Use the window.onload event to ensure DOM elements are available after page load
  Inside this function:
    - Select the "adminLogin" and "doctorLogin" buttons using getElementById
    - If the admin login button exists:
        - Add a click event listener that calls openModal('adminLogin') to show the admin login modal
    - If the doctor login button exists:
        - Add a click event listener that calls openModal('doctorLogin') to show the doctor login modal


  Define a function named adminLoginHandler on the global window object
  This function will be triggered when the admin submits their login credentials

  Step 1: Get the entered username and password from the input fields
  Step 2: Create an admin object with these credentials

  Step 3: Use fetch() to send a POST request to the ADMIN_API endpoint
    - Set method to POST
    - Add headers with 'Content-Type: application/json'
    - Convert the admin object to JSON and send in the body

  Step 4: If the response is successful:
    - Parse the JSON response to get the token
    - Store the token in localStorage
    - Call selectRole('admin') to proceed with admin-specific behavior

  Step 5: If login fails or credentials are invalid:
    - Show an alert with an error message

  Step 6: Wrap everything in a try-catch to handle network or server errors
    - Show a generic error message if something goes wrong


  Define a function named doctorLoginHandler on the global window object
  This function will be triggered when a doctor submits their login credentials

  Step 1: Get the entered email and password from the input fields
  Step 2: Create a doctor object with these credentials

  Step 3: Use fetch() to send a POST request to the DOCTOR_API endpoint
    - Include headers and request body similar to admin login

  Step 4: If login is successful:
    - Parse the JSON response to get the token
    - Store the token in localStorage
    - Call selectRole('doctor') to proceed with doctor-specific behavior

  Step 5: If login fails:
    - Show an alert for invalid credentials

  Step 6: Wrap in a try-catch block to handle errors gracefully
    - Log the error to the console
    - Show a generic error message
*/
