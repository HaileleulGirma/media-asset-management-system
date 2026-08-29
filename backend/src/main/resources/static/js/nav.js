/**
 * Renders the top nav into <div id="app-nav"></div> on every app page
 * (not the login page). Links are shown/hidden based on the JWT's
 * decoded role claim -- display convenience only, server still enforces
 * access via @PreAuthorize.
 */
(function () {
    if (!Auth.isLoggedIn()) {
        window.location.href = "/index.html";
        return;
    }

    const isAdmin = Auth.hasRole("ADMIN");
    const isStaff = Auth.hasRole("STAFF");
    const isAdminOrStaff = isAdmin || isStaff;
    const currentPage = window.location.pathname.split("/").pop();

    function navLink(href, label, visible) {
        if (!visible) return "";
        const active = currentPage === href ? "active" : "";
        return `<a href="/${href}" class="${active}">${label}</a>`;
    }

    const roleLabel = isAdmin ? "Admin" : isStaff ? "Staff" : "Viewer";

    const navHtml = `
<img src="/images/ena_logo_full.png" alt="Agency Logo" class="topnav-logo" />
    <div class="topnav-brand">MAM</div>
    <div class="topnav-links">
      ${navLink("news.html", "News", true)}
      ${navLink("reporters.html", "Reporters", isAdminOrStaff)}
      ${navLink("cameramen.html", "Cameramen", isAdminOrStaff)}
      ${navLink("locations.html", "Locations", isAdminOrStaff)}
      ${navLink("staff.html", "Staff", isAdminOrStaff)}
      ${navLink("users.html", "Users", isAdmin)}
    </div>
    <div class="topnav-user">
      <span class="role-badge">${roleLabel}</span>
      <button id="logout-btn" class="btn btn-secondary">Log out</button>
    </div>
  `;

    const navEl = document.getElementById("app-nav");
    navEl.innerHTML = navHtml;
    navEl.classList.add("topnav");

    document.getElementById("logout-btn").addEventListener("click", () => {
        Auth.logout();
    });
})();