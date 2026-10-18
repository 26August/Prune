#!/bin/sh
set -eu
ROOT="$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)"
SH="$ROOT/app/src/main/assets/debloat.sh"
TMP="$(mktemp -d 2>/dev/null || mktemp -d -t prune_test)"
trap 'rm -rf "$TMP"' EXIT INT TERM

mkdir -p "$TMP/bin" "$TMP/work"

cat > "$TMP/bin/pm" <<'PM'
#!/bin/sh
case "$1" in
  "list")
    shift
    has_u=0; has_d=0; has_user0=0
    for a in "$@"; do case "$a" in "-u") has_u=1;; "-d") has_d=1;; "--user") has_user0=1;; esac; done
    if [ "$has_d" = "1" ]; then
      echo "package:com.example.disabled1"
      echo "package:com.example.disabled2"
    elif [ "$has_u" = "1" ]; then
      echo "package:com.example.installed1"
      echo "package:com.example.installed2"
      echo "package:com.example.disabled1"
      echo "package:com.example.disabled2"
      echo "package:com.android.systemui"
      echo "package:com.miui.msa.global"
    else
      echo "package:com.example.installed1"
      echo "package:com.example.installed2"
    fi
    ;;
  "disable-user"|"uninstall"|"enable")
    echo "mock $*"
    exit 0
    ;;
  *) echo "mock pm $*" >&2; exit 0;;
esac
PM
cat > "$TMP/bin/getprop" <<'GP'
#!/bin/sh
case "$1" in
  "ro.product.brand") echo "xiaomi" ;;
  "ro.product.manufacturer") echo "xiaomi" ;;
  "ro.product.model") echo "TestDevice" ;;
  "ro.build.version.release") echo "14" ;;
  "ro.build.version.sdk") echo "34" ;;
  *) echo "unknown" ;;
esac
GP
cat > "$TMP/bin/cmd" <<'CMD'
#!/bin/sh
echo "mock cmd $*"
exit 0
CMD
cat > "$TMP/bin/id" <<'ID'
#!/bin/sh
echo "uid=0(root) gid=0(root)"
ID
chmod +x "$TMP/bin/"*

export PATH="$TMP/bin:$PATH"

denylist="$TMP/work/denylist.txt"
printf "com.android.systemui\ncom.android.providers.*\n" > "$denylist"

list="$TMP/work/selection.txt"

pass=0; fail=0
assert_contains() {
  desc="$1"; file="$2"; pat="$3"
  if grep -q "$pat" "$file" 2>/dev/null || grep -q "$pat" "$TMP/out.txt" 2>/dev/null; then
    echo "PASS $desc"; pass=$((pass+1))
  else
    echo "FAIL $desc (missing $pat)"; fail=$((fail+1))
  fi
}

echo "== test info =="
sh "$SH" info --workdir "$TMP/work" > "$TMP/out.txt" 2>&1
assert_contains "info brand" "$TMP/out.txt" "INFO|brand|xiaomi"
assert_contains "info done" "$TMP/out.txt" "DONE|"

echo "== test scan =="
sh "$SH" scan --workdir "$TMP/work" > "$TMP/out.txt" 2>&1
assert_contains "scan installed" "$TMP/out.txt" "INFO|com.example.installed1|installed"
assert_contains "scan disabled" "$TMP/out.txt" "disabled"

echo "== test disable dry-run =="
printf "com.example.installed1\ncom.miui.msa.global\n" > "$list"
sh "$SH" disable --list "$list" --workdir "$TMP/work" --dry-run > "$TMP/out.txt" 2>&1
assert_contains "dry-run ok" "$TMP/out.txt" "DRY|com.example.installed1|"
assert_contains "dry-run done" "$TMP/out.txt" "DONE|"

echo "== test denylist blocked =="
printf "com.android.systemui\ncom.android.providers.telephony\ncom.example.installed1\n" > "$list"
sh "$SH" disable --list "$list" --workdir "$TMP/work" --dry-run > "$TMP/out.txt" 2>&1
assert_contains "denylist skip exact" "$TMP/out.txt" "SKIP|com.android.systemui|"
assert_contains "denylist skip wildcard" "$TMP/out.txt" "SKIP|com.android.providers.telephony|"

echo "== test invalid package =="
printf "not_a_pkg\ncom.example.installed1\n" > "$list"
sh "$SH" disable --list "$list" --workdir "$TMP/work" --dry-run > "$TMP/out.txt" 2>&1
assert_contains "invalid skip" "$TMP/out.txt" "SKIP|not_a_pkg|"

echo "== test uninstall wipe dry-run =="
printf "com.example.installed1\n" > "$list"
sh "$SH" uninstall --list "$list" --workdir "$TMP/work" --dry-run --wipe > "$TMP/out.txt" 2>&1
assert_contains "wipe dry" "$TMP/out.txt" "pm uninstall --user 0"

echo "== test file rotation =="
for i in $(seq 1 22); do sh "$SH" backup --workdir "$TMP/work" > /dev/null 2>&1 || true; done
count=$(ls -1 "$TMP/work"/backup_*.txt 2>/dev/null | wc -l | tr -d ' ')
if [ "$count" -le 20 ]; then echo "PASS backup rotation ($count)"; pass=$((pass+1)); else echo "FAIL backup rotation ($count)"; fail=$((fail+1)); fi

echo ""
echo "Results: $pass passed, $fail failed"
[ "$fail" -eq 0 ]
