{/* ═══════════════════════════════════════════════════════════════════════════
 * PHILL — Phillips Mobile Automotive
 * Interactive UI Mockup (React / JSX)
 * ─────────────────────────────────────────────────────────────────────────
 * This file is a SINGLE-FILE React mockup that mirrors every screen
 * implemented in the Kotlin/Compose codebase as of this commit.
 *
 * BOTTOM NAV (5 tabs):
 *   Dashboard | Schedule | Jobs | Comms | More
 *
 * "More" opens CustomerListScreen, which acts as the overflow hub.
 *
 * SCREENS (18 total):
 *   1.  DashboardScreen
 *   2.  ScheduleScreen
 *   3.  AppointmentFormScreen
 *   4.  JobQueueScreen
 *   5.  JobDetailScreen
 *   6.  CustomerListScreen
 *   7.  CustomerDetailScreen
 *   8.  CustomerFormScreen
 *   9.  VehicleFormScreen
 *  10.  ConversationListScreen
 *  11.  ConversationDetailScreen
 *  12.  BillingHubScreen
 *  13.  InvoiceBuilderScreen
 *  14.  InvoiceDetailScreen
 *  15.  ExpenseFormScreen
 *  16.  PaymentLogScreen
 *  17.  ShopSettingsScreen
 *  18.  AnalyticsScreen
 *
 * COLOR SYSTEM (from Color.kt + inline usage):
 *   Primary Container: #D0BCFF / Purple80
 *   Status:
 *     SCHEDULED  → Gray
 *     EN_ROUTE   → #2196F3 (Blue)
 *     ON_SITE    → #FF9800 (Orange)
 *     COMPLETE   → #4CAF50 (Green)
 *   Revenue      → #4CAF50
 *   Error/Expense→ #F44336
 *   Warning      → #FF9800
 *
 * FIDUCIARY RULE: All money stored as Long cents. Displayed via
 * BillingEngine.formatCents(). This mockup renders "$X.XX".
 * ═══════════════════════════════════════════════════════════════════════════ */

import React, { useState } from "react";

// ── Helpers ──────────────────────────────────────────────────────────────
const fmt = (cents) => {
  const neg = cents < 0;
  const abs = Math.abs(cents);
  return (neg ? "-" : "") + "$" + (abs / 100).toFixed(2);
};

const today = () => {
  const d = new Date();
  const days = ["Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"];
  const months = ["January","February","March","April","May","June","July","August","September","October","November","December"];
  return `${days[d.getDay()]}, ${months[d.getMonth()]} ${d.getDate()}`;
};

const formatTime = (h, m) => {
  const ampm = h >= 12 ? "PM" : "AM";
  return `${h % 12 || 12}:${String(m).padStart(2, "0")} ${ampm}`;
};

// ── Mock Data ────────────────────────────────────────────────────────────
const MOCK = {
  customers: [
    { id: "c1", firstName: "James", lastName: "Wilson", phone: "843-555-0142", email: "jwilson@email.com", address: "1420 King St, Charleston SC 29403", notes: "Prefers morning appointments" },
    { id: "c2", firstName: "Sarah", lastName: "Martinez", phone: "843-555-0198", email: null, address: "782 Meeting St, Charleston SC 29403", notes: null },
    { id: "c3", firstName: "Robert", lastName: "Chen", phone: "843-555-0267", email: "rchen@outlook.com", address: null, notes: "Fleet customer — 3 vehicles" },
  ],
  vehicles: [
    { id: "v1", customerId: "c1", year: 2019, make: "Toyota", model: "Camry", engine: "2.5L I4", vin: "4T1B11HK5KU123456", color: "Silver" },
    { id: "v2", customerId: "c1", year: 2022, make: "Honda", model: "CR-V", engine: "1.5L Turbo", vin: null, color: "Blue" },
    { id: "v3", customerId: "c2", year: 2017, make: "Ford", model: "F-150", engine: "5.0L V8", vin: null, color: "White" },
    { id: "v4", customerId: "c3", year: 2021, make: "Chevrolet", model: "Silverado", engine: "6.2L V8", vin: null, color: "Black" },
  ],
  jobs: [
    { id: "j1", customerId: "c1", vehicleId: "v1", status: "ON_SITE", description: "Oil change + brake inspection", createdAt: Date.now() },
    { id: "j2", customerId: "c2", vehicleId: "v3", status: "SCHEDULED", description: "Transmission flush", createdAt: Date.now() },
    { id: "j3", customerId: "c3", vehicleId: "v4", status: "EN_ROUTE", description: "Battery replacement", createdAt: Date.now() },
    { id: "j4", customerId: "c1", vehicleId: "v2", status: "COMPLETE", description: "A/C recharge", createdAt: Date.now() - 86400000, completedAt: Date.now() - 3600000 },
  ],
  appointments: [
    { id: "a1", customerId: "c1", vehicleId: "v1", address: "1420 King St, Charleston SC", startEpoch: Date.now() + 3600000, status: "CONFIRMED", notes: "Oil change + brake inspection" },
    { id: "a2", customerId: "c2", vehicleId: "v3", address: "782 Meeting St, Charleston SC", startEpoch: Date.now() + 7200000, status: "PENDING", notes: "Transmission flush" },
  ],
  invoices: [
    { id: "inv1", jobId: "j4", customerId: "c1", status: "INVOICE", totalCents: 18500, lineItems: [
      { type: "LABOR", desc: "A/C Recharge Labor", qty: "1.5", unitPrice: "75.00", totalCents: 11250 },
      { type: "PARTS", desc: "R-134a Refrigerant (2 cans)", qty: "2", unitPrice: "18.50", totalCents: 5180 },
      { type: "MISC", desc: "Dye kit", qty: "1", unitPrice: "12.00", totalCents: 1200 },
    ]},
    { id: "inv2", jobId: "j1", customerId: "c1", status: "ESTIMATE", totalCents: 9800, lineItems: [
      { type: "LABOR", desc: "Oil Change", qty: "0.5", unitPrice: "75.00", totalCents: 3750 },
      { type: "PARTS", desc: "Oil Filter", qty: "1", unitPrice: "8.50", totalCents: 1190 },
      { type: "PARTS", desc: "5W-30 Synthetic (5qt)", qty: "1", unitPrice: "28.00", totalCents: 3920 },
    ]},
  ],
  payments: [
    { id: "p1", invoiceId: "inv1", amountCents: 10000, method: "CASH", paidAt: Date.now() - 7200000, ref: null, notes: "Partial payment" },
  ],
  expenses: [
    { id: "e1", description: "Oil filters (bulk)", amountCents: 12500, category: "PARTS", vendor: "O'Reilly Auto", dateEpoch: Date.now() },
    { id: "e2", description: "Gas - work truck", amountCents: 6800, category: "FUEL", vendor: "Shell", dateEpoch: Date.now() },
  ],
  conversations: [
    { id: "conv1", phoneNumber: "843-555-0142", customerId: "c1", displayName: "James Wilson", unreadCount: 2, lastMessageEpoch: Date.now() - 600000,
      messages: [
        { id: "m1", body: "Hi, I need an oil change for my Camry. Available tomorrow?", isInbound: true, ts: Date.now() - 3600000 },
        { id: "m2", body: "Good morning James! I have a 10am slot open tomorrow. Would that work?", isInbound: false, ts: Date.now() - 3000000 },
        { id: "m3", body: "Perfect, 10am works. See you then!", isInbound: true, ts: Date.now() - 2400000 },
        { id: "m4", body: "Also, can you check my brakes while you're at it?", isInbound: true, ts: Date.now() - 600000 },
      ]
    },
    { id: "conv2", phoneNumber: "843-555-9999", customerId: null, displayName: null, unreadCount: 1, lastMessageEpoch: Date.now() - 300000,
      messages: [
        { id: "m5", body: "Hey do you do mobile oil changes? What's your rate?", isInbound: true, ts: Date.now() - 300000 },
      ]
    },
  ],
  clockEntries: [
    { id: "clk1", jobId: "j1", clockIn: Date.now() - 5400000, clockOut: null },
  ],
  mileageEntries: [
    { id: "mil1", jobId: "j1", miles: 12.4, purpose: "To customer" },
  ],
  shopProfile: {
    businessName: "Phillips Mobile Automotive",
    businessAddress: "Charleston, SC 29403",
    ownerName: "Devon Phillips",
    ownerPhone: "843-555-0100",
    laborRateCents: 7500,
    serviceFeeCents: 2500,
    partsMarkupBasisPoints: 14000,
    taxRateBasisPoints: 900,
    taxId: "",
    licenseNumber: "",
  },
};

// ── Styling Constants ────────────────────────────────────────────────────
const COLORS = {
  primary: "#6650a4",
  primaryContainer: "#D0BCFF",
  onPrimaryContainer: "#21005D",
  secondary: "#625b71",
  secondaryContainer: "#E8DEF8",
  surface: "#FEF7FF",
  surfaceVariant: "#E7E0EC",
  onSurface: "#1D1B20",
  onSurfaceVariant: "#49454F",
  error: "#B3261E",
  errorContainer: "#F9DEDC",
  tertiary: "#7D5260",

  // Status
  scheduled: "#9E9E9E",
  enRoute: "#2196F3",
  onSite: "#FF9800",
  complete: "#4CAF50",
  warning: "#FF9800",
  warningBg: "#FFF3E0",
  revenue: "#4CAF50",
  danger: "#F44336",
};

const statusColor = (s) => ({
  SCHEDULED: COLORS.scheduled,
  EN_ROUTE: COLORS.enRoute,
  ON_SITE: COLORS.onSite,
  COMPLETE: COLORS.complete,
  PENDING: COLORS.tertiary,
  CONFIRMED: COLORS.primary,
  CANCELLED: COLORS.error,
})[s] || COLORS.scheduled;

// ── Shared Components ────────────────────────────────────────────────────

const TopBar = ({ title, subtitle, onBack, actions }) => (
  <div style={{ background: COLORS.primaryContainer, color: COLORS.onPrimaryContainer, padding: "12px 16px", display: "flex", alignItems: "center", gap: 12 }}>
    {onBack && <button onClick={onBack} style={{ background: "none", border: "none", cursor: "pointer", fontSize: 20 }}>←</button>}
    <div style={{ flex: 1 }}>
      <div style={{ fontWeight: 600, fontSize: 18 }}>{title}</div>
      {subtitle && <div style={{ fontSize: 12, opacity: 0.8 }}>{subtitle}</div>}
    </div>
    {actions}
  </div>
);

const Card = ({ children, style, onClick }) => (
  <div onClick={onClick} style={{ background: "#fff", borderRadius: 12, boxShadow: "0 1px 3px rgba(0,0,0,0.12)", padding: 16, cursor: onClick ? "pointer" : "default", ...style }}>
    {children}
  </div>
);

const StatCard = ({ icon, label, value, color, style }) => (
  <Card style={{ textAlign: "center", background: color + "18", flex: 1, ...style }}>
    <div style={{ fontSize: 24 }}>{icon}</div>
    <div style={{ fontSize: 22, fontWeight: 700, color }}>{value}</div>
    <div style={{ fontSize: 12, color: COLORS.onSurfaceVariant }}>{label}</div>
  </Card>
);

const Chip = ({ label, selected, color, onClick }) => (
  <button onClick={onClick} style={{
    padding: "6px 14px", borderRadius: 20, fontSize: 12, fontWeight: 600,
    background: selected ? (color || COLORS.primary) + "22" : "transparent",
    color: selected ? (color || COLORS.primary) : COLORS.onSurfaceVariant,
    border: `1px solid ${selected ? (color || COLORS.primary) : "#ccc"}`,
    cursor: onClick ? "pointer" : "default",
  }}>{label}</button>
);

const OutlinedBtn = ({ children, onClick, style }) => (
  <button onClick={onClick} style={{
    padding: "10px 16px", borderRadius: 20, background: "transparent",
    border: `1px solid ${COLORS.primary}`, color: COLORS.primary,
    cursor: "pointer", fontWeight: 500, display: "flex", alignItems: "center", justifyContent: "center", gap: 6, ...style
  }}>{children}</button>
);

const FilledBtn = ({ children, onClick, color, style }) => (
  <button onClick={onClick} style={{
    padding: "10px 16px", borderRadius: 20, background: color || COLORS.primary,
    border: "none", color: "#fff", cursor: "pointer", fontWeight: 600,
    display: "flex", alignItems: "center", justifyContent: "center", gap: 6, ...style
  }}>{children}</button>
);

const FAB = ({ icon, onClick }) => (
  <button onClick={onClick} style={{
    position: "absolute", bottom: 72, right: 16, width: 56, height: 56, borderRadius: 16,
    background: COLORS.primary, color: "#fff", border: "none", fontSize: 24,
    cursor: "pointer", boxShadow: "0 4px 12px rgba(0,0,0,0.25)", zIndex: 10
  }}>{icon}</button>
);

const TextField = ({ label, value, prefix, suffix, multiline, style }) => (
  <div style={{ ...style }}>
    <label style={{ fontSize: 12, color: COLORS.onSurfaceVariant }}>{label}</label>
    <div style={{
      border: `1px solid #ccc`, borderRadius: 4, padding: "8px 12px",
      display: "flex", alignItems: "center", gap: 4, marginTop: 2,
      minHeight: multiline ? 60 : "auto"
    }}>
      {prefix && <span style={{ color: COLORS.onSurfaceVariant }}>{prefix}</span>}
      <span style={{ flex: 1, color: value ? COLORS.onSurface : "#aaa" }}>{value || label}</span>
      {suffix && <span style={{ color: COLORS.onSurfaceVariant }}>{suffix}</span>}
    </div>
  </div>
);

const SectionHeader = ({ icon, title }) => (
  <div style={{ display: "flex", alignItems: "center", gap: 8, padding: "8px 0 4px" }}>
    {icon && <span style={{ color: COLORS.primary }}>{icon}</span>}
    <span style={{ fontWeight: 700, color: COLORS.primary }}>{title}</span>
  </div>
);

const Divider = () => <hr style={{ border: "none", borderTop: "1px solid #e0e0e0", margin: "8px 0" }} />;

const Badge = ({ count }) => count > 0 ? (
  <span style={{ background: COLORS.error, color: "#fff", borderRadius: 10, padding: "2px 8px", fontSize: 11, fontWeight: 700 }}>{count}</span>
) : null;


// ═══════════════════════════════════════════════════════════════════════════
// SCREENS
// ═══════════════════════════════════════════════════════════════════════════

// ── 1. Dashboard ─────────────────────────────────────────────────────────
const DashboardScreen = ({ nav }) => {
  const activeJobs = MOCK.jobs.filter(j => j.status !== "COMPLETE");
  const todayRevenue = MOCK.payments.reduce((s, p) => s + p.amountCents, 0);
  const unpaid = MOCK.invoices.filter(i => i.status === "INVOICE");
  const outstanding = unpaid.reduce((s, i) => s + i.totalCents, 0) - MOCK.payments.reduce((s, p) => s + p.amountCents, 0);

  return (
    <div style={{ flex: 1, overflow: "auto" }}>
      <TopBar title="Phillips Mobile Automotive" subtitle={today()} />
      <div style={{ padding: 16, display: "flex", flexDirection: "column", gap: 12 }}>
        {/* Top row: Active Jobs + Today's Revenue */}
        <div style={{ display: "flex", gap: 12 }}>
          <StatCard icon="🔧" label="Active Jobs" value={activeJobs.length} color={COLORS.enRoute} />
          <StatCard icon="💰" label="Today's Revenue" value={fmt(todayRevenue)} color={COLORS.revenue} />
        </div>

        {/* Second row: Appointments + Unpaid Invoices */}
        <div style={{ display: "flex", gap: 12 }}>
          <StatCard icon="📅" label="Today's Appts" value={MOCK.appointments.length} color="#9C27B0" />
          <StatCard icon="🧾" label="Unpaid Invoices" value={unpaid.length} color={unpaid.length > 0 ? COLORS.warning : COLORS.scheduled} />
        </div>

        {/* Outstanding balance alert */}
        {outstanding > 0 && (
          <Card style={{ background: COLORS.warningBg }}>
            <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
              <span style={{ fontSize: 24, color: COLORS.warning }}>⚠️</span>
              <div>
                <div style={{ fontWeight: 700 }}>Outstanding Balance</div>
                <div style={{ fontSize: 22, fontWeight: 700, color: COLORS.warning }}>{fmt(outstanding)}</div>
              </div>
            </div>
          </Card>
        )}

        {/* Today's Jobs */}
        <div style={{ fontWeight: 700, color: COLORS.primary }}>Today's Jobs</div>
        {activeJobs.length > 0 ? activeJobs.map(job => {
          const c = MOCK.customers.find(x => x.id === job.customerId);
          return (
            <Card key={job.id} onClick={() => nav("jobDetail", { jobId: job.id })}>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <div>
                  <div style={{ fontWeight: 500 }}>{job.description || "Job"}</div>
                  <div style={{ fontSize: 12, color: COLORS.onSurfaceVariant }}>{c?.firstName} {c?.lastName}</div>
                </div>
                <Chip label={job.status.replace("_", " ")} selected color={statusColor(job.status)} />
              </div>
            </Card>
          );
        }) : (
          <Card style={{ background: COLORS.surfaceVariant, textAlign: "center" }}>
            <div>No active jobs</div>
            <div style={{ fontSize: 12, color: COLORS.onSurfaceVariant }}>Schedule an appointment to get started</div>
          </Card>
        )}
      </div>
    </div>
  );
};

// ── 2. Schedule ──────────────────────────────────────────────────────────
const ScheduleScreen = ({ nav }) => {
  const [viewMode, setViewMode] = useState("Day");
  return (
    <div style={{ flex: 1, overflow: "auto", position: "relative" }}>
      <TopBar title="Schedule" />
      <div style={{ padding: "4px 16px", display: "flex", gap: 8 }}>
        {["Day", "Week", "Month"].map(m => <Chip key={m} label={m} selected={viewMode === m} onClick={() => setViewMode(m)} />)}
      </div>
      {/* Date navigation */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "4px 16px" }}>
        <button style={{ background: "none", border: "none", fontSize: 18, cursor: "pointer" }}>←</button>
        <div style={{ textAlign: "center" }}>
          <div style={{ fontWeight: 700 }}>{today()}</div>
        </div>
        <button style={{ background: "none", border: "none", fontSize: 18, cursor: "pointer" }}>→</button>
      </div>
      {/* Appointment list */}
      <div style={{ padding: "0 16px", display: "flex", flexDirection: "column", gap: 4 }}>
        {MOCK.appointments.map(appt => {
          const c = MOCK.customers.find(x => x.id === appt.customerId);
          const d = new Date(appt.startEpoch);
          const setup = new Date(appt.startEpoch - 15 * 60000);
          return (
            <Card key={appt.id} onClick={() => nav("appointmentForm", { appointmentId: appt.id })} style={{ boxShadow: "0 1px 2px rgba(0,0,0,0.1)" }}>
              <div style={{ display: "flex", gap: 12 }}>
                <div style={{ width: 72, textAlign: "center" }}>
                  <div style={{ fontSize: 11, color: COLORS.onSurfaceVariant }}>{formatTime(setup.getHours(), setup.getMinutes())}</div>
                  <div style={{ fontWeight: 700 }}>{formatTime(d.getHours(), d.getMinutes())}</div>
                  <div style={{ fontSize: 10, color: COLORS.onSurfaceVariant }}>setup ↑</div>
                </div>
                <div style={{ flex: 1 }}>
                  <div style={{ fontWeight: 500 }}>{c?.firstName} {c?.lastName}</div>
                  <div style={{ fontSize: 12, color: COLORS.onSurfaceVariant }}>📍 {appt.address}</div>
                  {appt.notes && <div style={{ fontSize: 12 }}>{appt.notes}</div>}
                </div>
                <div style={{ fontSize: 11, fontWeight: 700, color: statusColor(appt.status) }}>{appt.status}</div>
              </div>
            </Card>
          );
        })}
        {MOCK.appointments.length === 0 && (
          <div style={{ textAlign: "center", padding: 32, color: COLORS.onSurfaceVariant }}>
            <div style={{ fontSize: 32 }}>📅</div>
            <div>No appointments</div>
          </div>
        )}
      </div>
      <FAB icon="+" onClick={() => nav("appointmentForm")} />
    </div>
  );
};

