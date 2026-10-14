import './style.css';
import { api, onApiCall, ApiError } from './api.js';

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => [...document.querySelectorAll(sel)];

const esc = (v) =>
  String(v ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);
const money = (v) => (v == null ? '—' : `$${Number(v).toFixed(2)}`);
const dateTime = (iso) => (iso ? new Date(iso).toLocaleString() : '—');
const timeOnly = (iso) => (iso ? new Date(iso).toLocaleTimeString() : '—');
const shortId = (id) => (id ? String(id).slice(0, 8) : '—');
const badge = (value) => (value ? `<span class="badge b-${esc(value)}">${esc(value)}</span>` : '');
const formData = (form) => Object.fromEntries(new FormData(form).entries());

function showMsg(el, type, text) {
  el.className = `msg show ${type}`;
  el.textContent = text;
}
function clearMsg(el) {
  el.className = 'msg';
  el.textContent = '';
}
function describe(e) {
  if (e instanceof ApiError) return e.status ? `${e.code} (${e.status}): ${e.message}` : e.message;
  return e.message;
}

/** Runs an async UI action: disables the button, shows the error in msgEl when it fails. */
async function run(button, msgEl, fn) {
  if (button) button.disabled = true;
  clearMsg(msgEl);
  try {
    return await fn();
  } catch (e) {
    showMsg(msgEl, 'error', describe(e));
  } finally {
    if (button) button.disabled = false;
    refreshOrderButtons();
  }
}

function scrollTo(id) {
  document.getElementById(id).scrollIntoView({ behavior: 'smooth', block: 'start' });
}

// ---------------------------------------------------------------------------
// State and sessions (JWTs are kept in localStorage until they expire)
// ---------------------------------------------------------------------------

const SESSION_KEY = 'pc-builder-demo-sessions';

const state = {
  sessions: { customer: null, admin: null },
  categories: [],
  products: [],
  selectedProductId: null,
  addresses: [],
  order: null,
  recommendation: null,
  orderPoll: null,
  alertTimer: null,
  seenAlertIds: null,
};

function loadSessions() {
  try {
    const saved = JSON.parse(localStorage.getItem(SESSION_KEY)) || {};
    for (const role of ['customer', 'admin']) {
      if (saved[role] && new Date(saved[role].expiresAt) > new Date()) state.sessions[role] = saved[role];
    }
  } catch {
    /* storage unavailable: start logged out */
  }
}
function saveSessions() {
  try {
    localStorage.setItem(SESSION_KEY, JSON.stringify(state.sessions));
  } catch {
    /* ignore */
  }
}

function session(role) {
  const s = state.sessions[role];
  if (s && new Date(s.expiresAt) <= new Date()) {
    state.sessions[role] = null;
    saveSessions();
    renderSessions();
    return null;
  }
  return s;
}
function requireCustomer() {
  const s = session('customer');
  if (!s) throw new Error('Log in as a customer first (Login section).');
  return s;
}
function requireAdmin() {
  const s = session('admin');
  if (!s) throw new Error('Log in as admin first (Login section).');
  return s;
}

function renderSessions() {
  const chip = (role, label) => {
    const s = state.sessions[role];
    return s
      ? `<span class="chip on">${label}: <b>${esc(s.username)}</b> <small>until ${timeOnly(s.expiresAt)}</small></span>`
      : `<span class="chip off">${label}: not logged in</span>`;
  };
  $('#session-chips').innerHTML = chip('customer', 'Customer') + chip('admin', 'Admin');

  for (const role of ['customer', 'admin']) {
    const s = state.sessions[role];
    $(`#${role}-session`).innerHTML = s
      ? `<div class="session-box">
           <div>✅ <b>${esc(s.username)}</b> ${badge(s.role)}</div>
           <div class="mono small">userId ${esc(s.userId)}</div>
           <div class="small">JWT expires ${dateTime(s.expiresAt)}</div>
           <button type="button" class="small ghost" data-logout="${role}">Log out</button>
         </div>`
      : '';
  }
  $('#admin-hint').textContent = state.sessions.admin
    ? `Acting as admin "${state.sessions.admin.username}".`
    : 'Stock updates and product management need the admin login. Reading stock is public.';
}

// ---------------------------------------------------------------------------
// Login / register (account-service)
// ---------------------------------------------------------------------------

async function login(form, expectedRole) {
  const msg = $('#login-msg');
  await run(form.querySelector('button'), msg, async () => {
    const { username, password } = formData(form);
    const res = await api('account', 'POST', '/auth/login', { body: { username, password } });
    const role = res.role === 'ADMIN' ? 'admin' : 'customer';
    if (role !== expectedRole) {
      throw new Error(`"${username}" is a ${res.role} account; use the ${role} login form instead.`);
    }
    state.sessions[role] = { token: res.token, userId: res.userId, role: res.role, username, expiresAt: res.expiresAt };
    saveSessions();
    renderSessions();
    form.password.value = '';
    showMsg(msg, 'success', `Logged in as ${username} (${res.role}). The JWT is sent as "Authorization: Bearer …".`);
    if (role === 'customer') onCustomerChanged();
  });
}

