@echo off
rem Build: compile the game into the "out" folder (needs lib\jSerialComm-2.11.0.jar)
if not exist out mkdir out
javac -cp "lib\jSerialComm-2.11.0.jar" -d out src\tiltmaze\*.java
