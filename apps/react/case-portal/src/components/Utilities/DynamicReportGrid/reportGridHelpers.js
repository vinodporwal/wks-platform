export const MONTH_MAP = {
  apr: 4,
  april: 4,
  may: 5,
  jun: 6,
  june: 6,
  jul: 7,
  july: 7,
  aug: 8,
  august: 8,
  sep: 9,
  sept: 9,
  september: 9,
  oct: 10,
  october: 10,
  nov: 11,
  november: 11,
  dec: 12,
  december: 12,
  jan: 1,
  january: 1,
  feb: 2,
  february: 2,
  mar: 3,
  march: 3,
}

export function isValidDateString(str) {
  if (typeof str !== 'string') return false
  const datePatterns = [
    /^\d{1,2}[-/]\d{1,2}[-/]\d{4}$/,
    /^\d{4}[-/]\d{1,2}[-/]\d{1,2}$/,
    /^[A-Za-z]{3}\s+\d{1,2},\s+\d{4}/,
    /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/,
  ]
  return datePatterns.some((pattern) => pattern.test(str.trim()))
}

export function inferColumnsFromRows(rows = []) {
  const fieldSet = new Set()
  rows.forEach((r) => {
    if (!r || typeof r !== 'object') return
    Object.keys(r).forEach((k) => fieldSet.add(k))
  })

  return Array.from(fieldSet).map((f) => {
    let detectedType = 'string'
    for (const r of rows) {
      if (!r) continue
      const v = r?.[f]
      if (v === undefined || v === null || v === '') continue
      if (typeof v === 'number') {
        detectedType = 'number'
        break
      }
      const d = new Date(v)
      if (!isNaN(d.getTime()) && isValidDateString(v)) {
        detectedType = 'date'
        break
      }
      const numericCandidate = String(v).replace(/[,]/g, '')
      if (!isNaN(Number(numericCandidate))) {
        detectedType = 'number'
        break
      }
    }
    return { field: f, title: f, type: detectedType }
  })
}
