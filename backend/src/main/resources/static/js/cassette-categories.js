(function () {
    const CATEGORY_ENDPOINT = "/api/cassette-category";

    // Category writes are Admin-only. This only controls what the UI shows;
    // the server enforces access via @PreAuthorize.
    if (!Auth.hasRole("ADMIN")) {
        document.querySelector(".main-content").innerHTML = `
            <div class="panel">
                <div class="alert alert-error visible">You do not have permission to view this page.</div>
            </div>`;
        return;
    }

    const createForm = document.getElementById("create-category-form");
    const createAlert = document.getElementById("create-category-alert");
    const createSubmit = document.getElementById("create-category-submit");

    const listAlert = document.getElementById("categories-alert");
    const listEmpty = document.getElementById("categories-empty");
    const listTable = document.getElementById("categories-table");
    const listBody = document.getElementById("categories-body");

    const editOverlay = document.getElementById("edit-category-overlay");
    const editForm = document.getElementById("edit-category-form");
    const editAlert = document.getElementById("edit-category-alert");
    const editSubmit = document.getElementById("edit-category-submit");
    let currentEditVersion = null;

    function escapeHtml(str) {
        const div = document.createElement("div");
        div.textContent = str;
        return div.innerHTML;
    }

    function clearFieldErrors(formEl) {
        formEl.querySelectorAll(".field").forEach((f) => {
            f.classList.remove("has-error");
            const errEl = f.querySelector(".field-error");
            if (errEl) errEl.textContent = "";
        });
    }

    function setFieldError(formEl, message) {
        const field = formEl.querySelector(".field");
        field.classList.add("has-error");
        field.querySelector(".field-error").textContent = message;
    }

    // ---------- List ----------

    async function loadCategories() {
        try {
            const categories = await apiFetch(CATEGORY_ENDPOINT);
            renderCategories(Array.isArray(categories) ? categories : []);
        } catch (err) {
            showAlert(listAlert, "Could not load categories: " + (err.body?.message || err.message), "error");
        }
    }

    function renderCategories(categories) {
        listBody.innerHTML = "";

        if (categories.length === 0) {
            listTable.classList.add("hidden");
            listEmpty.classList.remove("hidden");
            return;
        }
        listEmpty.classList.add("hidden");
        listTable.classList.remove("hidden");

        categories
            .slice()
            .sort((a, b) => (a.categoryName || "").localeCompare(b.categoryName || ""))
            .forEach((category) => {
                const row = document.createElement("tr");
                row.innerHTML = `
                    <td>${escapeHtml(category.categoryName ?? "")}</td>
                    <td>
                        <div class="row-actions">
                            <button type="button" class="btn btn-secondary btn-sm edit-btn">Rename</button>
                            <button type="button" class="btn btn-danger btn-sm delete-btn">Delete</button>
                        </div>
                    </td>
                `;
                row.querySelector(".edit-btn").addEventListener("click", () => openEditModal(category));
                row.querySelector(".delete-btn").addEventListener("click", () => deleteCategory(category));
                listBody.appendChild(row);
            });
    }

    // ---------- Add ----------

    createForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        hideAlert(createAlert);
        clearFieldErrors(createForm);

        const name = document.getElementById("categoryName").value.trim();
        if (!name) {
            setFieldError(createForm, "Category name is required.");
            return;
        }

        createSubmit.disabled = true;
        createSubmit.textContent = "Saving...";

        try {
            await apiFetch(CATEGORY_ENDPOINT, { method: "POST", body: { categoryName: name } });
            createForm.reset();
            showAlert(createAlert, "Category added.", "success");
            await loadCategories();
        } catch (err) {
            showFormErrors(createForm, err, createAlert);
        } finally {
            createSubmit.disabled = false;
            createSubmit.textContent = "Add category";
        }
    });

    // ---------- Rename ----------

    function openEditModal(category) {
        editForm.reset();
        clearFieldErrors(editForm);
        hideAlert(editAlert);
        currentEditVersion = category.version ?? null;
        document.getElementById("editCategoryId").value = category.id;
        document.getElementById("editCategoryName").value = category.categoryName || "";
        editOverlay.classList.remove("hidden");
    }

    function closeEditModal() {
        editOverlay.classList.add("hidden");
    }

    document.getElementById("edit-category-cancel").addEventListener("click", closeEditModal);
    document.getElementById("edit-category-close").addEventListener("click", closeEditModal);
    editOverlay.addEventListener("click", (e) => {
        if (e.target === editOverlay) closeEditModal();
    });
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && !editOverlay.classList.contains("hidden")) closeEditModal();
    });

    editForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        hideAlert(editAlert);
        clearFieldErrors(editForm);

        const name = document.getElementById("editCategoryName").value.trim();
        if (!name) {
            setFieldError(editForm, "Category name is required.");
            return;
        }

        editSubmit.disabled = true;
        editSubmit.textContent = "Saving...";

        const id = document.getElementById("editCategoryId").value;

        try {
            await apiFetch(`${CATEGORY_ENDPOINT}/${id}`, {
                method: "PUT",
                body: { categoryName: name, version: currentEditVersion },
            });
            closeEditModal();
            await loadCategories();
            showAlert(listAlert, "Category renamed.", "success");
        } catch (err) {
            if (err.status === 409) {
                showAlert(editAlert, "This category was changed by someone else. Close this dialog and try again.", "error");
                loadCategories();
            } else {
                showFormErrors(editForm, err, editAlert);
            }
        } finally {
            editSubmit.disabled = false;
            editSubmit.textContent = "Save changes";
        }
    });

    // ---------- Delete ----------

    async function deleteCategory(category) {
        if (!confirm(`Delete the category "${category.categoryName}"?`)) return;
        try {
            await apiFetch(`${CATEGORY_ENDPOINT}/${category.id}`, { method: "DELETE" });
            await loadCategories();
            showAlert(listAlert, "Category deleted.", "success");
        } catch (err) {
            showAlert(
                listAlert,
                "Could not delete the category. It may still be used by cassettes or cuts. " + (err.body?.message || err.message || ""),
                "error"
            );
        }
    }

    loadCategories();
})();