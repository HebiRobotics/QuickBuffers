@echo off
SET mypath=%~dp0
java -jar "%mypath:~0,-1%\{jarfile}.jar"