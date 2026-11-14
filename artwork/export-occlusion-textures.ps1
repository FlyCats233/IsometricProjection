Add-Type -AssemblyName System.Drawing
$source=[System.Drawing.Bitmap]::new((Resolve-Path artwork/occlusion-editor-states-preview.png).Path)
$outputDir=Join-Path $pwd 'src/main/resources/assets/isometricprojection/textures/item'
New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
try {
  foreach($state in 0,1) {
    $sprite=[System.Drawing.Bitmap]::new(32,32,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
      for($y=0;$y -lt 32;$y++){for($x=0;$x -lt 32;$x++){
        $sampleX=40+$state*887+[int][Math]::Floor(($x+0.5)*808/32)
        $sampleY=58+[int][Math]::Floor(($y+0.5)*808/32)
        $color=$source.GetPixel($sampleX,$sampleY)
        if($color.A -ge 128){$sprite.SetPixel($x,$y,[System.Drawing.Color]::FromArgb(255,$color.R,$color.G,$color.B))}
      }}
      if($state -eq 1) {
        # Remove the isolated bright highlight along the blue top's lower-right edge.
        $topBlue=$sprite.GetPixel(22,9)
        $sprite.SetPixel(23,9,$topBlue)
        $sprite.SetPixel(24,9,$topBlue)
      }
      $name=if($state -eq 0){'occlusion_tool'}else{'occlusion_tool_selected'}
      $sprite.Save((Join-Path $outputDir "$name.png"),[System.Drawing.Imaging.ImageFormat]::Png)
    } finally {$sprite.Dispose()}
  }
} finally {$source.Dispose()}
$preview=[System.Drawing.Bitmap]::new(768,384)
$graphics=[System.Drawing.Graphics]::FromImage($preview)
try {
  $graphics.Clear([System.Drawing.Color]::FromArgb(37,43,52))
  $graphics.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
  $graphics.PixelOffsetMode=[System.Drawing.Drawing2D.PixelOffsetMode]::Half
  foreach($state in 0,1) {
    $name=if($state -eq 0){'occlusion_tool'}else{'occlusion_tool_selected'}
    $sprite=[System.Drawing.Bitmap]::new((Join-Path $outputDir "$name.png"))
    try {$graphics.DrawImage($sprite,[System.Drawing.Rectangle]::new($state*384,0,384,384),0,0,32,32,[System.Drawing.GraphicsUnit]::Pixel)}finally{$sprite.Dispose()}
  }
  $preview.Save((Join-Path $pwd 'artwork/occlusion-editor-textures-32x-preview.png'),[System.Drawing.Imaging.ImageFormat]::Png)
} finally {$graphics.Dispose();$preview.Dispose()}
