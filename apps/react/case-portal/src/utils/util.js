export const buildCreateUrl = (url, caseDefId = 'create') => {
  const cleanedUrl = cleanUrl(url);
  let newUrl;
  if (cleanedUrl.includes('case-list')) {
    newUrl = cleanedUrl;
  } else {
    if(caseDefId === 'create'){
    newUrl = cleanedUrl.replace(
    'create?',
    'case-list/create?',
    );
    } else if (caseDefId === 'picreate'){
      newUrl = cleanedUrl.replace(
        'picreate?assetName',
        'case-list/picreate?assetName',
        );
    }
  }

  return newUrl
}

export const cleanUrl = (url) => {
  return url.includes('?')
    ? url.split('?')[0] +
        '?' +
        new URLSearchParams(url.split('?')[1])
          .toString()
          .replace(/(&?caseNo=[^&]*)/, '')
          .replace(/^&/, '')
    : url
}

export const getQueryParamValue = (url, paramName) => {
  const value = url.includes('?')
    ? new URLSearchParams(url.split('?')[1]).get(paramName)
    : null
  return value
}

export const formatLocalDateTime = (value) => {
  const date = value instanceof Date ? value : new Date(value)

  if (Number.isNaN(date.getTime())) {
    throw new Error(`Invalid date value: ${value}`)
  }

  const month = date.getMonth() + 1
  const day = date.getDate()
  const year = date.getFullYear()

  const hours = date.getHours()
  const displayHour = hours % 12 || 12

  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  const meridian = hours >= 12 ? 'PM' : 'AM'

  return `${month}/${day}/${year} ${displayHour}:${minutes}:${seconds} ${meridian}`
}
