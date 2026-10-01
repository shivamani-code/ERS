# Emergency Ambulance Driver Console & Dashboard

A real-time web telemetry console and interactive map dashboard designed for ambulance drivers and emergency response teams.

---

## 🚑 Features

* **Interactive GPS Map**: Powered by Leaflet with custom ambulance telemetry markers.
* **Live Emergency Alerts**: Visual and audio alarm notifications when a new emergency dispatch is received.
* **Caller Telemetry**: Displays caller phone number, precision latitude/longitude coordinates, GPS accuracy, speed, altitude, and timestamp.
* **Driver Console Desktop Shortcut**: Includes `Ambulance Driver Console.lnk` for launching directly into an app-like standalone browser window.

---

## 🌐 How to Use

1. Double-click `index.html` to open directly in any modern web browser (Chrome, Brave, Edge).
2. The dashboard connects to Firebase Firestore (`emergency-response-11a1c`) in real-time and listens for incoming location records.
