import React from 'react'
import NetProductionHoursAvg from './NetProductionHoursAvg'
import ProposedOperatingCapacityAvg from './ProposedOperatingCapacityAvg'

const OtherProduction = () => {
  return (
    <div>
      <ProposedOperatingCapacityAvg />
      <NetProductionHoursAvg />
    </div>
  )
}

export default OtherProduction
