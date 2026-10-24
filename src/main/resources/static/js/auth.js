// Authentication JS (login)
const loginForm = document.getElementById('loginForm');
const loginButton = loginForm.querySelector('button[type="submit"]');

loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    authUI.clearMessage();

    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;

    let redirecting = false;
    authUI.setLoading(loginButton, true, 'Signing in');

    try {
        const response = await api.login(email, password);

        if (response.success) {
            // Save authentication details
            localStorage.setItem('token', response.token);
            localStorage.setItem('userId', response.userId);

            // Normalize the role
            const role = String(response.role || '')
                .replace(/^ROLE_/i, '')
                .toUpperCase();

            localStorage.setItem('role', role);

            // Redirect based on the user's role
            if (role === 'STUDENT') {
                redirecting = true;
                window.location.href = '/student/studentdashboard.html';

            } else if (role === 'FACULTY') {
                redirecting = true;
                window.location.href = '/faculty/facultydashboard.html';

            } else if (role === 'ADMIN') {
                redirecting = true;
                window.location.href = '/admin/admindashboard.html';

            } else {
                localStorage.removeItem('token');
                localStorage.removeItem('userId');
                localStorage.removeItem('role');

                showError('Invalid user role. Please contact the administrator.');
            }

        } else {
            showError(response.message || 'Login failed');
        }
    } catch (error) {
        showError('An error occurred during login');
    } finally {
        // Keep the spinner while the browser is navigating away
        if (!redirecting) authUI.setLoading(loginButton, false);
    }
});

function showError(message) {
    authUI.showMessage('error', message);
}