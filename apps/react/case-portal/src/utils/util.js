export const buildCreateUrl = (url) => {
  const cleanedUrl = cleanUrl(url);
  let newUrl;
  if (cleanedUrl.includes('case-list')) {
    if(cleanedUrl.includes('?'))  
    newUrl = cleanedUrl;
  else newUrl = cleanedUrl + '?'

  } else {
    newUrl = cleanedUrl.replace(
    'create?assetName',
    'case-list/create?assetName',
    );
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
