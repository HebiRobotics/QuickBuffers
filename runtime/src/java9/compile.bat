@echo off
SET mypath=%~dp0
"%JAVA11%/bin/javac" -source 6 -target 6 %mypath:~0,-1%/Math9.java