/**
 * Ethiopian (Ge'ez) calendar <-> Gregorian calendar conversion.
 *
 * Pure functions, no DOM dependency. Pivoted through the Julian Day
 * Number (JDN), Amete Mihret epoch. Verified with a 5000-sample
 * round-trip fuzz test (1950-2100) and known reference dates
 * (Ethiopian New Year 2017 = 2024-09-11 Gregorian).
 *
 * All dates here are civil calendar dates (year/month/day integers),
 * not timestamps -- there is no timezone concept involved.
 */
(function (global) {
    "use strict";
    const JDN_OFFSET_AMETE_MIHRET = 1723856;
    const MONTHS = [
        "Meskerem", "Tikimt", "Hidar", "Tahsas", "Tir", "Yekatit",
        "Megabit", "Miazia", "Ginbot", "Sene", "Hamle", "Nehase", "Pagume"
    ];
    // Amharic month names, same index order as MONTHS.
    const MONTHS_AM = [
        "\u1218\u1235\u12A8\u1228\u121D", "\u1325\u1245\u121D\u1275", "\u1215\u12F3\u122D", "\u1273\u1285\u1223\u1225",
        "\u1325\u122D", "\u12E8\u12AB\u1272\u1275", "\u1218\u130B\u1262\u1275", "\u121A\u12EB\u12DD\u12EB",
        "\u130D\u1295\u1266\u1275", "\u1230\u1294", "\u1210\u121D\u120C", "\u1290\u1210\u1234", "\u1333\u1309\u121C"
    ];
    const WEEKDAYS_SHORT = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
    // Amharic weekday short labels, same index order (0 = Sunday).
    const WEEKDAYS_SHORT_AM = [
        "\u12A5\u1211\u12F5", "\u1230\u129E", "\u121B\u12AD\u1230\u129E", "\u1228\u1261\u12D5",
        "\u1210\u1219\u1235", "\u12D3\u122D\u1265", "\u1245\u12F2\u1218"
    ];
    function isLeap(ethYear) {
        // Amete Mihret leap years: year % 4 === 3 (Pagume gets a 6th day).
        return (((ethYear % 4) + 4) % 4) === 3;
    }
    function daysInMonth(ethYear, ethMonth) {
        if (ethMonth === 13) return isLeap(ethYear) ? 6 : 5;
        return 30;
    }
    function gregorianToJDN(year, month, day) {
        const a = Math.floor((14 - month) / 12);
        const y = year + 4800 - a;
        const m = month + 12 * a - 3;
        return day + Math.floor((153 * m + 2) / 5) + 365 * y +
            Math.floor(y / 4) - Math.floor(y / 100) + Math.floor(y / 400) - 32045;
    }
    function jdnToGregorian(jdn) {
        const a = jdn + 32044;
        const b = Math.floor((4 * a + 3) / 146097);
        const c = a - Math.floor((146097 * b) / 4);
        const d = Math.floor((4 * c + 3) / 1461);
        const e = c - Math.floor((1461 * d) / 4);
        const m = Math.floor((5 * e + 2) / 153);
        const day = e - Math.floor((153 * m + 2) / 5) + 1;
        const month = m + 3 - 12 * Math.floor(m / 10);
        const year = 100 * b + d - 4800 + Math.floor(m / 10);
        return { year, month, day };
    }
    function ethiopianToJDN(year, month, day) {
        const k = ((year % 4) + 4) % 4;
        const q = (year - k) / 4;
        const n = (month - 1) * 30 + (day - 1);
        const r = k * 365 + n;
        return JDN_OFFSET_AMETE_MIHRET + q * 1461 + r;
    }
    function jdnToEthiopian(jdn) {
        const diff = jdn - JDN_OFFSET_AMETE_MIHRET;
        const r = diff % 1461;
        const n = (r % 365) + 365 * Math.floor(r / 1460);
        const year = 4 * Math.floor(diff / 1461) + Math.floor(r / 365) - Math.floor(r / 1460);
        const month = Math.floor(n / 30) + 1;
        const day = (n % 30) + 1;
        return { year, month, day };
    }
    function jdnToWeekday(jdn) {
        // 0 = Sunday .. 6 = Saturday, matches Date#getDay().
        return ((jdn % 7) + 7 + 1) % 7;
    }
    /** @param {string} isoDate "YYYY-MM-DD" Gregorian */
    function gregorianToEthiopian(isoDate) {
        const [y, m, d] = isoDate.split("-").map(Number);
        return jdnToEthiopian(gregorianToJDN(y, m, d));
    }
    /** @returns {string} "YYYY-MM-DD" Gregorian, zero-padded */
    function ethiopianToGregorianIso(ethYear, ethMonth, ethDay) {
        const g = jdnToGregorian(ethiopianToJDN(ethYear, ethMonth, ethDay));
        return `${String(g.year).padStart(4, "0")}-${String(g.month).padStart(2, "0")}-${String(g.day).padStart(2, "0")}`;
    }
    function weekdayOfEthiopian(ethYear, ethMonth, ethDay) {
        return jdnToWeekday(ethiopianToJDN(ethYear, ethMonth, ethDay));
    }
    function todayEthiopian() {
        const now = new Date();
        return gregorianToEthiopian(
            `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`
        );
    }
    function formatEthiopian(ethYear, ethMonth, ethDay) {
        return `${ethDay} ${MONTHS[ethMonth - 1]} ${ethYear}`;
    }
    // Amharic equivalent of formatEthiopian.
    function formatEthiopianAmharic(ethYear, ethMonth, ethDay) {
        return `${ethDay} ${MONTHS_AM[ethMonth - 1]} ${ethYear}`;
    }
    global.EthiopianCalendar = {
        MONTHS,
        MONTHS_AM,
        WEEKDAYS_SHORT,
        WEEKDAYS_SHORT_AM,
        isLeap,
        daysInMonth,
        gregorianToEthiopian,
        ethiopianToGregorianIso,
        weekdayOfEthiopian,
        todayEthiopian,
        formatEthiopian,
        formatEthiopianAmharic,
    };
})(window);