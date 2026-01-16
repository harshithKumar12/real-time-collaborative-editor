@echo off
echo Starting Collaborative Editor Client...
echo.

REM Set your JavaFX path here (must match compile.bat)
set JAVAFX_PATH=C:\Users\kotes\Desktop\javafx-sdk-21.0.9\lib

java --module-path "%JAVAFX_PATH%" --add-modules javafx.controls -cp out client.ClientMain
pause