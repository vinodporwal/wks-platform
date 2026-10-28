/**
 * utils.js
 *
 * Utility for converting Business Demand month/total values between units:
 *   TPM  – Tonnes Per Month  (raw API value — base unit)
 *   TPD  – Tonnes Per Day    (TPM ÷ days in that month)
 *   TPH  – Tonnes Per Hour   (TPM ÷ (days × 24))
 *
 * Month-to-days mapping is built dynamically from the AOP year so that
 * February correctly uses 28 or 29 days based on the actual calendar year.
 * AOP fiscal year runs April–March, so February belongs to AOP_YEAR + 1.
 */

/**
 * Returns the number of days in a given month using native JS Date.
 * new Date(year, monthNumber1Based, 0).getDate() gives the last day
 * of that month — leap year aware, no manual calculation needed.
 *
 * @param {number} year            – Full calendar year (e.g. 2025)
 * @param {number} month1Based     – Month number 1–12
 * @returns {number}
 */
const daysInCalendarMonth = (year, month1Based) =>
  new Date(year, month1Based, 0).getDate()

/**
 * Build the days-per-month map from an AOP year string like "2025-26".
 * AOP fiscal year runs April–March:
 *   April–December → startYear (e.g. 2025)
 *   January–March  → endYear   (e.g. 2026)
 *
 * @param {string|number} aopYear – e.g. "2025-26" or 2025
 * @returns {Object} month-field → number-of-days
 */
const getDaysInMonth = (aopYear) => {
  const parts = String(aopYear ?? '').split('-')
  const startYear = parseInt(parts[0], 10)
  const endYear = isNaN(startYear) ? new Date().getFullYear() : startYear + 1

  return {
    april: daysInCalendarMonth(startYear, 4),
    may: daysInCalendarMonth(startYear, 5),
    june: daysInCalendarMonth(startYear, 6),
    july: daysInCalendarMonth(startYear, 7),
    aug: daysInCalendarMonth(startYear, 8),
    sep: daysInCalendarMonth(startYear, 9),
    oct: daysInCalendarMonth(startYear, 10),
    nov: daysInCalendarMonth(startYear, 11),
    dec: daysInCalendarMonth(startYear, 12),
    jan: daysInCalendarMonth(endYear, 1),
    feb: daysInCalendarMonth(endYear, 2),
    march: daysInCalendarMonth(endYear, 3),
  }
}

const MONTH_FIELDS = [
  'april',
  'may',
  'june',
  'july',
  'aug',
  'sep',
  'oct',
  'nov',
  'dec',
  'jan',
  'feb',
  'march',
]
const HOURS_PER_DAY = 24

const ROUND_PRECISION = 5

/** Clean up IEEE-754 artifacts in a reverse-converted value */
const cleanFloat = (val) =>
  val !== null && val !== undefined
    ? parseFloat(val.toFixed(ROUND_PRECISION))
    : null

/**
 * Convert a single TPM value to the target unit for a given month field.
 *
 * @param {number|string}     tpmValue   – Raw value in TPM
 * @param {string}            monthField – e.g. 'april', 'feb', ...
 * @param {'TPM'|'TPD'|'TPH'} unit       – Target unit
 * @param {number|string}     aopYear    – AOP fiscal year (e.g. 2026)
 * @returns {number|null}
 */
export const convertValue = (tpmValue, monthField, unit, aopYear) => {
  const num = Number(tpmValue)
  if (tpmValue === null || tpmValue === undefined || isNaN(num)) return null
  if (unit === 'TPM') return num

  const daysMap = getDaysInMonth(aopYear)
  const days = daysMap[monthField] ?? 30

  // Round to ROUND_PRECISION (5) decimal places — consistent with display formatter
  if (unit === 'TPD') return days > 0 ? cleanFloat(num / days) : null
  if (unit === 'TPH') {
    const hours = days * HOURS_PER_DAY
    return hours > 0 ? cleanFloat(num / hours) : null
  }
  return num
}

/**
 * Convert all month fields + total of a single row from TPM to the target unit.
 * The `total` column is recalculated as the sum of the converted month values.
 *
 * @param {Object}            row     – Data row (must contain month fields in TPM)
 * @param {'TPM'|'TPD'|'TPH'} unit    – Target unit
 * @param {number|string}     aopYear – AOP fiscal year
 * @returns {Object} – Row with month values and total converted
 */
