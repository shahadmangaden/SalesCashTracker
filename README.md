# Sales Cash Tracker (Windows)

Desktop version of the **SAHAD M — Bakhtar Travels Sales & Cash Paid Tracker** Excel workbook.

## Download & install
1. Open the **Releases** page of this repository (right side of the repo home page).
2. Download `SalesCashTracker-Setup-<version>.exe` from the latest release.
3. Run it. If Windows SmartScreen shows "Windows protected your PC", click **More info → Run anyway** (the app is not code-signed).
4. Open **Sales Cash Tracker** from the Desktop or Start menu.
5. First time: click **Import my Excel tracker** and pick your tracker `.xlsx` — all bookings, the cash-paid log, agents and visa types come in.

To update, install the newer Setup exe over the old one. Your records stay.

## What it does
- **Sales / Bookings** – every column of the blue table (A:Z). Profit, Profit %, Balance Due, Expire Date and Days to Expiry calculate automatically. Filter by agent, product or payment status; search by name, PNR, etc. `Ctrl+N` = new sale.
- **Cash Paid Log** – the green table (AB:AG) with daily and per-agent totals.
- **Dashboard** – totals, month-by-month breakdown, sales by product and customer type, and the Cash Paid checker (by date, agent, or both).
- **Agent Ledger** – bookings, net cost, selling, paid in rows, balance due, cash paid, net remaining per agent.
- **Visa Expiry** – visas sorted by days left, with a red badge for those expiring within 7 days.
- **Lists & Data** – edit dropdown lists and visa durations; import/export Excel; backup/restore.

## Where data is stored
`%APPDATA%\Sales Cash Tracker\tracker-data.json`, with an automatic dated copy every day in the `backups` folder (last 60 days). Use **Lists & Data → Open data folder** to see it.

## Building
Every push to `main` runs the GitHub Action `Build Windows app`, which builds the installer and a portable exe and publishes them as a new release.

Local development: `npm install` then `npm start`.
