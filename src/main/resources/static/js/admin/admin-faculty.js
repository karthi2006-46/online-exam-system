/* =====================================================
   ADMIN - FACULTY MANAGEMENT
===================================================== */
(function () {
    "use strict";

    var UI = window.AdminUI;

    if (!UI.guard()) return;

    var icons = UI.icons;

    var state = {
        page: 0,
        size: 10,
        search: "",
        active: "",
        editingId: null,
        assigningId: null
    };

    var courseCache = null;
    var searchTimer = null;

    /* ---------------- modal helpers ---------------- */

    function openModal(id) {
        var node = UI.el(id);
        if (!node) return;
        node.classList.add("is-open");
        node.setAttribute("aria-hidden", "false");
        document.body.classList.add("as-lock");

        var focusable = node.querySelector("input, select, textarea, button");
        if (focusable) setTimeout(function () { focusable.focus(); }, 80);
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
        button.addEventListener("click", function () {
            closeModal(button.dataset.close);
        });
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

    /* ---------------- data ---------------- */

    async function loadCourses(force) {

        if (courseCache && !force) return courseCache;

        var response = await UI.request("/courses");
        courseCache = (response && response.data) ? response.data : (response || []);

        return courseCache;
    }

    function courseLabel(course) {
        var code = course.courseCode ? " (" + course.courseCode + ")" : "";
        return (course.title || "Course " + course.id) + code;
    }

    function ownerLabel(course) {
        if (!course.facultyName) return "Unassigned";
        return course.facultyName;
    }

    function renderCoursePicker(container, courses, options) {
        options = options || {};

        if (!container) return;

        if (!courses.length) {
            container.innerHTML = '<span class="subtle">No courses exist yet.</span>';
            return;
        }

        container.innerHTML = courses.map(function (course) {
            var isOwned = options.ownedIds && options.ownedIds.indexOf(course.id) >= 0;

            return '<label class="course-option">' +
                '<input type="checkbox" value="' + course.id + '"' +
                (isOwned ? " disabled" : "") + ">" +
                "<span>" + UI.escapeHtml(courseLabel(course)) + "</span>" +
                "<small>" +
                    (isOwned
                        ? "Already assigned"
                        : UI.escapeHtml(ownerLabel(course))) +
                "</small>" +
                "</label>";
        }).join("");
    }

    /* ---------------- table ---------------- */

    function selectedCourseIds(container) {
        if (!container) return [];

        return Array.prototype.slice
            .call(container.querySelectorAll('input[type="checkbox"]:checked'))
            .map(function (input) { return Number(input.value); });
    }

    function renderRow(row) {
        var courses = row.assignedCourses && row.assignedCourses.length
            ? UI.escapeHtml(row.assignedCourses.join(", "))
            : '<span class="subtle">Not assigned</span>';

        return "<tr>" +
            '<td data-label="Faculty"><span class="person-cell">' +
                '<span class="person-avatar">' + UI.escapeHtml(UI.initials(row.name)) + "</span>" +
                "<span><strong>" +
                    '<a class="row-link" href="/admin/faculty-detail.html?id=' + row.facultyId + '">' +
                    UI.escapeHtml(row.name) + "</a></strong>" +
                "<small>" + UI.escapeHtml(row.email) + "</small></span>" +
            "</span></td>" +
            '<td data-label="Courses" class="cell-wrap">' + courses + "</td>" +
            '<td data-label="Students" class="num">' + UI.number(row.totalStudents) + "</td>" +
            '<td data-label="Active" class="num">' + UI.number(row.activeStudents) + "</td>" +
            '<td data-label="Results" class="num">' + UI.number(row.totalResults) + "</td>" +
            '<td data-label="Pass rate">' + UI.progressBar(row.passPercentage) + "</td>" +
            '<td data-label="Status">' + UI.statusPill(row.active) + "</td>" +
            '<td data-label="Actions" class="cell-actions">' +
                '<a class="button button-light button-sm" href="/admin/faculty-detail.html?id=' +
                    row.facultyId + '">View</a>' +
                '<button class="button button-light button-sm" type="button" data-act="edit" data-id="' +
                    row.facultyId + '">Edit</button>' +
                '<button class="icon-button" type="button" data-act="assign" data-id="' +
                    row.facultyId + '" data-name="' + UI.escapeHtml(row.name) +
                    '" title="Assign courses" aria-label="Assign courses">' +
                    icons.courses + "</button>" +
                '<button class="icon-button" type="button" data-act="reset" data-id="' +
                    row.facultyId + '" data-name="' + UI.escapeHtml(row.name) +
                    '" data-email="' + UI.escapeHtml(row.email) +
                    '" title="Reset password" aria-label="Reset password">' +
                    icons.shield + "</button>" +
                '<button class="icon-button is-danger" type="button" data-act="delete" data-id="' +
                    row.facultyId + '" data-name="' + UI.escapeHtml(row.name) +
                    '" data-courses="' + (row.courseIds ? row.courseIds.length : 0) +
                    '" title="Delete" aria-label="Delete faculty">' +
                    icons.x + "</button>" +
            "</td>" +
            "</tr>";
    }

    async function loadFaculty() {

        var body = UI.el("facultyTableBody");
        if (body) body.innerHTML = UI.loadingRow(8);

        var params = UI.query({
            search: state.search,
            active: state.active,
            page: state.page,
            size: state.size
        });

        try {
            var page = await UI.request("/admin/faculty" + params);

            var rows = page.content || [];

            if (!rows.length) {
                body.innerHTML = UI.tableState(
                    8,
                    "No faculty found",
                    state.search || state.active
                        ? "Try a different search term or clear the filters."
                        : "Use Add faculty to create the first account.");
            } else {
                body.innerHTML = rows.map(renderRow).join("");
            }

            UI.renderPager(UI.el("facultyPager"), page, function (nextPage) {
                state.page = nextPage;
                loadFaculty();
            });

            var message = UI.el("pageMessage");
            if (message) {
                message.textContent = "";
                message.className = "page-message";
            }

        } catch (error) {
            body.innerHTML = UI.tableState(8, "Unable to load faculty", error.message, true);
            UI.toast(error.message, "error");
        }
    }

    /* ---------------- create / edit ---------------- */

    async function openCreate() {

        state.editingId = null;

        UI.el("facultyModalTitle").textContent = "Add faculty";
        UI.el("facultyModalSubtitle").textContent =
            "Create a faculty account and assign courses.";
        UI.el("saveFaculty").textContent = "Create faculty";

        UI.el("facultyForm").reset();
        UI.el("passwordGroup").hidden = false;
        UI.el("activeGroup").hidden = true;
        UI.el("courseAssignGroup").hidden = false;

        openModal("facultyModal");

        var picker = UI.el("createCoursePicker");
        picker.innerHTML = '<span class="subtle">Loading courses...</span>';

        try {
            var courses = await loadCourses();
            renderCoursePicker(picker, courses);
        } catch (error) {
            picker.innerHTML = '<span class="subtle">' + UI.escapeHtml(error.message) + "</span>";
        }
    }

    async function openEdit(id) {

        state.editingId = id;

        var message = UI.el("pageMessage");
        message.className = "page-message";
        message.textContent = "Loading faculty details...";

        try {
            var detail = await UI.request("/admin/faculty/" + id);
            var profile = detail.profile || {};

            UI.el("facultyModalTitle").textContent = "Edit faculty";
            UI.el("facultyModalSubtitle").textContent =
                "Update profile details and account status.";
            UI.el("saveFaculty").textContent = "Save changes";

            UI.el("facultyFirstName").value = profile.firstName || "";
            UI.el("facultyLastName").value = profile.lastName || "";
            UI.el("facultyEmail").value = profile.email || "";
            UI.el("facultyPhone").value = profile.phone || "";
            UI.el("facultyAddress").value = profile.address || "";

            UI.el("passwordGroup").hidden = true;
            UI.el("activeGroup").hidden = false;
            UI.el("facultyActive").checked = profile.active !== false;
            UI.el("courseAssignGroup").hidden = true;

            message.textContent = "";
            openModal("facultyModal");

        } catch (error) {
            message.className = "page-message is-error";
            message.textContent = error.message;
            UI.toast(error.message, "error");
        }
    }

    async function submitFaculty(event) {
        event.preventDefault();

        var button = UI.el("saveFaculty");

        var payload = {
            firstName: UI.el("facultyFirstName").value.trim(),
            lastName: UI.el("facultyLastName").value.trim(),
            email: UI.el("facultyEmail").value.trim(),
            phone: UI.el("facultyPhone").value.trim(),
            address: UI.el("facultyAddress").value.trim()
        };

        if (!payload.firstName || !payload.lastName || !payload.email) {
            UI.toast("First name, last name and email are required.", "error", "Missing details");
            return;
        }

        try {
            UI.setBusy(button, true, "Saving...");

            if (state.editingId) {

                payload.active = UI.el("facultyActive").checked;
                payload.courseIds = [];

                await UI.request("/admin/faculty/" + state.editingId, {
                    method: "PUT",
                    body: payload
                });

                closeModal("facultyModal");
                UI.toast("Faculty details updated.", "success", "Saved");
                loadFaculty();

            } else {

                var password = UI.el("facultyPassword").value;

                payload.password = password || null;
                payload.generatePassword = !password;
                payload.courseIds = selectedCourseIds(UI.el("createCoursePicker"));

                var account = await UI.request("/admin/faculty", {
                    method: "POST",
                    body: payload
                });

                closeModal("facultyModal");
                loadFaculty();

                if (account && account.passwordGenerated && account.temporaryPassword) {
                    showPassword(
                        account.temporaryPassword,
                        account.faculty
                    );
                } else {
                    UI.toast("Faculty account created.", "success", "Created");
                }
            }

        } catch (error) {
            UI.toast(error.message, "error");
        } finally {
            UI.setBusy(button, false);
        }
    }

    /* ---------------- assign courses ---------------- */

    async function openAssign(id, name) {

        state.assigningId = id;

        UI.el("assignModalTitle").textContent = "Assign courses";
        UI.el("assignModalSubtitle").textContent =
            "Choose courses to move to " + (name || "this faculty member") + ".";

        var picker = UI.el("assignCoursePicker");
        picker.innerHTML = '<span class="subtle">Loading courses...</span>';

        openModal("assignModal");

        try {
            var detail = await UI.request("/admin/faculty/" + id);
            var courses = await loadCourses();

            renderCoursePicker(picker, courses, {
                ownedIds: (detail.courses || []).map(function (course) {
                    return course.id;
                })
            });
        } catch (error) {
            picker.innerHTML = '<span class="subtle">' + UI.escapeHtml(error.message) + "</span>";
        }
    }

    async function submitAssign(event) {
        event.preventDefault();

        var button = UI.el("saveAssign");
        var courseIds = selectedCourseIds(UI.el("assignCoursePicker"));

        if (!courseIds.length) {
            UI.toast("Select at least one course to assign.", "error", "Nothing selected");
            return;
        }

        try {
            UI.setBusy(button, true, "Assigning...");

            await UI.request("/admin/faculty/" + state.assigningId + "/courses", {
                method: "POST",
                body: { courseIds: courseIds }
            });

            closeModal("assignModal");
            UI.toast("Courses assigned successfully.", "success", "Updated");
            loadFaculty();

        } catch (error) {
            UI.toast(error.message, "error");
        } finally {
            UI.setBusy(button, false);
        }
    }

    /* ---------------- reset password ---------------- */

    async function resetPassword(id, name, email) {
        var confirmed = await UI.confirm({
            title: "Reset password for " + name + "?",
            message: "A new temporary password will be generated. The faculty member's current password stops working immediately.",
            confirmLabel: "Reset password",
            danger: false
        });

        if (!confirmed) return;

        try {
            var result = await UI.request("/admin/faculty/" + id + "/reset-password", {
                method: "POST",
                body: { generatePassword: true }
            });

            if (result && result.temporaryPassword) {
                showPassword(result.temporaryPassword, { email: email, firstName: name });
            } else {
                UI.toast("Password reset.", "success", "Updated");
            }

        } catch (error) {
            UI.toast(error.message, "error");
        }
    }

    function showPassword(password, faculty) {
        UI.el("temporaryPassword").textContent = password;

        var recipient = "Share this with ";
        if (faculty) {
            var name = UI.joinName(faculty.firstName, faculty.lastName) || faculty.name;
            recipient += (name || "the faculty member");
            if (faculty.email) recipient += " (" + faculty.email + ")";
        } else {
            recipient += "the faculty member";
        }

        UI.el("passwordRecipient").textContent = recipient + ".";
        openModal("passwordModal");
    }

    /* ---------------- delete ---------------- */

    async function deleteFaculty(id, name, courseCount) {

        var detail = courseCount
            ? "This faculty member still owns " + courseCount +
              " course(s). Reassign those courses first, or the request will be rejected."
            : "If this account has review history it is deactivated; otherwise it is removed permanently.";

        var confirmed = await UI.confirm({
            title: "Delete " + name + "?",
            message: detail,
            confirmLabel: "Delete faculty"
        });

        if (!confirmed) return;

        try {
            var result = await UI.request("/admin/faculty/" + id, { method: "DELETE" });

            if (result && result.mode === "DEACTIVATED") {
                UI.toast(result.message, "success", "Account deactivated");
            } else {
                UI.toast(result && result.message ? result.message : "Faculty account deleted.",
                    "success", "Deleted");
            }

            loadFaculty();

        } catch (error) {
            UI.toast(error.message, "error");
        }
    }

    /* ---------------- events ---------------- */

    UI.el("facultySearch").addEventListener("input", function (event) {
        clearTimeout(searchTimer);
        var value = event.target.value.trim();

        searchTimer = setTimeout(function () {
            state.search = value;
            state.page = 0;
            loadFaculty();
        }, 350);
    });

    UI.el("facultyStatus").addEventListener("change", function (event) {
        state.active = event.target.value;
        state.page = 0;
        loadFaculty();
    });

    UI.el("clearFacultyFilters").addEventListener("click", function () {
        UI.el("facultySearch").value = "";
        UI.el("facultyStatus").value = "";
        state.search = "";
        state.active = "";
        state.page = 0;
        loadFaculty();
    });

    UI.el("refreshFaculty").addEventListener("click", loadFaculty);
    UI.el("addFaculty").addEventListener("click", openCreate);
    UI.el("facultyForm").addEventListener("submit", submitFaculty);
    UI.el("assignForm").addEventListener("submit", submitAssign);

    UI.el("exportFaculty").addEventListener("click", async function () {
        var button = UI.el("exportFaculty");

        try {
            UI.setBusy(button, true, "Preparing...");
            await UI.download(
                "/admin/export/faculty.xlsx" + UI.query({
                    search: state.search,
                    active: state.active
                }),
                "faculty.xlsx"
            );
        } catch (error) {
            UI.toast(error.message, "error");
        } finally {
            UI.setBusy(button, false);
        }
    });

    UI.el("copyPassword").addEventListener("click", async function () {
        var value = UI.el("temporaryPassword").textContent;

        try {
            await navigator.clipboard.writeText(value);
            UI.toast("Password copied to the clipboard.", "success", "Copied");
        } catch (error) {
            UI.toast("Copy failed. Select the password and copy it manually.", "error");
        }
    });

    UI.el("facultyTableBody").addEventListener("click", function (event) {
        var button = event.target.closest("button[data-act]");
        if (!button) return;

        var id = Number(button.dataset.id);

        if (button.dataset.act === "edit") openEdit(id);
        else if (button.dataset.act === "assign") openAssign(id, button.dataset.name);
        else if (button.dataset.act === "reset") {
            resetPassword(id, button.dataset.name, button.dataset.email);
        } else if (button.dataset.act === "delete") {
            deleteFaculty(
                id,
                button.dataset.name,
                Number(button.dataset.courses || 0)
            );
        }
    });

    /* ---------------- init ---------------- */

    UI.bindChrome();
    loadFaculty();
})();
