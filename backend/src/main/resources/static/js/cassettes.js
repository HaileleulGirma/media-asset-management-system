(function () {
    const MEDIA_ENDPOINT = "/api/digitized-media";
    const CUT_ENDPOINT = "/api/cassette-cuts";
    const CATEGORY_ENDPOINT = "/api/cassette-category";
    const STAFF_ENDPOINT = "/api/staffmember";
    const PAGE_SIZE = 20;
    const LOOKUP_DELAY_MS = 400;

    const canEdit = Auth.hasRole("ADMIN") || Auth.hasRole("STAFF");

    const $ = (id) => document.getElementById(id);

    function removeById(id) {
        const el = $(id);
        if (el) el.remove();
    }

    // Viewers can only search. The tab bar, add/manage panels and the edit
    // modal are removed from the DOM (not just hidden). Display convenience
    // only -- the server enforces access, and hides private cassettes and
    // their cuts from viewers.
    if (!canEdit) {
        [
            "tab-bar", "create-cassette-panel", "manage-cassette-panel", "staff-summary-panel",
            "edit-cut-overlay", "summary-results-overlay", "results-actions-th",
        ].forEach(removeById);
    }

    // Global GC/EC toggle + Ethiopian picker. Ids removed above for Viewers
    // are skipped by attach().
    DatePicker.mountToggle();
    DatePicker.attachAll([
        "searchStartDate", "searchEndDate", "cutDate", "editCutDate",
        "cassetteDigitizedDate", "editCassetteDigitizedDate", "summaryStartDate", "summaryEndDate",
    ]);

    // ---------- Helpers ----------

    // The cut response has capitalised names (CutId, CutTitle, ...). Reading
    // both spellings keeps the page working whichever way Jackson sends them.
    function first(obj, names) {
        for (const n of names) {
            if (obj[n] !== undefined && obj[n] !== null) return obj[n];
        }
        return null;
    }

    function normCassette(raw) {
        return {
            id: first(raw, ["digitizedMediaId", "DigitizedMediaId"]),
            filePath: raw.filePath ?? "",
            categoryId: first(raw, ["identifierCategoryId"]),
            number: first(raw, ["identifierNumber"]),
            digitizedDate: raw.digitizedDate ?? null,
            importerId: first(raw, ["importerId", "importedBy"]),
            isPrivate: first(raw, ["isPrivate", "private"]) === true,
            version: raw.version ?? null,
        };
    }

    // categoryId is the CUT's category. shelfCategoryId / shelfNumber /
    // filePath / isPrivate describe the cassette the cut sits in; they are
    // only present once the backend adds them to the cut response.
    function normCut(raw) {
        return {
            id: first(raw, ["CutId", "cutId"]),
            title: first(raw, ["CutTitle", "cutTitle"]) ?? "",
            date: raw.cutDate ?? null,
            mediaId: first(raw, ["digitizedMediaId", "DigitizedMediaId"]),
            categoryId: first(raw, ["CassetteCategoryId", "cassetteCategoryId"]),
            version: raw.version ?? null,
            shelfCategoryId: first(raw, ["identifierCategoryId"]),
            shelfNumber: first(raw, ["identifierNumber"]),
            filePath: raw.filePath ?? "",
            isPrivate: first(raw, ["isPrivate", "private"]) === true,
        };
    }

    function numberOrNull(value) {
        return value === "" || value === null || value === undefined ? null : Number(value);
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
        const input = $(inputId);
        const field = input && input.closest(".field");
        if (!field) return;
        field.classList.toggle("has-error", Boolean(message));
        const errEl = field.querySelector(".field-error");
        if (errEl) errEl.textContent = message || "";
    }

    // Keeps the first (placeholder) option, replaces the rest, and keeps the
    // current choice if it is still in the list.
    function fillFirstOptionSelect(selectId, items, idField, labelField) {
        const select = $(selectId);
        if (!select) return;
        const previous = select.value;
        while (select.options.length > 1) select.remove(1);
        items.forEach((item) => {
            const opt = document.createElement("option");
            opt.value = item[idField];
            opt.textContent = item[labelField];
            select.appendChild(opt);
        });
        if (Array.from(select.options).some((o) => o.value === previous)) select.value = previous;
    }

    // ---------- Reference data (categories + staff) ----------

    let categories = [];
    const categoryNames = {};
    let activeStaff = [];
    let inactiveStaff = [];
    const staffNames = {};

    const CATEGORY_SELECTS = [
        "cassetteSeries", "manageSeries", "editCassetteSeries",
        "cutCategory", "editCutCategory", "searchCutCategory",
    ];

    function categoryName(id) {
        return categoryNames[id] || (id != null ? "Category " + id : "—");
    }

    function cassetteLabel(categoryId, number) {
        return `${categoryName(categoryId)} #${number}`;
    }

    function staffName(id) {
        return id != null ? staffNames[id] || "" : "";
    }

    async function loadReferenceData() {
        const calls = [apiFetch(CATEGORY_ENDPOINT)];
        if (canEdit) {
            calls.push(apiFetch(STAFF_ENDPOINT + "?activeOnly=true"));
            calls.push(apiFetch(STAFF_ENDPOINT + "?activeOnly=false"));
        }
        const [cats, staffActive, staffInactive] = await Promise.allSettled(calls);

        if (cats.status === "fulfilled" && Array.isArray(cats.value)) {
            categories = cats.value
                .slice()
                .sort((a, b) => (a.categoryName || "").localeCompare(b.categoryName || ""));
            categories.forEach((c) => (categoryNames[c.id] = c.categoryName));
        } else {
            const err = cats.reason;
            showAlert($("search-alert"), "Could not load categories: " + (err?.body?.message || err?.message || "unknown error"), "error");
        }

        if (canEdit) {
            const active = staffActive.status === "fulfilled" && Array.isArray(staffActive.value) ? staffActive.value : [];
            const inactive = staffInactive.status === "fulfilled" && Array.isArray(staffInactive.value) ? staffInactive.value : [];
            activeStaff = active;
            inactiveStaff = inactive;
            [...active, ...inactive].forEach((s) => (staffNames[s.staffMemberId] = s.staffMemberName));
            fillFirstOptionSelect("cassetteImporter", activeStaff, "staffMemberId", "staffMemberName");
            const summaryActive = $("summaryActiveOnly");
            fillFirstOptionSelect("summaryStaffId", summaryActive && !summaryActive.checked ? inactiveStaff : activeStaff, "staffMemberId", "staffMemberName");
        }

        CATEGORY_SELECTS.forEach((id) => fillFirstOptionSelect(id, categories, "id", "categoryName"));
    }

    // ---------- Tabs ----------

    const tabs = {
        search: { btn: $("tab-search-btn"), panel: $("search-cassette-panel") },
        add: { btn: $("tab-add-btn"), panel: $("create-cassette-panel") },
        manage: { btn: $("tab-manage-btn"), panel: $("manage-cassette-panel") },
        summary: { btn: $("tab-summary-btn"), panel: $("staff-summary-panel") },
    };

    function showTab(name) {
        const active = tabs[name] && (name === "search" || canEdit) ? name : "search";
        Object.entries(tabs).forEach(([key, t]) => {
            if (t.panel) t.panel.classList.toggle("hidden", key !== active);
            if (t.btn) t.btn.classList.toggle("active", key === active);
        });
    }

    Object.entries(tabs).forEach(([key, t]) => {
        if (t.btn) t.btn.addEventListener("click", () => showTab(key));
    });

    showTab("search");

    // ---------- Cassette picker: category first, then number ----------
    // The number field stays locked until a category is chosen. Changing
    // either one drops the cassette that was found, so nothing below the
    // picker can keep acting on a stale cassette.

    function makePicker(cfg) {
        const seriesEl = $(cfg.seriesId);
        const numberEl = $(cfg.numberId);
        const statusEl = $(cfg.statusId);
        let timer = null;
        let token = 0;      // invalidates lookups that are still in flight
        let current = null; // the cassette found for the current inputs

        function setStatus(text, kind) {
            statusEl.textContent = text || "";
            statusEl.className = "picker-status" + (kind ? " picker-status--" + kind : "");
        }

        function parseNumber() {
            const raw = numberEl.value.trim();
            if (!/^\d+$/.test(raw)) return null;
            const n = Number(raw);
            return n > 0 ? n : null;
        }

        function syncLock() {
            numberEl.disabled = !seriesEl.value;
        }

        function clearFound() {
            clearTimeout(timer);
            token += 1;
            current = null;
            cfg.onCleared();
        }

        async function lookup() {
            const categoryId = seriesEl.value;
            const number = parseNumber();
            if (!categoryId || number === null) return;

            token += 1;
            const mine = token;
            setStatus("Looking up...");

            // Only the request sits inside the try, so a problem while
            // showing the cassette is never mistaken for a failed lookup.
            let page;
            try {
                const params = new URLSearchParams({ categoryId, identifierNumber: number, size: 1 });
                page = await apiFetch(`${MEDIA_ENDPOINT}?${params.toString()}`);
            } catch (err) {
                if (mine !== token) return;
                const code = err && err.status ? ` (HTTP ${err.status})` : "";
                setStatus("The lookup failed" + code + ": " + (err.body?.message || err.message || "unknown error"), "error");
                return;
            }
            if (mine !== token) return;

            // Accept a paged response ({content: [...]}) or a plain array.
            const list = Array.isArray(page) ? page : page && page.content;
            const raw = list && list[0];
            if (!raw) {
                current = null;
                const catLabel = seriesEl.selectedOptions[0] ? seriesEl.selectedOptions[0].textContent : "this category";
                setStatus(`No cassette numbered ${number} in ${catLabel}.`, "error");
                return;
            }
            current = normCassette(raw);
            setStatus("Cassette found.", "ok");
            cfg.onFound(current);
        }

        seriesEl.addEventListener("change", () => {
            numberEl.value = "";
            syncLock();
            clearFound();
            setStatus(seriesEl.value ? "Enter the cassette number." : cfg.idleText);
        });

        numberEl.addEventListener("input", () => {
            clearFound();
            if (numberEl.value.trim() === "") {
                setStatus("Enter the cassette number.");
                return;
            }
            if (parseNumber() === null) {
                setStatus("Enter a whole number above 0.", "error");
                return;
            }
            setStatus("Looking up...");
            timer = setTimeout(lookup, LOOKUP_DELAY_MS);
        });

        numberEl.addEventListener("keydown", (e) => {
            if (e.key !== "Enter") return;
            e.preventDefault();
            clearTimeout(timer);
            if (parseNumber() !== null) lookup();
        });

        syncLock();
        setStatus(cfg.idleText);

        return {
            get cassette() { return current; },
            reset() {
                clearTimeout(timer);
                seriesEl.value = "";
                numberEl.value = "";
                syncLock();
                setStatus(cfg.idleText);
                clearFound();
            },
            // Show a cassette we already have (for example one just created).
            select(cassette) {
                clearTimeout(timer);
                token += 1;
                seriesEl.value = String(cassette.categoryId);
                numberEl.value = String(cassette.number);
                syncLock();
                current = cassette;
                setStatus("Cassette found.", "ok");
                cfg.onFound(cassette);
            },
            // Fill both fields and look the cassette up.
            lookupFor(categoryId, number) {
                seriesEl.value = String(categoryId);
                numberEl.value = String(number);
                syncLock();
                clearFound();
                lookup();
            },
        };
    }

    // ---------- Add cassette (ADMIN + STAFF) ----------
    // Registering a new tape never looks anything up: the number is just a
    // value to save. It stays locked until a category is chosen, and the
    // server rejects a duplicate (category, number) pair on save.

    let managePicker = null;

    if (canEdit) {
        const form = $("create-cassette-form");
        const alertEl = $("create-cassette-alert");
        const submitBtn = $("create-cassette-submit");
        const seriesEl = $("cassetteSeries");
        const numberEl = $("cassetteNumber");

        function syncCreateLock() {
            numberEl.disabled = !seriesEl.value;
        }

        seriesEl.addEventListener("change", () => {
            numberEl.value = "";
            syncCreateLock();
        });

        function resetCreateForm() {
            form.reset();
            DatePicker.refresh("cassetteDigitizedDate");
            syncCreateLock();
            clearFieldErrors(form);
        }

        syncCreateLock();

        $("create-cassette-reset").addEventListener("click", () => {
            hideAlert(alertEl);
            resetCreateForm();
        });

        form.addEventListener("submit", async (e) => {
            e.preventDefault();
            hideAlert(alertEl);
            clearFieldErrors(form);

            if (!$("cassetteDigitizedDate").value) {
                setFieldError("cassetteDigitizedDate", "Digitized date is required.");
                return;
            }

            submitBtn.disabled = true;
            submitBtn.textContent = "Saving...";

            const payload = {
                identifierCategoryId: numberOrNull($("cassetteSeries").value),
                identifierNumber: numberOrNull($("cassetteNumber").value),
                digitizedDate: $("cassetteDigitizedDate").value,
                filePath: $("cassetteFilePath").value.trim(),
                importedBy: numberOrNull($("cassetteImporter").value),
                isPrivate: $("cassettePrivate").checked,
            };

            try {
                const created = normCassette(await apiFetch(MEDIA_ENDPOINT, { method: "POST", body: payload }));
                resetCreateForm();
                // Straight into the new cassette so its cuts can be added.
                managePicker.select(created);
                showTab("manage");
                showAlert($("manage-alert"), "Cassette created. Add its cuts below.", "success");
            } catch (err) {
                showFormErrors(form, err, alertEl);
                alertEl.scrollIntoView({ behavior: "smooth", block: "center" });
            } finally {
                submitBtn.disabled = false;
                submitBtn.textContent = "Add cassette";
            }
        });
    }

    // ---------- Manage cassette and its cuts (ADMIN + STAFF) ----------

    if (canEdit) {
        const manageAlert = $("manage-alert");
        const detail = $("cassette-detail");

        const editCassetteForm = $("edit-cassette-form");
        const editCassetteAlert = $("edit-cassette-alert");
        const editCassetteSubmit = $("edit-cassette-submit");

        const cutsAlert = $("cuts-alert");
        const cutsEmpty = $("cuts-empty");
        const cutsTable = $("cuts-table");
        const cutsBody = $("cuts-body");

        const addCutForm = $("add-cut-form");
        const addCutAlert = $("add-cut-alert");
        const addCutSubmit = $("add-cut-submit");

        const editCutOverlay = $("edit-cut-overlay");
        const editCutForm = $("edit-cut-form");
        const editCutAlert = $("edit-cut-alert");
        const editCutSubmit = $("edit-cut-submit");
        let currentEditCutVersion = null;

        let currentCassette = null;
        let currentCuts = [];

        managePicker = makePicker({
            seriesId: "manageSeries",
            numberId: "manageNumber",
            statusId: "managePickerStatus",
            idleText: "Choose a category first.",
            onFound: showCassette,
            onCleared: hideCassette,
        });

        function hideCassette() {
            currentCassette = null;
            currentCuts = [];
            detail.classList.add("hidden");
        }

        function showCassette(cassette) {
            currentCassette = cassette;
            currentCuts = [];
            detail.classList.remove("hidden");
            [manageAlert, editCassetteAlert, cutsAlert, addCutAlert].forEach(hideAlert);
            clearFieldErrors(editCassetteForm);

            $("cassetteTitle").textContent = cassetteLabel(cassette.categoryId, cassette.number);
            $("cassettePrivateBadge").classList.toggle("hidden", !cassette.isPrivate);
            const metaParts = [];
            if (cassette.importerId != null && staffName(cassette.importerId)) metaParts.push("Imported by " + staffName(cassette.importerId));
            if (cassette.digitizedDate) metaParts.push("Digitized " + cassette.digitizedDate);
            $("cassetteMeta").textContent = metaParts.join(" · ");

            $("editCassetteSeries").value = String(cassette.categoryId ?? "");
            $("editCassetteNumber").value = cassette.number ?? "";
            $("editCassetteFilePath").value = cassette.filePath || "";
            $("editCassetteDigitizedDate").value = cassette.digitizedDate || "";
            DatePicker.refresh("editCassetteDigitizedDate");
            $("editCassettePrivate").checked = cassette.isPrivate;

            // The dropdown lists active staff. If the current importer is now
            // inactive, add them so saving does not wipe the field.
            fillFirstOptionSelect("editCassetteImporter", activeStaff, "staffMemberId", "staffMemberName");
            const importerSelect = $("editCassetteImporter");
            if (cassette.importerId != null && !Array.from(importerSelect.options).some((o) => o.value === String(cassette.importerId))) {
                const opt = document.createElement("option");
                opt.value = cassette.importerId;
                opt.textContent = (staffName(cassette.importerId) || "Staff #" + cassette.importerId) + " (inactive)";
                importerSelect.appendChild(opt);
            }
            importerSelect.value = cassette.importerId != null ? String(cassette.importerId) : "";

            resetAddCutForm();
            renderCuts();
            loadCuts();
        }

        // ----- cuts -----

        async function fetchAllCuts(mediaId) {
            const all = [];
            let page = 0;
            while (true) {
                const params = new URLSearchParams({ digitizedMediaId: mediaId, page, size: 100 });
                const result = await apiFetch(`${CUT_ENDPOINT}?${params.toString()}`);
                const content = result.content || [];
                all.push(...content.map(normCut));
                if (result.last || content.length === 0) break;
                page += 1;
            }
            return all;
        }

        async function loadCuts() {
            if (!currentCassette) return;
            const forId = currentCassette.id;
            try {
                const cuts = await fetchAllCuts(forId);
                if (!currentCassette || currentCassette.id !== forId) return; // user moved on
                currentCuts = cuts;
                renderCuts();
            } catch (err) {
                if (!currentCassette || currentCassette.id !== forId) return;
                showAlert(cutsAlert, "Could not load cuts: " + (err.body?.message || err.message), "error");
            }
        }

        function renderCuts() {
            const n = currentCuts.length;
            $("cutCount").textContent = n === 1 ? "(1 cut)" : `(${n} cuts)`;
            $("privacyHint").textContent = n === 0
                ? "Private cassettes are hidden from viewers."
                : `Cuts follow the cassette: changing this also changes who can see its ${n === 1 ? "1 cut" : n + " cuts"}.`;

            cutsBody.innerHTML = "";
            if (n === 0) {
                cutsTable.classList.add("hidden");
                cutsEmpty.classList.remove("hidden");
                return;
            }
            cutsEmpty.classList.add("hidden");
            cutsTable.classList.remove("hidden");

            currentCuts.forEach((cut) => {
                const row = document.createElement("tr");
                row.innerHTML = `
                    <td>${escapeHtml(cut.title)}</td>
                    <td>${cut.date ?? "—"}</td>
                    <td>${escapeHtml(categoryName(cut.categoryId))}</td>
                    <td>
                        <div class="row-actions">
                            <button type="button" class="btn btn-secondary btn-sm edit-btn">Edit</button>
                            <button type="button" class="btn btn-danger btn-sm delete-btn">Delete</button>
                        </div>
                    </td>
                `;
                row.querySelector(".edit-btn").addEventListener("click", () => openEditCut(cut));
                row.querySelector(".delete-btn").addEventListener("click", () => deleteCut(cut));
                cutsBody.appendChild(row);
            });
        }

        // A new cut starts in the cassette's own category (the common case);
        // the user changes it when the cut covers something else.
        function resetAddCutForm() {
            addCutForm.reset();
            DatePicker.refresh("cutDate");
            clearFieldErrors(addCutForm);
            $("cutCategory").value = currentCassette && currentCassette.categoryId != null
                ? String(currentCassette.categoryId)
                : "";
        }

        addCutForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            hideAlert(addCutAlert);
            clearFieldErrors(addCutForm);
            if (!currentCassette) return;

            const categoryId = $("cutCategory").value;
            if (!categoryId) {
                setFieldError("cutCategory", "Choose a cut category.");
                return;
            }

            addCutSubmit.disabled = true;
            addCutSubmit.textContent = "Saving...";

            const payload = {
                cutTitle: $("cutTitle").value.trim(),
                cutDate: $("cutDate").value || null,
                digitizedMediaId: currentCassette.id,
                cassetteCategoryId: Number(categoryId),
            };

            try {
                await apiFetch(CUT_ENDPOINT, { method: "POST", body: payload });
                resetAddCutForm();
                await loadCuts();
                showAlert(addCutAlert, "Cut added.", "success");
            } catch (err) {
                showFormErrors(addCutForm, err, addCutAlert);
            } finally {
                addCutSubmit.disabled = false;
                addCutSubmit.textContent = "Add cut";
            }
        });

        async function deleteCut(cut) {
            if (!confirm(`Delete the cut "${cut.title}"?`)) return;
            try {
                await apiFetch(`${CUT_ENDPOINT}/${cut.id}`, { method: "DELETE" });
                await loadCuts();
                showAlert(cutsAlert, "Cut deleted.", "success");
            } catch (err) {
                showAlert(cutsAlert, "Failed to delete: " + (err.body?.message || err.message), "error");
            }
        }

        function openEditCut(cut) {
            editCutForm.reset();
            clearFieldErrors(editCutForm);
            hideAlert(editCutAlert);
            currentEditCutVersion = cut.version;

            $("editCutId").value = cut.id;
            $("editCutTitle").value = cut.title;
            $("editCutDate").value = cut.date || "";
            DatePicker.refresh("editCutDate");
            $("editCutCategory").value = cut.categoryId != null ? String(cut.categoryId) : "";
            editCutOverlay.classList.remove("hidden");
        }

        function closeEditCut() {
            editCutOverlay.classList.add("hidden");
        }

        $("edit-cut-cancel").addEventListener("click", closeEditCut);
        $("edit-cut-close").addEventListener("click", closeEditCut);
        editCutOverlay.addEventListener("click", (e) => {
            if (e.target === editCutOverlay) closeEditCut();
        });

        editCutForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            hideAlert(editCutAlert);
            clearFieldErrors(editCutForm);
            if (!currentCassette) return;

            const categoryId = $("editCutCategory").value;
            if (!categoryId) {
                setFieldError("editCutCategory", "Choose a cut category.");
                return;
            }

            editCutSubmit.disabled = true;
            editCutSubmit.textContent = "Saving...";

            const id = $("editCutId").value;
            const payload = {
                cutTitle: $("editCutTitle").value.trim(),
                cutDate: $("editCutDate").value || null,
                digitizedMediaId: currentCassette.id,
                cassetteCategoryId: Number(categoryId),
                version: currentEditCutVersion,
            };

            try {
                await apiFetch(`${CUT_ENDPOINT}/${id}`, { method: "PUT", body: payload });
                closeEditCut();
                await loadCuts();
                showAlert(cutsAlert, "Cut updated.", "success");
            } catch (err) {
                if (err.status === 409) {
                    showAlert(editCutAlert, "This cut was changed by someone else. Close this dialog and try again.", "error");
                    loadCuts();
                } else {
                    showFormErrors(editCutForm, err, editCutAlert);
                }
            } finally {
                editCutSubmit.disabled = false;
                editCutSubmit.textContent = "Save changes";
            }
        });

        // ----- the cassette itself -----

        editCassetteForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            hideAlert(editCassetteAlert);
            clearFieldErrors(editCassetteForm);
            if (!currentCassette) return;

            if (!$("editCassetteDigitizedDate").value) {
                setFieldError("editCassetteDigitizedDate", "Digitized date is required.");
                return;
            }

            editCassetteSubmit.disabled = true;
            editCassetteSubmit.textContent = "Saving...";

            const payload = {
                identifierCategoryId: numberOrNull($("editCassetteSeries").value),
                identifierNumber: numberOrNull($("editCassetteNumber").value),
                digitizedDate: $("editCassetteDigitizedDate").value,
                filePath: $("editCassetteFilePath").value.trim(),
                importedBy: numberOrNull($("editCassetteImporter").value),
                isPrivate: $("editCassettePrivate").checked,
                version: currentCassette.version,
            };

            try {
                const saved = normCassette(
                    await apiFetch(`${MEDIA_ENDPOINT}/${currentCassette.id}`, { method: "PUT", body: payload })
                );
                // Reload through the picker so its fields, the title and the
                // version all reflect what was saved (the category or number
                // may have changed).
                managePicker.select(saved);
                showAlert(editCassetteAlert, "Cassette saved.", "success");
            } catch (err) {
                if (err.status === 409) {
                    showAlert(editCassetteAlert, "This cassette was changed by someone else. Look it up again to reload it.", "error");
                } else {
                    showFormErrors(editCassetteForm, err, editCassetteAlert);
                }
            } finally {
                editCassetteSubmit.disabled = false;
                editCassetteSubmit.textContent = "Save cassette";
            }
        });

        $("delete-cassette-btn").addEventListener("click", async () => {
            if (!currentCassette) return;
            const n = currentCuts.length;
            const warning = n > 0
                ? `Delete ${cassetteLabel(currentCassette.categoryId, currentCassette.number)} and its ${n === 1 ? "1 cut" : n + " cuts"}? This cannot be undone.`
                : `Delete ${cassetteLabel(currentCassette.categoryId, currentCassette.number)}? This cannot be undone.`;
            if (!confirm(warning)) return;

            const label = cassetteLabel(currentCassette.categoryId, currentCassette.number);
            try {
                await apiFetch(`${MEDIA_ENDPOINT}/${currentCassette.id}`, { method: "DELETE" });
                managePicker.reset();
                showAlert(manageAlert, `${label} deleted.`, "success");
            } catch (err) {
                showAlert(editCassetteAlert, "Failed to delete: " + (err.body?.message || err.message), "error");
            }
        });
    }

    // ---------- Staff summary (ADMIN + STAFF) ----------
    // How many cassettes a staff member imported, counted by the day each
    // cassette was digitized, within a required date range.

    if (canEdit) {
        const staffSelect = $("summaryStaffId");
        const startInput = $("summaryStartDate");
        const endInput = $("summaryEndDate");
        const activeOnlyBox = $("summaryActiveOnly");
        const loadBtn = $("summaryLoadBtn");
        const summaryAlert = $("summary-alert");

        const overlay = $("summary-results-overlay");
        const title = $("summary-modal-title");
        const loadingEl = $("summary-modal-loading");
        const emptyEl = $("summary-modal-empty");
        const contentEl = $("summary-modal-content");
        const categoriesBody = $("summary-categories-body");
        const listBody = $("summary-body");
        const pageStatusEl = $("summary-page-status");
        const prevBtnEl = $("summary-prev");
        const nextBtnEl = $("summary-next");

        // Every cassette the person imported, fetched once. The totals come
        // from the whole list; the table is a client-side page into it.
        let items = [];
        let listPage = 0;

        activeOnlyBox.addEventListener("change", () => {
            fillFirstOptionSelect("summaryStaffId", activeOnlyBox.checked ? activeStaff : inactiveStaff, "staffMemberId", "staffMemberName");
        });
        staffSelect.addEventListener("change", () => setFieldError("summaryStaffId", null));
        startInput.addEventListener("input", () => setFieldError("summaryStartDate", null));
        endInput.addEventListener("input", () => setFieldError("summaryEndDate", null));

        // Staff member and both dates are required: a summary is always
        // scoped to a range, never "all time".
        function validateSummaryInputs(staffId, startDate, endDate) {
            let valid = true;
            setFieldError("summaryStaffId", null);
            setFieldError("summaryStartDate", null);
            setFieldError("summaryEndDate", null);

            if (!staffId) {
                setFieldError("summaryStaffId", "Please select a staff member.");
                valid = false;
            }
            if (!startDate) {
                setFieldError("summaryStartDate", "From date is required.");
                valid = false;
            }
            if (!endDate) {
                setFieldError("summaryEndDate", "To date is required.");
                valid = false;
            }
            if (startDate && endDate && startDate > endDate) {
                setFieldError("summaryStartDate", '"From date" cannot be later than "To date".');
                setFieldError("summaryEndDate", '"From date" cannot be later than "To date".');
                valid = false;
            }
            return valid;
        }

        function openSummaryModal(staffLabel, startDate, endDate) {
            const range = startDate && endDate ? ` (${startDate} to ${endDate})` : "";
            title.textContent = staffLabel ? "Staff summary — " + staffLabel + range : "Staff summary";
            overlay.classList.remove("hidden");
            loadingEl.classList.remove("hidden");
            emptyEl.classList.add("hidden");
            contentEl.classList.add("hidden");
        }

        function closeSummaryModal() {
            overlay.classList.add("hidden");
        }

        $("summary-results-close").addEventListener("click", closeSummaryModal);
        overlay.addEventListener("click", (e) => {
            if (e.target === overlay) closeSummaryModal();
        });

        // Pulls every page so the totals cover all of the person's cassettes
        // in the range.
        async function fetchAllImported(staffId, startDate, endDate) {
            const all = [];
            let page = 0;
            while (true) {
                const params = new URLSearchParams({ importerId: staffId, startDate, endDate, page, size: 100 });
                const result = await apiFetch(`${MEDIA_ENDPOINT}?${params.toString()}`);
                const content = result.content || [];
                all.push(...content.map(normCassette));
                if (result.last || content.length === 0) break;
                page += 1;
            }
            return all;
        }

        function totalListPages() {
            return Math.max(1, Math.ceil(items.length / PAGE_SIZE));
        }

        function renderListPage() {
            const total = totalListPages();
            listPage = Math.min(Math.max(listPage, 0), total - 1);

            const start = listPage * PAGE_SIZE;
            listBody.innerHTML = "";
            items.slice(start, start + PAGE_SIZE).forEach((cassette) => {
                const row = document.createElement("tr");
                row.innerHTML = `
                    <td>${escapeHtml(cassetteLabel(cassette.categoryId, cassette.number))}</td>
                    <td>${cassette.digitizedDate ?? "—"}</td>
                    <td>${escapeHtml(cassette.filePath)}</td>
                    <td>${cassette.isPrivate ? '<span class="private-badge">Private</span>' : "Public"}</td>
                    <td><button type="button" class="btn btn-secondary btn-sm open-btn">Open</button></td>
                `;
                row.querySelector(".open-btn").addEventListener("click", () => {
                    closeSummaryModal();
                    showTab("manage");
                    managePicker.select(cassette);
                });
                listBody.appendChild(row);
            });

            pageStatusEl.textContent = `Page ${listPage + 1} of ${total}`;
            prevBtnEl.disabled = listPage === 0;
            nextBtnEl.disabled = listPage >= total - 1;
        }

        prevBtnEl.addEventListener("click", () => {
            listPage -= 1;
            renderListPage();
        });
        nextBtnEl.addEventListener("click", () => {
            listPage += 1;
            renderListPage();
        });

        function renderSummary(list) {
            items = list;
            listPage = 0;
            loadingEl.classList.add("hidden");

            if (list.length === 0) {
                contentEl.classList.add("hidden");
                emptyEl.classList.remove("hidden");
                return;
            }

            const perCategory = new Map();
            let privateCount = 0;
            list.forEach((c) => {
                perCategory.set(c.categoryId, (perCategory.get(c.categoryId) || 0) + 1);
                if (c.isPrivate) privateCount += 1;
            });

            $("summaryCount").textContent = list.length;
            $("summaryPrivate").textContent = privateCount;
            $("summaryCategoryCount").textContent = perCategory.size;

            categoriesBody.innerHTML = "";
            Array.from(perCategory.entries())
                .sort((a, b) => b[1] - a[1] || categoryName(a[0]).localeCompare(categoryName(b[0])))
                .forEach(([categoryId, count]) => {
                    const row = document.createElement("tr");
                    row.innerHTML = `<td>${escapeHtml(categoryName(categoryId))}</td><td>${count}</td>`;
                    categoriesBody.appendChild(row);
                });

            emptyEl.classList.add("hidden");
            contentEl.classList.remove("hidden");
            renderListPage();
        }

        loadBtn.addEventListener("click", async () => {
            const staffId = staffSelect.value;
            const startDate = startInput.value;
            const endDate = endInput.value;
            hideAlert(summaryAlert);

            if (!validateSummaryInputs(staffId, startDate, endDate)) return;

            loadBtn.disabled = true;
            loadBtn.textContent = "Loading...";
            openSummaryModal(staffSelect.selectedOptions[0]?.textContent, startDate, endDate);

            try {
                renderSummary(await fetchAllImported(staffId, startDate, endDate));
            } catch (err) {
                closeSummaryModal();
                showAlert(summaryAlert, "Could not load summary: " + (err.body?.message || err.message), "error");
            } finally {
                loadBtn.disabled = false;
                loadBtn.textContent = "Load summary";
            }
        });
    }

    // ---------- Search cuts (everyone) ----------

    const searchForm = $("search-cassette-form");
    const searchAlert = $("search-alert");
    const resultsOverlay = $("search-results-overlay");
    const resultsEmpty = $("search-results-empty");
    const resultsTable = $("search-results-table");
    const resultsBody = $("search-results-body");
    const paginationEl = $("search-pagination");
    const pageStatus = $("page-status");
    const prevBtn = $("page-prev");
    const nextBtn = $("page-next");

    let currentPage = 0;
    let totalPages = 0;

    const searchStartInput = $("searchStartDate");
    const searchEndInput = $("searchEndDate");

    function setDateRangeError(message) {
        ["searchStartDate", "searchEndDate"].forEach((id) => setFieldError(id, message));
    }

    // "To date" only makes sense once "From date" is chosen.
    function syncEndDateAvailability() {
        const hasStart = Boolean(searchStartInput.value);
        searchEndInput.disabled = !hasStart;
        if (!hasStart) searchEndInput.value = "";
    }

    searchStartInput.addEventListener("input", () => {
        setDateRangeError(null);
        syncEndDateAvailability();
    });
    searchEndInput.addEventListener("input", () => setDateRangeError(null));
    syncEndDateAvailability();

    searchForm.addEventListener("submit", (e) => {
        e.preventDefault();
        hideAlert(searchAlert);

        if (searchStartInput.value && searchEndInput.value && searchStartInput.value > searchEndInput.value) {
            setDateRangeError('"From date" cannot be later than "To date".');
            return;
        }
        setDateRangeError(null);

        currentPage = 0;
        openResultsModal();
        runSearch();
    });

    $("search-reset").addEventListener("click", () => {
        searchForm.reset();
        DatePicker.refresh("searchStartDate");
        DatePicker.refresh("searchEndDate");
        setDateRangeError(null);
        syncEndDateAvailability();
        clearTermPiles();
        hideAlert(searchAlert);
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

    $("search-results-close").addEventListener("click", closeResultsModal);
    resultsOverlay.addEventListener("click", (e) => {
        if (e.target === resultsOverlay) closeResultsModal();
    });

    // Escape closes whichever modal is open: edit cut, summary, then results.
    document.addEventListener("keydown", (e) => {
        if (e.key !== "Escape") return;
        const open = ["edit-cut-overlay", "summary-results-overlay", "search-results-overlay"]
            .map((id) => $(id))
            .find((el) => el && !el.classList.contains("hidden"));
        if (open) open.classList.add("hidden");
    });

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

    const searchTermInput = $("searchTermInput");

    $("addTermBtn").addEventListener("click", addCurrentTerm);
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
        const container = $(containerId);
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

        // A single day is sent as a one-day range, so the backend's
        // "between" filter always gets both ends.
        const startDate = searchStartInput.value;
        if (startDate) {
            params.append("startDate", startDate);
            params.append("endDate", searchEndInput.value || startDate);
        }

        const cutCategory = $("searchCutCategory").value;
        if (cutCategory) params.append("categoryId", cutCategory);

        params.append("page", currentPage);
        params.append("size", PAGE_SIZE);

        try {
            const result = await apiFetch(CUT_ENDPOINT + "?" + params.toString());
            renderResults(result);
        } catch (err) {
            resultsEmpty.textContent = "Could not load results: " + (err.body?.message || err.message);
            resultsEmpty.classList.remove("hidden");
            resultsTable.classList.add("hidden");
            paginationEl.classList.add("hidden");
        }
    }

    function cassetteCellHtml(cut) {
        const label = cut.shelfNumber != null
            ? cassetteLabel(cut.shelfCategoryId, cut.shelfNumber)
            : "Cassette #" + cut.mediaId;
        const badge = cut.isPrivate ? ' <span class="private-badge">Private</span>' : "";
        return escapeHtml(label) + badge;
    }

    function renderResults(page) {
        totalPages = page.totalPages;

        if (!page.content || page.content.length === 0) {
            resultsEmpty.textContent = "No cuts match your search.";
            resultsEmpty.classList.remove("hidden");
            resultsTable.classList.add("hidden");
            paginationEl.classList.add("hidden");
            return;
        }

        resultsEmpty.classList.add("hidden");
        resultsTable.classList.remove("hidden");

        resultsBody.innerHTML = "";
        page.content.map(normCut).forEach((cut) => {
            const canOpen = cut.shelfCategoryId != null && cut.shelfNumber != null;
            const row = document.createElement("tr");
            row.innerHTML = `
                <td>${cassetteCellHtml(cut)}</td>
                <td>${escapeHtml(cut.title)}</td>
                <td>${cut.date ?? "—"}</td>
                <td>${escapeHtml(categoryName(cut.categoryId))}</td>
                <td>${escapeHtml(cut.filePath)}</td>
                ${canEdit ? `<td><button type="button" class="btn btn-secondary btn-sm open-btn"${canOpen ? "" : " disabled"}>Open cassette</button></td>` : ""}
            `;
            if (canEdit && canOpen) {
                row.querySelector(".open-btn").addEventListener("click", () => {
                    closeResultsModal();
                    showTab("manage");
                    managePicker.lookupFor(cut.shelfCategoryId, cut.shelfNumber);
                });
            }
            resultsBody.appendChild(row);
        });

        paginationEl.classList.toggle("hidden", totalPages <= 1);
        pageStatus.textContent = `Page ${page.number + 1} of ${totalPages}`;
        prevBtn.disabled = page.first;
        nextBtn.disabled = page.last;
    }

    loadReferenceData();
})();