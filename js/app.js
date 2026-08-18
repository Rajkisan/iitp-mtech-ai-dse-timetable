let currentFilter = "all";
let selectedElective = localStorage.getItem("iitp-ai-dse-elective") || "Computational Data Analysis";
let selectedReminderOffsets = JSON.parse(localStorage.getItem("iitp-ai-dse-reminders") || "[10]");
let selectedReminderCourseIds = JSON.parse(
  localStorage.getItem("iitp-ai-dse-reminder-courses") || JSON.stringify(COURSES.map(course => course.id))
);
let draftReminderOffsets = [...selectedReminderOffsets];
let draftReminderCourseIds = [...selectedReminderCourseIds];

const timetableEl = document.getElementById("timetable");
const courseGridEl = document.getElementById("courseGrid");
const todayTitleEl = document.getElementById("todayTitle");
const todayClassesEl = document.getElementById("todayClasses");
const electiveSelectEl = document.getElementById("electiveSelect");
const reminderPanelEl = document.getElementById("reminderPanel");
const reminderOptionsEl = document.getElementById("reminderOptions");
const reminderCourseOptionsEl = document.getElementById("reminderCourseOptions");
const reminderStatusEl = document.getElementById("reminderStatus");
const reminderConfigureBtnEl = document.getElementById("reminderConfigureBtn");
const reminderModalEl = document.getElementById("reminderModal");
const reminderCloseBtnEl = document.getElementById("reminderCloseBtn");
const reminderCancelBtnEl = document.getElementById("reminderCancelBtn");
const reminderSaveBtnEl = document.getElementById("reminderSaveBtn");
const reminderTestBtnEl = document.getElementById("reminderTestBtn");
const reminderTestAlarmBtnEl = document.getElementById("reminderTestAlarmBtn");

const REMINDER_OPTIONS = [5, 10, 15, 30, 45, 60];
const REMINDER_LOOKAHEAD_DAYS = 30;

electiveSelectEl.value = selectedElective;

function getCourse(id) {
  return COURSES.find(course => course.id === id);
}

function getToday() {
  return new Date().toLocaleDateString("en-US", { weekday: "long" });
}

function effectiveCourse(scheduleItem) {
  if (scheduleItem.course !== "elective-slot") return getCourse(scheduleItem.course);

  if (selectedElective === "Computational Data Analysis") return getCourse("cda");
  if (selectedElective === "Pattern Recognition") return getCourse("pr");
  return getCourse("aml");
}

// Attendance is only recorded when classes/recordings are accessed via Moodle,
// so every "join" action routes to the course's Moodle page.
function getMeetingUrl(course) {
  return course.moodleUrl;
}

function isAndroidApp() {
  return Boolean(window.AndroidReminders);
}

function reminderLabel(minutes) {
  return minutes === 60 ? "1 hr" : `${minutes} min`;
}

function normalizeReminderSettings() {
  selectedReminderOffsets = selectedReminderOffsets
    .map(Number)
    .filter(minutes => REMINDER_OPTIONS.includes(minutes));

  if (!Array.isArray(selectedReminderCourseIds) || !selectedReminderCourseIds.length) {
    selectedReminderCourseIds = COURSES.map(course => course.id);
  }

  selectedReminderCourseIds = selectedReminderCourseIds
    .filter(id => COURSES.some(course => course.id === id));
}

function parseStartTimeMinutes(timeRange) {
  const match = timeRange.match(/(\d{1,2}):(\d{2})\s*(AM|PM)/i);
  if (!match) return null;

  let hours = Number(match[1]);
  const minutes = Number(match[2]);
  const period = match[3].toUpperCase();

  if (period === "PM" && hours !== 12) hours += 12;
  if (period === "AM" && hours === 12) hours = 0;

  return hours * 60 + minutes;
}

function nextClassOccurrences() {
  const now = new Date();
  const events = [];

  for (let dayOffset = 0; dayOffset < REMINDER_LOOKAHEAD_DAYS; dayOffset += 1) {
    const date = new Date(now);
    date.setDate(now.getDate() + dayOffset);
    date.setSeconds(0, 0);

    const day = date.toLocaleDateString("en-US", { weekday: "long" });

    SCHEDULE
      .filter(item => item.day === day)
      .forEach(item => {
        const startMinutes = parseStartTimeMinutes(item.time);
        const course = effectiveCourse(item);
        if (startMinutes === null || !course || !selectedReminderCourseIds.includes(course.id)) return;

        const startsAt = new Date(date);
        startsAt.setHours(Math.floor(startMinutes / 60), startMinutes % 60, 0, 0);

        if (startsAt.getTime() <= now.getTime()) return;

        events.push({
          id: `${startsAt.toISOString()}-${course.id}-${item.time}`,
          courseId: course.id,
          title: course.shortName,
          text: `${item.time}${item.showLabTag ? " · Lab" : ""}`,
          startsAt: startsAt.getTime(),
          moodleUrl: course.moodleUrl || ""
        });
      });
  }

  return events;
}

