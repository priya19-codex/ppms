/**
 * PPMS - Paddy Procurement Management System
 * Full Frontend Application Controller & Router
 * Government of Tamil Nadu · TNCSC
 */

const App = document.getElementById('app');
const $ = (q) => document.querySelector(q);
const $$ = (q) => document.querySelectorAll(q);

// Session State
let S = {
  u: JSON.parse(sessionStorage.getItem('ppms_u') || 'null'),
  a: sessionStorage.getItem('ppms_a') || ''
};

// Utilities
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, c => '&#' + c.charCodeAt(0) + ';');
const formatTime = (t) => {
  if (!t) return '';
  const [h, m] = t.split(':');
  const H = +h;
  return `${H % 12 || 12}:${m} ${H < 12 ? 'AM' : 'PM'}`;
};
const formatDate = (d) => {
  if (!d) return '';
  return new Date(d + 'T00:00').toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
};
const isoToday = () => {
  const d = new Date();
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
};
const formDataObj = (f) => Object.fromEntries(new FormData(f));

// Status Styling & Badges
const STATUS_CONFIG = {
  CONFIRMED: ['🔵', 'Confirmed', 'b-blue'],
  ARRIVED: ['📍', 'Arrived', 'b-teal'],
  WAITING: ['⏳', 'Waiting in Queue', 'b-amber'],
  UNLOADING: ['🚜', 'Unloading Paddy', 'b-orange'],
  COMPLETED: ['✅', 'Procurement Completed', 'b-green'],
  CANCELLED: ['✖', 'Cancelled', 'b-red'],
  AVAILABLE: ['🟢', 'Available', 'b-green'],
  LIMITED: ['🟡', 'Limited Spaces', 'b-amber'],
  FULL: ['🔴', 'Slot Full', 'b-red'],
  CLOSED: ['⚪', 'Closed', 'b-grey'],
  OPEN: ['🟢', 'Open', 'b-green'],
  ACTIVE: ['🟢', 'Active', 'b-green'],
  INACTIVE: ['⚪', 'Inactive', 'b-grey'],
  PENDING: ['⏳', 'Pending Approval', 'b-amber'],
  APPROVED: ['✅', 'Approved', 'b-green'],
  REJECTED: ['✖', 'Rejected', 'b-red']
};

const badge = (st) => {
  const cfg = STATUS_CONFIG[st] || ['', st, 'b-grey'];
  return `<span class="badge ${cfg[2]}">${cfg[0]} ${cfg[1]}</span>`;
};

// Queue state transitions (strictly validated workflow)
const QUEUE_TRANSITIONS = {
  CONFIRMED: [['ARRIVED', 'Mark Arrived']],
  ARRIVED: [['WAITING', 'Move to Waiting'], ['UNLOADING', 'Start Unloading']],
  WAITING: [['UNLOADING', 'Start Unloading']],
  UNLOADING: [['COMPLETED', 'Complete Procurement']]
};

const ROLE_HOMES = {
  FARMER: 'dashboard',
  STAFF: 'staff',
  ADMIN: 'admin'
};

// API Fetch helper
async function api(path, opts = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (S.a) headers['Authorization'] = 'Basic ' + S.a;

  const res = await fetch('/api' + path, {
    method: opts.method || 'GET',
    headers,
    body: opts.body ? JSON.stringify(opts.body) : undefined
  });

  let data = null;
  try { data = await res.json(); } catch (_) {}

  if (!res.ok) {
    if (res.status === 401 && S.u && !opts.keepAuth) {
      logout(true);
    }
    throw new Error(data?.message || 'Something went wrong. Please try again.');
  }
  return data;
}

// Toast notification
function toast(msg, isBad = false) {
  const el = document.createElement('div');
  el.className = 'toast' + (isBad ? ' bad' : '');
  el.innerHTML = `${isBad ? '⚠️' : '✅'} <span>${esc(msg)}</span>`;
  document.body.appendChild(el);
  setTimeout(() => el.remove(), 4000);
}

// Auth helpers
function logout(quiet = false) {
  S = { u: null, a: '' };
  sessionStorage.removeItem('ppms_u');
  sessionStorage.removeItem('ppms_a');
  if (!quiet) {
    toast('Logged out successfully.');
    location.hash = '#/';
    route();
  }
}

async function loginWith(mobile, password) {
  S.a = btoa(mobile + ':' + password);
  try {
    S.u = await api('/auth/login', { method: 'POST', keepAuth: true });
    sessionStorage.setItem('ppms_a', S.a);
    sessionStorage.setItem('ppms_u', JSON.stringify(S.u));
    return S.u;
  } catch (err) {
    S = { u: null, a: '' };
    throw err;
  }
}

// Quick fill credentials helper
window.quickFill = (mobile, pw, role) => {
  const mInput = $('input[name="mobile"]');
  const pInput = $('input[name="password"]');
  if (mInput && pInput) {
    mInput.value = mobile;
    pInput.value = pw;
    const form = mInput.closest('form');
    if (form) form.requestSubmit();
  } else {
    // If on another page, navigate to appropriate login page and fill
    const path = role === 'ADMIN' ? 'admin-login' : (role === 'STAFF' ? 'staff-login' : 'login');
    location.hash = '#/' + path;
    setTimeout(() => {
      const mi = $('input[name="mobile"]');
      const pi = $('input[name="password"]');
      if (mi && pi) {
        mi.value = mobile;
        pi.value = pw;
        const f = mi.closest('form');
        if (f) f.requestSubmit();
      }
    }, 150);
  }
};

// Navigation Links
const NAV_LINKS = {
  FARMER: [
    ['dashboard', '📊 Dashboard'],
    ['centres', '📅 Book Slot'],
    ['bookings', '📋 My Bookings'],
    ['profile', '👤 Profile & Limits']
  ],
  STAFF: [
    ['staff', '📊 Dashboard'],
    ['staff/queue', "🚚 Today's Queue"],
    ['staff/slots', '⏰ Slot Management']
  ],
  ADMIN: [
    ['admin', '📊 Dashboard'],
    ['admin/farmers', '🌾 Farmers & Limits'],
    ['admin/centres', '🏢 Centres & Staff'],
    ['staff/slots', '⏰ Slots'],
    ['admin/bookings', '📋 All Bookings'],
    ['admin/reports', '📈 Reports']
  ]
};

// Layout Shell
function shell(contentHtml) {
  const u = S.u;
  let navItems = '';

  if (u) {
    const links = NAV_LINKS[u.role] || [];
    const hash = (location.hash.slice(2) || '').split('?')[0];
    navItems = links.map(([h, t]) => {
      const active = hash === h ? 'active' : '';
      return `<a href="#/${h}" class="${active}">${t}</a>`;
    }).join('');
  } else {
    navItems = `
      <a href="#/centres">🏢 Procurement Centres</a>
      <a href="#/login">🌾 Farmer Login</a>
      <a href="#/staff-login">🏢 Staff Login</a>
      <a href="#/admin-login">🛡️ Admin Login</a>
      <a href="#/register" class="btn sm gold">Register as Farmer</a>
    `;
  }

  return `
    <div class="gov-bar noprint">
      <span>🌾 தமிழ்நாடு அரசு | Government of Tamil Nadu · Food & Consumer Protection Department</span>
      <span>Toll-Free Helpline: 1800-425-4440 | TNCSC PPMS v1.0</span>
    </div>
    <header class="nav noprint">
      <a class="brand-group" href="#/">
        <div class="brand-icon">🌾</div>
        <div class="brand-text">
          <h1>PPMS</h1>
          <small>Paddy Procurement Management</small>
        </div>
      </a>
      <button class="burger" aria-label="Toggle navigation" onclick="document.querySelector('.menu').classList.toggle('open')">☰</button>
      <nav class="menu">
        ${navItems}
        ${u ? `
          <a href="#/notifications" class="bell" aria-label="Notifications" title="Notifications">
            🔔<span id="notif-badge" class="cnt" hidden>0</span>
          </a>
          <div class="user-badge">
            <span class="user-role-tag">${esc(u.role)}</span>
            <b>${esc(u.name.split(' ')[0])}</b>
          </div>
          <a href="#" class="btn tiny out" style="color:#ffffff;border-color:rgba(255,255,255,0.4)" onclick="logout();return false">Logout</a>
        ` : ''}
      </nav>
    </header>
    <main class="wrap">${contentHtml}</main>
    <footer class="noprint">
      <p><b>Paddy Procurement Management System (PPMS)</b> · Tamil Nadu Civil Supplies Corporation (TNCSC)</p>
      <p class="small muted">“Smart Booking. Less Waiting. Better Procurement.” · Designed to eliminate queues and empower paddy farmers.</p>
      <p class="small muted">Demo accounts available: Admin (9000000001), Staff Thanjavur (9000000002), Farmer (9876500001)</p>
    </footer>
  `;
}

// Update unread notification count
function updateNotificationBadge() {
  if (!S.u) return;
  api('/notifications/unread-count').then(res => {
    const el = $('#notif-badge');
    if (el) {
      if (res.count > 0) {
        el.textContent = res.count;
        el.hidden = false;
      } else {
        el.hidden = true;
      }
    }
  }).catch(() => {});
}

// -------------------------------------------------------------
// 1. LANDING PAGE
// -------------------------------------------------------------
function landingView() {
  return `
    <section class="hero">
      <div class="hero-tagline-badge">🌾 TAMIL NADU CIVIL SUPPLIES CORPORATION · TNCSC</div>
      <h1>Smart Paddy Procurement Management</h1>
      <p>“Book your procurement slot before you travel. Reduce waiting time, avoid unnecessary trips, and plan your paddy delivery efficiently.”</p>
      <div class="hero-cta-row">
        <a class="btn lg gold" href="#/${S.u ? (ROLE_HOMES[S.u.role] || 'centres') : 'centres'}">📅 Book a Slot Now</a>
        <a class="btn lg alt" href="#/login">🌾 Farmer Login</a>
        <a class="btn lg out" style="color:#fff;border-color:rgba(255,255,255,0.4)" href="#/staff-login">🏢 Staff Login</a>
        <a class="btn lg out" style="color:#fff;border-color:rgba(255,255,255,0.4)" href="#/admin-login">🛡️ Admin Login</a>
      </div>
    </section>

    <div class="demo-bar">
      <div>
        <p><b>⚡ Quick 1-Click Demo Login:</b> Click any account below to sign in instantly:</p>
      </div>
      <div class="demo-chips">
        <button class="demo-chip" onclick="quickFill('9000000001','Admin@123','ADMIN')">🛡️ Admin (9000000001)</button>
        <button class="demo-chip" onclick="quickFill('9000000002','Staff@123','STAFF')">🏢 Staff Thanjavur (9000000002)</button>
        <button class="demo-chip" onclick="quickFill('9000000003','Staff@123','STAFF')">🏢 Staff Tiruvarur (9000000003)</button>
        <button class="demo-chip" onclick="quickFill('9876500001','Farmer@123','FARMER')">🌾 Farmer Priya (9876500001)</button>
        <button class="demo-chip" onclick="quickFill('9876500002','Farmer@123','FARMER')">🌾 Farmer Ravi (9876500002)</button>
      </div>
    </div>

    <div class="section-title">
      <h2>How It Works</h2>
      <p class="muted">Simple 4-step digital token booking process for farmers</p>
    </div>

    <div class="steps-container">
      <div class="step-card">
        <div class="step-number">1</div>
        <h3>Register Account</h3>
        <p>Enter Farmer ID, land acres & Aadhaar. Provisional limit (20 bags/acre) auto-assigned.</p>
      </div>
      <span class="step-arrow">➜</span>
      <div class="step-card">
        <div class="step-number">2</div>
        <h3>Select Centre</h3>
        <p>Choose an open procurement centre in Thanjavur, Tiruvarur, or Nagapattinam.</p>
      </div>
      <span class="step-arrow">➜</span>
      <div class="step-card">
        <div class="step-number">3</div>
        <h3>Check Real-Time Slots</h3>
        <p>View 7-day live calendar with instant capacity (Green, Yellow, Red indicators).</p>
      </div>
      <span class="step-arrow">➜</span>
      <div class="step-card">
        <div class="step-number">4</div>
        <h3>Book & Get Token</h3>
        <p>Receive unique Booking ID, Token number & QR code. Arrive directly at your slot!</p>
      </div>
    </div>

    <div class="section-title">
      <h2>Key Features & Benefits</h2>
      <p class="muted">Solving agricultural queue bottlenecks with technology</p>
    </div>

    <div class="grid g3">
      <div class="feat-card">
        <div class="feat-icon">⏱️</div>
        <h3>Reduced Waiting Time</h3>
        <p>Eliminate 12-to-24 hour vehicle lines outside direct purchase centres by scheduling an exact delivery hour.</p>
      </div>
      <div class="feat-card">
        <div class="feat-icon">🚚</div>
        <h3>Planned Transportation</h3>
        <p>Hire tractors, lorries or mini-trucks only when your slot is confirmed, saving rental costs and vehicle idle time.</p>
      </div>
      <div class="feat-card">
        <div class="feat-icon">🎫</div>
        <h3>Digital Token & QR Code</h3>
        <p>Official digital token receipt generated instantly with Booking ID, token number and scannable QR verification.</p>
      </div>
      <div class="feat-card">
        <div class="feat-icon">📊</div>
        <h3>Real-Time Slot Availability</h3>
        <p>Live visibility into available spaces per hour with atomic server-side protection preventing overbooking.</p>
      </div>
      <div class="feat-card">
        <div class="feat-icon">🔔</div>
        <h3>Automatic Reminders & Alerts</h3>
        <p>Receive automatic reminders 1 day in advance and status alerts from arrival to weighing and payment completion.</p>
      </div>
      <div class="feat-card">
        <div class="feat-icon">🏢</div>
        <h3>Complete Queue Management</h3>
        <p>Staff dashboard with strict workflow transitions: CONFIRMED ➔ ARRIVED ➔ WAITING ➔ UNLOADING ➔ COMPLETED.</p>
      </div>
    </div>
  `;
}

