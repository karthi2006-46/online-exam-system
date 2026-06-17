// Signup JS
document.getElementById('signupForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const formData = {
        firstName: document.getElementById('firstName').value,
        lastName: document.getElementById('lastName').value,
        email: document.getElementById('email').value,
        password: document.getElementById('password').value,
        phone: document.getElementById('phone').value,
        address: document.getElementById('address').value,
        roleId: parseInt(document.getElementById('roleId').value),
    };

    try {
        const response = await api.signup(formData);
        if (response.success) {
            alert('Registration successful! Please login.');
            window.location.href = '/login.html';
        } else {
            showError(response.message || 'Signup failed');
        }
    } catch (error) {
        showError('An error occurred during signup');
    }
});

function showError(message) {
    alert(message);
}
