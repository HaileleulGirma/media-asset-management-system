(function () {
    const STAFF_ENDPOINT = "/api/staffmember";
    const isAdmin = Auth.hasRole("ADMIN");

    const addBtn = document.getElementById("add-staff-btn");
    const actionsHeader = document.getElementById("staff-actions-header");
    if (isAdmin) {
        addBtn.classList.remove("hidden");
        actionsHeader.classList.remove("hidden");
    }

    const activeOnlyCheckbox = document.getElementById("staffActiveOnly");
    const listAlert = document.getElementById("staff-alert");
    const emptyEl = document.getElementById("staff-empty");
    const table = document.getElementById("staff-table");
    const tbody = document.getElementById("staff-body");

    const PAGE_SIZE = 10;
    let allStaff = [];
    let currentPage = 0;

    const paginationEl = document.getElementById("staff-pagination");
    const pageStatus = document.getElementById("staff-page-status");
    const prevBtn = document.getElementById("staff-page-prev");
    const nextBtn = document.getElementById("staff-page-next");

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
        return Math.max(1, Math.ceil(allStaff.length / PAGE_SIZE));
    }

    activeOnlyCheckbox.addEventListener("change", loadStaff);
    loadStaff();

    async function loadStaff() {
        hideAlert(listAlert);
        try {
            allStaff = await apiFetch(`${STAFF_ENDPOINT}?activeOnly=${activeOnlyCheckbox.checked}`);
            currentPage = 0;
            renderPage();
        } catch (err) {
            showAlert(listAlert, "Could not load staff: " + (err.body?.message || err.message), "error");
        }
    }

    function renderPage() {
        if (allStaff.length === 0) {
            table.classList.add("hidden");
            emptyEl.classList.remove("hidden");
            paginationEl.classList.add("hidden");
            return;
        }

        emptyEl.classList.add("hidden");
        table.classList.remove("hidden");

        const start = currentPage * PAGE_SIZE;
        const pageItems = allStaff.slice(start, start + PAGE_SIZE);
        renderStaff(pageItems);

        const pages = totalPages();
        paginationEl.classList.toggle("hidden", pages <= 1);
        pageStatus.textContent = `Page ${currentPage + 1} of ${pages}`;
        prevBtn.disabled = currentPage === 0;
        nextBtn.disabled = currentPage >= pages - 1;
    }

    function renderStaff(staff) {
        tbody.innerHTML = "";

        staff.forEach((s) => {
            const row = document.createElement("tr");
            const statusBadge = s.isActive
                ? '<span class="badge-active">Active</span>'
                : '<span class="badge-inactive">Inactive</span>';

            row.innerHTML = `
                <td>${escapeHtml(s.staffMemberName ?? "")}</td>
                <td>${statusBadge}</td>
                ${isAdmin ? `
                <td>
                    <div class="row-actions">
                        <button type="button" class="btn btn-secondary btn-sm edit-btn">Edit</button>
                        <button type="button" class="btn btn-danger btn-sm delete-btn">Delete</button>
                    </div>
                </td>` : ""}
            `;

            if (isAdmin) {
                row.querySelector(".edit-btn").addEventListener("click", () => openEditModal(s));
                row.querySelector(".delete-btn").addEventListener("click", () => deleteStaff(s.staffMemberId));
            }

            tbody.appendChild(row);
        });
    }

    async function deleteStaff(id) {
        if (!confirm("Are you sure you want to permanently delete this staff member?")) return;
        try {
            await apiFetch(`${STAFF_ENDPOINT}/${id}`, { method: "DELETE" });
            showAlert(listAlert, "Staff member deleted.", "success");
            loadStaff();
        } catch (err) {
            showAlert(listAlert, "Failed to delete: " + (err.body?.message || err.message), "error");
        }
    }

    // ---------- Add/Edit modal ----------

    if (!isAdmin) return; // Staff/Viewer never reach the form itself

    const modalOverlay = document.getElementById("staff-modal-overlay");
    const modalTitle = document.getElementById("staff-modal-title");
    const modalAlert = document.getElementById("staff-modal-alert");
    const form = document.getElementById("staff-form");
    const submitBtn = document.getElementById("staff-form-submit");
    const cancelBtn = document.getElementById("staff-form-cancel");
    const closeBtn = document.getElementById("staff-modal-close");

    addBtn.addEventListener("click", openAddModal);

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
        document.getElementById("staffMemberId").value = "";
        document.getElementById("staffIsActive").checked = true;
        modalTitle.textContent = "Add staff member";
        modalOverlay.classList.remove("hidden");
    }

    function openEditModal(staff) {
        form.reset();
        clearFieldErrors();
        hideAlert(modalAlert);
        document.getElementById("staffMemberId").value = staff.staffMemberId;
        document.getElementById("staffMemberName").value = staff.staffMemberName || "";
        document.getElementById("staffIsActive").checked = !!staff.isActive;
        modalTitle.textContent = "Edit staff member";
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

    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        hideAlert(modalAlert);
        clearFieldErrors();
        submitBtn.disabled = true;
        submitBtn.textContent = "Saving...";

        const id = document.getElementById("staffMemberId").value;
        const payload = {
            staffMemberName: document.getElementById("staffMemberName").value.trim(),
            isActive: document.getElementById("staffIsActive").checked,
        };

        try {
            if (id) {
                await apiFetch(`${STAFF_ENDPOINT}/${id}`, { method: "PUT", body: payload });
            } else {
                // Note the trailing slash -- StaffMemberController maps
                // create to "/api/staffmember/", not "/api/staffmember".
                await apiFetch(`${STAFF_ENDPOINT}/`, { method: "POST", body: payload });
            }
            closeModal();
            loadStaff();
        } catch (err) {
            showFormErrors(form, err, modalAlert);
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = "Save";
        }
    });

})();