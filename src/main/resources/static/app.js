let authHeader = null;

const el = (id) => document.getElementById(id);

function setMsg(text) { el("msg").textContent = text || ""; }
function setEditMsg(text) { el("editMsg").textContent = text || ""; }

function buildAuth() {
    const u = el("username").value.trim();
    const p = el("password").value;
    if (!u || !p) return null;
    return "Basic " + btoa(u + ":" + p);
}

async function apiFetch(path, options = {}) {
    if (!authHeader) throw new Error("Set credentials first.");

    const headers = { ...(options.headers || {}), Authorization: authHeader };

    const isFormData = options.body instanceof FormData;
    if (!isFormData) headers["Content-Type"] = "application/json";

    const res = await fetch(path, { ...options, headers });

    if (!res.ok) {
        const txt = await res.text().catch(() => "");
        throw new Error(`HTTP ${res.status} ${res.statusText}${txt ? " - " + txt : ""}`);
    }

    const ct = res.headers.get("content-type") || "";
    if (ct.includes("application/json")) return res.json();
    return null;
}

function rowButton(label, cls, onClick) {
    const btn = document.createElement("button");
    btn.textContent = label;
    btn.className = cls;
    btn.onclick = onClick;
    return btn;
}

function renderItems(items) {
    const q = el("search").value.trim().toLowerCase();
    const filtered = items.filter((it) => {
        const s = `${it.name} ${it.category} ${it.location}`.toLowerCase();
        return s.includes(q);
    });

    const body = el("itemsBody");
    body.innerHTML = "";

    filtered.forEach((it) => {
        const tr = document.createElement("tr");

        tr.innerHTML = `
      <td>${it.id}</td>
      <td>${escapeHtml(it.name)}</td>
      <td>${escapeHtml(it.category)}</td>
      <td>${escapeHtml(it.location)}</td>
      <td>${it.quantity}</td>
      <td></td>
    `;

        const actionsTd = tr.querySelector("td:last-child");
        actionsTd.appendChild(rowButton("Edit", "secondary", () => openEdit(it)));
        actionsTd.appendChild(rowButton("Delete", "secondary", async () => {
            if (!confirm(`Delete item #${it.id}?`)) return;
            try {
                await apiFetch(`/api/items/${it.id}`, { method: "DELETE" });
                setMsg("Deleted.");
                await loadItems();
                await fetchHistory(); // refresh history too
            } catch (e) {
                setMsg(e.message);
            }
        }));

        body.appendChild(tr);
    });
}

async function loadItems() {
    setMsg("");
    const items = await apiFetch("/api/items", { method: "GET" });
    renderItems(items);
}

async function addItem() {
    setMsg("");

    const name = el("Description").value.trim();
    const category = el("Category").value.trim();
    const location = el("Location").value.trim();
    const quantity = Number(el("Quantity").value);

    if (!name || !category || !location || Number.isNaN(quantity) || quantity < 0) {
        setMsg("Fill out all fields correctly (quantity must be 0 or more).");
        return;
    }

    const created = await apiFetch("/api/items", {
        method: "POST",
        body: JSON.stringify({ name, category, location, quantity })
    });

    el("Description").value = "";
    el("Category").value = "";
    el("Location").value = "";
    el("Quantity").value = "";

    setMsg(`Added item #${created.id}.`);
    await loadItems();
    await fetchHistory();
}

function openEdit(it) {
    setEditMsg("");
    el("editId").value = it.id;
    el("editName").value = it.name;
    el("editCategory").value = it.category;
    el("editLocation").value = it.location;
    el("editQuantity").value = it.quantity;
    el("modal").classList.remove("hidden");
}

function closeEdit() {
    el("modal").classList.add("hidden");
}

async function saveEdit() {
    setEditMsg("");

    const id = el("editId").value;

    const payload = {
        name: el("editName").value.trim(),
        category: el("editCategory").value.trim(),
        location: el("editLocation").value.trim(),
        quantity: Number(el("editQuantity").value)
    };

    if (!payload.name || !payload.category || !payload.location || Number.isNaN(payload.quantity) || payload.quantity < 0) {
        setEditMsg("Fill out all fields correctly (quantity must be 0 or more).");
        return;
    }

    const updated = await apiFetch(`/api/items/${id}`, {
        method: "PUT",
        body: JSON.stringify(payload)
    });

    setEditMsg(`Saved item #${updated.id}.`);
    await loadItems();
    await fetchHistory();
    setTimeout(closeEdit, 400);
}

async function importExcel() {
    try {
        if (!authHeader) throw new Error("Set credentials first.");
        const f = el("excelFile").files[0];
        if (!f) throw new Error("Choose an .xlsx file first.");

        const form = new FormData();
        form.append("file", f);

        const data = await apiFetch("/api/import/excel", {
            method: "POST",
            body: form
        });

        setMsg(`Import done: inserted=${data.inserted}, updated=${data.updated}, errors=${data.errors?.length ?? 0}`);
        await loadItems();
        await fetchHistory();
    } catch (e) {
        setMsg(e.message);
    }
}

async function fetchHistory() {
    if (!authHeader) return;

    const historyList = await apiFetch("/api/history", { method: "GET" });
    renderHistory(historyList);
}

function renderHistory(historyList) {
    const tbody = document.getElementById("historyBody");
    tbody.innerHTML = "";

    historyList.sort((a, b) => (b.id ?? 0) - (a.id ?? 0));

    for (const h of historyList) {
        const tr = document.createElement("tr");

        const dateText = formatDateTime(h.createdAt);
        const itemText = h.itemName ?? "(unknown)";
        const actionText = h.action ?? "";
        const qty = (h.quantityChange ?? 0);

        tr.innerHTML = `
      <td>${escapeHtml(dateText)}</td>
      <td>${escapeHtml(itemText)}</td>
      <td>${escapeHtml(actionText)}</td>
      <td style="font-weight:600;">${qty > 0 ? "+" + qty : qty}</td>
    `;

        tbody.appendChild(tr);
    }
}

function formatDateTime(value) {
    if (!value) return "";
    const d = new Date(value);
    if (isNaN(d.getTime())) return String(value);
    return d.toLocaleString();
}

function escapeHtml(str) {
    return String(str)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

el("btnSaveAuth").onclick = async () => {
    try {
        authHeader = buildAuth();
        if (!authHeader) {
            el("authStatus").textContent = "Enter username & password first.";
            return;
        }
        await apiFetch("/api/items", { method: "GET" });
        el("authStatus").textContent = "Connected ";
        await loadItems();
        await fetchHistory();
    } catch (e) {
        el("authStatus").textContent = "Authentication failed ";
        setMsg(e.message);
    }
};

el("btnAdd").onclick = () => addItem().catch((e) => setMsg(e.message));
el("btnRefresh").onclick = () => loadItems().catch((e) => setMsg(e.message));
el("search").oninput = () => loadItems().catch(() => {});
el("btnCancelEdit").onclick = closeEdit;
el("btnSaveEdit").onclick = () => saveEdit().catch((e) => setEditMsg(e.message));
el("btnImport").onclick = () => importExcel().catch((e) => setMsg(e.message));
document.getElementById("refreshHistoryBtn")?.addEventListener("click", () => fetchHistory().catch(() => {}));
document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") closeEdit();
});