// -------------------------------------------------------------
// 2. AUTHENTICATION PAGES (FARMER, STAFF, ADMIN)
// -------------------------------------------------------------
let CURRENT_LOGIN_ROLE = 'FARMER';

function loginView(role) {
  CURRENT_LOGIN_ROLE = role;
  const titles = {
    FARMER: '🌾 Farmer Login',
    STAFF: '🏢 Procurement Centre Staff Login',
    ADMIN: '🛡️ Administrator Login'
  };
  const demoAccounts = {
    FARMER: { mobile: '9876500001', pass: 'Farmer@123', name: 'Priya Selvam' },
    STAFF: { mobile: '9000000002', pass: 'Staff@123', name: 'Anitha (Thanjavur)' },
    ADMIN: { mobile: '9000000001', pass: 'Admin@123', name: 'System Admin' }
  };
  const demo = demoAccounts[role];

  return `
    <div class="card narrow">
      <div class="card-header">
        <h2>${titles[role]}</h2>
      </div>
      <p class="muted small">Please enter your registered mobile number and password to continue.</p>

      <form onsubmit="return handleLoginSubmit(event)">
        <label>
          Registered Mobile Number
          <input name="mobile" type="tel" inputmode="numeric" pattern="\\d{10}" maxlength="10" placeholder="10-digit mobile number" required autofocus>
        </label>
        <label>
          Password
          <input name="password" type="password" placeholder="Enter password" required>
        </label>
        <button class="btn lg block" style="margin-top:1rem" id="login-btn">Sign In</button>
      </form>

      <div class="card ok" style="margin-top:1.25rem;padding:0.9rem">
        <div style="display:flex;justify-content:space-between;align-items:center">
          <div>
            <b class="small">Demo Credentials:</b><br>
            <span class="small muted">Mobile: <b>${demo.mobile}</b> | Pass: <b>${demo.pass}</b> (${demo.name})</span>
          </div>
          <button class="btn tiny gold" onclick="quickFill('${demo.mobile}','${demo.pass}','${role}')">Fill & Login</button>
        </div>
      </div>

      ${role === 'FARMER' ? `
        <p class="c small" style="margin-top:1rem">
          Not registered yet? <a href="#/register"><b>Register as a New Farmer</b></a>
        </p>
      ` : ''}

      <div style="border-top:1px solid var(--line);margin-top:1rem;padding-top:0.75rem" class="c small muted">
        Switch portal: 
        <a href="#/login">Farmer</a> · 
        <a href="#/staff-login">Staff</a> · 
        <a href="#/admin-login">Admin</a>
      </div>
    </div>
  `;
}

window.handleLoginSubmit = async (ev) => {
  ev.preventDefault();
  const btn = $('#login-btn');
  if (btn) btn.disabled = true;

  const f = formDataObj(ev.target);
  try {
    const user = await loginWith(f.mobile, f.password);
    if (user.role !== CURRENT_LOGIN_ROLE) {
      logout(true);
      throw new Error(`This account has role '${user.role}'. Please use the ${user.role.toLowerCase()} login page.`);
    }
    toast(`Welcome back, ${user.name}!`);
    location.hash = '#/' + ROLE_HOMES[user.role];
  } catch (err) {
    toast(err.message, true);
    if (btn) btn.disabled = false;
  }
  return false;
};

// -------------------------------------------------------------
// 3. FARMER REGISTRATION
// -------------------------------------------------------------
async function registerView() {
  const centres = await api('/centres');
  return `
    <div class="card narrow wide">
      <div class="card-header">
        <h2>🌾 New Farmer Registration</h2>
      </div>
      <p class="muted small">Register to book paddy delivery slots at direct purchase centres. Aadhaar is encrypted and only the last 4 digits are retained.</p>

      <form onsubmit="return handleRegisterSubmit(event)" class="fgrid">
        <label>
          Full Name
          <input name="name" placeholder="e.g. Priyadharshini Selvam" required>
        </label>
        <label>
          Mobile Number
          <input name="mobile" type="tel" inputmode="numeric" pattern="\\d{10}" maxlength="10" placeholder="10-digit mobile number" required>
        </label>
        <label>
          Password (min 6 characters)
          <input name="password" type="password" minlength="6" placeholder="Create secure password" required>
        </label>
        <label>
          Farmer ID (Pattadhar / Uzhavar ID)
          <input name="farmerId" placeholder="e.g. TNF-2001" required>
        </label>
        <label>
          Aadhaar Number (12 digits)
          <input name="aadhaar" type="password" inputmode="numeric" pattern="\\d{12}" maxlength="12" placeholder="12-digit Aadhaar number" autocomplete="off" required>
          <small class="muted" style="display:block;margin-top:2px">Only last 4 digits (XXXX XXXX 1234) are stored.</small>
        </label>
        <label>
          Land Owned in Acres
          <input name="landAcres" id="land-input" type="number" step="0.1" min="0.1" placeholder="e.g. 5.0" oninput="calcProvisionalLimit()" required>
          <small class="muted" style="display:block;margin-top:2px">Provisional limit: 20 bags per acre.</small>
        </label>

        <div class="full card ok" style="padding:0.75rem;margin:0.25rem 0 0.75rem">
          <span id="limit-preview">Provisional limit: <b>0 bags</b> (Enter land acres to see calculated limit)</span>
        </div>

        <label class="full">
          Residential Address
          <input name="address" placeholder="Door No, Street Name" required>
        </label>
        <label>
          Village
          <input name="village" placeholder="e.g. Vallam" required>
        </label>
        <label>
          District
          <input name="district" placeholder="e.g. Thanjavur" required>
        </label>
        <label>
          State
          <input name="state" value="Tamil Nadu" required>
        </label>
        <label>
          Preferred Procurement Centre
          <select name="preferredCentreId" required>
            ${centres.map(c => `<option value="${c.id}">${esc(c.centreName)} (${esc(c.district)})</option>`).join('')}
          </select>
        </label>

        <div class="full" style="margin-top:1rem">
          <button class="btn lg block gold" id="reg-btn">Complete Farmer Registration</button>
        </div>
      </form>
      <p class="c small muted" style="margin-top:1rem">
        Already registered? <a href="#/login">Click here to Login</a>
      </p>
    </div>
  `;
}

window.calcProvisionalLimit = () => {
  const acres = +($('#land-input')?.value || 0);
  const preview = $('#limit-preview');
  if (preview) {
    const bags = Math.round(acres * 20);
    preview.innerHTML = `🌾 Provisional Paddy Limit: <b>${bags} bags</b> (${acres} acres × 20 bags/acre provisional limit)`;
  }
};

window.handleRegisterSubmit = async (ev) => {
  ev.preventDefault();
  const btn = $('#reg-btn');
  if (btn) btn.disabled = true;

  const f = formDataObj(ev.target);
  f.landAcres = +f.landAcres;
  f.preferredCentreId = +f.preferredCentreId;

  try {
    await api('/auth/register', { method: 'POST', body: f });
    toast('Registration successful! Logging you in...');
    await loginWith(f.mobile, f.password);
    location.hash = '#/dashboard';
  } catch (err) {
    toast(err.message, true);
    if (btn) btn.disabled = false;
  }
  return false;
};

// -------------------------------------------------------------
// 4. FARMER DASHBOARD
// -------------------------------------------------------------
async function farmerDashboardView() {
  const [myBookings, profile, centres] = await Promise.all([
    api('/bookings/my'),
    api('/farmers/me'),
    api('/centres')
  ]);

  const activeBookings = myBookings.filter(b => ['CONFIRMED', 'ARRIVED', 'WAITING', 'UNLOADING'].includes(b.status));
  const completedBookings = myBookings.filter(b => b.status === 'COMPLETED');
  const cancelledBookings = myBookings.filter(b => b.status === 'CANCELLED');
  const nextBooking = activeBookings[0] || null;

  const remainingQty = Math.max(0, profile.approvedMaxQty - profile.usedQty);
  const usedPercent = profile.approvedMaxQty > 0 ? Math.min(100, Math.round((profile.usedQty / profile.approvedMaxQty) * 100)) : 0;

  return `
    <div style="display:flex;justify-content:space-between;align-items:flex-start;flex-wrap:wrap;gap:1rem;margin-bottom:1.5rem">
      <div>
        <h1 style="margin:0">🌾 Welcome, ${esc(profile.name)}</h1>
        <p class="muted">Farmer ID: <b>${esc(profile.farmerId)}</b> · ${esc(profile.village)}, ${esc(profile.district)} · Land: <b>${profile.landAcres} acres</b></p>
      </div>
      <div>
        <a class="btn gold" href="#/centres">📅 Book a Slot</a>
      </div>
    </div>

    <!-- Stats Grid -->
    <div class="grid g4">
      <div class="stat">
        <small>Approved Paddy Limit</small>
        <b>${profile.approvedMaxQty} <span class="small muted">bags</span></b>
        <div class="stat-hint">Calculated @ 20 bags/acre (${profile.landAcres} ac)</div>
      </div>
      <div class="stat gold-top">
        <small>Used / Booked Quantity</small>
        <b>${profile.usedQty} <span class="small muted">bags</span></b>
        <div class="stat-hint">${usedPercent}% of approved quota booked</div>
      </div>
      <div class="stat teal-top">
        <small>Remaining Available</small>
        <b>${remainingQty} <span class="small muted">bags</span></b>
        <div class="stat-hint">Can be booked for new slots</div>
      </div>
      <div class="stat blue-top">
        <small>Total Bookings</small>
        <b>${myBookings.length}</b>
        <div class="stat-hint">Active: ${activeBookings.length} | Done: ${completedBookings.length}</div>
      </div>
    </div>

    <!-- Active Booking Spotlight -->
    <div class="card" style="margin-top:1.5rem">
      <div class="card-header">
        <h3>🚚 Current / Next Procurement Slot</h3>
        ${nextBooking ? badge(nextBooking.status) : '<span class="badge b-grey">No Active Booking</span>'}
      </div>

      ${nextBooking ? `
        <div class="grid g2" style="align-items:center">
          <div>
            <div style="display:flex;align-items:baseline;gap:0.75rem">
              <span class="small muted">TOKEN NUMBER</span>
              <span style="font-size:2.2rem;font-weight:800;color:var(--g9)">${esc(nextBooking.token)}</span>
              <span class="small muted">(${esc(nextBooking.bookingId)})</span>
            </div>
            <p>
              🏢 <b>${esc(nextBooking.centre)}</b><br>
              📅 <b>${formatDate(nextBooking.date)}</b> · ⏰ <b>${formatTime(nextBooking.start)} – ${formatTime(nextBooking.end)}</b><br>
              🚜 Vehicle: <b>${esc(nextBooking.vehicle)}</b> (${esc(nextBooking.vehicleType)})<br>
              🌾 Quantity: <b>${nextBooking.quantity} bags</b> of <b>${esc(nextBooking.paddyType)}</b>
            </p>
            <div class="row">
              <a class="btn sm" href="#/token/${nextBooking.bookingId}">🎫 View Token & QR</a>
              ${nextBooking.status === 'CONFIRMED' ? `
                <button class="btn sm red" onclick="cancelBookingPrompt('${nextBooking.bookingId}')">✖ Cancel Booking</button>
              ` : ''}
            </div>
          </div>
          <div>
            <p class="small muted bold">PROCUREMENT WORKFLOW STATUS</p>
            ${renderWorkflowTracker(nextBooking.status)}
            <p class="small muted c">Please reach the centre 15 minutes before your slot.</p>
          </div>
        </div>
      ` : `
        <div class="c muted" style="padding:1.5rem">
          <p>You have no active procurement booking scheduled.</p>
          <a class="btn gold" href="#/centres">Book Your Next Slot Now</a>
        </div>
      `}
    </div>

    <!-- Quick Actions -->
    <div class="section-title" style="text-align:left;margin:1.5rem 0 0.75rem">
      <h3>Quick Actions</h3>
    </div>
    <div class="grid g4">
      <a class="card feat-card" href="#/centres">
        <div class="feat-icon">📅</div>
        <h3>Book Slot</h3>
        <p>Check available dates & hours at centres</p>
      </a>
      <a class="card feat-card" href="#/bookings">
        <div class="feat-icon">📋</div>
        <h3>My Bookings</h3>
        <p>View previous tokens, receipts & history</p>
      </a>
      <a class="card feat-card" href="#/notifications">
        <div class="feat-icon">🔔</div>
        <h3>Notifications</h3>
        <p>Queue updates, reminders & approval alerts</p>
      </a>
      <a class="card feat-card" href="#/profile">
        <div class="feat-icon">👤</div>
        <h3>Profile & Limits</h3>
        <p>View land records or request extra quantity</p>
      </a>
    </div>
  `;
}

