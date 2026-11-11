#!/usr/bin/env bash
set -euo pipefail

# Adapted from the official Super-agent deployment guide's prepare command.
# Run only as an Execution prepare command; the host supplies these variables.
: "${DSH_SUPERAGENT_GITHUB_REPOSITORY:?Run this script through Super-agent Execution prepare}"
: "${DSH_SUPERAGENT_TICKET:?Missing Ticket ID}"
: "${DSH_SUPERAGENT_NAME:?Missing Ticket short name}"
: "${DSH_SUPERAGENT_DIR:?Missing Ticket directory}"

DEMO_REPOSITORY_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
git -C "$DEMO_REPOSITORY_ROOT" rev-parse --git-dir >/dev/null
git -C "$DEMO_REPOSITORY_ROOT" fetch --no-tags \
  "https://github.com/${DSH_SUPERAGENT_GITHUB_REPOSITORY}.git" \
  "${DSH_SUPERAGENT_GITHUB_PR_HEAD:-HEAD}:refs/dsh-superagent/${DSH_SUPERAGENT_TICKET}"
git -C "$DEMO_REPOSITORY_ROOT" worktree add \
  -b "agent/${DSH_SUPERAGENT_NAME}" \
  "$DSH_SUPERAGENT_DIR" \
  "refs/dsh-superagent/${DSH_SUPERAGENT_TICKET}"
