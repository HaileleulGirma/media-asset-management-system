(async function () {
    const USER_ENDPOINT = "/api/appuser";
    const ROLE_ENDPOINT = "/api/approles";
    const ADMIN_ROLE_NAME = "ADMIN";
    const ASSIGNABLE_ROLE_NAMES = ["VIEWER", "STAFF"];

    if (!Auth.hasRole("ADMIN")) {
        document.querySelector(".main-content").innerHTML = `
            <div class="panel">
                <div class="alert alert-error visible">You do not have permission to view this page.</div>
            </div>`;
        return;
    }

    function getLoggedInUsername() {
        if (typeof Auth.getUsername === "function") return Auth.getUsername();
        const token = localStorage.getItem("jwt");
        if (!token) return null;
        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            return payload.sub;
        } catch(e) {
            return null;
        }
    }

    const currentUsername = getLoggedInUsername();

    const addBtn = document.getElementById("add-user-btn");
    const actionsHeader = document.getElementById("users-actions-header");
    addBtn.classList.remove("hidden");
    actionsHeader.classList.remove("hidden");

    const listAlert = document.getElementById("users-alert");
    const emptyEl = document.getElementById("users-empty");
    const table = document.getElementById("users-table");
    const tbody = document.getElementById("users-body");

    const PAGE_SIZE = 10;
    let allUsers = [];       // everyone EXCEPT the system admin -- paginated table
    let systemAdmin = null;  // the one admin account, if found -- pinned panel
    let allRoles = [];
    let assignableRoles = [];
    let currentPage = 0;

    const paginationEl = document.getElementById("users-pagination");
    const pageStatus = document.getElementById("users-page-status");
    const prevBtn = document.getElementById("users-page-prev");
    const nextBtn = document.getElementById("users-page-next");

    // A small pinned panel for the system administrator, inserted above
    // the regular users panel. Built once here; content filled in by
    // renderAdminPanel() once we know who the admin is.
    const adminPanel = document.createElement("div");
    adminPanel.className = "panel admin-pinned-panel hidden";
    adminPanel.innerHTML = `
        <span class="admin-pinned-badge">System Administrator</span>
        <div class="admin-pinned-row">
            <div>
                <div class="admin-pinned-name" id="admin-pinned-name"></div>
                <div class="admin-pinned-username" id="admin-pinned-username"></div>
            </div>
            <button type="button" class="btn btn-secondary btn-sm" id="admin-edit-btn">Edit</button>
        </div>
    `;
    document.querySelector(".main-content").insertBefore(adminPanel, document.querySelector(".main-content .panel"));

    // Declared here (rather than down with the rest of the modal refs) because
    // loadRoles() runs next and calls rebuildRoleOptions(), which needs
    // roleSelect to already be initialized.
    const roleSelect = document.getElementById("userRole");
    const roleHint = document.getElementById("role-hint");

    try {
        await loadRoles();
        await loadUsers();
    } catch (err) {
        showAlert(listAlert, "Failed to initialize page: " + (err.body?.message || err.message), "error");
    }

    async function loadRoles() {
        allRoles = await apiFetch(ROLE_ENDPOINT);
        // Allow-list, not exclude-list: only these exact roles are ever
        // offered, regardless of what else exists in the roles table.
        assignableRoles = allRoles.filter(role => ASSIGNABLE_ROLE_NAMES.includes(role.roleName));

        // Base population; rebuildRoleOptions() is called again (with an
        // optional extra role) each time the modal is opened, so this
        // just makes sure the <select> isn't empty before that happens.
        rebuildRoleOptions();
    }

    // Rebuilds the #userRole <select> from the assignable-roles allow-list.
    // If extraRole is passed and isn't already in that allow-list (e.g. the
    // system admin's ADMIN role), it's appended as an extra option so the
    // select can correctly display/select it for THIS modal open only.
    // Called fresh every time the modal opens so a non-assignable role
    // never lingers as a selectable choice for Add-user or for editing a
    // different, regular user.
    function rebuildRoleOptions(extraRole) {
        roleSelect.innerHTML = '<option value="">Select a role...</option>';
        assignableRoles.forEach(role => {
            const opt = document.createElement("option");
            opt.value = role.roleId;
            opt.textContent = role.roleName.replace("ROLE_", "");
            roleSelect.appendChild(opt);
        });
        if (extraRole && !assignableRoles.some(r => r.roleId === extraRole.roleId)) {
            const opt = document.createElement("option");
            opt.value = extraRole.roleId;
            opt.textContent = extraRole.roleName.replace("ROLE_", "");
            roleSelect.appendChild(opt);
        }
    }

    function roleNameFor(user) {
        const roleObj = allRoles.find(r => r.roleId === user.role);
        return roleObj ? roleObj.roleName : "";
    }

    async function loadUsers() {
        hideAlert(listAlert);
        try {
            const users = await apiFetch(`${USER_ENDPOINT}/`);

            systemAdmin = users.find(u => roleNameFor(u) === ADMIN_ROLE_NAME) || null;
            allUsers = users.filter(u => u !== systemAdmin);

            renderAdminPanel();
            currentPage = 0;
            renderPage();
        } catch (err) {
            showAlert(listAlert, "Could not load users: " + (err.body?.message || err.message), "error");
        }
    }

    // ---------- Pinned admin panel ----------

    function renderAdminPanel() {
        if (!systemAdmin) {
            adminPanel.classList.add("hidden");
            return;
        }
        adminPanel.classList.remove("hidden");
        document.getElementById("admin-pinned-name").textContent = systemAdmin.fullname || "";
        document.getElementById("admin-pinned-username").textContent = "@" + (systemAdmin.username || "");

        const editBtn = document.getElementById("admin-edit-btn");
        // Replace to clear any previously attached listener before re-adding.
        const freshEditBtn = editBtn.cloneNode(true);
        editBtn.parentNode.replaceChild(freshEditBtn, editBtn);
        freshEditBtn.addEventListener("click", () => openEditModal(systemAdmin, true));
        // Note: there is deliberately no delete button anywhere in this
        // panel's markup -- not hidden, never created.
    }

    // ---------- Pagination & Rendering (everyone except the admin) ----------

    prevBtn.addEventListener("click", () => {
        if (currentPage > 0) {
            currentPage -= 1;
            renderPage();
        }
    });

    nextBtn.addEventListener("click", () => {
        if (currentPage < totalPages() - 1) {
            currentPage += 1;
            renderPage();
        }
    });

    function totalPages() {
        return Math.max(1, Math.ceil(allUsers.length / PAGE_SIZE));
    }

    function renderPage() {
        if (allUsers.length === 0) {
            table.classList.add("hidden");
            emptyEl.classList.remove("hidden");
            paginationEl.classList.add("hidden");
            return;
        }

        emptyEl.classList.add("hidden");
        table.classList.remove("hidden");

        const start = currentPage * PAGE_SIZE;
        const pageItems = allUsers.slice(start, start + PAGE_SIZE);
        renderUsers(pageItems);

        const pages = totalPages();
        paginationEl.classList.toggle("hidden", pages <= 1);
        pageStatus.textContent = `Page ${currentPage + 1} of ${pages}`;
        prevBtn.disabled = currentPage === 0;
        nextBtn.disabled = currentPage >= pages - 1;
    }

    function renderUsers(users) {
        tbody.innerHTML = "";

        // systemAdmin never reaches this function -- it was filtered out
        // in loadUsers() -- so there is no isSystemAdmin check needed
        // here at all, and no way for a delete button to leak onto it.
        users.forEach((u) => {
            const roleObj = allRoles.find(r => r.roleId === u.role);
            const roleLabel = roleObj ? roleObj.roleName.replace("ROLE_", "") : "Unknown";

            const row = document.createElement("tr");
            const isSelf = currentUsername && u.username === currentUsername;

            row.innerHTML = `
                <td>${escapeHtml(u.fullname ?? "")} ${isSelf ? `<span class="role-badge" style="background:#e0f2fe; color:#0369a1;">You</span>` : ""}</td>
                <td>${escapeHtml(u.username ?? "")}</td>
                <td><span class="role-badge">${escapeHtml(roleLabel)}</span></td>
                <td>
                    <div class="row-actions">
                        <button type="button" class="btn btn-secondary btn-sm edit-btn">Edit</button>
                        ${!isSelf ? `<button type="button" class="btn btn-danger btn-sm delete-btn">Delete</button>` : `<div style="width: 55px"></div>`}
                    </div>
                </td>
            `;

            row.querySelector(".edit-btn").addEventListener("click", () => openEditModal(u, isSelf));

            if (!isSelf) {
                row.querySelector(".delete-btn").addEventListener("click", () => deleteUser(u.id));
            }

            tbody.appendChild(row);
        });
    }

    async function deleteUser(id) {
        if (!confirm("Are you sure you want to permanently delete this user?")) return;
        try {
            await apiFetch(`${USER_ENDPOINT}/${id}`, { method: "DELETE" });
            showAlert(listAlert, "User deleted.", "success");
            loadUsers();
        } catch (err) {
            showAlert(listAlert, "Failed to delete: " + (err.body?.message || err.message), "error");
        }
    }

    // ---------- Modal Interactions ----------

    const modalOverlay = document.getElementById("user-modal-overlay");
    const modalTitle = document.getElementById("user-modal-title");
    const modalAlert = document.getElementById("user-modal-alert");
    const form = document.getElementById("user-form");
    const submitBtn = document.getElementById("user-form-submit");
    const cancelBtn = document.getElementById("user-form-cancel");
    const closeBtn = document.getElementById("user-modal-close");

    const pwInput = document.getElementById("password");
    const pwToggle = document.getElementById("password-toggle");
    const pwHint = document.getElementById("password-hint");

    const pwConfirmInput = document.getElementById("confirmPassword");
    const pwConfirmToggle = document.getElementById("confirm-password-toggle");

    const usernameInput = document.getElementById("username");
    const usernameHint = document.getElementById("username-hint");

    addBtn.addEventListener("click", openAddModal);

    pwToggle.addEventListener("click", () => {
        if (pwInput.type === "password") {
            pwInput.type = "text";
            pwToggle.textContent = "Hide";
        } else {
            pwInput.type = "password";
            pwToggle.textContent = "Show";
        }
    });

    pwConfirmToggle.addEventListener("click", () => {
        if (pwConfirmInput.type === "password") {
            pwConfirmInput.type = "text";
            pwConfirmToggle.textContent = "Hide";
        } else {
            pwConfirmInput.type = "password";
            pwConfirmToggle.textContent = "Show";
        }
    });

    function clearFieldErrors() {
        form.querySelectorAll(".field").forEach((f) => {
            f.classList.remove("has-error");
            const errEl = f.querySelector(".field-error");
            if (errEl) errEl.textContent = "";
        });
    }

    function openAddModal() {
        form.reset();
        clearFieldErrors();
        hideAlert(modalAlert);

        document.getElementById("userId").value = "";
        modalTitle.textContent = "Add user";

        pwInput.type = "password";
        pwToggle.textContent = "Show";
        pwHint.textContent = "Required. Must be at least 4 characters.";
        pwInput.required = true;

        pwConfirmInput.type = "password";
        pwConfirmToggle.textContent = "Show";
        pwConfirmInput.required = true;

        usernameInput.disabled = false;
        usernameHint.textContent = "";

        rebuildRoleOptions();
        roleSelect.disabled = false;
        roleHint.textContent = "";

        modalOverlay.classList.remove("hidden");
    }

    function openEditModal(user, isSelf) {
        form.reset();
        clearFieldErrors();
        hideAlert(modalAlert);

        document.getElementById("userId").value = user.id;
        document.getElementById("fullname").value = user.fullname || "";
        usernameInput.value = user.username || "";

        const roleObj = allRoles.find(r => r.roleId === user.role);
        rebuildRoleOptions(roleObj);
        roleSelect.value = user.role;

        pwInput.type = "password";
        pwToggle.textContent = "Show";
        pwHint.textContent = "Leave blank to keep current password.";
        pwInput.required = false;

        pwConfirmInput.type = "password";
        pwConfirmToggle.textContent = "Show";
        pwConfirmInput.required = false;

        if (isSelf) {
            modalTitle.textContent = "Edit Your Profile";

            usernameInput.disabled = true;
            usernameHint.textContent = "You cannot change your active username.";

            roleSelect.disabled = true;
            roleHint.textContent = "You cannot change your own role.";
        } else {
            modalTitle.textContent = "Edit user";

            usernameInput.disabled = false;
            usernameHint.textContent = "";

            roleSelect.disabled = false;
            roleHint.textContent = "";
        }

        modalOverlay.classList.remove("hidden");
    }

    function closeModal() {
        modalOverlay.classList.add("hidden");
    }

    closeBtn.addEventListener("click", closeModal);
    cancelBtn.addEventListener("click", closeModal);
    modalOverlay.addEventListener("click", (e) => {
        if (e.target === modalOverlay) closeModal();
    });
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && !modalOverlay.classList.contains("hidden")) closeModal();
    });

    // ---------- Form Submission ----------

    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        hideAlert(modalAlert);
        clearFieldErrors();

        const id = document.getElementById("userId").value;
        const passwordValue = pwInput.value;
        const confirmValue = pwConfirmInput.value;

        if (!id && passwordValue.trim().length < 4) {
            const pwField = pwInput.closest(".field");
            pwField.classList.add("has-error");
            pwField.querySelector(".field-error").textContent = "Password must be at least 4 characters long.";
            return;
        }

        // Whenever a password is actually being set -- new user, or an
        // edit where the password field was filled in -- the confirm
        // field must match it exactly.
        if (passwordValue && passwordValue !== confirmValue) {
            const confirmField = pwConfirmInput.closest(".field");
            confirmField.classList.add("has-error");
            confirmField.querySelector(".field-error").textContent = "Passwords do not match.";
            return;
        }

        submitBtn.disabled = true;
        submitBtn.textContent = "Saving...";

        const payload = {
            fullName: document.getElementById("fullname").value.trim(),
            username: document.getElementById("username").value.trim(),
            role: parseInt(roleSelect.value, 10)
        };

        if (passwordValue) {
            payload.password = passwordValue;
        }

        try {
            if (id) {
                await apiFetch(`${USER_ENDPOINT}/${id}`, { method: "PUT", body: payload });
            } else {
                await apiFetch(`${USER_ENDPOINT}/create`, { method: "POST", body: payload });
            }
            closeModal();
            loadUsers();
        } catch (err) {
            showFormErrors(form, err, modalAlert);
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = "Save";
        }
    });

})();