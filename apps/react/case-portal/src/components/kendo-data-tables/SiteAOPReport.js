import React, { useState, useEffect, useCallback } from 'react'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { SiteReportDataService } from 'services/SiteReportDataService'
import AopTabs from 'components/AopTabs'
import SiteTeam from './SiteAOPReport/SiteTeam'
import SiteSafetyPerformanceTarget from './SiteAOPReport/SiteSafetyPerformanceTarget'
import ConversionVariableCost from './ConversionVariableCost'
import EnergyPerformance from './SiteAOPReport/EnergyPerformance'
import FixedExpenses from './FixedExpenses'
import Capex from './Capex'
import ShutdownSlowdownPlan from './SlowdownPlan'
import TechnicalAvailability from './TechnicalAvailability'
import CrackerReportMannualEntry from './CrackerReportMannualEntry'
import MajorSafetyInitiative from './SiteAOPReport/MajorSafetyInitiative'
import MajorProfitInitiative from './MajorProfitInitiative'
import MajorReliabilityInitiative from './MajorReliabilityInitiative'
import MajorPeopleInitiative from './SiteAOPReport/MajorPeopleInitiative'
import MajorReliabilityInitiative from './SiteAOPReport/MajorReliabilityInitiative'
import MajorPeopleInitiative from './MajorPeopleInitiative'
import MCUCapacityUtilization from './MCUCapacityUtilization'

// Default hardcoded fallback tabs
const DEFAULT_TABS = [
  {
    tabName: 'SiteTeam',
    tabDisplayName: 'Site Team',
    tabSequence: 1,
    isVisible: true,
  },
  {
    tabName: 'SiteSafetyPerformanceTarget',
    tabDisplayName: 'Safety Performance & Targets',
    tabSequence: 2,
    isVisible: true,
  },
  {
    tabName: 'MajorSafetyInitiative',
    tabDisplayName: 'Major Safety Improvement',
    tabSequence: 3,
    isVisible: true,
  },
  {
    tabName: 'EnergyPerformance',
    tabDisplayName: 'Energy Performance',
    tabSequence: 4,
    isVisible: true,
  },
  {
    tabName: 'MajorProfitInitiative',
    tabDisplayName: 'Major Profit and Operability Improvement',
    tabSequence: 5,
    isVisible: true,
  },
  {
    tabName: 'MajorReliabilityInitiative',
    tabDisplayName: 'Major Reliability Improvement',
    tabSequence: 6,
    isVisible: true,
  },
  {
    tabName: 'MajorPeopleInitiative',
    tabDisplayName: 'Major People Initiative',
    tabSequence: 7,
    isVisible: true,
  },
  {
    tabName: 'ShutdownSlowdownPlan',
    tabDisplayName: 'Shutdown / Slowdown plan',
    tabSequence: 8,
    isVisible: true,
  },
  {
    tabName: 'FixedExpenses',
    tabDisplayName: 'Fixed Expenses',
    tabSequence: 9,
    isVisible: true,
  },
  {
    tabName: 'ReportManualEntry',
    tabDisplayName: 'Report Manual Entry',
    tabSequence: 10,
    isVisible: true,
  },
  {
    tabName: 'Capex',
    tabDisplayName: 'Capex/PIO Plan',
    tabSequence: 11,
    isVisible: true,
  },
  {
    tabName: 'TechnicalAvailability',
    tabDisplayName: 'Technical Availability',
    tabSequence: 12,
    isVisible: true,
  },
  {
    tabName: 'MCUCapacityUtilization',
    tabDisplayName: 'MCU Capacity Utilization (%)',
    tabSequence: 13,
    isVisible: true,
  },
  {
    tabName: 'ConversionVariableCost',
    tabDisplayName: 'Conversion & Variable Cost',
    tabSequence: 14,
    isVisible: true,
  },
]

