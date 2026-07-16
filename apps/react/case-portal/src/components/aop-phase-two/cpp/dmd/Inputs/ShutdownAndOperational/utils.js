const monthMap = {
  apr: 'april',
  may: 'may',
  jun: 'june',
  jul: 'july',
  aug: 'aug',
  sep: 'sep',
  oct: 'oct',
  nov: 'nov',
  dec: 'dec',
  jan: 'jan',
  feb: 'feb',
  mar: 'march',
}

export const transformApiResponseToGridFormat = (apiData, hoursRows = []) => {
  return apiData.map((row) => {
    const transformedRow = {
      ...row,
      utilityDistributed: {
        name: row.utilityDistributed,
        sapCode: row.distributedSapCode,
      },
      utilityGenerated: {
        name: row.utilityGenerated,
        sapCode: row.generatedUtilityCode,
      },
    }

    Object.entries(monthMap).forEach(([apiKey, gridKey]) => {
      const operationalHrs = row[apiKey] || 0
      const totalHours = hoursRows[0]?.[apiKey] || 0
      const shutdownHrs = Math.max(0, totalHours - operationalHrs)

      transformedRow[gridKey] = {
        shutdownHrs,
        netOperationHrs: operationalHrs,
      }
    })

    return transformedRow
  })
}

export const transformGridFormatToApiFormat = (gridData) => {
  return gridData.map((row) => ({
    id: row.id,
    assetFkId: row.assetFkId,
    utilityDistributed: row.utilityDistributed?.name || row.utilityDistributed,
    distributedSapCode:
      row.utilityDistributed?.sapCode || row.distributedSapCode,
    utilityGenerated: row.utilityGenerated?.name || row.utilityGenerated,
    generatedUtilityCode:
      row.utilityGenerated?.sapCode || row.generatedUtilityCode,
    apr: row.april?.netOperationHrs || row.apr,
    may: row.may?.netOperationHrs || row.may,
    jun: row.june?.netOperationHrs || row.jun,
    jul: row.july?.netOperationHrs || row.jul,
    aug: row.aug?.netOperationHrs || row.aug,
    sep: row.sep?.netOperationHrs || row.sep,
    oct: row.oct?.netOperationHrs || row.oct,
    nov: row.nov?.netOperationHrs || row.nov,
    dec: row.dec?.netOperationHrs || row.dec,
    jan: row.jan?.netOperationHrs || row.jan,
    feb: row.feb?.netOperationHrs || row.feb,
    mar: row.march?.netOperationHrs || row.mar,
    aopYear: row.aopYear,
    remarks: row.remarks,
    siteFkId: row.siteFkId,
    verticalFkId: row.verticalFkId,
    plantFkId: row.plantFkId,
    assetName: row.assetName,
    plantName: row.plantName,
    assetType: row.assetType,
  }))
}
