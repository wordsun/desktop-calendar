@echo off
chcp 65001 >nul
echo ========================================
echo   Desktop Calendar - Build & Package
echo ========================================
echo.

set JAVA_HOME=D:\sweet_L\CLOUD\jdk\openjdk-26.0.1
set MVN=D:\Program Files\maven\apache-maven-3.8.8\bin\mvn.cmd
set PROJECT_DIR=D:\sweet_L\CLOUD\Cloud
set JFX_REPO=D:\Program Files\maven\apache-maven-3.8.8\maven_repository\org\openjfx
set JFX_VER=21.0.6

echo [1/4] Building fat jar with Maven...
cd /d "%PROJECT_DIR%"
call "%MVN%" clean package -DskipTests -q
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Maven build failed!
    pause
    exit /b 1
)
echo     Done.
echo.

echo [2/4] Creating custom runtime with jlink...
set JFX_MP=%JFX_REPO%\javafx-base\%JFX_VER%\javafx-base-%JFX_VER%.jar;%JFX_REPO%\javafx-base\%JFX_VER%\javafx-base-%JFX_VER%-win.jar;%JFX_REPO%\javafx-controls\%JFX_VER%\javafx-controls-%JFX_VER%.jar;%JFX_REPO%\javafx-controls\%JFX_VER%\javafx-controls-%JFX_VER%-win.jar;%JFX_REPO%\javafx-fxml\%JFX_VER%\javafx-fxml-%JFX_VER%.jar;%JFX_REPO%\javafx-fxml\%JFX_VER%\javafx-fxml-%JFX_VER%-win.jar;%JFX_REPO%\javafx-graphics\%JFX_VER%\javafx-graphics-%JFX_VER%.jar;%JFX_REPO%\javafx-graphics\%JFX_VER%\javafx-graphics-%JFX_VER%-win.jar

if exist "%PROJECT_DIR%\dist\javafx-runtime" rmdir /s /q "%PROJECT_DIR%\dist\javafx-runtime"
"%JAVA_HOME%\bin\jlink.exe" ^
    --module-path "%JFX_MP%" ^
    --add-modules javafx.controls,javafx.fxml,javafx.graphics,java.logging,java.net.http,java.prefs,java.desktop,jdk.unsupported ^
    --output "%PROJECT_DIR%\dist\javafx-runtime" ^
    --no-header-files --no-man-pages --strip-debug
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: jlink failed!
    pause
    exit /b 1
)
echo     Done.
echo.

echo [3/4] Packaging EXE with jpackage...
if exist "%PROJECT_DIR%\dist\DesktopCalendar" rmdir /s /q "%PROJECT_DIR%\dist\DesktopCalendar"

"%JAVA_HOME%\bin\jpackage.exe" ^
    --type app-image ^
    --runtime-image "%PROJECT_DIR%\dist\javafx-runtime" ^
    --input "%PROJECT_DIR%\target" ^
    --main-jar "calendar-plugin-1.0.0.jar" ^
    --main-class "com.desktop.calendar.Main" ^
    --name "DesktopCalendar" ^
    --dest "%PROJECT_DIR%\dist" ^
    --java-options "--add-opens=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED" ^
    --java-options "--add-exports=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED" ^
    --java-options "-Dfile.encoding=UTF-8"

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: jpackage failed!
    pause
    exit /b 1
)
echo     Done.
echo.

echo [4/4] Creating distributable ZIP...
if exist "%PROJECT_DIR%\dist\DesktopCalendar.zip" del /f /q "%PROJECT_DIR%\dist\DesktopCalendar.zip"
powershell -NoProfile -Command "Compress-Archive -Path '%PROJECT_DIR%\dist\DesktopCalendar\*' -DestinationPath '%PROJECT_DIR%\dist\DesktopCalendar.zip' -CompressionLevel Optimal -Force"
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: ZIP creation failed!
    pause
    exit /b 1
)
echo     Done.
echo.
echo ========================================
echo   Build complete!
echo.
echo   EXE:  dist\DesktopCalendar\DesktopCalendar.exe
echo   ZIP:  dist\DesktopCalendar.zip (for distribution)
echo.
echo   Send DesktopCalendar.zip to other computers,
echo   extract it, then run DesktopCalendar.exe
echo ========================================
pause
@echo off
chcp 65001 >nul
echo ========================================
echo   Desktop Calendar - Build & Package
echo ========================================
echo.

