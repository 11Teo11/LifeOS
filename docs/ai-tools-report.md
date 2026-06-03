# Raport folosire tooluri AI — LifeOS

Acest document descrie toolurile AI folosite în dezvoltarea proiectului LifeOS, cu exemple concrete de utilizare pentru fiecare etapă a procesului de dezvoltare software.

---

## Tooluri folosite

| Tool | Versiune / Model | Utilizare principală |
|---|---|---|
| Claude (Anthropic) | Claude Sonnet | Arhitectură, implementare, debugging, documentație |
| Gemini (Google) | Gemini 1.5 Pro | Cercetare, întrebări despre API-uri Android |
| GitHub Copilot | GPT-4o | Commit messages, CI/CD workflow, autocomplete |
| Android Studio AI | Gemini in Android Studio | Autocomplete cod, sugestii inline |
| Ollama + llama3.2:1b | llama3.2:1b | Clasificare tranzacții (Agent 2) în aplicație |
| Ollama + Mistral 7B | Mistral 7B | Pattern detection, academic context, evening report (Agent 1, 3, 5) |

---

## Teodor Vasile — StudyBudget, Agent 2, Agent 3, Google Calendar

### 1. Design și arhitectură

**Tool folosit:** Claude

Am folosit Claude pentru a proiecta arhitectura modulului StudyBudget înainte de a scrie cod. Am descris cerințele din backlog și Claude a propus structura de fișiere, entitățile Room necesare și fluxul de date între ViewModel și Repository.

Exemplu de prompt:
> "Am o aplicație Android cu Jetpack Compose și Room. Vreau să implementez un modul de import CSV din Revolut care să clasifice tranzacțiile cu un LLM local. Ce entități Room am nevoie și cum structurez ViewModel-ul?"

Claude a generat structura inițială pentru `Transaction`, `BudgetTarget`, `TransactionCorrection` și `AgentInsight`, pe care am adaptat-o ulterior.

---

### 2. Documentație echipă — TEAM_GUIDELINES.md și agent-contracts.md

**Tool folosit:** Claude

Înainte de a începe implementarea, am folosit Claude pentru a genera documentele de lucru ale echipei.

**`TEAM_GUIDELINES.md`** — Claude a generat regulile de colaborare pentru echipă, inclusiv:
- Reguli pentru structura proiectului și `AppDatabase`
- Procesul corect de adăugare a dependențelor Gradle
- Workflow Git (branch creation, commit messages, Pull Requests)
- Checklist înainte de orice PR
- Reguli pentru Firebase și Google OAuth
- Tabelul de categorii de buget cu emoji

**`docs/agent-contracts.md`** — Claude a generat contractele JSON pentru toți cei 5 agenți AI, specificând:
- Input/output schemas exacte pentru fiecare agent
- Trigger-urile și ownership-ul per agent
- Exemple de JSON pentru fiecare agent

Aceste documente au servit ca referință comună pentru toată echipa pe parcursul dezvoltării.

---

### 3. Implementare — CSV Parser

**Tool folosit:** Claude + Android Studio AI

CSV-ul Revolut are un format nestandard — primele 29 de rânduri sunt metadata, headerul real e pe rândul 30. Android Studio AI a sugerat folosirea OpenCSV, iar Claude a scris implementarea completă a `CsvParser.kt` cu detecție dinamică a header-ului și parsare robustă a sumelor (`"-37.50 RON"` → `-37.50`).

**Ce a generat AI-ul:**
- Logica de scanare a rândurilor pentru găsirea header-ului real
- Parsarea sumelor cu extragerea valutei
- Sealed class `CsvParseResult` cu Success/InvalidFormat/EmptyFile
- Mesajul de eroare cu instrucțiuni pas cu pas pentru export Revolut

---

### 4. Implementare — Agent 2 (clasificare tranzacții)

**Tool folosit:** Claude + Gemini

