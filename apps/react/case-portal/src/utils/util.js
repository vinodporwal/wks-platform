export const buildCreateUrl = (url, caseDefId = 'create') => {
  const cleanedUrl = cleanUrl(url);
  let newUrl;
  if (cleanedUrl.includes('case-list')) {
    newUrl = cleanedUrl;
  } else {
    if(caseDefId === 'create'){
    newUrl = cleanedUrl.replace(
    'create?assetName',
    'case-list/create?assetName',
    );
    } else if (caseDefId === 'cms'){
      newUrl = cleanedUrl.replace(
        '/cms',
        '/case-list/cms',
        );
    }

  }

  return newUrl
}

// export const cleanUrl = (url) => {
//   return url.includes('?')
//     ? url.split('?')[0] +
//         '?' +
//         new URLSearchParams(url.split('?')[1])
//           .toString()
//           .replace(/(&?caseNo=[^&]*)/, '')
//           .replace(/^&/, '')
//     : url
// }

export const cleanUrl = (url) => {
  if (!url.includes('?')) return url;
 
  const [base, query] = url.split('?');
  const params = new URLSearchParams(query);
 
  // Remove the caseNo param
  params.delete('caseNo');
 
  const cleanedQuery = params.toString();
 
  return cleanedQuery ? `${base}?${cleanedQuery}` : base;
};

export const getQueryParamValue = (url, paramName) => {
  const value = url.includes('?')
    ? new URLSearchParams(url.split('?')[1]).get(paramName)
    : null
  return value
}
