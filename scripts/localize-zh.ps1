param(
    [string]$Root = (Join-Path $PSScriptRoot '..')
)

$ErrorActionPreference = 'Stop'

$mapPath = Join-Path $PSScriptRoot 'zh-map.json'
$map = Get-Content -LiteralPath $mapPath -Raw -Encoding UTF8 | ConvertFrom-Json

function Write-Utf8NoBom([string]$path, [string]$text) {
    [System.IO.File]::WriteAllText(
        $path,
        $text,
        (New-Object System.Text.UTF8Encoding $false)
    )
}

function Get-RelativePath([string]$base, [string]$path) {
    $root = [System.IO.Path]::GetFullPath($base).TrimEnd('\') + '\'
    $full = [System.IO.Path]::GetFullPath($path)
    if ($full.StartsWith($root, [System.StringComparison]::OrdinalIgnoreCase)) {
        return $full.Substring($root.Length)
    }
    return $full
}

function Apply-QuotedReplacements([string]$text, $entries) {
    $total = 0
    foreach ($entry in $entries) {
        $src = [string]$entry.source
        $dst = [string]$entry.translation
        if ($src -eq $dst) { continue }

        $needles = @(
            '"' + $src + '"'
            "'" + $src + "'"
        )
        foreach ($needle in $needles) {
            $count = ([regex]::Matches($text, [regex]::Escape($needle))).Count
            if ($count -gt 0) {
                $text = $text.Replace($needle, $needle[0] + $dst + $needle[0])
                $total += $count
            }
        }
    }
    return @{ text = $text; count = $total }
}

function Apply-PatternReplacements([string]$text, $tokens) {
    $total = 0
    foreach ($token in $tokens) {
        $src = [string]$token.source
        $dst = [string]$token.translation
        if ($src -eq $dst) { continue }
        $needle = $src
        $count = ([regex]::Matches($text, [regex]::Escape($needle))).Count
        if ($count -gt 0) {
            $text = $text.Replace($needle, $dst)
            $total += $count
        }
    }
    return @{ text = $text; count = $total }
}

function Apply-ClusterReplacements([string]$text, $entries, [bool]$html) {
    $total = 0
    $quoteStyles = @("'", '"')

    # Quoted display labels and ordinary UI strings. Regen values and state
    # enums are excluded here; they are only translated in explicit UI contexts.
    $valueOnly = @(
        'Normal', 'Reduzido', 'Clean', 'Esportivo Clean',
        'ALTO', 'NORMAL', 'BAIXO',
        'External', 'Internal', 'WARN', 'SCORE', 'ODO '
    )
    $tokens = @()
    foreach ($entry in $entries) {
        $src = [string]$entry.source
        $dst = [string]$entry.translation
        if ($src -eq $dst) { continue }
        if ($valueOnly -contains $src) { continue }
        foreach ($q in $quoteStyles) {
            $tokens += @{ source = $q + $src + $q; translation = $q + $dst + $q }
        }
    }
    $r = Apply-PatternReplacements -text $text -tokens $tokens
    $text = $r.text
    $total += $r.count

    # Only UI labels, never value/enum fields. Both quote styles are tried
    # because sources use single quotes while bundled HTML uses double quotes.
    $labels = @()
    foreach ($q in $quoteStyles) {
        foreach ($sep in @(':', ': ')) {
            $labels += @(
                @{ source = 'label' + $sep + $q + 'Normal' + $q; translation = 'label' + $sep + $q + '标准' + $q }
                @{ source = 'label' + $sep + $q + 'Reduzido' + $q; translation = 'label' + $sep + $q + '精简' + $q }
                @{ source = 'label' + $sep + $q + 'Clean' + $q; translation = 'label' + $sep + $q + '简洁' + $q }
                @{ source = 'label' + $sep + $q + 'Esportivo Clean' + $q; translation = 'label' + $sep + $q + '运动简洁' + $q }
                @{ source = 'displayLabel' + $sep + $q + 'ALTO' + $q; translation = 'displayLabel' + $sep + $q + '高' + $q }
                @{ source = 'displayLabel' + $sep + $q + 'NORMAL' + $q; translation = 'displayLabel' + $sep + $q + '正常' + $q }
                @{ source = 'displayLabel' + $sep + $q + 'BAIXO' + $q; translation = 'displayLabel' + $sep + $q + '低' + $q }
                @{ source = 'children' + $sep + $q + 'External' + $q; translation = 'children' + $sep + $q + '室外' + $q }
                @{ source = 'children' + $sep + $q + 'Internal' + $q; translation = 'children' + $sep + $q + '车内' + $q }
                @{ source = 'children' + $sep + $q + 'WARN' + $q; translation = 'children' + $sep + $q + '警示' + $q }
                @{ source = 'children' + $sep + $q + 'SCORE' + $q; translation = 'children' + $sep + $q + '评分' + $q }
                @{ source = 'children' + $sep + $q + 'ODO ' + $q; translation = 'children' + $sep + $q + '里程 ' + $q }
            )
        }
    }
    $r = Apply-PatternReplacements -text $text -tokens $labels
    $text = $r.text
    $total += $r.count

    return @{ text = $text; count = $total }
}

function Get-MaintenanceTemplates([bool]$source) {
    if ($source) {
        # Non-minified JS sources keep the accent characters and full
        # placeholder names used in the odometer component.
        return @(
            @{ source = '`Manutenção em: ${remainingKm} Km`'; translation = '`保养：${remainingKm} 公里`' }
            @{ source = '` ou ${remainingDays} dias`'; translation = '`或 ${remainingDays} 天`' }
        )
    }

    # Minified HTML ships escaped Portuguese bytes and per-build placeholder
    # names; match the literal template text in each variant.
    return @(
        @{ source = '`Manuten\xe7\xe3o em: ${t} Km`'; translation = '`保养：${t} 公里`' }
        @{ source = '`Manuten\xe7\xe3o em: ${n} Km`'; translation = '`保养：${n} 公里`' }
        @{ source = '` ou ${i} dias`'; translation = '`或 ${i} 天`' }
        @{ source = '` ou ${n} dias`'; translation = '`或 ${n} 天`' }
    )
}

$totalReplaced = 0

# Android Kotlin/Java UI strings. Only quoted literals are touched, so
# identifiers, enum names and car data constants are never rewritten.
$sourceFiles = Get-ChildItem -Path (Join-Path $Root 'app\src\main\java') -Recurse -Include *.kt, *.java
foreach ($file in $sourceFiles) {
    $text = [System.IO.File]::ReadAllText($file.FullName, [System.Text.Encoding]::UTF8)
    $before = $text

    $r1 = Apply-QuotedReplacements -text $text -entries $map.exact
    $text = $r1.text
    $count = $r1.count

    foreach ($entry in $map.substring) {
        $src = [string]$entry.source
        $dst = [string]$entry.translation
        if ($src -eq $dst) { continue }
        $c = ([regex]::Matches($text, [regex]::Escape($src))).Count
        if ($c -gt 0) {
            $text = $text.Replace($src, $dst)
            $count += $c
        }
    }

    if ($count -gt 0) {
        $relative = Get-RelativePath $Root $file.FullName
        Write-Utf8NoBom $file.FullName $text
        Write-Host ("{0}: {1} replacements" -f $relative, $count)
        $totalReplaced += $count
    }
}

# "Normal" is a data value almost everywhere; localize only the known UI labels.
$normalFiles = @(
    'app\src\main\java\br\com\redesurftank\havalshisuku\ui\components\BottomBarUI.kt'
    'app\src\main\java\br\com\redesurftank\havalshisuku\models\screens\MainMenu.java'
    'app\src\main\java\br\com\redesurftank\havalshisuku\models\screens\RegenScreen.java'
)
foreach ($rel in $normalFiles) {
    $file = Join-Path $Root $rel
    if (-not (Test-Path -LiteralPath $file)) { continue }
    $text = [System.IO.File]::ReadAllText($file, [System.Text.Encoding]::UTF8)
    $needle = '"Normal"'
    $count = ([regex]::Matches($text, [regex]::Escape($needle))).Count
    if ($count -gt 0) {
        $text = $text.Replace($needle, '"标准"')
        Write-Utf8NoBom $file $text
        Write-Host ("{0}: {1} Normal label replacements" -f $rel, $count)
        $totalReplaced += $count
    }
}

# Aircon theme UI strings live in JS sources and the inlined HTML shipped to the
# Android raw resources. This map is intentionally applied only to these files.
$themeFiles = @(
    Get-ChildItem -Path (Join-Path $Root 'cluster-widgets') -Recurse -Filter *.js |
        Where-Object { $_.FullName -match '\\aircon\\' }
    Get-Item -Path (Join-Path $Root 'app\src\main\res\raw\app.html')
    Get-Item -Path (Join-Path $Root 'app\src\main\res\raw\app_light.html')
    Get-ChildItem -Path (Join-Path $Root 'cluster-widgets\Themes') -Recurse -Filter *.html
)
foreach ($file in $themeFiles) {
    $text = [System.IO.File]::ReadAllText($file.FullName, [System.Text.Encoding]::UTF8)
    $r = Apply-QuotedReplacements -text $text -entries $map.theme
    if ($r.count -gt 0) {
        Write-Utf8NoBom $file.FullName $r.text
        $relative = Get-RelativePath $Root $file.FullName
        Write-Host ("{0}: {1} theme replacements" -f $relative, $r.count)
        $totalReplaced += $r.count
    }
}

# Cluster dashboard UI strings live in the three theme JS sources and in the
# already-minified HTML shipped to the app. The HTML files are bundled verbatim
# by the Android build, so they get the same replacements here.
$clusterJs = Get-ChildItem -Path (Join-Path $Root 'cluster-widgets') -Recurse -Filter *.js |
    Where-Object { $_.FullName -match '\\basic\\src\\|\\basic-light\\src\\|\\default\\src\\' }
foreach ($file in $clusterJs) {
    $text = [System.IO.File]::ReadAllText($file.FullName, [System.Text.Encoding]::UTF8)
    $r = Apply-ClusterReplacements -text $text -entries $map.cluster
    $r2 = Apply-PatternReplacements -text $r.text -tokens (Get-MaintenanceTemplates -source $true)
    $r.count += $r2.count
    if ($r.count -gt 0) {
        Write-Utf8NoBom $file.FullName $r2.text
        $relative = Get-RelativePath $Root $file.FullName
        Write-Host ("{0}: {1} cluster replacements" -f $relative, $r.count)
        $totalReplaced += $r.count
    }
}

$clusterHtml = @(
    Get-Item -Path (Join-Path $Root 'app\src\main\res\raw\app.html')
    Get-Item -Path (Join-Path $Root 'app\src\main\res\raw\app_light.html')
    Get-Item -Path (Join-Path $Root 'cluster-widgets\Themes\Basic\index.html')
    Get-Item -Path (Join-Path $Root 'cluster-widgets\Themes\BasicLight\app_light.html')
    Get-Item -Path (Join-Path $Root 'cluster-widgets\Themes\Default\index.html')
)
foreach ($file in $clusterHtml) {
    $text = [System.IO.File]::ReadAllText($file.FullName, [System.Text.Encoding]::UTF8)
    $r = Apply-ClusterReplacements -text $text -entries $map.cluster
    $r2 = Apply-PatternReplacements -text $r.text -tokens (Get-MaintenanceTemplates -source $false)
    $r.count += $r2.count

    if ($r.count -gt 0) {
        Write-Utf8NoBom $file.FullName $r2.text
        $relative = Get-RelativePath $Root $file.FullName
        Write-Host ("{0}: {1} cluster replacements" -f $relative, $r.count)
        $totalReplaced += $r.count
    }
}

Write-Host ("Total replacements: {0}" -f $totalReplaced)
