@echo off
cd /d "%~dp0"
if not exist .venv\Scripts\python.exe py -3 -m venv .venv
if errorlevel 1 goto failed
.venv\Scripts\python.exe -m pip install -r requirements.txt
if errorlevel 1 goto failed
.venv\Scripts\python.exe -m PyInstaller --noconfirm --clean --onefile --windowed --name DeskPilot --collect-all keyring --hidden-import keyring.backends.Windows --collect-all pyautogui --collect-all keyboard app.py
if errorlevel 1 goto failed
 echo Ready: dist\DeskPilot.exe
 pause
 exit /b 0
:failed
 echo Build failed. See errors above.
 pause
 exit /b 1