$('#customer-login').addEventListener('submit', (e) => {
  e.preventDefault();
  login(e.target, 'customer');
});
$('#admin-login').addEventListener('submit', (e) => {
  e.preventDefault();
  login(e.target, 'admin');
});

document.addEventListener('click', (e) => {
  const role = e.target.dataset?.logout;
  if (!role) return;
  state.sessions[role] = null;
  saveSessions();
  renderSessions();
  if (role === 'customer') onCustomerChanged();
});

$('#register-form').addEventListener('submit', (e) => {
  e.preventDefault();
  const form = e.target;
  const msg = $('#login-msg');
  run(form.querySelector('button'), msg, async () => {
    const data = formData(form);
    const body = { username: data.username, email: data.email, phone: data.phone, password: data.password };
    if (data.firstName) body.firstName = data.firstName;
    if (data.lastName) body.lastName = data.lastName;
    const user = await api('account', 'POST', '/users', { body });
    $('#customer-login').username.value = user.username;
    form.reset();
    showMsg(msg, 'success', `Registered customer "${user.username}" (id ${user.id}). Now log in as customer.`);
  });
});

// ---------------------------------------------------------------------------
// Catalog (catalog-service)
// ---------------------------------------------------------------------------

const categoryName = (id) => state.categories.find((c) => c.id === id)?.name ?? '—';
const product = (id) => state.products.find((p) => p.id === id);
const productName = (id) => product(id)?.name ?? `product ${shortId(id)}`;

async function loadCategories() {
  state.categories = await api('catalog', 'GET', '/categories');
  const options = state.categories.map((c) => `<option value="${esc(c.id)}">${esc(c.name)}</option>`).join('');
  const filter = $('#category-filter');
  const current = filter.value;
  filter.innerHTML = `<option value="">All categories</option>${options}`;
  filter.value = current;
  $$('.category-select').forEach((sel) => {
    sel.innerHTML = state.categories.length ? options : '<option value="">No categories yet</option>';
  });
}

async function loadProducts(filters = {}) {
  const params = new URLSearchParams(Object.entries(filters).filter(([, v]) => v));
  const query = params.toString() ? `?${params}` : '';
  const list = await api('catalog', 'GET', `/products${query}`);
  if (!query) state.products = list;
  renderProductRows(list);
  if (!query) renderProductSelects();
}

function renderProductRows(list) {
  $('#product-rows').innerHTML = list.length
    ? list
        .map(
          (p) => `<tr class="clickable ${p.id === state.selectedProductId ? 'selected' : ''}" data-product="${esc(p.id)}">
            <td><b>${esc(p.name)}</b></td>
            <td>${esc(categoryName(p.categoryId))}</td>
            <td>${esc(p.brand)}</td>
            <td class="num">${money(p.price)}</td>
          </tr>`,
        )
        .join('')
    : '<tr><td colspan="4" class="empty">No products found. An admin can create categories and products in the Admin section.</td></tr>';
}

/** Fills every product dropdown, grouped by category. */
function renderProductSelects() {
  const groups = new Map();
  for (const p of state.products) {
    const cat = categoryName(p.categoryId);
    if (!groups.has(cat)) groups.set(cat, []);
    groups.get(cat).push(p);
  }
  const html = state.products.length
    ? [...groups.entries()]
        .map(
          ([cat, items]) =>
            `<optgroup label="${esc(cat)}">${items
              .map((p) => `<option value="${esc(p.id)}">${esc(p.name)} — ${money(p.price)}</option>`)
              .join('')}</optgroup>`,
        )
        .join('')
    : '<option value="">No products in the catalog</option>';
  $$('.product-select').forEach((sel) => {
    const current = sel.value;
    sel.innerHTML = html;
    if (current && product(current)) sel.value = current;
  });
}

async function loadTrending() {
  const list = await api('catalog', 'GET', '/products/trending');
  $('#trending-list').innerHTML = list.length
    ? list
        .map(
          (p, i) =>
            `<button type="button" class="chip-btn" data-product="${esc(p.id)}"><span class="rank">#${i + 1}</span> ${esc(p.name)}</button>`,
        )
        .join('')
    : '<span class="empty">Nothing viewed in the last 24 h. Open a product\'s details to make it trend.</span>';
}

async function selectProduct(id) {
  state.selectedProductId = id;
  $$('#product-rows tr').forEach((tr) => tr.classList.toggle('selected', tr.dataset.product === id));
  const box = $('#product-details');
  box.innerHTML = '<p class="empty">Loading…</p>';
  const p = await api('catalog', 'GET', `/products/${id}`);
  const reviews = await api('catalog', 'GET', `/products/${id}/reviews`);
  renderProductDetails(p, reviews);
  // The view was published as ProductViewed; the trending list picks it up via Kafka.
  setTimeout(() => loadTrending().catch(() => {}), 1500);
}

