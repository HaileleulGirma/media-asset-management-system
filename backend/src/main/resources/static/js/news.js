(function () {
    const NEWS_ENDPOINT = "/api/news";
    const MB_PER_GB = 1024;
    const PAGE_SIZE = 20;

    const isAdminOrStaff = Auth.hasRole("ADMIN") || Auth.hasRole("STAFF");
    const createPanel = document.getElementById("create-news-panel");
    const searchPanel = document.getElementById("search-news-panel");
    const modifyPanel = document.getElementById("modify-news-panel");
    const staffSummaryPanel = document.getElementById("staff-summary-panel");

    const resultsOverlay = document.getElementById("search-results-overlay");
    const resultsCloseBtn = document.getElementById("search-results-close");

    const tabSearchBtn = document.getElementById("tab-search-btn");
    const tabAddBtn = document.getElementById("tab-add-btn");
    const tabModifyBtn = document.getElementById("tab-modify-btn");
    const tabStaffBtn = document.getElementById("tab-staff-btn");

    const nameMaps = { reporters: {}, cameramen: {}, locations: {}, staff: {} };

    // Global GC/EC calendar toggle (top nav) + wraps every date field on
    // this page with the Ethiopian picker. The native inputs stay the
    // source of truth (always Gregorian "YYYY-MM-DD"), so every other
    // `.value` read in this file below is unaffected.
    DatePicker.mountToggle();
    DatePicker.attachAll([
        "newsDate", "startDate", "endDate", "modifySearchDate",
        "editNewsDate", "summaryStartDate", "summaryEndDate"
    ]);

    function showTab(tab) {
        const showAdd = tab === "add" && isAdminOrStaff;
        const showModify = tab === "modify" && isAdminOrStaff;
        const showStaff = tab === "staff" && isAdminOrStaff;
        const showSearch = tab === "search" || (!showAdd && !showModify && !showStaff);

        createPanel.classList.toggle("hidden", !showAdd);
        modifyPanel.classList.toggle("hidden", !showModify);
        staffSummaryPanel.classList.toggle("hidden", !showStaff);
        searchPanel.classList.toggle("hidden", !showSearch);

        tabSearchBtn.classList.toggle("active", showSearch);
        tabAddBtn.classList.toggle("active", showAdd);
        tabModifyBtn.classList.toggle("active", showModify);
        tabStaffBtn.classList.toggle("active", showStaff);
    }

    tabSearchBtn.addEventListener("click", () => showTab("search"));

    if (isAdminOrStaff) {
        tabAddBtn.classList.remove("hidden");
        tabAddBtn.addEventListener("click", () => showTab("add"));

        tabModifyBtn.classList.remove("hidden");
        tabModifyBtn.addEventListener("click", () => showTab("modify"));

        tabStaffBtn.classList.remove("hidden");
        tabStaffBtn.addEventListener("click", () => showTab("staff"));
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

            [...activeReporters, ...inactiveReporters].forEach((r) => (nameMaps.reporters[r.reporterId] = r.reporterName));
            [...activeCameramen, ...inactiveCameramen].forEach((c) => (nameMaps.cameramen[c.cameramanId] = c.cameramanName));
            [...localLocations, ...abroadLocations].forEach((l) => (nameMaps.locations[l.locationId] = l.locationName));

            searchLocationController.fillSelect(localLocations, "locationId", "locationName");
            await loadSearchPeopleOptions(document.getElementById("searchActiveOnly").checked);

            if (isAdminOrStaff) {
                createLocationController.fillSelect(localLocations, "locationId", "locationName");
                editLocationController.fillSelect(localLocations, "locationId", "locationName");

                await loadCreatePeopleOptions(document.getElementById("createActiveOnly").checked);
                await loadEditPeopleOptions(document.getElementById("editActiveOnly").checked);
            }
        } catch (err) {
            showAlert(searchAlertEl(), "Could not load filter data: " + (err.body?.message || err.message), "error");
        }
    }

    document.getElementById("searchActiveOnly").addEventListener("change", (e) => loadSearchPeopleOptions(e.target.checked));
    if (isAdminOrStaff) {
        document.getElementById("createActiveOnly").addEventListener("change", (e) => loadCreatePeopleOptions(e.target.checked));
        document.getElementById("editActiveOnly").addEventListener("change", (e) => loadEditPeopleOptions(e.target.checked));
    }

    function makeMultiSelectController(selectId, chipsId) {
        const selected = new Map();

        function fillSelect(items, idField, labelField) {
            const select = document.getElementById(selectId);
            const placeholder = select.querySelector("option[disabled]");
            select.innerHTML = "";
            if (placeholder) select.appendChild(placeholder);

            items.forEach((item) => {
                const opt = document.createElement("option");
                opt.value = item[idField];
                opt.textContent = item[labelField];
                if (selected.has(String(item[idField]))) {
                    opt.selected = true;
                }
                select.appendChild(opt);
            });
            if (placeholder && !Array.from(select.options).some((o) => o.selected && o !== placeholder)) {
                placeholder.selected = true;
            }
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

                    const placeholder = select.querySelector("option[disabled]");
                    if (placeholder && !Array.from(select.options).some((o) => o.selected && o !== placeholder)) {
                        placeholder.selected = true;
                    }
                    renderChipsFn();
                });

                chip.appendChild(removeBtn);
                container.appendChild(chip);
            });
        }

        document.getElementById(selectId).addEventListener("change", () => {
            const select = document.getElementById(selectId);
            Array.from(select.options).forEach((opt) => {
                if (opt.disabled) return;
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
            setIds(idsArray, mapObj) {
                selected.clear();
                (idsArray || []).forEach((id) => {
                    if (mapObj[id]) {
                        selected.set(String(id), mapObj[id]);
                    }
                });
                const select = document.getElementById(selectId);
                Array.from(select.options).forEach((opt) => {
                    if (!opt.disabled) {
                        opt.selected = selected.has(opt.value);
                    }
                });
                renderChipsFn();
            },
        };
    }

    const searchLocationController = makeMultiSelectController("searchLocationIds", "searchLocationChips");
    const createLocationController = makeMultiSelectController("locationIds", "locationChips");
    const editLocationController = makeMultiSelectController("editLocationIds", "editLocationChips");

    const searchReporterController = makeMultiSelectController("searchReporterIds", "searchReporterChips");
    const searchCameramanController = makeMultiSelectController("searchCameramanIds", "searchCameramanChips");

    const createReporterController = makeMultiSelectController("reporterIds", "reporterChips");
    const createCameramanController = makeMultiSelectController("cameramanIds", "cameramanChips");

    const editReporterController = makeMultiSelectController("editReporterIds", "editReporterChips");
    const editCameramanController = makeMultiSelectController("editCameramanIds", "editCameramanChips");

    document.getElementById("searchAbroadOnly").addEventListener("change", async (e) => {
        try {
            const locations = await apiFetch(`/api/location?abroadOnly=${e.target.checked}`);
            searchLocationController.fillSelect(locations, "locationId", "locationName");
        } catch (err) {
            showAlert(searchAlertEl(), "Could not load locations: " + (err.body?.message || err.message), "error");
        }
    });

    if (isAdminOrStaff) {
        document.getElementById("createAbroadOnly").addEventListener("change", async (e) => {
            try {
                const locations = await apiFetch(`/api/location?abroadOnly=${e.target.checked}`);
                createLocationController.fillSelect(locations, "locationId", "locationName");
            } catch (err) {
                showAlert(createAlertEl(), "Could not load locations: " + (err.body?.message || err.message), "error");
            }
        });

        document.getElementById("editAbroadOnly").addEventListener("change", async (e) => {
            try {
                const locations = await apiFetch(`/api/location?abroadOnly=${e.target.checked}`);
                editLocationController.fillSelect(locations, "locationId", "locationName");
            } catch (err) {
                showAlert(document.getElementById("edit-news-alert"), "Could not load locations: " + (err.body?.message || err.message), "error");
            }
        });
    }

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
    function loadEditPeopleOptions(activeOnly) {
        return loadPeopleOptions(activeOnly, editReporterController, editCameramanController, document.getElementById("edit-news-alert"));
    }

    if (isAdminOrStaff) {
        loadStaffData();
    }

    // Fills the create/edit staff <select>s AND records id->name in
    // nameMaps.staff so the modify table can show "Imported by" /
    // "Ingested by" without extra requests.
    async function loadStaffData() {
        try {
            const staff = await apiFetch("/api/staffmember?activeOnly=true");
            staff.forEach((s) => (nameMaps.staff[s.staffMemberId] = s.staffMemberName));

            fillSingleSelect("importerId", staff, "staffMemberId", "staffMemberName");
            fillSingleSelect("ingestorId", staff, "staffMemberId", "staffMemberName");
            fillSingleSelect("editImporterId", staff, "staffMemberId", "staffMemberName");
            fillSingleSelect("editIngestorId", staff, "staffMemberId", "staffMemberName");
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

    function numberOrNull(value) {
        return value === "" ? null : Number(value);
    }

    function clearFieldErrors(form) {
        form.querySelectorAll(".field").forEach((f) => {
            f.classList.remove("has-error");
            const errEl = f.querySelector(".field-error");
            if (errEl) errEl.textContent = "";
        });
    }

    // ---------- Create news form ----------

    const form = document.getElementById("create-news-form");
    function createAlertEl() { return document.getElementById("create-news-alert"); }
    const submitBtn = document.getElementById("create-news-submit");

    function refreshCreateLists() {
        loadCreatePeopleOptions(document.getElementById("createActiveOnly").checked);
        apiFetch(`/api/location?abroadOnly=${document.getElementById("createAbroadOnly").checked}`)
            .then((locations) => createLocationController.fillSelect(locations, "locationId", "locationName"))
            .catch((err) => showAlert(createAlertEl(), "Could not load locations: " + (err.body?.message || err.message), "error"));
    }

    if (form) {
        const clearFormBtn = document.getElementById("create-news-reset");
        clearFormBtn.addEventListener("click", () => {
            form.reset();
            DatePicker.refresh("newsDate");
            hideAlert(createAlertEl());
            clearFieldErrors(form);
            createReporterController.clear();
            createCameramanController.clear();
            createLocationController.clear();
            refreshCreateLists();
        });

        form.addEventListener("submit", async (e) => {
            e.preventDefault();
            const alertEl = createAlertEl();
            hideAlert(alertEl);
            clearFieldErrors(form);
            submitBtn.disabled = true;
            submitBtn.textContent = "Saving...";

            const rawSize = document.getElementById("totalSize").value;
            const sizeUnit = document.getElementById("totalSizeUnit").value;
            let finalSize = rawSize === "" ? null : (sizeUnit === "MB" ? Number(rawSize) / MB_PER_GB : Number(rawSize));

            const payload = {
                title: document.getElementById("title").value.trim(),
                newsDate: document.getElementById("newsDate").value || null,
                filePath: document.getElementById("filePath").value.trim(),
                numberOfFiles: numberOrNull(document.getElementById("numberOfFiles").value),
                totalSize: finalSize,
                importerId: numberOrNull(document.getElementById("importerId").value),
                ingestorId: numberOrNull(document.getElementById("ingestorId").value),
                reporterIds: createReporterController.ids(),
                cameramanIds: createCameramanController.ids(),
                locationIds: createLocationController.ids(),
            };

            try {
                await apiFetch(NEWS_ENDPOINT, { method: "POST", body: payload });
                form.reset();
                DatePicker.refresh("newsDate");
                clearFieldErrors(form);
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
    }

    // ---------- Modify / Delete News ----------
    if (isAdminOrStaff) {
        const modifySearchBtn = document.getElementById("modifySearchBtn");
        const modifySearchDate = document.getElementById("modifySearchDate");
        const modifyListAlert = document.getElementById("modify-list-alert");
        const modifyResultsBody = document.getElementById("modify-results-body");
        const modifyResultsTable = document.getElementById("modify-results-table");

        const editOverlay = document.getElementById("edit-news-overlay");
        const editForm = document.getElementById("edit-news-form");
        const editCancelBtn = document.getElementById("edit-news-cancel");
        const editCloseBtn = document.getElementById("edit-news-close");
        const editSubmitBtn = document.getElementById("edit-news-submit");
        let currentEditVersion = null;

        modifySearchBtn.addEventListener("click", async () => {
            const dateVal = modifySearchDate.value;
            if (!dateVal) {
                showAlert(modifyListAlert, "Please select a date to search.", "error");
                return;
            }
            hideAlert(modifyListAlert);
            modifySearchBtn.disabled = true;
            modifySearchBtn.textContent = "Loading...";

            try {
                const result = await apiFetch(`${NEWS_ENDPOINT}?startDate=${dateVal}&size=100`);
                renderModifyResults(result.content || []);
            } catch (err) {
                showAlert(modifyListAlert, "Could not load news: " + (err.body?.message || err.message), "error");
            } finally {
                modifySearchBtn.disabled = false;
                modifySearchBtn.textContent = "Load news";
            }
        });

        // Same columns/name-resolution as the search results table, plus
        // Imported by / Ingested by so missing-ingestor items are easy to
        // spot at a glance (a news item can exist without an ingestor,
        // since ingestion always happens after import).
        function renderModifyResults(items) {
            modifyResultsBody.innerHTML = "";
            if (items.length === 0) {
                modifyResultsTable.classList.add("hidden");
                showAlert(modifyListAlert, "No news found for this date.", "error");
                return;
            }
            hideAlert(modifyListAlert);
            modifyResultsTable.classList.remove("hidden");

            items.forEach((item) => {
                const importerName = item.importerId != null
                    ? escapeHtml(nameMaps.staff[item.importerId] || "")
                    : "\u2014";
                const ingestorCell = item.ingestorId != null
                    ? escapeHtml(nameMaps.staff[item.ingestorId] || "")
                    : '<span class="badge-missing">Not ingested</span>';

                const row = document.createElement("tr");
                row.innerHTML = `
                    <td>${item.newsDate ?? ""}</td>
                    <td>${escapeHtml(item.title ?? "")}</td>
                    <td>${namesFor(item.cameramanIds, nameMaps.cameramen)}</td>
                    <td>${namesFor(item.reporterIds, nameMaps.reporters)}</td>
                    <td>${namesFor(item.locationIds, nameMaps.locations)}</td>
                    <td>${item.numberOfFiles ?? ""}</td>
                    <td>${item.totalSize != null ? item.totalSize.toFixed(2) : ""}</td>
                    <td>${importerName}</td>
                    <td>${ingestorCell}</td>
                    <td>
                        <div class="row-actions">
                            <button type="button" class="btn btn-secondary btn-sm edit-btn">Edit</button>
                            <button type="button" class="btn btn-danger btn-sm delete-btn">Delete</button>
                        </div>
                    </td>
                `;

                row.querySelector(".edit-btn").addEventListener("click", () => openEditModal(item));
                row.querySelector(".delete-btn").addEventListener("click", () => deleteNewsItem(item.newsId));
                modifyResultsBody.appendChild(row);
            });
        }

        async function deleteNewsItem(id) {
            if (!confirm("Are you sure you want to permanently delete this news item?")) return;
            try {
                await apiFetch(`${NEWS_ENDPOINT}/${id}`, { method: "DELETE" });
                showAlert(modifyListAlert, "News item deleted successfully.", "success");
                modifySearchBtn.click();
            } catch (err) {
                showAlert(modifyListAlert, "Failed to delete: " + (err.body?.message || err.message), "error");
            }
        }

        function openEditModal(item) {
            editOverlay.classList.remove("hidden");
            hideAlert(document.getElementById("edit-news-alert"));
            editForm.reset();
            clearFieldErrors(editForm);

            currentEditVersion = item.version ?? null;

            document.getElementById("editNewsId").value = item.newsId;
            document.getElementById("editTitle").value = item.title || "";
            document.getElementById("editNewsDate").value = item.newsDate || "";
            DatePicker.refresh("editNewsDate");
            document.getElementById("editFilePath").value = item.filePath || "";
            document.getElementById("editNumberOfFiles").value = item.numberOfFiles || "";

            if (item.totalSize != null) {
                document.getElementById("editTotalSize").value = item.totalSize;
                document.getElementById("editTotalSizeUnit").value = "GB";
            } else {
                document.getElementById("editTotalSize").value = "";
            }

            document.getElementById("editImporterId").value = item.importerId || "";
            document.getElementById("editIngestorId").value = item.ingestorId || "";

            editLocationController.setIds(item.locationIds || [], nameMaps.locations);
            editReporterController.setIds(item.reporterIds || [], nameMaps.reporters);
            editCameramanController.setIds(item.cameramanIds || [], nameMaps.cameramen);
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
            if (e.key === "Escape") {
                if (!editOverlay.classList.contains("hidden")) closeEditModal();
                else if (!resultsOverlay.classList.contains("hidden")) closeResultsModal();
            }
        });

        editForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const alertEl = document.getElementById("edit-news-alert");
            hideAlert(alertEl);
            clearFieldErrors(editForm);
            editSubmitBtn.disabled = true;
            editSubmitBtn.textContent = "Saving...";

            const id = document.getElementById("editNewsId").value;

            const rawSize = document.getElementById("editTotalSize").value;
            const sizeUnit = document.getElementById("editTotalSizeUnit").value;
            let finalSize = rawSize === "" ? null : (sizeUnit === "MB" ? Number(rawSize) / MB_PER_GB : Number(rawSize));

            const payload = {
                title: document.getElementById("editTitle").value.trim(),
                newsDate: document.getElementById("editNewsDate").value || null,
                filePath: document.getElementById("editFilePath").value.trim(),
                numberOfFiles: numberOrNull(document.getElementById("editNumberOfFiles").value),
                totalSize: finalSize,
                importerId: numberOrNull(document.getElementById("editImporterId").value),
                ingestorId: numberOrNull(document.getElementById("editIngestorId").value),
                reporterIds: editReporterController.ids(),
                cameramanIds: editCameramanController.ids(),
                locationIds: editLocationController.ids(),
                version: currentEditVersion,
            };

            try {
                await apiFetch(`${NEWS_ENDPOINT}/${id}`, { method: "PUT", body: payload });
                closeEditModal();
                showAlert(modifyListAlert, "News item updated successfully.", "success");
                modifySearchBtn.click();
            } catch (err) {
                if (err.status === 409) {
                    showAlert(alertEl, "This news item was changed by someone else. Close this dialog and reload the list to try again.", "error");
                } else {
                    showFormErrors(editForm, err, alertEl);
                }
            } finally {
                editSubmitBtn.disabled = false;
                editSubmitBtn.textContent = "Save changes";
            }
        });
    }

    // ---------- Staff summary ----------
    if (isAdminOrStaff) {
        const summaryActiveOnly = document.getElementById("summaryActiveOnly");
        const summaryStaffSelect = document.getElementById("summaryStaffId");
        const summaryLoadBtn = document.getElementById("summaryLoadBtn");

        const summaryResultsOverlay = document.getElementById("summary-results-overlay");
        const summaryResultsCloseBtn = document.getElementById("summary-results-close");
        const summaryModalLoading = document.getElementById("summary-modal-loading");
        const summaryModalEmpty = document.getElementById("summary-modal-empty");
        const summaryModalContent = document.getElementById("summary-modal-content");

        // Full lists for the current staff member/date range, fetched once.
        // Totals are computed from these; the two tables below are just a
        // client-side page into them, kept independent so paging one list
        // never touches the other's page.
        let importedItems = [];
        let ingestedItems = [];
        let importedPage = 0;
        let ingestedPage = 0;

        function summaryAlertEl() {
            return document.getElementById("summary-alert");
        }

        // Populates the staff dropdown for the given active/inactive state.
        // Staff, like reporters/cameramen, has no "everyone" endpoint --
        // activeOnly=true returns active staff, activeOnly=false returns
        // inactive staff -- so the checkbox drives a single re-fetch,
        // same pattern as the reporter/cameraman active-only toggles.
        async function loadSummaryStaffOptions(activeOnly) {
            try {
                const staff = await apiFetch(`/api/staffmember?activeOnly=${activeOnly}`);
                const previousValue = summaryStaffSelect.value;
                summaryStaffSelect.innerHTML = '<option value="">Select staff member...</option>';
                staff.forEach((s) => {
                    const opt = document.createElement("option");
                    opt.value = s.staffMemberId;
                    opt.textContent = s.staffMemberName;
                    summaryStaffSelect.appendChild(opt);
                });
                if (staff.some((s) => String(s.staffMemberId) === previousValue)) {
                    summaryStaffSelect.value = previousValue;
                }
            } catch (err) {
                showAlert(summaryAlertEl(), "Could not load staff list: " + (err.body?.message || err.message), "error");
            }
        }

        summaryActiveOnly.addEventListener("change", (e) => loadSummaryStaffOptions(e.target.checked));
        loadSummaryStaffOptions(summaryActiveOnly.checked);

        // Staff member and both dates are required -- a summary is always
        // scoped to a date range, never "all time". Same has-error /
        // field-error convention as the search tab's date range check.
        function setSummaryFieldError(inputId, message) {
            const field = document.getElementById(inputId).closest(".field");
            if (!field) return;
            field.classList.toggle("has-error", Boolean(message));
            const errEl = field.querySelector(".field-error");
            if (errEl) errEl.textContent = message || "";
        }

        function validateSummaryInputs(staffId, startDate, endDate) {
            let valid = true;
            setSummaryFieldError("summaryStaffId", null);
            setSummaryFieldError("summaryStartDate", null);
            setSummaryFieldError("summaryEndDate", null);

            if (!staffId) {
                setSummaryFieldError("summaryStaffId", "Please select a staff member.");
                valid = false;
            }
            if (!startDate) {
                setSummaryFieldError("summaryStartDate", "From date is required.");
                valid = false;
            }
            if (!endDate) {
                setSummaryFieldError("summaryEndDate", "To date is required.");
                valid = false;
            }
            if (startDate && endDate && startDate > endDate) {
                setSummaryFieldError("summaryStartDate", '"From date" cannot be later than "To date".');
                setSummaryFieldError("summaryEndDate", '"From date" cannot be later than "To date".');
                valid = false;
            }
            return valid;
        }

        summaryStaffSelect.addEventListener("change", () => setSummaryFieldError("summaryStaffId", null));
        document.getElementById("summaryStartDate").addEventListener("input", () => setSummaryFieldError("summaryStartDate", null));
        document.getElementById("summaryEndDate").addEventListener("input", () => setSummaryFieldError("summaryEndDate", null));

        function openSummaryModal() {
            summaryResultsOverlay.classList.remove("hidden");
            summaryModalLoading.classList.remove("hidden");
            summaryModalEmpty.classList.add("hidden");
            summaryModalContent.classList.add("hidden");
        }

        function closeSummaryModal() {
            summaryResultsOverlay.classList.add("hidden");
        }

        summaryResultsCloseBtn.addEventListener("click", closeSummaryModal);
        summaryResultsOverlay.addEventListener("click", (e) => {
            if (e.target === summaryResultsOverlay) closeSummaryModal();
        });
        document.addEventListener("keydown", (e) => {
            if (e.key === "Escape" && !summaryResultsOverlay.classList.contains("hidden")) {
                closeSummaryModal();
            }
        });

        // Pulls every page of news matching a single filter param (importerId
        // or ingestorId) plus the mandatory date range, so the totals and the
        // on-screen pagination both reflect ALL of that staff member's news
        // in-range, not just whatever the backend's first page returned.
        async function fetchAllNews(paramName, paramValue, startDate, endDate) {
            const all = [];
            let page = 0;
            const size = 100;

            while (true) {
                const params = new URLSearchParams();
                params.append(paramName, paramValue);
                params.append("startDate", startDate);
                params.append("endDate", endDate);
                params.append("page", page);
                params.append("size", size);

                const result = await apiFetch(`${NEWS_ENDPOINT}?${params.toString()}`);
                const content = result.content || [];
                all.push(...content);

                if (result.last || content.length === 0) break;
                page += 1;
            }

            return all;
        }

        function summaryTotals(items) {
            return items.reduce(
                (acc, item) => {
                    acc.count += 1;
                    acc.files += item.numberOfFiles || 0;
                    acc.size += item.totalSize || 0;
                    return acc;
                },
                { count: 0, files: 0, size: 0 }
            );
        }

        function totalPagesFor(items) {
            return Math.max(1, Math.ceil(items.length / PAGE_SIZE));
        }

        function paginateItems(items, page) {
            const start = page * PAGE_SIZE;
            return items.slice(start, start + PAGE_SIZE);
        }

        // Same row shape as the search/modify results tables -- date,
        // title, cameramen/reporters/locations resolved via nameMaps,
        // files, size -- so a staff member's news reads consistently
        // with the rest of the app.
        function renderSummaryTable(bodyId, items) {
            const body = document.getElementById(bodyId);
            body.innerHTML = "";
            items.forEach((item) => {
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
                body.appendChild(row);
            });
        }

        function renderImportedPage() {
            const total = totalPagesFor(importedItems);
            if (importedPage >= total) importedPage = total - 1;
            if (importedPage < 0) importedPage = 0;

            renderSummaryTable("summary-imported-body", paginateItems(importedItems, importedPage));
            document.getElementById("summary-imported-page-status").textContent = `Page ${importedPage + 1} of ${total}`;
            document.getElementById("summary-imported-prev").disabled = importedPage === 0;
            document.getElementById("summary-imported-next").disabled = importedPage >= total - 1;
        }

        function renderIngestedPage() {
            const total = totalPagesFor(ingestedItems);
            if (ingestedPage >= total) ingestedPage = total - 1;
            if (ingestedPage < 0) ingestedPage = 0;

            renderSummaryTable("summary-ingested-body", paginateItems(ingestedItems, ingestedPage));
            document.getElementById("summary-ingested-page-status").textContent = `Page ${ingestedPage + 1} of ${total}`;
            document.getElementById("summary-ingested-prev").disabled = ingestedPage === 0;
            document.getElementById("summary-ingested-next").disabled = ingestedPage >= total - 1;
        }

        document.getElementById("summary-imported-prev").addEventListener("click", () => {
            importedPage -= 1;
            renderImportedPage();
        });
        document.getElementById("summary-imported-next").addEventListener("click", () => {
            importedPage += 1;
            renderImportedPage();
        });
        document.getElementById("summary-ingested-prev").addEventListener("click", () => {
            ingestedPage -= 1;
            renderIngestedPage();
        });
        document.getElementById("summary-ingested-next").addEventListener("click", () => {
            ingestedPage += 1;
            renderIngestedPage();
        });

        function renderSummary(imported, ingested) {
            importedItems = imported;
            ingestedItems = ingested;
            importedPage = 0;
            ingestedPage = 0;

            const importedTotals = summaryTotals(imported);
            const ingestedTotals = summaryTotals(ingested);

            document.getElementById("summaryImportedCount").textContent = importedTotals.count;
            document.getElementById("summaryImportedFiles").textContent = importedTotals.files;
            document.getElementById("summaryImportedSize").textContent = importedTotals.size.toFixed(2);

            document.getElementById("summaryIngestedCount").textContent = ingestedTotals.count;
            document.getElementById("summaryIngestedFiles").textContent = ingestedTotals.files;
            document.getElementById("summaryIngestedSize").textContent = ingestedTotals.size.toFixed(2);

            renderImportedPage();
            renderIngestedPage();

            summaryModalLoading.classList.add("hidden");

            const hasAny = imported.length > 0 || ingested.length > 0;
            summaryModalContent.classList.toggle("hidden", !hasAny);
            summaryModalEmpty.classList.toggle("hidden", hasAny);
        }

        summaryLoadBtn.addEventListener("click", async () => {
            const staffId = summaryStaffSelect.value;
            const startDate = document.getElementById("summaryStartDate").value;
            const endDate = document.getElementById("summaryEndDate").value;

            if (!validateSummaryInputs(staffId, startDate, endDate)) {
                return;
            }

            hideAlert(summaryAlertEl());
            summaryLoadBtn.disabled = true;
            summaryLoadBtn.textContent = "Loading...";
            openSummaryModal();

            try {
                const [imported, ingested] = await Promise.all([
                    fetchAllNews("importerId", staffId, startDate, endDate),
                    fetchAllNews("ingestorId", staffId, startDate, endDate),
                ]);
                renderSummary(imported, ingested);
            } catch (err) {
                closeSummaryModal();
                showAlert(summaryAlertEl(), "Could not load summary: " + (err.body?.message || err.message), "error");
            } finally {
                summaryLoadBtn.disabled = false;
                summaryLoadBtn.textContent = "Load summary";
            }
        });
    }

    // ---------- Search news ----------

    const searchForm = document.getElementById("search-news-form");
    function searchAlertEl() { return document.getElementById("search-results-empty"); }
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

    // "To date" is only meaningful once a "From date" has been chosen, so
    // it stays disabled (and gets cleared) until startDate has a value --
    // this is the from/to dependency requested, kept local to this form.
    const startDateInput = document.getElementById("startDate");
    const endDateInput = document.getElementById("endDate");

    function syncEndDateAvailability() {
        const hasStart = Boolean(startDateInput.value);
        endDateInput.disabled = !hasStart;
        if (!hasStart) {
            endDateInput.value = "";
        }
    }

    startDateInput.addEventListener("input", () => {
        setDateRangeError(null);
        syncEndDateAvailability();
    });
    endDateInput.addEventListener("input", () => setDateRangeError(null));

    syncEndDateAvailability();

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
        DatePicker.refresh("startDate");
        DatePicker.refresh("endDate");
        setDateRangeError(null);
        searchReporterController.clear();
        searchCameramanController.clear();
        searchLocationController.clear();
        loadSearchPeopleOptions(document.getElementById("searchActiveOnly").checked);
        clearTermPiles();
        syncEndDateAvailability();
    });

    resultsCloseBtn.addEventListener("click", closeResultsModal);

    resultsOverlay.addEventListener("click", (e) => {
        if (e.target === resultsOverlay) closeResultsModal();
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

        // A term can only sit in one pile at a time. Adding it to a new pile
        // pulls it out of whichever pile(s) it was already in, so e.g. moving
        // a term from "Must include" to "Exclude" removes the old chip too.
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
            const result = await apiFetch(NEWS_ENDPOINT + "?" + params.toString());
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