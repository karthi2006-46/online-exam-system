// ==========================================
// LMS FORGOT PASSWORD
// ==========================================

const emailForm = document.getElementById("emailForm");
const otpForm = document.getElementById("otpForm");
const resetForm = document.getElementById("resetForm");

const emailInput = document.getElementById("email");
const otpInput = document.getElementById("otp");
const newPasswordInput = document.getElementById("newPassword");
const confirmPasswordInput = document.getElementById("confirmPassword");

const messageBox = document.getElementById("messageBox");
const stepTitle = document.getElementById("stepTitle");
const stepDescription = document.getElementById("stepDescription");
const emailPreview = document.getElementById("emailPreview");

const sendOtpButton = document.getElementById("sendOtpButton");
const resendOtpButton = document.getElementById("resendOtpButton");
const changeEmailButton = document.getElementById("changeEmailButton");
const resetButton = document.getElementById("resetButton");

const strengthMeter = document.getElementById("strengthMeter");
const strengthLabel = document.getElementById("strengthLabel");
const matchText = document.getElementById("matchText");
const ruleItems = document.querySelectorAll("#passwordRules li");

const RESEND_SECONDS = 30;

let verified = false;
let resendTimer = null;


// ==========================================
// MESSAGES
// ==========================================

function showMessage(message, type) {
    messageBox.textContent = message;
    messageBox.className = "fp-message " + type;
    messageBox.hidden = false;
}

function clearMessage() {
    messageBox.textContent = "";
    messageBox.hidden = true;
}


// ==========================================
// BUTTON LOADING STATE
// ==========================================

function setLoading(button, isLoading, loadingText) {
    const text = button.querySelector(".fp-btn-text");

    if (isLoading) {
        button.dataset.originalText = text.textContent;
        text.textContent = loadingText;
        button.classList.add("is-loading");
        button.disabled = true;
    } else {
        text.textContent = button.dataset.originalText || text.textContent;
        button.classList.remove("is-loading");
        button.disabled = false;
    }
}


// ==========================================
// STEP NAVIGATION
// ==========================================

const STEP_TEXT = {
    1: {
        title: "Reset your password",
        description: "Enter your registered email address and we'll send you a 6-digit code."
    },
    2: {
        title: "Check your email",
        description: "Enter the 6-digit code we sent to your email address."
    },
    3: {
        title: "Create a new password",
        description: "Choose a strong password that you don't use anywhere else."
    }
};

function showStep(step, keepMessage) {

    emailForm.hidden = step !== 1;
    otpForm.hidden = step !== 2;
    resetForm.hidden = step !== 3;

    stepTitle.textContent = STEP_TEXT[step].title;
    stepDescription.textContent = STEP_TEXT[step].description;

    document.querySelectorAll(".fp-step").forEach(function (item) {
        const n = Number(item.dataset.step);
        item.classList.toggle("is-active", n === step);
        item.classList.toggle("is-done", n < step);
    });

    if (!keepMessage) {
        clearMessage();
    }

    // Focus the first field of the new step
    const focusTarget = { 1: emailInput, 2: otpInput, 3: newPasswordInput }[step];
    setTimeout(function () { focusTarget.focus(); }, 50);
}


// ==========================================
// API REQUEST
// ==========================================

async function sendRequest(url, data) {

    const response = await fetch(url, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(data)
    });

    const result = await response.json().catch(function () { return {}; });

    if (!response.ok || result.success === false) {
        throw new Error(
            result.message || "Something went wrong. Please try again."
        );
    }

    return result;
}


// ==========================================
// RESEND COOLDOWN
// ==========================================

function startResendCooldown() {

    let remaining = RESEND_SECONDS;
    const text = resendOtpButton.querySelector(".fp-btn-text");

    clearInterval(resendTimer);
    resendOtpButton.disabled = true;
    text.textContent = "Resend code in " + remaining + "s";

    resendTimer = setInterval(function () {

        remaining -= 1;

        if (remaining <= 0) {
            clearInterval(resendTimer);
            resendOtpButton.disabled = false;
            text.textContent = "Resend code";
            return;
        }

        text.textContent = "Resend code in " + remaining + "s";

    }, 1000);
}


// ==========================================
// STEP 1: SEND OTP
// ==========================================

emailForm.addEventListener("submit", async function (e) {

    e.preventDefault();
    clearMessage();

    const email = emailInput.value.trim().toLowerCase();

    if (!emailInput.checkValidity() || email === "") {
        emailInput.classList.add("is-invalid");
        showMessage("Enter a valid email address, like name@example.com.", "error");
        return;
    }

    emailInput.classList.remove("is-invalid");
    setLoading(sendOtpButton, true, "Sending code...");

    try {

        await sendRequest("/api/auth/forgot-password", { email: email });

        emailPreview.textContent = email;
        otpInput.value = "";

        showStep(2, true);
        startResendCooldown();

        showMessage(
            "If an account exists for this email, a verification code is on its way.",
            "success"
        );

    } catch (error) {

        showMessage(error.message, "error");

    } finally {

        setLoading(sendOtpButton, false);
    }
});

emailInput.addEventListener("input", function () {
    emailInput.classList.remove("is-invalid");
});


// ==========================================
// STEP 2: VERIFY OTP (format check only)
// ==========================================

otpInput.addEventListener("input", function () {
    // Allow digits only
    otpInput.value = otpInput.value.replace(/\D/g, "").slice(0, 6);
    otpInput.classList.remove("is-invalid");
});

otpForm.addEventListener("submit", function (e) {

    e.preventDefault();
    clearMessage();

    const otp = otpInput.value.trim();

    if (!/^\d{6}$/.test(otp)) {
        otpInput.classList.add("is-invalid");
        showMessage("Enter the 6-digit code from your email.", "error");
        return;
    }

    // This checks only the OTP format.
    // Actual OTP verification happens during password reset.
    verified = true;

    showStep(3);
});