function stars(n) {
  return '★'.repeat(n) + '☆'.repeat(5 - n);
}

function renderProductDetails(p, reviews) {
  const avg = reviews.length ? reviews.reduce((s, r) => s + r.rating, 0) / reviews.length : null;
  $('#product-details').innerHTML = `
    <div class="details-head">
      <div>
        <h3>${esc(p.name)}</h3>
        <div class="muted">${esc(p.brand)} · ${esc(categoryName(p.categoryId))} · <span class="mono">${esc(shortId(p.id))}</span></div>
      </div>
      <div class="price">${money(p.price)}</div>
    </div>
    <div class="details-meta">Stock status ${badge(p.stockStatus)}</div>
    <table class="specs">
      ${(p.specifications || []).map((s) => `<tr><th>${esc(s.name)}</th><td>${esc(s.value)}</td></tr>`).join('')}
    </table>
    <div class="row wrap">
      <button type="button" class="small" data-action="to-cart">Add to cart…</button>
      <button type="button" class="small secondary" data-action="subscribe">Subscribe to stock alerts</button>
      <button type="button" class="small ghost" data-action="manage-stock">Manage stock (admin)</button>
    </div>
    <h4>Reviews ${avg != null ? `<span class="stars">${stars(Math.round(avg))}</span> <small>${avg.toFixed(1)} / 5 · ${reviews.length}</small>` : ''}</h4>
    <ul class="reviews">
      ${
        reviews.length
          ? reviews
              .map(
                (r) => `<li><span class="stars">${stars(r.rating)}</span> ${esc(r.comment || '')}
                  <div class="small muted">user ${esc(shortId(r.userId))} · ${dateTime(r.createdAt)}</div></li>`,
              )
              .join('')
          : '<li class="empty">No reviews yet.</li>'
      }
    </ul>
    <form id="review-form" class="row">
      <select name="rating">${[5, 4, 3, 2, 1].map((n) => `<option value="${n}">${n} ★</option>`).join('')}</select>
      <input name="comment" placeholder="Write a review (customer login)" />
      <button type="submit" class="small">Post</button>
    </form>`;
}

$('#product-search').addEventListener('submit', (e) => {
  e.preventDefault();
  run(e.target.querySelector('button'), $('#catalog-msg'), () => loadProducts(formData(e.target)));
});
$('#load-trending').addEventListener('click', (e) => run(e.target, $('#catalog-msg'), loadTrending));

document.addEventListener('click', (e) => {
  const el = e.target.closest('[data-product]');
  if (el && (el.closest('#product-rows') || el.closest('#trending-list'))) {
    run(null, $('#catalog-msg'), () => selectProduct(el.dataset.product));
  }
});

$('#product-details').addEventListener('click', (e) => {
  const action = e.target.dataset.action;
  const id = state.selectedProductId;
  if (!action || !id) return;
  if (action === 'to-cart') {
    $('#cart-product').value = id;
    scrollTo('order-card');
  } else if (action === 'subscribe') {
    $('#subscribe-form').productId.value = id;
    subscribe(e.target, id);
  } else if (action === 'manage-stock') {
    $('#stock-product').value = id;
    scrollTo('admin-card');
    run(null, $('#admin-msg'), loadStock);
  }
});

$('#product-details').addEventListener('submit', (e) => {
  if (e.target.id !== 'review-form') return;
  e.preventDefault();
  const form = e.target;
  run(form.querySelector('button'), $('#catalog-msg'), async () => {
    const s = requireCustomer();
    const { rating, comment } = formData(form);
    const id = state.selectedProductId;
    await api('catalog', 'POST', `/products/${id}/reviews`, {
      token: s.token,
      body: { userId: s.userId, rating: Number(rating), comment: comment || undefined },
    });
    await selectProduct(id);
    showMsg($('#catalog-msg'), 'success', 'Review posted.');
  });
});

// ---------------------------------------------------------------------------
// Admin / Inventory (inventory-service I3, catalog-service C5)
// ---------------------------------------------------------------------------

async function loadStock() {
  const id = $('#stock-product').value;
  if (!id) throw new Error('Select a product first.');
  const view = $('#stock-view');
  try {
    const inv = await api('inventory', 'GET', `/inventory/${id}`);
    renderStock(inv);
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) {
      view.innerHTML = '<p class="empty">No inventory record for this product yet — set a quantity to create one.</p>';
      return;
    }
    throw e;
  }
}

function renderStock(inv) {
  const available = inv.quantity - inv.reservedQuantity;
  $('#stock-view').innerHTML = `
    <div class="stat"><span>Quantity</span><b>${inv.quantity}</b></div>
    <div class="stat"><span>Reserved</span><b>${inv.reservedQuantity}</b></div>
    <div class="stat ${available > 0 ? 'ok' : 'bad'}"><span>Available</span><b>${available}</b></div>`;
}