// ── 3. Appointment Form ──────────────────────────────────────────────────
const AppointmentFormScreen = ({ nav }) => (
  <div style={{ flex: 1, overflow: "auto", position: "relative" }}>
    <TopBar title="New Appointment" onBack={() => nav("schedule")} />
    <div style={{ padding: 16, display: "flex", flexDirection: "column", gap: 8 }}>
      <SectionHeader title="Customer" />
      <Card style={{ background: COLORS.secondaryContainer }}>
        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          <span>👤</span>
          <div style={{ flex: 1 }}>
            <div style={{ fontWeight: 500 }}>James Wilson</div>
            <div style={{ fontSize: 12 }}>843-555-0142</div>
          </div>
          <button style={{ color: COLORS.primary, background: "none", border: "none", cursor: "pointer" }}>Change</button>
        </div>
      </Card>

      <Divider />
      <SectionHeader title="Vehicle" />
      <div style={{ display: "flex", gap: 8 }}>
        <Chip label="🚗 2019 Toyota Camry" selected />
        <Chip label="🚗 2022 Honda CR-V" />
      </div>

      <Divider />
      <SectionHeader title="Date & Time" />
      <div style={{ display: "flex", gap: 8 }}>
        <TextField label="Date" value="May 27, 2026" style={{ flex: 1 }} />
        <TextField label="Time" value="10:00 AM" style={{ flex: 1 }} />
      </div>
      <div style={{ fontSize: 12, color: COLORS.tertiary }}>⏱ Arrive by 9:45 AM (15 min setup)</div>

      <TextField label="Service Address *" value="1420 King St, Charleston SC 29403" multiline />

      <SectionHeader title="Status" />
      <div style={{ display: "flex", gap: 8 }}>
        {["Pending", "Confirmed", "Cancelled"].map(s => <Chip key={s} label={s} selected={s === "Confirmed"} />)}
      </div>

      <TextField label="Notes" value="Oil change + brake inspection" multiline />
    </div>
    <FAB icon="💾" onClick={() => nav("schedule")} />
  </div>
);

