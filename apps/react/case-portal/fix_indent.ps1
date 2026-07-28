$file = 'c:\Users\om\Desktop\NEW WKS AOP K\wks-platform\apps\react\case-portal\src\components\kendo-data-tables\KendoConfigCrackerActivities.js'
$content = Get-Content $file -Raw
$bad  = "    }    return calculatedEd.toDate()"
$good = "    }" + "`r`n" + "    return calculatedEd.toDate()"
$fixed = $content.Replace($bad, $good)
Set-Content $file $fixed -NoNewline
Write-Host "Done"
