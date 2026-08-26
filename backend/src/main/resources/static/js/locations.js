(function () {
    const LOCATION_ENDPOINT = "/api/location";
    const isAdmin = Auth.hasRole("ADMIN");

    const addBtn = document.getElementById("add-location-btn");
    const actionsHeader = document.getElementById("locations-actions-header");
    if (isAdmin) {
        addBtn.classList.remove("hidden");
        actionsHeader.classList.remove("hidden");
    }

    const abroadOnlyCheckbox = document.getElementById("locationsAbroadOnly");
    const listAlert = document.getElementById("locations-alert");
    const emptyEl = document.getElementById("locations-empty");
    const table = document.getElementById("locations-table");
    const tbody = document.getElementById("locations-body");

    const PAGE_SIZE = 10;
    let allLocations = [];
    let currentPage = 0;

    const paginationEl = document.getElementById("locations-pagination");
    const pageStatus = document.getElementById("locations-page-status");
    const prevBtn = document.getElementById("locations-page-prev");
    const nextBtn = document.getElementById("locations-page-next");

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
        return Math.max(1, Math.ceil(allLocations.length / PAGE_SIZE));
    }

    // Local/abroad is a strict either/or (not a superset relationship like
    // active/all), so toggling always requests one of the two categories
    // explicitly -- matches the same pattern used for the abroad filter
    // in news.js.
    abroadOnlyCheckbox.addEventListener("change", loadLocations);
    loadLocations();

    async function loadLocations() {
        hideAlert(listAlert);
        try {
            allLocations = await apiFetch(`${LOCATION_ENDPOINT}?abroadOnly=${abroadOnlyCheckbox.checked}`);
            currentPage = 0;
            renderPage();
        } catch (err) {
            showAlert(listAlert, "Could not load locations: " + (err.body?.message || err.message), "error");
        }
    }

    function renderPage() {
        if (allLocations.length === 0) {
            table.classList.add("hidden");
            emptyEl.classList.remove("hidden");
            paginationEl.classList.add("hidden");
            return;
        }

        emptyEl.classList.add("hidden");
        table.classList.remove("hidden");

        const start = currentPage * PAGE_SIZE;
        const pageItems = allLocations.slice(start, start + PAGE_SIZE);
        renderLocations(pageItems);

        const pages = totalPages();
        paginationEl.classList.toggle("hidden", pages <= 1);
        pageStatus.textContent = `Page ${currentPage + 1} of ${pages}`;
        prevBtn.disabled = currentPage === 0;
        nextBtn.disabled = currentPage >= pages - 1;
    }

    function renderLocations(locations) {
        tbody.innerHTML = "";

        locations.forEach((l) => {
            const row = document.createElement("tr");
            // Local/abroad is a category, not a good/bad state, so both use
            // the same neutral pill style (role-badge) rather than the
            // green/gray active-inactive coloring.
            const categoryBadge = `<span class="role-badge">${l.isAbroad ? "Abroad" : "Local"}</span>`;

            row.innerHTML = `
                <td>${escapeHtml(l.locationName ?? "")}</td>
                <td>${categoryBadge}</td>
                ${isAdmin ? `
                <td>
                    <div class="row-actions">
                        <button type="button" class="btn btn-secondary btn-sm edit-btn">Edit</button>
                        <button type="button" class="btn btn-danger btn-sm delete-btn">Delete</button>
                    </div>
                </td>` : ""}
            `;

            if (isAdmin) {
                row.querySelector(".edit-btn").addEventListener("click", () => openEditModal(l));
                row.querySelector(".delete-btn").addEventListener("click", () => deleteLocation(l.locationId));
            }

            tbody.appendChild(row);
        });
    }

    async function deleteLocation(id) {
        if (!confirm("Are you sure you want to permanently delete this location?")) return;
        try {
            await apiFetch(`${LOCATION_ENDPOINT}/${id}`, { method: "DELETE" });
            showAlert(listAlert, "Location deleted.", "success");
            loadLocations();
        } catch (err) {
            showAlert(listAlert, "Failed to delete: " + (err.body?.message || err.message), "error");
        }
    }

    // ---------- Add/Edit modal ----------

    if (!isAdmin) return; // Staff/Viewer never reach the form itself

    const modalOverlay = document.getElementById("location-modal-overlay");
    const modalTitle = document.getElementById("location-modal-title");
    const modalAlert = document.getElementById("location-modal-alert");
    const form = document.getElementById("location-form");
    const submitBtn = document.getElementById("location-form-submit");
    const cancelBtn = document.getElementById("location-form-cancel");
    const closeBtn = document.getElementById("location-modal-close");

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
        document.getElementById("locationId").value = "";
        document.getElementById("locationIsAbroad").checked = false;
        modalTitle.textContent = "Add location";
        modalOverlay.classList.remove("hidden");
    }

    function openEditModal(location) {
        form.reset();
        clearFieldErrors();
        hideAlert(modalAlert);
        document.getElementById("locationId").value = location.locationId;
        document.getElementById("locationName").value = location.locationName || "";
        document.getElementById("locationIsAbroad").checked = !!location.isAbroad;
        modalTitle.textContent = "Edit location";
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

        const id = document.getElementById("locationId").value;
        const payload = {
            locationName: document.getElementById("locationName").value.trim(),
            isAbroad: document.getElementById("locationIsAbroad").checked,
        };

        try {
            if (id) {
                await apiFetch(`${LOCATION_ENDPOINT}/${id}`, { method: "PUT", body: payload });
            } else {
                await apiFetch(`${LOCATION_ENDPOINT}/create`, { method: "POST", body: payload });
            }
            closeModal();
            loadLocations();
        } catch (err) {
            showFormErrors(form, err, modalAlert);
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = "Save";
        }
    });

})();