// ── 4. Job Queue ─────────────────────────────────────────────────────────
const JobQueueScreen = ({ nav }) => {
  const active = MOCK.jobs.filter(j => j.status !== "COMPLETE");
  const completed = MOCK.jobs.filter(j => j.status === "COMPLETE");

  return (
    <div style={{ flex: 1, overflow: "auto" }}>
      <TopBar title="Job Queue" />
      <div style={{ padding: "0 0 16px" }}>
        {active.length > 0 && (
          <>
            <div style={{ padding: "8px 16px", fontWeight: 700, fontSize: 13, color: COLORS.primary }}>Active</div>
            {active.map(job => <JobCard key={job.id} job={job} onClick={() => nav("jobDetail", { jobId: job.id })} />)}
          </>
        )}
        {completed.length > 0 && (
          <>
            <Divider />
            <div style={{ padding: "8px 16px", fontWeight: 700, fontSize: 13, color: COLORS.onSurfaceVariant }}>Completed</div>
            {completed.map(job => <JobCard key={job.id} job={job} onClick={() => nav("jobDetail", { jobId: job.id })} />)}
          </>
        )}
        {active.length === 0 && completed.length === 0 && (
          <div style={{ textAlign: "center", padding: 48, color: COLORS.onSurfaceVariant }}>
            <div style={{ fontSize: 32 }}>🔧</div>
            <div>No jobs yet</div>
            <div style={{ fontSize: 12 }}>Schedule an appointment to create a job</div>
          </div>
        )}
      </div>
    </div>
  );
};

const JobCard = ({ job, onClick }) => {
  const c = MOCK.customers.find(x => x.id === job.customerId);
  const v = MOCK.vehicles.find(x => x.id === job.vehicleId);
  return (
    <div onClick={onClick} style={{ margin: "0 16px 4px", cursor: "pointer" }}>
      <Card style={{ boxShadow: "0 1px 2px rgba(0,0,0,0.1)" }}>
        <div style={{ display: "flex", gap: 12 }}>
          <div style={{ width: 60, textAlign: "center" }}>
            <div style={{ fontSize: 11, fontWeight: 700, color: statusColor(job.status), whiteSpace: "pre-line" }}>
              {job.status.replace("_", "\n")}
            </div>
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ fontWeight: 500 }}>{c?.firstName} {c?.lastName}</div>
            <div style={{ fontSize: 12, color: COLORS.onSurfaceVariant }}>🚗 {v ? `${v.year || ""} ${v.make} ${v.model}` : "—"}</div>
            {job.description && <div style={{ fontSize: 12 }}>{job.description}</div>}
          </div>
        </div>
      </Card>
    </div>
  );
};

