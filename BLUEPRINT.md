

---

# 📱 APP BLUEPRINT: ScheduleJS

## 1. TECH STACK & ARCHITECTURE
*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose (allows you to build the UI with code, much faster than XML).
*   **Architecture Pattern:** MVVM (Model-View-ViewModel).
*   **Local Database:** Room (SQLite wrapper — keeps the app 100% offline, lightning-fast, and battery-efficient).
*   **Background Tasks:** `AlarmManager` (for exact notifications like your 35-min transit) and `Foreground Service` (for the persistent top-bar notification).

---

## 2. DATABASE SCHEMA (The "Brain" of the App)
Instead of adding tasks every day, the database holds your templates. You will need 4 main tables (Entities in Room):

1.  **Day_Template_Table:**
    *   `id` (Int)
    *   `day_type` (String: "Class_Day", "Office_Day", "Friday")
    *   `wake_up_time` (String: "06:30", "07:30")
2.  **Task_Table:**
    *   `id` (Int)
    *   `template_id` (Foreign Key)
    *   `title` (String: e.g., "The 35-Min Sprint")
    *   `start_time` (Time)
    *   `end_time` (Time)
    *   `category` (String: Study, Workout, Transit, Office, Routine)
    *   `details` (Text: e.g., "1. Cold Face Wash 2. Shower 3. Lunch...")
3.  **Study_Rotation_Table & Workout_Table:**
    *   Maps specific days to specific subjects/muscle groups so the app knows *what* to show on the Dashboard automatically.
4.  **Weekly_Review_Log:**
    *   `date` (Date)
    *   `q1_covered`, `q2_behind`, `q3_tuition`, `q4_energy`, `q5_adjustment` (Text)

---

## 3. UI/UX DESIGN & SCREEN FLOW

### Screen 1: The Dashboard (Home)
*   **Top HUD (Fixed):** 
    *   Current Day & Date.
    *   **Massive Progress Bar:** Visualizing the current block. 
    *   **Text:** *"CURRENT: Office (14:00 - 17:40) | REMAINING: 1h 15m"*
    *   **Text:** *"NEXT: Transit (Bicycle to Home) at 17:40"*
*   **Bottom Section (Scrollable):** 
    *   Vertical timeline of today's tasks.
    *   Past tasks are greyed out. Future tasks are standard color. Current task is highlighted in bright blue/green.
    *   Tapping a task expands a dropdown with the `details` string.

### Screen 2: The Workout Module
*   **Header:** "Today: [Legs]" (Fetched automatically based on the day).
*   **Big Button:** `[START 5-MIN BELLY ROUTINE]`
    *   *Action:* Opens a full-screen timer. Shows "Plank (1 min)". Beeps at 60s. Switches to "Stomach Vacuum (10 reps)".
*   **List:** Displays today's specific exercises, sets, and reps.
*   **Checkbox:** A single button at the bottom: `[MARK WORKOUT COMPLETE]`

### Screen 3: The Study Module
*   **Header:** "Morning Block: [Linear Algebra]" & "Evening Block: [Physics 1]"
*   **Strategy Reminder Alert:** *"Solve NU Board Questions. Don't just read."*
*   **The Focus Engine:**
    *   A button: `[ENTER DEEP WORK - 60 MIN]`
    *   *Action:* Triggers a countdown timer. Uses Android's `NotificationManager` API to automatically put the phone in **Do Not Disturb (DND)** mode so nobody can text or call you during study. Restores normal volume when the timer ends.

### Screen 4: The Friday Review
*   **State 1 (Sat-Thu):** Screen is locked. Shows a padlock icon and text: *"Review unlocks Friday at 15:30."*
*   **State 2 (Friday 15:30):** Unlocks into a form with your 5 questions.
*   **History Button:** Lets you read past weeks' logs to spot patterns in your energy levels or study deficits.

### Screen 5: Settings & Automation
*   **Notification Lead Time:** Dropdown (On Time, 5 mins before, 10 mins before).
*   **Transit Alerts Toggle:** Turn on/off specialized loud alarms for your bicycle departures.
*   **Edit Templates:** The CRUD system to tweak your 3 Master Day schedules if college times change.

---

## 4. THE 3 "MAGIC" FEATURES (Must Implement)

**1. The "Zero-Friction" Night Checklist Modal**
*   **Trigger:** Exactly at 21:25 every night, a full-screen popup appears over whatever you are doing on your phone.
*   **UI:** 
    *   *Title:* "Prepare for Tomorrow."
    *   *Checklist:* [ ] Clothes laid out? [ ] Lunch prepped? [ ] Bag packed?
    *   You cannot dismiss the popup until all three boxes are checked. This alone will save you from failing the 35-minute transit window.

**2. The Persistent Notification**
*   Create an Android `Foreground Service`. This places an un-swipeable notification in your phone's drop-down menu.
*   Whenever you pull down your notifications, you instantly see: *"Currently: Studying Calculus. 15 mins left."* You don't even have to open the app.

**3. Home Screen Widget**
*   A 4x2 widget for your Android home screen. Updates every minute to show the HUD.

---

## 5. DEVELOPMENT ROADMAP (How to build it)

As an intern and student, do not try to code this all at once. Build it in 4 distinct phases:

*   **Phase 1: The UI Shell (Week 1)**
    *   Design the Dashboard, Workout, and Study pages in Jetpack Compose using *dummy text* (hardcoded strings). Don't worry about databases yet. Just make it look right. 
    *   Implement Dark Mode natively.
*   **Phase 2: The Brain (Week 2)**
    *   Set up the Room Database.
    *   Write the logic that checks what day of the week it is (e.g., `if Calendar.DAY_OF_WEEK == SUNDAY`), and loads the "Class Day" template and "Back + Core" workout to the UI.
*   **Phase 3: The Clocks (Week 3)**
    *   Build the countdown logic for the HUD.
    *   Build the 5-min belly timer and the Study Focus timer.
*   **Phase 4: The Enforcer (Week 4)**
    *   Implement `AlarmManager` for notifications.
    *   Add the 21:25 Night Checklist modal.
    *   Add the DND (Do Not Disturb) permissions for the study timer.

