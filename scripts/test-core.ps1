$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$out = Join-Path $root 'build\core-tests'
New-Item -ItemType Directory -Path $out -Force | Out-Null
$sources = @(
    (Join-Path $root 'app\src\main\java\app\tres\notes\Note.java'),
    (Join-Path $root 'app\src\main\java\app\tres\notes\Notebook.java'),
    (Join-Path $root 'tests\NotebookTest.java')
    (Join-Path $root 'tests\ScheduleTest.java')
)
& "$env:JAVA_HOME\bin\javac.exe" -encoding UTF-8 -d $out $sources
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
& "$env:JAVA_HOME\bin\java.exe" -cp $out NotebookTest
if ($LASTEXITCODE -ne 0) { throw 'Regression tests failed.' }
& "$env:JAVA_HOME\bin\java.exe" -cp $out ScheduleTest
if ($LASTEXITCODE -ne 0) { throw 'Schedule tests failed.' }
