// Authentication JS
document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;

    try {
        const response = await api.login(email, password);
        if (response.success) {
            localStorage.setItem('token', response.token);
            localStorage.setItem('userId', response.userId);
            window.location.href = '/dashboard.html';
        } else {
            showError(response.message || 'Login failed');
        }
    } catch (error) {
        showError('An error occurred during login');
    }
});

function showError(message) {
    alert(message);
}
