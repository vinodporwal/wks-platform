import { DataService } from 'services/DataService'

/**
 * Format date to YYYY-MM-DD format
 */
export const formatDate = (date) => {
  if (!date) return ''
  const year = date?.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/**
 * Format date for display text (DD-MM-YYYY with optional time)
 */
export const formatDateForText = (date, time = false) => {
  if (!date) return ''
  const parsedDate = new Date(date)
  if (isNaN(parsedDate)) return 'Invalid Date'
  const day = String(parsedDate.getDate()).padStart(2, '0')
  const month = String(parsedDate.getMonth() + 1).padStart(2, '0')
  const year = parsedDate.getFullYear()
  let formatted = `${day}-${month}-${year}`
  if (time) {
    let hours = parsedDate.getHours()
    const minutes = String(parsedDate.getMinutes()).padStart(2, '0')
    const ampm = hours >= 12 ? 'PM' : 'AM'
    hours = hours % 12
    hours = hours ? hours : 12
    const formattedTime = `${String(hours).padStart(2, '0')}:${minutes} ${ampm}`
    formatted += ` ${formattedTime}`
  }
  return formatted
}

/**
 * Validate date range
 */
export const validateDateRange = (startDate, endDate) => {
  if (!startDate || !endDate) {
    return { valid: false, message: 'Please select both start and end dates.' }
  }

  const s = new Date(startDate)
  const e = new Date(endDate)

  s.setHours(0, 0, 0, 0)
  e.setHours(0, 0, 0, 0)

  if (s > e) {
    return {
      valid: false,
      message:
        'Please choose valid dates (start date must be before end date).',
    }
  }

  return { valid: true }
}