// ==========================================
// RESEND OTP
// ==========================================

resendOtpButton.addEventListener("click", async function () {

    clearMessage();

    const text = resendOtpButton.querySelector(".fp-btn-text");

    resendOtpButton.disabled = true;
    text.textContent = "Sending...";

    try {

        await sendRequest(
            "/api/auth/forgot-password",
            { email: emailInput.value.trim().toLowerCase() }
        );

        otpInput.value = "";
        otpInput.focus();

        showMessage(
            "If an account exists for this email, a new code has been sent.",
            "success"
        );

        startResendCooldown();

    } catch (error) {

        showMessage(error.message, "error");

        resendOtpButton.disabled = false;
        text.textContent = "Resend code";
    }
});

changeEmailButton.addEventListener("click", function () {
    clearInterval(resendTimer);
    verified = false;
    showStep(1);
});


// ==========================================
// SHOW / HIDE PASSWORD
// ==========================================

document.querySelectorAll(".fp-toggle").forEach(function (toggle) {

    toggle.addEventListener("click", function () {

        const input = document.getElementById(toggle.dataset.target);
        const show = input.type === "password";

        input.type = show ? "text" : "password";

        toggle.setAttribute("aria-pressed", String(show));
        toggle.setAttribute("aria-label", show ? "Hide password" : "Show password");

        input.focus();
    });
});


// ==========================================
// PASSWORD STRENGTH
// ==========================================

const PASSWORD_RULES = {
    length: function (p) { return p.length >= 8; },
    lower: function (p) { return /[a-z]/.test(p); },
    upper: function (p) { return /[A-Z]/.test(p); },
    number: function (p) { return /\d/.test(p); },
    special: function (p) { return /[^A-Za-z0-9]/.test(p); }
};

const STRENGTH_LABELS = ["Enter a password", "Weak", "Fair", "Good", "Strong"];

function evaluatePassword(password) {

    let metCount = 0;
    const results = {};

    Object.keys(PASSWORD_RULES).forEach(function (rule) {
        results[rule] = PASSWORD_RULES[rule](password);
        if (results[rule]) metCount += 1;
    });

    // Level 0-4
    let level = 0;

    if (password.length > 0) {
        if (metCount <= 2) level = 1;
        else if (metCount === 3) level = 2;
        else if (metCount === 4) level = 3;
        else level = password.length >= 12 ? 4 : 3;   // all rules met; 12+ chars = Strong
    }

    return {
        level: level,
        results: results,
        allMet: metCount === Object.keys(PASSWORD_RULES).length
    };
}

function updatePasswordUI() {

    const password = newPasswordInput.value;
    const confirm = confirmPasswordInput.value;
    const evaluation = evaluatePassword(password);

    // Meter
    strengthMeter.dataset.level = evaluation.level;
    strengthLabel.textContent = STRENGTH_LABELS[evaluation.level];

    // Checklist
    ruleItems.forEach(function (item) {
        item.classList.toggle("is-met", evaluation.results[item.dataset.rule]);
    });

    // Match message
    if (confirm.length === 0) {
        matchText.textContent = "";
        matchText.className = "fp-match";
        confirmPasswordInput.classList.remove("is-valid", "is-invalid");
    } else if (password === confirm) {
        matchText.textContent = "Passwords match.";
        matchText.className = "fp-match ok";
        confirmPasswordInput.classList.add("is-valid");
        confirmPasswordInput.classList.remove("is-invalid");
    } else {
        matchText.textContent = "Passwords don't match yet.";
        matchText.className = "fp-match bad";
        confirmPasswordInput.classList.add("is-invalid");
        confirmPasswordInput.classList.remove("is-valid");
    }

    // Enable submit only for a strong, matching password
    resetButton.disabled = !(evaluation.allMet && password === confirm && confirm.length > 0);
}

newPasswordInput.addEventListener("input", updatePasswordUI);
confirmPasswordInput.addEventListener("input", updatePasswordUI);


// ==========================================
// STEP 3: RESET PASSWORD
// ==========================================

resetForm.addEventListener("submit", async function (e) {

    e.preventDefault();
    clearMessage();

    const email = emailInput.value.trim().toLowerCase();
    const otp = otpInput.value.trim();
    const newPassword = newPasswordInput.value;
    const confirmPassword = confirmPasswordInput.value;

    if (!verified) {
        showMessage("Enter your verification code first.", "error");
        showStep(2, true);
        return;
    }

    if (!evaluatePassword(newPassword).allMet) {
        showMessage("Your password doesn't meet all the requirements yet.", "error");
        return;
    }

    if (newPassword !== confirmPassword) {
        showMessage("Passwords don't match.", "error");
        return;
    }

    setLoading(resetButton, true, "Resetting password...");

    try {

        await sendRequest(
            "/api/auth/reset-password",
            {
                email: email,
                otp: otp,
                newPassword: newPassword,
                confirmPassword: confirmPassword
            }
        );

        verified = false;
        clearInterval(resendTimer);

        emailForm.reset();
        otpForm.reset();
        resetForm.reset();
        updatePasswordUI();

        showStep(1);

        showMessage(
            "Password reset. You can now log in with your new password.",
            "success"
        );

    } catch (error) {

        showMessage(error.message, "error");

        // Return to code entry if the code was rejected or expired.
        const msg = error.message.toLowerCase();

        if (msg.includes("otp") || msg.includes("code") || msg.includes("expired")) {
            verified = false;
            otpInput.value = "";
            showStep(2, true);
        }

    } finally {

        setLoading(resetButton, false);
        updatePasswordUI();
    }
});

// Initial state
showStep(1);
updatePasswordUI();