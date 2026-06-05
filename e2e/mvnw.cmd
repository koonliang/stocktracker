@ECHO OFF
WHERE mvn >NUL 2>NUL
IF %ERRORLEVEL% NEQ 0 (
  ECHO Maven is not installed. Install Maven or use a standard Maven wrapper jar.
  EXIT /B 1
)
mvn %*