// Workflow tracker helper
function renderWorkflowTracker(currentStatus) {
  const steps = ['CONFIRMED', 'ARRIVED', 'WAITING', 'UNLOADING', 'COMPLETED'];
  const currentIndex = steps.indexOf(currentStatus);

  return `
    <div class="workflow-tracker">
      ${steps.map((st, i) => {
        const isDone = currentIndex > i;
        const isActive = currentIndex === i;
        const cls = isDone ? 'done' : (isActive ? 'active' : '');
        const icon = isDone ? '✓' : (i + 1);
        return `
          <div class="wf-step ${cls}">
            <div class="wf-bullet">${icon}</div>
            <div class="wf-label">${st}</div>
          </div>
        `;
      }).join('')}
    </div>
  `;
}

// -------------------------------------------------------------
// 5. PROCUREMENT CENTRES PAGE
// -------------------------------------------------------------
let ALL_CENTRES = [];

async function centresView() {
  ALL_CENTRES = await api('/centres');
  window._filterCentres = () => {
    const q = ($('#centre-search')?.value || '').toLowerCase().trim();
    const openOnly = $('#open-only-toggle')?.checked || false;

    const filtered = ALL_CENTRES.filter(c => {
      const matchSearch = (c.centreName + ' ' + c.village + ' ' + c.district + ' ' + c.address).toLowerCase().includes(q);
      const matchOpen = openOnly ? c.status === 'OPEN' : true;
      return matchSearch && matchOpen;
    });

    const el = $('#centres-list');
    if (!el) return;

    if (!filtered.length) {
      el.innerHTML = '<p class="muted c full" style="padding:2rem">No procurement centres found matching your search.</p>';
      return;
    }

    el.innerHTML = filtered.map(c => `
      <div class="card">
        <div class="card-header" style="margin-bottom:0.5rem;padding-bottom:0.5rem">
          <div>
            <h3 style="margin:0">${esc(c.centreName)}</h3>
            <small class="muted">Code: <b>${esc(c.centreCode)}</b></small>
          </div>
          ${badge(c.status)}
        </div>
        <p class="small muted">
          📍 ${esc(c.address)}, ${esc(c.village)}, ${esc(c.district)}<br>
          📞 Contact: <b>${esc(c.contactNumber || '04362-200100')}</b><br>
          🕗 Operating Hours: <b>${formatTime(c.openTime)} – ${formatTime(c.closeTime)}</b>
        </p>
        <div style="background:#f8fafc;border:1px solid var(--line);border-radius:8px;padding:0.6rem;margin-bottom:0.8rem;display:flex;justify-content:space-around;text-align:center">
          <div>
            <small class="muted" style="display:block">Slots Left Today</small>
            <b style="color:var(--g7);font-size:1.15rem">${c.availableToday}</b>
          </div>
          <div style="border-left:1px solid var(--line);padding-left:1rem">
            <small class="muted" style="display:block">Vehicles in Queue</small>
            <b style="color:var(--gold);font-size:1.15rem">${c.queue}</b>
          </div>
        </div>
        ${c.status === 'OPEN' ? `
          <a class="btn block" href="#/slots/${c.id}">📅 Check Slots & Book</a>
        ` : `
          <button class="btn block" disabled title="Centre is currently closed">⚪ Centre Closed</button>
        `}
      </div>
    `).join('');
  };

  window._afterRender = window._filterCentres;

  return `
    <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:1rem;margin-bottom:1rem">
      <div>
        <h1 style="margin:0">🏢 Direct Purchase Procurement Centres</h1>
        <p class="muted">Select an open procurement centre to view 7-day slot availability.</p>
      </div>
      <div>
        <label style="display:inline-flex;align-items:center;gap:0.5rem;cursor:pointer;font-weight:normal">
          <input type="checkbox" id="open-only-toggle" onchange="_filterCentres()" style="width:auto;margin:0">
          <b>Show Open Centres Only</b>
        </label>
      </div>
    </div>

    <div class="card" style="padding:0.75rem 1rem">
      <input id="centre-search" type="search" placeholder="🔍 Search by centre name, village, or district (e.g. Thanjavur, Vallam)..." oninput="_filterCentres()">
    </div>

    <div id="centres-list" class="grid g3"></div>
  `;
}

// -------------------------------------------------------------
// 6. SLOT AVAILABILITY (7-DAY CALENDAR)
// -------------------------------------------------------------
let SELECTED_SLOT_DATE = isoToday();
let ACTIVE_CENTRE_ID = null;

async function slotsPageView(centreId) {
  ACTIVE_CENTRE_ID = centreId;
  const centre = await api('/centres/' + centreId);
  SELECTED_SLOT_DATE = isoToday();

  // Next 7 days array
  const days = [...Array(7)].map((_, i) => {
    const d = new Date();
    d.setDate(d.getDate() + i);
    return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
  });

  window.loadSlotsForDate = async (dStr) => {
    SELECTED_SLOT_DATE = dStr;
    $$('.date-pill').forEach(b => b.classList.toggle('on', b.dataset.date === dStr));

    const container = $('#slots-grid');
    if (!container) return;

    try {
      container.innerHTML = '<p class="muted c full" style="padding:2rem">⏳ Loading available time slots...</p>';
      const slots = await api(`/slots?centreId=${centreId}&date=${dStr}`);

      if (!slots || !slots.length) {
        container.innerHTML = '<p class="muted c full" style="padding:2rem">No slots have been scheduled for this date yet.</p>';
        return;
      }

      container.innerHTML = slots.map(s => {
        const isClickable = ['AVAILABLE', 'LIMITED'].includes(s.state);
        const clickAttr = isClickable ? `role="button" tabindex="0" onclick="location.hash='#/book/${s.id}'"` : '';

        return `
          <div class="slot s-${s.state}" ${clickAttr}>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <b>${formatTime(s.startTime)} – ${formatTime(s.endTime)}</b>
              ${badge(s.state)}
            </div>
            <div style="font-size:0.88rem;color:#334155">
              ${s.state === 'FULL' ? '<b>Slot is already Full</b> (Capacity reached)' :
                (s.state === 'CLOSED' ? 'Slot Unavailable / Passed' :
                `Available: <b>${s.available}</b> of <b>${s.capacity}</b> spaces`)}
            </div>
            ${isClickable ? `
              <div style="margin-top:0.35rem">
                <span class="btn tiny gold" style="display:inline-block">Select & Book ➔</span>
              </div>
            ` : `
              <small class="muted">${s.state === 'FULL' ? 'Please choose another slot' : 'Not bookable'}</small>
            `}
          </div>
        `;
      }).join('');
    } catch (err) {
      container.innerHTML = `<p class="muted c full">${esc(err.message)}</p>`;
    }
  };

  // Refresh every 15s to maintain live capacity visibility
  window._afterRender = () => {
    loadSlotsForDate(SELECTED_SLOT_DATE);
    window._slotsPoll = setInterval(() => {
      if ($('#slots-grid')) loadSlotsForDate(SELECTED_SLOT_DATE);
    }, 15000);
  };

  return `
    <div style="margin-bottom:1rem">
      <a href="#/centres">← Back to All Procurement Centres</a>
    </div>

    <div class="card">
      <div class="card-header">
        <div>
          <h2 style="margin:0">${esc(centre.centreName)}</h2>
          <span class="small muted">Code: ${esc(centre.centreCode)} · 📍 ${esc(centre.address)}, ${esc(centre.village)}, ${esc(centre.district)}</span>
        </div>
        ${badge(centre.status)}
      </div>
      <p class="small muted" style="margin:0">
        🕗 Hours: <b>${formatTime(centre.openTime)} – ${formatTime(centre.closeTime)}</b> · Daily Capacity: <b>${centre.maxDailyCapacity} vehicles</b> · Contact: <b>${esc(centre.contactNumber)}</b>
      </p>
    </div>

    <div class="section-title" style="text-align:left;margin-bottom:0.75rem">
      <h3>Select Date (Next 7 Days)</h3>
    </div>

    <div class="dates">
      ${days.map((dStr, idx) => {
        const dObj = new Date(dStr + 'T00:00');
        const weekday = idx === 0 ? 'Today' : (idx === 1 ? 'Tomorrow' : dObj.toLocaleDateString('en-IN', { weekday: 'short' }));
        const dayNum = dStr.slice(8);
        const month = dObj.toLocaleDateString('en-IN', { month: 'short' });
        const isSelected = dStr === SELECTED_SLOT_DATE ? 'on' : '';

        return `
          <button class="date-pill ${isSelected}" data-date="${dStr}" onclick="loadSlotsForDate('${dStr}')">
            <small>${weekday}</small>
            <b>${dayNum}</b>
            <small>${month}</small>
          </button>
        `;
      }).join('')}
    </div>

    <div class="legend">
      <small class="muted bold">SLOT STATUS:</small>
      <span class="badge b-green">🟢 AVAILABLE (Bookable)</span>
      <span class="badge b-amber">🟡 LIMITED (Few Left)</span>
      <span class="badge b-red">🔴 FULL (Closed)</span>
      <span class="badge b-grey">⚪ CLOSED (Past / Unavailable)</span>
      <button class="btn tiny out" style="margin-left:auto" onclick="loadSlotsForDate(SELECTED_SLOT_DATE)">🔄 Refresh Live Slots</button>
    </div>

    <div id="slots-grid" class="grid g4"></div>
  `;
}

// -------------------------------------------------------------
// 7. BOOKING PROCESS (FORM -> REVIEW -> TOKEN)
// -------------------------------------------------------------
let CURRENT_BOOK_DATA = {};

