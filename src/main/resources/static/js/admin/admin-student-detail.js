/* =====================================================
   ADMIN - STUDENT DETAIL
===================================================== */
(function () {
    "use strict";

    var UI = window.AdminUI;

    if (!UI.guard()) return;

    var icons = UI.icons;
    var studentId = Number(new URLSearchParams(window.location.search).get("id"));

    if (!studentId || studentId < 1) {
        window.location.replace("/admin/students.html");
        return;
    }

    var profile = null;

    var kpiIcons = {
        kpiCourses: icons.courses,
        kpiCompleted: icons.check,
        kpiResults: icons.performance,
        kpiPassed: icons.shield,
        kpiAverage: icons.results
    };

    Object.keys(kpiIcons).forEach(function (id) {
        var node = UI.el(id);
        if (node) node.innerHTML = kpiIcons[id];
    });

    /* ---------------- helpers ---------------- */

    function setText(id, value) {
        var node = UI.el(id);
        if (node) node.textContent = value;
    }

    function enrollmentPill(status) {
        var value = String(status || "").toUpperCase();
        var map = {
            ACTIVE: ["pill-green", "Active"],
            COMPLETED: ["pill-blue", "Completed"],
            INACTIVE: ["pill-grey", "Inactive"],
            DROPPED: ["pill-red", "Dropped"]
        };
        var config = map[value] || ["pill-grey", value || "-"];
        var label = config[1].charAt(0) + config[1].slice(1).toLowerCase();

        return '<span class="pill ' + config[0] + '">' + UI.escapeHtml(label) + "</span>";
    }

    /* ---------------- render ---------------- */

    function renderHeader(detail) {
        profile = detail.profile || {};

        var name = UI.joinName(profile.firstName, profile.lastName) || "Student";

        document.title = name + " | LearnHub LMS";

        setText("pageTitle", name);
        setText("profileName", name);
        setText("profileAvatar", UI.initials(name));
        setText("profileEmail", profile.email || "-");
        setText("profilePhone", profile.phone || "-");
        setText("profileJoined", UI.date(profile.createdAt));
        setText("profileId", String(profile.id || studentId));

        UI.el("profileStatus").innerHTML = UI.statusPill(profile.active);

        setText("infoEmail", profile.email || "-");
        setText("infoPhone", profile.phone || "-");
        setText("infoAddress", profile.address || "-");
        setText("infoCreated", UI.dateTime(profile.createdAt));
        setText("infoUpdated", UI.dateTime(profile.updatedAt));

        UI.el("toggleStatusBtn").textContent = profile.active ? "Deactivate" : "Activate";

        ["profileHeader", "profileStats", "accountPanel", "reportsPanel"]
            .forEach(function (id) {
                var node = UI.el(id);
                if (node) node.hidden = false;
            });
    }

    function renderStats(detail) {
        setText("kpiEnrolled", UI.number(detail.enrolledCourses));
        setText("kpiCompletedCount", UI.number(detail.completedCourses));
        setText("kpiResultCount", UI.number(detail.totalResults));
        setText("kpiPassedCount", UI.number(detail.passedResults));
        setText("kpiAverageValue", UI.percent(detail.averagePercentage));
    }

    function renderExams(exams) {
        if (!exams || !exams.length) {
            return '<p class="subtle" style="margin-top:10px;">No exams attempted in this course yet.</p>';
        }

        var rows = exams.map(function (exam) {
            return "<tr>" +
                '<td data-label="Exam">' + UI.escapeHtml(exam.examTitle || "-") + "</td>" +
                '<td data-label="Submitted">' + UI.dateTime(exam.submissionDate) + "</td>" +
                '<td data-label="Marks" class="num">' +
                    UI.number(exam.obtainedMarks) + " / " + UI.number(exam.totalMarks) + "</td>" +
                '<td data-label="Percentage">' + UI.progressBar(exam.percentage) + "</td>" +
                '<td data-label="Status">' + UI.resultPill(exam.status) + "</td>" +
                '<td data-label="Answers" class="num">' +
                    UI.number(exam.correctAnswers) + " / " +
                    UI.number(exam.wrongAnswers) + " / " +
                    UI.number(exam.skippedQuestions) + "</td>" +
                "</tr>";
        }).join("");

        return '<div class="table-responsive" style="margin-top:10px;">' +
            '<table class="data-table">' +
                "<thead><tr>" +
                    "<th>Exam</th><th>Submitted</th>" +
                    '<th class="num">Marks</th><th>Percentage</th>' +
                    "<th>Status</th>" +
                    '<th class="num">C / W / S</th>' +
                "</tr></thead>" +
                "<tbody>" + rows + "</tbody>" +
            "</table></div>";
    }

    function renderReport(report) {

        var progress = report.overallProgress !== null && report.overallProgress !== undefined
            ? report.overallProgress
            : report.completionPercentage;

        return '<article class="report-block">' +
            '<div class="report-block-head">' +
                "<div>" +
                    "<h3>" + UI.escapeHtml(report.courseTitle || "Course") +
                        (report.courseCode
                            ? ' <span class="subtle">(' + UI.escapeHtml(report.courseCode) + ")</span>"
                            : "") +
                    "</h3>" +
                    "<p>" +
                        (report.facultyName
                            ? "Faculty: " + UI.escapeHtml(report.facultyName)
                            : "No faculty assigned") +
                        " &middot; Enrolled " + UI.date(report.enrollmentDate) +
                        (report.dueDate ? " &middot; Due " + UI.date(report.dueDate) : "") +
                    "</p>" +
                "</div>" +
                '<div class="panel-header-actions">' +
                    enrollmentPill(report.enrollmentStatus) +
                    '<button class="button button-light button-sm" type="button" ' +
                        'data-act="pdf" data-course-id="' + report.courseId + '">PDF report</button>' +
                "</div>" +
            "</div>" +

            '<div class="course-card-grid">' +
                '<div class="mini-stat"><span>Progress</span><strong>' +
                    UI.percent(progress) + "</strong></div>" +
                '<div class="mini-stat"><span>Exams attempted</span><strong>' +
                    UI.number(report.totalExamsAttempted) + "</strong></div>" +
                '<div class="mini-stat"><span>Passed</span><strong>' +
                    UI.number(report.examsPassed) + "</strong></div>" +
                '<div class="mini-stat"><span>Failed</span><strong>' +
                    UI.number(report.examsFailed) + "</strong></div>" +
                '<div class="mini-stat"><span>Average</span><strong>' +
                    UI.percent(report.averagePercentage) + "</strong></div>" +
            "</div>" +

            renderExams(report.examPerformances) +
            "</article>";
    }

    function renderReports(reports) {
        var container = UI.el("courseReports");

        if (!reports || !reports.length) {
            container.innerHTML = '<p class="subtle">This student has no course enrollments yet.</p>';
            return;
        }

        container.innerHTML = reports.map(renderReport).join("");
    }

    /* ---------------- load ---------------- */

    async function load() {

        var message = UI.el("pageMessage");
        message.className = "page-message";
        message.textContent = "Loading student profile...";

        try {
            var detail = await UI.request("/admin/students/" + studentId);

            renderHeader(detail);
            renderStats(detail);
            renderReports(detail.courseReports || []);

            message.textContent = "";
            message.className = "page-message";

        } catch (error) {
            message.className = "page-message is-error";
            message.textContent = error.message || "Unable to load this student profile.";
            UI.toast(error.message, "error");
        }
    }

    /* ---------------- actions ---------------- */

    async function toggleStatus() {

        if (!profile) return;

        var next = !profile.active;
        var name = UI.joinName(profile.firstName, profile.lastName) || "this student";

        var confirmed = await UI.confirm({
            title: (next ? "Activate " : "Deactivate ") + name + "?",
            message: next
                ? "The student will be able to sign in and continue their courses."
                : "The account is blocked immediately. Academic records stay intact and the account can be reactivated later.",
            confirmLabel: next ? "Activate account" : "Deactivate account",
            danger: !next
        });

        if (!confirmed) return;

        try {
            await UI.request("/admin/students/" + studentId + "/status", {
                method: "PATCH",
                body: { active: next }
            });

            UI.toast("Student account " + (next ? "activated" : "deactivated") + ".",
                "success", "Updated");
            load();

        } catch (error) {
            UI.toast(error.message, "error");
        }
    }

    async function deleteStudent() {

        if (!profile) return;

        var name = UI.joinName(profile.firstName, profile.lastName) || "this student";

        var confirmed = await UI.confirm({
            title: "Delete " + name + "?",
            message: "If the student has academic records, the account is deactivated instead of removed.",
            confirmLabel: "Delete student"
        });

        if (!confirmed) return;

        try {
            var result = await UI.request("/admin/students/" + studentId, { method: "DELETE" });

            UI.toast(
                result && result.message ? result.message : "Student account deleted.",
                "success",
                result && result.mode === "DEACTIVATED" ? "Account deactivated" : "Deleted"
            );

            setTimeout(function () {
                window.location.href = "/admin/students.html";
            }, 900);

        } catch (error) {
            UI.toast(error.message, "error");
        }
    }

    async function downloadPdf(courseId, button) {

        try {
            UI.setBusy(button, true, "Preparing...");
            await UI.download(
                "/admin/export/pdf/student/" + studentId +
                UI.query({ courseId: courseId }),
                "progress-report-" + studentId + "-" + courseId + ".pdf"
            );
        } catch (error) {
            UI.toast(error.message, "error");
        } finally {
            UI.setBusy(button, false);
        }
    }

    async function downloadFullReport(format, button) {

        try {
            UI.setBusy(button, true, "Preparing...");
            await UI.download(
                "/admin/export/student/" + studentId + "/report." + format,
                "student-report-" + studentId + "." + format
            );
        } catch (error) {
            UI.toast(error.message, "error");
        } finally {
            UI.setBusy(button, false);
        }
    }

    /* ---------------- events ---------------- */

    UI.el("toggleStatusBtn").addEventListener("click", toggleStatus);
    UI.el("deleteStudentBtn").addEventListener("click", deleteStudent);

    UI.el("downloadFullExcel").addEventListener("click", function () {
        downloadFullReport("xlsx", UI.el("downloadFullExcel"));
    });

    UI.el("downloadFullPdf").addEventListener("click", function () {
        downloadFullReport("pdf", UI.el("downloadFullPdf"));
    });

    UI.el("courseReports").addEventListener("click", function (event) {
        var button = event.target.closest('button[data-act="pdf"]');
        if (!button) return;
        downloadPdf(Number(button.dataset.courseId), button);
    });

    /* ---------------- init ---------------- */

    UI.bindChrome();
    load();
})();