$('#load-stock').addEventListener('click', (e) => run(e.target, $('#admin-msg'), loadStock));
$('#stock-product').addEventListener('change', () => run(null, $('#admin-msg'), loadStock));

$('#stock-form').addEventListener('submit', (e) => {
  e.preventDefault();
  const form = e.target;
  run(form.querySelector('button[type=submit]'), $('#admin-msg'), async () => {
    const s = requireAdmin();
    const { productId, quantity, reason } = formData(form);
    const inv = await api('inventory', 'PUT', `/inventory/${productId}`, {
      token: s.token,
      body: { quantity: Number(quantity), reason: reason || undefined },
    });
    renderStock(inv);
    form.quantity.value = '';
    showMsg($('#admin-msg'), 'success',
      `Stock of ${productName(productId)} set to ${inv.quantity}. Subscribers get an alert within a few seconds if it crossed a threshold.`);
  });
});

$('#category-form').addEventListener('submit', (e) => {
  e.preventDefault();
  const form = e.target;
  run(form.querySelector('button'), $('#admin-msg'), async () => {
    const s = requireAdmin();
    const c = await api('catalog', 'POST', '/categories', { token: s.token, body: { name: formData(form).name } });
    form.reset();
    await loadCategories();
    showMsg($('#admin-msg'), 'success', `Category "${c.name}" created.`);
  });
});

$('#product-form').addEventListener('submit', (e) => {
  e.preventDefault();
  const form = e.target;
  run(form.querySelector('button'), $('#admin-msg'), async () => {
    const s = requireAdmin();
    const data = formData(form);
    const specifications = data.specifications
      .split('\n')
      .map((line) => line.trim())
      .filter(Boolean)
      .map((line) => {
        const i = line.indexOf('=');
        if (i < 1) throw new Error(`Specification "${line}" must look like Name=Value.`);
        return { name: line.slice(0, i).trim(), value: line.slice(i + 1).trim() };
      });
    const p = await api('catalog', 'POST', '/products', {
      token: s.token,
      body: { name: data.name, brand: data.brand, price: Number(data.price), categoryId: data.categoryId, specifications },
    });
    form.reset();
    await loadProducts();
    $('#stock-product').value = p.id;
    showMsg($('#admin-msg'), 'success', `Product "${p.name}" created. Give it stock above so it can be ordered and recommended.`);
  });
});

// ---------------------------------------------------------------------------
// Stock alerts (inventory-service I2)
// ---------------------------------------------------------------------------

async function subscribe(button, productId) {
  await run(button, $('#alerts-msg'), async () => {
    const s = requireCustomer();
    await api('inventory', 'POST', `/inventory/${productId}/notifications`, { token: s.token, body: { userId: s.userId } });
    showMsg($('#alerts-msg'), 'success',
      `Subscribed to ${productName(productId)}. Change its stock in the Admin section to trigger STOCK_LOW / PRODUCT_RESTOCKED.`);
    $('#alerts-auto').checked = true;
    startAlertPolling();
  });
}

$('#subscribe-form').addEventListener('submit', (e) => {
  e.preventDefault();
  subscribe(e.target.querySelector('button'), e.target.productId.value);
});

async function loadAlerts(poll = false) {
  const s = session('customer');
  const list = $('#alerts-list');
  if (!s) {
    list.innerHTML = '<li class="empty">Log in as a customer to see your stock alerts.</li>';
    return;
  }
  const alerts = await api('inventory', 'GET', `/users/${s.userId}/stock-alerts`, { token: s.token, poll });
  const firstLoad = state.seenAlertIds === null;
  const seen = state.seenAlertIds ?? new Set();
  list.innerHTML = alerts.length
    ? alerts
        .map(
          (a) => `<li class="${!firstLoad && !seen.has(a.id) ? 'fresh' : ''}">
            <div class="alert-head">${badge(a.type)} <b>${esc(productName(a.productId))}</b>
              <span class="muted small">${timeOnly(a.occurredAt)}</span></div>
            <div>${esc(a.message)}</div>
            <div class="small muted">available: ${a.availableQuantity}</div>
          </li>`,
        )
        .join('')
    : '<li class="empty">No alerts yet. Subscribe to a product, then change its stock as admin.</li>';
  state.seenAlertIds = new Set(alerts.map((a) => a.id));
}

function startAlertPolling() {
  stopAlertPolling();
  if (!$('#alerts-auto').checked || !session('customer')) return;
  state.alertTimer = setInterval(() => {
    loadAlerts(true).catch((e) => {
      stopAlertPolling();
      $('#alerts-auto').checked = false;
      showMsg($('#alerts-msg'), 'error', `Auto-refresh stopped: ${describe(e)}`);
    });
  }, 3000);
}
function stopAlertPolling() {
  clearInterval(state.alertTimer);
  state.alertTimer = null;
}

