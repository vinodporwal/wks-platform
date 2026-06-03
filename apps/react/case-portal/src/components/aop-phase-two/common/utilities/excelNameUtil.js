export const generateExcelName = (dataGridStore, PAGE_NAME = '') => {
  const { verticalObject, siteObject, plantObject, year } = dataGridStore

  const VERTICAL_NAME = verticalObject?.name || ''
  const SITE_NAME = siteObject?.name || ''
  const PLANT_NAME = plantObject?.name || ''
  const AOP_YEAR = year?.selectedYear || ''

  return `${VERTICAL_NAME}_${SITE_NAME}_${PLANT_NAME}_${PAGE_NAME}_${AOP_YEAR}.xlsx`
}