async function bookingFormView(slotId) {
  const [slot, farmer] = await Promise.all([
    api('/slots/' + slotId),
    api('/farmers/me')
  ]);

  const remainingQuota = Math.max(0, farmer.approvedMaxQty - farmer.usedQty);
  CURRENT_BOOK_DATA = { slot, farmer, remainingQuota };

  return `
    <div style="margin-bottom:1rem">
      <a href="#/slots/${slot.centre.id}">← Back to Slot Selection</a>
    </div>

    <div class="card narrow wide">
      <div class="card-header">
        <h2>🌾 Book Paddy Procurement Slot</h2>
      </div>

      <div class="card ok" style="padding:0.75rem 1rem;margin-bottom:1.25rem">
        <p style="margin:0" class="small">
          <b>Farmer:</b> ${esc(farmer.name)} (${esc(farmer.farmerId)}) · 
          <b>Approved Limit:</b> ${farmer.approvedMaxQty} bags · 
          <b>Already Booked:</b> ${farmer.usedQty} bags · 
          <b>Remaining Quota:</b> <b style="color:var(--g7)">${remainingQuota} bags</b>
        </p>
      </div>

      <form id="booking-form" class="fgrid" onsubmit="return handleBookingReview(event)">
        <!-- Farmer Info -->
        <label>
          Farmer Name
          <input value="${esc(farmer.name)}" readonly>
        </label>
        <label>
          Farmer ID
          <input value="${esc(farmer.farmerId)}" readonly>
        </label>

        <!-- Procurement Info -->
        <label>
          Procurement Centre
          <input value="${esc(slot.centre.centreName)}" readonly>
        </label>
        <label>
          Date & Time Slot
          <input value="${formatDate(slot.slotDate)} | ${formatTime(slot.startTime)} – ${formatTime(slot.endTime)}" readonly>
        </label>

        <!-- Vehicle Details -->
        <label>
          Vehicle Number
          <input name="vehicleNumber" placeholder="e.g. TN 68 AB 1234" required style="text-transform:uppercase">
        </label>
        <label>
          Vehicle Type
          <select name="vehicleType" required>
            <option value="Tractor">Tractor</option>
            <option value="Mini Truck">Mini Truck</option>
            <option value="Lorry">Lorry</option>
            <option value="Tempo">Tempo</option>
            <option value="Bullock Cart">Bullock Cart</option>
          </select>
        </label>

        <!-- Paddy Details -->
        <label>
          Expected Quantity (in Bags)
          <input name="quantity" type="number" min="1" max="${remainingQuota}" placeholder="Max: ${remainingQuota} bags" required>
          <small class="muted" style="display:block;margin-top:2px">Validated against remaining limit (${remainingQuota} bags max).</small>
        </label>
        <label>
          Paddy Variety
          <select name="paddyType" required>
            <option value="Ponni">Ponni</option>
            <option value="ADT-45">ADT-45</option>
            <option value="CO-51">CO-51</option>
            <option value="IR-20">IR-20</option>
            <option value="CR-1009">CR-1009</option>
            <option value="Other">Other / Traditional</option>
          </select>
        </label>

        <label class="full">
          Contact Mobile Number
          <input name="contactNumber" value="${esc(farmer.mobile)}" type="tel" pattern="\\d{10}" maxlength="10" required>
        </label>

        <div class="full" style="margin-top:1rem">
          <button class="btn lg block gold" id="review-btn">Proceed to Review Booking</button>
        </div>
      </form>

      <!-- Review Section Placeholder -->
      <div id="booking-review-container"></div>
    </div>
  `;
}

window.handleBookingReview = (ev) => {
  ev.preventDefault();
  const f = formDataObj(ev.target);
  const { slot, farmer, remainingQuota } = CURRENT_BOOK_DATA;

  const qty = +f.quantity;
  if (qty > remainingQuota) {
    toast(`Quantity exceeds your remaining limit of ${remainingQuota} bags.`, true);
    return false;
  }

  CURRENT_BOOK_DATA.formValues = f;

  const reviewBox = $('#booking-review-container');
  if (reviewBox) {
    reviewBox.innerHTML = `
      <div class="card ok" style="margin-top:1.5rem;border-width:2px">
        <h3>📋 Review Booking Details</h3>
        <p>Please double-check all information before final submission:</p>

        <dl style="display:grid;grid-template-columns:140px 1fr;gap:0.4rem 1rem;font-size:0.92rem;margin:1rem 0">
          <dt class="muted">Farmer Name:</dt><dd><b>${esc(farmer.name)}</b> (${esc(farmer.farmerId)})</dd>
          <dt class="muted">Procurement Centre:</dt><dd><b>${esc(slot.centre.centreName)}</b></dd>
          <dt class="muted">Date & Time:</dt><dd><b>${formatDate(slot.slotDate)}</b> (${formatTime(slot.startTime)} – ${formatTime(slot.endTime)})</dd>
          <dt class="muted">Vehicle:</dt><dd><b>${esc(f.vehicleNumber.toUpperCase())}</b> (${esc(f.vehicleType)})</dd>
          <dt class="muted">Paddy Variety:</dt><dd><b>${esc(f.paddyType)}</b></dd>
          <dt class="muted">Quantity:</dt><dd><b>${f.quantity} bags</b></dd>
          <dt class="muted">Contact Number:</dt><dd><b>${esc(f.contactNumber)}</b></dd>
        </dl>

        <div class="row" style="margin-top:1rem">
          <button class="btn lg gold" id="confirm-book-btn" onclick="executeBookingConfirmation()">Confirm & Book Slot</button>
          <button class="btn out" onclick="$('#booking-review-container').innerHTML=''">Edit Details</button>
        </div>
      </div>
    `;
    reviewBox.scrollIntoView({ behavior: 'smooth' });
  }
  return false;
};

window.executeBookingConfirmation = async () => {
  const btn = $('#confirm-book-btn');
  if (btn) btn.disabled = true;

  const { slot, formValues } = CURRENT_BOOK_DATA;

  try {
    const res = await api('/bookings', {
      method: 'POST',
      body: {
        slotId: slot.id,
        vehicleNumber: formValues.vehicleNumber,
        vehicleType: formValues.vehicleType,
        paddyType: formValues.paddyType,
        quantity: +formValues.quantity,
        contactNumber: formValues.contactNumber
      }
    });

    toast('Booking confirmed successfully!');
    location.hash = '#/token/' + res.bookingId + '?new=1';
  } catch (err) {
    toast(err.message, true);
    if (btn) btn.disabled = false;
  }
};

// -------------------------------------------------------------
// 8. DIGITAL BOOKING TOKEN CARD & QR CODE
// -------------------------------------------------------------
let CURRENT_TOKEN_DATA = null;

async function tokenReceiptView(bookingId) {
  CURRENT_TOKEN_DATA = await api('/bookings/' + bookingId);
  const b = CURRENT_TOKEN_DATA;
  const isNewlyBooked = location.hash.includes('?new=1');

  window._afterRender = () => {
    const qrContainer = $('#qr');
    if (qrContainer && window.QRCode) {
      qrContainer.innerHTML = '';
      const qrPayload = JSON.stringify({
        id: b.bookingId,
        token: b.token,
        farmer: b.farmerId,
        centre: b.centre,
        date: b.date,
        slot: `${b.start}-${b.end}`,
        qty: b.quantity
      });
      new QRCode(qrContainer, {
        text: qrPayload,
        width: 140,
        height: 140,
        colorDark: '#064e3b',
        colorLight: '#ffffff',
        correctLevel: QRCode.CorrectLevel.M
      });
    }
  };

  return `
    ${isNewlyBooked ? `
      <div class="card ok c noprint" style="max-width:500px;margin:1rem auto">
        <h3 style="color:var(--g7);margin-bottom:0.25rem">✅ Booking Confirmed Successfully!</h3>
        <p class="small muted" style="margin:0">Your token and QR code have been issued. Please reach the centre 15 minutes before your slot.</p>
      </div>
    ` : ''}

    <div class="token" id="printable-token">
      <div class="th">
        <small>GOVERNMENT OF TAMIL NADU · TNCSC</small>
        <h2>PADDY PROCUREMENT MANAGEMENT SYSTEM</h2>
        <small>OFFICIAL DIGITAL E-TOKEN</small>
      </div>

      <div class="tn">
        <small>DAILY TOKEN NUMBER</small>
        <b>${esc(b.token)}</b>
      </div>

      <div class="tr">
        <small>UNIQUE BOOKING ID</small><br>
        <b>${esc(b.bookingId)}</b>
      </div>

      <dl>
        <dt>Farmer Name</dt><dd>${esc(b.farmer)}</dd>
        <dt>Farmer ID</dt><dd>${esc(b.farmerId)}</dd>
        <dt>Procurement Centre</dt><dd>${esc(b.centre)}</dd>
        <dt>Slot Date</dt><dd>${formatDate(b.date)}</dd>
        <dt>Delivery Time</dt><dd>${formatTime(b.start)} – ${formatTime(b.end)}</dd>
        <dt>Vehicle No.</dt><dd>${esc(b.vehicle)} (${esc(b.vehicleType)})</dd>
        <dt>Paddy Variety</dt><dd>${esc(b.paddyType)}</dd>
        <dt>Quantity</dt><dd><b>${b.quantity} bags</b></dd>
        <dt>Contact Mobile</dt><dd>${esc(b.contact)}</dd>
        <dt>Current Status</dt><dd>${badge(b.status)}</dd>
      </dl>

      <div id="qr"></div>

      <div class="token-footer">
        ⚠️ Please present this digital token or QR code upon arrival at the procurement centre.
      </div>
    </div>

    <div class="row noprint" style="justify-content:center;margin-top:1.5rem">
      <button class="btn gold" onclick="downloadTokenImage()">📥 Download Token Card (PNG)</button>
      <button class="btn alt" onclick="window.print()">🖨️ Print / Save PDF</button>
      <a class="btn out" href="#/bookings">📋 View My Bookings</a>
      <a class="btn out" href="#/dashboard">🏠 Back to Dashboard</a>
    </div>
  `;
}

// Download Token Card as High-Res PNG
window.downloadTokenImage = () => {
  const b = CURRENT_TOKEN_DATA;
  if (!b) return;

  const canvas = document.createElement('canvas');
  canvas.width = 460;
  canvas.height = 660;
  const ctx = canvas.getContext('2d');

  // Background
  ctx.fillStyle = '#ffffff';
  ctx.fillRect(0, 0, 460, 660);

  // Header Banner
  ctx.fillStyle = '#064e3b';
  ctx.fillRect(0, 0, 460, 85);
  ctx.fillStyle = '#fef08a';
  ctx.font = 'bold 12px Inter, sans-serif';
  ctx.textAlign = 'center';
  ctx.fillText('GOVERNMENT OF TAMIL NADU · TNCSC', 230, 28);
  ctx.fillStyle = '#ffffff';
  ctx.font = 'bold 16px Inter, sans-serif';
  ctx.fillText('PADDY PROCUREMENT MANAGEMENT SYSTEM', 230, 52);
  ctx.font = '11px Inter, sans-serif';
  ctx.fillText('OFFICIAL DIGITAL E-TOKEN RECEIPT', 230, 72);

  // Token Number
  ctx.fillStyle = '#64748b';
  ctx.font = 'bold 12px Inter, sans-serif';
  ctx.fillText('DAILY TOKEN NUMBER', 230, 120);
  ctx.fillStyle = '#064e3b';
  ctx.font = 'bold 54px Inter, sans-serif';
  ctx.fillText(b.token, 230, 175);

  // Booking ID
  ctx.fillStyle = '#f8fafc';
  ctx.fillRect(40, 195, 380, 36);
  ctx.fillStyle = '#0f172a';
  ctx.font = 'bold 15px monospace';
  ctx.fillText('Booking ID: ' + b.bookingId, 230, 218);

  // Detail lines
  ctx.textAlign = 'left';
  ctx.font = '13px Inter, sans-serif';
  const rows = [
    ['Farmer Name', b.farmer + ' (' + b.farmerId + ')'],
    ['Procurement Centre', b.centre],
    ['Date & Time Slot', formatDate(b.date) + ' | ' + formatTime(b.start) + ' – ' + formatTime(b.end)],
    ['Vehicle Details', b.vehicle + ' (' + b.vehicleType + ')'],
    ['Paddy Details', b.quantity + ' bags (' + b.paddyType + ')'],
    ['Contact Mobile', b.contact],
    ['Current Status', b.status]
  ];

  rows.forEach(([k, v], idx) => {
    ctx.fillStyle = '#64748b';
    ctx.fillText(k + ':', 40, 260 + idx * 26);
    ctx.fillStyle = '#0f172a';
    ctx.font = 'bold 13px Inter, sans-serif';
    ctx.fillText(v, 180, 260 + idx * 26);
    ctx.font = '13px Inter, sans-serif';
  });

  // QR Code draw
  const qrCanvas = $('#qr canvas');
  if (qrCanvas) {
    ctx.drawImage(qrCanvas, 165, 460, 130, 130);
  }

  // Footer Note
  ctx.fillStyle = '#f0fdf4';
  ctx.fillRect(0, 615, 460, 45);
  ctx.fillStyle = '#15803d';
  ctx.textAlign = 'center';
  ctx.font = 'bold 11px Inter, sans-serif';
  ctx.fillText('Please reach the procurement centre 15 minutes before your slot.', 230, 642);

  // Trigger download
  const link = document.createElement('a');
  link.download = `PPMS-Token-${b.token}-${b.bookingId}.png`;
  link.href = canvas.toDataURL('image/png');
  link.click();
};

