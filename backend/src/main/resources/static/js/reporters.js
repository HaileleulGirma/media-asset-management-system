(function () {
    const REPORTER_ENDPOINT = "/api/reporter";
    const isAdmin = Auth.hasRole("ADMIN");

    const addBtn = document.getElementById("add-reporter-btn");
    const actionsHeader = document.getElementById("reporters-actions-header");
    if (isAdmin) {
        addBtn.classList.remove("hidden");
        actionsHeader.classList.remove("hidden");
    }

    const activeOnlyCheckbox = document.getElementById("reportersActiveOnly");
    const listAlert = document.getElementById("reporters-alert");
    const emptyEl = document.getElementById("reporters-empty");
    const table = document.getElementById("reporters-table");
    const tbody = document.getElementById("reporters-body");

    const PAGE_SIZE = 10;
    let allReporters = [];
    let currentPage = 0;

    const paginationEl = document.getElementById("reporters-pagination");
    const pageStatus = document.getElementById("reporters-page-status");
    const prevBtn = document.getElementById("reporters-page-prev");
    const nextBtn = document.getElementById("reporters-page-next");

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
        return Math.max(1, Math.ceil(allReporters.length / PAGE_SIZE));
    }

    activeOnlyCheckbox.addEventListener("change", loadReporters);
    loadReporters();

    async function loadReporters() {
        hideAlert(listAlert);
        try {
            allReporters = await apiFetch(`${REPORTER_ENDPOINT}?activeOnly=${activeOnlyCheckbox.checked}`);
            currentPage = 0;
            renderPage();
        } catch (err) {
            showAlert(listAlert, "Could not load reporters: " + (err.body?.message || err.message), "error");
        }
    }

    function renderPage() {
        if (allReporters.length === 0) {
            table.classList.add("hidden");
            emptyEl.classList.remove("hidden");
            paginationEl.classList.add("hidden");
            return;
        }

        emptyEl.classList.add("hidden");
        table.classList.remove("hidden");

        const start = currentPage * PAGE_SIZE;
        const pageItems = allReporters.slice(start, start + PAGE_SIZE);
        renderReporters(pageItems);

        const pages = totalPages();
        paginationEl.classList.toggle("hidden", pages <= 1);
        pageStatus.textContent = `Page ${currentPage + 1} of ${pages}`;
        prevBtn.disabled = currentPage === 0;
        nextBtn.disabled = currentPage >= pages - 1;
    }

    function renderReporters(reporters) {
        tbody.innerHTML = "";

        reporters.forEach((r) => {
            const row = document.createElement("tr");
            const statusBadge = r.isActive
                ? '<span class="badge-active">Active</span>'
                : '<span class="badge-inactive">Inactive</span>';

            row.innerHTML = `
                <td>${escapeHtml(r.reporterName ?? "")}</td>
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
                row.querySelector(".edit-btn").addEventListener("click", () => openEditModal(r));
                row.querySelector(".delete-btn").addEventListener("click", () => deleteReporter(r.reporterId));
            }

            tbody.appendChild(row);
        });
    }

    async function deleteReporter(id) {
        if (!confirm("Are you sure you want to permanently delete this reporter?")) return;
        try {
            await apiFetch(`${REPORTER_ENDPOINT}/${id}`, { method: "DELETE" });
            showAlert(listAlert, "Reporter deleted.", "success");
            loadReporters();
        } catch (err) {
            showAlert(listAlert, "Failed to delete: " + (err.body?.message || err.message), "error");
        }
    }

    // ---------- Add/Edit modal ----------

    if (!isAdmin) return; // Staff/Viewer never reach the form itself

    const modalOverlay = document.getElementById("reporter-modal-overlay");
    const modalTitle = document.getElementById("reporter-modal-title");
    const modalAlert = document.getElementById("reporter-modal-alert");
    const form = document.getElementById("reporter-form");
    const submitBtn = document.getElementById("reporter-form-submit");
    const cancelBtn = document.getElementById("reporter-form-cancel");
    const closeBtn = document.getElementById("reporter-modal-close");

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
        document.getElementById("reporterId").value = "";
        document.getElementById("reporterIsActive").checked = true;
        modalTitle.textContent = "Add reporter";
        modalOverlay.classList.remove("hidden");
    }

    function openEditModal(reporter) {
        form.reset();
        clearFieldErrors();
        hideAlert(modalAlert);
        document.getElementById("reporterId").value = reporter.reporterId;
        document.getElementById("reporterName").value = reporter.reporterName || "";
        document.getElementById("reporterIsActive").checked = !!reporter.isActive;
        modalTitle.textContent = "Edit reporter";
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

        const id = document.getElementById("reporterId").value;
        const payload = {
            reporterName: document.getElementById("reporterName").value.trim(),
            isActive: document.getElementById("reporterIsActive").checked,
        };

        try {
            if (id) {
                await apiFetch(`${REPORTER_ENDPOINT}/${id}`, { method: "PUT", body: payload });
            } else {
                await apiFetch(`${REPORTER_ENDPOINT}/create`, { method: "POST", body: payload });
            }
            closeModal();
            loadReporters();
        } catch (err) {
            showFormErrors(form, err, modalAlert);
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = "Save";
        }
    });

})();