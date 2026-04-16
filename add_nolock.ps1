# Script to add WITH(NOLOCK) to all SELECT statements in SQL stored procedures
# This script carefully adds NOLOCK hints to table references in FROM and JOIN clauses

$inputFile = "c:\Users\shrik\Desktop\Project\fork repo\development\New\wks-platform\script_clean.sql"
$outputFile = "c:\Users\shrik\Desktop\Project\fork repo\development\New\wks-platform\script_with_nolock.sql"

# Read the entire file
$content = Get-Content $inputFile -Raw

# Pattern to match table references in FROM and JOIN clauses that don't already have WITH(NOLOCK)
# This will match: FROM tablename, JOIN tablename, but not if already followed by WITH(NOLOCK)

# Replace FROM dbo.TableName (without WITH(NOLOCK))
$content = $content -replace '(?i)FROM\s+dbo\.([a-zA-Z0-9_]+)(?!\s+WITH\s*\(NOLOCK\))(?=\s|$|\r|\n|,)', 'FROM dbo.$1 WITH(NOLOCK)'

# Replace FROM TableName (without dbo prefix and without WITH(NOLOCK))
$content = $content -replace '(?i)FROM\s+([a-zA-Z0-9_]+)(?!\s+WITH\s*\(NOLOCK\))(?=\s+WHERE|\s+INNER|\s+LEFT|\s+RIGHT|\s+CROSS|\s+ORDER|\s+GROUP|\s*\)|;|\r|\n)', 'FROM $1 WITH(NOLOCK)'

# Replace JOIN dbo.TableName
$content = $content -replace '(?i)(INNER\s+JOIN|LEFT\s+JOIN|RIGHT\s+JOIN|CROSS\s+APPLY|OUTER\s+APPLY)\s+dbo\.([a-zA-Z0-9_]+)(?!\s+WITH\s*\(NOLOCK\))(?=\s|$|\r|\n)', '$1 dbo.$2 WITH(NOLOCK)'

# Replace JOIN TableName (without dbo prefix)
$content = $content -replace '(?i)(INNER\s+JOIN|LEFT\s+JOIN|RIGHT\s+JOIN|JOIN)\s+([a-zA-Z0-9_]+)(?!\s+WITH\s*\(NOLOCK\))(?=\s+ON|\s+WHERE|\s*\()', '$1 $2 WITH(NOLOCK)'

# Save to output file
$content | Out-File $outputFile -Encoding UTF8

Write-Host "Processing complete!"
Write-Host "Input file: $inputFile"
Write-Host "Output file: $outputFile"
Write-Host ""
Write-Host "Please review the output file to ensure all changes are correct."