$('#alerts-auto').addEventListener('change', startAlertPolling);
$('#load-alerts').addEventListener('click', (e) => run(e.target, $('#alerts-msg'), () => loadAlerts()));

// ---------------------------------------------------------------------------
// Recommendation (recommendation-service R1, R3, R4)
// ---------------------------------------------------------------------------

const COMPONENT_ICONS = { CPU: '🧠', Motherboard: '🧩', RAM: '💾', GPU: '🎮', Storage: '🗄️', PSU: '🔌', Case: '📦' };

function renderRecommendation(r) {
  state.recommendation = r;
  const remaining = r.budget - r.totalPrice;
  const pct = Math.min(100, Math.round((r.totalPrice / r.budget) * 100));
  $('#rec-result').innerHTML = `
    <div class="stats">
      <div class="stat"><span>Total price</span><b>${money(r.totalPrice)}</b></div>
      <div class="stat"><span>Budget</span><b>${money(r.budget)}</b></div>
      <div class="stat ${remaining >= 0 ? 'ok' : 'bad'}"><span>Left over</span><b>${money(remaining)}</b></div>
    </div>
    <div class="bar"><div style="width:${pct}%"></div></div>
    <div class="muted small">Purpose: <b>${esc(r.purpose)}</b>${r.preferences ? ` · Preferences: ${esc(r.preferences)}` : ''} ·
      recommendation <span class="mono">${esc(shortId(r.recommendationId))}</span></div>
    <h4>Build</h4>
    <p>${esc(r.description)}</p>
    <h4>Why this build (AI reasoning)</h4>
    <blockquote>${esc(r.reasoning)}</blockquote>
    <h4>Components (${r.components.length})</h4>
    <div class="components">
      ${r.components
        .map(
          (c) => `<div class="component">
            <span class="icon">${COMPONENT_ICONS[c.componentType] ?? '🔧'}</span>
            <div><div class="small muted">${esc(c.componentType)}</div><b>${esc(productName(c.productId))}</b></div>
            <span class="price">${money(c.price)}</span>
          </div>`,
        )
        .join('')}
    </div>
    <button type="button" id="rec-to-cart">Add this build to my cart (R3)</button>`;
}

$('#rec-form').addEventListener('submit', (e) => {
  e.preventDefault();
  const form = e.target;
  const button = $('#rec-submit');
  run(button, $('#rec-msg'), async () => {
    const s = requireCustomer();
    const data = formData(form);
    button.textContent = 'Asking the AI…';
    $('#rec-result').innerHTML = '<div class="loading"><span class="spinner"></span> Generating a build — the LLM can take up to a minute…</div>';
    try {
      const r = await api('recommendation', 'POST', '/recommendations', {
        token: s.token,
        body: { userId: s.userId, budget: Number(data.budget), purpose: data.purpose, preferences: data.preferences || undefined },
      });
      renderRecommendation(r);
      loadRecHistory().catch(() => {});
    } catch (err) {
      $('#rec-result').innerHTML = '<p class="empty">No recommendation — see the error below.</p>';
      throw err;
    } finally {
      button.textContent = 'Generate build';
    }
  });
});

$('#rec-result').addEventListener('click', (e) => {
  if (e.target.id !== 'rec-to-cart') return;
  run(e.target, $('#rec-msg'), async () => {
    const s = requireCustomer();
    const o = await api('order', 'POST', `/orders/from-recommendation/${state.recommendation.recommendationId}`, { token: s.token });
    setOrder(o);
    loadOrders().catch(() => {});
    scrollTo('order-card');
  });
});

async function loadRecHistory() {
  const s = session('customer');
  const box = $('#rec-history');
  if (!s) {
    box.innerHTML = '<p class="empty">Log in as a customer to see previous recommendations.</p>';
    return;
  }
  const list = await api('recommendation', 'GET', `/users/${s.userId}/recommendations`, { token: s.token });
  box.innerHTML = list.length
    ? list
        .map(
          (r) => `<button type="button" class="history-card" data-rec="${esc(r.recommendationId)}">
            <b>${esc(r.purpose)}</b>
            <span>${money(r.totalPrice)} <small class="muted">of ${money(r.budget)}</small></span>
            <small class="muted">${r.components.length} components · ${esc(shortId(r.recommendationId))}</small>
          </button>`,
        )
        .join('')
    : '<p class="empty">No recommendations yet.</p>';
}

$('#load-rec-history').addEventListener('click', (e) => run(e.target, $('#rec-msg'), loadRecHistory));
$('#rec-history').addEventListener('click', (e) => {
  const card = e.target.closest('[data-rec]');
  if (!card) return;
  run(null, $('#rec-msg'), async () => {
    const s = requireCustomer();
    renderRecommendation(await api('recommendation', 'GET', `/recommendations/${card.dataset.rec}`, { token: s.token }));
  });
});

