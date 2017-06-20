call clean.bat %1
echo %ERRORLEVEL%

call scrape.bat %1
echo %ERRORLEVEL%

call enhance.bat %1
echo %ERRORLEVEL%

call validate.bat %1
echo %ERRORLEVEL%
