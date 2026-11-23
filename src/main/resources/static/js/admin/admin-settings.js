/* =====================================================
   ADMIN - PLATFORM SETTINGS
===================================================== */
(function () {
    "use strict";

    var UI = window.AdminUI;

    if (!UI.guard()) return;

    var money = new Intl.NumberFormat("en-IN", {
        style: "currency",
        currency: "INR",
        maximumFractionDigits: 0
    });

    var EMAIL = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

    /* ---------------- render ---------------- */

    function setText(id, value) {
        var node = UI.el(id);
        if (node) node.textContent = value;
    }

    function renderState() {
        setText("settingsRegistrationState",
            UI.el("allowStudentSignup").checked ? "Open" : "Closed");

        setText("settingsDurationState",
            (UI.el("defaultCourseDurationDays").value || "-") + " days");

        var fees = Number(UI.el("defaultCourseFees").value || 0);
        setText("settingsFeesState", money.format(fees));

        setText("settingsEmailState",
            UI.el("supportEmail").value.trim() || "Not set");
    }

    function fill(settings) {
        UI.el("platformName").value = settings.platformName || "";
        UI.el("supportEmail").value = settings.supportEmail || "";
        UI.el("supportPhone").value = settings.supportPhone || "";
        UI.el("allowStudentSignup").checked =
            settings.allowStudentSignup !== false;
        UI.el("defaultCourseDurationDays").value =
            settings.defaultCourseDurationDays == null
                ? 30
                : settings.defaultCourseDurationDays;
        UI.el("defaultCourseFees").value =
            settings.defaultCourseFees == null
                ? 0
                : settings.defaultCourseFees;

        setText("settingsMeta", settings.updatedAt
            ? "Last saved " + UI.dateTime(settings.updatedAt)
            : "Using platform defaults. Save to store your changes.");

        renderState();
    }

    /* ---------------- load ---------------- */

    async function load(showToast) {

        try {
            var settings = await UI.request("/admin/settings");

            fill(settings);
            UI.showPageMessage("pageMessage", "");

            if (showToast) {
                UI.toast("Showing the last saved values.", "success", "Reset");
            }

        } catch (error) {
            UI.showPageMessage("pageMessage", error.message, "error");
            UI.toast(error.message, "error");
        }
    }

    /* ---------------- save ---------------- */

    async function save(event) {

        event.preventDefault();

        var platformName = UI.el("platformName").value.trim();
        var supportEmail = UI.el("supportEmail").value.trim();
        var supportPhone = UI.el("supportPhone").value.trim();
        var duration = Number(UI.el("defaultCourseDurationDays").value);
        var fees = Number(UI.el("defaultCourseFees").value);

        if (!platformName) {
            UI.toast("Platform name is required.", "error");
            UI.el("platformName").focus();
            return;
        }

        if (supportEmail && !EMAIL.test(supportEmail)) {
            UI.toast("Enter a valid support email address.", "error");
            UI.el("supportEmail").focus();
            return;
        }

        if (!duration || duration < 1 || duration > 3650) {
            UI.toast("Default duration must be between 1 and 3650 days.", "error");
            UI.el("defaultCourseDurationDays").focus();
            return;
        }

        if (isNaN(fees) || fees < 0) {
            UI.toast("Default fees cannot be negative.", "error");
            UI.el("defaultCourseFees").focus();
            return;
        }

        var button = UI.el("saveSettings");

        try {
            UI.setBusy(button, true, "Saving...");

            var saved = await UI.request("/admin/settings", {
                method: "PUT",
                body: {
                    platformName: platformName,
                    supportEmail: supportEmail,
                    supportPhone: supportPhone,
                    allowStudentSignup: UI.el("allowStudentSignup").checked,
                    defaultCourseDurationDays: duration,
                    defaultCourseFees: fees
                }
            });

            fill(saved);

            UI.toast("Platform settings updated.", "success", "Saved");

        } catch (error) {
            UI.toast(error.message, "error");
        } finally {
            UI.setBusy(button, false);
        }
    }

    /* ---------------- events ---------------- */

    UI.el("settingsForm").addEventListener("submit", save);

    UI.el("settingsForm").addEventListener("input", renderState);
    UI.el("settingsForm").addEventListener("change", renderState);

    UI.el("resetSettings").addEventListener("click", function () {
        load(true);
    });

    /* ---------------- init ---------------- */

    UI.bindChrome();
    load(false);
})();