// ---------------------------------------------------------------------------
// Order (order-service O1–O6) and saved addresses (account-service A1)
// ---------------------------------------------------------------------------

const LIFECYCLE = ['DRAFT', 'PLACED', 'PROCESSING', 'COMPLETED'];
const POLL_SECONDS = 20;

function addressLabel(a) {
  return `${a.streetNo} ${a.streetName}, ${a.suburb}, ${a.city} ${a.state} ${a.postCode}`;
}

function setOrder(o, pollInfo = '') {
  state.order = o;
  if (!o) {
    $('#order-view').innerHTML = '<p class="empty">No current order. Create a cart or open one from the history.</p>';
    refreshOrderButtons();
    return;
  }
  const reached = LIFECYCLE.indexOf(o.status);
  const address = state.addresses.find((a) => a.id === o.deliveryAddressId);
  $('#order-view').innerHTML = `
    <div class="details-head">
      <div>
        <h3>Order <span class="mono">${esc(shortId(o.id))}</span></h3>
        <div class="muted small">created ${dateTime(o.createdAt)}</div>
      </div>
      <div class="price">${money(o.totalPrice)}</div>
    </div>
    <ol class="timeline ${o.status === 'CANCELLED' ? 'cancelled' : ''}">
      ${LIFECYCLE.map((st, i) => `<li class="${i <= reached ? 'done' : ''} ${st === o.status ? 'current' : ''}">${st}</li>`).join('')}
      ${o.status === 'CANCELLED' ? '<li class="current cancel">CANCELLED</li>' : ''}
    </ol>
    <div class="details-meta">Status ${badge(o.status)} Payment ${badge(o.paymentStatus)}
      ${o.paymentDueAt && o.status === 'PROCESSING' ? `<span class="small muted">pay by ${timeOnly(o.paymentDueAt)}</span>` : ''}</div>
    ${pollInfo ? `<div class="polling"><span class="spinner"></span> ${esc(pollInfo)}</div>` : ''}
    <div class="small">Delivery address: ${
      o.deliveryAddressId ? esc(address ? addressLabel(address) : o.deliveryAddressId) : '<span class="muted">not selected</span>'
    }</div>
    <table class="items">
      <thead><tr><th>Product</th><th class="num">Qty</th><th class="num">Unit</th><th class="num">Subtotal</th><th></th></tr></thead>
      <tbody>
      ${
        o.items.length
          ? o.items
              .map(
                (it) => `<tr>
                  <td>${esc(productName(it.productId))}</td>
                  <td class="num">${it.quantity}</td>
                  <td class="num">${money(it.unitPrice)}</td>
                  <td class="num">${money(it.unitPrice * it.quantity)}</td>
                  <td>${o.status === 'DRAFT' ? `<button type="button" class="small ghost" data-remove-item="${esc(it.id)}" title="Remove">✕</button>` : ''}</td>
                </tr>`,
              )
              .join('')
          : '<tr><td colspan="5" class="empty">Cart is empty.</td></tr>'
      }
      </tbody>
    </table>`;
  refreshOrderButtons();
}

/** Enables the order actions that are valid in the current order status. */
function refreshOrderButtons() {
  const status = state.order?.status;
  $$('#order-card [data-needs]').forEach((b) => {
    const needs = b.dataset.needs;
    b.disabled = needs === 'CANCELLABLE' ? !['DRAFT', 'PLACED', 'PROCESSING'].includes(status) : status !== needs;
  });
}

function stopOrderPoll() {
  clearInterval(state.orderPoll);
  state.orderPoll = null;
}

/** After checkout the order is PLACED; the reservation result moves it on asynchronously via Kafka. */
function pollOrder(orderId) {
  stopOrderPoll();
  const s = requireCustomer();
  const started = Date.now();
  setOrder(state.order, 'Waiting for the stock reservation (inventory-service via Kafka)…');
  state.orderPoll = setInterval(async () => {
    const elapsed = Math.round((Date.now() - started) / 1000);
    try {
      const o = await api('order', 'GET', `/orders/${orderId}`, { token: s.token, poll: true });
      if (o.status !== 'PLACED') {
        stopOrderPoll();
        setOrder(o);
        loadOrders().catch(() => {});
        const msg = $('#order-msg');
        if (o.status === 'PROCESSING') showMsg(msg, 'success', `StockReserved received after ~${elapsed}s: order is PROCESSING. Now record the payment.`);
        else showMsg(msg, 'error', `Order is ${o.status} — stock could not be reserved (StockReservationFailed).`);
      } else if (elapsed >= POLL_SECONDS) {
        stopOrderPoll();
        setOrder(o);
        showMsg($('#order-msg'), 'error',
          `Still PLACED after ${POLL_SECONDS}s. Are Kafka and inventory-service running? Use "Open" in the history to check again.`);
      } else {
        setOrder(o, `Polling GET /orders/${shortId(orderId)}… ${elapsed}s (status PLACED)`);
      }
    } catch (e) {
      stopOrderPoll();
      setOrder(state.order);
      showMsg($('#order-msg'), 'error', `Polling stopped: ${describe(e)}`);
    }
  }, 1000);
}

