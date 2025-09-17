# Script para generar iconos temporales de taxi

# Colores
$yellow = "FFD700"
$black = "000000"

# Función para crear un archivo PNG simple con PowerShell
function Create-SimplePNG {
    param(
        [string]$Path,
        [int]$Width,
        [int]$Height
    )
    
    Add-Type -AssemblyName System.Drawing
    
    $bitmap = New-Object System.Drawing.Bitmap($Width, $Height)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    
    # Fondo amarillo
    $yellowBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 215, 0))
    $graphics.FillRectangle($yellowBrush, 0, 0, $Width, $Height)
    
    # Texto TAXI en negro
    $blackBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::Black)
    $font = New-Object System.Drawing.Font("Arial", ($Height / 4), [System.Drawing.FontStyle]::Bold)
    
    $text = "TAXI"
    $textSize = $graphics.MeasureString($text, $font)
    $x = ($Width - $textSize.Width) / 2
    $y = ($Height - $textSize.Height) / 2
    
    $graphics.DrawString($text, $font, $blackBrush, $x, $y)
    
    # Guardar como PNG
    $bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    
    # Limpiar recursos
    $graphics.Dispose()
    $bitmap.Dispose()
    $yellowBrush.Dispose()
    $blackBrush.Dispose()
    $font.Dispose()
}

# Crear iconos en todos los tamaños
Write-Host "Generando iconos temporales..."

# ic_launcher.png
Create-SimplePNG -Path "app\src\main\res\mipmap-mdpi\ic_launcher.png" -Width 48 -Height 48
Create-SimplePNG -Path "app\src\main\res\mipmap-hdpi\ic_launcher.png" -Width 72 -Height 72
Create-SimplePNG -Path "app\src\main\res\mipmap-xhdpi\ic_launcher.png" -Width 96 -Height 96
Create-SimplePNG -Path "app\src\main\res\mipmap-xxhdpi\ic_launcher.png" -Width 144 -Height 144
Create-SimplePNG -Path "app\src\main\res\mipmap-xxxhdpi\ic_launcher.png" -Width 192 -Height 192

# ic_launcher_round.png (los mismos archivos por ahora)
Copy-Item "app\src\main\res\mipmap-mdpi\ic_launcher.png" -Destination "app\src\main\res\mipmap-mdpi\ic_launcher_round.png"
Copy-Item "app\src\main\res\mipmap-hdpi\ic_launcher.png" -Destination "app\src\main\res\mipmap-hdpi\ic_launcher_round.png"
Copy-Item "app\src\main\res\mipmap-xhdpi\ic_launcher.png" -Destination "app\src\main\res\mipmap-xhdpi\ic_launcher_round.png"
Copy-Item "app\src\main\res\mipmap-xxhdpi\ic_launcher.png" -Destination "app\src\main\res\mipmap-xxhdpi\ic_launcher_round.png"
Copy-Item "app\src\main\res\mipmap-xxxhdpi\ic_launcher.png" -Destination "app\src\main\res\mipmap-xxxhdpi\ic_launcher_round.png"

Write-Host "¡Iconos temporales creados!" -ForegroundColor Green
Write-Host "Puedes reemplazarlos con tu icono personalizado de taxi más tarde." -ForegroundColor Yellow 