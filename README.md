# LifeOS

Aplicație Android pentru studenți care combină gestionarea bugetului, urmărirea obiceiurilor, un calendar academic și monitorizarea stării de bine într-un singur loc. Dezvoltată în cadrul cursului MDS 2026 la FMI.

---

## Cuprins

- [Descriere](#descriere)
- [Funcționalități](#funcționalități)
- [Arhitectură și Tech Stack](#arhitectură-și-tech-stack)
- [Agenți AI](#agenți-ai)
- [Structura proiectului](#structura-proiectului)
- [Instalare și configurare](#instalare-și-configurare)
- [Echipă](#echipă)

---

## Descriere

LifeOS este o aplicație Android nativă care ajută studenții să își gestioneze viața academică și personală. Aplicația importă automat tranzacțiile bancare din Revolut, le clasifică folosind un model AI local (llama3.2), sincronizează evenimentele din Google Calendar și generează rapoarte zilnice personalizate.

---

## Funcționalități

### 💰 StudyBudget
Modul principal de gestionare a cheltuielilor.

<!-- SCREENSHOT: Tab-ul Budget cu progress bars per categorie (verde/galben/roșu) -->
![Budget Screen](docs/screenshots/budget_screen.png)

- Import CSV din Revolut cu preview înainte de confirmare
- Clasificare automată a tranzacțiilor cu AI (Agent 2)
- Progress bars per categorie cu color coding: verde sub 80%, galben 80-99%, roșu peste 100%
- Corecție manuală a categoriilor cu propagare automată la tranzacții similare
- Notificări push când bugetul atinge 80% din limită
- Categorii disponibile: 💰 Total, 🍔 Food, 🚌 Transport, 🎬 Entertainment, 🛍️ Shopping, 💊 Health, 📚 Education, 📦 Other

### 📅 Calendar Academic
Vizualizare și gestionare a evenimentelor academice.

<!-- SCREENSHOT: Tab-ul Calendar cu grid lunar și evenimente colorate -->
![Calendar Screen](docs/screenshots/calendar_screen.png)

- Sincronizare cu Google Calendar prin OAuth2
- Adăugare manuală de evenimente
- Clasificare automată a presiunii academice: high (examene, colocvii), medium (deadline-uri, prezentări), low (laboratoare, seminare)
- Vizualizare grid lunar cu puncte colorate per eveniment
- Vizualizare listă cu toggle

### ✅ Habit Tracker
Urmărirea obiceiurilor zilnice.

<!-- SCREENSHOT: Tab-ul Habits cu lista de obiceiuri și starea lor -->
![Habits Screen](docs/screenshots/habits_screen.png)

- Adăugare și ștergere obiceiuri personalizate
- Marcare zilnică a obiceiurilor completate
- Reset automat la miezul nopții prin WorkManager

### 💚 Check-In Zilnic
Monitorizarea stării de bine.

<!-- SCREENSHOT: Tab-ul Check-In cu sliderele pentru somn, energie, stres -->
![CheckIn Screen](docs/screenshots/checkin_screen.png)

- Înregistrare zilnică: ore de somn, nivel de energie (1-10), nivel de stres (1-10), simptome
- Reminder zilnic la 10:00 dacă nu s-a făcut check-in
- Detectare automată a pattern-urilor negative prin Agent 1
- Grafic de tendințe (ultimele 7 zile): energie (teal) și somn (mov), construit cu Jetpack Compose Canvas
- Punctele de energie sub 4/10 sunt marcate vizual cu un halo roșu de avertizare
- Graficul se afișează doar dacă există minimum 3 zile de date înregistrate

### ⚙️ Settings
Configurare bugete și setări Ollama.

<!-- SCREENSHOT: Tab-ul Settings cu lista de bugete și secțiunea Ollama Settings -->
![Settings Screen](docs/screenshots/settings_screen.png)

- Setare limite lunare per categorie
- Configurare IP-ul laptopului pentru conexiunea la Ollama (pentru dispozitiv real)
- Valoare default: 10.0.2.2 (emulator)

### 📊 Raport Seară
Raport zilnic generat automat la 21:00.

<!-- SCREENSHOT: Tab-ul Report cu textul raportului generat de AI -->
![Report Screen](docs/screenshots/report_screen.png)

- Raport personalizat generat de Agent 5 (Mistral 7B)
- Corelează datele de wellness, obiceiuri și cheltuieli
- Fallback rule-based dacă Ollama nu este disponibil
- Notificare push la 21:00 cu link direct la tab-ul Report

---

## Arhitectură și Tech Stack

### Stack principal
- **Limbaj:** Kotlin 2.3.21
- **UI:** Jetpack Compose (Material 3)
- **Build:** Android Studio Panda, AGP 9.2.0, KSP
- **Baza de date:** Room 2.7.1 (versiunea 8)
- **Preferințe:** DataStore
- **Background tasks:** WorkManager
- **CSV parsing:** OpenCSV 5.9
- **HTTP:** OkHttp (pentru OllamaClient), HttpURLConnection (pentru OllamaService)

### Firebase și Google
- **Firebase:** Analytics, project ID `lifeos-a8473`
- **Google Calendar API v3** cu OAuth2 scope `calendar.readonly`
- **Google Cloud project:** `sound-bay-495018-m6`

### AI local
- **Ollama** rulează local pe laptop
- **llama3.2:1b** pentru clasificarea tranzacțiilor (Agent 2) — rapid, ~400MB
- **Mistral 7B** pentru pattern detection (Agent 1) și evening report (Agent 5) — mai capabil

### Arhitectura aplicației
Aplicația urmează arhitectura MVVM cu Repository pattern:

```
UI Layer (Compose Screens + ViewModels)
        ↓
Repository Layer
        ↓
Data Layer (Room DAOs + DataStore + API clients)
        ↓
WorkManager (background tasks)
```

### Baza de date Room
Versiunea curentă: **8**

| Entitate | Descriere |
|---|---|
| Transaction | Tranzacții importate din Revolut |
| BudgetTarget | Limite lunare per categorie |
| Habit | Obiceiuri definite de utilizator |
| HabitLog | Log-uri zilnice de completare |
| DailyCheckIn | Check-in-uri zilnice de wellness |
| AcademicEvent | Evenimente din Google Calendar sau adăugate manual |
| TransactionCorrection | Corecții manuale de categorii |
| AgentInsight | Output-uri ale agenților AI |
| PatternAlert | Alerte de pattern-uri detectate |

### Workers (WorkManager)

| Worker | Trigger | Funcție |
|---|---|---|
| HabitResetWorker | Daily la miezul nopții | Reset obiceiuri |
| BudgetCheckWorker | La fiecare oră + după import CSV | Verifică limitele bugetare |
| CheckInReminderWorker | Daily la 10:00 | Reminder check-in |
| EveningReportWorker | Daily la 21:00 | Generează raportul serii |
| PatternDetectorWorker | După fiecare check-in | Detectează pattern-uri negative |

---

## Agenți AI

| Agent | Owner | Model | Trigger | Funcție |
|---|---|---|---|---|
| Agent 1 — Pattern Detector | Roberta | Mistral 7B (fallback: rule-based) | După fiecare check-in | Detectează pattern-uri negative (somn/energie/stres) pe fereastra de 14 zile; severitate: Low / Medium / High; UI feedback cu carduri colorate (roșu/portocaliu/galben) |
| Agent 2 — Budget Analyzer | Teo | llama3.2:1b | După import CSV | Clasifică tranzacțiile pe categorii |
| Agent 3 — Academic Context | Teo | - | Weekly + calendar sync | Compară cheltuielile normale vs perioadele de examen |
| Agent 4 — Day Planner | Erika | - | On-demand | Generează sugestii pentru ziua următoare |
| Agent 5 — Accountability Coach | Erika | Mistral 7B (fallback: rule-based) | 21:00 daily | Generează raportul serii; dacă lipsește check-in-ul → notificare redirecționată la tab Wellness; dacă lipsesc tranzacțiile → redirecționare la tab Budget |

Toți agenții au fallback rule-based dacă Ollama nu este disponibil.

> **Notă Agent 1:** Calea LLM (Ollama) este implementată dar dezactivată în build-ul curent (`tryOllama` returnează `null`). Sistemul rulează exclusiv pe fallback rule-based (detecție streak ≥ 3 zile consecutive). Pentru a activa LLM-ul, decomentează apelul din `PatternDetectorAgent.kt` și asigură-te că serverul Ollama rulează.

---

## Testare

### Strategie pe trei niveluri

| Nivel | Tip | Scopul |
|---|---|---|
| 1 | Unit Tests (JUnit + MockK) | Logică pură din Repositories și ViewModels |
| 2 | Instrumented Tests (Espresso / Compose Test) | Verificarea UI pe emulator |
| 3 | Agent Evals | Testarea prompt-urilor și a mecanismelor de fallback pentru Agent 1 și Agent 5 |

### Agent Evals — ce se testează

**Agent 1 — PatternDetectorAgent**
- **Calea LLM:** verificarea parsării corecte a răspunsului JSON returnat de Ollama
- **Calea fallback:** simularea căderii serverului + verificarea logicii de streak (ex: 3 zile consecutive cu somn < 6h)
- **Date de test:** set controlat care include cazuri limită — exact 2 zile de date (sub pragul de 3), valori extreme (energie 1/10, stres 10/10)
- **Tooling:** `MockWebServer` (OkHttp) pentru simularea răspunsurilor HTTP fără server Ollama activ

**Agent 5 — EveningReportAgent**
- Validarea emisiei corecte a `StateFlow` din `ReportViewModel`
- Testarea scenariilor de date lipsă (fără check-in, fără tranzacții)

### Cum să rulezi testele

```bash
# Unit tests
./gradlew test

# Instrumented tests (necesită emulator pornit)
./gradlew connectedAndroidTest

# Forțează reconstrucția bazei de date de test după modificări de schemă Room
./gradlew assembleDebugAndroidTest
```

> **Atenție:** Orice modificare a schemei Room necesită incrementarea versiunii în `AppDatabase` **și** recompilarea testelor instrumentate — altfel acestea vor eșua cu `IllegalStateException`.

---

## Structura proiectului

```
app/src/main/java/com/example/lifeos/
  data/
    agent/
      EveningReportAgent.kt      (Agent 5)
      PatternDetectorAgent.kt    (Agent 1)
      OllamaClient.kt            (shared Agent 1 și 5 — Mistral)
    ai/
      OllamaService.kt           (Agent 2 — llama3.2:1b)
      Agent2Classifier.kt
    calendar/
      GoogleCalendarService.kt
    db/
      dao/                       (TransactionDao, HabitDao, etc.)
      entity/                    (Transaction, Habit, etc.)
      AppDatabase.kt
    preferences/
      OllamaPreferences.kt
      OnboardingPreferences.kt
      ReportPreferences.kt
    repository/                  (HabitRepository, TransactionRepository, etc.)
  ui/
    checkin/
    habit/
    onboarding/
    report/
    studybudget/
    theme/
  util/
    CsvParser.kt
    NotificationHelper.kt
  worker/                        (HabitResetWorker, BudgetCheckWorker, etc.)
  MainActivity.kt
```

---

## Instalare și configurare

### Cerințe
- Android Studio Panda sau mai nou
- JDK 17+
- Ollama instalat pe laptop ([ollama.com](https://ollama.com))
- Modele Ollama instalate:
  ```powershell
  ollama pull llama3.2:1b
  ollama pull mistral
  ```

### Configurare Ollama
Pornește Ollama cu acces din rețea:
```powershell
$env:OLLAMA_HOST = "0.0.0.0"
ollama serve
```

Adaugă regula de firewall pentru portul 11434:
```powershell
netsh advfirewall firewall add rule name="Ollama" dir=in action=allow protocol=TCP localport=11434
```

### Manifest — permisiune HTTP necriptată

Conexiunea la Ollama se face prin HTTP (nu HTTPS). Pe Android 9+, traficul cleartext este blocat implicit. Asigură-te că `AndroidManifest.xml` conține:

```xml
<application
    android:usesCleartextTraffic="true"
    ...>
```

Fără această setare, `OllamaClient` va eșua silențios pe dispozitivele reale chiar dacă serverul rulează corect.

### Configurare Google OAuth (per developer)
Fiecare developer trebuie să își înregistreze SHA-1 fingerprint-ul:

```bash
keytool -keystore ~/.android/debug.keystore -list -v -alias androiddebugkey -storepass android -keypass android
```

Adaugă SHA-1-ul în **Google Cloud Console → Google Auth Platform → Clients → Android**.

Descarcă `google-services.json` din **Firebase Console → LifeOS → Project Settings** și pune-l în `app/google-services.json`. **Nu îl commit-a niciodată.**

### Rulare pe dispozitiv real
1. Activează **Developer Options** pe telefon (Settings → About Phone → apasă de 7 ori pe Build Number)
2. Activează **USB Debugging**
3. Conectează telefonul prin USB
4. Selectează telefonul în Android Studio și dă Run
5. Asigură-te că telefonul și laptopul sunt pe **același WiFi**
6. Află IP-ul laptopului: `ipconfig` → caută `IPv4 Address` la adaptorul WiFi
7. În aplicație: Settings → Ollama Settings → introdu IP-ul → Save Ollama Host

### CI/CD
Pipeline-ul GitHub Actions rulează la fiecare push/PR pe `develop` și `main`:
- `lintDebug`
- `assembleDebug`
- Uploadează APK ca artifact (retenție 7 zile)

---

## Echipă

| Developer | Module | User Stories |
|---|---|---|
| Teo | StudyBudget (import CSV, clasificare AI, calendar academic) | SB-01, SB-02, SB-03, SB-04, ON-02 |
| Roberta | WellCheck / Check-In / Pattern Detection | WC-01, WC-02, WC-03 |
| Erika | Habit Tracker, Accountability Coach, Evening Report, Onboarding | HT-01, HT-02, AC-01, AC-02, ON-01, ON-03 |

---

## Branch strategy

```
main        ← releases stabile
develop     ← integrare continuă
feature/*   ← feature branches (ex: feature/SB-02-agent2-classifier)
```

Orice modificare merge prin Pull Request cu Approve obligatoriu înainte de Merge.

---

*Proiect realizat pentru cursul MDS 2026, Facultatea de Matematică și Informatică.*