async function loadOrders() {
  const s = session('customer');
  const rows = $('#order-rows');
  if (!s) {
    rows.innerHTML = '<tr><td colspan="7" class="empty">Log in as a customer to see the order history.</td></tr>';
    return [];
  }
  const list = await api('order', 'GET', `/users/${s.userId}/orders`, { token: s.token });
  rows.innerHTML = list.length
    ? list
        .map(
          (o) => `<tr class="${o.id === state.order?.id ? 'selected' : ''}">
            <td>${dateTime(o.createdAt)}</td>
            <td class="mono">${esc(shortId(o.id))}</td>
            <td>${badge(o.status)}</td>
            <td>${badge(o.paymentStatus)}</td>
            <td class="num">${o.items.reduce((n, it) => n + it.quantity, 0)}</td>
            <td class="num">${money(o.totalPrice)}</td>
            <td><button type="button" class="small ghost" data-open-order="${esc(o.id)}">Open</button></td>
          </tr>`,
        )
        .join('')
    : '<tr><td colspan="7" class="empty">No orders yet.</td></tr>';
  return list;
}

async function loadAddresses() {
  const s = session('customer');
  const sel = $('#address-select');
  if (!s) {
    state.addresses = [];
    sel.innerHTML = '<option value="">Log in to load saved addresses</option>';
    return;
  }
  state.addresses = await api('account', 'GET', `/users/${s.userId}/addresses`, { token: s.token });
  sel.innerHTML = state.addresses.length
    ? state.addresses.map((a) => `<option value="${esc(a.id)}">${esc(addressLabel(a))}</option>`).join('')
    : '<option value="">No saved addresses — add one below</option>';
}

/** Runs an order action that returns the updated OrderResponse. */
function orderAction(button, fn) {
  return run(button, $('#order-msg'), async () => {
    const s = requireCustomer();
    if (!state.order) throw new Error('No current order: create a cart first.');
    stopOrderPoll();
    const o = await fn(s, state.order.id);
    setOrder(o);
    loadOrders().catch(() => {});
    return o;
  });
}

$('#create-cart').addEventListener('click', (e) =>
  run(e.target, $('#order-msg'), async () => {
    const s = requireCustomer();
    try {
      setOrder(await api('order', 'POST', '/orders', { token: s.token, body: { userId: s.userId } }));
      showMsg($('#order-msg'), 'success', 'Cart created (order in DRAFT status).');
    } catch (err) {
      if (!(err instanceof ApiError) || err.code !== 'OrderStateViolation') throw err;
      const draft = (await loadOrders()).find((o) => o.status === 'DRAFT');
      if (!draft) throw err;
      setOrder(draft);
      showMsg($('#order-msg'), 'info', `You already have a cart (${err.message}). Loaded the existing DRAFT order.`);
    }
    loadOrders().catch(() => {});
  }),
);

$('#add-item-form').addEventListener('submit', (e) => {
  e.preventDefault();
  const { productId, quantity } = formData(e.target);
  orderAction(e.submitter, (s, id) =>
    api('order', 'POST', `/orders/${id}/items`, { token: s.token, body: { productId, quantity: Number(quantity) } }),
  );
});

$('#order-view').addEventListener('click', async (e) => {
  const itemId = e.target.dataset.removeItem;
  if (!itemId) return;
  orderAction(e.target, async (s, id) => {
    await api('order', 'DELETE', `/orders/${id}/items/${itemId}`, { token: s.token });
    return api('order', 'GET', `/orders/${id}`, { token: s.token });
  });
});

$('#address-form').addEventListener('submit', (e) => {
  e.preventDefault();
  const { addressId } = formData(e.target);
  orderAction(e.submitter, (s, id) => {
    if (!addressId) throw new Error('No saved address selected — add one first.');
    return api('order', 'PUT', `/orders/${id}/delivery-address`, { token: s.token, body: { addressId } });
  });
});

$('#new-address-form').addEventListener('submit', (e) => {
  e.preventDefault();
  const form = e.target;
  run(form.querySelector('button'), $('#order-msg'), async () => {
    const s = requireCustomer();
    const d = formData(form);
    const a = await api('account', 'POST', `/users/${s.userId}/addresses`, {
      token: s.token,
      body: { ...d, streetNo: Number(d.streetNo), postCode: Number(d.postCode) },
    });
    form.reset();
    await loadAddresses();
    $('#address-select').value = a.id;
    showMsg($('#order-msg'), 'success', `Address saved: ${addressLabel(a)}.`);
  });
});

$('#checkout').addEventListener('click', async (e) => {
  const o = await orderAction(e.target, (s, id) => api('order', 'POST', `/orders/${id}/checkout`, { token: s.token }));
  if (o?.status === 'PLACED') pollOrder(o.id);
});

