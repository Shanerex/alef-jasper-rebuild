#!/usr/bin/env bash
# Refuse to end the turn on a red suite. Exit 2 = keep working.
# Scoped to run only when a build file exists, so it stays quiet during early scaffolding.
# Swap the runner per module (mvn for api, npm for web).
if [ -f api/pom.xml ]; then
  if ! (cd api && ./mvnw -q test >/tmp/agentic-test.log 2>&1); then
    echo "api test suite is red. Fix before this turn ends. See /tmp/agentic-test.log" >&2
    exit 2
  fi
fi
exit 0
