import Config from '../consts'

const CACHE_KEY = 'functionalLocationOptions'

const readCachedOptions = () => {
  try {
    const options = JSON.parse(localStorage.getItem(CACHE_KEY) || '[]')
    return Array.isArray(options) ? options : []
  } catch (error) {
    return []
  }
}

const authorizedGet = async (keycloak, url) => {
  const response = await fetch(url, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${keycloak.token}`,
    },
  })
  if (!response.ok) throw new Error(`Request failed with status: ${response.status}`)
  return response.json()
}

const resolveAssetName = async (keycloak, assetName) => {
  if (assetName) return assetName

  const eventIds = new URLSearchParams(window.location.search).get('eventIds')
  if (!eventIds) return ''

  for (const eventId of eventIds.split(',')) {
    const history = await authorizedGet(
      keycloak,
      `${Config.CaseEngineUrl}/case-definition/fault-history/eventIds?eventIds=${encodeURIComponent(eventId)}`,
    )
    const mainAsset = history.find((item) => item.mainAsset)?.mainAsset
    if (mainAsset) return mainAsset
  }
  return ''
}

export const loadFunctionalLocationOptions = async (keycloak, assetName = '') => {
  try {
    const resolvedAssetName = await resolveAssetName(keycloak, assetName)
    if (!resolvedAssetName) return readCachedOptions()

    const locations = await authorizedGet(
      keycloak,
      `${Config.CaseEngineUrl}/case-definition/funcational-locations?assetName=${encodeURIComponent(resolvedAssetName)}`,
    )
    const options = locations.map((location) => ({
      label: location.assetFL,
      value: location.assetFL,
    }))
    localStorage.setItem(CACHE_KEY, JSON.stringify(options))
    return options
  } catch (error) {
    console.error('Unable to load Equipment Function Location options:', error)
    return readCachedOptions()
  }
}

export const hydrateFunctionalLocationOptions = (form, options, savedValues = []) => {
  const values = [...options]
  savedValues.filter(Boolean).forEach((savedValue) => {
    if (!values.some((option) => option.value === savedValue)) {
      values.push({ label: savedValue, value: savedValue })
    }
  })

  const hydrate = (component) => {
    if (component?.key === 'equipmentFunctionLocation') {
      component.data = { ...component.data, values }
      component.calculateValue = ''
    }
    const children = [
      ...(component?.components || []),
      ...(component?.columns || []),
    ]
    children.forEach(hydrate)
  }

  hydrate(form?.structure)
  return form
}
