#!/system/bin/sh
set -eu

WORKDIR=""
LIST_FILE=""
DRY_RUN=0
WIPE=0
ACTION=""

DENYLIST_FILE=""
SCRIPT_DIR=""

is_valid_pkg() {
  case "$1" in
    ""|*[!a-zA-Z0-9._]*|.*|*..*|*.) return 1 ;;
    *.*) return 0 ;;
    *) return 1 ;;
  esac
}

is_denylisted() {
  pkg="$1"
  if [ -n "$DENYLIST_FILE" ] && [ -f "$DENYLIST_FILE" ]; then
    while IFS= read -r pat || [ -n "$pat" ]; do
      pat="$(printf '%s' "$pat" | tr -d '\r' | sed 's/^[[:space:]]*//;s/[[:space:]]*//')"
      [ -z "$pat" ] && continue
      case "$pat" in \#*) continue;; esac
      case "$pat" in
        *\*)
          prefix="${pat%\*}"
          case "$pkg" in "$prefix"*) return 0;; esac
          ;;
        *)
          [ "$pkg" = "$pat" ] && return 0
          ;;
      esac
    done < "$DENYLIST_FILE"
  fi
  return 1
}

resolve_denylist() {
  if [ -n "$WORKDIR" ] && [ -f "$WORKDIR/denylist.txt" ]; then
    DENYLIST_FILE="$WORKDIR/denylist.txt"
  elif [ -n "$SCRIPT_DIR" ] && [ -f "$SCRIPT_DIR/denylist.txt" ]; then
    DENYLIST_FILE="$SCRIPT_DIR/denylist.txt"
  elif [ -f "./denylist.txt" ]; then
    DENYLIST_FILE="./denylist.txt"
  fi
}

require_root() {
  if [ "$(id -u)" != "0" ]; then
    echo "FAIL|root|not running as uid 0" >&2
    echo "FAIL|root|not running as uid 0"
    exit 3
  fi
}

do_backup() {
  [ "$DRY_RUN" = "1" ] && return 0
  [ -z "$WORKDIR" ] && return 0
  mkdir -p "$WORKDIR"
  ts="$(date +%Y%m%d_%H%M%S 2>/dev/null || echo unknown)"
  bk="$WORKDIR/backup_$ts.txt"
  if ! pm list packages -u > "$bk" 2>/dev/null; then
    echo "FAIL|backup|cannot write backup"
    exit 4
  fi
  echo "INFO|backup|$bk"
  n=20
  # shellcheck disable=SC2012
  count="$(ls -1 "$WORKDIR"/backup_*.txt 2>/dev/null | wc -l | tr -d ' ')"
  if [ "$count" -gt "$n" ]; then
    # shellcheck disable=SC2012
    ls -1t "$WORKDIR"/backup_*.txt 2>/dev/null | tail -n +"$((n+1))" | while IFS= read -r f; do rm -f "$f"; done
  fi
}

log_file=""
init_log() {
  [ -z "$WORKDIR" ] && return 0
  mkdir -p "$WORKDIR"
  ts="$(date +%Y%m%d_%H%M%S 2>/dev/null || echo unknown)"
  log_file="$WORKDIR/exec_${ACTION}_${ts}.log"
  : > "$log_file" 2>/dev/null || true
}

log_line() {
  echo "$1"
  if [ -n "$log_file" ]; then
    echo "$1" >> "$log_file" 2>/dev/null || true
  fi
}

do_info() {
  brand="$(getprop ro.product.brand 2>/dev/null || echo unknown)"
  manufacturer="$(getprop ro.product.manufacturer 2>/dev/null || echo unknown)"
  model="$(getprop ro.product.model 2>/dev/null || echo unknown)"
  android="$(getprop ro.build.version.release 2>/dev/null || echo unknown)"
  sdk="$(getprop ro.build.version.sdk 2>/dev/null || echo unknown)"
  log_line "INFO|brand|$brand"
  log_line "INFO|manufacturer|$manufacturer"
  log_line "INFO|model|$model"
  log_line "INFO|android|$android"
  log_line "INFO|sdk|$sdk"
  log_line "DONE|0|0|0|0"
}

do_scan() {
  installed="$(pm list packages --user 0 2>/dev/null | sed 's/^package://' | sort || true)"
  disabled="$(pm list packages -d --user 0 2>/dev/null | sed 's/^package://' | sort || true)"
  uninstalled="$(pm list packages -u --user 0 2>/dev/null | sed 's/^package://' | sort || true)"

  tmp_all="$(mktemp 2>/dev/null || echo /data/local/tmp/prune_scan_all.$$)"
  tmp_dis="$(mktemp 2>/dev/null || echo /data/local/tmp/prune_scan_dis.$$)"
  tmp_uni="$(mktemp 2>/dev/null || echo /data/local/tmp/prune_scan_uni.$$)"
  tmp_inst="$(mktemp 2>/dev/null || echo /data/local/tmp/prune_scan_inst.$$)"
  printf '%s\n' "$uninstalled" > "$tmp_all"
  printf '%s\n' "$disabled" > "$tmp_dis"
  printf '%s\n' "$installed" > "$tmp_inst"

  sort -u "$tmp_all" 2>/dev/null | while IFS= read -r pkg || [ -n "$pkg" ]; do
    [ -z "$pkg" ] && continue
    if grep -qxF "$pkg" "$tmp_dis" 2>/dev/null; then
      log_line "INFO|$pkg|disabled"
    elif grep -qxF "$pkg" "$tmp_inst" 2>/dev/null; then
      log_line "INFO|$pkg|installed"
    else
      log_line "INFO|$pkg|uninstalled"
    fi
  done
  rm -f "$tmp_all" "$tmp_dis" "$tmp_uni" "$tmp_inst" 2>/dev/null || true
  log_line "DONE|0|0|0|0"
}

