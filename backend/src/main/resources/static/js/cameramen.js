(function () {
    const CAMERAMAN_ENDPOINT = "/api/cameraman";
    const isAdmin = Auth.hasRole("ADMIN");

    const addBtn = document.getElementById("add-cameraman-btn");
    const actionsHeader = document.getElementById("cameramen-actions-header");
    if (isAdmin) {
        addBtn.classList.remove("hidden");
        actionsHeader.classList.remove("hidden");
    }

    const activeOnlyCheckbox = document.getElementById("cameramenActiveOnly");
    const listAlert = document.getElementById("cameramen-alert");
    const emptyEl = document.getElementById("cameramen-empty");
    const table = document.getElementById("cameramen-table");
    const tbody = document.getElementById("cameramen-body");

    const PAGE_SIZE = 10;
    let allCameramen = [];
    let currentPage = 0;

    const paginationEl = document.getElementById("cameramen-pagination");
    const pageStatus = document.getElementById("cameramen-page-status");
    const prevBtn = document.getElementById("cameramen-page-prev");
    const nextBtn = document.getElementById("cameramen-page-next");

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
        return Math.max(1, Math.ceil(allCameramen.length / PAGE_SIZE));
    }

    activeOnlyCheckbox.addEventListener("change", loadCameramen);
    loadCameramen();

    async function loadCameramen() {
        hideAlert(listAlert);
        try {
            allCameramen = await apiFetch(`${CAMERAMAN_ENDPOINT}?activeOnly=${activeOnlyCheckbox.checked}`);
            currentPage = 0;
            renderPage();
        } catch (err) {
            showAlert(listAlert, "Could not load cameramen: " + (err.body?.message || err.message), "error");
        }
    }

    function renderPage() {
        if (allCameramen.length === 0) {
            table.classList.add("hidden");
            emptyEl.classList.remove("hidden");
            paginationEl.classList.add("hidden");
            return;
        }

        emptyEl.classList.add("hidden");
        table.classList.remove("hidden");

        const start = currentPage * PAGE_SIZE;
        const pageItems = allCameramen.slice(start, start + PAGE_SIZE);
        renderCameramen(pageItems);

        const pages = totalPages();
        paginationEl.classList.toggle("hidden", pages <= 1);
        pageStatus.textContent = `Page ${currentPage + 1} of ${pages}`;
        prevBtn.disabled = currentPage === 0;
        nextBtn.disabled = currentPage >= pages - 1;
    }

    function renderCameramen(cameramen) {
        tbody.innerHTML = "";

        cameramen.forEach((c) => {
            const row = document.createElement("tr");
            const statusBadge = c.isActive
                ? '<span class="badge-active">Active</span>'
                : '<span class="badge-inactive">Inactive</span>';

            row.innerHTML = `
                <td>${escapeHtml(c.cameramanName ?? "")}</td>
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
                row.querySelector(".edit-btn").addEventListener("click", () => openEditModal(c));
                row.querySelector(".delete-btn").addEventListener("click", () => deleteCameraman(c.cameramanId));
            }

            tbody.appendChild(row);
        });
    }

    async function deleteCameraman(id) {
        if (!confirm("Are you sure you want to permanently delete this cameraman?")) return;
        try {
            await apiFetch(`${CAMERAMAN_ENDPOINT}/${id}`, { method: "DELETE" });
            showAlert(listAlert, "Cameraman deleted.", "success");
            loadCameramen();
        } catch (err) {
            showAlert(listAlert, "Failed to delete: " + (err.body?.message || err.message), "error");
        }
    }

    // ---------- Add/Edit modal ----------

    if (!isAdmin) return; // Staff/Viewer never reach the form itself

    const modalOverlay = document.getElementById("cameraman-modal-overlay");
    const modalTitle = document.getElementById("cameraman-modal-title");
    const modalAlert = document.getElementById("cameraman-modal-alert");
    const form = document.getElementById("cameraman-form");
    const submitBtn = document.getElementById("cameraman-form-submit");
    const cancelBtn = document.getElementById("cameraman-form-cancel");
    const closeBtn = document.getElementById("cameraman-modal-close");

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
        document.getElementById("cameramanId").value = "";
        document.getElementById("cameramanIsActive").checked = true;
        modalTitle.textContent = "Add cameraman";
        modalOverlay.classList.remove("hidden");
    }

    function openEditModal(cameraman) {
        form.reset();
        clearFieldErrors();
        hideAlert(modalAlert);
        document.getElementById("cameramanId").value = cameraman.cameramanId;
        document.getElementById("cameramanName").value = cameraman.cameramanName || "";
        document.getElementById("cameramanIsActive").checked = !!cameraman.isActive;
        modalTitle.textContent = "Edit cameraman";
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

        const id = document.getElementById("cameramanId").value;
        const payload = {
            cameramanName: document.getElementById("cameramanName").value.trim(),
            isActive: document.getElementById("cameramanIsActive").checked,
        };

        try {
            if (id) {
                await apiFetch(`${CAMERAMAN_ENDPOINT}/${id}`, { method: "PUT", body: payload });
            } else {
                await apiFetch(`${CAMERAMAN_ENDPOINT}/create`, { method: "POST", body: payload });
            }
            closeModal();
            loadCameramen();
        } catch (err) {
            showFormErrors(form, err, modalAlert);
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = "Save";
        }
    });

})();