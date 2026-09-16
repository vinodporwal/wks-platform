import { CaseDefService } from '../services'

export const RECOMMENDATION_OPTION_CACHE_KEYS = {
  priority: 'recommendationPriorityOptions',
  plannerGroup: 'recommendationPlannerGroupOptions',
}

const readCachedOptions = (cacheKey) => {
  try {
    const options = JSON.parse(localStorage.getItem(cacheKey))
    return Array.isArray(options) ? options : []
  } catch (error) {
    console.error(`Unable to read cached options for ${cacheKey}:`, error)
    return []
  }
}

const loadAndCacheOptions = async (cacheKey, request, mapOption) => {
  const cachedOptions = readCachedOptions(cacheKey)
  try {
    const items = await request()
    const options = items.map(mapOption)
    localStorage.setItem(cacheKey, JSON.stringify(options))
    return options
  } catch (error) {
    if (cachedOptions.length > 0) return cachedOptions
    throw error
  }
}

export const loadRecommendationOptions = async (keycloak) => {
  const [plannerGroups, priorities] = await Promise.all([
    loadAndCacheOptions(
      RECOMMENDATION_OPTION_CACHE_KEYS.plannerGroup,
      () => CaseDefService.getRecommendationPlannerGroups(keycloak),
      (item) => ({ label: item.displayValue, value: String(item.apiValue) }),
    ),
    loadAndCacheOptions(
      RECOMMENDATION_OPTION_CACHE_KEYS.priority,
      () => CaseDefService.getRecommendationPriorities(keycloak),
      (item) => ({ label: item.displayValue, value: String(item.apiValue) }),
    ),
  ])

  return { plannerGroups, priorities }
}

export const resolveRecommendationOptionLabel = (cacheKey, storedValue) => {
  const matchingOption = readCachedOptions(cacheKey).find(
    (option) => String(option.value) === String(storedValue),
  )
  return matchingOption ? matchingOption.label : storedValue
}