// ── 5. Job Detail ────────────────────────────────────────────────────────
const JobDetailScreen = ({ nav, params }) => {
  const job = MOCK.jobs.find(j => j.id === params?.jobId) || MOCK.jobs[0];
  const c = MOCK.customers.find(x => x.id === job.customerId);
  const v = MOCK.vehicles.find(x => x.id === job.vehicleId);
  const invoices = MOCK.invoices.filter(i => i.jobId === job.id);
  const clock = MOCK.clockEntries.filter(e => e.jobId === job.id);
  const mileage = MOCK.mileageEntries.filter(e => e.jobId === job.id);
  const isClockedIn = clock.some(e => !e.clockOut);

  const nextLabel = { SCHEDULED: "Start Route", EN_ROUTE: "Arrived On Site", ON_SITE: "Mark Complete" }[job.status] || "";

  return (
    <div style={{ flex: 1, overflow: "auto" }}>
      <TopBar title="Job Detail" onBack={() => nav("jobs")} />
      <div style={{ padding: 16, display: "flex", flexDirection: "column", gap: 12 }}>
        {/* Header: Customer + Vehicle */}
        <Card>
          {c && (
            <div onClick={() => nav("customerDetail", { customerId: c.id })} style={{ cursor: "pointer" }}>
              <div style={{ fontSize: 20, fontWeight: 700 }}>{c.firstName} {c.lastName}</div>
              <div>{c.phone}</div>
              {c.address && <div style={{ fontSize: 12, color: COLORS.onSurfaceVariant }}>{c.address}</div>}
            </div>
          )}
          {v && (
            <div style={{ display: "flex", alignItems: "center", gap: 8, marginTop: 8 }}>
              <span style={{ color: COLORS.tertiary }}>🚗</span>
              <span style={{ fontWeight: 500 }}>{v.year || ""} {v.make} {v.model}</span>
            </div>
          )}
        </Card>

        {/* Status Controls */}
        <Card style={{ background: statusColor(job.status) + "18", textAlign: "center" }}>
          <div style={{ fontWeight: 700, color: statusColor(job.status), fontSize: 16 }}>
            {job.status.replace("_", " ")}
          </div>
          {job.status !== "COMPLETE" && (
            <FilledBtn color={statusColor(job.status)} style={{ marginTop: 8, width: "100%" }}>
              → {nextLabel}
            </FilledBtn>
          )}
        </Card>

        {/* Time Clock */}
        <Divider />
        <SectionHeader icon="⏱" title="Time Clock" />
        {isClockedIn && (
          <div style={{ textAlign: "center", fontSize: 28, fontWeight: 700, color: COLORS.onSite }}>1:30:00</div>
        )}
        <div style={{ display: "flex", justifyContent: "center" }}>
          {isClockedIn ? (
            <FilledBtn color={COLORS.danger}>⏹ Clock Out</FilledBtn>
          ) : (
            <FilledBtn color={COLORS.complete}>▶ Clock In</FilledBtn>
          )}
        </div>
        {clock.length > 0 && clock.map(entry => (
          <div key={entry.id} style={{ display: "flex", justifyContent: "space-between", fontSize: 12 }}>
            <span>{formatTime(new Date(entry.clockIn).getHours(), new Date(entry.clockIn).getMinutes())} → {entry.clockOut ? formatTime(new Date(entry.clockOut).getHours(), new Date(entry.clockOut).getMinutes()) : "active"}</span>
          </div>
        ))}

        {/* Mileage */}
        <Divider />
        <SectionHeader icon="🏎" title="Mileage" />
        {mileage.map(e => (
          <div key={e.id} style={{ display: "flex", justifyContent: "space-between", fontSize: 13 }}>
            <span>{e.miles.toFixed(1)} mi</span>
            {e.purpose && <span style={{ color: COLORS.onSurfaceVariant }}>{e.purpose}</span>}
          </div>
        ))}
        {mileage.length > 0 && (
          <div style={{ textAlign: "right", fontWeight: 700, fontSize: 13 }}>
            Total: {mileage.reduce((s, e) => s + e.miles, 0).toFixed(1)} mi
          </div>
        )}
        <OutlinedBtn style={{ width: "100%" }}>+ Log Mileage</OutlinedBtn>

        {/* Invoice Link */}
        <Divider />
        {invoices.length > 0 ? invoices.map(inv => (
          <OutlinedBtn key={inv.id} onClick={() => nav("invoiceDetail", { invoiceId: inv.id })} style={{ width: "100%" }}>
            🧾 View Invoice ({inv.status})
          </OutlinedBtn>
        )) : (
          <OutlinedBtn onClick={() => nav("invoiceBuilder", { jobId: job.id })} style={{ width: "100%" }}>
            🧾 Create Estimate
          </OutlinedBtn>
        )}

        {/* Notes */}
        {job.description && (
          <>
            <Divider />
            <div style={{ fontWeight: 700, fontSize: 13 }}>Notes</div>
            <div style={{ fontSize: 13 }}>{job.description}</div>
          </>
        )}
      </div>
    </div>
  );
};

// ── 6. Customer List ─────────────────────────────────────────────────────
const CustomerListScreen = ({ nav }) => (
  <div style={{ flex: 1, overflow: "auto", position: "relative" }}>
    <TopBar title="Customers" onBack={() => nav("dashboard")} />

    {/* Quick-links to other "More" destinations */}
    <div style={{ padding: "8px 16px", display: "flex", gap: 8, flexWrap: "wrap" }}>
      <Chip label="📊 Analytics" onClick={() => nav("analytics")} />
      <Chip label="💵 Billing" onClick={() => nav("billingHub")} />
      <Chip label="⚙️ Settings" onClick={() => nav("settings")} />
    </div>

    <div style={{ padding: "0 16px 8px" }}>
      <TextField label="Search customers" value="" prefix="🔍" />
    </div>
    <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
      {MOCK.customers.map(c => (
        <div key={c.id} style={{ margin: "0 16px" }}>
          <Card onClick={() => nav("customerDetail", { customerId: c.id })} style={{ boxShadow: "0 1px 2px rgba(0,0,0,0.1)" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 16 }}>
              <span style={{ color: COLORS.primary, fontSize: 20 }}>👤</span>
              <div>
                <div style={{ fontWeight: 500 }}>{c.firstName} {c.lastName}</div>
                <div style={{ fontSize: 13, color: COLORS.onSurfaceVariant }}>{c.phone}</div>
                {c.address && <div style={{ fontSize: 12, color: COLORS.onSurfaceVariant }}>{c.address}</div>}
              </div>
            </div>
          </Card>
        </div>
      ))}
    </div>
    <FAB icon="+" onClick={() => nav("customerForm")} />
  </div>
);

// ── 7. Customer Detail ───────────────────────────────────────────────────
const CustomerDetailScreen = ({ nav, params }) => {
  const c = MOCK.customers.find(x => x.id === params?.customerId) || MOCK.customers[0];
  const vehicles = MOCK.vehicles.filter(v => v.customerId === c.id);
  const jobs = MOCK.jobs.filter(j => j.customerId === c.id);
  const invoices = MOCK.invoices.filter(i => i.customerId === c.id);
  const unpaidCents = invoices.filter(i => i.status === "INVOICE").reduce((s, i) => s + i.totalCents, 0);

  return (
    <div style={{ flex: 1, overflow: "auto" }}>
      <TopBar title={`${c.firstName} ${c.lastName}`} onBack={() => nav("customers")}
        actions={<button onClick={() => nav("customerForm", { customerId: c.id })} style={{ background: "none", border: "none", cursor: "pointer", fontSize: 18 }}>✏️</button>} />
      <div style={{ padding: 16, display: "flex", flexDirection: "column", gap: 12 }}>
        {/* Contact Info */}
        <Card>
          <SectionHeader title="Contact Information" />
          <div style={{ fontSize: 13 }}>
            <div><b>Phone:</b> {c.phone}</div>
            {c.email && <div><b>Email:</b> {c.email}</div>}
            {c.address && <div><b>Address:</b> {c.address}</div>}
            {c.notes && <div><b>Notes:</b> {c.notes}</div>}
          </div>
        </Card>

        {/* Summary Stats */}
        <div style={{ display: "flex", gap: 8 }}>
          <StatCard icon="🔧" label="Jobs" value={jobs.length} color={COLORS.secondary} style={{ background: COLORS.secondaryContainer }} />
          <StatCard icon="🧾" label="Balance" value={fmt(unpaidCents)} color={COLORS.secondary} style={{ background: COLORS.secondaryContainer }} />
        </div>

        {/* Vehicles */}
        <Card>
          <SectionHeader title="Vehicles" />
          {vehicles.length === 0 ? (
            <div style={{ fontSize: 13, color: COLORS.onSurfaceVariant }}>No vehicles registered</div>
          ) : vehicles.map(v => (
            <div key={v.id} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "4px 0" }}>
              <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                <span style={{ color: COLORS.tertiary }}>🚗</span>
                <div>
                  <div style={{ fontWeight: 500 }}>{v.year || ""} {v.make} {v.model}</div>
                  {v.engine && <div style={{ fontSize: 12, color: COLORS.onSurfaceVariant }}>{v.engine}</div>}
                </div>
              </div>
              <button onClick={() => nav("vehicleForm", { customerId: c.id, vehicleId: v.id })} style={{ background: "none", border: "none", cursor: "pointer" }}>✏️</button>
            </div>
          ))}
          <OutlinedBtn onClick={() => nav("vehicleForm", { customerId: c.id })} style={{ width: "100%", marginTop: 8 }}>+ Add Vehicle</OutlinedBtn>
        </Card>

        {/* Quick Actions */}
        <div style={{ display: "flex", gap: 8 }}>
          <OutlinedBtn style={{ flex: 1 }}>View Jobs</OutlinedBtn>
          <OutlinedBtn style={{ flex: 1 }}>View Invoices</OutlinedBtn>
        </div>
      </div>
    </div>
  );
};

