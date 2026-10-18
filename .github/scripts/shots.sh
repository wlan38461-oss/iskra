#!/bin/bash
# Installs the APK on the emulator and takes screenshots of every screen,
# all times of day, the egg/hatching, notifications and home-screen widgets.
APK="$1"
OUT=shots
PKG=dev.daniil.iskra
ACT=$PKG/.MainActivity
mkdir -p "$OUT"

log() { echo "== $*"; echo "$*" >> "$OUT/log.txt"; }

adb wait-for-device
adb shell settings put system screen_off_timeout 1800000
adb shell svc power stayon true
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard || true
adb shell settings put global window_animation_scale 0.5
adb shell settings put global transition_animation_scale 0.5
adb install -r -g "$APK" >> "$OUT/log.txt" 2>&1
adb shell appops set $PKG SCHEDULE_EXACT_ALARM allow || true
adb shell wm size >> "$OUT/log.txt"
adb shell wm density >> "$OUT/log.txt"
adb logcat -c

shot() {
  local name=$1; shift
  adb shell am start -S -W -n $ACT "$@" > /dev/null 2>&1
  sleep "${WAIT:-5}"
  adb exec-out screencap -p > "$OUT/$name.png"
  if ! adb shell pidof $PKG > /dev/null; then log "CRASH after $name"; fi
}

swipe_up() {
  adb shell input swipe 540 1900 540 500 400
  sleep 1.5
}

# --- every part of the day, tasks + pet tabs
for ph in "1_dawn 375" "2_morning 555" "3_day 840" "4_sunset 1110" "5_dusk 1235" "6_night 1380"; do
  set -- $ph
  shot "${1}_tasks" --ez demo true --ei demo_minute "$2" --ei demo_tab 0 --ei demo_lit 12 --ei demo_done 2
  shot "${1}_pet" --ez demo true --ei demo_minute "$2" --ei demo_tab 1 --ei demo_lit 12 --ei demo_done 2
done

# --- egg and growth
shot egg0_tasks --ez demo true --ei demo_minute 600 --ei demo_tab 0 --ei demo_lit 0 --ei demo_done 1
shot egg1_pet --ez demo true --ei demo_minute 840 --ei demo_tab 1 --ei demo_lit 1 --ei demo_done 3
shot egg2_pet --ez demo true --ei demo_minute 1110 --ei demo_tab 1 --ei demo_lit 2 --ei demo_done 5
shot form1_pet --ez demo true --ei demo_minute 1380 --ei demo_tab 1 --ei demo_lit 5 --ei demo_done 6
shot form3_pet --ez demo true --ei demo_minute 1235 --ei demo_tab 1 --ei demo_lit 30 --ei demo_done 4 --ei demo_palette 3
shot smolder_pet --ez demo true --ei demo_minute 900 --ei demo_tab 1 --ei demo_lit 6 --ei demo_done 0 --ez demo_broken true
shot empty_tasks --ez demo true --ei demo_minute 555 --ei demo_tab 0 --ei demo_lit 0 --ei demo_done 0 --ei demo_tasks 0

# --- scrolled screens
shot scroll_tasks_a --ez demo true --ei demo_minute 840 --ei demo_tab 0 --ei demo_lit 12 --ei demo_done 2
swipe_up
adb exec-out screencap -p > "$OUT/scroll_tasks_b.png"
shot scroll_pet_a --ez demo true --ei demo_minute 1380 --ei demo_tab 1 --ei demo_lit 12 --ei demo_done 2
swipe_up
adb exec-out screencap -p > "$OUT/scroll_pet_b.png"
swipe_up
adb exec-out screencap -p > "$OUT/scroll_pet_c.png"
shot settings_a --ez demo true --ei demo_minute 840 --ei demo_tab 2 --ei demo_lit 12 --ei demo_done 2
swipe_up
adb exec-out screencap -p > "$OUT/settings_b.png"
swipe_up
adb exec-out screencap -p > "$OUT/settings_c.png"
swipe_up
adb exec-out screencap -p > "$OUT/settings_d.png"
shot settings_night --ez demo true --ei demo_minute 1380 --ei demo_tab 2 --ei demo_lit 12 --ei demo_done 2