set JAVA_HOME=D:\sweet_L\CLOUD\jdk\openjdk-26.0.1
set MVN=D:\Program Files\maven\apache-maven-3.8.8\bin\mvn.cmd
set PROJECT_DIR=D:\sweet_L\CLOUD\Cloud
set JFX_REPO=D:\Program Files\maven\apache-maven-3.8.8\maven_repository\org\openjfx
set JFX_VER=21.0.6

echo [1/4] Building fat jar with Maven...
cd /d "%PROJECT_DIR%"
call "%MVN%" clean package -DskipTests -q
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Maven build failed!
    pause
    exit /b 1
)
echo     Done.
echo.

echo [2/4] Creating custom runtime with jlink...
set JFX_MP=%JFX_REPO%\javafx-base\%JFX_VER%\javafx-base-%JFX_VER%.jar;%JFX_REPO%\javafx-base\%JFX_VER%\javafx-base-%JFX_VER%-win.jar;%JFX_REPO%\javafx-controls\%JFX_VER%\javafx-controls-%JFX_VER%.jar;%JFX_REPO%\javafx-controls\%JFX_VER%\javafx-controls-%JFX_VER%-win.jar;%JFX_REPO%\javafx-fxml\%JFX_VER%\javafx-fxml-%JFX_VER%.jar;%JFX_REPO%\javafx-fxml\%JFX_VER%\javafx-fxml-%JFX_VER%-win.jar;%JFX_REPO%\javafx-graphics\%JFX_VER%\javafx-graphics-%JFX_VER%.jar;%JFX_REPO%\javafx-graphics\%JFX_VER%\javafx-graphics-%JFX_VER%-win.jar

if exist "%PROJECT_DIR%\dist\javafx-runtime" rmdir /s /q "%PROJECT_DIR%\dist\javafx-runtime"
"%JAVA_HOME%\bin\jlink.exe" ^
    --module-path "%JFX_MP%" ^
    --add-modules javafx.controls,javafx.fxml,javafx.graphics,java.logging,java.net.http,java.prefs,java.desktop,jdk.unsupported ^
    --output "%PROJECT_DIR%\dist\javafx-runtime" ^
    --no-header-files --no-man-pages --strip-debug
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: jlink failed!
    pause
    exit /b 1
)
echo     Done.
echo.

echo [3/4] Packaging EXE with jpackage...
if exist "%PROJECT_DIR%\dist\DesktopCalendar" rmdir /s /q "%PROJECT_DIR%\dist\DesktopCalendar"

"%JAVA_HOME%\bin\jpackage.exe" ^
    --type app-image ^
    --runtime-image "%PROJECT_DIR%\dist\javafx-runtime" ^
    --input "%PROJECT_DIR%\target" ^
    --main-jar "calendar-plugin-1.0.0.jar" ^
    --main-class "com.desktop.calendar.Main" ^
    --name "DesktopCalendar" ^
    --dest "%PROJECT_DIR%\dist" ^
    --java-options "--add-opens=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED" ^
    --java-options "--add-exports=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED" ^
    --java-options "-Dfile.encoding=UTF-8"

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: jpackage failed!
    pause
    exit /b 1
)
echo     Done.
echo.

echo [4/4] Creating distributable ZIP...
if exist "%PROJECT_DIR%\dist\DesktopCalendar.zip" del /f /q "%PROJECT_DIR%\dist\DesktopCalendar.zip"
powershell -NoProfile -Command "Compress-Archive -Path '%PROJECT_DIR%\dist\DesktopCalendar\*' -DestinationPath '%PROJECT_DIR%\dist\DesktopCalendar.zip' -CompressionLevel Optimal -Force"
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: ZIP creation failed!
    pause
    exit /b 1
)
echo     Done.
echo.
echo ========================================
echo   Build complete!
echo.
echo   EXE:  dist\DesktopCalendar\DesktopCalendar.exe
echo   ZIP:  dist\DesktopCalendar.zip (for distribution)
echo.
echo   Send DesktopCalendar.zip to other computers,
echo   extract it, then run DesktopCalendar.exe
echo ========================================
pause
@echo off
chcp 65001 >nul
echo ========================================
echo   Desktop Calendar - Build EXE
echo ========================================
echo.

