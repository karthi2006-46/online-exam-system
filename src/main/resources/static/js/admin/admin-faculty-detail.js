/* =====================================================
   ADMIN - FACULTY DETAIL
===================================================== */
(function () {
    "use strict";

    var UI = window.AdminUI;

    if (!UI.guard()) return;

    var icons = UI.icons;
    var facultyId = Number(new URLSearchParams(window.location.search).get("id"));

    if (!facultyId || facultyId < 1) {
        window.location.replace("/admin/faculty.html");
        return;
    }

    var profile = null;
    var courseCache = null;

    /* ---------------- modal helpers ---------------- */

    function openModal(id) {
        var node = UI.el(id);
        if (!node) return;
        node.classList.add("is-open");
        node.setAttribute("aria-hidden", "false");
        document.body.classList.add("as-lock");
    }

    function closeModal(id) {
        var node = UI.el(id);
        if (!node) return;
        node.classList.remove("is-open");
        node.setAttribute("aria-hidden", "true");
        if (!document.querySelector(".modal-overlay.is-open")) {
            document.body.classList.remove("as-lock");
        }
    }

    document.querySelectorAll("[data-close]").forEach(function (button) {
        button.addEventListener("click", function () { closeModal(button.dataset.close); });
    });

    document.querySelectorAll(".modal-overlay").forEach(function (overlay) {
        overlay.addEventListener("click", function (event) {
            if (event.target === overlay) closeModal(overlay.id);
        });
    });

    document.addEventListener("keydown", function (event) {
        if (event.key !== "Escape") return;
        var open = document.querySelector(".modal-overlay.is-open");
        if (open) closeModal(open.id);
    });

    /* ---------------- icons ---------------- */

    var kpiIcons = {
        kpiCourses: icons.courses,
        kpiStudents: icons.students,
        kpiActive: icons.check,
        kpiResults: icons.performance,
        kpiPass: icons.shield
    };

    Object.keys(kpiIcons).forEach(function (id) {
        var node = UI.el(id);
        if (node) node.innerHTML = kpiIcons[id];
    });

    /* ---------------- render ---------------- */

    function setText(id, value) {
        var node = UI.el(id);
        if (node) node.textContent = value;
    }

    function renderHeader(data) {
        var account = data.profile || {};
        var name = UI.joinName(account.firstName, account.lastName) || "Faculty";

        document.title = name + " | LearnHub LMS";

        setText("pageTitle", name);
        setText("profileName", name);
        setText("profileAvatar", UI.initials(name));
        setText("profileEmail", account.email || "-");
        setText("profilePhone", account.phone || "-");
        setText("profileJoined", UI.date(account.createdAt));
        setText("profileId", String(account.id || facultyId));

        UI.el("profileStatus").innerHTML = UI.statusPill(account.active);

        setText("infoEmail", account.email || "-");
        setText("infoPhone", account.phone || "-");
        setText("infoAddress", account.address || "-");
        setText("infoCreated", UI.dateTime(account.createdAt));
        setText("infoUpdated", UI.dateTime(account.updatedAt));

        UI.el("toggleStatusBtn").textContent =
            account.active ? "Deactivate" : "Activate";

        ["profileHeader", "profileStats", "profilePanels", "accountPanel"]
            .forEach(function (id) {
                var node = UI.el(id);
                if (node) node.hidden = false;
            });
    }

    function renderStats(data) {
        var dashboard = data.dashboard || {};

        setText("kpiCourseCount", UI.number(dashboard.totalCourses));
        setText("kpiStudentCount", UI.number(dashboard.totalStudents));
        setText("kpiActiveCount", UI.number(dashboard.activeStudents));
        setText("kpiResultCount", UI.number(dashboard.totalResults));
        setText("kpiPassRate", UI.percent(dashboard.passPercentage));

        setText("examPending", UI.number(data.pendingExamApprovals));
        setText("examApproved", UI.number(data.approvedExamRequests));
        setText("examRejected", UI.number(data.rejectedExamRequests));

        setText("extensionPending", UI.number(data.pendingExtensionRequests));
        setText("extensionApproved", UI.number(data.approvedExtensionRequests));
        setText("extensionRejected", UI.number(data.rejectedExtensionRequests));

        setText("codingPending", UI.number(data.pendingCodingSubmissions));
        setText("codingApproved", UI.number(data.approvedCodingSubmissions));
        setText("codingRejected", UI.number(data.rejectedCodingSubmissions));
    }

    function renderCourses(courses) {
        var container = UI.el("courseList");

        if (!courses || !courses.length) {
            container.innerHTML = '<p class="subtle">No courses are assigned to this faculty member yet.</p>';
            return;
        }

        container.innerHTML = courses.map(function (course) {
            return '<article class="course-card">' +
                '<div class="course-card-head">' +
                    "<div><h3>" + UI.escapeHtml(course.title) + "</h3>" +
                    "<p>" + UI.escapeHtml(course.courseCode || "-") + "</p></div>" +
                    (course.active
                        ? '<span class="pill pill-green">Active</span>'
                        : '<span class="pill pill-red">Inactive</span>') +
                "</div>" +
                '<div class="course-card-grid">' +
                    '<div class="mini-stat"><span>Students</span><strong>' +
                        UI.number(course.totalStudents) + "</strong></div>" +
                    '<div class="mini-stat"><span>Active</span><strong>' +
                        UI.number(course.activeStudents) + "</strong></div>" +
                    '<div class="mini-stat"><span>Exams</span><strong>' +
                        UI.number(course.totalExams) + "</strong></div>" +
                    '<div class="mini-stat"><span>Materials</span><strong>' +
                        UI.number(course.totalMaterials) + "</strong></div>" +
                    '<div class="mini-stat"><span>Challenges</span><strong>' +
                        UI.number(course.totalChallenges) + "</strong></div>" +
                    '<div class="mini-stat"><span>Avg score</span><strong>' +
                        UI.percent(course.averagePercentage) + "</strong></div>" +
                "</div>" +
                "</article>";
        }).join("");
    }

    function pageError(error) {
        var message = UI.el("pageMessage");
        message.className = "page-message is-error";
        message.textContent = error.message || "Unable to load this faculty profile.";
        UI.toast(error.message, "error");
    }

    /* ---------------- load ---------------- */

    async function load() {

        var message = UI.el("pageMessage");
        message.className = "page-message";
        message.textContent = "Loading faculty profile...";

        try {
            var data = await UI.request("/admin/faculty/" + facultyId);

            profile = data.profile || {};

            renderHeader(data);
            renderStats(data);
            renderCourses(data.courses || []);

            message.textContent = "";
            message.className = "page-message";

        } catch (error) {
            pageError(error);
        }
    }

    /* ---------------- assign courses ---------------- */

    async function loadCourses() {
        if (courseCache) return courseCache;
        var response = await UI.request("/courses");
        courseCache = (response && response.data) ? response.data : (response || []);
        return courseCache;
    }

    async function openAssign() {

        var picker = UI.el("assignCoursePicker");
        picker.innerHTML = '<span class="subtle">Loading courses...</span>';

        openModal("assignModal");

        try {
            var results = await Promise.all([
                loadCourses(),
                UI.request("/admin/faculty/" + facultyId)
            ]);

            var courses = results[0];
            var ownedIds = (results[1].courses || []).map(function (course) {
                return course.id;
            });

            if (!courses.length) {
                picker.innerHTML = '<span class="subtle">No courses exist yet.</span>';
                return;
            }

            picker.innerHTML = courses.map(function (course) {
                var owned = ownedIds.indexOf(course.id) >= 0;
                var label = (course.title || "Course " + course.id) +
                    (course.courseCode ? " (" + course.courseCode + ")" : "");

                return '<label class="course-option">' +
                    '<input type="checkbox" value="' + course.id + '"' +
                    (owned ? " disabled" : "") + ">" +
                    "<span>" + UI.escapeHtml(label) + "</span>" +
                    "<small>" +
                        (owned ? "Already assigned" : UI.escapeHtml(course.facultyName || "Unassigned")) +
                    "</small>" +
                    "</label>";
            }).join("");

        } catch (error) {
            picker.innerHTML = '<span class="subtle">' + UI.escapeHtml(error.message) + "</span>";
        }
    }

    async function submitAssign(event) {
        event.preventDefault();

        var button = UI.el("saveAssign");
        var picker = UI.el("assignCoursePicker");

        var courseIds = Array.prototype.slice
            .call(picker.querySelectorAll('input[type="checkbox"]:checked'))
            .map(function (input) { return Number(input.value); });

        if (!courseIds.length) {
            UI.toast("Select at least one course to assign.", "error", "Nothing selected");
            return;
        }

        try {
            UI.setBusy(button, true, "Assigning...");

            await UI.request("/admin/faculty/" + facultyId + "/courses", {
                method: "POST",
                body: { courseIds: courseIds }
            });

            courseCache = null;
            closeModal("assignModal");
            UI.toast("Courses assigned successfully.", "success", "Updated");
            load();

        } catch (error) {
            UI.toast(error.message, "error");
        } finally {
            UI.setBusy(button, false);
        }
    }

    /* ---------------- actions ---------------- */

    async function toggleStatus() {

        if (!profile) return;

        var next = !profile.active;

        var confirmed = await UI.confirm({
            title: (next ? "Activate " : "Deactivate ") +
                (UI.joinName(profile.firstName, profile.lastName) || "this faculty member") + "?",
            message: next
                ? "The faculty member will be able to sign in again."
                : "The account is blocked immediately. Courses must be reassigned first, otherwise the request is rejected.",
            confirmLabel: next ? "Activate account" : "Deactivate account",
            danger: !next
        });

        if (!confirmed) return;

        try {
            await UI.request("/admin/faculty/" + facultyId + "/status", {
                method: "PATCH",
                body: { active: next }
            });

            UI.toast("Account " + (next ? "activated" : "deactivated") + ".",
                "success", "Updated");
            load();

        } catch (error) {
            UI.toast(error.message, "error");
        }
    }

    async function resetPassword() {

        if (!profile) return;

        var name = UI.joinName(profile.firstName, profile.lastName) || "this faculty member";

        var confirmed = await UI.confirm({
            title: "Reset password for " + name + "?",
            message: "A new temporary password will be generated and the current password stops working immediately.",
            confirmLabel: "Reset password",
            danger: false
        });

        if (!confirmed) return;

        try {
            var result = await UI.request("/admin/faculty/" + facultyId + "/reset-password", {
                method: "POST",
                body: { generatePassword: true }
            });

            if (result && result.temporaryPassword) {
                UI.el("temporaryPassword").textContent = result.temporaryPassword;
                UI.el("passwordRecipient").textContent =
                    "Share this with " + name + " (" + (profile.email || "-") + ").";
                openModal("passwordModal");
            } else {
                UI.toast("Password reset.", "success", "Updated");
            }

        } catch (error) {
            UI.toast(error.message, "error");
        }
    }

    async function deleteFaculty() {

        if (!profile) return;

        var name = UI.joinName(profile.firstName, profile.lastName) || "this faculty member";

        var confirmed = await UI.confirm({
            title: "Delete " + name + "?",
            message: "Courses must be reassigned first. If review history exists, the account is deactivated instead of removed.",
            confirmLabel: "Delete faculty"
        });

        if (!confirmed) return;

        try {
            var result = await UI.request("/admin/faculty/" + facultyId, { method: "DELETE" });

            UI.toast(
                result && result.message ? result.message : "Faculty account deleted.",
                "success",
                result && result.mode === "DEACTIVATED" ? "Account deactivated" : "Deleted"
            );

            setTimeout(function () {
                window.location.href = "/admin/faculty.html";
            }, 900);

        } catch (error) {
            UI.toast(error.message, "error");
        }
    }

    /* ---------------- events ---------------- */

    UI.el("assignCoursesBtn").addEventListener("click", openAssign);
    UI.el("assignForm").addEventListener("submit", submitAssign);
    UI.el("resetPasswordBtn").addEventListener("click", resetPassword);
    UI.el("toggleStatusBtn").addEventListener("click", toggleStatus);
    UI.el("deleteFacultyBtn").addEventListener("click", deleteFaculty);

    UI.el("copyPassword").addEventListener("click", async function () {
        try {
            await navigator.clipboard.writeText(UI.el("temporaryPassword").textContent);
            UI.toast("Password copied to the clipboard.", "success", "Copied");
        } catch (error) {
            UI.toast("Copy failed. Select the password and copy it manually.", "error");
        }
    });

    /* ---------------- init ---------------- */

    UI.bindChrome();
    load();
})();
