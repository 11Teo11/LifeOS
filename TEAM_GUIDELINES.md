# Ghid de lucru în echipă — LifeOS

---

## 1. Structura proiectului

Tot codul respectă această structură și nu se abate de la ea:

```
data/
  db/
    dao/            ← toate DAO-urile (HabitDao, TransactionDao, etc.)
    entity/         ← toate entitățile Room (Habit, Transaction, etc.)
    AppDatabase.kt  ← O SINGURĂ bază de date pentru tot proiectul
  onboarding/       ← excepție acceptată, DataStore nu e Room
  repository/       ← toate repository-urile
ui/
  [modul]/          ← Screen.kt și ViewModel.kt pentru fiecare modul
  theme/            ← nu se modifică fără discuție în echipă
util/
worker/
```

> **Regula de aur:** Nu creați baze de date separate. Orice entitate nouă Room se adaugă în `AppDatabase.kt` și se incrementează `version`.

---

## 2. Când modifici schema bazei de date

De fiecare dată când adaugi, ștergi sau modifici o entitate Room:

```kotlin
// AppDatabase.kt
@Database(
    entities = [Transaction::class, Habit::class, HabitLog::class], // adaugi entitatea nouă
    version = 3, // incrementezi OBLIGATORIU
    exportSchema = false
)
```

> ⚠️ Dacă uiți să incrementezi versiunea, aplicația crashează la startup pe orice dispozitiv care are o versiune veche instalată.

---

## 3. Dependențe Gradle

Orice librărie nouă pe care o folosești în cod **trebuie declarată** în `libs.versions.toml` și `app/build.gradle.kts`. Nu e suficient că merge pe calculatorul tău — dacă nu e declarată, nu merge pe calculatorul nimănui altcuiva.

### Procesul corect când adaugi o librărie nouă:

**Pasul 1** — adaugi versiunea în `libs.versions.toml`:
```toml
[versions]
numeLibrarie = "1.0.0"

[libraries]
nume-librarie = { group = "com.example", name = "librarie", version.ref = "numeLibrarie" }
```

**Pasul 2** — adaugi în `app/build.gradle.kts`:
```kotlin
implementation(libs.nume.librarie)
```

**Pasul 3** — dai Sync Now și verifici că build-ul merge.

**Pasul 4** — abia după faci commit.

> ⚠️ Nu declara aceeași librărie de două ori — verifică că nu există deja în fișier înainte să adaugi.

---

## 4. Limba în UI

Toată aplicația e în **engleză**. Fără excepții. Orice string vizibil pentru user — titluri, butoane, mesaje de eroare, placeholder-uri — se scrie în engleză.

---

## 5. Workflow Git

Fiecare task = un branch separat creat din `develop`:

```bash
git checkout develop
git pull origin develop
git checkout -b feature/XX-YY-nume-task
```

### Commit-urile se fac frecvent și cu mesaje clare:

```
feat(SB-01): add Transaction Room entity
fix(HT-01): fix habit log duplicate insertion
chore: update Room version to 2.7.1
```

**Prefixe acceptate:** `feat`, `fix`, `chore`, `refactor`, `test`, `docs`

> ⚠️ Niciodată nu lucrezi direct pe `develop` sau `main`.

---

## 6. Pull Requests

Orice modificare merge prin PR, inclusiv fix-uri mici. Procesul:

1. Push pe branch-ul tău
2. Pe GitHub → **Compare & pull request**
3. **Base: develop** (nu main!)
4. Descriere clară cu ce ai făcut și ce issues rezolvi (`Closes #X`)
5. Aștepți **Approve** de la o colegă
6. Abia după dai **Merge**

> ℹ️ Dacă PR-ul rezolvă un issue, scrii `Closes #X` în descriere — GitHub îl închide automat la merge.

---

## 7. Issues pentru bug-uri

Orice problemă găsită în codul altcuiva se raportează ca **Issue pe GitHub** înainte să o rezolvi. Issue-ul trebuie să conțină:

- Ce e problema
- Ce comportament e așteptat
- Ce fix propui

Asta ne dă puncte la barem și ține evidența problemelor rezolvate.

---

## 8. Ce faci când aplicația crashează

1. Deschizi **Logcat** în Android Studio (`Alt+6`)
2. Cauți `FATAL EXCEPTION` sau linii roșii cu `E`
3. Citești mesajul de eroare — de obicei e clar

### Cele mai comune cauze:

| Eroare | Cauză | Fix |
|--------|-------|-----|
| `Room cannot verify data integrity` | Ai modificat o entitate fără să incrementezi `version` în AppDatabase | Incrementează `version` și dai Run |
| `NullPointerException` | Ai uitat să inițializezi ceva | Verifică că toate obiectele sunt inițializate înainte de folosire |
| `ClassNotFoundException` | O dependență lipsește din Gradle | Adaugă dependența în `libs.versions.toml` și `build.gradle.kts` |
| `Unresolved reference` | Import lipsă sau dependență nedeclarată | Verifică importurile și Gradle |

