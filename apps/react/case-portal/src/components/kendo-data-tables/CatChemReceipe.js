import React, { useState } from 'react'
import CatalystChecmicalsCalculationConstants from './CatalystChecmicalsCalculationConstants'
import CatalystChecmicalsCalculationRecipe from './CatalystChecmicalsCalculationRecipe'
import CatalystChecmicalsCalculationRecipeCalc from './CatalystChecmicalsCalculationRecipeCalc'
import CatalystChecmicalsCalculationRecipeChemGrade from './CatalystChecmicalsCalculationRecipeChemGrade'
import CatChemFinalCalutedData from './CatChemFinalCalutedData'

const CatChemReceipe = ({ permissions }) => {
  const [refresh2, setRefresh2] = useState(0)
  const [refresh3, setRefresh3] = useState(0)
  const [refresh4, setRefresh4] = useState(0)
  const [refresh5, setRefresh5] = useState(0)

  return (
    <div>
      <CatalystChecmicalsCalculationConstants
        onSaveOrImport={() => {
          setRefresh2((p) => p + 1)
          setRefresh3((p) => p + 1)
          setRefresh4((p) => p + 1)
          setRefresh5((p) => p + 1)
        }}
      />
      <CatalystChecmicalsCalculationRecipeChemGrade
        permissions={permissions}
        refreshTrigger={refresh2}
        onSaveOrImport={() => {
          setRefresh3((p) => p + 1)
          setRefresh4((p) => p + 1)
          setRefresh5((p) => p + 1)
        }}
      />
      <CatalystChecmicalsCalculationRecipe
        permissions={permissions}
        refreshTrigger={refresh3}
        onSaveOrImport={() => {
          setRefresh4((p) => p + 1)
          setRefresh5((p) => p + 1)
        }}
      />
      <CatalystChecmicalsCalculationRecipeCalc
        permissions={permissions}
        refreshTrigger={refresh4}
        onSaveOrImport={() => {
          setRefresh5((p) => p + 1)
        }}
      />
      <CatChemFinalCalutedData
        permissions={permissions}
        refreshTrigger={refresh5}
      />
    </div>
  )
}

export default CatChemReceipe
