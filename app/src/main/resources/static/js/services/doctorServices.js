import { API_BASE_URL } from "../config/config.js";

const DOCTOR_API = API_BASE_URL + '/doctor';

/**
 * Fetches the list of all doctors from the DOCTOR_API endpoint.
 * @returns {Promise<Array>} A promise that resolves to an array of doctor objects, 
 * or an empty array if an error occurs.
 */
export async function getDoctors() {
    try {
        // 1. Use fetch() to send a GET request to the DOCTOR_API endpoint
        const response = await fetch(DOCTOR_API);

        // Check if the request was successful (status code 200-299)
        if (!response.ok) {
            // Throw an error if the HTTP status is not successful
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        // 2. Convert the response to JSON
        const data = await response.json();

        // 3. Return the 'doctors' array from the response
        // Assuming the API response structure is like: { success: true, doctors: [...] }
        return data.doctors || []; // Use '[]' as a safeguard if 'doctors' is missing

    } catch (error) {
        // 4. If there's an error (e.g., network issue, JSON parsing, HTTP error), log it and return an empty array
        console.error("Failed to fetch doctors:", error);
        return [];
    }
}

// Example of how to call the function:
/*
(async () => {
    const doctorsList = await getDoctors();
    console.log("Fetched Doctors:", doctorsList);
})();
*/

/**
 * Sends an authenticated DELETE request to remove a specific doctor.
 * * @param {string} id - The unique ID of the doctor to be deleted.
 * @param {string} token - The authentication token required for authorization (e.g., JWT).
 * @returns {Promise<{success: boolean, message: string}>} An object indicating the operation result.
 */
export async function deleteDoctor(id, token) {
    // 1. Construct the full endpoint URL using the DOCTOR_API constant and the doctor's ID
    const fullEndpoint = `${DOCTOR_API}/${id}`;

    console.log(`Attempting to DELETE doctor ID: ${id} at ${fullEndpoint}`);

    try {
        // 2. Send a DELETE request with the authentication token in the Authorization header
        const response = await fetch(fullEndpoint, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`, // Common format for authenticated requests
                'Content-Type': 'application/json'
            }
        });

        // 3. Check if the request was successful (HTTP status 200-299)
        if (!response.ok) {
            // If the server responded with an error status (e.g., 404 Not Found, 403 Forbidden)
            const errorData = await response.json().catch(() => ({})); // Try to parse JSON error body
            const status = response.status;
            
            // Return a clear failure message
            return {
                success: false,
                message: `Deletion failed. Server responded with status ${status}. Details: ${errorData.message || 'No detailed message provided.'}`
            };
        }

        // 4. Parse the JSON response and return a success status and message
        const data = await response.json();

        return {
            success: true,
            message: data.message || `Doctor with ID ${id} successfully deleted.`
        };

    } catch (error) {
        // 5. Catch and handle any network or unknown errors
        console.error(`Network or critical error during doctor deletion for ID ${id}:`, error);
        
        return {
            success: false,
            message: `A critical error occurred: Could not connect to API or process request. Check network connection.`
        };
    }
}

/**
 * Sends an authenticated POST request to save (add) a new doctor record.
 * This powers the 'Add Doctor' feature in the Admin dashboard.
 * @param {object} doctor - The object containing the new doctor's details (name, email, etc.).
 * @param {string} token - The authentication token required for authorization.
 * @returns {Promise<{success: boolean, message: string, doctorId: (string|undefined)}>} An object indicating the operation result.
 */
export async function saveDoctor(doctor, token) {
    console.log(`Attempting to POST new doctor record to ${DOCTOR_API}`);
    // 

    try {
        // Send a POST request with JSON data in the body and Authorization header
        const response = await fetch(DOCTOR_API, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(doctor) // Convert the doctor object to a JSON string
        });

        // Check if the request was successful (HTTP status 200-201)
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            const status = response.status;
            
            // Return failure message with details from the server if available
            return {
                success: false,
                message: `Creation failed. Server responded with status ${status}. Details: ${errorData.message || 'No detailed message provided.'}`
            };
        }

        const data = await response.json();
        
        // Assuming the API returns the ID of the newly created doctor upon success
        return {
            success: true,
            message: data.message || `Doctor successfully added with ID: ${data.id || 'N/A'}.` //,
            //doctorId: data.id // Include the new ID for client-side routing/updates
        };

    } catch (error) {
        // Catch and handle any network or critical errors
        console.error("Network or critical error during doctor creation:", error);
        
        return {
            success: false,
            message: `A critical error occurred: Could not connect to API or process request. Check network connection.`
        };
    }
}

/**
 * Sends a GET request to retrieve a filtered list of doctors based on provided criteria.
 * Filters are appended as URL query parameters.
 * @param {string | null | undefined} name - Partial or full name of the doctor.
 * @param {string | null | undefined} time - Desired appointment time or availability slot.
 * @param {string | null | undefined} specialty - The doctor's specialization (e.g., 'Pediatrics').
 * @returns {Promise<Array>} A promise that resolves to an array of matching doctor objects, or an empty array if an error occurs.
 */

export async function filterDoctors(name, time, specialty) {
    // Use URLSearchParams to easily construct the query string from non-empty parameters
    const params = new URLSearchParams();

    if (name) {
        params.append('name', name);
    }
    if (time) {
        params.append('time', time);
    }
    if (specialty) {
        params.append('specialty', specialty);
    }

    const queryString = params.toString();
    const fullEndpoint = queryString ? `${DOCTOR_API}?${queryString}` : DOCTOR_API;

    console.log(`Attempting to GET filtered doctors from: ${fullEndpoint}`);
    // 

    try {
        // Since this is a public search/filter, no authorization token is usually needed
        const response = await fetch(fullEndpoint, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            // Handle server errors (e.g., 500) or bad request errors (e.g., 400)
            const errorData = await response.json().catch(() => ({}));
            console.error(`Filtering failed. Server responded with status ${response.status}. Details: ${errorData.message || 'No detailed message provided.'}`);
            // Alert user of the failure gracefully (in a real app, this would update the UI)
            // Since we can't use alert(), we log the critical message.
            
            // Return an empty array on failure
            return [];
        }

        // Assuming the response body contains an object with a 'doctors' array
        const data = await response.json();
        
        // Return the filtered list, defaulting to an empty array if 'doctors' is missing
        return data.doctors || []; 

    } catch (error) {
        // Handle network errors (e.g., disconnection)
        console.error("Network or critical error during doctor filtering:", error);
        // In a real app, you would show an error message in the UI:
        // 'Could not connect to the service. Please check your internet connection.'
        return [];
    }
}

/*
  Import the base API URL from the config file
  Define a constant DOCTOR_API to hold the full endpoint for doctor-related actions


  Function: getDoctors
  Purpose: Fetch the list of all doctors from the API

   Use fetch() to send a GET request to the DOCTOR_API endpoint
   Convert the response to JSON
   Return the 'doctors' array from the response
   If there's an error (e.g., network issue), log it and return an empty array


  Function: deleteDoctor
  Purpose: Delete a specific doctor using their ID and an authentication token

   Use fetch() with the DELETE method
    - The URL includes the doctor ID and token as path parameters
   Convert the response to JSON
   Return an object with:
    - success: true if deletion was successful
    - message: message from the server
   If an error occurs, log it and return a default failure response


  Function: saveDoctor
  Purpose: Save (create) a new doctor using a POST request

   Use fetch() with the POST method
    - URL includes the token in the path
    - Set headers to specify JSON content type
    - Convert the doctor object to JSON in the request body

   Parse the JSON response and return:
    - success: whether the request succeeded
    - message: from the server

   Catch and log errors
    - Return a failure response if an error occurs


  Function: filterDoctors
  Purpose: Fetch doctors based on filtering criteria (name, time, and specialty)

   Use fetch() with the GET method
    - Include the name, time, and specialty as URL path parameters
   Check if the response is OK
    - If yes, parse and return the doctor data
    - If no, log the error and return an object with an empty 'doctors' array

   Catch any other errors, alert the user, and return a default empty result
*/
