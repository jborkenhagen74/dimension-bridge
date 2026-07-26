#!/usr/bin/env sh
set -eu
exec gradle clean buildAll "$@"
