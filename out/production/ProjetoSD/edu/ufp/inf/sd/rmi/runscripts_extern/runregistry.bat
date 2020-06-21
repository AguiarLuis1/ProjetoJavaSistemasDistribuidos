@REM *************************************************
@REM Description: start rmi registry
@REM Author: Rui Moreira
@REM Date: 20/02/2005
@REM *************************************************
call setenv

cd %ABSPATH2CLASSES%
@REM cls
@REM start rmiregistry
%JDK%\bin\rmiregistry