set JAVA_HOME=D:\sweet_L\CLOUD\jdk\openjdk-26.0.1
set MVN=D:\Program Files\maven\apache-maven-3.8.8\bin\mvn.cmd
set PROJECT_DIR=D:\sweet_L\CLOUD\Cloud
set JFX_REPO=D:\Program Files\maven\apache-maven-3.8.8\maven_repository\org\openjfx
set JFX_VER=21.0.6

echo [1/3] Building fat jar with Maven...
cd /d "%PROJECT_DIR%"
call "%MVN%" clean package -DskipTests -q
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Maven build failed!
    pause
    exit /b 1
)
echo     Done.
echo.

echo [2/3] Creating custom runtime with jlink (JavaFX + all modules)...
set JFX_MP=%JFX_REPO%\javafx-base\%JFX_VER%\javafx-base-%JFX_VER%.jar;%JFX_REPO%\javafx-base\%JFX_VER%\javafx-base-%JFX_VER%-win.jar;%JFX_REPO%\javafx-controls\%JFX_VER%\javafx-controls-%JFX_VER%.jar;%JFX_REPO%\javafx-controls\%JFX_VER%\javafx-controls-%JFX_VER%-win.jar;%JFX_REPO%\javafx-fxml\%JFX_VER%\javafx-fxml-%JFX_VER%.jar;%JFX_REPO%\javafx-fxml\%JFX_VER%\javafx-fxml-%JFX_VER%-win.jar;%JFX_REPO%\javafx-graphics\%JFX_VER%\javafx-graphics-%JFX_VER%.jar;%JFX_REPO%\javafx-graphics\%JFX_VER%\javafx-graphics-%JFX_VER%-win.jar

if exist "%PROJECT_DIR%\dist\javafx-runtime" rmdir /s /q "%PROJECT_DIR%\dist\javafx-runtime"
"%JAVA_HOME%\bin\jlink.exe" ^
    --module-path "%JFX_MP%" ^
    --add-modules javafx.controls,javafx.fxml,javafx.graphics,java.logging,java.net.http,java.prefs,java.desktop,jdk.unsupported ^
    --output "%PROJECT_DIR%\dist\javafx-runtime" ^
    --no-header-files --no-man-pages --strip-debug
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: jlink failed!
    pause
    exit /b 1
)
echo     Done.
echo.

echo [3/3] Packaging EXE with jpackage...
if exist "%PROJECT_DIR%\dist\DesktopCalendar" rmdir /s /q "%PROJECT_DIR%\dist\DesktopCalendar"

"%JAVA_HOME%\bin\jpackage.exe" ^
    --type app-image ^
    --runtime-image "%PROJECT_DIR%\dist\javafx-runtime" ^
    --input "%PROJECT_DIR%\target" ^
    --main-jar "calendar-plugin-1.0.0.jar" ^
    --main-class "com.desktop.calendar.Main" ^
    --name "DesktopCalendar" ^
    --dest "%PROJECT_DIR%\dist" ^
    --java-options "--add-opens=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED" ^
    --java-options "--add-exports=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED" ^
    --java-options "-Dfile.encoding=UTF-8"

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: jpackage failed!
    pause
    exit /b 1
)
echo     Done.
echo.
echo ========================================
echo   EXE generated at:
echo   %PROJECT_DIR%\dist\DesktopCalendar\DesktopCalendar.exe
echo ========================================
pause
@echo off
chcp 65001 >nul
echo ========================================
echo   Desktop Calendar - Build EXE
echo ========================================
echo.

set JAVA_HOME=D:\sweet_L\CLOUD\jdk\openjdk-26.0.1
set MVN=D:\Program Files\maven\apache-maven-3.8.8\bin\mvn.cmd
set PROJECT_DIR=D:\sweet_L\CLOUD\Cloud
set JFX_REPO=D:\Program Files\maven\apache-maven-3.8.8\maven_repository\org\openjfx
set JFX_VER=21.0.6

echo [1/3] Building fat jar with Maven...
cd /d "%PROJECT_DIR%"
call "%MVN%" clean package -DskipTests -q
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Maven build failed!
    pause
    exit /b 1
)
echo     Done.
echo.