// ── 8. Customer Form ─────────────────────────────────────────────────────
const CustomerFormScreen = ({ nav, params }) => {
  const existing = params?.customerId ? MOCK.customers.find(c => c.id === params.customerId) : null;
  return (
    <div style={{ flex: 1, overflow: "auto", position: "relative" }}>
      <TopBar title={existing ? "Edit Customer" : "New Customer"} onBack={() => nav("customers")} />
      <div style={{ padding: 16, display: "flex", flexDirection: "column", gap: 8 }}>
        <TextField label="First Name *" value={existing?.firstName || ""} />
        <TextField label="Last Name *" value={existing?.lastName || ""} />
        <TextField label="Phone Number *" value={existing?.phone || ""} />
        <TextField label="Email" value={existing?.email || ""} />
        <TextField label="Address" value={existing?.address || ""} multiline />
        <TextField label="Notes" value={existing?.notes || ""} multiline />
      </div>
      <FAB icon="💾" onClick={() => nav("customers")} />
    </div>
  );
};

// ── 9. Vehicle Form ──────────────────────────────────────────────────────
const VehicleFormScreen = ({ nav, params }) => {
  const existing = params?.vehicleId ? MOCK.vehicles.find(v => v.id === params.vehicleId) : null;
  return (
    <div style={{ flex: 1, overflow: "auto", position: "relative" }}>
      <TopBar title={existing ? "Edit Vehicle" : "New Vehicle"} onBack={() => nav("customerDetail", { customerId: params?.customerId })} />
      <div style={{ padding: 16, display: "flex", flexDirection: "column", gap: 8 }}>
        <div style={{ display: "flex", gap: 8 }}>
          <TextField label="Year" value={existing?.year?.toString() || ""} style={{ flex: 1 }} />
          <TextField label="Make *" value={existing?.make || ""} style={{ flex: 1 }} />
        </div>
        <TextField label="Model *" value={existing?.model || ""} />
        <TextField label="Engine" value={existing?.engine || ""} />
        <TextField label="VIN" value={existing?.vin || ""} />
        <TextField label="Color" value={existing?.color || ""} />
        <TextField label="Notes" value="" multiline />
      </div>
      <FAB icon="💾" onClick={() => nav("customerDetail", { customerId: params?.customerId })} />
    </div>
  );
};

// ── 10. Conversation List ────────────────────────────────────────────────
const ConversationListScreen = ({ nav }) => (
  <div style={{ flex: 1, overflow: "auto" }}>
    <TopBar title="Messages" />
    {MOCK.conversations.length === 0 ? (
      <div style={{ textAlign: "center", padding: 48, color: COLORS.onSurfaceVariant }}>
        <div style={{ fontSize: 32 }}>💬</div>
        <div>No conversations</div>
        <div style={{ fontSize: 12 }}>Incoming SMS will appear here</div>
      </div>
    ) : (
      <div style={{ display: "flex", flexDirection: "column" }}>
        {MOCK.conversations.map(conv => {
          const c = conv.customerId ? MOCK.customers.find(x => x.id === conv.customerId) : null;
          const name = c ? `${c.firstName} ${c.lastName}` : (conv.displayName || conv.phoneNumber);
          const lastTime = conv.lastMessageEpoch ? formatTime(new Date(conv.lastMessageEpoch).getHours(), new Date(conv.lastMessageEpoch).getMinutes()) : "";
          return (
            <div key={conv.id} style={{ margin: "4px 16px" }}>
              <Card onClick={() => nav("conversationDetail", { convId: conv.id })}
                style={{ boxShadow: conv.unreadCount > 0 ? "0 2px 6px rgba(0,0,0,0.12)" : "0 0 2px rgba(0,0,0,0.08)" }}>
                <div style={{ display: "flex", alignItems: "center", gap: 16 }}>
                  <span style={{ color: conv.unreadCount > 0 ? COLORS.primary : COLORS.onSurfaceVariant, fontSize: 20 }}>
                    {c ? "👤" : "💬"}
                  </span>
                  <div style={{ flex: 1 }}>
                    <div style={{ display: "flex", justifyContent: "space-between" }}>
                      <span style={{ fontWeight: conv.unreadCount > 0 ? 700 : 400 }}>{name}</span>
                      <span style={{ fontSize: 12, color: COLORS.onSurfaceVariant }}>{lastTime}</span>
                    </div>
                    {c && <div style={{ fontSize: 12, color: COLORS.onSurfaceVariant }}>{conv.phoneNumber}</div>}
                  </div>
                  <Badge count={conv.unreadCount} />
                </div>
              </Card>
            </div>
          );
        })}
      </div>
    )}
  </div>
);

