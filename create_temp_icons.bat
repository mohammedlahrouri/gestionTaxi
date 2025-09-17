@echo off
echo Creando iconos temporales...

REM Crear archivos ic_launcher.png
echo. > "app\src\main\res\mipmap-mdpi\ic_launcher.png"
echo. > "app\src\main\res\mipmap-hdpi\ic_launcher.png"
echo. > "app\src\main\res\mipmap-xhdpi\ic_launcher.png"
echo. > "app\src\main\res\mipmap-xxhdpi\ic_launcher.png"
echo. > "app\src\main\res\mipmap-xxxhdpi\ic_launcher.png"

REM Crear archivos ic_launcher_round.png
echo. > "app\src\main\res\mipmap-mdpi\ic_launcher_round.png"
echo. > "app\src\main\res\mipmap-hdpi\ic_launcher_round.png"
echo. > "app\src\main\res\mipmap-xhdpi\ic_launcher_round.png"
echo. > "app\src\main\res\mipmap-xxhdpi\ic_launcher_round.png"
echo. > "app\src\main\res\mipmap-xxxhdpi\ic_launcher_round.png"

echo Iconos temporales creados!
echo.
echo IMPORTANTE: Estos son archivos vacios solo para compilar.
echo Debes reemplazarlos con tu icono de taxi real en PNG. 