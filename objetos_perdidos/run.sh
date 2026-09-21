#!/bin/sh
# Compila y arranca el servidor (solo necesita un JDK 17 o superior).
set -e
cd "$(dirname "$0")"
rm -rf out && mkdir out
javac -encoding UTF-8 -d out $(find src -name '*.java')
exec java -cp out Main