export const convertRow = (row, unit, aopYear) => {
  if (unit === 'TPM') return row // no conversion needed

  const converted = { ...row }
  let totalConverted = 0

  MONTH_FIELDS.forEach((field) => {
    const val = convertValue(row[field], field, unit, aopYear)
    converted[field] = val
    totalConverted += val ?? 0
  })

  converted.total = totalConverted // formatter handles display rounding
  return converted
}

/**
 * Convert all rows (including the __totals__ footer row) to the target unit.
 * Footer totals are re-derived from the converted data rows — not double-converted.
 *
 * @param {Array}             rows    – Full rows array (data rows + optional __totals__ row)
 * @param {'TPM'|'TPD'|'TPH'} unit    – Target unit
 * @param {number|string}     aopYear – AOP fiscal year (e.g. 2026)
 * @returns {Array} – Fully converted rows
 */
export const convertRows = (rows, unit, aopYear) => {
  if (!Array.isArray(rows) || rows.length === 0) return rows
  if (unit === 'TPM') return rows

  const dataRows = rows.filter((r) => r.id !== '__totals__')
  const totalsRow = rows.find((r) => r.id === '__totals__')

  const convertedDataRows = dataRows.map((r) => convertRow(r, unit, aopYear))

  // Recompute footer totals from converted data rows
  let convertedTotals = null
  if (totalsRow) {
    convertedTotals = { ...totalsRow }
    let grandTotal = 0
    MONTH_FIELDS.forEach((field) => {
      const colSum = convertedDataRows.reduce(
        (sum, r) => sum + (Number(r[field]) || 0),
        0,
      )
      convertedTotals[field] = colSum // formatter handles display rounding
      grandTotal += colSum
    })
    convertedTotals.total = grandTotal
  }

  return convertedTotals
    ? [...convertedDataRows, convertedTotals]
    : convertedDataRows
}

/** Dropdown options for the unit selector */
export const UNIT_OPTIONS = [
  { id: 'TPM', name: 'TPM' },
  { id: 'TPD', name: 'TPD' },
  { id: 'TPH', name: 'TPH' },
]

/** Default selected unit */
export const DEFAULT_UNIT = 'TPM'

// ─── Reverse conversions (displayed unit → TPM for saving) ────────────────────

/**
 * Convert a single displayed value back to TPM.
 *
 * @param {number|string}     displayValue – Value in the current display unit
 * @param {string}            monthField   – e.g. 'april', 'feb', ...
 * @param {'TPM'|'TPD'|'TPH'} unit         – Current display unit
 * @param {string|number}     aopYear      – AOP fiscal year e.g. "2025-26"
 * @returns {number|null}
 */
export const convertValueToTPM = (displayValue, monthField, unit, aopYear) => {
  const num = Number(displayValue)
  if (displayValue === null || displayValue === undefined || isNaN(num))
    return null
  if (unit === 'TPM') return num

  const daysMap = getDaysInMonth(aopYear)
  const days = daysMap[monthField] ?? 30

  // This ensures that unedited months round-trip cleanly back to their original TPM values.
  if (unit === 'TPD') return cleanFloat(num * days)
  if (unit === 'TPH') return cleanFloat(num * days * HOURS_PER_DAY)
  return num
}

/**
 * Convert all month fields of a row back to TPM.
 * Used before building the save payload.
 *
 * @param {Object}            row     – Row with values in the current display unit
 * @param {'TPM'|'TPD'|'TPH'} unit    – Current display unit
 * @param {string|number}     aopYear – AOP fiscal year
 * @returns {Object} – Row with month values in TPM
 */
export const convertRowToTPM = (row, unit, aopYear) => {
  if (unit === 'TPM') return row

  const converted = { ...row }
  MONTH_FIELDS.forEach((field) => {
    converted[field] = convertValueToTPM(row[field], field, unit, aopYear)
  })
  // Recalculate TPM total
  converted.total = MONTH_FIELDS.reduce(
    (sum, f) => sum + (Number(converted[f]) || 0),
    0,
  )
  return converted
}
