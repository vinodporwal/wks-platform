import React, { useState } from 'react'
import AopTabs from 'components/AopTabs'
import SafetyImprovementInitiative from './SafetyImprovementInitiative'
import ProfitImprovementInitiative from './ProfitImprovementInitiative'
import ReliabilityImprovementInitiative from './ReliabilityImprovementInitiative'
import PlantTeam from './PlantTeam'

const PlantAOPReport = ({ permissions }) => {
  const [tabIndex, setTabIndex] = useState(0)
  const defaultTabs = [
    'Plant Team (Size)',
    'Safety Improvement Initiative',
    'Profit Improvement Initiative',
    'Reliability Improvement Initiative',
    'People Initiative',
  ]

  return (
    <div>
      {defaultTabs?.length > 1 && (
        <AopTabs
          tabIndex={tabIndex}
          setTabIndex={setTabIndex}
          tabs={defaultTabs}
        />
      )}
      {tabIndex === 0 && <PlantTeam onlyPlantTeam />}

      {tabIndex === 1 && (
        <SafetyImprovementInitiative permissions={permissions} />
      )}
      {tabIndex === 2 && (
        <ProfitImprovementInitiative permissions={permissions} />
      )}
      {tabIndex === 3 && (
        <ReliabilityImprovementInitiative permissions={permissions} />
      )}
      {tabIndex === 4 && <PlantTeam onlyPeopleInitiative />}
    </div>
  )
}

export default PlantAOPReport