// ── 11. Conversation Detail ──────────────────────────────────────────────
const ConversationDetailScreen = ({ nav, params }) => {
  const conv = MOCK.conversations.find(x => x.id === params?.convId) || MOCK.conversations[0];
  const c = conv.customerId ? MOCK.customers.find(x => x.id === conv.customerId) : null;
  const activeJobs = c ? MOCK.jobs.filter(j => j.customerId === c.id && j.status !== "COMPLETE").length : 0;

  return (
    <div style={{ flex: 1, display: "flex", flexDirection: "column" }}>
      <TopBar title={conv.displayName || conv.phoneNumber} subtitle={conv.displayName ? conv.phoneNumber : null} onBack={() => nav("comms")} />

      {/* Context Header */}
      <Card style={{ margin: 16, background: COLORS.surfaceVariant }}>
        {c ? (
          <>
            <div style={{ fontWeight: 700 }}>{c.firstName} {c.lastName}</div>
            <div style={{ fontSize: 13 }}>Active Jobs: {activeJobs}</div>
            <div style={{ marginTop: 8 }}>
              <FilledBtn style={{ width: "100%" }}>📅 New Appt</FilledBtn>
            </div>
          </>
        ) : (
          <>
            <div style={{ fontWeight: 700 }}>Unknown Lead</div>
            <FilledBtn style={{ width: "100%", marginTop: 8 }} onClick={() => nav("customerForm")}>👤+ Create Customer</FilledBtn>
          </>
        )}
      </Card>

      {/* Messages */}
      <div style={{ flex: 1, overflow: "auto", padding: "0 16px", display: "flex", flexDirection: "column", gap: 4 }}>
        {conv.messages.map(msg => {
          const isOut = !msg.isInbound;
          const t = new Date(msg.ts);
          return (
            <div key={msg.id} style={{ display: "flex", justifyContent: isOut ? "flex-end" : "flex-start" }}>
              <div style={{
                maxWidth: 280, padding: 12,
                borderRadius: isOut ? "16px 16px 4px 16px" : "16px 16px 16px 4px",
                background: isOut ? COLORS.primary : COLORS.surfaceVariant,
                color: isOut ? "#fff" : COLORS.onSurfaceVariant,
              }}>
                <div style={{ fontSize: 14 }}>{msg.body}</div>
                <div style={{ fontSize: 10, textAlign: "right", opacity: 0.7, marginTop: 2 }}>
                  {formatTime(t.getHours(), t.getMinutes())}
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Compose Bar */}
      <div style={{ display: "flex", alignItems: "center", padding: 8, gap: 8, borderTop: "1px solid #e0e0e0" }}>
        <div style={{ flex: 1, border: "1px solid #ccc", borderRadius: 24, padding: "8px 16px", fontSize: 14, color: "#aaa" }}>
          Type a message…
        </div>
        <button style={{ background: "none", border: "none", fontSize: 20, color: COLORS.onSurfaceVariant }}>📤</button>
      </div>
    </div>
  );
};

// ── 12. Billing Hub ──────────────────────────────────────────────────────
const BillingHubScreen = ({ nav }) => {
  const todayRev = MOCK.payments.reduce((s, p) => s + p.amountCents, 0);
  const unpaid = MOCK.invoices.filter(i => i.status === "INVOICE");
  const outstanding = unpaid.reduce((s, i) => s + i.totalCents, 0) - todayRev;
  const todayExpenses = MOCK.expenses.reduce((s, e) => s + e.amountCents, 0);

  return (
    <div style={{ flex: 1, overflow: "auto" }}>
      <TopBar title="Billing" onBack={() => nav("customers")} />
      <div style={{ padding: 16, display: "flex", flexDirection: "column", gap: 12 }}>
        {/* Revenue Summary */}
        <SectionHeader title="Revenue" />
        <Card style={{ background: COLORS.revenue + "14" }}>
          {[["Today", todayRev], ["This Week", todayRev * 3], ["This Month", todayRev * 12]].map(([l, v]) => (
            <div key={l} style={{ display: "flex", justifyContent: "space-between", padding: "2px 0" }}>
              <span>{l}</span>
              <span style={{ fontWeight: 700, color: COLORS.revenue }}>{fmt(v)}</span>
            </div>
          ))}
        </Card>

        {/* Outstanding Invoices */}
        {unpaid.length > 0 && (
          <Card style={{ background: COLORS.warningBg }}>
            <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
              <span style={{ fontSize: 24, color: COLORS.warning }}>⚠️</span>
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 700 }}>{unpaid.length} Unpaid Invoice{unpaid.length !== 1 ? "s" : ""}</div>
                <div style={{ fontSize: 20, fontWeight: 700, color: COLORS.warning }}>{fmt(outstanding)}</div>
              </div>
            </div>
          </Card>
        )}

        {/* Today's Expenses */}
        <Divider />
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <SectionHeader title="Today's Expenses" />
          <span style={{ fontWeight: 700 }}>{fmt(todayExpenses)}</span>
        </div>
        {MOCK.expenses.map(e => (
          <Card key={e.id}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 500 }}>{e.description}</div>
                <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                  <Chip label={e.category} selected />
                  {e.vendor && <span style={{ fontSize: 12, color: COLORS.onSurfaceVariant }}>{e.vendor}</span>}
                </div>
              </div>
              <span style={{ fontWeight: 700, color: COLORS.error }}>{fmt(e.amountCents)}</span>
            </div>
          </Card>
        ))}

        {/* Quick Actions */}
        <Divider />
        <div style={{ display: "flex", gap: 8 }}>
          <OutlinedBtn onClick={() => nav("expenseForm")} style={{ flex: 1 }}>+ Log Expense</OutlinedBtn>
          <OutlinedBtn onClick={() => nav("paymentLog")} style={{ flex: 1 }}>💳 Payment Log</OutlinedBtn>
        </div>
      </div>
    </div>
  );
};

// ── 13. Invoice Builder ──────────────────────────────────────────────────
const InvoiceBuilderScreen = ({ nav, params }) => {
  const inv = MOCK.invoices.find(i => i.jobId === params?.jobId) || MOCK.invoices[1];
  const c = MOCK.customers.find(x => x.id === inv.customerId);
  const job = MOCK.jobs.find(j => j.id === inv.jobId);
  const v = job ? MOCK.vehicles.find(x => x.id === job.vehicleId) : null;
  const titleMap = { ESTIMATE: "Estimate / Work Order", INVOICE: "Invoice", PAID: "Invoice (Paid)", VOID: "Invoice (Void)" };
  const typeColor = { LABOR: COLORS.enRoute, PARTS: COLORS.warning, MISC: COLORS.scheduled };

  const laborTotal = inv.lineItems.filter(i => i.type === "LABOR").reduce((s, i) => s + i.totalCents, 0);
  const partsTotal = inv.lineItems.filter(i => i.type === "PARTS").reduce((s, i) => s + i.totalCents, 0);
  const miscTotal = inv.lineItems.filter(i => i.type === "MISC").reduce((s, i) => s + i.totalCents, 0);
  const serviceFee = MOCK.shopProfile.serviceFeeCents;
  const taxCents = Math.round(partsTotal * MOCK.shopProfile.taxRateBasisPoints / 10000);

  return (
    <div style={{ flex: 1, overflow: "auto" }}>
      <TopBar title={titleMap[inv.status] || "Estimate"} onBack={() => nav("jobDetail", { jobId: inv.jobId })} />
      <div style={{ padding: 16, display: "flex", flexDirection: "column", gap: 8 }}>
        {/* Header */}
        <Card>
          <div style={{ fontWeight: 700, fontSize: 18 }}>{c?.firstName} {c?.lastName}</div>
          <div style={{ color: COLORS.onSurfaceVariant }}>{v ? `${v.year || ""} ${v.make} ${v.model}` : ""}</div>
        </Card>

        {/* Line Items */}
        <Divider />
        <SectionHeader title="Line Items" />
        {inv.lineItems.map((item, i) => (
          <Card key={i} style={{ boxShadow: "0 1px 2px rgba(0,0,0,0.1)" }}>
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 8 }}>
              <Chip label={item.type} selected color={typeColor[item.type]} />
              <button style={{ background: "none", border: "none", color: COLORS.error, cursor: "pointer" }}>🗑</button>
            </div>
            <TextField label="Description" value={item.desc} />
            <div style={{ display: "flex", gap: 8, marginTop: 4 }}>
              <TextField label={item.type === "LABOR" ? "Hours" : "Qty"} value={item.qty} style={{ flex: 1 }} />
              <TextField label={item.type === "LABOR" ? "$/hr" : "Cost"} value={item.unitPrice} prefix="$" style={{ flex: 1 }} />
            </div>
            <div style={{ textAlign: "right", fontSize: 12, fontWeight: 500, marginTop: 4 }}>
              Line total: {fmt(item.totalCents)}
            </div>
          </Card>
        ))}
        <div style={{ display: "flex", gap: 8 }}>
          <OutlinedBtn style={{ flex: 1 }}>+ Labor</OutlinedBtn>
          <OutlinedBtn style={{ flex: 1 }}>+ Parts</OutlinedBtn>
          <OutlinedBtn style={{ flex: 1 }}>+ Misc</OutlinedBtn>
        </div>

        {/* Service Fee */}
        <Divider />
        <TextField label="Onsite Service Fee" value={(serviceFee / 100).toFixed(2)} prefix="$" />

        {/* Totals */}
        <Divider />
        <Card style={{ background: COLORS.surfaceVariant }}>
          {[["Labor Subtotal", laborTotal], ["Parts Subtotal", partsTotal]].map(([l, v]) => (
            <div key={l} style={{ display: "flex", justifyContent: "space-between", padding: "2px 0", fontSize: 13 }}>
              <span>{l}</span><span style={{ fontWeight: 500 }}>{fmt(v)}</span>
            </div>
          ))}
          {miscTotal > 0 && (
            <div style={{ display: "flex", justifyContent: "space-between", padding: "2px 0", fontSize: 13 }}>
              <span>Misc Subtotal</span><span style={{ fontWeight: 500 }}>{fmt(miscTotal)}</span>
            </div>
          )}
          <div style={{ display: "flex", justifyContent: "space-between", padding: "2px 0", fontSize: 13 }}>
            <span>Service Fee</span><span style={{ fontWeight: 500 }}>{fmt(serviceFee)}</span>
          </div>
          <div style={{ display: "flex", justifyContent: "space-between", padding: "2px 0", fontSize: 13 }}>
            <span>Tax ({(MOCK.shopProfile.taxRateBasisPoints / 100).toFixed(2)}% on parts)</span>
            <span style={{ fontWeight: 500 }}>{fmt(taxCents)}</span>
          </div>
          <Divider />
          <div style={{ display: "flex", justifyContent: "space-between", fontSize: 20 }}>
            <span style={{ fontWeight: 700 }}>TOTAL</span>
            <span style={{ fontWeight: 700, color: COLORS.primary }}>{fmt(laborTotal + partsTotal + miscTotal + serviceFee + taxCents)}</span>
          </div>
        </Card>

        {/* Legal Clause */}
        <Divider />
        <div style={{ fontWeight: 700, fontSize: 13 }}>Terms & Conditions</div>
        <TextField label="" value="Customer authorizes the above repairs. Parts warranty per manufacturer terms. Labor warranty 30 days." multiline />

        {/* Actions */}
        <Divider />
        <div style={{ display: "flex", gap: 8 }}>
          <OutlinedBtn style={{ flex: 1 }}>💾 Save Estimate</OutlinedBtn>
          {inv.status === "ESTIMATE" && <FilledBtn style={{ flex: 1 }}>Finalize Invoice</FilledBtn>}
        </div>
      </div>
    </div>
  );
};

