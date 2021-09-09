#call run all
rename data\all\all.nt datagovbe.nt
call edp.bat data\all\datagovbe.nt data\all\datagovbe_edp.xml
xcopy data\all\datagovbe* c:\data\dcat\all /Y

FOR /d %f in (data\*) DO XCOPY "%f\validate.html" c:\data\dcat\qa\%f\validate.html /S /Y /F