function syncAndroidReminders() {
  if (!reminderStatusEl) return;
  normalizeReminderSettings();

  if (!isAndroidApp()) {
    reminderStatusEl.textContent = "Reminder scheduling is available in the Android app.";
    return;
  }

  const payload = {
    offsets: selectedReminderOffsets,
    events: nextClassOccurrences()
  };

  try {
    const scheduledCount = window.AndroidReminders.configure(JSON.stringify(payload));
    const count = Number(scheduledCount) || 0;
    reminderStatusEl.textContent = selectedReminderOffsets.length && selectedReminderCourseIds.length
      ? `Saved ${selectedReminderOffsets.length} reminder time${selectedReminderOffsets.length === 1 ? "" : "s"} for ${selectedReminderCourseIds.length} course${selectedReminderCourseIds.length === 1 ? "" : "s"}. ${count} upcoming notifications scheduled.`
      : "Class reminders are off.";
  } catch (error) {
    reminderStatusEl.textContent = "Could not update Android reminders.";
  }
}

function renderReminderSettings() {
  if (!reminderPanelEl || !reminderOptionsEl) return;
  normalizeReminderSettings();

  if (isAndroidApp()) {
    reminderPanelEl.classList.add("android-reminders");
  }

  draftReminderOffsets = [...selectedReminderOffsets];
  draftReminderCourseIds = [...selectedReminderCourseIds];
  renderReminderModalOptions();
  syncAndroidReminders();
}

function renderReminderModalOptions() {
  if (!reminderOptionsEl || !reminderCourseOptionsEl) return;

  reminderOptionsEl.innerHTML = REMINDER_OPTIONS.map(minutes => `
    <label class="reminder-option">
      <input type="checkbox" value="${minutes}" ${draftReminderOffsets.includes(minutes) ? "checked" : ""} />
      <span>${reminderLabel(minutes)}</span>
    </label>
  `).join("");

  reminderCourseOptionsEl.innerHTML = COURSES.map(course => `
    <label class="course-reminder-option">
      <span>
        <strong>${course.shortName}</strong>
        <small>${course.code}</small>
      </span>
      <input type="checkbox" value="${course.id}" ${draftReminderCourseIds.includes(course.id) ? "checked" : ""} />
    </label>
  `).join("");

  reminderOptionsEl.querySelectorAll("input").forEach(input => {
    input.addEventListener("change", () => {
      draftReminderOffsets = [...reminderOptionsEl.querySelectorAll("input:checked")]
        .map(item => Number(item.value));
    });
  });

  reminderCourseOptionsEl.querySelectorAll("input").forEach(input => {
    input.addEventListener("change", () => {
      draftReminderCourseIds = [...reminderCourseOptionsEl.querySelectorAll("input:checked")]
        .map(item => item.value);
    });
  });
}

function openReminderModal() {
  draftReminderOffsets = [...selectedReminderOffsets];
  draftReminderCourseIds = [...selectedReminderCourseIds];
  renderReminderModalOptions();
  reminderModalEl.classList.add("open");
  reminderModalEl.setAttribute("aria-hidden", "false");
}

function closeReminderModal() {
  reminderModalEl.classList.remove("open");
  reminderModalEl.setAttribute("aria-hidden", "true");
}

function saveReminderSettings() {
  selectedReminderOffsets = [...draftReminderOffsets].sort((a, b) => a - b);
  selectedReminderCourseIds = [...draftReminderCourseIds];

  localStorage.setItem("iitp-ai-dse-reminders", JSON.stringify(selectedReminderOffsets));
  localStorage.setItem("iitp-ai-dse-reminder-courses", JSON.stringify(selectedReminderCourseIds));

  syncAndroidReminders();
  closeReminderModal();
}

if (reminderConfigureBtnEl) reminderConfigureBtnEl.addEventListener("click", openReminderModal);
if (reminderCloseBtnEl) reminderCloseBtnEl.addEventListener("click", closeReminderModal);
if (reminderCancelBtnEl) reminderCancelBtnEl.addEventListener("click", closeReminderModal);
if (reminderSaveBtnEl) reminderSaveBtnEl.addEventListener("click", saveReminderSettings);
if (reminderModalEl) {
  reminderModalEl.addEventListener("click", event => {
    if (event.target === reminderModalEl) closeReminderModal();
  });
}

if (reminderTestBtnEl) {
  reminderTestBtnEl.addEventListener("click", () => {
    if (!isAndroidApp()) return;

    try {
      const sent = Number(window.AndroidReminders.test());
      reminderStatusEl.textContent = sent
        ? "Test notification sent."
        : "Could not send test notification. Allow notifications for this app.";
    } catch (error) {
      reminderStatusEl.textContent = "Could not send test notification.";
    }
  });
}

if (reminderTestAlarmBtnEl) {
  reminderTestAlarmBtnEl.addEventListener("click", () => {
    if (!isAndroidApp()) return;

    try {
      const scheduled = Number(window.AndroidReminders.testInOneMinute());
      reminderStatusEl.textContent = scheduled
        ? "Test notification scheduled for 1 minute from now. Minimize the app to test background delivery."
        : "Could not schedule test notification. Check notification or alarm permission for this app.";
    } catch (error) {
      reminderStatusEl.textContent = "Could not schedule test notification.";
    }
  });
}

