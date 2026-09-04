/**
 * Focuses the active input in the newly edited cell.
 */
const focusActiveCellInput = () => {
  requestAnimationFrame(() => {
    setTimeout(() => {
      const activeInput = document.querySelector(
        '.k-grid-edit-cell input, .k-edit-cell input, .k-grid td.k-command-cell input, .input-editor input, input.input-editor, .k-grid-table input, td[class*="cell"] input'
      )
      if (activeInput) {
        activeInput.focus()
        if (typeof activeInput.select === 'function') {
          activeInput.select()
        }
      }
    }, 30)
  })
}

/**
 * Handles keyboard navigation (Tab, Shift+Tab, Enter, Shift+Enter, Escape)
 * between editable cells in KendoDataTables.
 */
export const handleTabKeyNavigation = ({
  e,
  activeCellRef,
  columns,
  hiddenFields = [],
  rows,
  setRows,
  setEdit,
  extractAllColumns,
}) => {
  const nativeEvent = e.nativeEvent
  const key = nativeEvent.key

  if (key === 'Escape') {
    nativeEvent.preventDefault()
    setEdit({})
    activeCellRef.current = { rowId: null, field: null }
    return
  }

  if (key !== 'Tab' && key !== 'Enter') return

  const { rowId, field: currentField } = activeCellRef.current
  if (!rowId || !currentField) return

  const allCols = extractAllColumns(columns).filter(
    (col) => !(hiddenFields || []).includes(col.field) && !col.hidden,
  )
  const editableCols = allCols.filter(
    (col) =>
      col.editable === true &&
      col.type !== 'textarea' &&
      col.field !== 'remarks' &&
      col.field !== 'reasons',
  )
  if (editableCols.length === 0) return

  const currentRowIndex = rows?.findIndex((r) => String(r.id) === String(rowId))
  if (currentRowIndex === -1 || currentRowIndex === undefined) return

  const currentEditableColIndex = editableCols.findIndex(
    (c) => c.field === currentField,
  )
  if (currentEditableColIndex === -1) return

  nativeEvent.preventDefault()

  if (key === 'Tab') {
    const nextIdx = nativeEvent.shiftKey
      ? currentEditableColIndex - 1
      : currentEditableColIndex + 1

    if (nextIdx >= 0 && nextIdx < editableCols.length) {
      // Next/prev editable cell in same row
      const nextField = editableCols[nextIdx].field
      const newEdit = { [rowId]: [nextField] }
      setEdit(newEdit)
      activeCellRef.current = { rowId, field: nextField }
      focusActiveCellInput()
    } else if (!nativeEvent.shiftKey && nextIdx >= editableCols.length) {
      // Wrap to first editable col of next editable row
      let nextRowIndex = currentRowIndex + 1
      while (nextRowIndex < (rows?.length || 0)) {
        const nextRow = rows[nextRowIndex]
        if (nextRow && nextRow.isEditable !== false) {
          const nextField = editableCols[0].field
          const newEdit = { [nextRow.id]: [nextField] }
          setEdit(newEdit)
          activeCellRef.current = { rowId: nextRow.id, field: nextField }
          focusActiveCellInput()
          break
        }
        nextRowIndex++
      }
    } else if (nativeEvent.shiftKey && nextIdx < 0) {
      // Wrap to last editable col of prev editable row
      let prevRowIndex = currentRowIndex - 1
      while (prevRowIndex >= 0) {
        const prevRow = rows[prevRowIndex]
        if (prevRow && prevRow.isEditable !== false) {
          const nextField = editableCols[editableCols.length - 1].field
          const newEdit = { [prevRow.id]: [nextField] }
          setEdit(newEdit)
          activeCellRef.current = { rowId: prevRow.id, field: nextField }
          focusActiveCellInput()
          break
        }
        prevRowIndex--
      }
    }
  } else if (key === 'Enter') {
    if (nativeEvent.shiftKey) {
      // Shift+Enter: Move to same column in previous editable row
      let prevRowIndex = currentRowIndex - 1
      while (prevRowIndex >= 0) {
        const prevRow = rows[prevRowIndex]
        if (prevRow && prevRow.isEditable !== false) {
          const newEdit = { [prevRow.id]: [currentField] }
          setEdit(newEdit)
          activeCellRef.current = { rowId: prevRow.id, field: currentField }
          focusActiveCellInput()
          break
        }
        prevRowIndex--
      }
    } else {
      // Enter: Move to same column in next editable row
      let nextRowIndex = currentRowIndex + 1
      while (nextRowIndex < (rows?.length || 0)) {
        const nextRow = rows[nextRowIndex]
        if (nextRow && nextRow.isEditable !== false) {
          const newEdit = { [nextRow.id]: [currentField] }
          setEdit(newEdit)
          activeCellRef.current = { rowId: nextRow.id, field: currentField }
          focusActiveCellInput()
          break
        }
        nextRowIndex++
      }
    }
  }
}
