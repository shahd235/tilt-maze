@echo off
rem Run the game. Examples:  run.bat   |   run.bat COM4   |   run.bat --test
java -cp "out;lib\jSerialComm-2.11.0.jar" tiltmaze.Main %*
