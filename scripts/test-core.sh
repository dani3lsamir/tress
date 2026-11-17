#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p build/core-tests
javac -encoding UTF-8 -d build/core-tests app/src/main/java/app/tres/notes/Note.java app/src/main/java/app/tres/notes/Notebook.java tests/NotebookTest.java tests/ScheduleTest.java
java -cp build/core-tests NotebookTest
java -cp build/core-tests ScheduleTest
