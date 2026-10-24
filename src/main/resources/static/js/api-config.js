// Shared API configuration for localhost and ngrok
window.API_BASE_URL = (() => {
    const hostname = window.location.hostname;

    if (hostname.endsWith("ngrok-free.dev")) {
        return window.location.origin;
    }

    return "http://localhost:8080";
})();

console.log("API Base URL:", window.API_BASE_URL);