@echo off
REM Compilar y ejecutar CorpusCrawler
javac -cp ".;jsoup-1.18.1.jar" CorpusCrawler.java
if %errorlevel% neq 0 (
    echo Error al compilar CorpusCrawler.java
    pause
    exit /b
)
java -cp ".;jsoup-1.18.1.jar" CorpusCrawler
if %errorlevel% neq 0 (
    echo Error al ejecutar CorpusCrawler
    pause
    exit /b
)

REM Compilar y ejecutar el indexador
javac -cp . .\indexacion.java .\utility\Tupla.java .\utility\Stemmer.java .\utility\preprocesado.java
if %errorlevel% neq 0 (
    echo Error al compilar los archivos del indexador
    pause
    exit /b
)
java -cp . indexacion
if %errorlevel% neq 0 (
    echo Error al ejecutar indexacion
    pause
    exit /b
)

echo Todas las tareas se ejecutaron correctamente.
pause
