// Shared UI helpers for login + signup:
// show/hide password, Caps Lock warning, inline messages, button loading state.
(function () {
    "use strict";

    var ERROR_ICON =
        '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M12 8v5M12 16h.01"/></svg>';
    var SUCCESS_ICON =
        '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="m8.5 12.5 2.5 2.5 4.5-5"/></svg>';

    /* ---------- Show / hide password ---------- */
    document.querySelectorAll(".toggle-visibility").forEach(function (btn) {
        btn.addEventListener("click", function () {
            var input = document.getElementById(btn.dataset.target);
            if (!input) return;

            var show = input.type === "password";
            input.type = show ? "text" : "password";
            btn.setAttribute("aria-pressed", String(show));
            btn.setAttribute("aria-label", (show ? "Hide" : "Show") + " password");
            input.focus();
        });
    });

    /* ---------- Caps Lock warning ---------- */
    document.querySelectorAll(".password-field input").forEach(function (input) {
        var hint = document.createElement("div");
        hint.className = "caps-hint";
        hint.textContent = "Caps Lock is on";
        input.closest(".field").insertAdjacentElement("afterend", hint);

        function check(e) {
            var on = e.getModifierState && e.getModifierState("CapsLock");
            hint.classList.toggle("is-visible", !!on);
        }
        input.addEventListener("keydown", check);
        input.addEventListener("keyup", check);
        input.addEventListener("blur", function () { hint.classList.remove("is-visible"); });
    });

    /* ---------- Inline messages (replace alert) ---------- */
    var box = document.getElementById("authMessage");

    function showMessage(type, text) {
        if (!box) { alert(text); return; }

        box.className = "auth-alert " + (type === "success" ? "is-success" : "is-error");
        box.innerHTML = (type === "success" ? SUCCESS_ICON : ERROR_ICON) + "<span></span>";
        box.lastChild.textContent = text; // textContent: safe against HTML in server messages
        box.hidden = false;

        if (type !== "success") {
            void box.offsetWidth; // restart the shake animation
            box.classList.add("shake");
        }
        box.scrollIntoView({ behavior: "smooth", block: "nearest" });
    }

    function clearMessage() {
        if (box) { box.hidden = true; box.textContent = ""; }
    }

    /* ---------- Button loading state ---------- */
    function setLoading(button, loading, loadingText) {
        if (!button) return;
        var label = button.querySelector(".btn-label");

        if (loading) {
            if (label && !button.dataset.label) button.dataset.label = label.textContent;
            if (label && loadingText) label.textContent = loadingText;
        } else if (label && button.dataset.label) {
            label.textContent = button.dataset.label;
        }

        button.disabled = loading;
        button.classList.toggle("is-loading", loading);
        button.setAttribute("aria-busy", String(loading));
    }

    window.authUI = { showMessage: showMessage, clearMessage: clearMessage, setLoading: setLoading };
})();