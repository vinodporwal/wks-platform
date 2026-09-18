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

export function parseDateSafe(val) {
  if (val == null || val === '') return null
  if (val instanceof Date && !isNaN(val.getTime())) {
    return new Date(val.getFullYear(), val.getMonth(), val.getDate())
  }
  const str = String(val).trim()
  // DD/MM/YYYY or DD-MM-YYYY
  const ddmmyyyy = str.match(/^(\d{1,2})[-/](\d{1,2})[-/](\d{4})$/)
  if (ddmmyyyy) {
    const day = parseInt(ddmmyyyy[1], 10)
    const month = parseInt(ddmmyyyy[2], 10) - 1
    const year = parseInt(ddmmyyyy[3], 10)
    const d = new Date(year, month, day)
    if (!isNaN(d.getTime())) return d
  }
  // YYYY-MM-DD or YYYY/MM/DD
  const yyyymmdd = str.match(/^(\d{4})[-/](\d{1,2})[-/](\d{1,2})/)
  if (yyyymmdd) {
    const year = parseInt(yyyymmdd[1], 10)
    const month = parseInt(yyyymmdd[2], 10) - 1
    const day = parseInt(yyyymmdd[3], 10)
    const d = new Date(year, month, day)
    if (!isNaN(d.getTime())) return d
  }
  const parsed = new Date(str)
  if (!isNaN(parsed.getTime())) {
    return new Date(parsed.getFullYear(), parsed.getMonth(), parsed.getDate())
  }
  return null
}

export function isDateColumn(colDef, sampleRows = []) {
  if (colDef?.type === 'date') return true
  const fieldName = String(colDef?.field || '').toLowerCase()
  const titleName = String(colDef?.title || colDef?.headerName || '').toLowerCase()
  if (
    fieldName.includes('date') ||
    fieldName.includes('timestamp') ||
    titleName.includes('date')
  ) {
    return true
  }
  for (let i = 0; i < Math.min(sampleRows.length, 5); i++) {
    const v = sampleRows[i]?.[colDef?.field]
    if (v != null && v !== '' && parseDateSafe(v) != null && isValidDateString(String(v))) {
      return true
    }
  }
  return false
}

export function inferColumnsFromRows(rows = []) {
  const fieldSet = new Set()
  rows.forEach((r) => {
    if (!r || typeof r !== 'object') return
    Object.keys(r).forEach((k) => fieldSet.add(k))
  })

  return Array.from(fieldSet).map((f) => {
    const fieldLower = f.toLowerCase()
    let detectedType = 'string'

    if (fieldLower.includes('date') || fieldLower.includes('timestamp')) {
      detectedType = 'date'
    } else {
      for (const r of rows) {
        if (!r) continue
        const v = r?.[f]
        if (v === undefined || v === null || v === '') continue
        if (typeof v === 'number') {
          detectedType = 'number'
          break
        }
        if (parseDateSafe(v) != null && isValidDateString(String(v))) {
          detectedType = 'date'
          break
        }
        const numericCandidate = String(v).replace(/[,]/g, '')
        if (!isNaN(Number(numericCandidate))) {
          detectedType = 'number'
          break
        }
      }
    }
    return { field: f, title: f, type: detectedType }
  })
}