process_list() {
  total=0; ok=0; fail=0; skip=0
  require_root
  if [ -z "$LIST_FILE" ] || [ ! -f "$LIST_FILE" ]; then
    echo "FAIL|args|missing --list file"
    exit 2
  fi
  resolve_denylist
  do_backup
  init_log

  while IFS= read -r pkg || [ -n "$pkg" ]; do
    pkg="$(printf '%s' "$pkg" | tr -d '\r' | sed 's/^[[:space:]]*//;s/[[:space:]]*//')"
    [ -z "$pkg" ] && continue
    case "$pkg" in \#*) continue;; esac
    total=$((total+1))

    if ! is_valid_pkg "$pkg"; then
      log_line "SKIP|$pkg|invalid package name"
      skip=$((skip+1))
      continue
    fi
    if is_denylisted "$pkg"; then
      log_line "SKIP|$pkg|blocked by denylist"
      skip=$((skip+1))
      continue
    fi

    if [ "$ACTION" = "disable" ] || [ "$ACTION" = "uninstall" ]; then
      if ! pm list packages --user 0 2>/dev/null | grep -q "^package:${pkg}$"; then
        if pm list packages -u --user 0 2>/dev/null | grep -q "^package:${pkg}$"; then
          :
        else
          log_line "SKIP|$pkg|not installed"
          skip=$((skip+1))
          continue
        fi
      fi
    fi

    if [ "$DRY_RUN" = "1" ]; then
      case "$ACTION" in
        disable) log_line "DRY|$pkg|pm disable-user --user 0 $pkg" ;;
        uninstall)
          if [ "$WIPE" = "1" ]; then log_line "DRY|$pkg|pm uninstall --user 0 $pkg"
          else log_line "DRY|$pkg|pm uninstall -k --user 0 $pkg"; fi ;;
        restore) log_line "DRY|$pkg|pm enable $pkg; cmd package install-existing --user 0 $pkg" ;;
      esac
      ok=$((ok+1))
      continue
    fi

    case "$ACTION" in
      disable)
        out="$(pm disable-user --user 0 "$pkg" 2>&1)"
        rc=$?
        if [ $rc -eq 0 ]; then log_line "OK|$pkg|disabled"; ok=$((ok+1))
        else log_line "FAIL|$pkg|$out"; fail=$((fail+1)); fi
        ;;
      uninstall)
        if [ "$WIPE" = "1" ]; then out="$(pm uninstall --user 0 "$pkg" 2>&1)"; rc=$?
        else out="$(pm uninstall -k --user 0 "$pkg" 2>&1)"; rc=$?
        fi
        if [ $rc -eq 0 ]; then log_line "OK|$pkg|uninstalled"; ok=$((ok+1))
        else log_line "FAIL|$pkg|$out"; fail=$((fail+1)); fi
        ;;
      restore)
        out1="$(pm enable "$pkg" 2>&1)"; rc1=$?
        out2="$(cmd package install-existing --user 0 "$pkg" 2>&1)"; rc2=$?
        if [ $rc2 -eq 0 ] || [ $rc1 -eq 0 ]; then log_line "OK|$pkg|restored"; ok=$((ok+1))
        else log_line "FAIL|$pkg|$out1; $out2"; fail=$((fail+1)); fi
        ;;
    esac
  done < "$LIST_FILE"

  log_line "DONE|$total|$ok|$fail|$skip"
  if [ "$fail" -gt 0 ]; then exit 1; else exit 0; fi
}

usage() {
  echo "Usage: sh debloat.sh <info|scan|disable|uninstall|restore|backup> [--list FILE] [--dry-run] [--wipe] [--workdir DIR]" >&2
  exit 2
}

[ $# -lt 1 ] && usage
ACTION="$1"; shift
case "$ACTION" in info|scan|disable|uninstall|restore|backup) ;; *) usage;; esac

while [ $# -gt 0 ]; do
  case "$1" in
    --list) LIST_FILE="$2"; shift 2 ;;
    --dry-run) DRY_RUN=1; shift ;;
    --wipe) WIPE=1; shift ;;
    --workdir) WORKDIR="$2"; shift 2 ;;
    --denylist) DENYLIST_FILE="$2"; shift 2 ;;
    *) echo "FAIL|args|unknown option $1"; usage ;;
  esac
done

SCRIPT_DIR="$(dirname "$0" 2>/dev/null || echo .)"

case "$ACTION" in
  info) do_info ;;
  scan) do_scan ;;
  backup)
    require_root
    WORKDIR="${WORKDIR:-.}"
    mkdir -p "$WORKDIR"
    ts="$(date +%Y%m%d_%H%M%S 2>/dev/null || echo unknown)"
    bk="$WORKDIR/backup_$ts.txt"
    if pm list packages -u > "$bk" 2>/dev/null; then
      log_line "OK|backup|$bk"
      log_line "DONE|1|1|0|0"
    else
      echo "FAIL|backup|failed"; exit 4
    fi
    ;;
  disable|uninstall|restore) process_list ;;
esac
