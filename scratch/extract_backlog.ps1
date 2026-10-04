$tempDir = Join-Path $env:TEMP "backlog_unzip"
if (Test-Path $tempDir) { Remove-Item -Recurse -Force $tempDir }
New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
$zipPath = Join-Path $tempDir "backlog.zip"
Copy-Item -Path "Product Backlog.xlsx" -Destination $zipPath
Expand-Archive -Path $zipPath -DestinationPath $tempDir

# Read shared strings
$sharedStrings = @()
if (Test-Path "$tempDir\xl\sharedStrings.xml") {
    $stringsXml = [xml](Get-Content "$tempDir\xl\sharedStrings.xml" -Raw -Encoding UTF8)
    foreach ($si in $stringsXml.sst.si) {
        if ($si.t) {
            $sharedStrings += $si.t
        } elseif ($si.r) {
            $sharedStrings += (($si.r | ForEach-Object { $_.t }) -join "")
        } else {
            $sharedStrings += ""
        }
    }
}

# Read workbook.xml to get sheet names
$workbookXml = [xml](Get-Content "$tempDir\xl\workbook.xml" -Raw -Encoding UTF8)
$sheets = @{}
foreach ($sheet in $workbookXml.workbook.sheets.sheet) {
    Write-Output "Sheet: $($sheet.name) (ID: $($sheet.sheetId), r:id: $($sheet.id))"
}

# Search all worksheets
Get-ChildItem "$tempDir\xl\worksheets\*.xml" | ForEach-Object {
    $sheetXml = [xml](Get-Content $_.FullName -Raw -Encoding UTF8)
    $sheetFile = $_.Name
    foreach ($row in $sheetXml.worksheet.sheetData.row) {
        $rowTexts = @()
        foreach ($c in $row.c) {
            $val = ""
            if ($c.t -eq "s") {
                $idx = [int]$c.v
                if ($idx -lt $sharedStrings.Count) {
                    $val = $sharedStrings[$idx]
                }
            } else {
                $val = $c.v
            }
            if ($val) { $rowTexts += $val }
        }
        $line = $rowTexts -join " | "
        if ($line -match "S2-09|lead|Lead|LEAD|Tuyển sinh|tuyển sinh") {
            Write-Output "[$sheetFile] $line"
        }
    }
}