// ── 14. Invoice Detail ───────────────────────────────────────────────────
const InvoiceDetailScreen = ({ nav, params }) => {
  const inv = MOCK.invoices.find(i => i.id === params?.invoiceId) || MOCK.invoices[0];
  const payments = MOCK.payments.filter(p => p.invoiceId === inv.id);
  const totalPaid = payments.reduce((s, p) => s + p.amountCents, 0);
  const remaining = inv.totalCents - totalPaid;

  return (
    <div style={{ flex: 1, overflow: "auto" }}>
      <TopBar title={`Invoice ${inv.status}`} onBack={() => nav("billingHub")} />
      <div style={{ padding: 16, display: "flex", flexDirection: "column", gap: 12 }}>
        {/* Totals */}
        <Card style={{ background: COLORS.surfaceVariant }}>
          {[["Total", inv.totalCents], ["Paid", totalPaid]].map(([l, v]) => (
            <div key={l} style={{ display: "flex", justifyContent: "space-between", padding: "2px 0" }}>
              <span>{l}</span>
              <span style={{ fontWeight: l === "Total" ? 700 : 400, color: l === "Paid" ? COLORS.primary : "inherit" }}>{fmt(v)}</span>
            </div>
          ))}
          <Divider />
          <div style={{ display: "flex", justifyContent: "space-between" }}>
            <span style={{ fontWeight: 700 }}>Remaining</span>
            <span style={{ fontWeight: 700, color: remaining > 0 ? COLORS.error : COLORS.primary }}>{fmt(remaining)}</span>
          </div>
        </Card>

        {/* Payments */}
        {payments.length > 0 && (
          <>
            <div style={{ fontWeight: 700, fontSize: 13 }}>Payment History</div>
            {payments.map(p => (
              <Card key={p.id}>
                <div style={{ fontWeight: 500 }}>{fmt(p.amountCents)}</div>
                <div style={{ fontSize: 12, color: COLORS.onSurfaceVariant }}>
                  {p.method} • {formatTime(new Date(p.paidAt).getHours(), new Date(p.paidAt).getMinutes())}
                </div>
                {p.ref && <div style={{ fontSize: 12 }}>Ref: {p.ref}</div>}
              </Card>
            ))}
          </>
        )}

        {remaining > 0 && <FilledBtn style={{ width: "100%" }}>💳 Log Payment</FilledBtn>}
      </div>
    </div>
  );
};

// ── 15. Expense Form ─────────────────────────────────────────────────────
const ExpenseFormScreen = ({ nav }) => (
  <div style={{ flex: 1, overflow: "auto", position: "relative" }}>
    <TopBar title="New Expense" onBack={() => nav("billingHub")} />
    <div style={{ padding: 16, display: "flex", flexDirection: "column", gap: 8 }}>
      <TextField label="Amount *" value="" prefix="$" />
      <TextField label="Description *" value="" />
      <TextField label="Vendor" value="" />
      <div style={{ fontWeight: 700, fontSize: 13 }}>Category</div>
      <div style={{ display: "flex", gap: 4, flexWrap: "wrap" }}>
        {["PARTS", "FUEL", "TOOLS", "SUPPLIES", "OTHER"].map(c => (
          <Chip key={c} label={c} selected={c === "PARTS"} />
        ))}
      </div>
      <TextField label="Notes" value="" multiline />
    </div>
    <FAB icon="💾" onClick={() => nav("billingHub")} />
  </div>
);

// ── 16. Payment Log ──────────────────────────────────────────────────────
const PaymentLogScreen = ({ nav }) => (
  <div style={{ flex: 1, overflow: "auto" }}>
    <TopBar title="Payment Log" onBack={() => nav("billingHub")} />
    <div style={{ padding: 16, display: "flex", flexDirection: "column", gap: 8 }}>
      {MOCK.payments.length === 0 ? (
        <div style={{ textAlign: "center", padding: 48, color: COLORS.onSurfaceVariant }}>
          <div style={{ fontSize: 48, opacity: 0.5 }}>💳</div>
          <div>No payments recorded</div>
        </div>
      ) : MOCK.payments.map(p => (
        <Card key={p.id}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <div style={{ fontSize: 20, fontWeight: 700 }}>{fmt(p.amountCents)}</div>
            <Chip label={p.method} selected />
          </div>
          <div style={{ fontSize: 12, color: COLORS.onSurfaceVariant }}>
            {new Date(p.paidAt).toLocaleString()}
          </div>
          {p.ref && <div style={{ fontSize: 12, color: COLORS.onSurfaceVariant }}>Ref: {p.ref}</div>}
          {p.notes && <div style={{ fontSize: 12, color: COLORS.onSurfaceVariant, marginTop: 4 }}>{p.notes}</div>}
        </Card>
      ))}
    </div>
  </div>
);

// ── 17. Shop Settings ────────────────────────────────────────────────────
const ShopSettingsScreen = ({ nav }) => {
  const s = MOCK.shopProfile;
  return (
    <div style={{ flex: 1, overflow: "auto", position: "relative" }}>
      <TopBar title="Shop Settings" onBack={() => nav("customers")} />
      <div style={{ padding: 16, display: "flex", flexDirection: "column", gap: 8 }}>
        <SectionHeader title="Business Information" />
        <TextField label="Business Name" value={s.businessName} />
        <TextField label="Business Address" value={s.businessAddress} />
        <div style={{ display: "flex", gap: 8 }}>
          <TextField label="Owner Name" value={s.ownerName} style={{ flex: 1 }} />
          <TextField label="Owner Phone" value={s.ownerPhone} style={{ flex: 1 }} />
        </div>

        <Divider />
        <SectionHeader title="Pricing & Rates" />
        <div style={{ display: "flex", gap: 8 }}>
          <TextField label="Labor Rate ($/hr)" value={(s.laborRateCents / 100).toFixed(2)} prefix="$" style={{ flex: 1 }} />
          <TextField label="Service Fee" value={(s.serviceFeeCents / 100).toFixed(2)} prefix="$" style={{ flex: 1 }} />
        </div>
        <div style={{ display: "flex", gap: 8 }}>
          <div style={{ flex: 1 }}>
            <TextField label="Parts Markup (%)" value={(s.partsMarkupBasisPoints / 100).toFixed(0)} suffix="%" />
            <div style={{ fontSize: 11, color: COLORS.onSurfaceVariant }}>140% = 1.4× multiplier</div>
          </div>
          <div style={{ flex: 1 }}>
            <TextField label="Tax Rate (%)" value={(s.taxRateBasisPoints / 100).toFixed(2)} suffix="%" />
            <div style={{ fontSize: 11, color: COLORS.onSurfaceVariant }}>Parts only (SC §117-306)</div>
          </div>
        </div>

        <Divider />
        <SectionHeader title="Legal & Identification" />
        <TextField label="Tax ID / EIN" value={s.taxId || ""} />
        <TextField label="Business License Number" value={s.licenseNumber || ""} />
      </div>
      <FAB icon="💾" onClick={() => {}} />
    </div>
  );
};

