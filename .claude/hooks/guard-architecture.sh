#!/usr/bin/env bash
# Block writes to the human-approval-only architecture file. Exit 2 denies the call.
path="$(jq -r '.tool_input.file_path // empty')"
case "$path" in
  *docs/knowledge/ARCHITECTURE.md)
    echo "ARCHITECTURE.md is human-approval only. Propose the diff and stop." >&2
    exit 2 ;;
esac
exit 0
