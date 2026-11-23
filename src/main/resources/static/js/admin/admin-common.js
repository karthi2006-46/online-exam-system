/* =====================================================
   ADMIN - shared helpers
   Loaded by every page after api-config.js and before the
   page script.
===================================================== */
(function () {
    "use strict";

    var API_BASE = (window.API_BASE_URL || "") + "/api";

    /* ---------------- auth ---------------- */

    function token() {
        return localStorage.getItem("token");
    }

    function role() {
        return String(localStorage.getItem("role") || "")
            .replace(/^ROLE_/i, "")
            .toUpperCase();
    }

    function clearAuth() {
        ["token", "userId", "role", "email", "userName"].forEach(function (key) {
            try { localStorage.removeItem(key); } catch (e) {}
            try { sessionStorage.removeItem(key); } catch (e) {}
        });
    }

    /**
     * Redirects unless a valid-looking ADMIN session is present.
     * Returns the stored user id or null.
     */
    function guard() {
        if (!token()) {
            window.location.replace("/login.html");
            return null;
        }

        if (role() !== "ADMIN") {
            window.location.replace("/login.html");
            return null;
        }

        return localStorage.getItem("userId");
    }

    /* ---------------- fetch ---------------- */

    function messageFromBody(body, fallback) {
        if (!body) return fallback;
        if (typeof body === "string") return body || fallback;
        return body.message || body.error || fallback;
    }

    async function request(path, options) {
        options = options || {};

        var headers = {
            Accept: "application/json",
            Authorization: "Bearer " + (token() || "")
        };

        var init = {
            method: options.method || "GET",
            headers: headers
        };

        if (options.body !== undefined) {
            headers["Content-Type"] = "application/json";
            init.body = JSON.stringify(options.body);
        }

        var response;

        try {
            response = await fetch(API_BASE + path, init);
        } catch (error) {
            throw new Error(
                "Cannot reach the server. Check your connection and try again.");
        }

        if (response.status === 401) {
            clearAuth();
            window.location.replace("/login.html");
            throw new Error("Your session has expired. Please sign in again.");
        }

        if (response.status === 403) {
            throw new Error(
                "Access denied. This action requires an administrator account.");
        }

        if (response.status === 204) return null;

        var text = await response.text();
        var body = null;

        if (text) {
            try { body = JSON.parse(text); } catch (e) { body = text; }
        }

        if (!response.ok) {
            throw new Error(
                messageFromBody(body, "Request failed (HTTP " + response.status + ")."));
        }

        return body;
    }

    async function download(path, fallbackName) {
        var response;

        try {
            response = await fetch(API_BASE + path, {
                headers: { Authorization: "Bearer " + (token() || "") }
            });
        } catch (error) {
            throw new Error(
                "Cannot reach the server. Check your connection and try again.");
        }

        if (!response.ok) {
            var detail = "";

            try {
                var contentType = response.headers.get("content-type") || "";
                detail = contentType.indexOf("json") >= 0
                    ? messageFromBody(await response.json(), "")
                    : await response.text();
            } catch (e) { /* keep the generic message */ }

            throw new Error(
                detail || "The download failed (HTTP " + response.status + ").");
        }

        var filename = fallbackName || "download";

        var disposition = response.headers.get("content-disposition") || "";
        var match = /filename=\"?([^\";]+)\"?/i.exec(disposition);
        if (match) filename = match[1];

        var blob = await response.blob();
        var url = URL.createObjectURL(blob);
        var link = document.createElement("a");

        link.href = url;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        link.remove();

        setTimeout(function () { URL.revokeObjectURL(url); }, 1500);
    }

    function query(params) {
        var parts = [];

        Object.keys(params || {}).forEach(function (key) {
            var value = params[key];
            if (value === undefined || value === null || value === "") return;
            parts.push(encodeURIComponent(key) + "=" + encodeURIComponent(value));
        });

        return parts.length ? "?" + parts.join("&") : "";
    }

    /* ---------------- DOM / formatting ---------------- */

    function el(id) {
        return document.getElementById(id);
    }

    function escapeHtml(value) {
        return String(value === null || value === undefined ? "" : value)
            .replace(/[&<>"']/g, function (character) {
                return {
                    "&": "&amp;",
                    "<": "&lt;",
                    ">": "&gt;",
                    '"': "&quot;",
                    "'": "&#039;"
                }[character];
            });
    }

    function initials(name) {
        var parts = String(name || "").trim().split(/\s+/).filter(Boolean);
        if (!parts.length) return "?";
        if (parts.length === 1) return parts[0].charAt(0).toUpperCase();
        return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    function joinName(first, last) {
        return ((first || "") + " " + (last || "")).trim();
    }

    function number(value) {
        if (value === null || value === undefined || value === "") return "0";
        return new Intl.NumberFormat("en-IN").format(value);
    }

    function percent(value, digits) {
        if (value === null || value === undefined || isNaN(value)) return "-";
        return Number(value).toFixed(digits === undefined ? 1 : digits) + "%";
    }

    function date(value) {
        if (!value) return "-";
        var d = new Date(value);
        if (isNaN(d.getTime())) return "-";
        return new Intl.DateTimeFormat("en-IN", {
            day: "2-digit", month: "short", year: "numeric"
        }).format(d);
    }

    function dateTime(value) {
        if (!value) return "-";
        var d = new Date(value);
        if (isNaN(d.getTime())) return "-";
        return new Intl.DateTimeFormat("en-IN", {
            day: "2-digit", month: "short", year: "numeric",
            hour: "2-digit", minute: "2-digit"
        }).format(d);
    }

    function statusPill(active) {
        return active
            ? '<span class="pill pill-green">Active</span>'
            : '<span class="pill pill-red">Inactive</span>';
    }

    function resultPill(status) {
        var value = String(status || "").toUpperCase();

        if (value === "PASS") return '<span class="pill pill-green">PASS</span>';
        if (value === "FAIL") return '<span class="pill pill-red">FAIL</span>';
        if (value === "NOT_ATTEMPTED") return '<span class="pill pill-grey">Not attempted</span>';

        return '<span class="pill pill-grey">' + escapeHtml(value || "-") + "</span>";
    }

    function approvalPill(status) {
        var value = String(status || "NOT_REQUESTED").toUpperCase();
        var labels = {
            APPROVED: ["pill-green", "Approved"],
            PENDING: ["pill-orange", "Pending"],
            REJECTED: ["pill-red", "Rejected"],
            NOT_REQUESTED: ["pill-grey", "Not requested"],
            NOT_SUBMITTED: ["pill-grey", "Not submitted"],
            PENDING_REVIEW: ["pill-orange", "Pending review"]
        };

        var config = labels[value] || ["pill-grey", value];
        var label = config[1].replace(/_/g, " ").toLowerCase();
        label = label.charAt(0).toUpperCase() + label.slice(1);

        return '<span class="pill ' + config[0] + '">' + escapeHtml(label) + "</span>";
    }

    function progressBar(value, options) {
        options = options || {};
        var numeric = Number(value || 0);
        var clamped = Math.max(0, Math.min(100, numeric));

        var tone = options.tone;
        if (!tone) {
            tone = numeric >= 75 ? "is-green" : numeric >= 40 ? "" : "is-orange";
        }

        return '<span class="progress-cell">' +
            '<span class="progress-track"><span class="progress-fill ' + tone +
            '" style="width:' + clamped + '%"></span></span>' +
            '<span class="progress-value">' + percent(numeric, 0) + "</span>" +
            "</span>";
    }

    function tableState(colspan, title, detail, isError) {
        return '<tr><td colspan="' + colspan + '" class="table-state' +
            (isError ? " is-error" : "") + '">' +
            "<strong>" + escapeHtml(title) + "</strong>" +
            escapeHtml(detail || "") + "</td></tr>";
    }

    function loadingRow(colspan) {
        return tableState(colspan, "Loading...", "Fetching the latest records.");
    }

    /* ---------------- toast ---------------- */

    function toastHost() {
        var host = document.getElementById("toastHost");

        if (!host) {
            host = document.createElement("div");
            host.id = "toastHost";
            host.setAttribute("aria-live", "polite");
            document.body.appendChild(host);
        }

        return host;
    }

    function toast(message, type, title) {
        var host = toastHost();
        var node = document.createElement("div");

        node.className = "toast" + (type ? " is-" + type : "");
        node.innerHTML =
            "<div><strong>" + escapeHtml(title || (type === "error" ? "Something went wrong" : "Done")) +
            "</strong><p>" + escapeHtml(message || "") + "</p></div>";

        host.appendChild(node);

        setTimeout(function () {
            node.style.opacity = "0";
            node.style.transform = "translateY(-6px)";
            node.style.transition = "opacity .25s ease, transform .25s ease";
            setTimeout(function () { node.remove(); }, 260);
        }, type === "error" ? 6500 : 3800);
    }

    /* ---------------- confirm dialog ---------------- */

    var confirmState = null;

    function confirmDialog(options) {
        options = options || {};

        return new Promise(function (resolve) {
            var overlay = document.createElement("div");
            overlay.className = "modal-overlay";

            overlay.innerHTML =
                '<div class="modal modal-sm" role="alertdialog" aria-modal="true">' +
                    '<div class="modal-head"><div>' +
                        "<h2>" + escapeHtml(options.title || "Are you sure?") + "</h2>" +
                        "<p>" + escapeHtml(options.message || "") + "</p>" +
                    "</div></div>" +
                    '<div class="modal-foot">' +
                        '<button type="button" class="button button-light" data-act="cancel">' +
                            escapeHtml(options.cancelLabel || "Cancel") + "</button>" +
                        '<button type="button" class="button ' +
                            (options.danger === false ? "button-primary" : "button-danger") +
                            '" data-act="ok">' +
                            escapeHtml(options.confirmLabel || "Confirm") + "</button>" +
                    "</div>" +
                "</div>";

            document.body.appendChild(overlay);
            document.body.classList.add("as-lock");

            requestAnimationFrame(function () {
                overlay.classList.add("is-open");
            });

            var okButton = overlay.querySelector('[data-act="ok"]');
            okButton.focus();

            function finish(result) {
                overlay.classList.remove("is-open");
                document.body.classList.remove("as-lock");
                setTimeout(function () { overlay.remove(); }, 220);
                document.removeEventListener("keydown", onKey);
                confirmState = null;
                resolve(result);
            }

            function onKey(event) {
                if (event.key === "Escape") finish(false);
            }

            overlay.querySelector('[data-act="cancel"]')
                .addEventListener("click", function () { finish(false); });
            okButton.addEventListener("click", function () { finish(true); });
            overlay.addEventListener("click", function (event) {
                if (event.target === overlay) finish(false);
            });
            document.addEventListener("keydown", onKey);

            confirmState = { finish: finish };
        });
    }

    /* ---------------- pagination ---------------- */

    function renderPager(container, pageData, onChange) {
        if (!container) return;

        var page = Number(pageData.page || 0);
        var totalPages = Number(pageData.totalPages || 0);
        var total = Number(pageData.totalElements || 0);
        var size = Number(pageData.size || 10);

        if (!total) {
            container.innerHTML = "";
            return;
        }

        var from = page * size + 1;
        var to = Math.min(total, (page + 1) * size);

        var buttons = [];

        buttons.push(
            '<button type="button" class="pager-button" data-page="' + (page - 1) + '"' +
            (page <= 0 ? " disabled" : "") + ' aria-label="Previous page">&#8592;</button>'
        );

        var windowStart = Math.max(0, page - 2);
        var windowEnd = Math.min(totalPages - 1, page + 2);

        if (windowStart > 0) {
            buttons.push('<button type="button" class="pager-button" data-page="0">1</button>');
            if (windowStart > 1) buttons.push('<span class="pager-gap">...</span>');
        }

        for (var index = windowStart; index <= windowEnd; index++) {
            buttons.push(
                '<button type="button" class="pager-button' +
                (index === page ? " is-active" : "") +
                '" data-page="' + index + '">' + (index + 1) + "</button>"
            );
        }

        if (windowEnd < totalPages - 1) {
            if (windowEnd < totalPages - 2) buttons.push('<span class="pager-gap">...</span>');
            buttons.push(
                '<button type="button" class="pager-button" data-page="' +
                (totalPages - 1) + '">' + totalPages + "</button>"
            );
        }

        buttons.push(
            '<button type="button" class="pager-button" data-page="' + (page + 1) + '"' +
            (page >= totalPages - 1 ? " disabled" : "") + ' aria-label="Next page">&#8594;</button>'
        );

        container.innerHTML =
            '<span class="pager-info">Showing ' + number(from) + "-" + number(to) +
            " of " + number(total) + "</span>" +
            '<span class="pager-controls">' + buttons.join("") + "</span>";

        container.querySelectorAll(".pager-button[data-page]").forEach(function (button) {
            button.addEventListener("click", function () {
                var target = Number(button.dataset.page);
                if (target >= 0 && target < totalPages && target !== page) {
                    onChange(target);
                }
            });
        });
    }

    /* ---------------- misc ---------------- */

    function setBusy(button, busy, label) {
        if (!button) return;

        if (busy) {
            button.dataset.originalLabel = button.innerHTML;
            button.disabled = true;
            button.innerHTML = escapeHtml(label || "Working...");
        } else {
            button.disabled = false;
            if (button.dataset.originalLabel) {
                button.innerHTML = button.dataset.originalLabel;
                delete button.dataset.originalLabel;
            }
        }
    }

    function showPageMessage(id, text, type) {
        var node = el(id);
        if (!node) return;
        node.textContent = text || "";
        node.className = "page-message" + (type ? " is-" + type : "");
    }

    function bindIdentity(profile) {
        if (!profile) return;

        var name = joinName(profile.firstName, profile.lastName) || "Administrator";
        var initial = initials(name);

        var sidebarName = document.getElementById("adminName");
        var sidebarAvatar = document.getElementById("adminAvatar");
        var topAvatar = document.getElementById("topAvatar");

        if (sidebarName) sidebarName.textContent = name;
        if (sidebarAvatar) sidebarAvatar.textContent = initial;
        if (topAvatar) topAvatar.textContent = initial;

        try {
            localStorage.setItem("userName", name);
            localStorage.setItem("email", profile.email || "");
        } catch (e) { /* storage may be unavailable */ }
    }

    async function loadProfile() {
        var userId = localStorage.getItem("userId");
        if (!userId) return null;

        try {
            var response = await request("/auth/profile/" + userId);
            var data = response && response.data ? response.data : response;
            if (data) bindIdentity(data);
            return data;
        } catch (e) {
            return null;
        }
    }

    function bindChrome() {
        var yearNode = el("year");
        if (yearNode) yearNode.textContent = new Date().getFullYear();

        var dateNode = el("currentDate");
        if (dateNode) {
            dateNode.textContent = new Intl.DateTimeFormat("en-IN", {
                weekday: "short", day: "numeric", month: "short", year: "numeric"
            }).format(new Date());
        }
    }

    /* ---------------- icons ---------------- */

    function svg(paths) {
        return '<svg viewBox="0 0 24 24" aria-hidden="true">' + paths + "</svg>";
    }

    var ICONS = {
        dashboard: svg('<rect x="3" y="3" width="7" height="9" rx="1.5"/><rect x="14" y="3" width="7" height="5" rx="1.5"/><rect x="14" y="12" width="7" height="9" rx="1.5"/><rect x="3" y="16" width="7" height="5" rx="1.5"/>'),
        faculty: svg('<circle cx="9" cy="8" r="3.2"/><path d="M3.5 20a5.5 5.5 0 0 1 11 0"/><path d="M16 5.5a3 3 0 0 1 0 5.8"/><path d="M17.5 14.2A5 5 0 0 1 21 19.5"/>'),
        students: svg('<path d="M12 3 3 7.5 12 12l9-4.5z"/><path d="M6 10v5c0 1.7 2.7 3 6 3s6-1.3 6-3v-5"/>'),
        performance: svg('<path d="M4 20V10"/><path d="M10 20V4"/><path d="M16 20v-8"/><path d="M22 20H2"/>'),
        courses: svg('<path d="M2 4h6a4 4 0 0 1 4 4v12a3 3 0 0 0-3-3H2z"/><path d="M22 4h-6a4 4 0 0 0-4 4v12a3 3 0 0 1 3-3h7z"/>'),
        logout: svg('<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="m16 17 5-5-5-5"/><path d="M21 12H9"/>'),
        download: svg('<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><path d="m7 10 5 5 5-5"/><path d="M12 15V3"/>'),
        plus: svg('<path d="M12 5v14M5 12h14"/>'),
        search: svg('<circle cx="11" cy="11" r="7"/><path d="m21 21-4.3-4.3"/>'),
        refresh: svg('<path d="M21 12a9 9 0 1 1-2.6-6.4"/><path d="M21 3v6h-6"/>'),
        mail: svg('<rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 7 9 6 9-6"/>'),
        phone: svg('<path d="M5 3h3l2 5-2.5 1.5a12 12 0 0 0 6 6L15 13l5 2v3a2 2 0 0 1-2 2A16 16 0 0 1 3 5a2 2 0 0 1 2-2z"/>'),
        shield: svg('<path d="M12 3 4 6v6c0 4.5 3.2 7.7 8 9 4.8-1.3 8-4.5 8-9V6z"/><path d="m9 12 2 2 4-4"/>'),
        clock: svg('<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>'),
        check: svg('<path d="m4 12 5 5L20 6"/>'),
        x: svg('<path d="M6 6l12 12M18 6 6 18"/>'),
        file: svg('<path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z"/><path d="M14 3v5h5"/><path d="M9 13h6M9 17h6"/>'),
        results: svg('<path d="M3 3v18h18"/><path d="m7 14 3-3 3 3 4-6"/>'),
        edit: svg('<path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"/>'),
        eye: svg('<path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7z"/><circle cx="12" cy="12" r="3"/>'),
        filter: svg('<path d="M3 4h18l-7 8v6l-4 2v-8z"/>'),
        approvals: svg('<path d="M9 4h6a1 1 0 0 1 1 1v1H8V5a1 1 0 0 1 1-1z"/><path d="M8 6H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-2"/><path d="m9 14 2 2 4-4"/>'),
        extensions: svg('<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M8 3v4M16 3v4M3 10h18"/><path d="M12 14v3l2 1"/>'),
        settings: svg('<circle cx="12" cy="12" r="3.2"/><path d="M12 2v3M12 19v3M4.9 4.9 7 7M17 17l2.1 2.1M2 12h3M19 12h3M4.9 19.1 7 17M17 7l2.1-2.1"/>')
    };

    window.AdminUI = {
        API_BASE: API_BASE,
        guard: guard,
        token: token,
        role: role,
        clearAuth: clearAuth,
        request: request,
        download: download,
        query: query,
        el: el,
        escapeHtml: escapeHtml,
        initials: initials,
        joinName: joinName,
        number: number,
        percent: percent,
        date: date,
        dateTime: dateTime,
        statusPill: statusPill,
        resultPill: resultPill,
        approvalPill: approvalPill,
        progressBar: progressBar,
        tableState: tableState,
        loadingRow: loadingRow,
        toast: toast,
        confirm: confirmDialog,
        renderPager: renderPager,
        setBusy: setBusy,
        showPageMessage: showPageMessage,
        bindIdentity: bindIdentity,
        loadProfile: loadProfile,
        bindChrome: bindChrome,
        icons: ICONS
    };
})();
