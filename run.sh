#!/bin/sh
cd "$(dirname "$0")"
mkdir -p bin
javac -encoding UTF-8 -d bin src/*.java && java -cp bin Main
