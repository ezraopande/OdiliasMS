@echo off
setlocal

:: 1. Cleanup
echo Cleaning up...
if exist target\classes rd /s /q target\classes
if exist dist_new rd /s /q dist_new
if exist target\odms.jar del /f /q target\odms.jar
mkdir target\classes

:: 2. Compile (Treating as non-modular to allow jbcrypt)
echo Compiling (Classpath Mode)...
:: We include JavaFX modules in the module-path but keep our code on the classpath
:: We ignore module-info.java to avoid JPMS enforcement (automatic module errors)
javac -d target\classes ^
--module-path "target\libs" ^
--add-modules javafx.controls,javafx.fxml ^
--class-path "target\libs\*" ^
src\main\java\com\odilias\odms\*.java ^
src\main\java\com\odilias\odms\controller\*.java ^
src\main\java\com\odilias\odms\dao\*.java ^
src\main\java\com\odilias\odms\model\*.java ^
src\main\java\com\odilias\odms\util\*.java
if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%

:: 3. Copy resources
echo Copying resources...
xcopy /e /y /q src\main\resources\* target\classes\
if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%

:: 4. Build JAR
echo Creating application JAR...
jar --create --file target\odms.jar --main-class com.odilias.odms.Launcher -C target\classes .
if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%

:: 5. Create app image
echo Packaging with jpackage (Classpath Mode)...
:: Since we are non-modular, we don't use --module-path for our own JAR in jpackage 
:: but we still need to provide JavaFX modules and the main jar
jpackage --type app-image ^
--name OdiliasMS ^
--input target ^
--main-jar odms.jar ^
--main-class com.odilias.odms.Launcher ^
--module-path "target\libs" ^
--add-modules javafx.controls,javafx.fxml,java.sql,java.naming ^
--dest dist_new
if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%

echo.
echo Build SUCCESS.
echo Output can be found at: dist_new\OdiliasMS
