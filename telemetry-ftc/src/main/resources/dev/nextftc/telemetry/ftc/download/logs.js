const summary = document.getElementById("summary");
const message = document.getElementById("message");
const table = document.getElementById("table");
const rows = document.getElementById("rows");

function formatSize(bytes) {
  if (bytes < 1024) return bytes + " B";
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
  return (bytes / (1024 * 1024)).toFixed(1) + " MB";
}

function cell(text, className) {
  const td = document.createElement("td");
  td.textContent = text;
  if (className) td.className = className;
  return td;
}

function nameCell(log) {
  const td = document.createElement("td");
  const opMode = document.createElement("span");
  opMode.className = "opmode";
  opMode.textContent = log.opMode;
  const file = document.createElement("span");
  file.className = "file";
  file.textContent = log.name;
  td.append(opMode, file);
  return td;
}

function actionCell(log) {
  const td = document.createElement("td");
  if (log.recording) {
    const badge = document.createElement("span");
    badge.className = "badge";
    badge.textContent = "Recording";
    td.appendChild(badge);
  } else {
    const link = document.createElement("a");
    link.className = "button";
    link.href = "/logs/" + encodeURIComponent(log.name);
    link.download = log.name;
    link.textContent = "Download";
    td.appendChild(link);
  }
  return td;
}

function render(logs) {
  rows.replaceChildren(...logs.map((log) => {
    const tr = document.createElement("tr");
    tr.append(
      nameCell(log),
      cell(log.startedText),
      cell(formatSize(log.sizeBytes), "num"),
      actionCell(log),
    );
    return tr;
  }));
  summary.textContent = logs.length + (logs.length === 1 ? " log" : " logs");
  table.hidden = logs.length === 0;
  message.hidden = logs.length !== 0;
  message.textContent = "No logs yet";
}

async function refresh() {
  try {
    const response = await fetch("/logs", { cache: "no-store" });
    if (!response.ok) throw new Error("HTTP " + response.status);
    render(await response.json());
  } catch (error) {
    summary.textContent = "Unable to load logs";
    table.hidden = true;
    message.hidden = false;
    message.textContent = String(error);
  }
}

refresh();
// So a log that was recording turns into a download once its OpMode stops
setInterval(refresh, 5000);
