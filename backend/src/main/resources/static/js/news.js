(function () {
    const NEWS_CREATE_ENDPOINT = "/api/news";
    const MB_PER_GB = 1024;
    const PAGE_SIZE = 20;

    const isAdminOrStaff = Auth.hasRole("ADMIN") || Auth.hasRole("STAFF");
    const createPanel = document.getElementById("create-news-panel");
    const searchPanel = document.getElementById("search-news-panel");
    const resultsOverlay = document.getElementById("search-results-overlay");
    const resultsCloseBtn = document.getElementById("search-results-close");
    const tabSearchBtn = document.getElementById("tab-search-btn");
    const tabAddBtn = document.getElementById("tab-add-btn");

    const nameMaps = { reporters: {}, cameramen: {}, locations: {} };

    function showTab(tab) {
        const showAdd = tab === "add" && isAdminOrStaff;
        createPanel.classList.toggle("hidden", !showAdd);
        searchPanel.classList.toggle("hidden", showAdd);
        tabSearchBtn.classList.toggle("active", !showAdd);
        tabAddBtn.classList.toggle("active", showAdd);
    }

    tabSearchBtn.addEventListener("click", () => showTab("search"));

    if (isAdminOrStaff) {
        tabAddBtn.classList.remove("hidden");
        tabAddBtn.addEventListener("click", () => showTab("add"));
    }

    showTab("search");

    loadReferenceData();

    async function loadReferenceData() {
        try {
            const [activeReporters, inactiveReporters, activeCameramen, inactiveCameramen, localLocations, abroadLocations] =
                await Promise.all([
                    apiFetch("/api/reporter?activeOnly=true"),
                    apiFetch("/api/reporter?activeOnly=false"),
                    apiFetch("/api/cameraman?activeOnly=true"),
                    apiFetch("/api/cameraman?activeOnly=false"),
                    apiFetch("/api/location?abroadOnly=false"),
                    apiFetch("/api/location?abroadOnly=true"),
                ]);

            // Search results can reference people regardless of their current
            // active status, so the name lookup needs both lists merged --
            // activeOnly is a strict either/or filter now, not "everyone".
            [...activeReporters, ...inactiveReporters].forEach((r) => (nameMaps.reporters[r.reporterId] = r.reporterName));
            [...activeCameramen, ...inactiveCameramen].forEach((c) => (nameMaps.cameramen[c.cameramanId] = c.cameramanName));
            [...localLocations, ...abroadLocations].forEach((l) => (nameMaps.locations[l.locationId] = l.locationName));

            searchLocationController.fillSelect(localLocations, "locationId", "locationName");
            await loadSearchPeopleOptions(document.getElementById("searchActiveOnly").checked);

            if (isAdminOrStaff) {
                createLocationController.fillSelect(localLocations, "locationId", "locationName");
                await loadCreatePeopleOptions(document.getElementById("createActiveOnly").checked);
            }
        } catch (err) {
            showAlert(searchAlertEl(), "Could not load filter data: " + (err.body?.message || err.message), "error");
        }
    }

    document.getElementById("searchActiveOnly").addEventListener("change", (e) => {
        loadSearchPeopleOptions(e.target.checked);
    });

    document.getElementById("createActiveOnly").addEventListener("change", (e) => {
        loadCreatePeopleOptions(e.target.checked);
    });

    // A handful of selects (locations under the abroad toggle, reporters and
    // cameramen under the active-only toggle) need the same behavior: the
    // visible option list always matches the current filter exactly, but a
    // pick made under one filter state stays selected -- and keeps sending
    // with the request -- after switching to the other, until the user
    // removes its chip themselves. This factory binds that behavior to one
    // <select> + chip container; idField/labelField are supplied per
    // fillSelect call so the same factory serves every field.
    function makeMultiSelectController(selectId, chipsId) {
        const selected = new Map(); // id (string) -> label

        function fillSelect(items, idField, labelField) {
            const select = document.getElementById(selectId);
            select.innerHTML = "";
            items.forEach((item) => {
                const opt = document.createElement("option");
                opt.value = item[idField];
                opt.textContent = item[labelField];
                if (selected.has(String(item[idField]))) {
                    opt.selected = true;
                }
                select.appendChild(opt);
            });
        }

        function renderChipsFn() {
            const container = document.getElementById(chipsId);
            container.innerHTML = "";
            selected.forEach((label, id) => {
                const chip = document.createElement("span");
                chip.className = "chip";
                chip.textContent = label;

                const removeBtn = document.createElement("button");
                removeBtn.type = "button";
                removeBtn.className = "chip-remove";
                removeBtn.setAttribute("aria-label", "Remove " + label);
                removeBtn.textContent = "\u00d7";
                removeBtn.addEventListener("click", () => {
                    selected.delete(id);
                    const select = document.getElementById(selectId);
                    const opt = Array.from(select.options).find((o) => o.value === id);
                    if (opt) opt.selected = false;
                    renderChipsFn();
                });

                chip.appendChild(removeBtn);
                container.appendChild(chip);
            });
        }

        document.getElementById(selectId).addEventListener("change", () => {
            const select = document.getElementById(selectId);
            // Only options actually visible right now can have been toggled
            // by the user -- anything hidden by the current filter is untouched.
            Array.from(select.options).forEach((opt) => {
                if (opt.selected) {
                    selected.set(opt.value, opt.textContent);
                } else {
                    selected.delete(opt.value);
                }
            });
            renderChipsFn();
        });

        return {
            fillSelect,
            renderChips: renderChipsFn,
            clear() {
                selected.clear();
                renderChipsFn();
            },
            ids() {
                return Array.from(selected.keys()).map(Number);
            },
        };
    }

    const searchLocationController = makeMultiSelectController("searchLocationIds", "searchLocationChips");
    const createLocationController = makeMultiSelectController("locationIds", "locationChips");
    const searchReporterController = makeMultiSelectController("searchReporterIds", "searchReporterChips");
    const searchCameramanController = makeMultiSelectController("searchCameramanIds", "searchCameramanChips");
    const createReporterController = makeMultiSelectController("reporterIds", "reporterChips");
    const createCameramanController = makeMultiSelectController("cameramanIds", "cameramanChips");

    document.getElementById("searchAbroadOnly").addEventListener("change", async (e) => {
        try {
            const locations = await apiFetch(`/api/location?abroadOnly=${e.target.checked}`);
            searchLocationController.fillSelect(locations, "locationId", "locationName");
        } catch (err) {
            showAlert(searchAlertEl(), "Could not load locations: " + (err.body?.message || err.message), "error");
        }
    });

    document.getElementById("createAbroadOnly").addEventListener("change", async (e) => {
        try {
            const locations = await apiFetch(`/api/location?abroadOnly=${e.target.checked}`);
            createLocationController.fillSelect(locations, "locationId", "locationName");
        } catch (err) {
            showAlert(createAlertEl(), "Could not load locations: " + (err.body?.message || err.message), "error");
        }
    });

    // Shared loader for a reporters+cameramen pair, driven by an
    // "active only" toggle. Used by both the search and create panels.
    // Selection persists across the toggle via the controllers' own Map,
    // so switching never mixes active and inactive in the visible list but
    // also never silently drops a pick made under the other state.
    async function loadPeopleOptions(activeOnly, reporterController, cameramanController, alertEl) {
        try {
            const [reporters, cameramen] = await Promise.all([
                apiFetch(`/api/reporter?activeOnly=${activeOnly}`),
                apiFetch(`/api/cameraman?activeOnly=${activeOnly}`),
            ]);

            reporterController.fillSelect(reporters, "reporterId", "reporterName");
            cameramanController.fillSelect(cameramen, "cameramanId", "cameramanName");
            reporterController.renderChips();
            cameramanController.renderChips();
        } catch (err) {
            showAlert(alertEl, "Could not load reporter/cameraman list: " + (err.body?.message || err.message), "error");
        }
    }

    function loadSearchPeopleOptions(activeOnly) {
        return loadPeopleOptions(activeOnly, searchReporterController, searchCameramanController, searchAlertEl());
    }

    function loadCreatePeopleOptions(activeOnly) {
        return loadPeopleOptions(activeOnly, createReporterController, createCameramanController, createAlertEl());
    }

    if (isAdminOrStaff) {
        loadStaffData();
    }

    async function loadStaffData() {
        try {
            const staff = await apiFetch("/api/staffmember?activeOnly=true");
            fillSingleSelect("importerId", staff, "staffMemberId", "staffMemberName");
            fillSingleSelect("ingestorId", staff, "staffMemberId", "staffMemberName");
        } catch (err) {
            showAlert(createAlertEl(), "Could not load staff data: " + (err.body?.message || err.message), "error");
        }
    }

    function fillSingleSelect(elementId, items, idField, labelField) {
        const select = document.getElementById(elementId);
        if (!select) return;
        items.forEach((item) => {
            const opt = document.createElement("option");
            opt.value = item[idField];
            opt.textContent = item[labelField];
            select.appendChild(opt);
        });
    }

    // ---------- Create news form ----------

    const form = document.getElementById("create-news-form");
    function createAlertEl() {
        return document.getElementById("create-news-alert");
    }
    const submitBtn = document.getElementById("create-news-submit");

    // Re-loads the reporter/cameraman/location lists to match whatever the
    // create form's active-only / abroad toggles are currently set to.
    // Needed after form.reset() (submit success, or Clear form), since
    // resetting the checkboxes back to their defaults doesn't itself
    // re-fetch the lists.
    function refreshCreateLists() {
        loadCreatePeopleOptions(document.getElementById("createActiveOnly").checked);
        apiFetch(`/api/location?abroadOnly=${document.getElementById("createAbroadOnly").checked}`)
            .then((locations) => createLocationController.fillSelect(locations, "locationId", "locationName"))
            .catch((err) => showAlert(createAlertEl(), "Could not load locations: " + (err.body?.message || err.message), "error"));
    }

    function clearCreateFieldErrors() {
        form.querySelectorAll(".field").forEach((f) => {
            f.classList.remove("has-error");
            const errEl = f.querySelector(".field-error");
            if (errEl) errEl.textContent = "";
        });
    }

    const clearFormBtn = document.getElementById("create-news-reset");
    clearFormBtn.addEventListener("click", () => {
        form.reset();
        hideAlert(createAlertEl());
        clearCreateFieldErrors();
        createReporterController.clear();
        createCameramanController.clear();
        createLocationController.clear();
        refreshCreateLists();
    });

    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        const alertEl = createAlertEl();
        hideAlert(alertEl);
        clearCreateFieldErrors();
        submitBtn.disabled = true;
        submitBtn.textContent = "Saving...";

        const payload = {
            title: document.getElementById("title").value.trim(),
            newsDate: document.getElementById("newsDate").value || null,
            filePath: document.getElementById("filePath").value.trim(),
            numberOfFiles: numberOrNull(document.getElementById("numberOfFiles").value),
            totalSize: totalSizeInGb(),
            importerId: numberOrNull(document.getElementById("importerId").value),
            ingestorId: numberOrNull(document.getElementById("ingestorId").value),
            reporterIds: createReporterController.ids(),
            cameramanIds: createCameramanController.ids(),
            locationIds: createLocationController.ids(),
        };

        try {
            await apiFetch(NEWS_CREATE_ENDPOINT, { method: "POST", body: payload });
            form.reset();
            clearCreateFieldErrors();
            createReporterController.clear();
            createCameramanController.clear();
            createLocationController.clear();
            refreshCreateLists();
            showAlert(alertEl, "News item added.", "success");
            alertEl.scrollIntoView({ behavior: "smooth", block: "center" });
        } catch (err) {
            showFormErrors(form, err, alertEl);
            alertEl.scrollIntoView({ behavior: "smooth", block: "center" });
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = "Add news item";
        }
    });

    function numberOrNull(value) {
        return value === "" ? null : Number(value);
    }

    function totalSizeInGb() {
        const rawValue = document.getElementById("totalSize").value;
        if (rawValue === "") return null;
        const unit = document.getElementById("totalSizeUnit").value;
        const value = Number(rawValue);
        return unit === "MB" ? value / MB_PER_GB : value;
    }

    // ---------- Search news ----------

    const searchForm = document.getElementById("search-news-form");
    function searchAlertEl() {
        return document.getElementById("search-results-empty");
    }
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

    // Shows (or clears, when message is falsy) a "From date"/"To date"
    // range error inline under both date fields, using the same
    // .has-error / .field-error convention as the rest of the form.
    function setDateRangeError(message) {
        ["startDate", "endDate"].forEach((id) => {
            const field = document.getElementById(id).closest(".field");
            if (!field) return;
            field.classList.toggle("has-error", Boolean(message));
            const errEl = field.querySelector(".field-error");
            if (errEl) errEl.textContent = message || "";
        });
    }

    document.getElementById("startDate").addEventListener("input", () => setDateRangeError(null));
    document.getElementById("endDate").addEventListener("input", () => setDateRangeError(null));

    searchForm.addEventListener("submit", (e) => {
        e.preventDefault();
        const startDateValue = document.getElementById("startDate").value;
        const endDateValue = document.getElementById("endDate").value;
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
        setDateRangeError(null);
        searchReporterController.clear();
        searchCameramanController.clear();
        searchLocationController.clear();
        loadSearchPeopleOptions(document.getElementById("searchActiveOnly").checked);
        clearTermPiles();
    });

    resultsCloseBtn.addEventListener("click", closeResultsModal);

    resultsOverlay.addEventListener("click", (e) => {
        if (e.target === resultsOverlay) closeResultsModal();
    });

    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && !resultsOverlay.classList.contains("hidden")) {
            closeResultsModal();
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

        const startDate = document.getElementById("startDate").value;
        if (startDate) params.append("startDate", startDate);

        const endDate = document.getElementById("endDate").value;
        if (endDate) params.append("endDate", endDate);

        searchReporterController.ids().forEach((id) => params.append("reporterIds", id));
        searchCameramanController.ids().forEach((id) => params.append("cameramanIds", id));
        searchLocationController.ids().forEach((id) => params.append("locationIds", id));

        params.append("page", currentPage);
        params.append("size", PAGE_SIZE);

        try {
            const result = await apiFetch("/api/news?" + params.toString());
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
            resultsEmpty.textContent = "No news items match your search.";
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
        <td>${item.newsDate ?? ""}</td>
        <td>${escapeHtml(item.title ?? "")}</td>
        <td>${namesFor(item.cameramanIds, nameMaps.cameramen)}</td>
        <td>${namesFor(item.reporterIds, nameMaps.reporters)}</td>
        <td>${namesFor(item.locationIds, nameMaps.locations)}</td>
        <td>${item.numberOfFiles ?? ""}</td>
        <td>${item.totalSize != null ? item.totalSize.toFixed(2) : ""}</td>
      `;
            resultsBody.appendChild(row);
        });

        paginationEl.classList.toggle("hidden", totalPages <= 1);
        pageStatus.textContent = `Page ${page.number + 1} of ${totalPages}`;
        prevBtn.disabled = page.first;
        nextBtn.disabled = page.last;
    }

    function namesFor(ids, map) {
        if (!ids || ids.length === 0) return "\u2014";
        return Array.from(ids)
            .map((id) => escapeHtml(map[id] || ""))
            .filter(Boolean)
            .join(", ") || "\u2014";
    }

    function escapeHtml(str) {
        const div = document.createElement("div");
        div.textContent = str;
        return div.innerHTML;
    }

})();