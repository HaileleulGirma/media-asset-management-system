(function () {
    // If already logged in, skip straight to the app.
    if (Auth.isLoggedIn()) {
        window.location.href = "/news.html";
        return;
    }

    const form = document.getElementById("login-form");
    const alertEl = document.getElementById("login-alert");
    const submitBtn = document.getElementById("login-submit");

    const passwordInput = document.getElementById("password");
    const passwordToggle = document.getElementById("password-toggle");
    passwordToggle.addEventListener("click", () => {
        const isHidden = passwordInput.type === "password";
        passwordInput.type = isHidden ? "text" : "password";
        passwordToggle.textContent = isHidden ? "Hide" : "Show";
        passwordToggle.setAttribute("aria-label", isHidden ? "Hide password" : "Show password");
    });

    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        hideAlert(alertEl);
        submitBtn.disabled = true;
        submitBtn.textContent = "Signing in...";

        const username = document.getElementById("username").value.trim();
        const password = document.getElementById("password").value;

        try {
            const response = await apiFetch("/api/auth/login", {
                method: "POST",
                body: { username, password },
            });

            Auth.setToken(response.token);
            window.location.href = "/news.html";
        } catch (err) {
            if (err.status === 401 || err.status === 403) {
                showAlert(alertEl, "Invalid username or password.", "error");
            } else {
                showFormErrors(form, err, alertEl);
            }
            submitBtn.disabled = false;
            submitBtn.textContent = "Sign in";
        }
    });
})();