for (const [id, paymentStatus] of [['#pay-paid', 'PAID'], ['#pay-failed', 'FAILED']]) {
  $(id).addEventListener('click', (e) =>
    orderAction(e.target, (s, orderId) =>
      api('order', 'PUT', `/orders/${orderId}/payment`, { token: s.token, body: { paymentStatus } }),
    ),
  );
}

$('#cancel-order').addEventListener('click', (e) =>
  orderAction(e.target, (s, id) => api('order', 'POST', `/orders/${id}/cancel`, { token: s.token })),
);

$('#load-orders').addEventListener('click', (e) => run(e.target, $('#order-msg'), loadOrders));
$('#order-rows').addEventListener('click', (e) => {
  const id = e.target.dataset.openOrder;
  if (!id) return;
  run(e.target, $('#order-msg'), async () => {
    const s = requireCustomer();
    stopOrderPoll();
    const o = await api('order', 'GET', `/orders/${id}`, { token: s.token });
    setOrder(o);
    if (o.status === 'PLACED') pollOrder(o.id);
    await loadOrders();
    scrollTo('order-card');
  });
});

// ---------------------------------------------------------------------------
// Activity / API log
// ---------------------------------------------------------------------------

const MAX_LOG = 60;

function preview(entry) {
  const r = entry.response;
  if (r && typeof r === 'object' && !Array.isArray(r)) {
    if (r.code && r.message) return `${r.code}: ${r.message}`;
    if (r.token) return `token issued · role ${r.role}`;
    if (r.status) return `status ${r.status}${r.paymentStatus ? ` · payment ${r.paymentStatus}` : ''}`;
    if ('quantity' in r && 'reservedQuantity' in r) return `quantity ${r.quantity} · reserved ${r.reservedQuantity}`;
    if (r.name) return r.name;
  }
  if (Array.isArray(r)) return `${r.length} item${r.length === 1 ? '' : 's'}`;
  if (entry.status === 0) return 'no response';
  return '';
}

function pretty(v) {
  if (v === undefined || v === null) return '—';
  const text = typeof v === 'string' ? v : JSON.stringify(v, null, 2);
  // Hide passwords and shorten JWTs in the log.
  return text
    .replace(/("password":\s*")[^"]*"/g, '$1••••••"')
    .replace(/("token":\s*")([^"]{24})[^"]+"/g, '$1$2…"');
}

onApiCall((entry) => {
  const statusClass = entry.status >= 200 && entry.status < 300 ? 'ok' : entry.status >= 400 && entry.status < 500 ? 'warn' : 'bad';
  const li = document.createElement('li');
  li.innerHTML = `<details>
      <summary>
        <span class="time">${entry.time.toLocaleTimeString()}</span>
        <span class="svc svc-${entry.service}">${entry.service}</span>
        <span class="method m-${entry.method}">${entry.method}</span>
        <span class="path">${esc(entry.path)}</span>
        <span class="http ${statusClass}">${entry.status || 'ERR'}</span>
        <span class="ms">${entry.ms} ms</span>
        ${entry.auth ? '<span class="lock" title="Sent with Authorization: Bearer JWT">🔒</span>' : ''}
        ${entry.poll ? '<span class="poll-tag">poll</span>' : ''}
        <span class="preview">${esc(preview(entry))}</span>
      </summary>
      <div class="bodies">
        <div><h5>Request</h5><pre>${esc(pretty(entry.request))}</pre></div>
        <div><h5>Response</h5><pre>${esc(pretty(entry.response))}</pre></div>
      </div>
    </details>`;
  const list = $('#log-list');
  list.prepend(li);
  while (list.children.length > MAX_LOG) list.lastElementChild.remove();
});

$('#clear-log').addEventListener('click', () => {
  $('#log-list').innerHTML = '';
});

// ---------------------------------------------------------------------------
// Start-up
// ---------------------------------------------------------------------------

/** Reloads everything that belongs to the logged-in customer. */
function onCustomerChanged() {
  stopOrderPoll();
  setOrder(null);
  state.seenAlertIds = null;
  const msg = $('#order-msg');
  loadAddresses().catch((e) => showMsg(msg, 'error', describe(e)));
  loadOrders().catch((e) => showMsg(msg, 'error', describe(e)));
  loadRecHistory().catch((e) => showMsg($('#rec-msg'), 'error', describe(e)));
  loadAlerts().catch((e) => showMsg($('#alerts-msg'), 'error', describe(e)));
  startAlertPolling();
}

async function init() {
  loadSessions();
  renderSessions();
  setOrder(null);
  const msg = $('#catalog-msg');
  try {
    await loadCategories();
    await loadProducts();
  } catch (e) {
    showMsg(msg, 'error', describe(e));
    renderProductRows([]);
    renderProductSelects();
  }
  loadTrending().catch((e) => showMsg(msg, 'error', describe(e)));
  onCustomerChanged();
}

init();
