import { openModal } from "../js/components/modals.js";
import { getDoctors, filterDoctors, saveDoctor } from "../js/services/doctorServices.js";
import { createDoctorCard } from "../js/components/doctorCard.js";

document.getElementById('addDocBtn').addEventListener('click', () => {
  openModal('addDoctor');
});

/**
 * Fetches all doctors, clears the content, and displays the results as cards.
 */
async function loadDoctorCards() {
  console.log("Executing loadDoctorCards...");
  const contentDiv = document.getElementById("content");

  // 1. Clear existing content and show loading state
  contentDiv.innerHTML = `
                <div class="col-span-full flex items-center justify-center p-12 text-gray-500">
                    <svg class="animate-spin -ml-1 mr-3 h-5 w-5 text-indigo-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    Fetching doctor list...
                </div>
            `;

  // 2. Fetch doctors
  const doctors = await getDoctors();

  // 3. Render results
  renderDoctorCards(doctors);
}

document.addEventListener('DOMContentLoaded', loadDoctorCards);

document.getElementById("search-bar").addEventListener("input", filterDoctorsOnChange);
document.getElementById("sort-time").addEventListener("change", filterDoctorsOnChange);
document.getElementById("filter-specialty").addEventListener("change", filterDoctorsOnChange);

/**
         * Renders the fetched doctor cards to the DOM, handling loading and empty states.
         * @param {Array} doctors - Array of doctor objects to display.
         */
function renderDoctorCards(doctors) {
  const contentDiv = document.getElementById("content");
  // Clear existing content
  contentDiv.innerHTML = "";

  if (doctors.length === 0) {
    contentDiv.innerHTML = `
                    <div class="col-span-full text-center p-12 bg-white rounded-xl shadow-md">
                        <p class="text-xl font-semibold text-red-500">No doctors found matching your criteria.</p>
                        <p class="mt-2 text-gray-500">Try adjusting your filters.</p>
                    </div>
                `;
    return;
  }

  // Inject the cards
  doctors.forEach(doctor => {
    const card = createDoctorCard(doctor);
    contentDiv.appendChild(card);
  });
}

/**
         * Gathers filter inputs and fetches/renders matching doctor records.
         */
async function filterDoctorsOnChange() {
  log("Executing filterDoctorsOnChange...");
  const contentDiv = document.getElementById("content");

  // 1. Gather current filter/search values
  const name = document.getElementById('filterName').value.trim();
  const specialty = document.getElementById('filter-specialty').value.trim();
  const time = document.getElementById('sort-time').value.trim();

  // Show temporary loading state
  contentDiv.innerHTML = `
                <div class="col-span-full flex items-center justify-center p-12 text-gray-500">
                    <svg class="animate-spin -ml-1 mr-3 h-5 w-5 text-indigo-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    Searching for matching doctors...
                </div>
            `;

  // 2. Fetch filtered results
  const doctors = await filterDoctors(name, time, specialty);

  // 3. Render the results (handles 'No doctors found')
  renderDoctorCards(doctors);
  log(`Search completed. Displaying ${doctors.length} results.`);
}

/**
         * Collects form data, authenticates, and attempts to save a new doctor.
         */
async function adminAddDoctor() {
  const submitBtn = document.getElementById('addDoctorSubmitBtn');
  submitBtn.disabled = true;
  displayModalStatus("Processing request...", "default");

  // 1. Collect input values from the modal form
  const name = document.getElementById('doctorName').value.trim();
  const email = document.getElementById('doctorEmail').value.trim();
  const phone = document.getElementById('doctorPhone').value.trim();
  const password = document.getElementById('doctorPassword').value;
  const specialty = document.getElementById('doctorSpecialty').value.trim();
  const availabilityString = document.getElementById('doctorAvailability').value.trim();

  // Convert availability string to array
  const availability = availabilityString ? availabilityString.split(',').map(item => item.trim()).filter(item => item) : [];

  // 2. Retrieve the authentication token from localStorage (Mocking retrieval)
  // In a real app, the admin would have stored this after login.
  const token = localStorage.getItem('adminToken') || 'MOCK_ADMIN_TOKEN_123';

  // If no token is found, show an alert and stop execution
  if (!token || token === 'MOCK_ADMIN_TOKEN_123') {
    displayModalStatus("Authentication token missing. Please log in as Admin.", "error");
    submitBtn.disabled = false;
    log("EXECUTION STOPPED: Missing Admin token.");
    return;
  }

  // 3. Build a doctor object with the form values
  const doctorData = {
    name,
    email,
    phone: phone || undefined, // Send as undefined if empty
    password,
    specialty,
    availability
  };

  // 4. Call saveDoctor(doctor, token) from the service
  const result = await saveDoctor(doctorData, token);

  // 5. Handle result
  if (result.success) {
    // Show a success message
    displayModalStatus(result.message, "success");
    log(`Successfully added new doctor: ${doctorData.name}`);

    // Close the modal and reload the doctor list
    setTimeout(() => {
      toggleAddDoctorModal(false);
      loadDoctorCards(); // Reload the list to show the new doctor
    }, 1500);
  } else {
    // If saving fails, show an error message
    displayModalStatus(`Failed to add doctor: ${result.message}`, "error");
    log(`FAILURE: ${result.message}`);
  }

  submitBtn.disabled = false;
}


/*
  This script handles the admin dashboard functionality for managing doctors:
  - Loads all doctor cards
  - Filters doctors by name, time, or specialty
  - Adds a new doctor via modal form


  Attach a click listener to the "Add Doctor" button
  When clicked, it opens a modal form using openModal('addDoctor')


  When the DOM is fully loaded:
    - Call loadDoctorCards() to fetch and display all doctors


  Function: loadDoctorCards
  Purpose: Fetch all doctors and display them as cards

    Call getDoctors() from the service layer
    Clear the current content area
    For each doctor returned:
    - Create a doctor card using createDoctorCard()
    - Append it to the content div

    Handle any fetch errors by logging them


  Attach 'input' and 'change' event listeners to the search bar and filter dropdowns
  On any input change, call filterDoctorsOnChange()


  Function: filterDoctorsOnChange
  Purpose: Filter doctors based on name, available time, and specialty

    Read values from the search bar and filters
    Normalize empty values to null
    Call filterDoctors(name, time, specialty) from the service

    If doctors are found:
    - Render them using createDoctorCard()
    If no doctors match the filter:
    - Show a message: "No doctors found with the given filters."

    Catch and display any errors with an alert


  Function: renderDoctorCards
  Purpose: A helper function to render a list of doctors passed to it

    Clear the content area
    Loop through the doctors and append each card to the content area


  Function: adminAddDoctor
  Purpose: Collect form data and add a new doctor to the system

    Collect input values from the modal form
    - Includes name, email, phone, password, specialty, and available times

    Retrieve the authentication token from localStorage
    - If no token is found, show an alert and stop execution

    Build a doctor object with the form values

    Call saveDoctor(doctor, token) from the service

    If save is successful:
    - Show a success message
    - Close the modal and reload the page

    If saving fails, show an error message
*/
