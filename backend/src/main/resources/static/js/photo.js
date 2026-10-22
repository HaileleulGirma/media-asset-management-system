(function () {
    const PHOTO_ENDPOINT = "/api/photos";
    const MB_PER_GB = 1024;
    const PAGE_SIZE = 20;

    const isAdminOrStaff = Auth.hasRole("ADMIN") || Auth.hasRole("STAFF");

    function removeById(id) {
        const el = document.getElementById(id);
        if (el) el.remove();
    }

    // Viewers can only search. The tab bar, add/modify panels, edit modal and
    // the staff filter are removed from the DOM (not just hidden), so there is
    // nothing to un-hide. Display convenience only -- the server enforces
    // access via @PreAuthorize.
    if (!isAdminOrStaff) {
        [
            "tab-bar", "create-photo-panel", "modify-photo-panel", "staff-summary-panel",
            "edit-photo-overlay", "summary-results-overlay", "searchImporterField",
        ].forEach(removeById);
    }

    const searchPanel = document.getElementById("search-photo-panel");
    const createPanel = document.getElementById("create-photo-panel");   // null for Viewers
    const modifyPanel = document.getElementById("modify-photo-panel");   // null for Viewers

    const resultsOverlay = document.getElementById("search-results-overlay");
    const resultsCloseBtn = document.getElementById("search-results-close");
    const searchAlert = document.getElementById("search-alert");

    // Global GC/EC toggle + Ethiopian picker on every date field. Ids that
    // were removed above for Viewers are simply skipped by attach().
    DatePicker.mountToggle();
    DatePicker.attachAll([
        "photoDate", "startDate", "endDate", "modifySearchDate",
        "editPhotoDate", "summaryStartDate", "summaryEndDate",
    ]);

    // ---------- Tabs ----------

    const tabs = {
        search: { btn: document.getElementById("tab-search-btn"), panel: searchPanel },
        add: { btn: document.getElementById("tab-add-btn"), panel: createPanel },
        modify: { btn: document.getElementById("tab-modify-btn"), panel: modifyPanel },
        summary: { btn: document.getElementById("tab-summary-btn"), panel: document.getElementById("staff-summary-panel") },
    };

    function showTab(name) {
        const active = tabs[name] && (name === "search" || isAdminOrStaff) ? name : "search";
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

    // The API stores photo size in MB. The forms accept MB or GB and convert
    // to MB before sending (rounded to 2 decimals, matching NUMERIC(12,2)).
    function sizeToMb(valueId, unitId) {
        const raw = document.getElementById(valueId).value;
        if (raw === "") return null;
        const value = Number(raw);
        const unit = document.getElementById(unitId).value;
        return unit === "GB" ? Math.round(value * MB_PER_GB * 100) / 100 : value;
    }

    function formatSize(n) {
        return n != null ? Number(n).toFixed(2) : "";
    }

    function namesFor(ids, map) {
        if (!ids || ids.length === 0) return "—";
        return Array.from(ids)
            .map((id) => escapeHtml(map[id] || ""))
            .filter(Boolean)
            .join(", ") || "—";
    }

    // Keeps the first (placeholder) option and replaces the rest.
    function fillFirstOptionSelect(selectId, items, idField, labelField) {
        const select = document.getElementById(selectId);
        if (!select) return;
        while (select.options.length > 1) select.remove(1);
        items.forEach((item) => {
            const opt = document.createElement("option");
            opt.value = item[idField];
            opt.textContent = item[labelField];
            select.appendChild(opt);
        });
    }

    // ---------- Multi-select with chips (cameramen) ----------
    // Null-safe: for Viewers the create/edit elements do not exist.

    function makeMultiSelectController(selectId, chipsId) {
        const selected = new Map(); // id (string) -> label

        const selectEl = () => document.getElementById(selectId);
        const chipsEl = () => document.getElementById(chipsId);

        function renderChips() {
            const container = chipsEl();
            if (!container) return;
            container.innerHTML = "";
            selected.forEach((label, id) => {
                const chip = document.createElement("span");
                chip.className = "chip";
                chip.textContent = label;

                const removeBtn = document.createElement("button");
                removeBtn.type = "button";
                removeBtn.className = "chip-remove";
                removeBtn.setAttribute("aria-label", "Remove " + label);
                removeBtn.textContent = "×";
                removeBtn.addEventListener("click", () => {
                    selected.delete(id);
                    const select = selectEl();
                    const opt = select && Array.from(select.options).find((o) => o.value === id);
                    if (opt) opt.selected = false;
                    renderChips();
                });

                chip.appendChild(removeBtn);
                container.appendChild(chip);
            });
        }

        function fillSelect(items, idField, labelField) {
            const select = selectEl();
            if (!select) return;
            select.innerHTML = "";
            items.forEach((item) => {
                const opt = document.createElement("option");
                opt.value = item[idField];
                opt.textContent = item[labelField];
                opt.selected = selected.has(String(item[idField]));
                select.appendChild(opt);
            });
        }

        const initial = selectEl();
        if (initial) {
            initial.addEventListener("change", () => {
                Array.from(initial.options).forEach((opt) => {
                    if (opt.selected) selected.set(opt.value, opt.textContent);
                    else selected.delete(opt.value);
                });
                renderChips();
            });
        }

        return {
            fillSelect,
            renderChips,
            clear() {
                selected.clear();
                const select = selectEl();
                if (select) Array.from(select.options).forEach((o) => (o.selected = false));
                renderChips();
            },
            ids() {
                return Array.from(selected.keys()).map(Number);
            },
            // Unknown ids still get a chip ("#12") so a cameraman missing
            // from the name map is never silently dropped on save.
            setIds(idsArray, mapObj) {
                selected.clear();
                (idsArray || []).forEach((id) => selected.set(String(id), mapObj[id] || "#" + id));
                const select = selectEl();
                if (select) Array.from(select.options).forEach((o) => (o.selected = selected.has(o.value)));
                renderChips();
            },
        };
    }

    const createCameramanController = makeMultiSelectController("cameramanIds", "cameramanChips");
    const editCameramanController = makeMultiSelectController("editCameramanIds", "editCameramanChips");

    // ---------- Reference data (cameramen + staff) ----------
    // Both active and inactive are loaded: names of inactive people are still
    // needed to display old photos, and the "Active only" toggles pick which
    // list a dropdown shows. Staff data is Admin/Staff only; if Viewers
    // cannot read cameramen either, the cameraman filter is just hidden.

    const nameMaps = { cameramen: {}, staff: {} };
    let activeCameramen = [];
    let inactiveCameramen = [];
    let activeStaff = [];
    let inactiveStaff = [];

    const searchActiveOnly = document.getElementById("searchActiveOnly");
    const createActiveOnly = document.getElementById("createActiveOnly");   // null for Viewers
    const editActiveOnly = document.getElementById("editActiveOnly");       // null for Viewers
    const summaryActiveOnly = document.getElementById("summaryActiveOnly"); // null for Viewers

    function valueOrEmpty(result) {
        return result && result.status === "fulfilled" && Array.isArray(result.value) ? result.value : [];
    }

    async function loadReferenceData() {
        const calls = [
            apiFetch("/api/cameraman?activeOnly=true"),
            apiFetch("/api/cameraman?activeOnly=false"),
        ];
        if (isAdminOrStaff) {
            calls.push(apiFetch("/api/staffmember?activeOnly=true"));
            calls.push(apiFetch("/api/staffmember?activeOnly=false"));
        }
        const [camActive, camInactive, staffActive, staffInactive] = await Promise.allSettled(calls);

        activeCameramen = valueOrEmpty(camActive);
        inactiveCameramen = valueOrEmpty(camInactive);
        activeStaff = valueOrEmpty(staffActive);
        inactiveStaff = valueOrEmpty(staffInactive);

        [...activeCameramen, ...inactiveCameramen].forEach((c) => (nameMaps.cameramen[c.cameramanId] = c.cameramanName));
        [...activeStaff, ...inactiveStaff].forEach((s) => (nameMaps.staff[s.staffMemberId] = s.staffMemberName));

        const camFailed = camActive.status === "rejected" && camInactive.status === "rejected";
        const staffFailed = isAdminOrStaff && staffActive.status === "rejected" && staffInactive.status === "rejected";

        if (camFailed) {
            if (isAdminOrStaff) {
                const err = camActive.reason;
                showAlert(searchAlert, "Could not load cameramen: " + (err?.body?.message || err?.message), "error");
            } else {
                removeById("searchCameramanField");
            }
        }
        if (staffFailed) {
            const err = staffActive.reason;
            showAlert(searchAlert, "Could not load staff: " + (err?.body?.message || err?.message), "error");
        }

        fillSearchLists();
        if (isAdminOrStaff) {
            fillCreateStaff();
            fillCreateCameramen();
            fillEditCameramen();
            fillSummaryStaff();
        }
    }

    function fillSearchLists() {
        const activeOnly = searchActiveOnly.checked;
        fillFirstOptionSelect("searchCameramanId", activeOnly ? activeCameramen : inactiveCameramen, "cameramanId", "cameramanName");
        fillFirstOptionSelect("searchImporterId", activeOnly ? activeStaff : inactiveStaff, "staffMemberId", "staffMemberName");
    }

    function fillCreateStaff() {
        fillFirstOptionSelect("importedBy", activeStaff, "staffMemberId", "staffMemberName");
    }

    function fillCreateCameramen() {
        if (!createActiveOnly) return;
        createCameramanController.fillSelect(createActiveOnly.checked ? activeCameramen : inactiveCameramen, "cameramanId", "cameramanName");
        createCameramanController.renderChips();
    }

    function fillEditCameramen() {
        if (!editActiveOnly) return;
        editCameramanController.fillSelect(editActiveOnly.checked ? activeCameramen : inactiveCameramen, "cameramanId", "cameramanName");
        editCameramanController.renderChips();
    }

    // Staff dropdown for the summary: active or inactive staff per the toggle,
    // keeping the current choice if it is still in the list.
    function fillSummaryStaff() {
        const select = document.getElementById("summaryStaffId");
        if (!select || !summaryActiveOnly) return;
        const previous = select.value;
        fillFirstOptionSelect("summaryStaffId", summaryActiveOnly.checked ? activeStaff : inactiveStaff, "staffMemberId", "staffMemberName");
        if (Array.from(select.options).some((o) => o.value === previous)) select.value = previous;
    }

    searchActiveOnly.addEventListener("change", fillSearchLists);
    if (summaryActiveOnly) summaryActiveOnly.addEventListener("change", fillSummaryStaff);
    if (createActiveOnly) createActiveOnly.addEventListener("change", fillCreateCameramen);
    if (editActiveOnly) editActiveOnly.addEventListener("change", fillEditCameramen);

    loadReferenceData();

    // ---------- Add photo item (ADMIN + STAFF) ----------

    const createForm = document.getElementById("create-photo-form");
    if (createForm) {
        const alertEl = document.getElementById("create-photo-alert");
        const submitBtn = document.getElementById("create-photo-submit");

        function resetCreateForm() {
            createForm.reset();
            DatePicker.refresh("photoDate");
            clearFieldErrors(createForm);
            createCameramanController.clear();
            fillCreateCameramen();
        }

        document.getElementById("create-photo-reset").addEventListener("click", () => {
            hideAlert(alertEl);
            resetCreateForm();
        });

        createForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            hideAlert(alertEl);
            clearFieldErrors(createForm);
            submitBtn.disabled = true;
            submitBtn.textContent = "Saving...";

            const payload = {
                cameramanIds: createCameramanController.ids(),
                title: document.getElementById("title").value.trim(),
                filePath: document.getElementById("filePath").value.trim(),
                numberOfFiles: numberOrNull(document.getElementById("numberOfFiles").value),
                fileSizeMb: sizeToMb("fileSize", "fileSizeUnit"),
                photoDate: document.getElementById("photoDate").value || null,
                importedBy: numberOrNull(document.getElementById("importedBy").value),
            };

            try {
                await apiFetch(PHOTO_ENDPOINT, { method: "POST", body: payload });
                resetCreateForm();
                showAlert(alertEl, "Photo item added.", "success");
                alertEl.scrollIntoView({ behavior: "smooth", block: "center" });
            } catch (err) {
                showFormErrors(createForm, err, alertEl);
                alertEl.scrollIntoView({ behavior: "smooth", block: "center" });
            } finally {
                submitBtn.disabled = false;
                submitBtn.textContent = "Add photo item";
            }
        });
    }

    // ---------- Modify / delete photos (ADMIN + STAFF) ----------

    if (isAdminOrStaff) {
        const modifySearchBtn = document.getElementById("modifySearchBtn");
        const modifySearchDate = document.getElementById("modifySearchDate");
        const modifyListAlert = document.getElementById("modify-list-alert");
        const modifyResultsBody = document.getElementById("modify-results-body");
        const modifyResultsTable = document.getElementById("modify-results-table");

        const editOverlay = document.getElementById("edit-photo-overlay");
        const editForm = document.getElementById("edit-photo-form");
        const editAlert = document.getElementById("edit-photo-alert");
        const editCancelBtn = document.getElementById("edit-photo-cancel");
        const editCloseBtn = document.getElementById("edit-photo-close");
        const editSubmitBtn = document.getElementById("edit-photo-submit");
        let currentEditVersion = null;

        // One date, everything from that day (from and to are both set to it
        // so the result is that single day whichever way the range is read).
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
                const params = new URLSearchParams({ startDate: dateVal, endDate: dateVal, size: "100" });
                const result = await apiFetch(`${PHOTO_ENDPOINT}?${params.toString()}`);
                renderModifyResults(result.content || []);
            } catch (err) {
                showAlert(modifyListAlert, "Could not load photos: " + (err.body?.message || err.message), "error");
            } finally {
                modifySearchBtn.disabled = false;
                modifySearchBtn.textContent = "Load photos";
            }
        }

        modifySearchBtn.addEventListener("click", loadModifyList);

        function renderModifyResults(items) {
            modifyResultsBody.innerHTML = "";
            if (items.length === 0) {
                modifyResultsTable.classList.add("hidden");
                showAlert(modifyListAlert, "No photos found for this date.", "error");
                return;
            }
            hideAlert(modifyListAlert);
            modifyResultsTable.classList.remove("hidden");

            items.forEach((item) => {
                const importerName = item.importedBy != null
                    ? escapeHtml(nameMaps.staff[item.importedBy] || "")
                    : "—";

                const row = document.createElement("tr");
                row.innerHTML = `
                    <td>${item.photoDate ?? ""}</td>
                    <td>${escapeHtml(item.title ?? "")}</td>
                    <td>${namesFor(item.cameramen, nameMaps.cameramen)}</td>
                    <td>${escapeHtml(item.filePath ?? "")}</td>
                    <td>${item.numberOfFiles ?? ""}</td>
                    <td>${formatSize(item.fileSizeMb)}</td>
                    <td>${importerName}</td>
                    <td>
                        <div class="row-actions">
                            <button type="button" class="btn btn-secondary btn-sm edit-btn">Edit</button>
                            <button type="button" class="btn btn-danger btn-sm delete-btn">Delete</button>
                        </div>
                    </td>
                `;
                row.querySelector(".edit-btn").addEventListener("click", () => openEditModal(item));
                row.querySelector(".delete-btn").addEventListener("click", () => deletePhoto(item.photoId));
                modifyResultsBody.appendChild(row);
            });
        }

        async function deletePhoto(id) {
            if (!confirm("Are you sure you want to permanently delete this photo item?")) return;
            try {
                await apiFetch(`${PHOTO_ENDPOINT}/${id}`, { method: "DELETE" });
                await loadModifyList();
                showAlert(modifyListAlert, "Photo item deleted successfully.", "success");
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

            document.getElementById("editPhotoId").value = item.photoId;
            document.getElementById("editTitle").value = item.title || "";
            document.getElementById("editPhotoDate").value = item.photoDate || "";
            DatePicker.refresh("editPhotoDate");
            document.getElementById("editFilePath").value = item.filePath || "";
            document.getElementById("editNumberOfFiles").value = item.numberOfFiles || "";

            document.getElementById("editFileSize").value = item.fileSizeMb != null ? item.fileSizeMb : "";
            document.getElementById("editFileSizeUnit").value = "MB";

            // Importer dropdown lists active staff; if this photo was
            // imported by someone who is now inactive, add them so the
            // current value is not lost on save.
            fillFirstOptionSelect("editImportedBy", activeStaff, "staffMemberId", "staffMemberName");
            const importerSelect = document.getElementById("editImportedBy");
            if (item.importedBy != null && !Array.from(importerSelect.options).some((o) => o.value === String(item.importedBy))) {
                const opt = document.createElement("option");
                opt.value = item.importedBy;
                opt.textContent = (nameMaps.staff[item.importedBy] || "Staff #" + item.importedBy) + " (inactive)";
                importerSelect.appendChild(opt);
            }
            importerSelect.value = item.importedBy != null ? String(item.importedBy) : "";

            // Reset restored the toggle to "active only"; sync the list to it,
            // then mark this photo's cameramen (inactive ones still get chips).
            fillEditCameramen();
            editCameramanController.setIds(item.cameramen ? Array.from(item.cameramen) : [], nameMaps.cameramen);
        }

        function closeEditModal() {
            editOverlay.classList.add("hidden");
        }

        editCancelBtn.addEventListener("click", closeEditModal);
        editCloseBtn.addEventListener("click", closeEditModal);
        editOverlay.addEventListener("click", (e) => {
            if (e.target === editOverlay) closeEditModal();
        });

        editForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            hideAlert(editAlert);
            clearFieldErrors(editForm);
            editSubmitBtn.disabled = true;
            editSubmitBtn.textContent = "Saving...";

            const id = document.getElementById("editPhotoId").value;

            const payload = {
                cameramanIds: editCameramanController.ids(),
                title: document.getElementById("editTitle").value.trim(),
                filePath: document.getElementById("editFilePath").value.trim(),
                numberOfFiles: numberOrNull(document.getElementById("editNumberOfFiles").value),
                fileSizeMb: sizeToMb("editFileSize", "editFileSizeUnit"),
                photoDate: document.getElementById("editPhotoDate").value || null,
                importedBy: numberOrNull(document.getElementById("editImportedBy").value),
                version: currentEditVersion,
            };

            try {
                await apiFetch(`${PHOTO_ENDPOINT}/${id}`, { method: "PUT", body: payload });
                closeEditModal();
                await loadModifyList();
                showAlert(modifyListAlert, "Photo item updated successfully.", "success");
            } catch (err) {
                if (err.status === 409) {
                    showAlert(editAlert, "This photo item was changed by someone else. Close this dialog and reload the list to try again.", "error");
                } else {
                    showFormErrors(editForm, err, editAlert);
                }
            } finally {
                editSubmitBtn.disabled = false;
                editSubmitBtn.textContent = "Save changes";
            }
        });
    }

    // ---------- Staff summary (ADMIN + STAFF) ----------
    // Photos have one staff link (imported by), so the summary is a single
    // list: every photo the chosen person imported in the range, with totals.

    if (isAdminOrStaff) {
        const summaryStaffSelect = document.getElementById("summaryStaffId");
        const summaryStartInput = document.getElementById("summaryStartDate");
        const summaryEndInput = document.getElementById("summaryEndDate");
        const summaryLoadBtn = document.getElementById("summaryLoadBtn");
        const summaryAlert = document.getElementById("summary-alert");

        const summaryOverlay = document.getElementById("summary-results-overlay");
        const summaryCloseBtn = document.getElementById("summary-results-close");
        const summaryTitle = document.getElementById("summary-modal-title");
        const summaryLoading = document.getElementById("summary-modal-loading");
        const summaryEmpty = document.getElementById("summary-modal-empty");
        const summaryContent = document.getElementById("summary-modal-content");
        const summaryBody = document.getElementById("summary-body");
        const summaryPageStatus = document.getElementById("summary-page-status");
        const summaryPrev = document.getElementById("summary-prev");
        const summaryNext = document.getElementById("summary-next");

        // Full list for the current staff member/range, fetched once.
        // Totals come from it; the table is a client-side page into it.
        let summaryItems = [];
        let summaryPage = 0;

        function setSummaryFieldError(inputId, message) {
            const field = document.getElementById(inputId).closest(".field");
            if (!field) return;
            field.classList.toggle("has-error", Boolean(message));
            const errEl = field.querySelector(".field-error");
            if (errEl) errEl.textContent = message || "";
        }

        // Staff member and both dates are required: a summary is always
        // scoped to a date range, never "all time".
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
        summaryStartInput.addEventListener("input", () => setSummaryFieldError("summaryStartDate", null));
        summaryEndInput.addEventListener("input", () => setSummaryFieldError("summaryEndDate", null));

        function openSummaryModal(staffName) {
            summaryTitle.textContent = staffName ? "Staff summary — " + staffName : "Staff summary";
            summaryOverlay.classList.remove("hidden");
            summaryLoading.classList.remove("hidden");
            summaryEmpty.classList.add("hidden");
            summaryContent.classList.add("hidden");
        }

        function closeSummaryModal() {
            summaryOverlay.classList.add("hidden");
        }

        summaryCloseBtn.addEventListener("click", closeSummaryModal);
        summaryOverlay.addEventListener("click", (e) => {
            if (e.target === summaryOverlay) closeSummaryModal();
        });

        // Pulls every page of photos for this importer and range, so totals
        // and paging cover ALL of their photos, not just the first page.
        async function fetchAllImported(staffId, startDate, endDate) {
            const all = [];
            let page = 0;
            const size = 100;

            while (true) {
                const params = new URLSearchParams();
                params.append("importerId", staffId);
                params.append("startDate", startDate);
                params.append("endDate", endDate);
                params.append("page", page);
                params.append("size", size);

                const result = await apiFetch(`${PHOTO_ENDPOINT}?${params.toString()}`);
                const content = result.content || [];
                all.push(...content);

                if (result.last || content.length === 0) break;
                page += 1;
            }
            return all;
        }

        function summaryTotalPages() {
            return Math.max(1, Math.ceil(summaryItems.length / PAGE_SIZE));
        }

        function renderSummaryPage() {
            const total = summaryTotalPages();
            if (summaryPage >= total) summaryPage = total - 1;
            if (summaryPage < 0) summaryPage = 0;

            const start = summaryPage * PAGE_SIZE;
            summaryBody.innerHTML = "";
            summaryItems.slice(start, start + PAGE_SIZE).forEach((item) => {
                const row = document.createElement("tr");
                row.innerHTML = `
                    <td>${item.photoDate ?? ""}</td>
                    <td>${escapeHtml(item.title ?? "")}</td>
                    <td>${namesFor(item.cameramen, nameMaps.cameramen)}</td>
                    <td>${escapeHtml(item.filePath ?? "")}</td>
                    <td>${item.numberOfFiles ?? ""}</td>
                    <td>${formatSize(item.fileSizeMb)}</td>
                `;
                summaryBody.appendChild(row);
            });

            summaryPageStatus.textContent = `Page ${summaryPage + 1} of ${total}`;
            summaryPrev.disabled = summaryPage === 0;
            summaryNext.disabled = summaryPage >= total - 1;
        }

        summaryPrev.addEventListener("click", () => {
            summaryPage -= 1;
            renderSummaryPage();
        });
        summaryNext.addEventListener("click", () => {
            summaryPage += 1;
            renderSummaryPage();
        });

        function renderSummary(items) {
            summaryItems = items;
            summaryPage = 0;
            summaryLoading.classList.add("hidden");

            if (items.length === 0) {
                summaryContent.classList.add("hidden");
                summaryEmpty.classList.remove("hidden");
                return;
            }

            const totals = items.reduce(
                (acc, item) => {
                    acc.files += item.numberOfFiles || 0;
                    acc.sizeMb += item.fileSizeMb || 0;
                    return acc;
                },
                { files: 0, sizeMb: 0 }
            );

            document.getElementById("summaryCount").textContent = items.length;
            document.getElementById("summaryFiles").textContent = totals.files;
            document.getElementById("summarySizeMb").textContent = totals.sizeMb.toFixed(2);
            // Sizes are stored in MB; show a GB equivalent once it is big enough to help.
            document.getElementById("summarySizeGb").textContent =
                totals.sizeMb >= MB_PER_GB ? `(≈ ${(totals.sizeMb / MB_PER_GB).toFixed(2)} GB)` : "";

            summaryEmpty.classList.add("hidden");
            summaryContent.classList.remove("hidden");
            renderSummaryPage();
        }

        summaryLoadBtn.addEventListener("click", async () => {
            const staffId = summaryStaffSelect.value;
            const startDate = summaryStartInput.value;
            const endDate = summaryEndInput.value;

            if (!validateSummaryInputs(staffId, startDate, endDate)) return;

            hideAlert(summaryAlert);
            summaryLoadBtn.disabled = true;
            summaryLoadBtn.textContent = "Loading...";
            openSummaryModal(summaryStaffSelect.selectedOptions[0]?.textContent);

            try {
                renderSummary(await fetchAllImported(staffId, startDate, endDate));
            } catch (err) {
                closeSummaryModal();
                showAlert(summaryAlert, "Could not load summary: " + (err.body?.message || err.message), "error");
            } finally {
                summaryLoadBtn.disabled = false;
                summaryLoadBtn.textContent = "Load summary";
            }
        });
    }

    // ---------- Search (everyone) ----------

    const searchForm = document.getElementById("search-photo-form");
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
        fillSearchLists();
    });

    resultsCloseBtn.addEventListener("click", closeResultsModal);
    resultsOverlay.addEventListener("click", (e) => {
        if (e.target === resultsOverlay) closeResultsModal();
    });

    // Escape closes whichever modal is open: edit, then summary, then results.
    document.addEventListener("keydown", (e) => {
        if (e.key !== "Escape") return;
        const open = ["edit-photo-overlay", "summary-results-overlay", "search-results-overlay"]
            .map((id) => document.getElementById(id))
            .find((el) => el && !el.classList.contains("hidden"));
        if (open) open.classList.add("hidden");
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
            removeBtn.textContent = "×";
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

        const cameramanSelect = document.getElementById("searchCameramanId");
        if (cameramanSelect && cameramanSelect.value) params.append("cameramanId", cameramanSelect.value);

        const importerSelect = document.getElementById("searchImporterId");   // null for Viewers
        if (importerSelect && importerSelect.value) params.append("importerId", importerSelect.value);

        params.append("page", currentPage);
        params.append("size", PAGE_SIZE);

        try {
            const result = await apiFetch(PHOTO_ENDPOINT + "?" + params.toString());
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
            resultsEmpty.textContent = "No photos match your search.";
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
                <td>${item.photoDate ?? ""}</td>
                <td>${escapeHtml(item.title ?? "")}</td>
                <td>${namesFor(item.cameramen, nameMaps.cameramen)}</td>
                <td>${escapeHtml(item.filePath ?? "")}</td>
                <td>${item.numberOfFiles ?? ""}</td>
                <td>${formatSize(item.fileSizeMb)}</td>
            `;
            resultsBody.appendChild(row);
        });

        paginationEl.classList.toggle("hidden", totalPages <= 1);
        pageStatus.textContent = `Page ${page.number + 1} of ${totalPages}`;
        prevBtn.disabled = page.first;
        nextBtn.disabled = page.last;
    }
})();