Agent 2 clasifică tranzacțiile bancare în categorii (Food, Transport, Entertainment etc.) folosind un LLM local via Ollama.

**Claude** a scris:
- `OllamaService.kt` — HTTP client la llama3.2:1b
- `Agent2Classifier.kt` — logica de clasificare cu correction map
- Promptul de clasificare, iterat de mai multe ori pentru acuratețe

**Gemini** a fost folosit pentru a înțelege cum funcționează `HttpURLConnection` pe Android și de ce `10.0.2.2` e IP-ul laptopului văzut din emulator.

Iterații pe prompt pentru îmbunătățirea acurateței:
- V1: prompt generic → Kaufland clasificat greșit ca Shopping
- V2: adăugat exemple explicite pentru merchantii români (Kaufland, Penny, STB) → acuratețe ~85%
- V3: adăugat secțiunea "Critical examples" cu instrucțiuni stricte → acuratețe ~95%

---

### 5. Implementare — Agent 3 (academic context)

**Tool folosit:** Claude

Agent 3 compară cheltuielile medii zilnice în perioadele normale vs perioadele de examene/deadline-uri, folosind datele din Google Calendar și tranzacțiile importate.

Claude a implementat:
- `Agent3AcademicContext.kt` — logica de calcul a mediilor per tip de zi
- Integrarea cu MPAndroidChart via `AndroidView` în Compose
- Generarea insight-ului prin Ollama cu fallback rule-based

---

### 6. Implementare — Google Calendar OAuth

**Tool folosit:** Claude + Gemini

Integrarea cu Google Calendar API v3 a fost cea mai complexă parte.

**Gemini** a fost folosit pentru a înțelege fluxul OAuth2 pe Android și diferența dintre `GoogleAccountCredential` și `GoogleAuthUtil`.

**Claude** a scris:
- `GoogleCalendarService.kt` — fetchEvents cu autentificare OAuth
- Logica de clasificare a presiunii academice (high/medium/low) bazată pe keywords din titlul evenimentului
- Gestionarea erorilor (`UserRecoverableAuthIOException`)

---

### 7. Configurare Ollama pentru dispozitiv real

**Tool folosit:** Claude

O problemă neașteptată a fost că `10.0.2.2` nu funcționează pe dispozitiv real. Claude a propus soluția de a salva IP-ul în DataStore și de a-l configura din Settings.

Claude a implementat:
- `OllamaPreferences.kt` — DataStore pentru IP
- Secțiunea Ollama Settings în `BudgetSettingsScreen`
- Actualizarea tuturor agenților să citească host-ul dinamic

---

### 8. Source control — Commit messages

**Tool folosit:** GitHub Copilot

GitHub Copilot a fost folosit pentru generarea automată a mesajelor de commit în formatul convenit de echipă (`feat`, `fix`, `chore` etc.). La fiecare commit, Copilot sugera automat un mesaj bazat pe diff-ul stagiat, pe care îl acceptam sau modificam.

Exemple de mesaje generate de Copilot:
```
feat(SB-01): add Transaction Room entity and CsvParser
fix(SB-02): fix Ollama connection timeout on real device
chore: update Room version to 2.7.1
```

---

### 9. CI/CD Pipeline

**Tool folosit:** GitHub Copilot

Fișierul `.github/workflows/ci.yml` a fost generat cu ajutorul GitHub Copilot. Am descris ce vrem să facă pipeline-ul (build, lint, upload APK) și Copilot a generat configurația YAML completă, inclusiv pasul de generare a placeholder-ului pentru `google-services.json`.

---

### 10. Debugging

**Tool folosit:** Claude + Android Studio AI

**Probleme rezolvate cu AI:**

