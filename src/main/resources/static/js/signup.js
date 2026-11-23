// Signup JS
const signupForm = document.getElementById('signupForm');
const signupButton = signupForm.querySelector('button[type="submit"]');

const passwordInput = document.getElementById('password');
const confirmInput = document.getElementById('confirmPassword');
const confirmHint = document.getElementById('confirmHint');
const strengthMeter = document.getElementById('strengthMeter');
const strengthLabel = document.getElementById('strengthLabel');
const ruleItems = document.querySelectorAll('#passwordRules li');
const courseSelect = document.getElementById('courseId');

/* ---------- Course list ---------- */
async function loadCourses() {
    try {
        const response = await api.getCourses();
        const courses = Array.isArray(response) ? response : (response && response.data);

        if (!Array.isArray(courses) || courses.length === 0) return;

        courseSelect.innerHTML =
            '<option value="" selected disabled>Select a course</option>' +
            courses.map((course) =>
                '<option value="' + course.id + '">' +
                escapeHtml(course.title || ('Course ' + course.id)) +
                '</option>'
            ).join('');
    } catch (error) {
        // Keep the static fallback list already present in the HTML.
        console.warn('Could not load courses, using default list.', error);
    }
}

function escapeHtml(value) {
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

loadCourses();

/* ---------- Password rules ---------- */
const PASSWORD_RULES = {
    length:  (p) => p.length >= 8,
    upper:   (p) => /[A-Z]/.test(p),
    lower:   (p) => /[a-z]/.test(p),
    number:  (p) => /[0-9]/.test(p),
    special: (p) => /[^A-Za-z0-9\s]/.test(p)
};

function evaluatePassword() {
    const value = passwordInput.value;
    let met = 0;

    ruleItems.forEach((item) => {
        const ok = PASSWORD_RULES[item.dataset.rule](value);
        item.classList.toggle('is-met', ok);
        if (ok) met++;
    });

    // 0-5 rules met  ->  strength level 0-4
    let level = 0;
    if (value.length > 0) {
        if (met <= 2) level = 1;
        else if (met === 3) level = 2;
        else if (met === 4) level = 3;
        else level = 4;
    }

    const labels = ['Enter a password', 'Weak', 'Fair', 'Good', 'Strong'];
    strengthMeter.dataset.level = level;
    strengthLabel.textContent = labels[level];

    return met === ruleItems.length;
}

function checkConfirm() {
    if (!confirmInput.value) {
        confirmHint.textContent = '';
        confirmHint.className = 'field-hint';
        return false;
    }

    const same = confirmInput.value === passwordInput.value;
    confirmHint.textContent = same ? 'Passwords match' : 'Incorrect password';
    confirmHint.className = 'field-hint ' + (same ? 'is-ok' : 'is-bad');
    return same;
}

passwordInput.addEventListener('input', () => { evaluatePassword(); checkConfirm(); });
confirmInput.addEventListener('input', checkConfirm);

/* ---------- Submit ---------- */
signupForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    authUI.clearMessage();

    // Browser validation for required fields / email format
    if (!signupForm.checkValidity()) {
        signupForm.reportValidity();
        return;
    }

    // Password strength
    if (!evaluatePassword()) {
        showError('Please choose a stronger password. Meet all the requirements listed below the password field.');
        passwordInput.focus();
        return;
    }

    // Confirm password
    if (!checkConfirm()) {
        showError('Incorrect password.');
        confirmInput.focus();
        return;
    }

    const courseId = parseInt(courseSelect.value, 10);

    if (!Number.isInteger(courseId)) {
        showError('Please select a course.');
        courseSelect.focus();
        return;
    }

    // No roleId is sent: the server always creates a STUDENT account.
    const formData = {
        firstName: document.getElementById('firstName').value.trim(),
        lastName: document.getElementById('lastName').value.trim(),
        email: document.getElementById('email').value.trim(),
        password: passwordInput.value,
        phone: document.getElementById('phone').value.trim(),
        address: document.getElementById('address').value.trim(),
        courseId: courseId
    };

    let done = false;
    authUI.setLoading(signupButton, true, 'Creating account');

    try {
        const response = await api.signup(formData);

        if (response.success) {
            done = true;
            authUI.showMessage('success', 'Registration successful! Redirecting you to login...');
            setTimeout(() => { window.location.href = '/login.html'; }, 1800);
        } else {
            showError(response.message || 'Signup failed');
        }
    } catch (error) {
        showError('An error occurred during signup');
    } finally {
        if (!done) authUI.setLoading(signupButton, false);
    }
});

function showError(message) {
    authUI.showMessage('error', message);
}