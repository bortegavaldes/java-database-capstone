// render.js
//const module = await import("./components/modals");


// Function to load the module asynchronously
async function loadModuleFunctions() {
  try {
    // Dynamic import() returns a Promise
    const module = await import("../js/components/modals.js");

    // EXPOSE FUNCTIONS GLOBALLY:
    // Attach the exported functions to the window object.
    // This is the key step that makes them available to inline onclick.
    window.openModal = module.openModal;

    console.log("Module functions loaded and ready!");

  } catch (error) {
    console.error("Error loading module:", error);
  }
}

// Start the loading process as soon as the script executes
loadModuleFunctions();

function selectRole(role) {
  setRole(role);
  const token = localStorage.getItem('token');
  if (role === "admin") {
    console.log("Selected role admin");
    if (token) {
      window.location.href = `/adminDashboard/${token}`;
    } else {
      console.log("No token for selected admin");
      openModal('adminLogin');
    }
  } 
  if (role === "patient") {
    window.location.href = "/pages/patientDashboard.html";
  } else if (role === "doctor") {
    if (token) {
      window.location.href = `/doctorDashboard/${token}`;
    }else{
      openModal('doctorLogin');
    }
  } else if (role === "loggedPatient") {
    window.location.href = "loggedPatientDashboard.html";
  }
}


function renderContent() {
  const role = getRole();
  if (!role) {
    window.location.href = "/"; // if no role, send to role selection page
    return;
  }
}
