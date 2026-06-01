# Raport folosire tooluri AI — LifeOS

Acest document descrie toolurile AI folosite în dezvoltarea proiectului LifeOS, cu exemple concrete de utilizare pentru fiecare etapă a procesului de dezvoltare software.

---

## Tooluri folosite

| Tool | Versiune / Model | Utilizare principală |
|---|---|---|
| Claude (Anthropic) | Claude Sonnet | Arhitectură, implementare, debugging, documentație |
| Gemini (Google) | Gemini 1.5 Pro | Cercetare, întrebări despre API-uri Android |
| Android Studio AI | Gemini in Android Studio | Autocomplete cod, sugestii inline |
| Ollama + llama3.2:1b | llama3.2:1b | Clasificare tranzacții (Agent 2) în aplicație |
| Ollama + Mistral 7B | Mistral 7B | Pattern detection, academic context, evening report (Agent 1, 3, 5) |

---

## Teo — StudyBudget, Agent 2, Agent 3, Google Calendar

### 1. Design și arhitectură

**Tool folosit:** Claude

Am folosit Claude pentru a proiecta arhitectura modulului StudyBudget înainte de a scrie cod. Am descris cerințele din backlog și Claude a propus structura de fișiere, entitățile Room necesare și fluxul de date între ViewModel și Repository.

Exemplu de prompt:
> "Am o aplicație Android cu Jetpack Compose și Room. Vreau să implementez un modul de import CSV din Revolut care să clasifice tranzacțiile cu un LLM local. Ce entități Room am nevoie și cum structurez ViewModel-ul?"

Claude a generat structura inițială pentru `Transaction`, `BudgetTarget`, `TransactionCorrection` și `AgentInsight`, pe care am adaptat-o ulterior.

---

### 2. Implementare — CSV Parser

**Tool folosit:** Claude + Android Studio AI

CSV-ul Revolut are un format nestandard — primele 29 de rânduri sunt metadata, headerul real e pe rândul 30. Android Studio AI a sugerat folosirea OpenCSV, iar Claude a scris implementarea completă a `CsvParser.kt` cu detecție dinamică a header-ului și parsare robustă a sumelor (`"-37.50 RON"` → `-37.50`).

**Ce a generat AI-ul:**
- Logica de scanare a rândurilor pentru găsirea header-ului real
- Parsarea sumelor cu extragerea valutei
- Sealed class `CsvParseResult` cu Success/InvalidFormat/EmptyFile
- Mesajul de eroare cu instrucțiuni pas cu pas pentru export Revolut

---

### 3. Implementare — Agent 2 (clasificare tranzacții)

**Tool folosit:** Claude + Gemini

Agent 2 clasifică tranzacțiile bancare în categorii (Food, Transport, Entertainment etc.) folosind un LLM local via Ollama.

**Claude** a scris:
- `OllamaService.kt` — HTTP client la Mistral/llama3.2:1b
- `Agent2Classifier.kt` — logica de clasificare cu correction map
- Promptul de clasificare, iterat de mai multe ori pentru acuratețe

**Gemini** a fost folosit pentru a înțelege cum funcționează `HttpURLConnection` pe Android și de ce `10.0.2.2` e IP-ul laptopului văzut din emulator.

Iterații pe prompt pentru îmbunătățirea acurateței:
- V1: prompt generic → Kaufland clasificat greșit ca Shopping
- V2: adăugat exemple explicite pentru merchantii români (Kaufland, Penny, STB) → acuratețe ~85%
- V3: adăugat secțiunea "Critical examples" cu instrucțiuni stricte → acuratețe ~95%

---

### 4. Implementare — Agent 3 (academic context)

**Tool folosit:** Claude

Agent 3 compară cheltuielile medii zilnice în perioadele normale vs perioadele de examene/deadline-uri, folosind datele din Google Calendar și tranzacțiile importate.

Claude a implementat:
- `Agent3AcademicContext.kt` — logica de calcul a mediilor per tip de zi
- Integrarea cu MPAndroidChart via `AndroidView` în Compose
- Generarea insight-ului prin Ollama cu fallback rule-based

---

### 5. Implementare — Google Calendar OAuth

**Tool folosit:** Claude + Gemini

Integrarea cu Google Calendar API v3 a fost cea mai complexă parte. Am folosit:

**Gemini** pentru a înțelege fluxul OAuth2 pe Android și diferența dintre `GoogleAccountCredential` și `GoogleAuthUtil`.

**Claude** a scris:
- `GoogleCalendarService.kt` — fetchEvents cu autentificare OAuth
- Logica de clasificare a presiunii academice (high/medium/low) bazată pe keywords din titlul evenimentului
- Gestionarea erorilor (`UserRecoverableAuthIOException`)

---

### 6. Configurare Ollama pentru dispozitiv real

**Tool folosit:** Claude

O problemă neașteptată a fost că `10.0.2.2` (IP-ul emulatorului) nu funcționează pe dispozitiv real. Claude a propus soluția de a salva IP-ul în DataStore și de a-l configura din Settings, cu `OLLAMA_HOST=0.0.0.0` pe laptop.

Claude a implementat:
- `OllamaPreferences.kt` — DataStore pentru IP
- Secțiunea Ollama Settings în `BudgetSettingsScreen`
- Actualizarea tuturor agenților să citească host-ul dinamic

---

### 7. Debugging

**Tool folosit:** Claude + Android Studio AI

**Probleme rezolvate cu AI:**

- **`JAVA_HOME not set`** — Claude a identificat că Gradle nu găsește JDK-ul din Android Studio și a dat comanda exactă pentru PowerShell
- **Google Calendar DNS failure pe emulator** — Claude a diagnosticat că e problemă de rețea a emulatorului și a recomandat cold boot
- **Ollama NETWORK_ERROR** — Claude a identificat că `192.168.56.1` e IP-ul VirtualBox, nu WiFi-ul real, și a explicat cum să găsesc IP-ul corect din `ipconfig`
- **`Room cannot verify data integrity`** — Android Studio AI a sugerat incrementarea versiunii DB

---

### 8. Documentație

**Tool folosit:** Claude

Claude a generat:
- `README.md` complet cu toate secțiunile cerute la barem
- Acest raport
- Titluri și descrieri pentru Pull Requests
- Comentarii în cod pentru logica mai complexă

---

## Roberta — WellCheck, Check-In, Agent 1

<!-- Roberta completează această secțiune -->

---

## Erika — Habit Tracker, Agent 5, Onboarding

<!-- Erika completează această secțiune -->

---

## Concluzii

Toolurile AI au accelerat semnificativ dezvoltarea proiectului. Estimăm că aproximativ **90% din codul scris** a fost generat sau corectat cu ajutorul AI, în special:

- Entitățile Room și DAO-urile — generate aproape integral de Claude
- Logica agenților AI — scrisă de Claude cu iterații pe prompt
- Debugging — majoritatea problemelor rezolvate cu ajutorul Claude sau Gemini

Principalele limitări observate:
- AI-ul halucinează ocazional API-uri care nu există sau versiuni incorecte de librării
- Prompturile pentru LLM-urile locale (Mistral, llama3.2) au necesitat mai multe iterații pentru acuratețe bună
- Pentru probleme specifice de Android (configurare OAuth, WorkManager) a fost necesară validarea cu documentația oficială

Folosirea AI nu a eliminat necesitatea înțelegerii codului — fiecare sugestie a trebuit verificată și adaptată contextului specific al proiectului.
