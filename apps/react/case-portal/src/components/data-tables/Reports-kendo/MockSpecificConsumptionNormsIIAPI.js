export const MockSpecificConsumptionNormsIIAPI = {
  async getReport({
    category,
    AOP_YEAR,
    valueFormat,
    lowerVertName,
    FORMAT_VALUES_PRICE,
  }) {
    const currFY = AOP_YEAR || ''
    let prevFY1 = ''
    let prevFY2 = ''
    let prevFY3 = ''
    let prevFY4 = ''
    if (currFY.includes('-')) {
      const [start, end] = currFY.split('-').map(Number)
      prevFY1 = `${start - 1}-${(end - 1).toString().padStart(2, '0')}`
      prevFY2 = `${start - 2}-${(end - 2).toString().padStart(2, '0')}`
      prevFY3 = `${start - 3}-${(end - 3).toString().padStart(2, '0')}`
      prevFY4 = `${start - 4}-${(end - 4).toString().padStart(2, '0')}`
    }
    //ProductMixAndProduction
    //OtherVariableCost
    //ProductionCostCalculations
    //BYPRODUCT
    //CatChem
    //RAWMATERIAL
    if (lowerVertName === 'meg') {
      switch (category) {
        case 'RAWMATERIAL':
          return {
            columns: [
              {
                field: 'sno',
                title: 'S.no',
                widthT: 30,
                editable: false,
                align: 'right',
                format: '{0:0}',
              },
              {
                field: 'id',
                title: 'Id',
                editable: false,
                hidden: true,
                isVisible: false,
              },
              {
                field: 'material',
                title: 'Raw material',
                editable: false,
                widthT: 200,
              },
              {
                field: 'price',
                title: 'Price (Rs/MT)',
                editable: false,
                align: 'right',
                format: FORMAT_VALUES_PRICE,
                type: 'number',
              },
              { field: 'uom', title: 'Unit', widthT: 60, editable: false },
              // { field: 'design', title: 'Design', editable: false },
              {
                title: 'Design',
                children: [
                  {
                    field: 'design',
                    title: 'Norms',
                    editable: true,
                    align: 'right',
                    format: valueFormat,
                    type: 'numberNonGrey',
                  },
                  {
                    field: 'designRsMt',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },
              // {
              //   field: 'bestAchievedSinceLastFourYears',
              //   title: 'Best Achieved (last 4 years)',
              //   format: valueFormat,
              //   type: 'number',
              //   editable: false,
              // },
              {
                title: 'Best Achieved for last 4 Years',
                children: [
                  {
                    field: 'bestAchivedActual',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'bestAchivedActualRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },
              // {
              //   field: 'globalBenchmark',
              //   title: 'Global Benchmark',
              //   editable: false,
              // },
              {
                title: 'Global Benchmark',
                children: [
                  {
                    field: 'globalBenchmark',
                    title: 'Norms',
                    editable: true,
                    align: 'right',
                    format: valueFormat,
                    type: 'numberNonGrey',
                  },
                  {
                    field: 'globalBenchmarkRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },

              {
                title: `Budget ${prevFY1}`,
                children: [
                  {
                    field: 'budgetPrevYear',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'budgetPrevYearRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },
              {
                title: `Actual ${prevFY1}`,
                children: [
                  {
                    field: 'actualPrevYear',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'actualPrevYearRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },
              {
                title: `Proposed ${currFY}`,
                children: [
                  {
                    field: 'proposedBudget',
                    title: 'Norms',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'proposedBudgetRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },
              {
                field: 'remarks',
                title: 'Remarks',
                editable: true,
                fixedWidth: 110,
              },
            ],
          }

        case 'BYPRODUCT':
          return {
            columns: [
              {
                field: 'sno',
                title: 'S.no',
                widthT: 30,
                editable: false,
                align: 'right',
                format: '{0:0}',
              },
              {
                field: 'id',
                title: 'Id',
                editable: false,
                hidden: true,
                isVisible: false,
              },
              {
                field: 'material',
                title: 'Raw material',
                editable: false,
                widthT: 200,
              },
              {
                field: 'price',
                title: 'Price (Rs/MT)',
                editable: false,
                align: 'right',
                format: FORMAT_VALUES_PRICE,
                type: 'number',
              },
              { field: 'uom', title: 'Unit', widthT: 60, editable: false },
              // { field: 'design', title: 'Design', editable: false },
              {
                title: 'Design',
                children: [
                  {
                    field: 'design',
                    title: 'Norms',
                    editable: true,
                    format: valueFormat,
                    type: 'numberNonGrey',
                  },
                  {
                    field: 'designRsMt',
                    title: 'Rs/MT',
                    editable: false,
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },
              // {
              //   field: 'bestAchievedSinceLastFourYears',
              //   title: 'Best Achieved (last 4 years)',
              //   format: valueFormat,
              //   type: 'number',
              //   editable: false,
              // },
              {
                title: 'Best Achieved for last 4 Years',
                children: [
                  {
                    field: 'bestAchivedActual',
                    title: 'Norms',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'bestAchivedActualRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },
              // {
              //   field: 'globalBenchmark',
              //   title: 'Global Benchmark',
              //   editable: false,
              // },
              {
                title: 'Global Benchmark',
                children: [
                  {
                    field: 'globalBenchmark',
                    title: 'Norms',
                    editable: true,
                    format: valueFormat,
                    type: 'numberNonGrey',
                  },
                  {
                    field: 'globalBenchmarkRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },

              {
                title: `Budget ${prevFY1}`,
                children: [
                  {
                    field: 'budgetPrevYear',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'budgetPrevYearRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },
              {
                title: `Actual ${prevFY1}`,
                children: [
                  {
                    field: 'actualPrevYear',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'actualPrevYearRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },
              {
                title: `Proposed ${currFY}`,
                children: [
                  {
                    field: 'proposedBudget',
                    title: 'Norms',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'proposedBudgetRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },
              {
                field: 'remarks',
                title: 'Remarks',
                editable: true,
                fixedWidth: 110,
              },
            ],
          }

        case 'CatChem':
          return {
            columns: [
              {
                field: 'sno',
                title: 'S.no',
                widthT: 30,
                editable: false,
                align: 'right',
                format: '{0:0}',
              },
              {
                field: 'id',
                title: 'Id',
                editable: false,
                hidden: true,
                isVisible: false,
              },
              {
                field: 'material',
                title: 'Raw material',
                editable: false,
                widthT: 200,
              },
              {
                field: 'price',
                title: 'Price (Rs/MT)',
                editable: false,
                align: 'right',
                format: FORMAT_VALUES_PRICE,
                type: 'number',
              },
              { field: 'uom', title: 'Unit', widthT: 60, editable: false },
              // { field: 'design', title: 'Design', editable: false },
              {
                title: 'Design',
                children: [
                  {
                    field: 'design',
                    title: 'Norms',
                    editable: true,
                    format: valueFormat,
                    type: 'numberNonGrey',
                  },
                  {
                    field: 'designRsMt',
                    title: 'Rs/MT',
                    editable: false,
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },
              // {
              //   field: 'bestAchievedSinceLastFourYears',
              //   title: 'Best Achieved (last 4 years)',
              //   format: valueFormat,
              //   type: 'number',
              //   editable: false,
              // },
              {
                title: 'Best Achieved for last 4 Years',
                children: [
                  {
                    field: 'bestAchivedActual',
                    title: 'Norms',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'bestAchivedActualRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },
              // {
              //   field: 'globalBenchmark',
              //   title: 'Global Benchmark',
              //   editable: false,
              // },
              {
                title: 'Global Benchmark',
                children: [
                  {
                    field: 'globalBenchmark',
                    title: 'Norms',
                    editable: true,
                    format: valueFormat,
                    type: 'numberNonGrey',
                  },
                  {
                    field: 'globalBenchmarkRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },

              {
                title: `Budget ${prevFY1}`,
                children: [
                  {
                    field: 'budgetPrevYear',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'budgetPrevYearRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },
              {
                title: `Actual ${prevFY1}`,
                children: [
                  {
                    field: 'actualPrevYear',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'actualPrevYearRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },
              {
                title: `Proposed ${currFY}`,
                children: [
                  {
                    field: 'proposedBudget',
                    title: 'Norms',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'proposedBudgetRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },
              {
                field: 'remarks',
                title: 'Remarks',
                editable: true,
                fixedWidth: 110,
              },
            ],
          }

        case 'ProductionCostCalculations':
          return {
            columns: [
              {
                field: 'sno',
                title: 'S.no',
                widthT: 30,
                editable: false,
                align: 'right',
                format: '{0:0}',
              },
              {
                field: 'id',
                title: 'Id',
                editable: false,
                hidden: true,
                isVisible: false,
              },
              {
                field: 'material',
                title: 'Raw material',
                editable: false,
                widthT: 200,
              },
              {
                field: 'price',
                title: 'Price (Rs/MT)',
                editable: false,
                align: 'right',
                format: FORMAT_VALUES_PRICE,
                type: 'number',
              },
              { field: 'uom', title: 'Unit', widthT: 60, editable: false },
              // { field: 'design', title: 'Design', editable: false },
              {
                title: 'Design',
                children: [
                  {
                    field: 'design',
                    title: 'Norms',
                    editable: true,
                    format: valueFormat,
                    type: 'numberNonGrey',
                  },
                  {
                    field: 'designRsMt',
                    title: 'Rs/MT',
                    editable: false,
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },
              // {
              //   field: 'bestAchievedSinceLastFourYears',
              //   title: 'Best Achieved (last 4 years)',
              //   format: valueFormat,
              //   type: 'number',
              //   editable: false,
              // },
              {
                title: 'Best Achieved for last 4 Years',
                children: [
                  {
                    field: 'bestAchivedActual',
                    title: 'Norms',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'bestAchivedActualRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },
              // {
              //   field: 'globalBenchmark',
              //   title: 'Global Benchmark',
              //   editable: false,
              // },
              {
                title: 'Global Benchmark',
                children: [
                  {
                    field: 'globalBenchmark',
                    title: 'Norms',
                    editable: true,
                    format: numberNonGrey,
                    type: 'number',
                  },
                  {
                    field: 'globalBenchmarkRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },

              {
                title: `Budget ${prevFY1}`,
                children: [
                  {
                    field: 'budgetPrevYear',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'budgetPrevYearRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },
              {
                title: `Actual ${prevFY1}`,
                children: [
                  {
                    field: 'actualPrevYear',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'actualPrevYearRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },
              {
                title: `Proposed ${currFY}`,
                children: [
                  {
                    field: 'proposedBudget',
                    title: 'Norms',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'proposedBudgetRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },
              {
                field: 'remarks',
                title: 'Remarks',
                editable: true,
                fixedWidth: 110,
              },
            ],
          }

        case 'OtherVariableCost':
          return {
            columns: [
              {
                field: 'sno',
                title: 'S.no',
                widthT: 30,
                editable: false,
                align: 'right',
                format: '{0:0}',
              },
              {
                field: 'id',
                title: 'Id',
                editable: false,
                hidden: true,
                isVisible: false,
              },
              {
                field: 'material',
                title: 'Raw material',
                editable: false,
                widthT: 200,
              },
              {
                field: 'price',
                title: 'Price (Rs/MT)',
                editable: false,
                align: 'right',
                format: FORMAT_VALUES_PRICE,
                type: 'number',
              },
              { field: 'uom', title: 'Unit', widthT: 60, editable: false },
              // { field: 'design', title: 'Design', editable: false },
              {
                title: 'Design',
                children: [
                  {
                    field: 'design',
                    title: 'Norms',
                    editable: true,
                    format: valueFormat,
                    type: 'numberNonGrey',
                  },
                  {
                    field: 'designRsMt',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              // {
              //   field: 'bestAchievedSinceLastFourYears',
              //   title: 'Best Achieved (last 4 years)',
              //   format: valueFormat,
              //   type: 'number',
              //   editable: false,
              // },
              {
                title: 'Best Achieved for last 4 Years',
                children: [
                  {
                    field: 'bestAchivedActual',
                    title: 'Norms',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'bestAchivedActualRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              // {
              //   field: 'globalBenchmark',
              //   title: 'Global Benchmark',
              //   editable: false,
              // },
              {
                title: 'Global Benchmark',
                children: [
                  {
                    field: 'globalBenchmark',
                    title: 'Norms',
                    editable: true,
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'globalBenchmarkRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },

              {
                title: `Budget ${prevFY1}`,
                children: [
                  {
                    field: 'budgetPrevYear',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'budgetPrevYearRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              {
                title: `Actual ${prevFY1}`,
                children: [
                  {
                    field: 'actualPrevYear',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'actualPrevYearRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              {
                title: `Proposed ${currFY}`,
                children: [
                  {
                    field: 'proposedBudget',
                    title: 'Norms',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'proposedBudgetRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              {
                field: 'remarks',
                title: 'Remarks',
                editable: true,
                fixedWidth: 110,
              },
            ],
          }
        case 'ProductMixAndProduction':
          return {
            columns: [
              {
                field: 'sno',
                title: 'S.no',
                widthT: 30,
                editable: false,
                align: 'right',
                format: '{0:0}',
              },
              {
                field: 'id',
                title: 'Id',
                editable: false,
                hidden: true,
                isVisible: false,
              },
              {
                field: 'material',
                title: 'Raw material',
                editable: false,
                widthT: 200,
              },
              {
                field: 'price',
                title: 'Price (Rs/MT)',
                editable: false,
                align: 'right',
                format: FORMAT_VALUES_PRICE,
                type: 'number',
              },
              { field: 'uom', title: 'Unit', widthT: 60, editable: false },
              // { field: 'design', title: 'Design', editable: false },
              {
                title: 'Design',
                children: [
                  {
                    field: 'design',
                    title: 'Norms',
                    editable: true,
                    format: valueFormat,
                    type: 'numberNonGrey',
                  },
                  {
                    field: 'designRsMt',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              // {
              //   field: 'bestAchievedSinceLastFourYears',
              //   title: 'Best Achieved (last 4 years)',
              //   format: valueFormat,
              //   type: 'number',
              //   editable: false,
              // },
              {
                title: 'Best Achieved for last 4 Years',
                children: [
                  {
                    field: 'bestAchivedActual',
                    title: 'Norms',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'bestAchivedActualRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              // {
              //   field: 'globalBenchmark',
              //   title: 'Global Benchmark',
              //   editable: false,
              // },
              {
                title: 'Global Benchmark',
                children: [
                  {
                    field: 'globalBenchmark',
                    title: 'Norms',
                    editable: true,
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'globalBenchmarkRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },

              {
                title: `Budget ${prevFY1}`,
                children: [
                  {
                    field: 'budgetPrevYear',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'budgetPrevYearRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              {
                title: `Actual ${prevFY1}`,
                children: [
                  {
                    field: 'actualPrevYear',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'actualPrevYearRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              {
                title: `Proposed ${currFY}`,
                children: [
                  {
                    field: 'proposedBudget',
                    title: 'Norms',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'proposedBudgetRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              {
                field: 'remarks',
                title: 'Remarks',
                editable: true,
                fixedWidth: 110,
              },
            ],
          }
        case 'Utilities':
          return {
            columns: [
              {
                field: 'sno',
                title: 'S.no',
                widthT: 30,
                editable: false,
                align: 'right',
                format: '{0:0}',
              },
              {
                field: 'id',
                title: 'Id',
                editable: false,
                hidden: true,
                isVisible: false,
              },
              {
                field: 'material',
                title: 'Raw material',
                widthT: 200,
                editable: false,
              },
              {
                field: 'price',
                title: 'Price (Rs/MT)',
                editable: false,
                align: 'right',
                format: FORMAT_VALUES_PRICE,
                type: 'number',
              },
              { field: 'uom', title: 'Unit', widthT: 60, editable: false },
              // { field: 'design', title: 'Design', editable: false },
              {
                title: 'Design',
                children: [
                  {
                    field: 'design',
                    title: 'Norms',
                    editable: true,
                    format: valueFormat,
                    type: 'numberNonGrey',
                  },
                  {
                    field: 'designRsMt',
                    title: 'Rs/MT',
                    editable: false,
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },
              // {
              //   field: 'bestAchievedSinceLastFourYears',
              //   title: 'Best Achieved (last 4 years)',
              //   format: valueFormat,
              //   type: 'number',
              //   editable: false,
              // },
              {
                title: 'Best Achieved for last 4 Years',
                children: [
                  {
                    field: 'bestAchivedActual',
                    title: 'Norms',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'bestAchivedActualRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              // {
              //   field: 'globalBenchmark',
              //   title: 'Global Benchmark',
              //   editable: false,
              // },
              {
                title: 'Global Benchmark',
                children: [
                  {
                    field: 'globalBenchmark',
                    title: 'Norms',
                    editable: true,
                    format: valueFormat,
                    type: 'numberNonGrey',
                  },
                  {
                    field: 'globalBenchmarkRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },

              {
                title: `Budget ${prevFY1}`,
                children: [
                  {
                    field: 'budgetPrevYear',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'budgetPrevYearRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },
              {
                title: `Actual ${prevFY1}`,
                children: [
                  {
                    field: 'actualPrevYear',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'actualPrevYearRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },
              {
                title: `Proposed ${currFY}`,
                children: [
                  {
                    field: 'proposedBudget',
                    title: 'Norms',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'proposedBudgetRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: FORMAT_VALUES_PRICE,
                    type: 'number',
                  },
                ],
              },
              {
                field: 'remarks',
                title: 'Remarks',
                editable: true,
                fixedWidth: 110,
              },
            ],
          }
        case 'MaterialBalanceProposedNorms':
          return {
            columns: [
              {
                field: 'sno',
                title: 'S.no',
                widthT: 30,
                editable: false,
                align: 'right',
                format: '{0:0}',
              },
              {
                field: 'id',
                title: 'Id',
                editable: false,
                hidden: true,
                isVisible: false,
              },
              {
                field: 'material',
                title: 'Material',
                editable: false,
                widthT: 200,
              },

              {
                field: 'proposedBudget',
                title: 'Proposed Budget',
                editable: false,
                format: valueFormat,
                type: 'number',
                fixedWidth: 200,
              },
            ],
          }

        default:
          return { columns: [] }
      }
    } else {
      switch (category) {
        case 'RAWMATERIAL':
          return {
            columns: [
              {
                field: 'sno',
                title: 'S.no',
                widthT: 30,
                editable: false,
                align: 'right',
                format: '{0:0}',
              },
              {
                field: 'id',
                title: 'Id',
                editable: false,
                hidden: true,
                isVisible: false,
              },
              {
                field: 'material',
                title: 'Raw material',
                editable: false,
                widthT: 200,
              },
              {
                field: 'price',
                title: 'Price (Rs/MT)',
                editable: false,
                align: 'right',
                format: valueFormat,
                type: 'number',
              },
              { field: 'uom', title: 'Unit', widthT: 60, editable: false },
              // { field: 'design', title: 'Design', editable: false },
              {
                title: 'Design',
                children: [
                  {
                    field: 'design',
                    title: 'Norms',
                    editable: true,
                    align: 'right',
                    format: valueFormat,
                    type: 'numberNonGrey',
                  },
                  {
                    field: 'designRsMt',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              // {
              //   field: 'bestAchievedSinceLastFourYears',
              //   title: 'Best Achieved (last 4 years)',
              //   format: valueFormat,
              //   type: 'number',
              //   editable: false,
              // },
              {
                title: 'Best Achieved Since Commissioning',
                children: [
                  {
                    field: 'bestAchivedActual',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'bestAchivedActualRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              // {
              //   field: 'globalBenchmark',
              //   title: 'Global Benchmark',
              //   editable: false,
              // },
              {
                title: 'Global Benchmark',
                children: [
                  {
                    field: 'globalBenchmark',
                    title: 'Norms',
                    editable: true,
                    align: 'right',
                    format: valueFormat,
                    type: 'numberNonGrey',
                  },
                  {
                    field: 'globalBenchmarkRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },

              {
                title: `Budget ${prevFY1}`,
                children: [
                  {
                    field: 'budgetPrevYear',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'budgetPrevYearRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              {
                title: `Actual ${prevFY1}`,
                children: [
                  {
                    field: 'actualPrevYear',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'actualPrevYearRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              {
                title: `Proposed ${currFY}`,
                children: [
                  {
                    field: 'proposedBudget',
                    title: 'Norms',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'proposedBudgetRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              {
                field: 'remarks',
                title: 'Remarks',
                editable: true,
                fixedWidth: 110,
              },
            ],
          }

        case 'BYPRODUCT':
          return {
            columns: [
              {
                field: 'sno',
                title: 'S.no',
                widthT: 30,
                editable: false,
                align: 'right',
                format: '{0:0}',
              },
              {
                field: 'id',
                title: 'Id',
                editable: false,
                hidden: true,
                isVisible: false,
              },
              {
                field: 'material',
                title: 'Raw material',
                editable: false,
                widthT: 200,
              },
              {
                field: 'price',
                title: 'Price (Rs/MT)',
                editable: false,
                align: 'right',
                format: valueFormat,
                type: 'number',
              },
              { field: 'uom', title: 'Unit', widthT: 60, editable: false },
              // { field: 'design', title: 'Design', editable: false },
              {
                title: 'Design',
                children: [
                  {
                    field: 'design',
                    title: 'Norms',
                    editable: true,
                    format: valueFormat,
                    type: 'numberNonGrey',
                  },
                  {
                    field: 'designRsMt',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              // {
              //   field: 'bestAchievedSinceLastFourYears',
              //   title: 'Best Achieved (last 4 years)',
              //   format: valueFormat,
              //   type: 'number',
              //   editable: false,
              // },
              {
                title: 'Best Achieved Since Commissioning',
                children: [
                  {
                    field: 'bestAchivedActual',
                    title: 'Norms',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'bestAchivedActualRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              // {
              //   field: 'globalBenchmark',
              //   title: 'Global Benchmark',
              //   editable: false,
              // },
              {
                title: 'Global Benchmark',
                children: [
                  {
                    field: 'globalBenchmark',
                    title: 'Norms',
                    editable: true,
                    format: valueFormat,
                    type: 'numberNonGrey',
                  },
                  {
                    field: 'globalBenchmarkRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },

              {
                title: `Budget ${prevFY1}`,
                children: [
                  {
                    field: 'budgetPrevYear',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'budgetPrevYearRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              {
                title: `Actual ${prevFY1}`,
                children: [
                  {
                    field: 'actualPrevYear',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'actualPrevYearRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              {
                title: `Proposed ${currFY}`,
                children: [
                  {
                    field: 'proposedBudget',
                    title: 'Norms',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'proposedBudgetRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              {
                field: 'remarks',
                title: 'Remarks',
                editable: true,
                fixedWidth: 110,
              },
            ],
          }

        case 'CatChem':
          return {
            columns: [
              {
                field: 'sno',
                title: 'S.no',
                widthT: 30,
                editable: false,
                align: 'right',
                format: '{0:0}',
              },
              {
                field: 'id',
                title: 'Id',
                editable: false,
                hidden: true,
                isVisible: false,
              },
              {
                field: 'material',
                title: 'Raw material',
                editable: false,
                widthT: 200,
              },
              {
                field: 'price',
                title: 'Price (Rs/MT)',
                editable: false,
                align: 'right',
                format: valueFormat,
                type: 'number',
              },
              { field: 'uom', title: 'Unit', widthT: 60, editable: false },
              // { field: 'design', title: 'Design', editable: false },
              {
                title: 'Design',
                children: [
                  {
                    field: 'design',
                    title: 'Norms',
                    editable: true,
                    format: valueFormat,
                    type: 'numberNonGrey',
                  },
                  {
                    field: 'designRsMt',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              // {
              //   field: 'bestAchievedSinceLastFourYears',
              //   title: 'Best Achieved (last 4 years)',
              //   format: valueFormat,
              //   type: 'number',
              //   editable: false,
              // },
              {
                title: 'Best Achieved Since Commissioning',
                children: [
                  {
                    field: 'bestAchivedActual',
                    title: 'Norms',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'bestAchivedActualRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              // {
              //   field: 'globalBenchmark',
              //   title: 'Global Benchmark',
              //   editable: false,
              // },
              {
                title: 'Global Benchmark',
                children: [
                  {
                    field: 'globalBenchmark',
                    title: 'Norms',
                    editable: true,
                    format: valueFormat,
                    type: 'numberNonGrey',
                  },
                  {
                    field: 'globalBenchmarkRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },

              {
                title: `Budget ${prevFY1}`,
                children: [
                  {
                    field: 'budgetPrevYear',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'budgetPrevYearRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              {
                title: `Actual ${prevFY1}`,
                children: [
                  {
                    field: 'actualPrevYear',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'actualPrevYearRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              {
                title: `Proposed ${currFY}`,
                children: [
                  {
                    field: 'proposedBudget',
                    title: 'Norms',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'proposedBudgetRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              {
                field: 'remarks',
                title: 'Remarks',
                editable: true,
                fixedWidth: 110,
              },
            ],
          }

        case 'ProductionCostCalculations':
          return {
            columns: [
              {
                field: 'sno',
                title: 'S.no',
                widthT: 30,
                editable: false,
                align: 'right',
                format: '{0:0}',
              },
              {
                field: 'id',
                title: 'Id',
                editable: false,
                hidden: true,
                isVisible: false,
              },
              {
                field: 'material',
                title: 'Raw material',
                editable: false,
                widthT: 200,
              },
              {
                field: 'price',
                title: 'Price (Rs/MT)',
                editable: false,
                align: 'right',
                format: valueFormat,
                type: 'number',
              },
              { field: 'uom', title: 'Unit', widthT: 60, editable: false },
              // { field: 'design', title: 'Design', editable: false },
              {
                title: 'Design',
                children: [
                  {
                    field: 'design',
                    title: 'Norms',
                    editable: true,
                    format: valueFormat,
                    type: 'numberNonGrey',
                  },
                  {
                    field: 'designRsMt',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              // {
              //   field: 'bestAchievedSinceLastFourYears',
              //   title: 'Best Achieved (last 4 years)',
              //   format: valueFormat,
              //   type: 'number',
              //   editable: false,
              // },
              {
                title: 'Best Achieved Since Commissioning',
                children: [
                  {
                    field: 'bestAchivedActual',
                    title: 'Norms',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'bestAchivedActualRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              // {
              //   field: 'globalBenchmark',
              //   title: 'Global Benchmark',
              //   editable: false,
              // },
              {
                title: 'Global Benchmark',
                children: [
                  {
                    field: 'globalBenchmark',
                    title: 'Norms',
                    editable: true,
                    format: valueFormat,
                    type: 'numberNonGrey',
                  },
                  {
                    field: 'globalBenchmarkRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },

              {
                title: `Budget ${prevFY1}`,
                children: [
                  {
                    field: 'budgetPrevYear',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'budgetPrevYearRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              {
                title: `Actual ${prevFY1}`,
                children: [
                  {
                    field: 'actualPrevYear',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'actualPrevYearRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              {
                title: `Proposed ${currFY}`,
                children: [
                  {
                    field: 'proposedBudget',
                    title: 'Norms',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'proposedBudgetRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              {
                field: 'remarks',
                title: 'Remarks',
                editable: true,
                fixedWidth: 110,
              },
            ],
          }

        case 'OtherVariableCost':
          return {
            columns: [
              {
                field: 'sno',
                title: 'S.no',
                widthT: 30,
                editable: false,
                align: 'right',
                format: '{0:0}',
              },
              {
                field: 'id',
                title: 'Id',
                editable: false,
                hidden: true,
                isVisible: false,
              },
              {
                field: 'material',
                title: 'Raw material',
                editable: false,
                widthT: 200,
              },
              {
                field: 'price',
                title: 'Price (Rs/MT)',
                editable: false,
                align: 'right',
                format: valueFormat,
                type: 'number',
              },
              { field: 'uom', title: 'Unit', widthT: 60, editable: false },
              // { field: 'design', title: 'Design', editable: false },
              {
                title: 'Design',
                children: [
                  {
                    field: 'design',
                    title: 'Norms',
                    editable: true,
                    format: valueFormat,
                    type: 'numberNonGrey',
                  },
                  {
                    field: 'designRsMt',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              // {
              //   field: 'bestAchievedSinceLastFourYears',
              //   title: 'Best Achieved (last 4 years)',
              //   format: valueFormat,
              //   type: 'number',
              //   editable: false,
              // },
              {
                title: 'Best Achieved Since Commissioning',
                children: [
                  {
                    field: 'bestAchivedActual',
                    title: 'Norms',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'bestAchivedActualRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              // {
              //   field: 'globalBenchmark',
              //   title: 'Global Benchmark',
              //   editable: false,
              // },
              {
                title: 'Global Benchmark',
                children: [
                  {
                    field: 'globalBenchmark',
                    title: 'Norms',
                    editable: true,
                    format: valueFormat,
                    type: 'numberNonGrey',
                  },
                  {
                    field: 'globalBenchmarkRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },

              {
                title: `Budget ${prevFY1}`,
                children: [
                  {
                    field: 'budgetPrevYear',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'budgetPrevYearRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              {
                title: `Actual ${prevFY1}`,
                children: [
                  {
                    field: 'actualPrevYear',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'actualPrevYearRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              {
                title: `Proposed ${currFY}`,
                children: [
                  {
                    field: 'proposedBudget',
                    title: 'Norms',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'proposedBudgetRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              {
                field: 'remarks',
                title: 'Remarks',
                editable: true,
                fixedWidth: 110,
              },
            ],
          }
        case 'ProductMixAndProduction':
          return {
            columns: [
              {
                field: 'sno',
                title: 'S.no',
                widthT: 30,
                editable: false,
                align: 'right',
                format: '{0:0}',
              },
              {
                field: 'id',
                title: 'Id',
                editable: false,
                hidden: true,
                isVisible: false,
              },
              {
                field: 'material',
                title: 'Raw material',
                editable: false,
                widthT: 200,
              },
              {
                field: 'price',
                title: 'Price (Rs/MT)',
                editable: false,
                align: 'right',
                format: valueFormat,
                type: 'number',
              },
              { field: 'uom', title: 'Unit', widthT: 60, editable: false },
              // { field: 'design', title: 'Design', editable: false },
              {
                title: 'Design',
                children: [
                  {
                    field: 'design',
                    title: 'Norms',
                    editable: true,
                    format: valueFormat,
                    type: 'numberNonGrey',
                  },
                  {
                    field: 'designRsMt',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              // {
              //   field: 'bestAchievedSinceLastFourYears',
              //   title: 'Best Achieved (last 4 years)',
              //   format: valueFormat,
              //   type: 'number',
              //   editable: false,
              // },
              {
                title: 'Best Achieved Since Commissioning',
                children: [
                  {
                    field: 'bestAchivedActual',
                    title: 'Norms',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'bestAchivedActualRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              // {
              //   field: 'globalBenchmark',
              //   title: 'Global Benchmark',
              //   editable: false,
              // },
              {
                title: 'Global Benchmark',
                children: [
                  {
                    field: 'globalBenchmark',
                    title: 'Norms',
                    editable: true,
                    format: valueFormat,
                    type: 'numberNonGrey',
                  },
                  {
                    field: 'globalBenchmarkRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },

              {
                title: `Budget ${prevFY1}`,
                children: [
                  {
                    field: 'budgetPrevYear',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'budgetPrevYearRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              {
                title: `Actual ${prevFY1}`,
                children: [
                  {
                    field: 'actualPrevYear',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'actualPrevYearRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              {
                title: `Proposed ${currFY}`,
                children: [
                  {
                    field: 'proposedBudget',
                    title: 'Norms',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'proposedBudgetRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              {
                field: 'remarks',
                title: 'Remarks',
                editable: true,
                fixedWidth: 110,
              },
            ],
          }
        case 'Utilities':
          return {
            columns: [
              {
                field: 'sno',
                title: 'S.no',
                widthT: 30,
                editable: false,
                align: 'right',
                format: '{0:0}',
              },
              {
                field: 'id',
                title: 'Id',
                editable: false,
                hidden: true,
                isVisible: false,
              },
              {
                field: 'material',
                title: 'Raw material',
                editable: false,
                widthT: 200,
              },
              {
                field: 'price',
                title: 'Price (Rs/MT)',
                editable: false,
                align: 'right',
                format: valueFormat,
                type: 'number',
              },
              { field: 'uom', title: 'Unit', widthT: 60, editable: false },
              // { field: 'design', title: 'Design', editable: false },
              {
                title: 'Design',
                children: [
                  {
                    field: 'design',
                    title: 'Norms',
                    editable: true,
                    format: valueFormat,
                    type: 'numberNonGrey',
                  },
                  {
                    field: 'designRsMt',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              // {
              //   field: 'bestAchievedSinceLastFourYears',
              //   title: 'Best Achieved (last 4 years)',
              //   format: valueFormat,
              //   type: 'number',
              //   editable: false,
              // },
              {
                title: 'Best Achieved Since Commissioning',
                children: [
                  {
                    field: 'bestAchivedActual',
                    title: 'Norms',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'bestAchivedActualRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              // {
              //   field: 'globalBenchmark',
              //   title: 'Global Benchmark',
              //   editable: false,
              // },
              {
                title: 'Global Benchmark',
                children: [
                  {
                    field: 'globalBenchmark',
                    title: 'Norms',
                    editable: true,
                    format: valueFormat,
                    type: 'numberNonGrey',
                  },
                  {
                    field: 'globalBenchmarkRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },

              {
                title: `Budget ${prevFY1}`,
                children: [
                  {
                    field: 'budgetPrevYear',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'budgetPrevYearRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              {
                title: `Actual ${prevFY1}`,
                children: [
                  {
                    field: 'actualPrevYear',
                    title: 'Norms',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'actualPrevYearRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    align: 'right',
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              {
                title: `Proposed ${currFY}`,
                children: [
                  {
                    field: 'proposedBudget',
                    title: 'Norms',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                  {
                    field: 'proposedBudgetRsMT',
                    title: 'Rs/MT',
                    editable: false,
                    format: valueFormat,
                    type: 'number',
                  },
                ],
              },
              {
                field: 'remarks',
                title: 'Remarks',
                editable: true,
                fixedWidth: 110,
              },
            ],
          }
        case 'MaterialBalanceProposedNorms':
          return {
            columns: [
              {
                field: 'sno',
                title: 'S.no',
                widthT: 30,
                editable: false,
                align: 'right',
                format: '{0:0}',
              },
              {
                field: 'id',
                title: 'Id',
                editable: false,
                hidden: true,
                isVisible: false,
              },
              {
                field: 'material',
                title: 'Material',
                editable: false,
                widthT: 200,
              },

              {
                field: 'proposedBudget',
                title: 'Proposed Budget',
                editable: false,
                format: valueFormat,
                type: 'number',
                fixedWidth: 200,
              },
            ],
          }

        default:
          return { columns: [] }
      }
    }
  },
}