// -------------------------------------------------------------
// 9. MY BOOKINGS PAGE
// -------------------------------------------------------------
let MY_BOOKINGS_CACHE = [];
let CURRENT_BOOKINGS_FILTER = 'all';

async function myBookingsView() {
  MY_BOOKINGS_CACHE = await api('/bookings/my');

  window.filterMyBookings = (tab) => {
    CURRENT_BOOKINGS_FILTER = tab;
    $$('.tab-btn').forEach(b => b.classList.toggle('active', b.dataset.tab === tab));

    const container = $('#my-bookings-list');
    if (!container) return;

    let list = MY_BOOKINGS_CACHE;
    if (tab === 'active') {
      list = list.filter(b => ['CONFIRMED', 'ARRIVED', 'WAITING', 'UNLOADING'].includes(b.status));
    } else if (tab !== 'all') {
      list = list.filter(b => b.status === tab.toUpperCase());
    }

    if (!list.length) {
      container.innerHTML = '<p class="muted c" style="padding:2rem">No bookings found in this view.</p>';
      return;
    }

    container.innerHTML = `
      <div class="tbl">
        <table>
          <thead>
            <tr>
              <th>Token</th>
              <th>Booking ID</th>
              <th>Centre</th>
              <th>Date & Time</th>
              <th>Vehicle</th>
              <th>Quantity</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            ${list.map(b => `
              <tr>
                <td><b style="color:var(--g9);font-size:1.1rem">${esc(b.token)}</b></td>
                <td><span style="font-family:monospace">${esc(b.bookingId)}</span></td>
                <td>${esc(b.centre)}</td>
                <td><b>${formatDate(b.date)}</b><br><small class="muted">${formatTime(b.start)} – ${formatTime(b.end)}</small></td>
                <td>${esc(b.vehicle)}<br><small class="muted">${esc(b.vehicleType)}</small></td>
                <td><b>${b.quantity} bags</b><br><small class="muted">${esc(b.paddyType)}</small></td>
                <td>${badge(b.status)}</td>
                <td>
                  <a class="btn tiny" href="#/token/${b.bookingId}">🎫 Token</a>
                  ${b.status === 'CONFIRMED' ? `
                    <button class="btn tiny red" onclick="cancelBookingPrompt('${b.bookingId}')">✖ Cancel</button>
                  ` : ''}
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
    `;
  };

  window._afterRender = () => filterMyBookings(CURRENT_BOOKINGS_FILTER);

  return `
    <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:1rem;margin-bottom:1.5rem">
      <div>
        <h1 style="margin:0">📋 My Procurement Bookings</h1>
        <p class="muted">View history, active tokens, and manage eligible cancellations.</p>
      </div>
      <div>
        <a class="btn gold" href="#/centres">📅 Book New Slot</a>
      </div>
    </div>

    <div class="tabs">
      <button class="tab-btn active" data-tab="all" onclick="filterMyBookings('all')">All Bookings (${MY_BOOKINGS_CACHE.length})</button>
      <button class="tab-btn" data-tab="active" onclick="filterMyBookings('active')">Active / In Progress</button>
      <button class="tab-btn" data-tab="confirmed" onclick="filterMyBookings('confirmed')">Confirmed</button>
      <button class="tab-btn" data-tab="completed" onclick="filterMyBookings('completed')">Completed</button>
      <button class="tab-btn" data-tab="cancelled" onclick="filterMyBookings('cancelled')">Cancelled</button>
    </div>

    <div id="my-bookings-list" class="card" style="padding:0"></div>
  `;
}

window.cancelBookingPrompt = async (bookingId) => {
  if (!confirm(`Are you sure you want to cancel booking ${bookingId}?\n\nNote: Cancellations are allowed up to 2 hours before the slot. Your paddy limit quota will be restored immediately.`)) {
    return;
  }

  try {
    await api(`/bookings/${bookingId}/cancel`, { method: 'POST' });
    toast('Booking cancelled successfully.');
    route();
  } catch (err) {
    toast(err.message, true);
  }
};

// -------------------------------------------------------------
// 10. NOTIFICATION CENTRE
// -------------------------------------------------------------
async function notificationsView() {
  const notifs = await api('/notifications');

  // Mark all read
  api('/notifications/read-all', { method: 'PUT' }).then(() => {
    const b = $('#notif-badge');
    if (b) b.hidden = true;
  }).catch(() => {});

  return `
    <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:1rem;margin-bottom:1.5rem">
      <div>
        <h1 style="margin:0">🔔 Notification Centre</h1>
        <p class="muted">Real-time status updates, reminders, and limit notifications.</p>
      </div>
      <div>
        <button class="btn sm out" onclick="route()">🔄 Refresh</button>
      </div>
    </div>

    <div class="card" style="padding:1rem">
      ${notifs.length ? notifs.map(n => `
        <div class="note ${n.read ? '' : 'new'}">
          <div style="display:flex;justify-content:space-between;align-items:baseline">
            <b>${esc(n.title)}</b>
            <small>${new Date(n.createdAt).toLocaleString('en-IN')}</small>
          </div>
          <p>${esc(n.message)}</p>
        </div>
      `).join('') : `
        <p class="muted c" style="padding:2rem">You have no notifications yet.</p>
      `}
    </div>
  `;
}

// -------------------------------------------------------------
// 11. FARMER PROFILE & EXTRA QUANTITY REQUESTS
// -------------------------------------------------------------
async function farmerProfileView() {
  const [profile, centres, requests] = await Promise.all([
    api('/farmers/me'),
    api('/centres'),
    api('/farmers/me/quantity-requests')
  ]);

  const remainingQuota = Math.max(0, profile.approvedMaxQty - profile.usedQty);

  return `
    <h1>👤 Farmer Profile & Paddy Limit</h1>

    <div class="grid g2">
      <!-- Profile Information -->
      <div class="card">
        <div class="card-header">
          <h3>Farmer Details</h3>
        </div>
        <form onsubmit="return handleProfileUpdate(event)" class="fgrid">
          <label>
            Full Name
            <input value="${esc(profile.name)}" readonly>
          </label>
          <label>
            Mobile Number
            <input value="${esc(profile.mobile)}" readonly>
          </label>
          <label>
            Farmer ID
            <input value="${esc(profile.farmerId)}" readonly>
          </label>
          <label>
            Aadhaar Number
            <input value="${esc(profile.aadhaar)}" readonly title="Aadhaar is masked for security">
          </label>
          <label class="full">
            Address
            <input name="address" value="${esc(profile.address)}" required>
          </label>
          <label>
            Village
            <input name="village" value="${esc(profile.village)}" required>
          </label>
          <label>
            District
            <input name="district" value="${esc(profile.district)}" required>
          </label>
          <label>
            State
            <input name="state" value="${esc(profile.state)}" required>
          </label>
          <label>
            Preferred Centre
            <select name="preferredCentreId">
              ${centres.map(c => `
                <option value="${c.id}" ${c.id === profile.preferredCentreId ? 'selected' : ''}>
                  ${esc(c.centreName)}
                </option>
              `).join('')}
            </select>
          </label>
          <div class="full" style="margin-top:0.75rem">
            <button class="btn gold block">Save Profile Changes</button>
          </div>
        </form>
      </div>

      <!-- Quantity Limit & Extra Request Form -->
      <div>
        <div class="card">
          <div class="card-header">
            <h3>Approved Paddy Quota</h3>
          </div>
          <p>
            Land Registered: <b>${profile.landAcres} acres</b><br>
            Formula: <b>20 bags per acre</b>
          </p>
          <div style="background:#f8fafc;border:1px solid var(--line);border-radius:10px;padding:1rem;margin-bottom:1rem">
            <div style="display:flex;justify-content:space-between;margin-bottom:0.4rem">
              <span>Approved Maximum:</span>
              <b>${profile.approvedMaxQty} bags</b>
            </div>
            <div style="display:flex;justify-content:space-between;margin-bottom:0.4rem">
              <span>Already Booked:</span>
              <b style="color:var(--gold)">${profile.usedQty} bags</b>
            </div>
            <div style="display:flex;justify-content:space-between;border-top:1px solid var(--line);padding-top:0.4rem">
              <span>Remaining Available:</span>
              <b style="color:var(--g7);font-size:1.15rem">${remainingQuota} bags</b>
            </div>
          </div>

          <h4>Request Additional Quantity</h4>
          <p class="small muted">If your yield was higher than provisional estimates, apply for an administrative quota revision:</p>
          <form onsubmit="return handleExtraQtySubmit(event)">
            <label>
              Extra Quantity Needed (in Bags)
              <input name="quantity" type="number" min="1" placeholder="e.g. 25" required>
            </label>
            <label>
              Reason for Additional Quota
              <textarea name="reason" rows="2" placeholder="e.g. Higher yield from second harvest / borewell irrigation" required></textarea>
            </label>
            <button class="btn out block">Submit Extra Quantity Request</button>
          </form>
        </div>

        <!-- Previous Requests -->
        <div class="card">
          <div class="card-header">
            <h3>Request History</h3>
          </div>
          ${requests.length ? `
            <div class="tbl">
              <table>
                <thead>
                  <tr>
                    <th>Requested</th>
                    <th>Reason</th>
                    <th>Status</th>
                    <th>Date</th>
                  </tr>
                </thead>
                <tbody>
                  ${requests.map(q => `
                    <tr>
                      <td><b>+${q.requestedQty} bags</b></td>
                      <td class="small">${esc(q.reason)}</td>
                      <td>${badge(q.status)}</td>
                      <td class="small muted">${formatDate(q.createdAt.slice(0, 10))}</td>
                    </tr>
                  `).join('')}
                </tbody>
              </table>
            </div>
          ` : '<p class="muted small c">No additional quantity requests submitted.</p>'}
        </div>
      </div>
    </div>
  `;
}

window.handleProfileUpdate = async (ev) => {
  ev.preventDefault();
  const f = formDataObj(ev.target);
  f.preferredCentreId = +f.preferredCentreId;
  try {
    await api('/farmers/me', { method: 'PUT', body: f });
    toast('Profile updated successfully.');
  } catch (err) {
    toast(err.message, true);
  }
  return false;
};

window.handleExtraQtySubmit = async (ev) => {
  ev.preventDefault();
  const f = formDataObj(ev.target);
  try {
    await api('/farmers/me/quantity-requests', {
      method: 'POST',
      body: { quantity: +f.quantity, reason: f.reason }
    });
    toast('Extra quantity request submitted for administrator review.');
    route();
  } catch (err) {
    toast(err.message, true);
  }
  return false;
};

// -------------------------------------------------------------
// 12. STAFF DASHBOARD & TODAY'S QUEUE
// -------------------------------------------------------------
async function staffDashboardView(queueOnly = false) {
  const [stats, queue] = await Promise.all([
    api('/staff/stats'),
    api('/staff/bookings')
  ]);

  // Periodic auto-refresh every 15 seconds
  window._slotsPoll = setInterval(route, 15000);

  return `
    <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:1rem;margin-bottom:1.5rem">
      <div>
        <h1 style="margin:0">${queueOnly ? "🚚 Today's Procurement Queue" : "🏢 Staff Centre Operations"}</h1>
        <p class="muted">Live queue management and stage progression for assigned procurement centre.</p>
      </div>
      <div>
        <button class="btn sm out" onclick="route()">🔄 Refresh Queue</button>
      </div>
    </div>

    ${!queueOnly ? `
      <!-- Stats Row -->
      <div class="grid g6" style="margin-bottom:1.5rem">
        <div class="stat">
          <small>Today's Total</small>
          <b>${stats.total}</b>
          <div class="stat-hint">${stats.quantity} total bags</div>
        </div>
        <div class="stat blue-top">
          <small>Confirmed</small>
          <b>${stats.confirmed}</b>
          <div class="stat-hint">Awaiting arrival</div>
        </div>
        <div class="stat teal-top">
          <small>Arrived</small>
          <b>${stats.arrived}</b>
          <div class="stat-hint">At centre gate</div>
        </div>
        <div class="stat gold-top">
          <small>Waiting</small>
          <b>${stats.waiting}</b>
          <div class="stat-hint">In queue lane</div>
        </div>
        <div class="stat" style="border-top-color:#ea580c">
          <small>Unloading</small>
          <b>${stats.unloading}</b>
          <div class="stat-hint">At weighbridge</div>
        </div>
        <div class="stat" style="border-top-color:#16a34a">
          <small>Completed</small>
          <b>${stats.completed}</b>
          <div class="stat-hint">Procured & weighed</div>
        </div>
      </div>
    ` : ''}

    <!-- Live Queue Table -->
    <div class="card" style="padding:0">
      <div style="padding:1rem 1.25rem;border-bottom:1px solid var(--line);display:flex;justify-content:space-between;align-items:center">
        <h3 style="margin:0">Live Vehicle Queue (${queue.length} bookings today)</h3>
        <span class="small muted">Auto-refreshes every 15s</span>
      </div>

      ${queue.length ? `
        <div class="tbl">
          <table>
            <thead>
              <tr>
                <th>Token</th>
                <th>Booking ID</th>
                <th>Farmer & ID</th>
                <th>Vehicle & Type</th>
                <th>Slot Time</th>
                <th>Paddy Bags</th>
                <th>Status</th>
                <th>Workflow Action</th>
              </tr>
            </thead>
            <tbody>
              ${queue.map(b => {
                const transitions = QUEUE_TRANSITIONS[b.status] || [];
                const actionButtons = transitions.map(([nextSt, label]) => `
                  <button class="btn tiny gold" onclick="advanceBookingStatus('${b.bookingId}','${nextSt}')">${label}</button>
                `).join(' ');

                return `
                  <tr>
                    <td><b style="color:var(--g9);font-size:1.2rem">${esc(b.token)}</b></td>
                    <td><span style="font-family:monospace">${esc(b.bookingId)}</span></td>
                    <td>
                      <b>${esc(b.farmer)}</b><br>
                      <small class="muted">${esc(b.farmerId)} · 📞 ${esc(b.contact)}</small>
                    </td>
                    <td>
                      <b>${esc(b.vehicle)}</b><br>
                      <small class="muted">${esc(b.vehicleType)}</small>
                    </td>
                    <td><b>${formatTime(b.start)} – ${formatTime(b.end)}</b></td>
                    <td><b>${b.quantity} bags</b><br><small class="muted">${esc(b.paddyType)}</small></td>
                    <td>${badge(b.status)}</td>
                    <td>
                      ${actionButtons || '<span class="small muted">Completed</span>'}
                    </td>
                  </tr>
                `;
              }).join('')}
            </tbody>
          </table>
        </div>
      ` : `
        <p class="muted c" style="padding:2.5rem">No bookings scheduled for today at this procurement centre.</p>
      `}
    </div>
  `;
}

window.advanceBookingStatus = async (bookingId, nextStatus) => {
  try {
    await api(`/staff/bookings/${bookingId}/status`, {
      method: 'PUT',
      body: { status: nextStatus }
    });
    toast(`Vehicle status updated to ${nextStatus}. Farmer notified.`);
    route();
  } catch (err) {
    toast(err.message, true);
  }
};

// -------------------------------------------------------------
// 13. SLOT MANAGEMENT (STAFF & ADMIN)
// -------------------------------------------------------------
let ADMIN_SELECTED_CENTRE_ID = null;

async function slotManagementView() {
  const isAdmin = S.u.role === 'ADMIN';
  let centresList = [];
  if (isAdmin) {
    centresList = await api('/centres');
    ADMIN_SELECTED_CENTRE_ID = ADMIN_SELECTED_CENTRE_ID || centresList[0].id;
  }

  const query = isAdmin ? `?centreId=${ADMIN_SELECTED_CENTRE_ID}` : '';
  const slots = await api('/staff/slots' + query);

  return `
    <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:1rem;margin-bottom:1.5rem">
      <div>
        <h1 style="margin:0">⏰ Slot Capacity & Availability Management</h1>
        <p class="muted">Publish new time slots, modify maximum capacities, or close slots.</p>
      </div>
      ${isAdmin ? `
        <div>
          <label style="display:flex;align-items:center;gap:0.5rem;margin:0">
            <span>Procurement Centre:</span>
            <select style="width:auto;margin:0" onchange="ADMIN_SELECTED_CENTRE_ID=+this.value;route()">
              ${centresList.map(c => `
                <option value="${c.id}" ${c.id === ADMIN_SELECTED_CENTRE_ID ? 'selected' : ''}>
                  ${esc(c.centreName)}
                </option>
              `).join('')}
            </select>
          </label>
        </div>
      ` : ''}
    </div>

    <!-- Create Slot Card -->
    <div class="card">
      <div class="card-header">
        <h3>Create New Time Slot</h3>
      </div>
      <form onsubmit="return handleCreateSlot(event)" class="fgrid">
        <label>
          Date
          <input type="date" name="date" min="${isoToday()}" value="${isoToday()}" required>
        </label>
        <label>
          Slot Capacity (Number of Vehicles)
          <input type="number" name="capacity" min="1" value="10" required>
        </label>
        <label>
          Start Time
          <input type="time" name="start" value="08:00" required>
        </label>
        <label>
          End Time
          <input type="time" name="end" value="09:00" required>
        </label>
        <div class="full" style="margin-top:0.5rem">
          <button class="btn gold block">Publish New Slot</button>
        </div>
      </form>
    </div>

    <!-- Slots Table -->
    <div class="card" style="padding:0">
      <div style="padding:1rem 1.25rem;border-bottom:1px solid var(--line)">
        <h3 style="margin:0">Published Slots (${slots.length})</h3>
      </div>

      <div class="tbl">
        <table>
          <thead>
            <tr>
              <th>Date</th>
              <th>Time Window</th>
              <th>Capacity</th>
              <th>Booked</th>
              <th>Available</th>
              <th>State</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            ${slots.map(s => `
              <tr>
                <td><b>${formatDate(s.slotDate)}</b></td>
                <td>${formatTime(s.startTime)} – ${formatTime(s.endTime)}</td>
                <td><b>${s.capacity}</b></td>
                <td>${s.bookedCount}</td>
                <td><b style="color:var(--g7)">${s.available}</b></td>
                <td>${badge(s.state)}</td>
                <td>
                  <button class="btn tiny out" onclick="modifySlotCapacityPrompt(${s.id},${s.capacity})">Edit Capacity</button>
                  <button class="btn tiny out" onclick="toggleSlotStatus(${s.id},'${s.status === 'OPEN' ? 'CLOSED' : 'OPEN'}')">
                    ${s.status === 'OPEN' ? 'Close Slot' : 'Reopen Slot'}
                  </button>
                  <button class="btn tiny red" onclick="deleteSlotPrompt(${s.id})">Delete</button>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
    </div>
  `;
}

window.handleCreateSlot = async (ev) => {
  ev.preventDefault();
  const f = formDataObj(ev.target);
  try {
    await api('/staff/slots', {
      method: 'POST',
      body: {
        centreId: ADMIN_SELECTED_CENTRE_ID,
        date: f.date,
        start: f.start,
        end: f.end,
        capacity: +f.capacity
      }
    });
    toast('New slot created successfully.');
    route();
  } catch (err) {
    toast(err.message, true);
  }
  return false;
};

window.modifySlotCapacityPrompt = async (id, currentCap) => {
  const val = prompt('Enter new maximum capacity for this slot:', currentCap);
  if (!val) return;
  try {
    await api('/staff/slots/' + id, { method: 'PUT', body: { capacity: +val } });
    toast('Slot capacity updated.');
    route();
  } catch (err) {
    toast(err.message, true);
  }
};

window.toggleSlotStatus = async (id, newStatus) => {
  try {
    await api('/staff/slots/' + id, { method: 'PUT', body: { status: newStatus } });
    toast(`Slot marked as ${newStatus}.`);
    route();
  } catch (err) {
    toast(err.message, true);
  }
};

window.deleteSlotPrompt = async (id) => {
  if (!confirm('Are you sure you want to delete this slot? Slots with bookings cannot be deleted.')) return;
  try {
    await api('/staff/slots/' + id, { method: 'DELETE' });
    toast('Slot deleted.');
    route();
  } catch (err) {
    toast(err.message, true);
  }
};

// -------------------------------------------------------------
// 14. ADMIN DASHBOARD & CHARTS
// -------------------------------------------------------------
async function adminDashboardView() {
  const d = await api('/admin/dashboard');

  window._afterRender = () => {
    if (!window.Chart) return;

    // 1. Daily Trend Bar Chart
    const dailyCtx = document.getElementById('dailyChart');
    if (dailyCtx) {
      new Chart(dailyCtx, {
        type: 'bar',
        data: {
          labels: Object.keys(d.daily),
          datasets: [
            {
              label: 'Total Bookings',
              data: Object.values(d.daily),
              backgroundColor: '#15803d',
              borderRadius: 6
            },
            {
              label: 'Completed Procurements',
              data: Object.values(d.completedDaily),
              backgroundColor: '#d97706',
              borderRadius: 6
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { position: 'bottom' } },
          scales: { y: { beginAtZero: true, ticks: { precision: 0 } } }
        }
      });
    }

    // 2. Status Doughnut Chart
    const statusCtx = document.getElementById('statusChart');
    if (statusCtx && d.statusBreakdown) {
      new Chart(statusCtx, {
        type: 'doughnut',
        data: {
          labels: ['Confirmed', 'Arrived', 'Waiting', 'Unloading', 'Completed', 'Cancelled'],
          datasets: [{
            data: [
              d.statusBreakdown.CONFIRMED || 0,
              d.statusBreakdown.ARRIVED || 0,
              d.statusBreakdown.WAITING || 0,
              d.statusBreakdown.UNLOADING || 0,
              d.statusBreakdown.COMPLETED || 0,
              d.statusBreakdown.CANCELLED || 0
            ],
            backgroundColor: ['#2563eb', '#0d9488', '#eab308', '#ea580c', '#16a34a', '#dc2626']
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { position: 'bottom' } }
        }
      });
    }

    // 3. Centre-wise Bar Chart
    const centreCtx = document.getElementById('centreChart');
    if (centreCtx) {
      new Chart(centreCtx, {
        type: 'bar',
        data: {
          labels: Object.keys(d.centreWise),
          datasets: [{
            label: 'Total Bookings by Centre',
            data: Object.values(d.centreWise),
            backgroundColor: '#065f46',
            borderRadius: 6
          }]
        },
        options: {
          indexAxis: 'y',
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { display: false } },
          scales: { x: { beginAtZero: true, ticks: { precision: 0 } } }
        }
      });
    }

    // 4. Centre Slot Utilization
    const utilCtx = document.getElementById('utilChart');
    if (utilCtx) {
      new Chart(utilCtx, {
        type: 'bar',
        data: {
          labels: Object.keys(d.utilization),
          datasets: [{
            label: 'Slot Utilization %',
            data: Object.values(d.utilization),
            backgroundColor: '#d97706',
            borderRadius: 6
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { display: false } },
          scales: { y: { beginAtZero: true, max: 100 } }
        }
      });
    }
  };

  return `
    <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:1rem;margin-bottom:1.5rem">
      <div>
        <h1 style="margin:0">🛡️ State Administrator Dashboard</h1>
        <p class="muted">Executive overview of paddy procurement, centres, queues, and utilization.</p>
      </div>
      <div>
        <a class="btn sm gold" href="#/admin/reports">📈 Generate Reports</a>
      </div>
    </div>

    <!-- Metrics Cards -->
    <div class="grid g4">
      <div class="stat">
        <small>Total Farmers</small>
        <b>${d.totalFarmers}</b>
        <div class="stat-hint">Registered across districts</div>
      </div>
      <div class="stat teal-top">
        <small>Procurement Centres</small>
        <b>${d.totalCentres}</b>
        <div class="stat-hint">Active Direct Purchase Centres</div>
      </div>
      <div class="stat blue-top">
        <small>Today's Bookings</small>
        <b>${d.todayBookings}</b>
        <div class="stat-hint">Procuring ${d.todayPaddyQuantity || 0} bags today</div>
      </div>
      <div class="stat gold-top">
        <small>Total Paddy Bags</small>
        <b>${d.totalPaddyQuantity || 0} <span class="small muted">bags</span></b>
        <div class="stat-hint">Lifetime bookings across centres</div>
      </div>
    </div>

    <!-- Secondary Row -->
    <div class="grid g5" style="margin-top:1.25rem">
      <div class="stat blue-top">
        <small>Today Confirmed</small>
        <b>${d.todayConfirmed || 0}</b>
      </div>
      <div class="stat teal-top">
        <small>Today Arrived</small>
        <b>${d.todayArrived || 0}</b>
      </div>
      <div class="stat gold-top">
        <small>Today Waiting</small>
        <b>${d.todayWaiting || 0}</b>
      </div>
      <div class="stat" style="border-top-color:#ea580c">
        <small>Today Unloading</small>
        <b>${d.todayUnloading || 0}</b>
      </div>
      <div class="stat" style="border-top-color:#16a34a">
        <small>Today Completed</small>
        <b>${d.todayCompleted || 0}</b>
      </div>
    </div>

    <!-- Charts Grid -->
    <div class="grid g2" style="margin-top:1.5rem">
      <div class="card">
        <h3>Daily Bookings & Completed Procurements</h3>
        <div class="chart-box">
          <canvas id="dailyChart"></canvas>
        </div>
      </div>
      <div class="card">
        <h3>Queue Status Distribution</h3>
        <div class="chart-box">
          <canvas id="statusChart"></canvas>
        </div>
      </div>
      <div class="card">
        <h3>Centre-wise Booking Volume</h3>
        <div class="chart-box">
          <canvas id="centreChart"></canvas>
        </div>
      </div>
      <div class="card">
        <h3>Centre Slot Utilization (Next 7 Days %)</h3>
        <div class="chart-box">
          <canvas id="utilChart"></canvas>
        </div>
      </div>
    </div>
  `;
}

// -------------------------------------------------------------
// 15. ADMIN FARMER MANAGEMENT & EXTRA QUANTITY REQUESTS
// -------------------------------------------------------------
let ADMIN_FARMERS_CACHE = [];

async function adminFarmersView() {
  const [farmers, requests] = await Promise.all([
    api('/admin/farmers'),
    api('/admin/quantity-requests')
  ]);
  ADMIN_FARMERS_CACHE = farmers;

  window.filterAdminFarmers = () => {
    const q = ($('#farmer-search-input')?.value || '').toLowerCase().trim();
    const filtered = ADMIN_FARMERS_CACHE.filter(f =>
      (f.name + ' ' + f.farmerId + ' ' + f.mobile + ' ' + f.village + ' ' + f.district).toLowerCase().includes(q)
    );

    const tbody = $('#farmers-table-body');
    if (!tbody) return;

    tbody.innerHTML = filtered.map(x => `
      <tr>
        <td><b>${esc(x.farmerId)}</b></td>
        <td><b>${esc(x.name)}</b></td>
        <td>${esc(x.mobile)}</td>
        <td>${esc(x.village)}, ${esc(x.district)}</td>
        <td>${x.landAcres} ac</td>
        <td><b style="color:var(--g7)">${x.approvedMaxQty} bags</b></td>
        <td>${x.usedQty} bags</td>
        <td>${badge(x.status)}</td>
        <td>
          <button class="btn tiny out" onclick="modifyApprovedLimitPrompt(${x.id},${x.approvedMaxQty})">Set Limit</button>
          <button class="btn tiny out" onclick="toggleUserStatus(${x.userId},'${x.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'}')">
            ${x.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
          </button>
        </td>
      </tr>
    `).join('') || '<tr><td colspan="9" class="c muted">No farmers match search criteria.</td></tr>';
  };

  window._afterRender = window.filterAdminFarmers;

  return `
    <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:1rem;margin-bottom:1.5rem">
      <div>
        <h1 style="margin:0">🌾 Farmer Management & Quantity Quotas</h1>
        <p class="muted">Manage registered farmers, review additional quota requests, and adjust approved bags.</p>
      </div>
    </div>

    <!-- Extra Quantity Requests Section -->
    <div class="card" style="padding:0;margin-bottom:1.5rem">
      <div style="padding:1rem 1.25rem;border-bottom:1px solid var(--line);display:flex;justify-content:space-between;align-items:center">
        <h3 style="margin:0">Pending Additional Quantity Requests (${requests.filter(r => r.status === 'PENDING').length} Pending)</h3>
      </div>

      ${requests.length ? `
        <div class="tbl">
          <table>
            <thead>
              <tr>
                <th>Farmer</th>
                <th>Farmer ID</th>
                <th>Current Limit</th>
                <th>Requested Extra</th>
                <th>Reason</th>
                <th>Date</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              ${requests.map(q => `
                <tr>
                  <td><b>${esc(q.farmer)}</b></td>
                  <td>${esc(q.farmerId)}</td>
                  <td>${q.approvedMaxQty} bags</td>
                  <td><b style="color:var(--gold)">+${q.requestedQty} bags</b></td>
                  <td class="small">${esc(q.reason)}</td>
                  <td class="small muted">${formatDate(q.createdAt.slice(0, 10))}</td>
                  <td>${badge(q.status)}</td>
                  <td>
                    ${q.status === 'PENDING' ? `
                      <button class="btn tiny gold" onclick="decideExtraRequest(${q.id}, true)">Approve</button>
                      <button class="btn tiny red" onclick="decideExtraRequest(${q.id}, false)">Reject</button>
                    ` : '<span class="small muted">Reviewed</span>'}
                  </td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>
      ` : '<p class="muted c" style="padding:1.5rem">No additional quantity requests pending.</p>'}
    </div>

    <!-- All Farmers Table -->
    <div class="card" style="padding:0">
      <div style="padding:1rem 1.25rem;border-bottom:1px solid var(--line);display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:0.75rem">
        <h3 style="margin:0">All Registered Farmers (${farmers.length})</h3>
        <input id="farmer-search-input" type="search" placeholder="🔍 Search farmer name, ID, village..." style="max-width:280px;margin:0" oninput="filterAdminFarmers()">
      </div>

      <div class="tbl">
        <table>
          <thead>
            <tr>
              <th>Farmer ID</th>
              <th>Full Name</th>
              <th>Mobile</th>
              <th>Location</th>
              <th>Land</th>
              <th>Approved Limit</th>
              <th>Used</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody id="farmers-table-body"></tbody>
        </table>
      </div>
    </div>
  `;
}

window.decideExtraRequest = async (id, approve) => {
  try {
    await api('/admin/quantity-requests/' + id, {
      method: 'PUT',
      body: { approve }
    });
    toast(approve ? 'Request approved and farmer limit updated!' : 'Request rejected.');
    route();
  } catch (err) {
    toast(err.message, true);
  }
};

window.modifyApprovedLimitPrompt = async (farmerId, currentVal) => {
  const val = prompt('Enter new approved maximum quantity limit (in bags):', currentVal);
  if (!val) return;
  try {
    await api(`/admin/farmers/${farmerId}/max-quantity`, {
      method: 'PUT',
      body: { value: +val }
    });
    toast('Farmer limit updated successfully.');
    route();
  } catch (err) {
    toast(err.message, true);
  }
};

window.toggleUserStatus = async (userId, newStatus) => {
  try {
    await api(`/admin/users/${userId}/status`, {
      method: 'PUT',
      body: { status: newStatus }
    });
    toast(`User status changed to ${newStatus}.`);
    route();
  } catch (err) {
    toast(err.message, true);
  }
};

// -------------------------------------------------------------
// 16. ADMIN PROCUREMENT CENTRES & STAFF
// -------------------------------------------------------------
let EDITING_CENTRE_ID = null;

async function adminCentresView() {
  const [centres, staff] = await Promise.all([
    api('/admin/centres'),
    api('/admin/staff')
  ]);

  const editingCentre = EDITING_CENTRE_ID ? centres.find(c => c.id === EDITING_CENTRE_ID) : {};

  return `
    <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:1rem;margin-bottom:1.5rem">
      <div>
        <h1 style="margin:0">🏢 Procurement Centres & Staff Management</h1>
        <p class="muted">Add or configure Direct Purchase Centres and assign staff login accounts.</p>
      </div>
    </div>

    <!-- Centre Form -->
    <div class="card">
      <div class="card-header">
        <h3>${EDITING_CENTRE_ID ? '✏️ Edit Procurement Centre' : '➕ Add New Procurement Centre'}</h3>
        ${EDITING_CENTRE_ID ? '<button class="btn tiny out" onclick="EDITING_CENTRE_ID=null;route()">Cancel Edit</button>' : ''}
      </div>
      <form onsubmit="return handleSaveCentre(event)" class="fgrid">
        <label>
          Centre Code
          <input name="centreCode" value="${esc(editingCentre.centreCode || '')}" placeholder="e.g. TNJ-002" required>
        </label>
        <label>
          Centre Name
          <input name="centreName" value="${esc(editingCentre.centreName || '')}" placeholder="e.g. Kumbakonam Direct Purchase Centre" required>
        </label>
        <label class="full">
          Address
          <input name="address" value="${esc(editingCentre.address || '')}" placeholder="Street / Highway location" required>
        </label>
        <label>
          Village
          <input name="village" value="${esc(editingCentre.village || '')}" placeholder="Village name">
        </label>
        <label>
          District
          <input name="district" value="${esc(editingCentre.district || '')}" placeholder="District name">
        </label>
        <label>
          Contact Number
          <input name="contactNumber" value="${esc(editingCentre.contactNumber || '')}" placeholder="e.g. 04362-200105">
        </label>
        <label>
          Max Daily Capacity (Vehicles)
          <input name="maxDailyCapacity" type="number" min="1" value="${editingCentre.maxDailyCapacity || 50}">
        </label>
        <label>
          Opening Time
          <input type="time" name="openTime" value="${(editingCentre.openTime || '08:00').slice(0, 5)}">
        </label>
        <label>
          Closing Time
          <input type="time" name="closeTime" value="${(editingCentre.closeTime || '17:00').slice(0, 5)}">
        </label>
        <label>
          Centre Status
          <select name="status">
            <option value="OPEN" ${editingCentre.status !== 'CLOSED' ? 'selected' : ''}>OPEN</option>
            <option value="CLOSED" ${editingCentre.status === 'CLOSED' ? 'selected' : ''}>CLOSED</option>
          </select>
        </label>
        <div class="full" style="margin-top:0.5rem">
          <button class="btn gold block">${EDITING_CENTRE_ID ? 'Update Procurement Centre' : 'Save New Centre'}</button>
        </div>
      </form>
    </div>

    <!-- Centres Table -->
    <div class="card" style="padding:0;margin-bottom:1.5rem">
      <div style="padding:1rem 1.25rem;border-bottom:1px solid var(--line)">
        <h3 style="margin:0">Active Centres (${centres.length})</h3>
      </div>
      <div class="tbl">
        <table>
          <thead>
            <tr>
              <th>Code</th>
              <th>Centre Name</th>
              <th>Location</th>
              <th>Contact</th>
              <th>Operating Hours</th>
              <th>Capacity/Day</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            ${centres.map(c => `
              <tr>
                <td><b>${esc(c.centreCode)}</b></td>
                <td><b>${esc(c.centreName)}</b></td>
                <td>${esc(c.village)}, ${esc(c.district)}</td>
                <td>${esc(c.contactNumber)}</td>
                <td>${formatTime(c.openTime)} – ${formatTime(c.closeTime)}</td>
                <td>${c.maxDailyCapacity}</td>
                <td>${badge(c.status)}</td>
                <td>
                  <button class="btn tiny out" onclick="EDITING_CENTRE_ID=${c.id};route()">Edit</button>
                  <button class="btn tiny red" onclick="deactivateCentrePrompt(${c.id})">Deactivate</button>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
    </div>

    <!-- Staff Management Section -->
    <div class="card">
      <div class="card-header">
        <h3>➕ Add Staff Member</h3>
      </div>
      <form onsubmit="return handleAddStaff(event)" class="fgrid">
        <label>
          Staff Name
          <input name="name" placeholder="e.g. Ramesh Staff" required>
        </label>
        <label>
          Mobile Number
          <input name="mobile" pattern="\\d{10}" maxlength="10" placeholder="10-digit mobile number" required>
        </label>
        <label>
          Password
          <input name="password" type="password" minlength="6" placeholder="Min 6 characters" required>
        </label>
        <label>
          Assigned Procurement Centre
          <select name="centreId" required>
            ${centres.map(c => `<option value="${c.id}">${esc(c.centreName)} (${esc(c.centreCode)})</option>`).join('')}
          </select>
        </label>
        <div class="full" style="margin-top:0.5rem">
          <button class="btn gold block">Create Staff Account</button>
        </div>
      </form>
    </div>

    <!-- Staff Members List -->
    <div class="card" style="padding:0">
      <div style="padding:1rem 1.25rem;border-bottom:1px solid var(--line)">
        <h3 style="margin:0">Staff Members (${staff.length})</h3>
      </div>
      <div class="tbl">
        <table>
          <thead>
            <tr>
              <th>Staff Name</th>
              <th>Mobile</th>
              <th>Assigned Centre</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            ${staff.map(u => `
              <tr>
                <td><b>${esc(u.name)}</b></td>
                <td>${esc(u.mobile)}</td>
                <td><b>${esc(u.centre)}</b></td>
                <td>${badge(u.status)}</td>
                <td>
                  <button class="btn tiny out" onclick="toggleUserStatus(${u.id},'${u.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'}')">
                    ${u.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                  </button>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
    </div>
  `;
}

window.handleSaveCentre = async (ev) => {
  ev.preventDefault();
  const f = formDataObj(ev.target);
  f.maxDailyCapacity = +f.maxDailyCapacity;
  try {
    await api(EDITING_CENTRE_ID ? '/admin/centres/' + EDITING_CENTRE_ID : '/admin/centres', {
      method: EDITING_CENTRE_ID ? 'PUT' : 'POST',
      body: f
    });
    EDITING_CENTRE_ID = null;
    toast('Procurement centre saved successfully.');
    route();
  } catch (err) {
    toast(err.message, true);
  }
  return false;
};

window.deactivateCentrePrompt = async (id) => {
  if (!confirm('Are you sure you want to deactivate this centre? New bookings will be stopped.')) return;
  try {
    await api('/admin/centres/' + id, { method: 'DELETE' });
    toast('Centre deactivated.');
    route();
  } catch (err) {
    toast(err.message, true);
  }
};

window.handleAddStaff = async (ev) => {
  ev.preventDefault();
  const f = formDataObj(ev.target);
  f.centreId = +f.centreId;
  try {
    await api('/admin/staff', { method: 'POST', body: f });
    toast('Staff account created.');
    route();
  } catch (err) {
    toast(err.message, true);
  }
  return false;
};

// -------------------------------------------------------------
// 17. ADMIN BOOKINGS AUDIT
// -------------------------------------------------------------
let ADMIN_BOOKING_FILTERS = { date: '', status: '', centreId: '' };

async function adminBookingsView() {
  const centres = await api('/centres');
  const query = `?date=${ADMIN_BOOKING_FILTERS.date}&status=${ADMIN_BOOKING_FILTERS.status}&centreId=${ADMIN_BOOKING_FILTERS.centreId}`;
  const bookings = await api('/admin/bookings' + query);

  return `
    <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:1rem;margin-bottom:1.5rem">
      <div>
        <h1 style="margin:0">📋 All Bookings Management</h1>
        <p class="muted">Search, filter, and audit statewide procurement bookings.</p>
      </div>
    </div>

    <!-- Filter Bar -->
    <div class="card" style="padding:1rem">
      <div class="row">
        <label style="margin:0">
          Date:
          <input type="date" value="${ADMIN_BOOKING_FILTERS.date}" onchange="ADMIN_BOOKING_FILTERS.date=this.value;route()" style="width:auto;margin:0">
        </label>
        <label style="margin:0">
          Status:
          <select onchange="ADMIN_BOOKING_FILTERS.status=this.value;route()" style="width:auto;margin:0">
            <option value="">All Statuses</option>
            ${['CONFIRMED', 'ARRIVED', 'WAITING', 'UNLOADING', 'COMPLETED', 'CANCELLED'].map(s => `
              <option value="${s}" ${ADMIN_BOOKING_FILTERS.status === s ? 'selected' : ''}>${s}</option>
            `).join('')}
          </select>
        </label>
        <label style="margin:0">
          Centre:
          <select onchange="ADMIN_BOOKING_FILTERS.centreId=this.value;route()" style="width:auto;margin:0">
            <option value="">All Centres</option>
            ${centres.map(c => `
              <option value="${c.id}" ${ADMIN_BOOKING_FILTERS.centreId == c.id ? 'selected' : ''}>${esc(c.centreName)}</option>
            `).join('')}
          </select>
        </label>
        <button class="btn tiny out" onclick="ADMIN_BOOKING_FILTERS={date:'',status:'',centreId:''};route()">Reset Filters</button>
      </div>
    </div>

    <!-- Bookings Table -->
    <div class="card" style="padding:0">
      <div style="padding:1rem 1.25rem;border-bottom:1px solid var(--line)">
        <h3 style="margin:0">Bookings (${bookings.length})</h3>
      </div>
      <div class="tbl">
        <table>
          <thead>
            <tr>
              <th>Token</th>
              <th>Booking ID</th>
              <th>Farmer</th>
              <th>Centre</th>
              <th>Date & Time</th>
              <th>Vehicle</th>
              <th>Quantity</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            ${bookings.map(b => `
              <tr>
                <td><b style="color:var(--g9);font-size:1.15rem">${esc(b.token)}</b></td>
                <td><span style="font-family:monospace">${esc(b.bookingId)}</span></td>
                <td><b>${esc(b.farmer)}</b><br><small class="muted">${esc(b.farmerId)}</small></td>
                <td>${esc(b.centre)}</td>
                <td><b>${formatDate(b.date)}</b><br><small class="muted">${formatTime(b.start)} – ${formatTime(b.end)}</small></td>
                <td>${esc(b.vehicle)}<br><small class="muted">${esc(b.vehicleType)}</small></td>
                <td><b>${b.quantity} bags</b><br><small class="muted">${esc(b.paddyType)}</small></td>
                <td>${badge(b.status)}</td>
                <td>
                  <a class="btn tiny" href="#/token/${b.bookingId}">🎫 Token</a>
                  ${b.status === 'CONFIRMED' ? `
                    <button class="btn tiny red" onclick="cancelBookingPrompt('${b.bookingId}')">Cancel</button>
                  ` : ''}
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
    </div>
  `;
}

// -------------------------------------------------------------
// 18. ADMIN REPORTS
// -------------------------------------------------------------
let REPORT_CACHE = { columns: [], rows: [] };

async function reportsView() {
  const today = isoToday();
  const weekAgo = new Date(Date.now() - 7 * 86400000).toISOString().slice(0, 10);

  return `
    <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:1rem;margin-bottom:1.5rem">
      <div>
        <h1 style="margin:0">📈 Procurement Reports & Exports</h1>
        <p class="muted">Generate comprehensive operational reports with date filters and export to CSV or PDF.</p>
      </div>
    </div>

    <!-- Report Generator Form -->
    <div class="card">
      <form onsubmit="return handleGenerateReport(event)" class="fgrid">
        <label>
          Report Type
          <select name="type">
            <option value="daily">Daily Booking & Procurement Summary</option>
            <option value="centre">Procurement Centre Aggregation</option>
            <option value="completed">Completed Procurements Detailed Audit</option>
            <option value="utilization">Centre Slot Utilization Report</option>
            <option value="farmer">Farmer Procurement Volume Report</option>
          </select>
        </label>
        <label>
          From Date
          <input type="date" name="from" value="${weekAgo}" required>
        </label>
        <label>
          To Date
          <input type="date" name="to" value="${today}" required>
        </label>
        <div style="display:flex;align-items:flex-end;margin-bottom:0.9rem">
          <button class="btn gold block" id="gen-rep-btn">Generate Report</button>
        </div>
      </form>
    </div>

    <!-- Results Display -->
    <div id="report-output-container"></div>
  `;
}

window.handleGenerateReport = async (ev) => {
  ev.preventDefault();
  const btn = $('#gen-rep-btn');
  if (btn) btn.disabled = true;

  const f = formDataObj(ev.target);
  const out = $('#report-output-container');

  try {
    const res = await api(`/admin/reports?type=${f.type}&from=${f.from}&to=${f.to}`);
    REPORT_CACHE = res;

    out.innerHTML = `
      <div class="card" style="padding:0">
        <div style="padding:1rem 1.25rem;border-bottom:1px solid var(--line);display:flex;justify-content:space-between;align-items:center" class="noprint">
          <div>
            <h3 style="margin:0">Generated Report (${res.rows.length} records)</h3>
            <span class="small muted">Period: ${formatDate(f.from)} to ${formatDate(f.to)}</span>
          </div>
          <div class="row">
            <button class="btn sm gold" onclick="exportReportCsv()">📥 Export CSV</button>
            <button class="btn sm alt" onclick="window.print()">🖨️ Print / Save PDF</button>
          </div>
        </div>

        <div class="tbl">
          <table>
            <thead>
              <tr>${res.columns.map(c => `<th>${esc(c)}</th>`).join('')}</tr>
            </thead>
            <tbody>
              ${res.rows.length ? res.rows.map(r => `
                <tr>${r.map(v => `<td>${esc(v)}</td>`).join('')}</tr>
              `).join('') : '<tr><td colspan="10" class="c muted" style="padding:2rem">No procurement data found for the selected date range.</td></tr>'}
            </tbody>
          </table>
        </div>
      </div>
    `;
  } catch (err) {
    toast(err.message, true);
  } finally {
    if (btn) btn.disabled = false;
  }
  return false;
};

window.exportReportCsv = () => {
  const { columns, rows } = REPORT_CACHE;
  if (!columns.length) return;

  const csvRows = [columns, ...rows];
  const csvContent = csvRows.map(r => r.map(v => '"' + String(v).replace(/"/g, '""') + '"').join(',')).join('\n');

  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.setAttribute('href', url);
  link.setAttribute('download', `PPMS-Report-${isoToday()}.csv`);
  link.click();
};

// -------------------------------------------------------------
// 19. ROUTER & CONTROLLER
// -------------------------------------------------------------
const ROUTES = [
  [/^\/$/, landingView],
  [/^\/login$/, () => loginView('FARMER')],
  [/^\/staff-login$/, () => loginView('STAFF')],
  [/^\/admin-login$/, () => loginView('ADMIN')],
  [/^\/register$/, registerView],
  [/^\/centres$/, centresView],
  [/^\/slots\/(\d+)$/, slotsPageView],
  [/^\/dashboard$/, farmerDashboardView, 'FARMER'],
  [/^\/book\/(\d+)$/, bookingFormView, 'FARMER'],
  [/^\/token\/(\w+)$/, tokenReceiptView, 'FARMER,STAFF,ADMIN'],
  [/^\/bookings$/, myBookingsView, 'FARMER'],
  [/^\/profile$/, farmerProfileView, 'FARMER'],
  [/^\/notifications$/, notificationsView, '*'],
  [/^\/staff$/, () => staffDashboardView(false), 'STAFF'],
  [/^\/staff\/queue$/, () => staffDashboardView(true), 'STAFF'],
  [/^\/staff\/slots$/, slotManagementView, 'STAFF,ADMIN'],
  [/^\/admin$/, adminDashboardView, 'ADMIN'],
  [/^\/admin\/farmers$/, adminFarmersView, 'ADMIN'],
  [/^\/admin\/centres$/, adminCentresView, 'ADMIN'],
  [/^\/admin\/bookings$/, adminBookingsView, 'ADMIN'],
  [/^\/admin\/reports$/, reportsView, 'ADMIN']
];

async function route() {
  const hash = (location.hash.slice(1) || '/').split('?')[0];

  clearInterval(window._slotsPoll);
  window._afterRender = null;

  for (const [pattern, handler, roleRequired] of ROUTES) {
    const match = hash.match(pattern);
    if (!match) continue;

    // Role check
    if (roleRequired) {
      if (!S.u) {
        const dest = roleRequired.includes('STAFF') ? 'staff-login' : (roleRequired === 'ADMIN' ? 'admin-login' : 'login');
        location.hash = '#/' + dest;
        return;
      }
      if (roleRequired !== '*' && !roleRequired.split(',').includes(S.u.role)) {
        location.hash = '#/' + ROLE_HOMES[S.u.role];
        return;
      }
    }

    try {
      const html = await handler(...match.slice(1));
      App.innerHTML = shell(html);
      window.scrollTo(0, 0);
      if (window._afterRender) window._afterRender();
    } catch (err) {
      App.innerHTML = shell(`
        <div class="card err narrow">
          <h2>⚠️ Error</h2>
          <p>${esc(err.message)}</p>
          <a class="btn gold" href="#/">Return to Home</a>
        </div>
      `);
    }

    updateNotificationBadge();
    return;
  }

  // 404
  App.innerHTML = shell(`
    <div class="card narrow c">
      <h2>Page Not Found</h2>
      <p class="muted">The requested page could not be found.</p>
      <a class="btn gold" href="#/">Go to Home</a>
    </div>
  `);
}

window.addEventListener('hashchange', route);
route();
