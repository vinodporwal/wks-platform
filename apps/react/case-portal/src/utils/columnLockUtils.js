/**
 * Determines whether a column is a description, particulars, name, or products column
 * that should be locked in Kendo grids on horizontal scroll.
 *
 * @param {Object} col - The column configuration object.
 * @returns {boolean} True if the column should be locked, false otherwise.
 */
export const shouldLockColumn = (col) => {
  if (!col) return false
  const title = col.title || ''
  const field = col.field || ''
  const fieldLower = field.toLowerCase()

  return (
    title === 'Particular' ||
    title === 'Particulars' ||
    title === 'Particulars/Grade' ||
    title === 'Products' ||
    title === 'By Products' ||
    title === 'Description' ||
    title === 'Slowdown Desc' ||
    title === 'Shutdown Desc' ||
    field === 'Particulars' ||
    field === 'productName' ||
    field === 'Name' ||
    fieldLower === 'discription' ||
    fieldLower === 'description' ||
    fieldLower === 'discriptiondrpdwn' ||
    fieldLower === 'uom' ||
    fieldLower === 'uom/mt' ||
    field ==='sapCode'
  )
}
