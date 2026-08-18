param()

$ErrorActionPreference = 'Stop'

$oldPath = Join-Path $PSScriptRoot '..\_diag\localize-zh-old.ps1'
$lines = Get-Content -LiteralPath $oldPath

$exact = [System.Collections.Generic.List[object]]::new()
$substring = [System.Collections.Generic.List[object]]::new()
$inSubstring = $false

foreach ($line in $lines) {
    if ($line -match '^\$substringMap = @') {
        $inSubstring = $true
        continue
    }

    $m = [regex]::Match($line, "^    @\('(?<src>.*)', '(?<dst>.*)'\)\s*$")
    if ($m.Success) {
        $src = $m.Groups['src'].Value
        $dst = $m.Groups['dst'].Value
        $obj = [pscustomobject]@{
            source      = $src
            translation = $dst
        }
        if ($inSubstring) {
            [void]$substring.Add($obj)
        }
        elseif ($src -notin @('AC', 'AUTO', 'Normal', 'Básico')) {
            [void]$exact.Add($obj)
        }
    }
}

$theme = @(
    [pscustomobject]@{ source = 'Fan'; translation = '风量' }
    [pscustomobject]@{ source = 'Temp'; translation = '温度' }
    [pscustomobject]@{ source = 'AUTO'; translation = '自动' }
    [pscustomobject]@{ source = 'MAX AUTO ON'; translation = '最大自动' }
    [pscustomobject]@{ source = 'Outside'; translation = '室外' }
    [pscustomobject]@{ source = 'Inside'; translation = '车内' }
    [pscustomobject]@{ source = 'Low'; translation = '最低' }
    [pscustomobject]@{ source = 'High'; translation = '最高' }
    [pscustomobject]@{ source = 'HI'; translation = '高' }
    [pscustomobject]@{ source = 'LO'; translation = '低' }
    [pscustomobject]@{ source = 'IMPULSE AUTO'; translation = '智能自动' }
)

$result = [ordered]@{
    exact     = $exact
    substring = $substring
    theme     = $theme
}

$jsonPath = Join-Path $PSScriptRoot 'zh-map.json'
[System.IO.File]::WriteAllText(
    $jsonPath,
    ($result | ConvertTo-Json -Depth 6),
    (New-Object System.Text.UTF8Encoding $false)
)

Write-Output ("exact={0} substring={1} theme={2}" -f $exact.Count, $substring.Count, $theme.Count)
