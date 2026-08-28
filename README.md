# FairFare 🚖 🚌 🚇

**FairFare** is a modern Android application built with Jetpack Compose for transport fare calculation, multi-transit route comparison, official tariff auditing, and live meter overcharge checking.

---

## 🌟 Key Features

- **Multi-Transit Fare Estimation**: Compare estimated travel times and costs across auto-rickshaws, cabs, metro, and local bus transit.
- **Official Tariff Database**: Accurate, government-approved tariff schedules and rate cards for transparent pricing.
- **Overcharge Audit & Meter Checker**: Verify whether the requested meter or driver fare matches official regulated rates based on distance and night/peak surcharges.
- **Interactive Route Map & Traffic**: Visual route rendering with key transit corridors and traffic congestion indicators.
- **Bus & Public Transit Routes**: Search and browse bus line stops, timings, and fares.
- **Incident Reporting**: Submit passenger reports for overcharging, broken meters, or refusal to ply.
- **Light & Dark Theme**: Full Material 3 dynamic theming support.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose, Material Design 3
- **Architecture**: MVVM with Kotlin Coroutines & StateFlow
- **Local Persistence**: Android Room Database (SQLite) with KSP
- **Networking**: Retrofit 2, OkHttp 3, Moshi JSON Converter
- **Location & Maps**: Google Play Services Location & Routing API integrations
- **Configuration & Security**: Secrets Gradle Plugin for API keys (.env support)
- **Minimum SDK**: 26 (Android 8.0 Oreo) | **Target SDK**: 35+

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- JDK 17 or JDK 21
- Android SDK installed with API 35+

### Installation & Setup

1. **Clone the repository**:
   ```bash
   git clone https://github.com/mohammadfaizansari786/fairfare.git
   cd fairfare
   ```

2. **Configure Environment Variables**:
   Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```
   Add any optional API keys (e.g., `MAPS_API_KEY`, `TOMTOM_API_KEY`).

3. **Build and Run**:
   Open the project in Android Studio or build using Gradle:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.
