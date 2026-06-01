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
