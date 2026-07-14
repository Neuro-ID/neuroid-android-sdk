#!/bin/bash
# One-time setup: point git at the repo's tracked hooks directory.
# Run this once after cloning: bash scripts/setup-git-hooks.sh

set -e

REPO_ROOT="$(git rev-parse --show-toplevel)"

chmod +x "$REPO_ROOT/.githooks/pre-commit"
git -C "$REPO_ROOT" config core.hooksPath .githooks

echo "Git hooks installed. pre-commit will now run ktlintCheck on staged NeuroID/*.kt files."

