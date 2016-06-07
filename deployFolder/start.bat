@echo off
setLocal enableDelayedExpansion
set cp=.
FOR %%i IN ("%~dp0libs\*.jar") DO set cp=!cp!;%%~fsi
start "JReuters" java -server -cp %cp% com.puxtech.reuters.rfa.RelayServer.RelayServer