function renderCourseLinks(course) {
  if (!course.moodleUrl) return "";

  return `<a class="join-btn" href="${course.moodleUrl}">Join on Moodle</a>`;
}

function renderImportantLinks() {
  const grid = document.getElementById("importantLinksGrid");
  if (!grid) return;

  grid.innerHTML = IMPORTANT_LINKS.map(link => `
    <a class="important-link-card" href="${link.url}">
      <span class="important-link-icon" aria-hidden="true">${link.icon}</span>
      <span class="important-link-label">${link.label}</span>
    </a>
  `).join("");
}

function renderTimetable() {
  timetableEl.innerHTML = "";

  const timeHeader = document.createElement("div");
  timeHeader.className = "time-head";
  timeHeader.textContent = "TIME";
  timetableEl.appendChild(timeHeader);

  const today = getToday();

  DAYS.forEach(day => {
    const header = document.createElement("div");
    header.className = `grid-head ${day === today ? "today" : ""}`;
    header.textContent = day;
    timetableEl.appendChild(header);
  });

  TIMES.forEach(time => {
    const timeEl = document.createElement("div");
    timeEl.className = "time-head";
    timeEl.textContent = time;
    timetableEl.appendChild(timeEl);

    DAYS.forEach(day => {
      const slot = document.createElement("div");
    
      // Friday is a full-day leave.
      if (day === "Friday") {
        slot.className = "slot leave";
        slot.textContent = "FULL DAY LEAVE";
        timetableEl.appendChild(slot);
        return;
      }
    
      const item = SCHEDULE.find(s => s.day === day && s.time === time);
    
      if (!item) {
        slot.className = "slot empty";
        slot.textContent = "—";
        timetableEl.appendChild(slot);
        return;
      }

      const course = effectiveCourse(item);
      slot.className = `slot ${course.type} ${item.showLabTag ? "lab" : ""}`;

      if (course.type === "elective" && course.name !== selectedElective) {
        slot.classList.add("dimmed");
      }

      if (currentFilter !== "all" && course.type !== currentFilter) {
        slot.classList.add("dimmed");
      }

      if (course.type === "elective" && course.name === selectedElective) {
        slot.classList.add("selected-elective");
      }

      slot.innerHTML = `
        <div class="slot-title">${course.shortName}</div>
        <div class="slot-code">${course.code}</div>
        <div class="slot-time">${item.time}</div>
        ${item.showLabTag ? '<span class="badge">Lab</span>' : ''}
      `;

      slot.addEventListener("click", () => {
        const url = getMeetingUrl(course);
        if (url) window.location.href = url;
      });

      timetableEl.appendChild(slot);
    });
  });
}

function renderCourses() {
  courseGridEl.innerHTML = "";

  COURSES.forEach(course => {
    const card = document.createElement("article");
    card.className = `course-card ${course.type}`;

    card.innerHTML = `
      <div class="course-type">${course.type === "regular" ? "Regular Course" : "Elective Course"}</div>
      <h3>${course.name}</h3>
      <div class="course-code">${course.code}</div>
      <div class="link-row">
        ${renderCourseLinks(course)}
      </div>
    `;

    courseGridEl.appendChild(card);
  });
}

function renderToday() {
  const today = getToday();
  todayTitleEl.textContent = today;

  // Friday is a full-day leave.
  if (today === "Friday") {
    todayClassesEl.innerHTML = `
      <div class="today-item">
        <strong>Full Day Leave</strong>
        <span>No classes scheduled</span>
      </div>
    `;
    return;
  }

  const todaySchedule = SCHEDULE
    .filter(item => item.day === today)
    .map(item => ({ item, course: effectiveCourse(item) }));

  if (!todaySchedule.length) {
    todayClassesEl.innerHTML = `<div class="muted">No classes scheduled today.</div>`;
    return;
  }

  todayClassesEl.innerHTML = todaySchedule.map(({ item, course }) => `
    <a class="today-item today-link" href="${getMeetingUrl(course)}">
      <strong>${item.time}</strong>
      <span>${course.shortName}${item.showLabTag ? " · Lab" : ""}</span>
    </a>
  `).join("");
}

function renderAll() {
  renderImportantLinks();
  renderTimetable();
  renderCourses();
  renderToday();
  renderReminderSettings();
}

document.querySelectorAll("[data-filter]").forEach(button => {
  button.addEventListener("click", () => {
    currentFilter = button.dataset.filter;
    document.querySelectorAll("[data-filter]").forEach(btn => btn.classList.remove("active"));
    button.classList.add("active");
    renderTimetable();
  });
});

electiveSelectEl.addEventListener("change", event => {
  selectedElective = event.target.value;
  localStorage.setItem("iitp-ai-dse-elective", selectedElective);
  renderAll();
});

document.getElementById("todayBtn").addEventListener("click", () => {
  const today = getToday();
  const header = [...document.querySelectorAll(".grid-head")].find(el => el.textContent === today);
  if (header) header.scrollIntoView({ behavior: "smooth", inline: "center", block: "start" });
});

document.querySelector('[data-filter="all"]').classList.add("active");

renderAll();
