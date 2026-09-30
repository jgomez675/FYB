#!/bin/sh
set -e
cd "$(dirname "$0")"
mvn spring-boot:run
