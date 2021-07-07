@ECHO OFF
setLocal enableExtensions enableDelayedExpansion

rem You can use -Xmx128m or less too for most examples, but it might be slower
set "jvmOptions=-Xms256m -Xmx1024m"
set "mainClass=org.drools.examples.DroolsExamplesApp"
set "mainClasspath=binaries/*;../binaries/*"

echo Usage: runExamples.bat
echo Notes:
echo - Java must be installed. Get the JRE ^(http://www.java.com^) or the JDK.
echo - For optimal performance, Java is recommended to be OpenJDK 7 or higher.
echo - For JDK, the environment variable JAVA_HOME should be set to the JDK installation directory
echo   For example: set "JAVA_HOME=C:\Program Files\Java\jdk1.6.0"
echo - The working dir should be the directory of this script.
echo.

if exist "%JAVA_HOME%\bin\java.exe" (
    echo Starting examples app with JDK from environment variable JAVA_HOME ^(%JAVA_HOME%^)...
    rem JDK supports -server mode
    "%JAVA_HOME%\bin\java" !jvmOptions! -server -cp !mainClasspath! !mainClass!
    goto endProcess
) else (
    rem Find JRE home in Windows Registry
    set "jreRegKey=HKLM\SOFTWARE\JavaSoft\Java Runtime Environment"
    set "jreVersion="
    for /f "tokens=3" %%v in ('reg query "!jreRegKey!" /v "CurrentVersion" 2^>nul') do set "jreVersion=%%v"
    if not defined jreVersion (
        set "jreRegKey=HKLM\SOFTWARE\Wow6432Node\JavaSoft\Java Runtime Environment"
        for /f "tokens=3" %%v in ('reg query "!jreRegKey!" /v "CurrentVersion" 2^>nul') do set "jreVersion=%%v"
        if not defined jreVersion (
            echo ERROR: The JRE is not installed on this system.
            goto failure
        )
    )
    set "jreHome="
    for /f "tokens=2,*" %%d in ('reg query "!jreRegKey!\!jreVersion!" /v "JavaHome" 2^>nul') do set "jreHome=%%e"
    if not defined jreHome (
        echo ERROR: The JRE is not properly installed on this system, although jreVersion ^(!jreVersion!^) exists.
        goto failure
    )

    if not exist "!jreHome!\bin\java.exe" (
        echo ERROR: The JRE home ^(!jreHome!^) does not contain java ^(bin\java.exe^).
        goto failure
    )
    echo Starting examples app with JRE from JRE home ^(!jreHome!^)...
    "!jreHome!\bin\java" !jvmOptions! -cp !mainClasspath! !mainClass!
    goto endProcess
)

:failure
    echo.
    echo Please install Java from http://www.java.com first.
    rem Prevent the terminal window to disappear before the user has seen the error message
    echo Press any key to close this window.
    pause
    goto endProcess

:endProcess
    endLocal