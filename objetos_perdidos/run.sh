#!/bin/sh
# Arranca el servidor con Spring Boot (necesita JDK 17+ y Maven).
set -e
cd "$(dirname "$0")"
mvn spring-boot:run
