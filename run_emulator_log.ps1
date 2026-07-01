$emulator = "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe"
& $emulator -avd Pixel_7 -show-kernel -verbose > emulator_output.txt 2>&1
