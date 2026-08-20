/**
 * Shared API helper for the MAM frontend.
 * Wraps fetch(): attaches the JWT, redirects to login on 401,
 * and normalizes error handling against the backend's ErrorResponse /
 * field-validation-error shapes.
 */

const TOKEN_KEY = "mam_token";

const Auth = {
    getToken() {
        return sessionStorage.getItem(TOKEN_KEY);
    },

    setToken(token) {
        sessionStorage.setItem(TOKEN_KEY, token);
    },

    clearToken() {
        sessionStorage.removeItem(TOKEN_KEY);
    },

    isLoggedIn() {
        return !!this.getToken();
    },

    /**
     * Decodes the JWT payload client-side to read the "roles" claim
     * for UI display/hiding only. This is NOT a security boundary —
     * the server still enforces @PreAuthorize on every request.
     */
    getRoles() {
        const token = this.getToken();
        if (!token) return [];
        try {
            const payload = JSON.parse(atob(token.split(".")[1]));
            return payload.roles || [];
        } catch (e) {
            return [];
        }
    },

    hasRole(role) {
        return this.getRoles().includes("ROLE_" + role);
    },

    logout() {
        this.clearToken();
        window.location.href = "/index.html";
    },
};

/**
 * ApiError carries the parsed backend error body so callers can
 * distinguish field-validation errors (Map<String,String>) from
 * general ErrorResponse { message, timestamp }.
 */
class ApiError extends Error {
    constructor(status, body) {
        super(typeof body === "object" && body.message ? body.message : "Request failed");
        this.status = status;
        this.body = body;
    }
}

/**
 * Core fetch wrapper. Usage:
 *   const data = await apiFetch("/api/news");
 *   const data = await apiFetch("/api/reporter/create", { method: "POST", body: {...} });
 */
async function apiFetch(url, options = {}) {
    const headers = { ...(options.headers || {}) };

    const token = Auth.getToken();
    if (token) {
        headers["Authorization"] = "Bearer " + token;
    }

    let fetchOptions = { ...options, headers };

    if (options.body && typeof options.body === "object") {
        headers["Content-Type"] = "application/json";
        fetchOptions.body = JSON.stringify(options.body);
    }

    const response = await fetch(url, fetchOptions);

    if (response.status === 401) {
        Auth.clearToken();
        if (!window.location.pathname.endsWith("index.html") && window.location.pathname !== "/") {
            window.location.href = "/index.html";
        }
        throw new ApiError(401, { message: "Session expired. Please log in again." });
    }

    if (response.status === 204) {
        return null;
    }

    const contentType = response.headers.get("content-type") || "";
    const body = contentType.includes("application/json") ? await response.json() : await response.text();

    if (!response.ok) {
        throw new ApiError(response.status, body);
    }

    return body;
}

/**
 * Applies field-level errors from a 400 validation response onto
 * matching <div class="field" data-field="fieldName"> containers.
 * Falls back to a general alert for non-field errors (403, 404, 409, 500).
 */
function showFormErrors(form, error, generalAlertEl) {
    form.querySelectorAll(".field").forEach((f) => {
        f.classList.remove("has-error");
        const errEl = f.querySelector(".field-error");
        if (errEl) errEl.textContent = "";
    });

    if (error.status === 400 && error.body && typeof error.body === "object" && !error.body.message) {
        // field-name -> message map from GlobalExceptionHandler's validation handler
        Object.entries(error.body).forEach(([fieldName, message]) => {
            const fieldEl = form.querySelector(`[data-field="${fieldName}"]`);
            if (fieldEl) {
                fieldEl.classList.add("has-error");
                const errEl = fieldEl.querySelector(".field-error");
                if (errEl) errEl.textContent = message;
                return;
            }
            if (generalAlertEl) showAlert(generalAlertEl, message, "error");
        });
        return;
    }

    const message = (error.body && error.body.message) || error.message || "Something went wrong.";
    if (generalAlertEl) showAlert(generalAlertEl, message, "error");
}

function showAlert(alertEl, message, type = "error") {
    alertEl.textContent = message;
    alertEl.className = "alert visible " + (type === "error" ? "alert-error" : "alert-success");
}

function hideAlert(alertEl) {
    alertEl.className = "alert";
    alertEl.textContent = "";
}