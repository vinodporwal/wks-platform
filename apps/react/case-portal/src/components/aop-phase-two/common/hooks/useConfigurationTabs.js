import { useState, useEffect, useCallback, useMemo, useRef } from 'react'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import TabAccessApiService from 'components/aop-phase-two/services/common/tabAccessApiService'

/**
 * Custom hook to fetch and manage configuration tabs (visible + available)
 * from the AOP Configuration Access Matrix.
 *
 * @param {string} [type] - Optional type filter (e.g. 'TCS', 'OutputReport').
 *                          When omitted, all types are considered (Type IS NULL in DB).
 * @param {Object} [options] - Optional overrides for plant/site/vertical/year.
 * @param {string} [options.plantId] - Override plantId (defaults to selected plant)
 * @param {string} [options.siteId] - Override siteId (defaults to selected site)
 * @param {string} [options.verticalId] - Override verticalId (defaults to selected vertical)
 * @param {string} [options.year] - Override AOP year (defaults to selected year)
 * @param {boolean} [options.sortBySequence] - When true, sorts filteredTabs by
 *                          displaySequence. Defaults to false (preserves JSON array order
 *                          from ConfigurationTabs, which allows different ordering per
 *                          access matrix row when tabs are shared across types).
 * @returns {Object} - { tabs, filteredTabs, availableTabs, tabIndex, setTabIndex,
 *                       currentTab, loading, error, refresh }
 */
const useConfigurationTabs = (type, options = {}) => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, siteObject, verticalObject, year } = dataGridStore

  const PLANT_ID = options.plantId ?? plantObject?.id
  const SITE_ID = options.siteId ?? siteObject?.id
  const VERTICAL_ID = options.verticalId ?? verticalObject?.id
  const AOP_YEAR = options.year ?? year?.selectedYear
  const sortBySequence = options.sortBySequence ?? false

  const [tabs, setTabs] = useState([])
  const [availableTabs, setAvailableTabs] = useState([])
  const [matrixLoading, setMatrixLoading] = useState(false)
  const [tabsLoading, setTabsLoading] = useState(false)
  const [error, setError] = useState(null)
  const [refreshKey, setRefreshKey] = useState(0)
  const [tabIndex, setTabIndex] = useState(0)

  // Guard against stale responses when plant/site/vertical/year changes
  // before a previous in-flight matrix call has resolved.
  const requestIdRef = useRef(0)

  const loading = matrixLoading || tabsLoading

  const refresh = useCallback(() => setRefreshKey((k) => k + 1), [])

  // Fetch visible tab IDs for the given plant/site/vertical/type
  const getConfigurationTabsMatrix = useCallback(async () => {
    if (!PLANT_ID || !AOP_YEAR || !SITE_ID || !VERTICAL_ID) return
    const requestId = ++requestIdRef.current
    setMatrixLoading(true)
    setError(null)
    try {
      const response = await TabAccessApiService.getConfigurationTabsMatrix(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        SITE_ID,
        VERTICAL_ID,
        type,
      )
      // Ignore stale responses from a previous in-flight call
      if (requestId !== requestIdRef.current) return
      if (response?.code === 200) {
        const parsedData = JSON.parse(response?.data)
        setTabs(parsedData)
      } else {
        setTabs([])
      }
    } catch (err) {
      if (requestId !== requestIdRef.current) return
      console.error('Error fetching configuration tabs matrix:', err)
      setError('Failed to fetch configuration tabs')
      setTabs([])
    } finally {
      if (requestId === requestIdRef.current) {
        setMatrixLoading(false)
      }
    }
  }, [keycloak, PLANT_ID, AOP_YEAR, SITE_ID, VERTICAL_ID, type])

  // Fetch all available tab metadata
  const getConfigurationAvailableTabs = useCallback(async () => {
    setTabsLoading(true)
    setError(null)
    try {
      const response =
        await TabAccessApiService.getConfigurationAvailableTabs(keycloak)
      if (response?.code === 200) {
        setAvailableTabs(response?.data?.configurationTypeList || [])
      } else {
        setAvailableTabs([])
      }
    } catch (err) {
      console.error('Error fetching configuration available tabs:', err)
      setError('Failed to fetch available tabs')
      setAvailableTabs([])
    } finally {
      setTabsLoading(false)
    }
  }, [keycloak])

  useEffect(() => {
    if (!PLANT_ID || !AOP_YEAR || !SITE_ID || !VERTICAL_ID) return
    getConfigurationTabsMatrix()
    getConfigurationAvailableTabs()
  }, [
    PLANT_ID,
    AOP_YEAR,
    SITE_ID,
    VERTICAL_ID,
    getConfigurationTabsMatrix,
    getConfigurationAvailableTabs,
    refreshKey,
  ])

  // Match visible tab IDs with available tab metadata to build filteredTabs.
  // By default, preserves the order from the ConfigurationTabs JSON array
  // (allows different ordering per access matrix row when tabs are shared).
  // When sortBySequence is true, sorts by displaySequence instead.
  // Memoized so the reference is stable across renders unless the underlying
  // data (tabs / availableTabs / sortBySequence) actually changes.
  const sortedFilteredTabs = useMemo(() => {
    const filtered = tabs
      .map((tabId) => {
        const tabInfo = availableTabs.find(
          (tab) => tab.id.toLowerCase() === tabId.toLowerCase(),
        )
        if (!tabInfo) return null
        return {
          id: tabId,
          name: tabInfo.displayName,
          displayName: tabInfo?.displayName,
          displaySequence: tabInfo.displaySequence,
        }
      })
      .filter(Boolean)

    return sortBySequence
      ? [...filtered].sort(
          (a, b) => (a.displaySequence || 0) - (b.displaySequence || 0),
        )
      : filtered
  }, [tabs, availableTabs, sortBySequence])

  // Select the first tab by default whenever the filtered tabs change.
  // Safe to depend on `sortedFilteredTabs` directly because it is memoized.
  useEffect(() => {
    if (sortedFilteredTabs.length > 0) {
      setTabIndex(0)
    } else {
      setTabIndex(null)
    }
  }, [sortedFilteredTabs])

  const currentTab = sortedFilteredTabs[tabIndex] || null

  return {
    tabs,
    filteredTabs: sortedFilteredTabs,
    availableTabs,
    tabIndex,
    setTabIndex,
    currentTab,
    loading,
    error,
    refresh,
  }
}

export default useConfigurationTabs
