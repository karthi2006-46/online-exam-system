/* =====================================================
   ADMIN - EXAMINATION RESULTS
   Results are loaded per course, with optional exam,
   date-range and outcome filters.
===================================================== */
(function () {
    "use strict";

    var UI = window.AdminUI;

    if (!UI.guard()) return;

    var state = {
        page: 0,
        size: 10,
        courseId: "",
        examId: "",
        status: "",
        fromDate: "",
        toDate: ""
    };

    var examsForCourse = "";

    /* ---------------- filter options ---------------- */

    async function loadCourses() {
        try {
            var response = await UI.request("/courses");
            var courses = (response && response.data) ? response.data : (response || []);

            UI.el("resultCourse").innerHTML = '<option value="">Select a course</option>' +
                courses.map(function (course) {
                    var label = (course.title || "Course " + course.id) +
                        (course.courseCode ? " (" + course.courseCode + ")" : "");
                    return '<option value="' + course.id + '">' +
                        UI.escapeHtml(label) + "</option>";
                }).join("");

        } catch (error) {
            UI.showPageMessage("pageMessage",
                "Unable to load courses: " + error.message, "error");
        }
    }

    async function loadExams(courseId) {
        var examSelect = UI.el("resultExam");
        examSelect.innerHTML = '<option value="">All exams</option>';

        if (!courseId) return;

        try {
            var exams = await UI.request(
                "/admin/performance/exams" + UI.query({ courseId: courseId })) || [];

            examSelect.innerHTML = '<option value="">All exams</option>' +
                exams.map(function (exam) {
                    return '<option value="' + exam.id + '">' +
                        UI.escapeHtml(exam.title || ("Exam " + exam.id)) + "</option>";
                }).join("");

        } catch (error) {
            /* the filter still works with "All exams" */
        }
    }

    /* ---------------- table ---------------- */

    function renderRow(row) {
        var studentCell = '<span class="person-cell">' +
            '<span class="person-avatar">' + UI.escapeHtml(UI.initials(row.studentName)) + "</span>" +
            "<span><strong>" +
                '<a class="row-link" href="/admin/student-detail.html?id=' + row.studentId + '">' +
                UI.escapeHtml(row.studentName || "-") + "</a>" +
            "</strong><small>" + UI.escapeHtml(row.email || "-") + "</small></span>" +
        "</span>";

        return "<tr>" +
            '<td data-label="Student">' + studentCell + "</td>" +
            '<td data-label="Course" class="cell-wrap">' +
                UI.escapeHtml(row.courseTitle || "-") + "</td>" +
            '<td data-label="Exam" class="cell-wrap">' +
                UI.escapeHtml(row.examTitle || "-") + "</td>" +
            '<td data-label="Submitted">' + UI.dateTime(row.examDate) + "</td>" +
            '<td data-label="Marks" class="num">' +
                UI.number(row.obtainedMarks) + " / " + UI.number(row.totalMarks) + "</td>" +
            '<td data-label="Percentage">' + UI.progressBar(row.percentage) + "</td>" +
            '<td data-label="Status">' + UI.resultPill(row.resultStatus) + "</td>" +
            '<td data-label="Correct" class="num">' + UI.number(row.correctAnswers) + "</td>" +
            '<td data-label="Wrong" class="num">' + UI.number(row.wrongAnswers) + "</td>" +
            '<td data-label="Skipped" class="num">' + UI.number(row.skippedQuestions) + "</td>" +
            "</tr>";
    }

    function emptyState() {
        return UI.tableState(
            10,
            "No results found",
            "Try a different exam, date range or outcome.");
    }

    async function loadResults() {
        var body = UI.el("resultTableBody");

        if (!state.courseId) {
            body.innerHTML = UI.tableState(
                10,
                "Select a course",
                "Results are loaded per course.");
            UI.el("resultPager").innerHTML = "";
            UI.showPageMessage("pageMessage", "Select a course to load results.", "");
            return;
        }

        body.innerHTML = UI.loadingRow(10);
        UI.showPageMessage("pageMessage", "Loading results...", "");

        var params = UI.query({
            courseId: state.courseId,
            examId: state.examId,
            status: state.status,
            fromDate: state.fromDate,
            toDate: state.toDate,
            page: state.page,
            size: state.size
        });

        try {
            var page = await UI.request("/admin/results" + params);
            var rows = page.content || [];

            body.innerHTML = rows.length
                ? rows.map(renderRow).join("")
                : emptyState();

            UI.renderPager(UI.el("resultPager"), page, function (nextPage) {
                state.page = nextPage;
                loadResults();
            });

            var total = Number(page.totalElements || 0);
            UI.showPageMessage("pageMessage",
                total
                    ? "Showing " + UI.number(total) + " result" + (total === 1 ? "" : "s") + "."
                    : "No results match the current filters.",
                "");

        } catch (error) {
            body.innerHTML = UI.tableState(10, "Unable to load results", error.message, true);
            UI.el("resultPager").innerHTML = "";
            UI.showPageMessage("pageMessage", error.message, "error");
            UI.toast(error.message, "error");
        }
    }

    /* ---------------- events ---------------- */

    UI.el("resultCourse").addEventListener("change", async function (event) {
        state.courseId = event.target.value;
        state.examId = "";
        state.page = 0;

        UI.el("resultExam").value = "";

        if (state.courseId && examsForCourse !== state.courseId) {
            examsForCourse = state.courseId;
            await loadExams(state.courseId);
        } else if (!state.courseId) {
            examsForCourse = "";
            await loadExams("");
        }

        loadResults();
    });

    UI.el("resultExam").addEventListener("change", function (event) {
        state.examId = event.target.value;
        state.page = 0;
        loadResults();
    });

    UI.el("resultStatus").addEventListener("change", function (event) {
        state.status = event.target.value;
        state.page = 0;
        loadResults();
    });

    ["resultFrom", "resultTo"].forEach(function (id) {
        UI.el(id).addEventListener("change", function (event) {
            state[id === "resultFrom" ? "fromDate" : "toDate"] = event.target.value;
            state.page = 0;
            loadResults();
        });
    });

    UI.el("refreshResults").addEventListener("click", loadResults);

    UI.el("clearResultFilters").addEventListener("click", function () {
        UI.el("resultExam").value = "";
        UI.el("resultStatus").value = "";
        UI.el("resultFrom").value = "";
        UI.el("resultTo").value = "";

        state.page = 0;
        state.examId = "";
        state.status = "";
        state.fromDate = "";
        state.toDate = "";

        loadResults();
    });

    UI.el("exportResults").addEventListener("click", async function () {
        var button = UI.el("exportResults");

        if (!state.courseId) {
            UI.toast("Select a course before exporting.", "error");
            return;
        }

        try {
            UI.setBusy(button, true, "Preparing...");
            await UI.download(
                "/admin/export/results.xlsx" + UI.query({
                    courseId: state.courseId,
                    examId: state.examId,
                    status: state.status,
                    fromDate: state.fromDate,
                    toDate: state.toDate
                }),
                "exam-results.xlsx"
            );
        } catch (error) {
            UI.toast(error.message, "error");
        } finally {
            UI.setBusy(button, false);
        }
    });

    /* ---------------- init ---------------- */

    UI.bindChrome();
    loadCourses();
    loadResults();
})();
