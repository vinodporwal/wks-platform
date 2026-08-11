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

// ─── Field-name constants shared by calcEndDateFromDuration & calcDurationFromDates ───
export const DURATION_FIELDS = ['Duration', 'duration', 'may']
export const START_DATE_FIELDS = ['startDate', 'StartDate', 'apr']
export const END_DATE_FIELDS = ['endDate', 'EndDate']

/**
 * Calculates End Date when Start Date or Duration changes.
 *
 * @param {object}   params
 * @param {string}   params.field               - The field that just changed.
 * @param {*}        params.value               - The new value of that field.
 * @param {object}   params.currentRow          - The current row data.
 * @param {object}   params.dataItem            - The Kendo dataItem (UOM fallback).
 * @param {function} params.parseDateFn         - parseDateRobust injected to avoid circular deps.
 * @param {function} [params.onMissingStartDate] - Called when Start Date is absent and Duration changed.
 * @returns {{ calculatedEndDate: string|null, shouldCalculateEndDate: boolean }}
 */
export const calcEndDateFromDuration = ({
  field,
  value,
  currentRow,
  dataItem,
  parseDateFn,
  onMissingStartDate,
}) => {
  const shouldCalculateEndDate =
    DURATION_FIELDS.includes(field) || START_DATE_FIELDS.includes(field)

  if (!shouldCalculateEndDate) {
    return { calculatedEndDate: null, shouldCalculateEndDate: false }
  }

  const rawDuration = DURATION_FIELDS.includes(field)
    ? value
    : currentRow?.Duration ??
      currentRow?.duration ??
      currentRow?.ConstantValue ??
      currentRow?.constantValue ??
      currentRow?.may

  const rawStartDate = START_DATE_FIELDS.includes(field)
    ? value
    : currentRow?.startDate ?? currentRow?.StartDate ?? currentRow?.apr

  const isDurationValid =
    rawDuration !== null &&
    rawDuration !== undefined &&
    String(rawDuration).trim() !== '' &&
    !isNaN(Number(rawDuration))

  if (!isDurationValid) {
    return { calculatedEndDate: null, shouldCalculateEndDate: true }
  }

  const durationVal = Number(rawDuration)
  const startDateObj = parseDateFn(rawStartDate)

  if (!startDateObj || isNaN(startDateObj.getTime())) {
    if (DURATION_FIELDS.includes(field) && typeof onMissingStartDate === 'function') {
      onMissingStartDate()
    }
    return { calculatedEndDate: null, shouldCalculateEndDate: true }
  }

  const uom = (currentRow?.UOM || dataItem?.UOM || '').trim().toLowerCase()
  const isMonthUom = uom === 'month' || uom === 'months'

  let endDateObj
  if (isMonthUom) {
    endDateObj = new Date(startDateObj)
    endDateObj.setMonth(endDateObj.getMonth() + durationVal)
  } else {
    endDateObj = new Date(
      startDateObj.getTime() + durationVal * 24 * 60 * 60 * 1000,
    )
  }

  const dd = String(endDateObj.getDate()).padStart(2, '0')
  const mm = String(endDateObj.getMonth() + 1).padStart(2, '0')
  const yyyy = endDateObj.getFullYear()

  return { calculatedEndDate: `${dd}-${mm}-${yyyy}`, shouldCalculateEndDate: true }
}

/**
 * Calculates Duration when End Date changes.
 *
 * @param {object}   params
 * @param {string}   params.field       - The field that just changed.
 * @param {*}        params.value       - The new value of that field.
 * @param {object}   params.currentRow  - The current row data.
 * @param {object}   params.dataItem    - The Kendo dataItem (UOM fallback).
 * @param {function} params.parseDateFn - parseDateRobust injected to avoid circular deps.
 * @returns {string|null} Duration string, or null when inputs are invalid.
 */
export const calcDurationFromDates = ({
  field,
  value,
  currentRow,
  dataItem,
  parseDateFn,
}) => {
  if (!END_DATE_FIELDS.includes(field)) return null

  const rawEndDate = value
  const rawStartDate =
    currentRow?.startDate ?? currentRow?.StartDate ?? currentRow?.apr

  const startDateObj = parseDateFn(rawStartDate)
  const endDateObj = parseDateFn(rawEndDate)

  if (
    !startDateObj || isNaN(startDateObj.getTime()) ||
    !endDateObj   || isNaN(endDateObj.getTime())
  ) {
    return null
  }

  const uom = (currentRow?.UOM || dataItem?.UOM || '').trim().toLowerCase()
  const isMonthUom = uom === 'month' || uom === 'months'

  if (isMonthUom) {
    const months =
      (endDateObj.getFullYear() - startDateObj.getFullYear()) * 12 +
      (endDateObj.getMonth() - startDateObj.getMonth())
    return months >= 0 ? String(months) : '0'
  }

  const diffMs = endDateObj.getTime() - startDateObj.getTime()
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))
  return diffDays >= 0 ? String(diffDays) : '0'
}