- **`JAVA_HOME not set`** — Claude a identificat că Gradle nu găsește JDK-ul din Android Studio și a dat comanda exactă pentru PowerShell
- **Google Calendar DNS failure pe emulator** — Claude a diagnosticat că e problemă de rețea a emulatorului și a recomandat cold boot
- **Ollama NETWORK_ERROR** — Claude a identificat că `192.168.56.1` e IP-ul VirtualBox, nu WiFi-ul real
- **`Room cannot verify data integrity`** — Android Studio AI a sugerat incrementarea versiunii DB

---

### 11. Documentație finală

**Tool folosit:** Claude

Claude a generat:
- `README.md` complet cu toate secțiunile cerute la barem
- Acest raport
- Titluri și descrieri pentru Pull Requests

---

## Roberta Virghileanu — WellCheck, Check-In, Agent 1

<!-- Roberta completează această secțiune -->

---

## Erika Plesca — Habit Tracker, Agent 4, Onboarding

### 1. Design și arhitectură — Habit Tracker (HT-01)

**Tool folosit:** Claude

Am folosit Claude pentru a proiecta structura modulului Habit Tracker înainte de a scrie cod. Am descris cerința („utilizatorul își creează habit-uri zilnice și le bifează din UI; bifările se resetează la miezul nopții") și Claude a propus structura de entități Room, fluxul ViewModel → Repository → DAO și schema bottom navigation-ului.

Exemplu de prompt:
> "Am o aplicație Android cu Jetpack Compose și Room. Vreau un modul de tracking habit-uri zilnice. Userul creează habit-uri, le bifează în fiecare zi, iar bifările trebuie să se reseteze la 00:00. Ce entități Room am nevoie și cum fac reset-ul automat?"

Claude a propus separarea în două entități (`Habit` pentru definiție, `HabitLog` pentru bifări per zi), folosind `WorkManager` cu `PeriodicWorkRequest` pentru reset.

---

### 2. Implementare — Habit Tracker (HT-01)

**Tool folosit:** Claude + GitHub Copilot

Claude a scris structura inițială, iar Copilot a sugerat completări inline pe măsură ce scriam.

**Ce a generat AI-ul:**
- `Habit` și `HabitLog` Room entities cu Foreign Key și `onDelete = CASCADE`
- `HabitDao` cu query-uri pentru habit-urile active, log-urile zilei curente și ștergerea log-urilor vechi
- `HabitRepository` ca wrapper peste DAO
- `HabitViewModel` cu `StateFlow` pentru habit-uri și set-ul de id-uri bifate azi
- `HabitScreen` în Jetpack Compose cu `LazyColumn` și `Checkbox`
- `HabitResetWorker` programat la miezul nopții via `PeriodicWorkRequest`
- Tema pastel temporară (`LifeOSTheme`) și integrarea în `bottomBar` cu `NavigationBar`

---

### 3. Implementare — Onboarding flow (ON-01)

**Tool folosit:** Claude

Onboarding-ul are 3 pași (Profile → Calendar → Budget) și trebuie să persiste starea în `DataStore` ca să nu se reseteze între restart-uri. Am folosit Claude pentru a proiecta state machine-ul și pentru a scrie `OnboardingViewModel`.

**Ce a generat Claude:**
- `OnboardingScreen` cu Composable-uri separate pentru fiecare pas (`StepProfile`, `StepCalendar`, `StepBudget`)
- `OnboardingViewModel` cu `currentStep`, `userName`, `monthlyBudget` ca `StateFlow`
- `OnboardingPreferences` (DataStore) cu flag-uri `isCompleted` și `isFullyCompleted`
- Logica de validare a numelui și a sumei înainte de avansare la următorul pas
- Inserarea automată a unui `BudgetTarget` din pasul final, înainte ca navigarea să se închidă
- Opțiunea „Resume onboarding" din Settings, pentru re-intrarea în flow după ce userul l-a sărit inițial

Am iterat pe Claude pentru a corecta un bug în care navigarea închidea onboarding-ul înainte ca `BudgetTarget` să apuce să fie salvat în DB.

---

### 4. Implementare — Calendar success feedback (ON-01 follow-up)

**Tool folosit:** Claude

În prima versiune a pasului „Connect Google Calendar" din onboarding, dacă userul refuza permisiunea sau OAuth-ul eșua, ecranul rămânea blocat fără feedback. Claude a propus refactor-ul în care fluxul OAuth + consent este extras într-un composable reutilizabil.

**Ce a generat Claude:**
- `CalendarConnector.kt` — composable cu `rememberLauncherForActivityResult` care wraps account picker + consent intent și expune un state object (`state`, `onConnectClick`, `onReset`)
- Reutilizarea aceluiași connector în `OnboardingScreen` și `CalendarScreen` pentru consistență
- Stările Loading / Error / Success cu carduri colorate în Material 3
- Persistarea ultimului cont selectat în `SharedPreferences` pentru ca retry-ul după refuz de consent să nu mai ceară din nou alegerea contului
- Null-safety în `GoogleCalendarService.fetchEvents()` pentru cazurile în care `event.start.dateTime` poate fi `null`

În același PR, Claude a identificat că `google-services.json` fusese commit-at din greșeală în root-ul repo-ului (în loc de doar `app/`) și a propus ștergerea lui plus update la `.gitignore` pentru a ignora `.claude/`.

---

### 5. Implementare — Agent 4 Day Planner (HT-02)

**Tool folosit:** Claude

Agent 4 e agentul de orchestration care generează 3-5 sugestii pentru ziua de mâine, folosind datele de la Agent 1 (PatternAlert), Agent 2 (tranzacții), Agent 3 (evenimente academice) și Habit Tracker. Am cerut Claude să oglindească pattern-ul lui `EveningReportAgent` (Agent 5 zilnic).

**Ce a generat Claude:**
- `DayPlannerAgent.kt` cu sufficiency gate (≥3 daily check-ins), prompt builder care include lista oficială de categorii de buget din `TEAM_GUIDELINES §13`, parser de JSON cu coerciune pe valori invalide, și fallback rule-based
- **Energy guardrail**: dacă media de energie pe ultimele 3 zile e <5/10, agentul filtrează sugestiile cu `effort: high` și forțează cel puțin o sugestie cu `category: recovery` (atât în path-ul LLM cât și în post-validation)
- `DayPlan` + `DayPlanSuggestion` ca entități Room cu Foreign Key, ca să poată fi editate per sugestie
- `DayPlanCard` ca UI inline în topul `HabitScreen`, cu stări Idle / Loading / InsufficientData / Draft / Saved / Error și `OutlinedTextField` pentru fiecare sugestie
- `DayPlanViewModel` care colectează input-ul din 6 repository-uri diferite, citește host-ul Ollama din `OllamaPreferences` și salvează drafts transactional
- 11 teste unitare pe path-ul rule-based, inclusiv test pentru guardrail-ul de energie (ambele direcții) și pentru parsing-ul JSON cu markdown fences

Am descoperit cu Claude că `org.json.JSONObject` este stubbed în Android unit tests (aruncă `RuntimeException: Method not mocked`), iar fix-ul a fost adăugarea `testImplementation("org.json:json")` declarată în `libs.versions.toml` conform regulilor din §3.

Promptul pentru Ollama l-am iterat de două ori:
- V1: prompt generic → LLM-ul inventa categorii de buget care nu existau în aplicație
- V2: adăugat lista exactă de categorii (`Food, Transport, Entertainment, Shopping, Health, Education, Other`) ca `Budget category options` în prompt → output consistent

---

### 6. Configurare Ollama pe macOS pentru testing local

**Tool folosit:** Claude

Pentru a testa Agent 4 cu LLM real, am avut nevoie să instalez Ollama local. Claude m-a ghidat prin setup:
- `brew install ollama` + `brew services start ollama` (auto-start la login)
- `ollama pull mistral` (~4GB) și verificarea cu `curl localhost:11434/api/tags`
- Configurarea host-ului default `10.0.2.2` care funcționează automat din emulator (loopback la host-ul Mac-ului)
- Pentru testing pe device real, schimbarea host-ului din Settings → Budget → Ollama host la IP-ul de LAN al Mac-ului

---

### 7. Source control — Commit messages

**Tool folosit:** GitHub Copilot

GitHub Copilot a generat automat mesajele de commit pe baza diff-ului stagiat, în formatul convenit cu echipa (`feat(HT-01)`, `fix(ON-01)`, `chore`, etc.).

Exemple de mesaje generate de Copilot:
```
feat(HT-01): add Habit and HabitLog Room entities
feat(ON-01): complete onboarding flow at app start with 3 steps and DataStore
fix(ON-01): insert BudgetTarget before triggering navigation on complete
feat(HT-02): add Agent 4 Day Planner and tomorrow's plan UI
```

---

### 8. Debugging

**Tool folosit:** Claude

**Probleme rezolvate cu AI:**

- **`Resource and asset merger: Duplicate resources`** la `values.xml` și `values 2.xml` — Claude a diagnosticat că fișierele cu ` 2.xml` în nume sunt duplicate generate de macOS Finder / iCloud Drive (proiectul stă în `~/Desktop`, sincronizat cu iCloud). Fix imediat: `./gradlew clean`. Fix permanent recomandat: mutarea proiectului în afara folderului Desktop sau dezactivarea „Desktop & Documents Folders" în iCloud
- **Gradle build timeouts pe primul build** — Claude a propus bumping de JVM heap (`-Xmx2048m` → `-Xmx4096m`) și activarea `org.gradle.caching=true` + `org.gradle.configuration-cache=true` în `gradle.properties`
- **`Room cannot verify data integrity`** la primul start după adăugarea unei entități noi — Claude a reamintit regula din `TEAM_GUIDELINES §2` despre incrementarea `version` în `@Database`
- **Android Studio shows red errors but CLI build is green** — Claude a indicat că e indexare stale după adăugarea de fișiere noi; fix-ul e `File → Sync Project with Gradle Files`, sau în cazuri persistente `Invalidate Caches and Restart`

---

### 9. Documentație și Pull Requests

**Tool folosit:** Claude

Claude a generat descrierile pentru Pull Request-urile mele (titlu + summary + test plan), inclusiv:
- Identificarea conflictelor de merge prevăzute (versiunea `AppDatabase` între HT-02 și AC-02 / Agent 5 care vor fi mergeate consecutiv)
- Note despre limitări cunoscute (de ex. că Agent 4 folosește total daily outflow în loc de o categorie specifică precum „food delivery" pentru că Agent 2 încă nu clasifică tranzacțiile)
- Test plan-uri cu pași concreți pentru reviewer

---

---

## Concluzii

Toolurile AI au accelerat semnificativ dezvoltarea proiectului. Estimăm că aproximativ **90% din codul scris** a fost generat sau corectat cu ajutorul AI, în special:

- Entitățile Room și DAO-urile — generate aproape integral de Claude
- Logica agenților AI — scrisă de Claude cu iterații pe prompt
- Documentele de echipă (TEAM_GUIDELINES, agent-contracts) — generate de Claude
- Commit messages — generate automat de GitHub Copilot
- CI/CD workflow — generat de GitHub Copilot

Principalele limitări observate:
- AI-ul halucinează ocazional API-uri care nu există sau versiuni incorecte de librării
- Prompturile pentru LLM-urile locale (Mistral, llama3.2) au necesitat mai multe iterații pentru acuratețe bună
- Pentru probleme specifice de Android (configurare OAuth, WorkManager) a fost necesară validarea cu documentația oficială

Folosirea AI nu a eliminat necesitatea înțelegerii codului — fiecare sugestie a trebuit verificată și adaptată contextului specific al proiectului.
