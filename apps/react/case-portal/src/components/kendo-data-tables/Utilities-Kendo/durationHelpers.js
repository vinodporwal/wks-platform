export const recalcEndDate = (startRaw, durationStr) => {
  if (!startRaw) return null
  const start = new Date(startRaw)
  if (!(start instanceof Date) || isNaN(start)) return null
  // parse "HH.MM"
  const [hrsPart, minPart = '0'] = String(durationStr).split('.')
  const hrs = parseInt(hrsPart, 10)
  const mins = parseInt(minPart.padEnd(2, '0').slice(0, 2), 10)
  if (isNaN(hrs) || isNaN(mins) || mins < 0 || mins > 59) return null
  const end = new Date(start.getTime() + (hrs * 60 + mins) * 60000)
  return end
}

export const recalcDuration = (startRaw, endRaw) => {
  const start = startRaw ? new Date(startRaw) : null
  const end = endRaw ? new Date(endRaw) : null
  if (
    start instanceof Date &&
    !isNaN(start) &&
    end instanceof Date &&
    !isNaN(end)
  ) {
    const diffMs = end.getTime() - start.getTime()
    if (diffMs < 0) return ''
    const totalMins = Math.floor(diffMs / 60000)
    const hrs = Math.floor(totalMins / 60)
    const mins = totalMins % 60
    // format as "H.MM" with two-digit minutes
    return `${hrs}.${mins.toString().padStart(2, '0')}`
  }
  return ''
}

export const calculateMonthDuration = (monthName, fiscalYear) => {
  if (!monthName || !fiscalYear) return ''

  const monthIndexMap = {
    january: 0,
    february: 1,
    march: 2,
    april: 3,
    may: 4,
    june: 5,
    july: 6,
    august: 7,
    september: 8,
    october: 9,
    november: 10,
    december: 11,
  }
  const m = monthIndexMap[(monthName || '').toLowerCase()]
  if (m === undefined) return ''

  let startYear = fiscalYear
  if (typeof fiscalYear === 'string' && fiscalYear.includes('-')) {
    startYear = parseInt(fiscalYear.split('-')[0], 10)
  }
  startYear = Number(startYear)
  if (isNaN(startYear)) return ''

  const yearForMonth = m >= 3 ? startYear : startYear + 1

  const start = new Date(yearForMonth, m, 1, 0, 0, 0, 0)
  const end = new Date(yearForMonth, m + 1, 0, 23, 59, 59, 999)

  const diffMs = end.getTime() - start.getTime() + 1
  const totalMins = Math.floor(diffMs / 60000)
  const hrs = Math.floor(totalMins / 60)
  const mins = totalMins % 60
  return `${hrs}.${mins.toString().padStart(2, '0')}`
}

export const getMonthStartEndDate = (monthName, fiscalYear) => {
  if (!monthName || !fiscalYear) return [null, null]

  const monthIndexMap = {
    january: 0,
    february: 1,
    march: 2,
    april: 3,
    may: 4,
    june: 5,
    july: 6,
    august: 7,
    september: 8,
    october: 9,
    november: 10,
    december: 11,
  }
  const m = monthIndexMap[(monthName || '').toLowerCase()]
  if (m === undefined) return [null, null]

  let startYear = fiscalYear
  if (typeof fiscalYear === 'string' && fiscalYear.includes('-')) {
    startYear = parseInt(fiscalYear.split('-')[0], 10)
  }
  startYear = Number(startYear)
  if (isNaN(startYear)) return [null, null]

  const yearForMonth = m >= 3 ? startYear : startYear + 1

  const start = new Date(yearForMonth, m, 1, 0, 0, 0, 0)
  const end = new Date(yearForMonth, m + 1, 0, 23, 59, 59, 999)

  return [start, end]
}
