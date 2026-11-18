import React, { useMemo } from 'react'
import { Box, Stack } from '@mui/material'
import TabSection from '../../utilities/Tabs'
import useConfigurationTabs from '../../hooks/useConfigurationTabs'
import usePlantContext from '../../hooks/usePlantContext'
import JWAvgNorms from './JWAvgNorms'

export { JWAvgNorms }

/**
 * Central registry mapping backend tab display names to shared tab components
 * under `common/components/Tabs/`. Additional production-norms-basis tabs can
 * be registered here incrementally without touching existing vertical folders.
 */
export const COMMON_TABS_REGISTRY = {
  'Job Work Avg Norms': JWAvgNorms,
}

const normalizeTabKey = (name) =>
  String(name ?? '')
    .trim()
    .toLowerCase()

/**
 * Resolves a tab component by exact or normalized name across:
 *   1. `overrideTabs` (explicit per-screen overrides)
 *   2. `COMMON_TABS_REGISTRY` (shared tabs under common/components/Tabs/)
 *   3. `localTabs` (existing vertical-specific tabs not yet moved to common)
 */
export const resolveTabComponent = (
  tabName,
  { overrideTabs = {}, localTabs = {} } = {},
) => {
  if (!tabName) return null
  const normalizedTarget = normalizeTabKey(tabName)

  const lookup = (map) => {
    if (!map) return null
    if (map[tabName]) return map[tabName]
    const matchedKey = Object.keys(map).find(
      (key) => normalizeTabKey(key) === normalizedTarget,
    )
    return matchedKey ? map[matchedKey] : null
  }

  return (
    lookup(overrideTabs) ||
    lookup(COMMON_TABS_REGISTRY) ||
    lookup(localTabs) ||
    null
  )
}

/**
 * Plug-and-play tabs wrapper for any `production-norms-basis` screen.
 * Loads visible tabs from the backend via `useConfigurationTabs`, renders
 * `TabSection`, and resolves the active tab component from `COMMON_TABS_REGISTRY`
 * with fallback to `localTabs` / `renderFallbackTab`.
 */
export const ProductionNormsTabs = ({
  tabType,
  tabOptions,
  hiddenTabs = [],
  transformTabs,
  localTabs = {},
  overrideTabs = {},
  tabProps = {},
  renderFallbackTab,
  rightAction = null,
  contentSx = { mt: 2 },
  onActiveTabChange,
}) => {
  const plantContext = usePlantContext()
  const { filteredTabs, tabIndex, setTabIndex } = useConfigurationTabs(
    tabType,
    tabOptions,
  )

  const normalizedHidden = useMemo(
    () => new Set(hiddenTabs.map(normalizeTabKey)),
    [hiddenTabs],
  )

  const visibleTabs = useMemo(() => {
    const base = filteredTabs.map((tab) => ({
      ...tab,
      originalName: tab.name,
    }))

    const afterHide =
      normalizedHidden.size > 0
        ? base.filter((tab) => !normalizedHidden.has(normalizeTabKey(tab.name)))
        : base

    return typeof transformTabs === 'function'
      ? transformTabs(afterHide, plantContext).filter(Boolean)
      : afterHide
  }, [filteredTabs, normalizedHidden, transformTabs, plantContext])

  const safeTabIndex =
    visibleTabs.length > 0 && tabIndex >= 0 && tabIndex < visibleTabs.length
      ? tabIndex
      : 0

  const activeTab = visibleTabs[safeTabIndex] || null

  const handleTabChange = (nextIndex) => {
    setTabIndex(nextIndex)
    if (typeof onActiveTabChange === 'function') {
      onActiveTabChange(visibleTabs[nextIndex] || null, nextIndex)
    }
  }

  const renderActiveTab = () => {
    if (!activeTab) return null

    // Match using original backend displayName first so display renames still resolve
    const lookupKey = activeTab.originalName || activeTab.name
    const TabComponent =
      resolveTabComponent(lookupKey, { overrideTabs, localTabs }) ||
      (lookupKey !== activeTab.name
        ? resolveTabComponent(activeTab.name, { overrideTabs, localTabs })
        : null)

    if (TabComponent) {
      return <TabComponent {...tabProps} />
    }

    if (typeof renderFallbackTab === 'function') {
      return renderFallbackTab(lookupKey, activeTab, tabProps)
    }

    return null
  }

  return (
    <>
      {visibleTabs.length > 0 && (
        <Stack
          direction='row'
          justifyContent='space-between'
          alignItems='center'
        >
          <TabSection
            tabIndex={safeTabIndex}
            setTabIndex={handleTabChange}
            tabs={visibleTabs.map((tab) => tab.name)}
          />
          {rightAction}
        </Stack>
      )}

      <Box sx={contentSx}>{renderActiveTab()}</Box>
    </>
  )
}

export default ProductionNormsTabs