const SiteAOPReport = ({ permissions }) => {
  const [tabIndex, setTabIndex] = useState(0)
  const [tabs, setTabs] = useState(DEFAULT_TABS)

  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const SITE_ID = dataGridStore?.siteObject?.id

  // Fetch tabs from SP via API, fallback to DEFAULT_TABS
  const fetchTabs = useCallback(async () => {
    try {
      const response = await SiteReportDataService.getSiteAopReportTabs(
        keycloak,
        SITE_ID,
      )
      const data = response?.data?.Data || response?.data || []
      if (Array.isArray(data) && data.length > 0) {
        const sortedVisibleTabs = data
          .filter((t) => t.isVisible !== false)
          .sort((a, b) => (a.tabSequence ?? 0) - (b.tabSequence ?? 0))
        if (sortedVisibleTabs.length > 0) {
          setTabs(sortedVisibleTabs)
        }
      }
    } catch (error) {
      console.warn('Using default hardcoded tabs due to fetch error:', error)
      setTabs(DEFAULT_TABS)
    }
  }, [keycloak, SITE_ID])

  useEffect(() => {
    fetchTabs()
  }, [fetchTabs])

  const renderTabContent = (tabName, tabDisplayName) => {
    switch (tabName || tabDisplayName) {
      case 'SiteTeam':
      case 'Site Team':
        return (
          <SiteTeam permissions={permissions} tabDisplayName={tabDisplayName} />
        )
      case 'SiteSafetyPerformanceTarget':
      case 'Safety Performance & Targets':
        return (
          <SiteSafetyPerformanceTarget
            permissions={permissions}
            tabDisplayName={tabDisplayName}
          />
        )
      case 'MajorSafetyInitiative':
      case 'Major Safety Improvement':
        return (
          <MajorSafetyInitiative
            permissions={permissions}
            tabDisplayName={tabDisplayName}
          />
        )
      case 'EnergyPerformance':
      case 'Energy Performance':
        return (
          <EnergyPerformance
            permissions={permissions}
            tabDisplayName={tabDisplayName}
          />
        )
      case 'MajorProfitInitiative':
      case 'Major Profit and Operability Improvement':
        return (
          <MajorProfitInitiative
            permissions={permissions}
            tabDisplayName={tabDisplayName}
          />
        )
      case 'MajorReliabilityInitiative':
      case 'Major Reliability Improvement':
        return (
          <MajorReliabilityInitiative
            permissions={permissions}
            tabDisplayName={tabDisplayName}
          />
        )
      case 'MajorPeopleInitiative':
      case 'Major People Initiative':
        return (
          <MajorPeopleInitiative
            permissions={permissions}
            tabDisplayName={tabDisplayName}
          />
        )
      case 'ShutdownSlowdownPlan':
      case 'Shutdown / Slowdown plan':
        return (
          <ShutdownSlowdownPlan
            permissions={permissions}
            tabDisplayName={tabDisplayName}
          />
        )
      case 'FixedExpenses':
      case 'Fixed Expenses':
        return (
          <FixedExpenses
            permissions={permissions}
            tabDisplayName={tabDisplayName}
          />
        )
      case 'ReportManualEntry':
      case 'Report Manual Entry':
        return (
          <CrackerReportMannualEntry
            tabIndex={5}
            permissions={permissions}
            tabDisplayName={tabDisplayName}
          />
        )
      case 'Capex':
      case 'Capex/PIO Plan':
        return (
          <Capex permissions={permissions} tabDisplayName={tabDisplayName} />
        )
      case 'TechnicalAvailability':
      case 'Technical Availability':
        return (
          <TechnicalAvailability
            permissions={permissions}
            tabDisplayName={tabDisplayName}
          />
        )
      case 'MCUCapacityUtilization':
      case 'MCU Capacity Utilization (%)':
        return (
          <MCUCapacityUtilization
            permissions={permissions}
            tabDisplayName={tabDisplayName}
          />
        )
      case 'ConversionVariableCost':
      case 'Conversion & Variable Cost':
        return (
          <ConversionVariableCost
            permissions={permissions}
            tabDisplayName={tabDisplayName}
          />
        )
      default:
        return null
    }
  }

  const tabDisplayNames = tabs.map((t) => t.tabDisplayName || t.tabName)
  const currentTab = tabs[tabIndex] || tabs[0]

  return (
    <div>
      {tabDisplayNames?.length > 1 && (
        <AopTabs
          tabIndex={tabIndex}
          setTabIndex={setTabIndex}
          tabs={tabDisplayNames}
        />
      )}

      {currentTab &&
        renderTabContent(currentTab.tabName, currentTab.tabDisplayName)}
    </div>
  )
}

export default SiteAOPReport
