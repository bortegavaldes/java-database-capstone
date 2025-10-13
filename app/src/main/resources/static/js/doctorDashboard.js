import { getAllAppointments } from "../js/services/appointmentRecordService.js";
import { createPatientRow } from "../js/components/patientRows.js";

const patientTable = document.getElementById("patientTableBody");
let selectedDate = new Date();
const token = localStorage.getItem("token");
let patientName = '0';

// Reference to the search bar
const searchBar = document.querySelector('#searchBar');

// Add input event listener
searchBar.addEventListener('input', (event) => {
  const inputValue = event.target.value.trim();

  // Update patientName or default to "null"
  patientName = inputValue === '' ? null : inputValue;

  // Refresh the appointment list with filtered data
  loadAppointments();
});

function formatDateForInput(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0'); // Months are 0-indexed
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

const todayButton = document.getElementById('todayButton');
const datePicker = document.getElementById('datePicker');
todayButton.addEventListener('click', () => {
  console.log("Today's button clicked. Resetting date.");

  // a. Resets the selectedDate to today.
  selectedDate =  new Date();

  // b. Updates the date picker field to reflect today’s date.
  datePicker.value = formatDateForInput(selectedDate);

  // c. Calls loadAppointments().
  loadAppointments();
});

datePicker.addEventListener('change', (event) => {
  console.log(`Date picker changed to: ${event.target.value}`);

  // a. Updates the selectedDate variable when changed.
  // Using a new Date() based on the input's YYYY-MM-DD value
  // Note: This often sets the time to midnight UTC. Adjust time zone if needed.
  selectedDate = new Date(event.target.value);

  // b. Calls loadAppointments() to fetch appointments for the selected date.
  loadAppointments();
});

async function loadAppointments() {

  // 1. Clears existing content in the table.
  patientTable.innerHTML = '';

  // Optional: Add a temporary loading indicator
  const loadingRow = document.createElement('tr');
  loadingRow.innerHTML = `<td colspan="4" class="text-center">Loading appointments...</td>`;
  patientTable.appendChild(loadingRow);

  try {
    // Use getAllAppointments(selectedDate, patientName, token) to fetch appointment data.
    const formatSelDate = getDateString(selectedDate);
    const appointments = await getAllAppointments(formatSelDate, patientName, token);

    // Clear the loading indicator
    patientTable.innerHTML = '';

    if (!appointments || appointments.length === 0) {
      // 2. If no appointments are found:
      // Displays a row with a “No Appointments found for today” message.
      const noDataRow = document.createElement('tr');
      // Use a colspan that matches the number of columns in your table
      noDataRow.innerHTML = `<td colspan="4" class="text-center">No Appointments found for this date.</td>`;
      patientTable.appendChild(noDataRow);

    } else {
      // 3. If appointments exist:
      appointments.forEach(appointment => {
        // For each appointment, extract the patient’s details. (Done inside createPatientRow)

        // Use createPatientRow() to create a <tr> for each.
        const row = createPatientRow(appointment);

        // Append each row to the appointment table body.
        patientTable.appendChild(row);
      });
    }

  } catch (error) {
    // 4. In case of error, display a fallback error message row in the table.
    console.error("Error fetching appointments:", error);

    // Clear any previous content (including the loading message)
    patientTable.innerHTML = '';

    const errorRow = document.createElement('tr');
    // Use a colspan that matches the number of columns in your table
    errorRow.innerHTML = `<td colspan="4" class="text-center text-danger">⚠️ Error loading appointments. Please try again.</td>`;
    patientTable.appendChild(errorRow);
  }
}

document.addEventListener('DOMContentLoaded', () => {
  console.log("DOM fully loaded and parsed. Starting initial render.");

  // 1. Call renderContent() (if used).
  // This function would typically handle rendering navigation, user profile info, etc.
  if (typeof renderContent === 'function') {
    renderContent();
  } else {
    console.warn("renderContent() function is not defined.");
  }

  // 2. Call loadAppointments() to load today’s appointments by default.
  // The loadAppointments function (from the previous request) assumes 'selectedDate' 
  // is already initialized to 'new Date()' (today).
  if (typeof loadAppointments === 'function') {
    loadAppointments();
  } else {
    console.error("loadAppointments() function is not defined. Cannot load default appointments.");
  }
});


/**
 * Creates a string representing today's date in 'YYYY-MM-DD' format.
 * @returns {string} The formatted date string.
 */
function getDateString(date) {

    // Get the full year (YYYY)
    const year = date.getFullYear();

    // Get the month (0-11). Add 1 for 1-12, then pad with a leading zero if needed.
    // .padStart(2, '0') ensures two digits (e.g., '01', '12').
    const month = String(date.getMonth() + 1).padStart(2, '0');

    // Get the day of the month (1-31) and pad with a leading zero if needed.
    const day = String(date.getDate()).padStart(2, '0');

    // Combine into the required format
    return `${year}-${month}-${day}`;
}

/*
  Import getAllAppointments to fetch appointments from the backend
  Import createPatientRow to generate a table row for each patient appointment


  Get the table body where patient rows will be added
  Initialize selectedDate with today's date in 'YYYY-MM-DD' format
  Get the saved token from localStorage (used for authenticated API calls)
  Initialize patientName to null (used for filtering by name)


  Add an 'input' event listener to the search bar
  On each keystroke:
    - Trim and check the input value
    - If not empty, use it as the patientName for filtering
    - Else, reset patientName to "null" (as expected by backend)
    - Reload the appointments list with the updated filter


  Add a click listener to the "Today" button
  When clicked:
    - Set selectedDate to today's date
    - Update the date picker UI to match
    - Reload the appointments for today


  Add a change event listener to the date picker
  When the date changes:
    - Update selectedDate with the new value
    - Reload the appointments for that specific date


  Function: loadAppointments
  Purpose: Fetch and display appointments based on selected date and optional patient name

  Step 1: Call getAllAppointments with selectedDate, patientName, and token
  Step 2: Clear the table body content before rendering new rows

  Step 3: If no appointments are returned:
    - Display a message row: "No Appointments found for today."

  Step 4: If appointments exist:
    - Loop through each appointment and construct a 'patient' object with id, name, phone, and email
    - Call createPatientRow to generate a table row for the appointment
    - Append each row to the table body

  Step 5: Catch and handle any errors during fetch:
    - Show a message row: "Error loading appointments. Try again later."


  When the page is fully loaded (DOMContentLoaded):
    - Call renderContent() (assumes it sets up the UI layout)
    - Call loadAppointments() to display today's appointments by default
*/
