const normalizeCol = (col, valueFormat) => {
  const isNumeric = col.type === 'number' || col.type === 'numberNonGrey'

  const newCol = {
    ...col,
    fixedWidth: col.fixedWidth,
    ...(isNumeric && !col.format && { format: valueFormat }),
    ...(col.children && {
      children: col.children.map((c) => normalizeCol(c, valueFormat)),
    }),
  }

  return newCol
}

const getEnhancedColDefsProposedAOP = ({ valueFormat }) => {
  const colDefs = [
    {
      field: 'productName',
      title: 'Particulars',
      editable: false,
      fixedWidth: 250,
      locked: true,
    },
    {
      field: 'UOM',
      title: 'UOM',
      editable: false,
      fixedWidth: 100,
    },
    {
      field: 'lastFY',
      title: 'Last FY',
      editable: false,
      type: 'number',
      fixedWidth: 150,
    },
    {
      field: 'sysGrn',
      title: 'Sys Gen',
      editable: false,
      type: 'number',
      fixedWidth: 150,
    },
    {
      field: 'proposed',
      title: 'Proposed',
      editable: true,
      type: 'numberNonGrey',
      fixedWidth: 150,
    },
    {
      field: 'remarks',
      title: 'Remarks',
      editable: true,
      fixedWidth: 200,
    },
  ]
  return colDefs.map((c) => normalizeCol(c, valueFormat))
}

export default getEnhancedColDefsProposedAOP
