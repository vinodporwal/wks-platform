// utils/dateUtils.js
export const formatDate = (date) => {
  if (!date) return ''

  if (typeof date === 'string') {
    const trimmed = date.trim()
    if (!trimmed) return ''
    if (/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) {
      return trimmed
    }
    const isoMatch = trimmed.match(/^(\d{4}-\d{2}-\d{2})/)
    if (isoMatch) {
      return isoMatch[1]
    }
    const dmyMatch = trimmed.match(/^(\d{2})[-/](\d{2})[-/](\d{4})$/)
    if (dmyMatch) {
      return `${dmyMatch[3]}-${dmyMatch[2]}-${dmyMatch[1]}`
    }
  }

  const parsedDate = date instanceof Date ? date : new Date(date)
  if (isNaN(parsedDate.getTime())) return ''

  const year = parsedDate.getFullYear()
  const month = String(parsedDate.getMonth() + 1).padStart(2, '0')
  const day = String(parsedDate.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export const formatDateForText = (date, includeTime = false) => {
  if (!date) return ''

  let parsedDate
  if (typeof date === 'string') {
    const trimmed = date.trim()
    if (!trimmed) return ''
    const dmyMatch = trimmed.match(/^(\d{2})[-/](\d{2})[-/](\d{4})$/)
    if (dmyMatch) {
      return `${dmyMatch[1]}-${dmyMatch[2]}-${dmyMatch[3]}`
    }
    const ymdMatch = trimmed.match(/^(\d{4})-(\d{2})-(\d{2})$/)
    if (ymdMatch) {
      return `${ymdMatch[3]}-${ymdMatch[2]}-${ymdMatch[1]}`
    }
    parsedDate = new Date(trimmed)
  } else if (date instanceof Date) {
    parsedDate = date
  } else {
    parsedDate = new Date(date)
  }

  if (isNaN(parsedDate.getTime())) return 'Invalid Date'

  const day = String(parsedDate.getDate()).padStart(2, '0')
  const month = String(parsedDate.getMonth() + 1).padStart(2, '0')
  const year = parsedDate.getFullYear()
  let formatted = `${day}-${month}-${year}`
  if (includeTime) {
    let hours = parsedDate.getHours()
    const minutes = String(parsedDate.getMinutes()).padStart(2, '0')
    const ampm = hours >= 12 ? 'PM' : 'AM'
    hours = hours % 12 || 12
    formatted += ` ${String(hours).padStart(2, '0')}:${minutes} ${ampm}`
  }
  return formatted
}
