let authHeader = null;
let allItems = [];
let selectedItem = null;

const CATEGORY_OPTIONS = ["Select Category", "Networking", "Servers", "Peripherals", "Storage", "Cabling", "Power", "Accessories"];
const LOCATION_OPTIONS = ["Select Location", "Warehouse A", "Warehouse B", "Main Office", "Server Room", "Lab 1", "Lab 2"];

const el = (id) => document.getElementById(id);

function setMsg(text) { el("msg").textContent = text || ""; }
function setEditMsg(text) { el("editMsg").textContent = text || ""; }

function buildAuth() {
    const u = el("username").value.trim();
    const p = el("password").value;
    if (!u || !p) return null;
    return "Basic " + btoa(u + ":" + p);
}

function populateSelect(id, options) {
    const select = el(id);
    select.innerHTML = options.map((opt, index) => `<option value="${index === 0 ? "" : escapeHtml(opt)}">${escapeHtml(opt)}</option>`).join("");
}

async function apiFetch(path, options = {}) {
    if (!authHeader) throw new Error("Please login first.");

    const headers = { ...(options.headers || {}), Authorization: authHeader };
    const isFormData = options.body instanceof FormData;
    const isBlobRequest = options.expectBlob;

    if (!isFormData && !isBlobRequest) headers["Content-Type"] = "application/json";

    const res = await fetch(path, { ...options, headers });
    if (!res.ok) {
        const txt = await res.text().catch(() => "");
        throw new Error(`HTTP ${res.status} ${res.statusText}${txt ? " - " + txt : ""}`);
    }

    if (isBlobRequest) return res.blob();

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

function buildActionDropdown(it) {
    const wrapper = document.createElement("div");
    wrapper.className = "action-menu";

    const select = document.createElement("select");
    select.className = "action-select";
    select.innerHTML = `
      <option value="">Actions</option>
      <option value="details">Delivery Receipt</option>
      <option value="hardware">Hardware Revision</option>
      <option value="vendor">Vendor</option>
      <option value="history">Inventory History</option>
      <option value="image">Upload Image</option>
    `;

    select.onchange = () => {
        if (!select.value) return;
        openDetails(it);
        if (select.value === "image") {
            el("detailsTitle").textContent = `Upload Image - ${it.serialNumber}`;
        }
        select.value = "";
    };

    wrapper.appendChild(select);
    wrapper.appendChild(rowButton("Edit", "secondary", () => openEdit(it)));
    wrapper.appendChild(rowButton("Delete", "secondary", async () => {
        if (!confirm(`Delete item ${it.serialNumber}?`)) return;
        try {
            await apiFetch(`/api/items/${it.id}`, { method: "DELETE" });
            setMsg("Deleted.");
            await loadItems();
            await fetchHistory();
        } catch (e) {
            setMsg(e.message);
        }
    }));

    return wrapper;
}

function renderItems(items) {
    const q = el("search").value.trim().toLowerCase();
    const filtered = items.filter((it) => {
        const s = `${it.serialNumber || ""} ${it.description || ""} ${it.category || ""} ${it.location || ""}`.toLowerCase();
        return s.includes(q);
    });

    const body = el("itemsBody");
    body.innerHTML = "";

    filtered.forEach((it) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
          <td>${escapeHtml(it.serialNumber || "")}</td>
          <td>${escapeHtml(it.description || "")}</td>
          <td>${escapeHtml(it.category || "")}</td>
          <td>${escapeHtml(it.location || "")}</td>
          <td>${it.quantity ?? 0}</td>
          <td>${escapeHtml(it.lastEditedBy || "-")}</td>
          <td></td>
        `;

        tr.querySelector("td:last-child").appendChild(buildActionDropdown(it));
        body.appendChild(tr);
    });
}

async function loadItems() {
    allItems = await apiFetch("/api/items", { method: "GET" });
    renderItems(allItems);
}

async function fileToDataUrl(file) {
    if (!file) return "";
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result);
        reader.onerror = reject;
        reader.readAsDataURL(file);
    });
}

function addPayloadFromForm(prefix = "") {
    return {
        serialNumber: el(`${prefix}serialNumber`).value.trim(),
        description: el(`${prefix}description`).value.trim(),
        category: el(`${prefix}category`).value.trim(),
        location: el(`${prefix}location`).value.trim(),
        quantity: Number(el(`${prefix}quantity`).value),
        deliveryReceipt: el(`${prefix}deliveryReceipt`).value.trim(),
        hardwareRevision: el(`${prefix}hardwareRevision`).value.trim(),
        vendor: el(`${prefix}vendor`).value.trim()
    };
}

async function addItem() {
    setMsg("");
    const payload = addPayloadFromForm("");
    const imageFile = el("imageFile").files[0];
    payload.imageData = await fileToDataUrl(imageFile);

    if (!payload.serialNumber || !payload.description || !payload.category || !payload.location || Number.isNaN(payload.quantity) || payload.quantity < 0) {
        setMsg("Fill out Serial Number, Description, Category, Location, and Quantity correctly.");
        return;
    }

    const created = await apiFetch("/api/items", { method: "POST", body: JSON.stringify(payload) });

    ["serialNumber", "description", "quantity", "deliveryReceipt", "hardwareRevision", "vendor", "imageFile"].forEach((id) => {
        if (el(id).type === "file") el(id).value = "";
        else el(id).value = "";
    });
    el("category").value = "";
    el("location").value = "";

    setMsg(`Added item ${created.serialNumber}.`);
    await loadItems();
    await fetchHistory();
}

function openEdit(it) {
    setEditMsg("");
    el("editId").value = it.id;
    el("editSerialNumber").value = it.serialNumber || "";
    el("editDescription").value = it.description || "";
    el("editCategory").value = it.category || "";
    el("editLocation").value = it.location || "";
    el("editQuantity").value = it.quantity ?? 0;
    el("editDeliveryReceipt").value = it.deliveryReceipt || "";
    el("editHardwareRevision").value = it.hardwareRevision || "";
    el("editVendor").value = it.vendor || "";
    el("editImageFile").value = "";
    el("modal").classList.remove("hidden");
}

function closeEdit() { el("modal").classList.add("hidden"); }
function closeDetails() { el("detailsModal").classList.add("hidden"); }

async function saveEdit() {
    const id = el("editId").value;
    const payload = {
        serialNumber: el("editSerialNumber").value.trim(),
        description: el("editDescription").value.trim(),
        category: el("editCategory").value.trim(),
        location: el("editLocation").value.trim(),
        quantity: Number(el("editQuantity").value),
        deliveryReceipt: el("editDeliveryReceipt").value.trim(),
        hardwareRevision: el("editHardwareRevision").value.trim(),
        vendor: el("editVendor").value.trim()
    };

    const existing = allItems.find((item) => String(item.id) === String(id));
    payload.imageData = existing?.imageData || "";
    const newImageFile = el("editImageFile").files[0];
    if (newImageFile) payload.imageData = await fileToDataUrl(newImageFile);

    if (!payload.serialNumber || !payload.description || !payload.category || !payload.location || Number.isNaN(payload.quantity) || payload.quantity < 0) {
        setEditMsg("Fill out all required fields correctly.");
        return;
    }

    const updated = await apiFetch(`/api/items/${id}`, { method: "PUT", body: JSON.stringify(payload) });
    setEditMsg(`Saved item ${updated.serialNumber}.`);
    await loadItems();
    await fetchHistory();
    setTimeout(closeEdit, 350);
}

async function importExcel() {
    try {
        const f = el("excelFile").files[0];
        if (!f) throw new Error("Choose an .xlsx file first.");

        const form = new FormData();
        form.append("file", f);

        const data = await apiFetch("/api/import/excel", { method: "POST", body: form });
        setMsg(`Import done: inserted=${data.inserted}, updated=${data.updated}, errors=${data.errors?.length ?? 0}`);
        await loadItems();
        await fetchHistory();
    } catch (e) {
        setMsg(e.message);
    }
}

async function exportExcel() {
    try {
        const blob = await apiFetch("/api/import/excel", { method: "GET", expectBlob: true });
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = "inventory-export.xlsx";
        a.click();
        URL.revokeObjectURL(url);
    } catch (e) {
        setMsg(e.message);
    }
}

async function fetchHistory() {
    const historyList = await apiFetch("/api/history", { method: "GET" });
    renderHistory(historyList, el("historyBody"), true);
}

function renderHistory(historyList, tbody, showItemName = false) {
    tbody.innerHTML = "";
    historyList.sort((a, b) => (b.id ?? 0) - (a.id ?? 0));
    for (const h of historyList) {
        const tr = document.createElement("tr");
        tr.innerHTML = showItemName
            ? `<td>${escapeHtml(formatDateTime(h.createdAt))}</td><td>${escapeHtml(h.itemName ?? "")}</td><td>${escapeHtml(h.action ?? "")}</td><td style="font-weight:600;">${formatQty(h.quantityChange)}</td>`
            : `<td>${escapeHtml(formatDateTime(h.createdAt))}</td><td>${escapeHtml(h.action ?? "")}</td><td style="font-weight:600;">${formatQty(h.quantityChange)}</td>`;
        tbody.appendChild(tr);
    }
}

async function openDetails(it) {
    selectedItem = it;
    el("detailsTitle").textContent = `Item Details - ${it.serialNumber || ""}`;
    el("detailDeliveryReceipt").textContent = it.deliveryReceipt || "-";
    el("detailHardwareRevision").textContent = it.hardwareRevision || "-";
    el("detailVendor").textContent = it.vendor || "-";
    el("detailLastEditedBy").textContent = it.lastEditedBy || "-";

    const image = el("detailImage");
    const noImage = el("detailNoImage");
    if (it.imageData) {
        image.src = it.imageData;
        image.classList.remove("hidden");
        noImage.classList.add("hidden");
    } else {
        image.src = "";
        image.classList.add("hidden");
        noImage.classList.remove("hidden");
    }

    await loadItemHistory(it.id);
    el("detailsModal").classList.remove("hidden");
}

async function loadItemHistory(itemId) {
    const historyList = await apiFetch(`/api/history/item/${itemId}`, { method: "GET" });
    renderHistory(historyList, el("itemHistoryBody"), false);
}

function formatQty(qty) { return qty > 0 ? `+${qty}` : `${qty ?? 0}`; }

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

async function login() {
    try {
        authHeader = buildAuth();
        if (!authHeader) {
            el("authStatus").textContent = "Enter username and password first.";
            return;
        }

        await apiFetch("/api/items", { method: "GET" });
        el("authStatus").textContent = "Connected";
        el("loginModal").classList.add("hidden");

        await loadItems();
        await fetchHistory();
    } catch (e) {
        el("authStatus").textContent = "Authentication failed";
        setMsg(e.message);
    }
}

populateSelect("category", CATEGORY_OPTIONS);
populateSelect("location", LOCATION_OPTIONS);
populateSelect("editCategory", CATEGORY_OPTIONS);
populateSelect("editLocation", LOCATION_OPTIONS);

el("btnSaveAuth").onclick = login;
el("btnOpenLogin").onclick = () => el("loginModal").classList.remove("hidden");
el("btnAdd").onclick = () => addItem().catch((e) => setMsg(e.message));
el("btnRefresh").onclick = () => loadItems().catch((e) => setMsg(e.message));
el("search").oninput = () => renderItems(allItems);
el("btnCancelEdit").onclick = closeEdit;
el("btnSaveEdit").onclick = () => saveEdit().catch((e) => setEditMsg(e.message));
el("btnImport").onclick = () => importExcel().catch((e) => setMsg(e.message));
el("btnExport").onclick = () => exportExcel().catch((e) => setMsg(e.message));
el("refreshHistoryBtn").onclick = () => fetchHistory().catch((e) => setMsg(e.message));
el("btnCloseDetails").onclick = closeDetails;
el("btnLoadItemHistory").onclick = () => selectedItem && loadItemHistory(selectedItem.id).catch((e) => setMsg(e.message));

document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
        closeEdit();
        closeDetails();
        el("loginModal").classList.add("hidden");
    }
});