---

## 9. Checklist înainte de orice PR

- [ ] Build-ul merge fără erori (`Build → Make Project`)
- [ ] Aplicația pornește pe emulator fără crash
- [ ] Nu ai duplicate în `libs.versions.toml`
- [ ] Toate string-urile vizibile pentru user sunt în **engleză**
- [ ] Dacă ai adăugat/modificat entități Room, ai incrementat `version` în `AppDatabase`
- [ ] Dependențele noi sunt declarate în Gradle și Sync a mers
- [ ] Branch-ul e creat din `develop` și PR-ul merge în `develop`
- [ ] Ai cel puțin un commit per task cu mesaj clar
- [ ] Ai așteptat **Approve** de la o colegă înainte de Merge

---

## 10. Firebase și Google OAuth — cum e configurat

Aplicația folosește Firebase și Google Calendar API. Iată cum e conectat totul, pentru referință.

### Ce e configurat

- **Firebase project:** `LifeOS` (project ID: `lifeos-a8473`)
- **Google Cloud project:** `LifeOS` (project ID: `sound-bay-495018-m6`)
- **API activat:** Google Calendar API
- **OAuth scope:** `https://www.googleapis.com/auth/calendar.readonly`

### Fișierul `google-services.json`

Se află în `app/google-services.json`. **Nu se pune în `.gitignore`** pentru aplicații Android — e safe să fie în repo pentru că nu conține chei private, doar identificatori publici ai proiectului.

### Cum funcționează OAuth în aplicație

1. Userul apasă "Connect Google Calendar" în `CalendarScreen`
2. `CalendarViewModel.getAccountPickerIntent()` deschide un picker nativ Android cu conturile Google de pe telefon
3. Userul selectează contul
4. `CalendarViewModel.syncCalendar(accountName)` apelează `GoogleCalendarService.fetchEvents()`
5. `GoogleCalendarService` folosește `GoogleAccountCredential` cu scope-ul `CALENDAR_READONLY`
6. Evenimentele sunt salvate în `AcademicEvent` table din Room DB

### SHA-1 fingerprint — ce trebuie să facă fiecare colegă

Fiecare developer are un SHA-1 diferit pe calculatorul ei. Pentru ca OAuth să funcționeze pe calculatorul tău, trebuie să îți înregistrezi SHA-1-ul în Google Cloud Console.

**Pasul 1 — Generează SHA-1-ul tău** (rulează în terminal):
```bash
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -keystore "$env:USERPROFILE\.android\debug.keystore" -list -v -alias androiddebugkey -storepass android -keypass android
```
Caută linia SHA1: în output și copiază valoarea.

**Pasul 2 — Adaugă SHA-1-ul în Google Cloud Console:**
1. Mergi pe **console.cloud.google.com** → proiectul LifeOS
2. **Google Auth Platform → Clients**
3. Apasă pe clientul Android existent sau **Create OAuth client** → Android
4. La **SHA-1 certificate fingerprint** adaugă SHA-1-ul tău
5. **Save**

Fără acest pas, Google va respinge cererile OAuth de pe calculatorul tău cu eroarea NEED_REMOTE_CONSENT.

### google-services.json — IMPORTANT

Fișierul google-services.json conține un API key și **nu se pune pe GitHub** — e în .gitignore.

Dacă ai nevoie de el, descarcă-l direct din:
**console.firebase.google.com → LifeOS → Project Settings → General → Your apps → google-services.json**

Pune-l în app/google-services.json local și **nu îl commit-a niciodată**.

Dacă îl publici accidental pe GitHub, regenerează imediat cheia din Firebase Console → Project Settings → Your apps → Regenerate key.

### Dacă adaugi un nou API Google

1. Mergi pe **console.cloud.google.com** → proiectul `LifeOS`
2. **APIs & Services → Library** → activezi API-ul
3. **Google Auth Platform → Data Access → Add or remove scopes** → adaugi scope-ul necesar
4. În cod, adaugi scope-ul în `GoogleAccountCredential.usingOAuth2(context, listOf(...))`

---

**DAO-urile nu afectează versiunea DB.** Incrementezi `version` doar când modifici entitățile (adaugi/ștergi tabele sau coloane). Modificările în DAO-uri (query-uri noi, funcții noi) nu necesită incrementarea versiunii.

---

## 12. Reguli rapide de reținut

| ✅ Fă asta | ❌ Nu face asta |
|-----------|----------------|
| Un branch per task | Lucra direct pe `develop` sau `main` |
| Declară dependențele în Gradle | Adăuga librării doar în cod fără Gradle |
| Incrementează `version` când modifici schema DB | Schimba entități Room fără să updatezi versiunea |
| Scrie UI în engleză | Amesteca română și engleză în UI |
| Raportează bug-uri ca Issues | Rezolva bug-uri fără să le documentezi |
| Cere Approve înainte de Merge | Da Merge fără review |
| Un singur `AppDatabase` pentru tot proiectul | Crea baze de date separate per modul |
