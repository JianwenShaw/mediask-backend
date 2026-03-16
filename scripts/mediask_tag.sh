#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   ./scripts/mediask_tag.sh <test|prod> [--push] [--dry-run] [--date YYYYMMDD]
#
# Tag format:
#   <env>-mediask-be-<YYYYMMDD>-<NN>
#   e.g. test-mediask-be-20260212-00

usage() {
  cat <<'EOF'
Usage:
  ./scripts/mediask_tag.sh <test|prod> [--push] [--dry-run] [--date YYYYMMDD]

Options:
  --push         Push the created tag to origin
  --dry-run      Print the next tag without creating it
  --date VALUE   Override date segment (default: current date in YYYYMMDD)
  -h, --help     Show this help message

Examples:
  ./scripts/mediask_tag.sh test
  ./scripts/mediask_tag.sh prod --push
  ./scripts/mediask_tag.sh test --date 20260212 --dry-run
EOF
}

if [[ $# -lt 1 ]]; then
  usage
  exit 1
fi

env_name="$1"
shift

if [[ "$env_name" != "test" && "$env_name" != "prod" ]]; then
  echo "Error: env must be one of: test, prod" >&2
  exit 1
fi

push_tag="false"
dry_run="false"
date_part="$(date '+%Y%m%d')"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --push)
      push_tag="true"
      shift
      ;;
    --dry-run)
      dry_run="true"
      shift
      ;;
    --date)
      if [[ $# -lt 2 ]]; then
        echo "Error: --date requires a value (YYYYMMDD)" >&2
        exit 1
      fi
      date_part="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Error: unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ ! "$date_part" =~ ^[0-9]{8}$ ]]; then
  echo "Error: date must match YYYYMMDD, got: $date_part" >&2
  exit 1
fi

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "Error: current directory is not a git repository" >&2
  exit 1
fi

prefix="${env_name}-mediask-be-${date_part}-"

max_suffix=-1
while IFS= read -r tag; do
  suffix="${tag##*-}"
  if [[ "$suffix" =~ ^[0-9]{2}$ ]]; then
    value=$((10#$suffix))
    if (( value > max_suffix )); then
      max_suffix=$value
    fi
  fi
done < <(git tag --list "${prefix}[0-9][0-9]")

next_suffix=$((max_suffix + 1))
if (( next_suffix > 99 )); then
  echo "Error: tag suffix overflow for ${prefix}NN (max 99)" >&2
  exit 1
fi
next_suffix_str="$(printf '%02d' "$next_suffix")"
new_tag="${prefix}${next_suffix_str}"

if [[ "$dry_run" == "true" ]]; then
  echo "$new_tag"
  exit 0
fi

if git rev-parse "$new_tag" >/dev/null 2>&1; then
  echo "Error: tag already exists: $new_tag" >&2
  exit 1
fi

git tag -a "$new_tag" -m "Release tag: $new_tag"
echo "Created tag: $new_tag"

if [[ "$push_tag" == "true" ]]; then
  git push origin "$new_tag"
  echo "Pushed tag to origin: $new_tag"
fi
