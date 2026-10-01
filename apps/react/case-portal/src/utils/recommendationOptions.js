import { CaseDefService } from '../services'

export const RECOMMENDATION_OPTION_CACHE_KEYS = {
  plannerGroup: 'recommendationPlannerGroupOptions',
  priority: 'recommendationPriorityOptions',
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

export const hydrateRecommendationOptions = (form) => {
  const optionsByKey = {
    recommendationPlannerGroup: readCachedOptions(
      RECOMMENDATION_OPTION_CACHE_KEYS.plannerGroup,
    ),
    recommendationPriority: readCachedOptions(
      RECOMMENDATION_OPTION_CACHE_KEYS.priority,
    ),
  }

  const hydrate = (component) => {
    const options = optionsByKey[component?.key]
    if (options?.length > 0) {
      component.data = { ...component.data, values: options }
      if (component.key === 'recommendationPriority') {
        // Hydrated API options are the runtime source of truth for Priority.
        component.calculateValue = ''
      }
    }

    const children = [
      ...(component?.components || []),
      ...(component?.columns || []),
    ]
    children.forEach(hydrate)
  }

  hydrate(form?.structure || form)
  return form
}

const GE_APM_USERS_CACHE_KEY = 'geAPMUsers'
const GE_APM_USERS_FETCHED_AT_KEY = 'geAPMUsersFetchedAt'
const GE_APM_USERS_CACHE_TTL_MS = 30 * 60 * 1000
const GE_APM_USERS_MAX_RESULTS = 100
const GE_APM_USERS_MIN_SEARCH = 2

let recommendationUsersMemoryCache = []
let recommendationUsersMemoryFetchedAt = 0
let recommendationUsersFetchPromise = null

const readRecommendationUsersCache = () => {
  try {
    const users = JSON.parse(localStorage.getItem(GE_APM_USERS_CACHE_KEY))
    const fetchedAt = Number(
      localStorage.getItem(GE_APM_USERS_FETCHED_AT_KEY),
    )
    if (
      Array.isArray(users) &&
      users.length > 0 &&
      users.every((user) => user?.label && user?.value) &&
      Number.isFinite(fetchedAt) &&
      Date.now() - fetchedAt < GE_APM_USERS_CACHE_TTL_MS
    ) {
      return { users, fetchedAt }
    }
  } catch (error) {
    console.error('Unable to read the GE APM user cache:', error)
  }
  return null
}

export const ensureRecommendationUsersLoaded = async (keycloak) => {
  if (
    recommendationUsersMemoryCache.length > 0 &&
    Date.now() - recommendationUsersMemoryFetchedAt <
      GE_APM_USERS_CACHE_TTL_MS
  ) {
    return recommendationUsersMemoryCache
  }

  const cached = readRecommendationUsersCache()
  if (cached) {
    recommendationUsersMemoryCache = cached.users
    recommendationUsersMemoryFetchedAt = cached.fetchedAt
    return cached.users
  }

  if (recommendationUsersFetchPromise) return recommendationUsersFetchPromise

  recommendationUsersFetchPromise = CaseDefService.getCaseDefinitionGEAPMUsers(
    keycloak,
  )
    .then((users) => {
      const options = users
        .filter((user) => user?.userId && user?.emailId)
        .map((user) => ({ label: user.userId, value: user.emailId }))
      if (options.length === 0) {
        throw new Error('GE APM returned no active users for the preload cache.')
      }
      const fetchedAt = Date.now()

      recommendationUsersMemoryCache = options
      recommendationUsersMemoryFetchedAt = fetchedAt
      try {
        localStorage.setItem(
          GE_APM_USERS_CACHE_KEY,
          JSON.stringify(options),
        )
        localStorage.setItem(
          GE_APM_USERS_FETCHED_AT_KEY,
          String(fetchedAt),
        )
      } catch (error) {
        console.error('Unable to persist the GE APM user cache:', error)
      }
      return options
    })
    .finally(() => {
      recommendationUsersFetchPromise = null
    })

  return recommendationUsersFetchPromise
}

const restoreRecommendationUserValue = async (instance, usersPromise) => {
  const savedValue = instance.getValue()
  if (!savedValue || instance._geUserRestoredValue === savedValue) return

  instance._geUserRestoredValue = savedValue
  const fallback = { label: savedValue, value: savedValue }
  instance.setItems([fallback])
  instance.setValue(savedValue)

  try {
    const users = await usersPromise
    if (instance.getValue() !== savedValue) return
    const matchingUser = users.find(
      (user) => user.value === savedValue || user.label === savedValue,
    )
    if (matchingUser) {
      instance.setItems([matchingUser])
      instance.setValue(savedValue)
    }
  } catch {
    instance._geUserRestoredValue = null
  }
}

export const bindRecommendationUserSearch = (formInstance, keycloak) => {
  if (!formInstance?.everyComponent) return

  const usersPromise = ensureRecommendationUsersLoaded(keycloak)
  void usersPromise.catch((error) => {
    console.error('Unable to preload GE APM users:', error)
  })
  const bindCurrentRecommendationUserInputs = () => {
    formInstance.everyComponent((instance) => {
      if (
        instance?.component?.key !== 'recommendationAssignedTo2' &&
        instance?.component?.key !== 'recommendationReviewer'
      ) {
        return
      }

      void restoreRecommendationUserValue(instance, usersPromise)
      const searchInput = instance.choices?.input?.element
      if (!searchInput || instance._geUserSearchInput === searchInput) return

      clearTimeout(instance._geUserSearchTimer)
      instance._geUserSearchSequence =
        (instance._geUserSearchSequence || 0) + 1
      if (instance._geUserSearchInput && instance._geUserSearchHandler) {
        instance.removeEventListener(
          instance._geUserSearchInput,
          'input',
          instance._geUserSearchHandler,
        )
      }

      const searchHandler = (event) => {
        const searchText = event.target.value.trim()
        const searchSequence = ++instance._geUserSearchSequence
        clearTimeout(instance._geUserSearchTimer)

        if (searchText.length < GE_APM_USERS_MIN_SEARCH) {
          const selectedValue = instance.getValue()
          const selectedUser = recommendationUsersMemoryCache.find(
            (user) =>
              user.value === selectedValue || user.label === selectedValue,
          )
          instance.setItems(
            selectedValue
              ? [
                  selectedUser || {
                    label: selectedValue,
                    value: selectedValue,
                  },
                ]
              : [],
          )
          return
        }

        instance._geUserSearchTimer = setTimeout(async () => {
          try {
            const users = await usersPromise
            if (
              searchSequence !== instance._geUserSearchSequence ||
              searchInput.value.trim() !== searchText
            ) {
              return
            }
            const term = searchText.toLowerCase()
            const matches = users
              .filter(
                (user) =>
                  user.label?.toLowerCase().startsWith(term) ||
                  user.value?.toLowerCase().startsWith(term),
              )
              .slice(0, GE_APM_USERS_MAX_RESULTS)
            instance.setItems(matches)
          } catch (error) {
            instance.setItems([])
          }
        }, 300)
      }

      instance._geUserSearchInput = searchInput
      instance._geUserSearchHandler = searchHandler
      instance.addEventListener(searchInput, 'input', searchHandler)
    })
  }

  bindCurrentRecommendationUserInputs()

  const dataGrid = formInstance.getComponent('dataGrid1')
  const observerTarget = dataGrid?.element?.parentNode
  if (!observerTarget) return

  if (formInstance._geUserDataGridObserverTarget !== observerTarget) {
    formInstance._geUserDataGridObserver?.disconnect()
    formInstance._geUserDataGridObserverTarget = observerTarget
    formInstance._geUserDataGridObserver = new MutationObserver(() => {
      bindCurrentRecommendationUserInputs()
    })
    formInstance._geUserDataGridObserver.observe(observerTarget, {
      childList: true,
      subtree: true,
    })
  }
}

export const hydrateRecommendationUserSearch = (form) => {
  const hydrate = (component) => {
    if (
      component?.key === 'recommendationAssignedTo2' ||
      component?.key === 'recommendationReviewer'
    ) {
      component.dataSrc = 'values'
      component.data = {
        values: [],
        resource: '',
        url: '',
        json: '',
        custom: '',
      }
      component.valueProperty = 'value'
      component.template = '<span>{{ item.label }}</span>'
      component.searchEnabled = true
      component.searchField = ''
      component.lazyLoad = false
      component.minSearch = 2
      component.limit = 100
      component.searchDebounce = 0.3
      component.disableLimit = false
      component.calculateValue = ''
    }

    const children = [
      ...(component?.components || []),
      ...(component?.columns || []),
    ]
    children.forEach(hydrate)
  }

  hydrate(form?.structure || form)
  return form
}

export const resolveRecommendationOptionLabel = (cacheKey, storedValue) => {
  const matchingOption = readCachedOptions(cacheKey).find(
    (option) => String(option.value) === String(storedValue),
  )
  return matchingOption ? matchingOption.label : storedValue
}
