/**
 * DatePicker: layers an Ethiopian-calendar picker on top of existing
 * `<input type="date">` elements, driven by one global GC/EC preference.
 *
 * Design goals:
 *  - Zero HTML changes: attach(id) finds the existing native input and
 *    wraps it in place.
 *  - The native input stays the single source of truth (always holds a
 *    Gregorian "YYYY-MM-DD" value). Every place in news.js that reads
 *    `document.getElementById(id).value` keeps working unmodified.
 *  - In GC mode the native input is shown (native browser picker).
 *  - In EC mode the native input is hidden but stays in the DOM; a
 *    trigger button + custom Ethiopian calendar popover drive it instead,
 *    converting the selected Ethiopian date to Gregorian before writing
 *    to the native input's value. The popover itself is rendered in
 *    Amharic (month names, weekday labels, trigger text).
 *  - Requires ethiopian-calendar.js to be loaded first.
 */
(function (global) {
    "use strict";

    const PREF_KEY = "mam_calendar_pref";
    const MIN_YEAR = 1;
    const MAX_YEAR = 9999;
    const instances = {};
    let toggleButtons = [];

    function getPreference() {
        try {
            return localStorage.getItem(PREF_KEY) === "EC" ? "EC" : "GC";
        } catch (e) {
            return "GC";
        }
    }

    function setPreference(pref) {
        const value = pref === "EC" ? "EC" : "GC";
        try {
            localStorage.setItem(PREF_KEY, value);
        } catch (e) {
            // ignore (private browsing / storage disabled) - preference just won't persist
        }
        Object.keys(instances).forEach(applyMode);
        updateToggleUI();
        window.dispatchEvent(new CustomEvent("mam:calendarpref", { detail: { pref: value } }));
    }

    function applyMode(inputId) {
        const inst = instances[inputId];
        if (!inst) return;
        const isEC = getPreference() === "EC";
        inst.input.classList.toggle("hidden", isEC);
        inst.trigger.classList.toggle("hidden", !isEC);
        if (!isEC) closePopover(inputId);
        renderTrigger(inputId);
    }

    function renderTrigger(inputId) {
        const inst = instances[inputId];
        if (!inst) return;
        const iso = inst.input.value;
        if (!iso) {
            inst.trigger.textContent = "\u2003\u1240\u1295 \u12ED\u121D\u1228\u1321"; // "  Select date"
            inst.trigger.classList.add("eth-dp-empty");
            return;
        }
        inst.trigger.classList.remove("eth-dp-empty");
        const e = EthiopianCalendar.gregorianToEthiopian(iso);
        inst.trigger.textContent = EthiopianCalendar.formatEthiopianAmharic(e.year, e.month, e.day) + " \u12D3.\u121D";
    }

    function syncDisabled(inputId) {
        const inst = instances[inputId];
        if (!inst) return;
        inst.trigger.disabled = inst.input.disabled;
        if (inst.input.disabled) closePopover(inputId);
    }

    function initView(inputId) {
        const inst = instances[inputId];
        if (inst.viewYear != null) return;
        if (inst.input.value) {
            const e = EthiopianCalendar.gregorianToEthiopian(inst.input.value);
            inst.viewYear = e.year;
            inst.viewMonth = e.month;
        } else {
            const t = EthiopianCalendar.todayEthiopian();
            inst.viewYear = t.year;
            inst.viewMonth = t.month;
        }
    }

    function clampYear(year) {
        return Math.min(MAX_YEAR, Math.max(MIN_YEAR, year));
    }

    function shiftMonth(inputId, delta) {
        const inst = instances[inputId];
        let m = inst.viewMonth + delta;
        let y = inst.viewYear;
        if (m > 13) { m = 1; y += 1; }
        if (m < 1) { m = 13; y -= 1; }
        inst.viewYear = clampYear(y);
        inst.viewMonth = m;
        renderPopover(inputId);
    }

    function shiftYear(inputId, delta) {
        const inst = instances[inputId];
        inst.viewYear = clampYear(inst.viewYear + delta);
        renderPopover(inputId);
    }

    // Commits whatever is currently typed in the year input as the view
    // year (called on Enter or on blur/change, not on every keystroke,
    // so the field doesn't get yanked out from under the user mid-type).
    function commitYearInput(inputId, rawValue) {
        const inst = instances[inputId];
        const parsed = parseInt(rawValue, 10);
        if (!Number.isNaN(parsed)) {
            inst.viewYear = clampYear(parsed);
        }
        renderPopover(inputId);
    }

    function selectDay(inputId, day) {
        const inst = instances[inputId];
        const iso = EthiopianCalendar.ethiopianToGregorianIso(inst.viewYear, inst.viewMonth, day);
        inst.input.value = iso;
        inst.input.dispatchEvent(new Event("input", { bubbles: true }));
        inst.input.dispatchEvent(new Event("change", { bubbles: true }));
        renderTrigger(inputId);
        closePopover(inputId);
    }

    function clearValue(inputId) {
        const inst = instances[inputId];
        inst.input.value = "";
        inst.input.dispatchEvent(new Event("input", { bubbles: true }));
        inst.input.dispatchEvent(new Event("change", { bubbles: true }));
        renderTrigger(inputId);
        closePopover(inputId);
    }

    function selectToday(inputId) {
        const inst = instances[inputId];
        const t = EthiopianCalendar.todayEthiopian();
        inst.viewYear = t.year;
        inst.viewMonth = t.month;
        selectDay(inputId, t.day);
    }

    function renderPopover(inputId) {
        const inst = instances[inputId];
        const { viewYear, viewMonth, popover } = inst;
        const today = EthiopianCalendar.todayEthiopian();
        const selected = inst.input.value ? EthiopianCalendar.gregorianToEthiopian(inst.input.value) : null;
        const firstWeekday = EthiopianCalendar.weekdayOfEthiopian(viewYear, viewMonth, 1);
        const totalDays = EthiopianCalendar.daysInMonth(viewYear, viewMonth);

        popover.innerHTML = "";

        // ---- Month nav row ----
        const header = document.createElement("div");
        header.className = "eth-dp-header";

        const prevBtn = document.createElement("button");
        prevBtn.type = "button";
        prevBtn.className = "eth-dp-nav-btn";
        prevBtn.textContent = "\u2039";
        prevBtn.setAttribute("aria-label", "Previous month");
        prevBtn.addEventListener("click", () => shiftMonth(inputId, -1));

        const title = document.createElement("span");
        title.className = "eth-dp-title";
        title.textContent = EthiopianCalendar.MONTHS_AM[viewMonth - 1];

        const nextBtn = document.createElement("button");
        nextBtn.type = "button";
        nextBtn.className = "eth-dp-nav-btn";
        nextBtn.textContent = "\u203a";
        nextBtn.setAttribute("aria-label", "Next month");
        nextBtn.addEventListener("click", () => shiftMonth(inputId, 1));

        header.appendChild(prevBtn);
        header.appendChild(title);
        header.appendChild(nextBtn);
        popover.appendChild(header);

        // ---- Year nav row: jump a year at a time, or type a year directly ----
        const yearRow = document.createElement("div");
        yearRow.className = "eth-dp-year-row";

        const prevYearBtn = document.createElement("button");
        prevYearBtn.type = "button";
        prevYearBtn.className = "eth-dp-nav-btn eth-dp-year-nav-btn";
        prevYearBtn.textContent = "\u00ab";
        prevYearBtn.setAttribute("aria-label", "Previous year");
        prevYearBtn.addEventListener("click", () => shiftYear(inputId, -1));

        const yearInput = document.createElement("input");
        yearInput.type = "number";
        yearInput.className = "eth-dp-year-input";
        yearInput.value = viewYear;
        yearInput.addEventListener("keydown", (e) => {
            if (e.key === "Enter") {
                e.preventDefault();
                commitYearInput(inputId, yearInput.value);
            }
        });
        yearInput.addEventListener("change", () => commitYearInput(inputId, yearInput.value));

        const nextYearBtn = document.createElement("button");
        nextYearBtn.type = "button";
        nextYearBtn.className = "eth-dp-nav-btn eth-dp-year-nav-btn";
        nextYearBtn.textContent = "\u00bb";
        nextYearBtn.setAttribute("aria-label", "Next year");
        nextYearBtn.addEventListener("click", () => shiftYear(inputId, 1));

        yearRow.appendChild(prevYearBtn);
        yearRow.appendChild(yearInput);
        yearRow.appendChild(nextYearBtn);
        popover.appendChild(yearRow);

        // ---- Day grid ----
        const grid = document.createElement("div");
        grid.className = "eth-dp-grid";

        EthiopianCalendar.WEEKDAYS_SHORT_AM.forEach((wd) => {
            const cell = document.createElement("div");
            cell.className = "eth-dp-dow";
            cell.textContent = wd;
            grid.appendChild(cell);
        });

        for (let i = 0; i < firstWeekday; i++) {
            grid.appendChild(document.createElement("div"));
        }

        for (let day = 1; day <= totalDays; day++) {
            const cell = document.createElement("button");
            cell.type = "button";
            cell.className = "eth-dp-day";
            cell.textContent = String(day);
            if (selected && selected.year === viewYear && selected.month === viewMonth && selected.day === day) {
                cell.classList.add("eth-dp-selected");
            }
            if (today.year === viewYear && today.month === viewMonth && today.day === day) {
                cell.classList.add("eth-dp-today");
            }
            cell.addEventListener("click", () => selectDay(inputId, day));
            grid.appendChild(cell);
        }

        popover.appendChild(grid);

        const footer = document.createElement("div");
        footer.className = "eth-dp-footer";

        const todayBtn = document.createElement("button");
        todayBtn.type = "button";
        todayBtn.className = "btn btn-secondary btn-sm";
        todayBtn.textContent = "\u12DB\u122C"; // "Today"
        todayBtn.addEventListener("click", () => selectToday(inputId));

        const clearBtn = document.createElement("button");
        clearBtn.type = "button";
        clearBtn.className = "btn btn-secondary btn-sm";
        clearBtn.textContent = "\u12A0\u133D\u12F3"; // "Clear"
        clearBtn.addEventListener("click", () => clearValue(inputId));

        footer.appendChild(todayBtn);
        footer.appendChild(clearBtn);
        popover.appendChild(footer);
    }

    function openPopover(inputId) {
        const inst = instances[inputId];
        if (!inst || inst.input.disabled) return;
        initView(inputId);
        renderPopover(inputId);
        Object.keys(instances).forEach((id) => {
            if (id !== inputId) closePopover(id);
        });
        inst.popover.classList.remove("hidden");
    }

    function closePopover(inputId) {
        const inst = instances[inputId];
        if (inst) inst.popover.classList.add("hidden");
    }

    function togglePopover(inputId) {
        const inst = instances[inputId];
        if (!inst) return;
        if (inst.popover.classList.contains("hidden")) openPopover(inputId);
        else closePopover(inputId);
    }

    function attach(inputId) {
        if (instances[inputId]) return instances[inputId];
        const input = document.getElementById(inputId);
        if (!input) return null;

        const wrap = document.createElement("div");
        wrap.className = "eth-dp-wrap";
        input.parentNode.insertBefore(wrap, input);
        wrap.appendChild(input);

        const trigger = document.createElement("button");
        trigger.type = "button";
        trigger.className = "eth-dp-trigger hidden";
        wrap.appendChild(trigger);

        const popover = document.createElement("div");
        popover.className = "eth-dp-popover hidden";
        wrap.appendChild(popover);

        // Fix: renderPopover() rebuilds the popover's contents (innerHTML = "")
        // on nav clicks, which detaches the very button that was just
        // clicked *before* the click event finishes bubbling. The
        // document-level "click outside closes it" listener below checks
        // wrap.contains(e.target), and a detached node fails that check
        // even though the click was squarely inside the picker - so the
        // popover was closing itself on every prev/next/today click.
        // Stopping propagation here, on the popover itself, ends the
        // event before it reaches that listener, regardless of whether
        // the clicked child gets swapped out during the handler.
        popover.addEventListener("click", (e) => e.stopPropagation());

        const inst = { input, wrap, trigger, popover, viewYear: null, viewMonth: null };
        instances[inputId] = inst;

        trigger.addEventListener("click", (e) => {
            e.stopPropagation();
            togglePopover(inputId);
        });
        input.addEventListener("change", () => renderTrigger(inputId));
        input.addEventListener("input", () => renderTrigger(inputId));

        new MutationObserver(() => syncDisabled(inputId))
            .observe(input, { attributes: true, attributeFilter: ["disabled"] });

        document.addEventListener("click", (e) => {
            if (!wrap.contains(e.target)) closePopover(inputId);
        });
        document.addEventListener("keydown", (e) => {
            if (e.key === "Escape") closePopover(inputId);
        });

        applyMode(inputId);
        syncDisabled(inputId);

        return inst;
    }

    function attachAll(inputIds) {
        inputIds.forEach(attach);
    }

    function refresh(inputId) {
        renderTrigger(inputId);
        closePopover(inputId);
        const inst = instances[inputId];
        if (inst) inst.viewYear = null; // re-derive view from value next open
    }

    function refreshAll() {
        Object.keys(instances).forEach(refresh);
    }

    function updateToggleUI() {
        const pref = getPreference();
        toggleButtons.forEach((btn) => btn.classList.toggle("active", btn.dataset.pref === pref));
    }

    function buildToggleEl() {
        const wrap = document.createElement("div");
        wrap.className = "cal-toggle";
        wrap.title = "Calendar system";

        ["GC", "EC"].forEach((pref) => {
            const btn = document.createElement("button");
            btn.type = "button";
            btn.className = "cal-toggle-btn";
            btn.dataset.pref = pref;
            btn.textContent = pref;
            btn.addEventListener("click", () => setPreference(pref));
            wrap.appendChild(btn);
            toggleButtons.push(btn);
        });

        return wrap;
    }

    // Injects the GC/EC toggle into the top nav. nav.js's exact markup
    // isn't known to this module, so it looks for the `.topnav-user`
    // slot (present in style.css) and retries briefly in case nav.js
    // renders asynchronously; falls back to `#app-nav` itself.
    function mountToggle(retriesLeft) {
        if (typeof retriesLeft !== "number") retriesLeft = 20;
        if (document.querySelector(".cal-toggle")) return; // already mounted

        const target = document.querySelector(".topnav-user") || document.getElementById("app-nav");
        if (!target) {
            if (retriesLeft > 0) setTimeout(() => mountToggle(retriesLeft - 1), 100);
            return;
        }
        if (!document.querySelector(".topnav-user") && retriesLeft > 0) {
            // app-nav exists but hasn't rendered its inner content yet - keep waiting
            // for the preferred .topnav-user slot a little longer.
            setTimeout(() => mountToggle(retriesLeft - 1), 100);
            return;
        }

        const toggleEl = buildToggleEl();
        target.insertBefore(toggleEl, target.firstChild);
        updateToggleUI();
    }

    global.DatePicker = {
        attach,
        attachAll,
        refresh,
        refreshAll,
        mountToggle,
        getPreference,
        setPreference,
    };
})(window);