# --- add-task sheet and hatching
shot sheet_day --ez demo true --ei demo_minute 840 --ei demo_tab 0 --ez demo_sheet true
shot sheet_night --ez demo true --ei demo_minute 1380 --ei demo_tab 0 --ez demo_sheet true
WAIT=2 shot hatch_shake --ez demo true --ei demo_minute 1235 --ei demo_tab 1 --ei demo_lit 3 --ez demo_hatch true
sleep 3
adb exec-out screencap -p > "$OUT/hatch_done.png"

# --- real app state for widgets and notifications (saved demo data)
adb shell am start -S -W -n $ACT --ez demo true --ez demo_save true --ei demo_minute 1235 --ei demo_lit 12 --ei demo_done 3 > /dev/null 2>&1
sleep 4
adb exec-out screencap -p > "$OUT/real_app.png"
if ! adb shell pidof $PKG > /dev/null; then log "CRASH after real_app"; fi

tap_node() {
  adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1
  adb pull /sdcard/ui.xml /tmp/ui.xml > /dev/null 2>&1
  python3 - "$1" <<'EOF'
import re, sys
try:
    x = open('/tmp/ui.xml', encoding='utf-8').read()
except Exception:
    sys.exit(0)
pat = re.compile(sys.argv[1], re.I)
best = None
for m in re.finditer(r'<node [^>]*>', x):
    n = m.group(0)
    t = (re.search(r' text="([^"]*)"', n) or [None, ''])[1]
    d = (re.search(r'content-desc="([^"]*)"', n) or [None, ''])[1]
    if pat.search(t) or pat.search(d):
        b = list(map(int, re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n).groups()))
        score = 0 if 'Button' in n else 1
        if best is None or score < best[0]:
            best = (score, (b[0] + b[2]) // 2, (b[1] + b[3]) // 2)
if best:
    print(best[1], best[2])
EOF
}

for kind in today pet strip; do
  adb shell am start -n $ACT --es demo_pin $kind > /dev/null 2>&1
  sleep 3
  adb exec-out screencap -p > "$OUT/pin_dialog_$kind.png"
  xy=$(tap_node '^(add|add to home screen|add automatically|добавить)$')
  log "pin $kind -> $xy"
  if [ -n "$xy" ]; then adb shell input tap $xy; fi
  sleep 3
done
adb shell input keyevent KEYCODE_HOME
sleep 6
adb exec-out screencap -p > "$OUT/home_widgets_dusk.png"

# tick a task right from the list widget
xy=$(tap_node 'Медитация')
log "widget tap -> $xy"
if [ -n "$xy" ]; then
  adb shell input tap $xy
  sleep 5
  adb exec-out screencap -p > "$OUT/home_widgets_after_tap.png"
fi

# daytime widgets
adb shell am start -S -W -n $ACT --ez demo true --ez demo_save true --ei demo_minute 840 --ei demo_lit 2 --ei demo_done 4 > /dev/null 2>&1
sleep 3
adb shell input keyevent KEYCODE_HOME
sleep 6
adb exec-out screencap -p > "$OUT/home_widgets_day.png"

# notifications
adb shell am start -n $ACT --es demo_notify task > /dev/null 2>&1
sleep 3
adb shell am broadcast -a dev.daniil.iskra.ALARM -n $PKG/.reminder.ReminderReceiver > /dev/null 2>&1
sleep 2
adb shell cmd statusbar expand-notifications
sleep 3
adb exec-out screencap -p > "$OUT/notifications.png"
adb shell cmd statusbar collapse

adb logcat -d -b crash > "$OUT/crash_log.txt" 2>&1
adb logcat -d -s AndroidRuntime:E > "$OUT/runtime_errors.txt" 2>&1
adb shell dumpsys alarm | grep -A3 "dev.daniil.iskra" > "$OUT/alarms.txt" 2>&1
log "done"
exit 0