echo [2/3] Creating JavaFX runtime with jlink...
set JFX_MP=%JFX_REPO%\javafx-base\%JFX_VER%\javafx-base-%JFX_VER%.jar;%JFX_REPO%\javafx-base\%JFX_VER%\javafx-base-%JFX_VER%-win.jar;%JFX_REPO%\javafx-controls\%JFX_VER%\javafx-controls-%JFX_VER%.jar;%JFX_REPO%\javafx-controls\%JFX_VER%\javafx-controls-%JFX_VER%-win.jar;%JFX_REPO%\javafx-fxml\%JFX_VER%\javafx-fxml-%JFX_VER%.jar;%JFX_REPO%\javafx-fxml\%JFX_VER%\javafx-fxml-%JFX_VER%-win.jar;%JFX_REPO%\javafx-graphics\%JFX_VER%\javafx-graphics-%JFX_VER%.jar;%JFX_REPO%\javafx-graphics\%JFX_VER%\javafx-graphics-%JFX_VER%-win.jar

if exist "%PROJECT_DIR%\dist\javafx-runtime" rmdir /s /q "%PROJECT_DIR%\dist\javafx-runtime"
"%JAVA_HOME%\bin\jlink.exe" --module-path "%JFX_MP%" --add-modules javafx.controls,javafx.fxml,javafx.graphics --output "%PROJECT_DIR%\dist\javafx-runtime" --no-header-files --no-man-pages --strip-debug
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: jlink failed!
    pause
    exit /b 1
)
echo     Done.
echo.

echo [3/3] Packaging EXE with jpackage...
if exist "%PROJECT_DIR%\dist\DesktopCalendar" rmdir /s /q "%PROJECT_DIR%\dist\DesktopCalendar"

"%JAVA_HOME%\bin\jpackage.exe" ^
    --type app-image ^
    --runtime-image "%PROJECT_DIR%\dist\javafx-runtime" ^
    --input "%PROJECT_DIR%\target" ^
    --main-jar "calendar-plugin-1.0.0.jar" ^
    --main-class "com.desktop.calendar.Main" ^
    --name "DesktopCalendar" ^
    --dest "%PROJECT_DIR%\dist" ^
    --java-options "--add-opens=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED" ^
    --java-options "--add-exports=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED" ^
    --java-options "-Dfile.encoding=UTF-8"

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: jpackage failed!
    pause
    exit /b 1
)
echo     Done.
echo.
echo ========================================
echo   EXE generated at:
echo   %PROJECT_DIR%\dist\DesktopCalendar\DesktopCalendar.exe
echo ========================================
pause
@echo off
chcp 65001 >nul
echo ========================================
echo   Desktop Calendar - Build EXE
echo ========================================
echo.

set JAVA_HOME=D:\sweet_L\CLOUD\jdk\openjdk-26.0.1
set MVN=D:\Program Files\maven\apache-maven-3.8.8\bin\mvn.cmd
set PROJECT_DIR=D:\sweet_L\CLOUD\Cloud

echo [1/2] Building fat jar with Maven...
cd /d "%PROJECT_DIR%"
call "%MVN%" clean package -DskipTests -q
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Maven build failed!
    pause
    exit /b 1
)
echo     Done.
echo.

echo [2/2] Packaging EXE with jpackage...
if exist "%PROJECT_DIR%\dist\DesktopCalendar" rmdir /s /q "%PROJECT_DIR%\dist\DesktopCalendar"

"%JAVA_HOME%\bin\jpackage.exe" ^
    --type app-image ^
    --input "%PROJECT_DIR%\target" ^
    --main-jar "calendar-plugin-1.0.0.jar" ^
    --main-class "com.desktop.calendar.Main" ^
    --name "DesktopCalendar" ^
    --dest "%PROJECT_DIR%\dist" ^
    --java-options "--add-opens=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED" ^
    --java-options "--add-exports=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED" ^
    --java-options "-Dfile.encoding=UTF-8"

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: jpackage failed!
    pause
    exit /b 1
)
echo     Done.
echo.
echo ========================================
echo   EXE generated at:
echo   %PROJECT_DIR%\dist\DesktopCalendar\DesktopCalendar.exe
echo ========================================
pause
