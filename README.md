# InvestTrack (Investment Tracker Android App)

[![Android](https://img.shields.io/badge/Platform-Android%206.0%2B%20(API%2023--36)-green.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![Build](https://img.shields.io/badge/Build-Gradle%208.13-brightgreen.svg)](https://gradle.org)

**InvestTrack** is a modern, high-performance financial investment tracking Android application designed to track, analyze, and manage personal and institutional investments across Android phones and tablets running Android 6.0 (API 23) up to the latest Android 16 (API 36).

---

## 📥 Direct APK Download

You can directly download and install the pre-built application APK:
- 📱 **[Download InvestTrack-v1.1.apk (v1.1 - Latest)](apk/InvestTrack-v1.1.apk)** *(Size: ~6.02 MB)*
  - **New**: App login password protection with PBKDF2 HMAC-SHA256 (65,536 iterations).
  - **New**: In-settings 3-stage verification flow to change login password (identical to SafeVault).
  - **New**: Instant App Lock feature from Settings.
- 📱 **[Download InvestTrack-v1.0.apk (v1.0)](apk/InvestTrack-v1.0.apk)** *(Size: ~6.04 MB)*
- **Compatibility**: Android 6.0+ (API 23 to 36) on all Android Mobile Phones & Tablets.

---

## 🌟 Key Features

### 1. App Login Password & Biometric-Grade Security
- **Master PIN Protection**: Protects your financial portfolio on app launch with a secure 4-digit numeric keypad and visual dot indicators.
- **First Launch PIN Setup**: Guided 2-step setup (`Set Master PIN` ➔ `Confirm Master PIN`) with automatic validation.
- **Military-Grade Key Derivation**: PBKDF2 with HMAC-SHA256, 65,536 key-stretching iterations, and 16-byte cryptographically secure random salt.
- **Timing-Attack Resistance**: `slowEquals` constant-time equality check ensures immunity against timing side-channel attacks.
- **Forgot / Reset PIN Option**: Secure PIN reset confirmation dialog preserving data integrity.

### 2. In-Settings "Change Login Password" (SafeVault Flow)
- **Top Header Settings Button**: Quick-access settings icon (`ic_settings`) in the app bar.
- **Exact 3-Stage Verification Machine (from SafeVault)**:
  - **Stage 1 (Verify Current)**: Authenticates the user's existing 4-digit Master PIN.
  - **Stage 2 (Enter New)**: Enforces that the new PIN differs from the current PIN.
  - **Stage 3 (Confirm New)**: Confirms the new PIN entry before committing the change.
- **Immediate App Lock**: One-tap "Lock App Now" in the Settings dialog to require Master PIN before the next access.

### 3. Complete Investment Details Tracking
- **6 Core Fields Maintained**:
  - **Investment Name**
  - **Investment Amount**
  - **Investment Date**
  - **Maturity Amount**
  - **Maturity Date**
  - **Investment Details / Notes** (Account #, Folio, Interest Rate, Terms)
- Automatic computation of **Net Expected Gain** and **Return on Investment (ROI %)**.
- Mark investments as **Redeemed / Closed / Collected**.

### 2. Export Functionality (PDF & Excel)
- **Vector PDF Report Exporter**: Generates a clean A4 report complete with executive KPI metrics (Total Invested, Maturity Value, Net Gain, Average ROI, Overdue count) and a categorized investment table.
- **Excel CSV Exporter**: Generates UTF-8 BOM CSV files seamlessly openable in Microsoft Excel, Google Sheets, and LibreOffice without character encoding issues.
- **Sample Template Exporter**: One-click download of a sample CSV template for formatting external spreadsheets.
- Uses Android `FileProvider` (`com.investmenttracker.app.fileprovider`) for secure sharing via WhatsApp, Gmail, Google Drive, Print, or Bluetooth.

### 3. Yearly Report View
- Dedicated segmented navigation: switch between **Portfolio** and **Yearly Report**.
- Annual year navigator (`< Year 2026 >`).
- **Annual Performance KPIs**: Annual Invested Capital, Annual Maturing Capital, Net Gain, and Annual ROI.
- **Month-by-Month Breakdown**: 12-month cashflow timeline with proportional graphical progress bars indicating peak maturity months.
- List of scheduled maturities for the selected year.

### 4. Visual Overdue Color Notification & Alerts
- **Prominent Top Alert Banner**: Vivid red notification card (`#2E151A` surface, `#DC2626` border) with an alert icon and direct *"VIEW"* filter button when maturities are overdue.
- **Visual Card Accents**:
  - 🔴 **Overdue**: Vivid Red border and badge (`35d OVERDUE`) with alert message (`⚠️ Matured 35 days ago (Action Required)`).
  - 🟡 **Maturing Soon (<30 days)**: Amber warning badge (`8d LEFT`) with countdown notice.
  - 🟢 **Active**: Emerald Green active badge.
  - 🟣 **Closed**: Muted purple redeemed badge.

### 5. Excel / CSV Import Provision
- Integrated with Android Storage Access Framework (`ActivityResultContracts.GetContent()`).
- Auto-detects delimiters (commas, semicolons, tabs), cleans currency symbols (`$`, `₹`, commas), and handles multi-format dates (`yyyy-MM-dd`, `dd-MM-yyyy`, `dd/MM/yyyy`, `dd MMM yyyy`).
- **Import Preview Dialog**: Displays parsed investments and error counters for confirmation before batch inserting into SQLite.

### 6. Tablet & Android Mobile Responsive Layout (Android 6.0+)
- `minSdk = 23` (Marshmallow 6.0) to `targetSdk = 36` (Android 16).
- Adaptive `GridLayoutManager`:
  - **Phones**: 1 column in List mode, 2 columns in Grid mode.
  - **Tablets (`widthDp >= 600`)**: Automatically expands to 2 columns in List mode and 3–4 columns in Grid mode.

### 7. GridView & Live Search Functionality
- Instant search bar filtering by name, category, notes, and amounts in real time.
- One-tap **Grid / List View toggle button** (`ic_grid` / `ic_list`).
- Quick filter chips: `All`, `⚠️ Overdue`, `⏳ Maturing Soon`, `🟢 Active`, `✓ Closed`.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.0+
- **Architecture**: Single Activity (`MainActivity`), ViewBinding, clean separation of concerns.
- **Database**: SQLite OpenHelper with batch transactional inserts and indexed queries.
- **UI Components**: CoordinatorLayout, Material 3 Dark financial palette, CardView, RecyclerView, DatePickerDialog, AutoCompleteTextView.
- **Document Generation**: Android Native `PdfDocument` Canvas engine & UTF-8 BOM CSV streaming.

---

## 🚀 Building & Testing

### Prerequisites
- Android Studio Ladybug / Meerkat or later
- JDK 17 or JDK 21
- Android SDK 36 (Build Tools 35.0.0+)

### Commands
```bash
# Run unit tests
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew assembleDebug
```

The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.