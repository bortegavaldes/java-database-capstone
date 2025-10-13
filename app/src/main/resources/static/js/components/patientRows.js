// patientRows.js
export function createPatientRow(appointment) {
  const tr = document.createElement("tr");
  console.log("CreatePatientRow :: ", appointment.doctorId)
  tr.innerHTML = `
      <td class="patient-id">${appointment.patientId}</td>
      <td>${appointment.patientName}</td>
      <td>${appointment.patientPhone}</td>
      <td>${appointment.patientEmail}</td>
      <td><img src="../assets/images/addPrescriptionIcon/addPrescription.png" alt="addPrescriptionIcon" class="prescription-btn" data-id="${appointment.patientId}"></img></td>
    `;

  // Attach event listeners
  tr.querySelector(".patient-id").addEventListener("click", () => {
    window.location.href = `/pages/patientRecord.html?id=${appointment.patientId}&doctorId=${appointment.doctorId}`;
  });

  tr.querySelector(".prescription-btn").addEventListener("click", () => {
    window.location.href = `/pages/addPrescription.html?appointmentId=${appointment.id}&patientName=${appointment.patientName}`;
  });

  return tr;
}
