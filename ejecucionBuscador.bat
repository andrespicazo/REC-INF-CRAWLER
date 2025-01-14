@echo off
REM Compilar y ejecutar el buscador
javac Buscador.java
if %errorlevel% neq 0 (
    echo Error al compilar Buscador.java
    pause
    exit /b
)
java Buscador
if %errorlevel% neq 0 (
    echo Error al ejecutar Buscador
    pause
    exit /b
)

echo Todas las tareas se ejecutaron correctamente.
pause
