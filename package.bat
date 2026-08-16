@echo off
echo ====================================================
echo Protein Domain Predictor - Packaging Script (Windows)
echo ====================================================
echo.

:: 1. Clean previous builds
echo [1/7] Cleaning previous build folders...
if exist bin rmdir /s /q bin
if exist dist rmdir /s /q dist
mkdir bin
mkdir dist
echo Clean complete.

:: 2. Compile Java Source Files
echo.
echo [2/7] Compiling source files...
javac -d bin src\*.java
if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed.
    exit /b %errorlevel%
)
echo Compilation successful.

:: 3. Copy resource files to bin/ (JAR classpath root)
echo.
echo [3/7] Copying resources to classpath...
copy Logo.png bin\
if %errorlevel% neq 0 (
    echo [ERROR] Failed to copy Logo.png to classpath.
    exit /b %errorlevel%
)
echo Resource copying complete.

:: 4. Package JAR
echo.
echo [4/7] Packaging ProteinDomainPredictor.jar...
jar --create --file ProteinDomainPredictor.jar --main-class Main -C bin .
if %errorlevel% neq 0 (
    echo [ERROR] JAR packaging failed.
    exit /b %errorlevel%
)
echo ProteinDomainPredictor.jar built successfully.

:: 5. Build portable app-image via jpackage
echo.
echo [5/7] Generating portable application image...
jpackage --type app-image --dest dist\app-image --name "Protein Domain Predictor" --input . --main-jar ProteinDomainPredictor.jar --main-class Main --icon Logo.ico --vendor "YashK55" --description "Graph-based structural protein domain analysis tool" --app-version "2.2.0"
if %errorlevel% neq 0 (
    echo [ERROR] Portable app-image generation failed.
    exit /b %errorlevel%
)
echo Portable app-image generated successfully at dist\app-image\'Protein Domain Predictor'

:: 6. Create portable ZIP package via PowerShell Compress-Archive
echo.
echo [6/7] Compressing portable ZIP package...
powershell -Command "Compress-Archive -Path 'dist\app-image\Protein Domain Predictor' -DestinationPath dist\ProteinDomainPredictor-Portable.zip -Force"
if %errorlevel% neq 0 (
    echo [ERROR] Zipping portable app-image failed.
    exit /b %errorlevel%
)
echo Portable ZIP built successfully at dist\ProteinDomainPredictor-Portable.zip

:: 7. Try to build Windows EXE installer
echo.
echo [7/7] Generating installer EXE...
jpackage --type exe --dest dist --name "Protein Domain Predictor" --input . --main-jar ProteinDomainPredictor.jar --main-class Main --icon Logo.ico --win-menu --win-shortcut --win-dir-chooser --win-shortcut-prompt --vendor "YashK55" --description "Graph-based structural protein domain analysis tool" --app-version "2.2.0"
if %errorlevel% neq 0 (
    echo.
    echo [WARNING] jpackage EXE installer build failed. This is likely because the WiX Toolset is not installed.
    echo WiX Toolset v3 is required on Windows to build MSI/EXE installers with jpackage.
    echo Portable app-image and ZIP packages have still been generated successfully under dist/
) else (
    if exist "dist\Protein Domain Predictor-2.2.0.exe" (
        move /y "dist\Protein Domain Predictor-2.2.0.exe" dist\ProteinDomainPredictor-Setup.exe >nul
        echo Installer EXE built successfully at dist\ProteinDomainPredictor-Setup.exe
    ) else (
        echo Installer built but output filename was unexpected. Check dist/ folder.
    )
)

echo.
echo ====================================================
echo Packaging Process Complete!
echo Outputs are located in the 'dist' directory.
echo ====================================================