// ── 18. Analytics ────────────────────────────────────────────────────────
const AnalyticsScreen = ({ nav }) => {
  const totalMiles = MOCK.mileageEntries.reduce((s, e) => s + e.miles, 0);
  const rev = MOCK.payments.reduce((s, p) => s + p.amountCents, 0);
  const exp = MOCK.expenses.reduce((s, e) => s + e.amountCents, 0);
  const completedJobs = MOCK.jobs.filter(j => j.status === "COMPLETE");

  return (
    <div style={{ flex: 1, overflow: "auto" }}>
      <TopBar title="Analytics" onBack={() => nav("customers")} />
      <div style={{ padding: 16, display: "flex", flexDirection: "column", gap: 12 }}>
        {/* Mileage */}
        <SectionHeader icon="🚗" title="Mileage" />
        <Card>
          {[["Today", totalMiles], ["This Week", totalMiles * 5], ["This Month", totalMiles * 22]].map(([l, v]) => (
            <div key={l} style={{ display: "flex", justifyContent: "space-between", padding: "2px 0" }}>
              <span>{l}</span><span style={{ fontWeight: 700 }}>{v.toFixed(1)} mi</span>
            </div>
          ))}
        </Card>

        {/* Revenue vs Expenses */}
        <SectionHeader icon="📈" title="Revenue vs. Expenses" />
        <Card>
          <div style={{ fontWeight: 700, fontSize: 12, color: COLORS.revenue }}>Revenue</div>
          <div style={{ display: "flex", justifyContent: "space-evenly", padding: "4px 0" }}>
            {[["Today", rev], ["Week", rev * 5], ["Month", rev * 22]].map(([l, v]) => (
              <div key={l} style={{ textAlign: "center" }}>
                <div style={{ fontSize: 10, color: COLORS.onSurfaceVariant }}>{l}</div>
                <div style={{ fontWeight: 700, color: COLORS.revenue, fontSize: 12 }}>{fmt(v)}</div>
              </div>
            ))}
          </div>
          <Divider />
          <div style={{ fontWeight: 700, fontSize: 12, color: COLORS.error }}>Expenses</div>
          <div style={{ display: "flex", justifyContent: "space-evenly", padding: "4px 0" }}>
            {[["Today", exp], ["Week", exp * 5], ["Month", exp * 22]].map(([l, v]) => (
              <div key={l} style={{ textAlign: "center" }}>
                <div style={{ fontSize: 10, color: COLORS.onSurfaceVariant }}>{l}</div>
                <div style={{ fontWeight: 700, color: COLORS.error, fontSize: 12 }}>{fmt(v)}</div>
              </div>
            ))}
          </div>
          <Divider />
          <div style={{ fontWeight: 700, fontSize: 12 }}>Net</div>
          <div style={{ display: "flex", justifyContent: "space-evenly", padding: "4px 0" }}>
            {[["Today", rev - exp], ["Week", (rev - exp) * 5], ["Month", (rev - exp) * 22]].map(([l, v]) => (
              <div key={l} style={{ textAlign: "center" }}>
                <div style={{ fontSize: 10, color: COLORS.onSurfaceVariant }}>{l}</div>
                <div style={{ fontWeight: 700, color: v >= 0 ? COLORS.revenue : COLORS.danger, fontSize: 12 }}>{fmt(v)}</div>
              </div>
            ))}
          </div>
        </Card>

        {/* Efficiency */}
        <SectionHeader icon="⏱" title="Efficiency" />
        <Card>
          <div style={{ display: "flex", justifyContent: "space-between", padding: "2px 0" }}><span>Total Hours Worked</span><span style={{ fontWeight: 700 }}>1.5 hrs</span></div>
          <div style={{ display: "flex", justifyContent: "space-between", padding: "2px 0" }}><span>Completed Jobs</span><span style={{ fontWeight: 700 }}>{completedJobs.length}</span></div>
          {completedJobs.length > 0 && (
            <div style={{ display: "flex", justifyContent: "space-between", padding: "2px 0" }}><span>Avg Hours/Job</span><span style={{ fontWeight: 700 }}>{(1.5 / completedJobs.length).toFixed(1)} hrs</span></div>
          )}
        </Card>

        {/* Job History */}
        <Divider />
        <SectionHeader title="Job History" />
        {completedJobs.length === 0 ? (
          <div style={{ fontSize: 13, color: COLORS.onSurfaceVariant, padding: "8px 0" }}>No completed jobs yet</div>
        ) : completedJobs.map(job => {
          const c = MOCK.customers.find(x => x.id === job.customerId);
          return (
            <Card key={job.id} onClick={() => nav("jobDetail", { jobId: job.id })}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <div>
                  <div style={{ fontWeight: 500 }}>{c?.firstName} {c?.lastName}</div>
                  {job.description && <div style={{ fontSize: 12, color: COLORS.onSurfaceVariant }}>{job.description}</div>}
                </div>
                {job.completedAt && <div style={{ fontSize: 12, color: COLORS.onSurfaceVariant }}>{new Date(job.completedAt).toLocaleDateString()}</div>}
              </div>
            </Card>
          );
        })}
      </div>
    </div>
  );
};


// ═══════════════════════════════════════════════════════════════════════════
// APP SHELL — Bottom Nav + Screen Router
// ═══════════════════════════════════════════════════════════════════════════

const BOTTOM_TABS = [
  { key: "dashboard", label: "Dashboard", icon: "🏠" },
  { key: "schedule", label: "Schedule", icon: "📅" },
  { key: "jobs", label: "Jobs", icon: "🔧" },
  { key: "comms", label: "Comms", icon: "💬" },
  { key: "customers", label: "More", icon: "☰" },
];

export default function PhillApp() {
  const [screen, setScreen] = useState("dashboard");
  const [params, setParams] = useState({});

  const nav = (to, p) => { setScreen(to); setParams(p || {}); };

  const renderScreen = () => {
    const props = { nav, params };
    switch (screen) {
      case "dashboard":       return <DashboardScreen {...props} />;
      case "schedule":        return <ScheduleScreen {...props} />;
      case "appointmentForm": return <AppointmentFormScreen {...props} />;
      case "jobs":            return <JobQueueScreen {...props} />;
      case "jobDetail":       return <JobDetailScreen {...props} />;
      case "customers":       return <CustomerListScreen {...props} />;
      case "customerDetail":  return <CustomerDetailScreen {...props} />;
      case "customerForm":    return <CustomerFormScreen {...props} />;
      case "vehicleForm":     return <VehicleFormScreen {...props} />;
      case "comms":           return <ConversationListScreen {...props} />;
      case "conversationDetail": return <ConversationDetailScreen {...props} />;
      case "billingHub":      return <BillingHubScreen {...props} />;
      case "invoiceBuilder":  return <InvoiceBuilderScreen {...props} />;
      case "invoiceDetail":   return <InvoiceDetailScreen {...props} />;
      case "expenseForm":     return <ExpenseFormScreen {...props} />;
      case "paymentLog":      return <PaymentLogScreen {...props} />;
      case "settings":        return <ShopSettingsScreen {...props} />;
      case "analytics":       return <AnalyticsScreen {...props} />;
      default:                return <DashboardScreen {...props} />;
    }
  };

  const showBottomNav = ["dashboard", "schedule", "jobs", "comms", "customers"].includes(screen);

  return (
    <div style={{
      width: 390, height: 844, margin: "20px auto", borderRadius: 24,
      border: "3px solid #333", background: COLORS.surface,
      display: "flex", flexDirection: "column", overflow: "hidden",
      fontFamily: "'Segoe UI', 'Roboto', sans-serif", position: "relative",
    }}>
      {/* Status Bar */}
      <div style={{ height: 44, background: COLORS.primaryContainer, display: "flex", alignItems: "center", justifyContent: "center" }}>
        <span style={{ fontSize: 12, fontWeight: 600, color: COLORS.onPrimaryContainer }}>
          {new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
        </span>
      </div>

      {/* Screen Content */}
      <div style={{ flex: 1, display: "flex", flexDirection: "column", overflow: "hidden" }}>
        {renderScreen()}
      </div>

      {/* Bottom Navigation */}
      {showBottomNav && (
        <div style={{
          display: "flex", borderTop: "1px solid #e0e0e0", background: COLORS.surface,
          padding: "4px 0 8px",
        }}>
          {BOTTOM_TABS.map(tab => {
            const isActive = screen === tab.key;
            return (
              <button key={tab.key} onClick={() => nav(tab.key)}
                style={{
                  flex: 1, display: "flex", flexDirection: "column", alignItems: "center",
                  background: "none", border: "none", cursor: "pointer",
                  color: isActive ? COLORS.primary : COLORS.onSurfaceVariant, gap: 2,
                }}>
                <span style={{
                  fontSize: 20, padding: "2px 16px", borderRadius: 16,
                  background: isActive ? COLORS.secondaryContainer : "transparent",
                }}>{tab.icon}</span>
                <span style={{ fontSize: 11, fontWeight: isActive ? 600 : 400 }}>{tab.label}</span>
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}
