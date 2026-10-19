(function () {
    const PRODUCTION_ENDPOINT = "/api/production";
    const MB_PER_GB = 1024;
    const PAGE_SIZE = 20;

    const isAdmin = Auth.hasRole("ADMIN");
    const isAdminOrStaff = isAdmin || Auth.hasRole("STAFF");

    // Viewers never see the nav link; this covers anyone who types the URL.
    // Display convenience only -- the server enforces access via @PreAuthorize.
    if (!isAdminOrStaff) {
        document.querySelector(".main-content").innerHTML = `
            <div class="panel">
                <div class="alert alert-error visible">You do not have permission to view this page.</div>
            </div>`;
        return;
    }

    // Staff only get Search. The add/modify UI is removed from the DOM
    // (not just hidden) so there is nothing to un-hide.
    if (!isAdmin) {
        ["tab-bar", "create-production-panel", "modify-production-panel", "edit-production-overlay"].forEach((id) => {
            const el = document.getElementById(id);
            if (el) el.remove();
        });
    }

    const searchPanel = document.getElementById("search-production-panel");
    const createPanel = document.getElementById("create-production-panel");   // null for Staff
    const modifyPanel = document.getElementById("modify-production-panel");   // null for Staff

    const resultsOverlay = document.getElementById("search-results-overlay");
    const resultsCloseBtn = document.getElementById("search-results-close");

    // Global GC/EC toggle + Ethiopian picker on every date field. Ids that
    // were removed above for Staff are simply skipped by attach().
    DatePicker.mountToggle();
    DatePicker.attachAll(["productionDate", "startDate", "endDate", "modifySearchDate", "editProductionDate"]);

    // ---------- Tabs ----------

    const tabs = {
        search: { btn: document.getElementById("tab-search-btn"), panel: searchPanel },
        add: { btn: document.getElementById("tab-add-btn"), panel: createPanel },
        modify: { btn: document.getElementById("tab-modify-btn"), panel: modifyPanel },
    };

    function showTab(name) {
        const active = tabs[name] && (name === "search" || isAdmin) ? name : "search";
        Object.entries(tabs).forEach(([key, t]) => {
            if (t.panel) t.panel.classList.toggle("hidden", key !== active);
            if (t.btn) t.btn.classList.toggle("active", key === active);
        });
    }

    Object.entries(tabs).forEach(([key, t]) => {
        if (t.btn) t.btn.addEventListener("click", () => showTab(key));
    });

    showTab("search");

    // ---------- Helpers ----------

    function numberOrNull(value) {
        return value === "" ? null : Number(value);
    }

    function intOrZero(value) {
        return value === "" ? 0 : Number(value);
    }

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

    function setFieldError(inputId, message) {
        const field = document.getElementById(inputId).closest(".field");
        if (!field) return;
        field.classList.toggle("has-error", Boolean(message));
        const errEl = field.querySelector(".field-error");
        if (errEl) errEl.textContent = message || "";
    }

    // Duration converter: the database stores seconds, the forms use h/m/s.

    // 3725 -> "1:02:05"
    function formatDuration(totalSec) {
        if (totalSec == null) return "\u2014";
        const sec = Math.max(0, Math.round(Number(totalSec)));
        const h = Math.floor(sec / 3600);
        const m = Math.floor((sec % 3600) / 60);
        const s = sec % 60;
        return `${h}:${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
    }

    // 3725 -> { hours: 1, minutes: 2, seconds: 5 }
    function splitDuration(totalSec) {
        const sec = Math.max(0, Math.round(Number(totalSec) || 0));
        return {
            hours: Math.floor(sec / 3600),
            minutes: Math.floor((sec % 3600) / 60),
            seconds: sec % 60,
        };
    }

    // Reads the three duration inputs. Empty counts as 0 so the backend
    // never receives a null part.
    function readDuration(ids) {
        return {
            durationHours: intOrZero(document.getElementById(ids.h).value),
            durationMinutes: intOrZero(document.getElementById(ids.m).value),
            durationSeconds: intOrZero(document.getElementById(ids.s).value),
        };
    }

    // Client-side check so "75 minutes" is caught before it is sent.
    function validateDuration(ids) {
        let ok = true;
        [
            [ids.h, "Hours", null],
            [ids.m, "Minutes", 59],
            [ids.s, "Seconds", 59],
        ].forEach(([id, label, max]) => {
            setFieldError(id, null);
            const raw = document.getElementById(id).value;
            if (raw === "") return;
            const n = Number(raw);
            if (!Number.isInteger(n) || n < 0) {
                setFieldError(id, `${label} must be a whole number, 0 or more.`);
                ok = false;
            } else if (max !== null && n > max) {
                setFieldError(id, `${label} must be ${max} or less.`);
                ok = false;
            }
        });
        return ok;
    }

    function sizeToGb(valueId, unitId) {
        const raw = document.getElementById(valueId).value;
        if (raw === "") return null;
        const unit = document.getElementById(unitId).value;
        return unit === "MB" ? Number(raw) / MB_PER_GB : Number(raw);
    }

    function formatSize(n) {
        return n != null ? Number(n).toFixed(2) : "";
    }

    const CREATE_DURATION_IDS = { h: "durationHours", m: "durationMinutes", s: "durationSeconds" };
    const EDIT_DURATION_IDS = { h: "editDurationHours", m: "editDurationMinutes", s: "editDurationSeconds" };

    // ---------- Add production item (ADMIN) ----------

    if (isAdmin) {
        const form = document.getElementById("create-production-form");
        const alertEl = document.getElementById("create-production-alert");
        const submitBtn = document.getElementById("create-production-submit");

        document.getElementById("create-production-reset").addEventListener("click", () => {
            form.reset();
            DatePicker.refresh("productionDate");
            hideAlert(alertEl);
            clearFieldErrors(form);
        });

        form.addEventListener("submit", async (e) => {
            e.preventDefault();
            hideAlert(alertEl);
            clearFieldErrors(form);

            if (!validateDuration(CREATE_DURATION_IDS)) return;

            submitBtn.disabled = true;
            submitBtn.textContent = "Saving...";

            const payload = {
                productionDate: document.getElementById("productionDate").value || null,
                title: document.getElementById("title").value.trim(),
                filePath: document.getElementById("filePath").value.trim(),
                fileSizeGb: sizeToGb("fileSize", "fileSizeUnit"),
                ...readDuration(CREATE_DURATION_IDS),
            };

            try {
                await apiFetch(PRODUCTION_ENDPOINT, { method: "POST", body: payload });
                form.reset();
                DatePicker.refresh("productionDate");
                clearFieldErrors(form);
                showAlert(alertEl, "Production item added.", "success");
                alertEl.scrollIntoView({ behavior: "smooth", block: "center" });
            } catch (err) {
                showFormErrors(form, err, alertEl);
                alertEl.scrollIntoView({ behavior: "smooth", block: "center" });
            } finally {
                submitBtn.disabled = false;
                submitBtn.textContent = "Add production item";
            }
        });
    }

    // ---------- Modify / delete production (ADMIN) ----------

    if (isAdmin) {
        const modifySearchBtn = document.getElementById("modifySearchBtn");
        const modifySearchDate = document.getElementById("modifySearchDate");
        const modifyListAlert = document.getElementById("modify-list-alert");
        const modifyResultsBody = document.getElementById("modify-results-body");
        const modifyResultsTable = document.getElementById("modify-results-table");

        const editOverlay = document.getElementById("edit-production-overlay");
        const editForm = document.getElementById("edit-production-form");
        const editAlert = document.getElementById("edit-production-alert");
        const editCancelBtn = document.getElementById("edit-production-cancel");
        const editCloseBtn = document.getElementById("edit-production-close");
        const editSubmitBtn = document.getElementById("edit-production-submit");
        let currentEditVersion = null;

        // Same approach as news: one date, load everything from that day.
        async function loadModifyList() {
            const dateVal = modifySearchDate.value;
            if (!dateVal) {
                showAlert(modifyListAlert, "Please select a date to search.", "error");
                return;
            }
            hideAlert(modifyListAlert);
            modifySearchBtn.disabled = true;
            modifySearchBtn.textContent = "Loading...";

            try {
                const result = await apiFetch(`${PRODUCTION_ENDPOINT}?startDate=${dateVal}&size=100`);
                renderModifyResults(result.content || []);
            } catch (err) {
                showAlert(modifyListAlert, "Could not load production: " + (err.body?.message || err.message), "error");
            } finally {
                modifySearchBtn.disabled = false;
                modifySearchBtn.textContent = "Load production";
            }
        }

        modifySearchBtn.addEventListener("click", loadModifyList);

        function renderModifyResults(items) {
            modifyResultsBody.innerHTML = "";
            if (items.length === 0) {
                modifyResultsTable.classList.add("hidden");
                showAlert(modifyListAlert, "No production items found for this date.", "error");
                return;
            }
            hideAlert(modifyListAlert);
            modifyResultsTable.classList.remove("hidden");

            items.forEach((item) => {
                const row = document.createElement("tr");
                row.innerHTML = `
                    <td>${item.date ?? ""}</td>
                    <td>${escapeHtml(item.title ?? "")}</td>
                    <td>${escapeHtml(item.filePath ?? "")}</td>
                    <td>${formatDuration(item.durationSec)}</td>
                    <td>${formatSize(item.fileSize)}</td>
                    <td>
                        <div class="row-actions">
                            <button type="button" class="btn btn-secondary btn-sm edit-btn">Edit</button>
                            <button type="button" class="btn btn-danger btn-sm delete-btn">Delete</button>
                        </div>
                    </td>
                `;
                row.querySelector(".edit-btn").addEventListener("click", () => openEditModal(item));
                row.querySelector(".delete-btn").addEventListener("click", () => deleteProduction(item.id));
                modifyResultsBody.appendChild(row);
            });
        }

        async function deleteProduction(id) {
            if (!confirm("Are you sure you want to permanently delete this production item?")) return;
            try {
                await apiFetch(`${PRODUCTION_ENDPOINT}/${id}`, { method: "DELETE" });
                await loadModifyList();
                showAlert(modifyListAlert, "Production item deleted successfully.", "success");
            } catch (err) {
                showAlert(modifyListAlert, "Failed to delete: " + (err.body?.message || err.message), "error");
            }
        }

        function openEditModal(item) {
            editOverlay.classList.remove("hidden");
            hideAlert(editAlert);
            editForm.reset();
            clearFieldErrors(editForm);

            currentEditVersion = item.version ?? null;

            document.getElementById("editProductionId").value = item.id;
            document.getElementById("editTitle").value = item.title || "";
            document.getElementById("editProductionDate").value = item.date || "";
            DatePicker.refresh("editProductionDate");
            document.getElementById("editFilePath").value = item.filePath || "";

            document.getElementById("editFileSize").value = item.fileSize != null ? item.fileSize : "";
            document.getElementById("editFileSizeUnit").value = "GB";

            const d = splitDuration(item.durationSec);
            document.getElementById("editDurationHours").value = d.hours;
            document.getElementById("editDurationMinutes").value = d.minutes;
            document.getElementById("editDurationSeconds").value = d.seconds;
        }

        function closeEditModal() {
            editOverlay.classList.add("hidden");
        }

        editCancelBtn.addEventListener("click", closeEditModal);
        editCloseBtn.addEventListener("click", closeEditModal);
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

            if (!validateDuration(EDIT_DURATION_IDS)) return;

            editSubmitBtn.disabled = true;
            editSubmitBtn.textContent = "Saving...";

            const id = document.getElementById("editProductionId").value;

            const payload = {
                productionDate: document.getElementById("editProductionDate").value || null,
                title: document.getElementById("editTitle").value.trim(),
                filePath: document.getElementById("editFilePath").value.trim(),
                fileSizeGb: sizeToGb("editFileSize", "editFileSizeUnit"),
                ...readDuration(EDIT_DURATION_IDS),
                version: currentEditVersion,
            };

            try {
                await apiFetch(`${PRODUCTION_ENDPOINT}/${id}`, { method: "PUT", body: payload });
                closeEditModal();
                await loadModifyList();
                showAlert(modifyListAlert, "Production item updated successfully.", "success");
            } catch (err) {
                if (err.status === 409) {
                    showAlert(editAlert, "This production item was changed by someone else. Close this dialog and reload the list to try again.", "error");
                } else {
                    showFormErrors(editForm, err, editAlert);
                }
            } finally {
                editSubmitBtn.disabled = false;
                editSubmitBtn.textContent = "Save changes";
            }
        });
    }

    // ---------- Search (ADMIN + STAFF) ----------

    const searchForm = document.getElementById("search-production-form");
    const resetBtn = document.getElementById("search-reset");
    const resultsEmpty = document.getElementById("search-results-empty");
    const resultsTable = document.getElementById("search-results-table");
    const resultsBody = document.getElementById("search-results-body");
    const paginationEl = document.getElementById("search-pagination");
    const pageStatus = document.getElementById("page-status");
    const prevBtn = document.getElementById("page-prev");
    const nextBtn = document.getElementById("page-next");

    let currentPage = 0;
    let totalPages = 0;

    function setDateRangeError(message) {
        ["startDate", "endDate"].forEach((id) => {
            const field = document.getElementById(id).closest(".field");
            if (!field) return;
            field.classList.toggle("has-error", Boolean(message));
            const errEl = field.querySelector(".field-error");
            if (errEl) errEl.textContent = message || "";
        });
    }

    // "To date" only makes sense once "From date" is chosen.
    const startDateInput = document.getElementById("startDate");
    const endDateInput = document.getElementById("endDate");

    function syncEndDateAvailability() {
        const hasStart = Boolean(startDateInput.value);
        endDateInput.disabled = !hasStart;
        if (!hasStart) endDateInput.value = "";
    }

    startDateInput.addEventListener("input", () => {
        setDateRangeError(null);
        syncEndDateAvailability();
    });
    endDateInput.addEventListener("input", () => setDateRangeError(null));
    syncEndDateAvailability();

    searchForm.addEventListener("submit", (e) => {
        e.preventDefault();
        const startDateValue = startDateInput.value;
        const endDateValue = endDateInput.value;
        if (startDateValue && endDateValue && startDateValue > endDateValue) {
            setDateRangeError('"From date" cannot be later than "To date".');
            return;
        }
        setDateRangeError(null);
        currentPage = 0;
        openResultsModal();
        runSearch();
    });

    resetBtn.addEventListener("click", () => {
        searchForm.reset();
        DatePicker.refresh("startDate");
        DatePicker.refresh("endDate");
        setDateRangeError(null);
        clearTermPiles();
        syncEndDateAvailability();
    });

    resultsCloseBtn.addEventListener("click", closeResultsModal);
    resultsOverlay.addEventListener("click", (e) => {
        if (e.target === resultsOverlay) closeResultsModal();
    });
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && !resultsOverlay.classList.contains("hidden")) {
            // The edit modal (admin) closes first if it is open on top.
            const editOverlay = document.getElementById("edit-production-overlay");
            if (!editOverlay || editOverlay.classList.contains("hidden")) closeResultsModal();
        }
    });

    function openResultsModal() {
        resultsOverlay.classList.remove("hidden");
        resultsEmpty.textContent = "Searching...";
        resultsEmpty.classList.remove("hidden");
        resultsTable.classList.add("hidden");
        paginationEl.classList.add("hidden");
    }

    function closeResultsModal() {
        resultsOverlay.classList.add("hidden");
    }

    prevBtn.addEventListener("click", () => {
        if (currentPage > 0) {
            currentPage -= 1;
            runSearch();
        }
    });

    nextBtn.addEventListener("click", () => {
        if (currentPage < totalPages - 1) {
            currentPage += 1;
            runSearch();
        }
    });

    // ----- search-term builder (same behavior as news) -----

    let mustIncludeTerms = [];
    let anyOfTerms = [];
    let excludeTerms = [];
    let currentTermType = "and";

    const termTypeButtons = document.querySelectorAll(".term-type-btn");
    termTypeButtons.forEach((btn) => {
        btn.addEventListener("click", () => {
            currentTermType = btn.dataset.type;
            termTypeButtons.forEach((b) => b.classList.toggle("active", b === btn));
        });
    });

    const searchTermInput = document.getElementById("searchTermInput");
    const addTermBtn = document.getElementById("addTermBtn");

    addTermBtn.addEventListener("click", addCurrentTerm);
    searchTermInput.addEventListener("keydown", (e) => {
        if (e.key === "Enter") {
            e.preventDefault();
            addCurrentTerm();
        }
    });

    function addCurrentTerm() {
        const value = searchTermInput.value.trim();
        if (!value) return;

        const targetArray =
            currentTermType === "and" ? mustIncludeTerms :
                currentTermType === "or" ? anyOfTerms :
                    excludeTerms;

        // A term sits in one pile at a time.
        [mustIncludeTerms, anyOfTerms, excludeTerms].forEach((pile) => {
            if (pile === targetArray) return;
            const idx = pile.indexOf(value);
            if (idx !== -1) pile.splice(idx, 1);
        });

        if (!targetArray.includes(value)) targetArray.push(value);
        searchTermInput.value = "";
        renderTermChips();
    }

    function renderTermChips() {
        renderTermGroup("mustIncludeChips", mustIncludeTerms);
        renderTermGroup("anyOfChips", anyOfTerms);
        renderTermGroup("excludeChips", excludeTerms);
    }

    function renderTermGroup(containerId, termArray) {
        const container = document.getElementById(containerId);
        container.innerHTML = "";
        termArray.forEach((term, index) => {
            const chip = document.createElement("span");
            chip.className = "chip";
            chip.textContent = term;

            const removeBtn = document.createElement("button");
            removeBtn.type = "button";
            removeBtn.className = "chip-remove";
            removeBtn.setAttribute("aria-label", "Remove " + term);
            removeBtn.textContent = "\u00d7";
            removeBtn.addEventListener("click", () => {
                termArray.splice(index, 1);
                renderTermChips();
            });

            chip.appendChild(removeBtn);
            container.appendChild(chip);
        });
    }

    function clearTermPiles() {
        mustIncludeTerms = [];
        anyOfTerms = [];
        excludeTerms = [];
        renderTermChips();
    }

    function formatTerm(term) {
        return term.includes(" ") ? `"${term}"` : term;
    }

    function buildSearchTerm() {
        const parts = [];
        if (mustIncludeTerms.length > 0) {
            parts.push(mustIncludeTerms.map(formatTerm).join(" "));
        }
        if (anyOfTerms.length > 0) {
            parts.push(anyOfTerms.map(formatTerm).join(" OR "));
        }
        excludeTerms.forEach((term) => parts.push("-" + formatTerm(term)));
        return parts.join(" ").trim();
    }

    async function runSearch() {
        const params = new URLSearchParams();

        const searchTerm = buildSearchTerm();
        if (searchTerm) params.append("searchTerm", searchTerm);

        if (startDateInput.value) params.append("startDate", startDateInput.value);
        if (endDateInput.value) params.append("endDate", endDateInput.value);

        params.append("page", currentPage);
        params.append("size", PAGE_SIZE);

        try {
            const result = await apiFetch(PRODUCTION_ENDPOINT + "?" + params.toString());
            renderResults(result);
        } catch (err) {
            resultsEmpty.textContent = "Could not load results: " + (err.body?.message || err.message);
            resultsEmpty.classList.remove("hidden");
            resultsTable.classList.add("hidden");
            paginationEl.classList.add("hidden");
        }
    }

    function renderResults(page) {
        totalPages = page.totalPages;

        if (!page.content || page.content.length === 0) {
            resultsEmpty.textContent = "No production items match your search.";
            resultsEmpty.classList.remove("hidden");
            resultsTable.classList.add("hidden");
            paginationEl.classList.add("hidden");
            return;
        }

        resultsEmpty.classList.add("hidden");
        resultsTable.classList.remove("hidden");

        resultsBody.innerHTML = "";
        page.content.forEach((item) => {
            const row = document.createElement("tr");
            row.innerHTML = `
                <td>${item.date ?? ""}</td>
                <td>${escapeHtml(item.title ?? "")}</td>
                <td>${escapeHtml(item.filePath ?? "")}</td>
                <td>${formatDuration(item.durationSec)}</td>
                <td>${formatSize(item.fileSize)}</td>
            `;
            resultsBody.appendChild(row);
        });

        paginationEl.classList.toggle("hidden", totalPages <= 1);
        pageStatus.textContent = `Page ${page.number + 1} of ${totalPages}`;
        prevBtn.disabled = page.first;
        nextBtn.disabled = page.last;
    }
})();