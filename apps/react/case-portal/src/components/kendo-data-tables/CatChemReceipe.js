import React from 'react'
import CatalystChecmicalsCalculationConstants from './CatalystChecmicalsCalculationConstants'
import CatalystChecmicalsCalculationRecipe from './CatalystChecmicalsCalculationRecipe'
import CatalystChecmicalsCalculationRecipeCalc from './CatalystChecmicalsCalculationRecipeCalc'
import CatalystChecmicalsCalculationRecipeChemGrade from './CatalystChecmicalsCalculationRecipeChemGrade'
import CatChemFinalCalutedData from './CatChemFinalCalutedData'

const CatChemReceipe = ({ permissions }) => {
     return (
          <div>
               <CatalystChecmicalsCalculationConstants />
               <CatalystChecmicalsCalculationRecipeChemGrade permissions={permissions} />
               <CatalystChecmicalsCalculationRecipe permissions={permissions} />
               <CatalystChecmicalsCalculationRecipeCalc permissions={permissions} />
               <CatChemFinalCalutedData permissions={permissions} />
          </div>
     )
}

export default CatChemReceipe
