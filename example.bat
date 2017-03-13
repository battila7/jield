@echo off
@setlocal

set example=%1

if [%example%] == [] (
    echo Please specify which example to run^!

    goto:eof
)

echo Compiling examples
echo.

call gradlew.bat :jield-examples:build 

echo Executing %example%
echo.

java -cp jield-core\build\classes\main\;jield-examples\build\classes\main\ jield.examples.%example%
