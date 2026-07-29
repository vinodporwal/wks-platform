import React, { useState, useEffect, useMemo } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  Box,
  Grid,
  MenuItem,
  Typography,
} from '@mui/material'
import { SaveIcon as SaveImageIcon } from 'assets/images/icons'
import { useSelector } from 'react-redux'

const MONTH_FIELDS = [
  'apr',
  'may',
  'jun',
  'jul',
  'aug',
  'sep',
  'oct',
  'nov',
  'dec',
  'jan',
  'feb',
  'mar',
]

const MONTH_LABELS = {
  apr: 'Apr',
  may: 'May',
  jun: 'Jun',
  jul: 'Jul',
  aug: 'Aug',
  sep: 'Sep',
  oct: 'Oct',
  nov: 'Nov',
  dec: 'Dec',
  jan: 'Jan',
  feb: 'Feb',
  mar: 'Mar',
}

const INITIAL_FORM_DATA = {
  sourceId: '',
  procurementPlant: '',
  utility: '',
  material: '',
  uom: '',
  processUnit: '',
  processPlantName: '',
  processPlantCode: '',
  cppPlantId: '',
  normParameterFkId: '',
  aopYear: '',
  remarks: '',
  ...MONTH_FIELDS.reduce((acc, m) => ({ ...acc, [m]: '' }), {}),
}

const INITIAL_FORM_ERROR = {
  sourceId: { isError: false, errorMessage: '' },
  processUnit: { isError: false, errorMessage: '' },
}

const FIELD_LABELS = {
  sourceId: 'Source',
  processUnit: 'Process Unit',
  remarks: 'Remarks',
}

const AddProcessUnitDialog = ({
  open,
  onClose,
  onSuccess,
  editRowData = null,
  sourceRows = [],
  plantRequirementData = [],
  financialYear = '',
  existingRows = [],
}) => {
  const isEditMode = !!editRowData

  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { year } = dataGridStore

  const AOP_YEAR = year?.selectedYear

  const [formData, setFormData] = useState(INITIAL_FORM_DATA)
  const [formError, setFormError] = useState(INITIAL_FORM_ERROR)
  const [isButtonDisabled, setIsButtonDisabled] = useState(false)

  const sourceOptions = useMemo(() => {
    return (sourceRows || []).map((row) => ({
      value: row.id,
      label: `${row.procurementPlant} — ${row.utility} / ${row.material}`,
      procurementPlant: row.procurementPlant,
      utility: row.utility,
      material: row.material,
      uom: row.uom,
      // FK fields available on the source row (importedPowerPlans API response)
      cppPlantFkId: row.cppPlantFkId || '', // → CPPPlant_FK_ID
      normParameterFkId: row.normParameterFkId || '', // → NormParameter_FK_Id
    }))
  }, [sourceRows])

  const processUnitOptions = useMemo(() => {
    const seen = new Set()
    const options = []
    plantRequirementData.forEach((item) => {
      const key = item.processPlant
      if (key && !seen.has(key)) {
        seen.add(key)
        options.push({
          value: item.processPlant,
          label: item.processPlant,
          processPlantName: item.processPlant,
          processPlantCode: item.processPlantCode || '',
        })
      }
    })
    return options
  }, [plantRequirementData])

  const allocatedProcessUnitsForSource = useMemo(() => {
    const sourceId = formData.sourceId
    if (!sourceId) return new Set()
    const dataRows = existingRows.filter((row) => !row.isTotal)
    return new Set(
      dataRows
        .filter((row) => row.sourceId === sourceId)
        .map((row) => row.processUnit),
    )
  }, [formData.sourceId, existingRows])

  const remainingBalanceBySource = useMemo(() => {
    const sourceId = formData.sourceId
    if (!sourceId) return {}
    const source = sourceRows.find((s) => s.id === sourceId)
    if (!source) return {}
    const dataRows = existingRows.filter((row) => !row.isTotal)
    const allocatedByMonth = {}
    MONTH_FIELDS.forEach((m) => {
      allocatedByMonth[m] = dataRows
        .filter((row) => row.sourceId === sourceId)
        .reduce((sum, row) => sum + (parseFloat(row[m]) || 0), 0)
    })
    const remaining = {}
    MONTH_FIELDS.forEach((m) => {
      const sourceQty = parseFloat(source[m]) || 0
      remaining[m] = Math.max(0, sourceQty - allocatedByMonth[m])
    })
    return remaining
  }, [formData.sourceId, sourceRows, existingRows])

  const powerDisData = useMemo(() => {
    return plantRequirementData?.filter(
      (item) => item.cppUtility === 'Power_Dis',
    )
  }, [plantRequirementData])

  const getHoursInMonth = (month, fy) => {
    const monthToCalendarMap = {
      apr: 0,
      may: 1,
      jun: 2,
      jul: 3,
      aug: 4,
      sep: 5,
      oct: 6,
      nov: 7,
      dec: 8,
      jan: 9,
      feb: 10,
      mar: 11,
    }
    const idx = monthToCalendarMap[month]
    if (idx === undefined || !fy) return 720
    const startYear = parseInt(fy.split('-')[0], 10)
    const calendarYear = idx < 9 ? startYear : startYear + 1
    const monthNumbers = [3, 4, 5, 6, 7, 8, 9, 10, 11, 0, 1, 2]
    const monthNum = monthNumbers[idx]
    const daysInMonth = new Date(calendarYear, monthNum + 1, 0).getDate()
    return daysInMonth * 24
  }

  const maxPossibleByProcessUnit = useMemo(() => {
    const result = {}
    powerDisData.forEach((item) => {
      const key = item.processPlant
      if (!result[key]) {
        const monthlyMax = {}
        MONTH_FIELDS.forEach((m) => {
          const kwh = parseFloat(item[m]) || 0
          const hours = getHoursInMonth(m, item.financialYear || financialYear)
          const value = kwh > 0 && hours > 0 ? kwh / 1000 / hours : 0
          monthlyMax[m] = Math.round(value * 100) / 100
        })
        result[key] = monthlyMax
      }
    })
    return result
  }, [powerDisData, financialYear])

  useEffect(() => {
    if (open) {
      if (editRowData) {
        setFormData({
          sourceId: editRowData.sourceId || '',
          procurementPlant: editRowData.procurementPlant || '',
          utility: editRowData.utility || '',
          material: editRowData.material || '',
          uom: editRowData.uom || '',
          processUnit: editRowData.processUnit || '',
          processPlantName: editRowData.processPlantName || '',
          processPlantCode: editRowData.processPlantCode || '',
          cppPlantId: editRowData.cppPlantId || '',
          normParameterFkId: editRowData.normParameterFkId || '',
          aopYear: editRowData.aopYear || '',
          remarks: editRowData.remarks || '',
          ...MONTH_FIELDS.reduce(
            (acc, m) => ({ ...acc, [m]: editRowData[m] ?? '' }),
            {},
          ),
        })
      } else {
        setFormData(INITIAL_FORM_DATA)
      }
      setFormError(INITIAL_FORM_ERROR)
      setIsButtonDisabled(false)
    }
  }, [open, editRowData])

  const handleFieldChange = (field, value) => {
    setFormData((prev) => {
      const next = { ...prev, [field]: value }
      if (field === 'sourceId') {
        const selected = sourceOptions.find((o) => o.value === value)
        if (selected) {
          next.procurementPlant = selected.procurementPlant
          next.utility = selected.utility
          next.material = selected.material
          next.uom = selected.uom
          // Capture FK fields from source row — correct per-row for JMD multi-plant
          next.cppPlantId = selected.cppPlantFkId // → CPPPlant_FK_ID
          next.normParameterFkId = selected.normParameterFkId // → NormParameter_FK_Id
        }
      }
      if (field === 'processUnit') {
        // Capture processPlantName and processPlantCode from the selected option
        const selectedOption = processUnitOptions.find((o) => o.value === value)
        next.processPlantName = selectedOption?.processPlantName || ''
        next.processPlantCode = selectedOption?.processPlantCode || ''
        // Note: cppPlantId comes from the source selection above, not from processUnit

        const maxVals = maxPossibleByProcessUnit[value]
        if (maxVals) {
          MONTH_FIELDS.forEach((m) => {
            const maxPossible = maxVals[m]
            const remaining = remainingBalanceBySource[m] || 0
            const defaultVal = Math.min(maxPossible, remaining)
            next[m] = Math.round(defaultVal * 100) / 100
          })
        }
      }
      return next
    })
    if (formError[field]?.isError) {
      setFormError((prev) => ({
        ...prev,
        [field]: { isError: false, errorMessage: '' },
      }))
    }
  }

  const validateForm = () => {
    let isValid = true
    const newErrors = { ...INITIAL_FORM_ERROR }

    if (!formData.sourceId) {
      newErrors.sourceId = {
        isError: true,
        errorMessage: `${FIELD_LABELS.sourceId} is required`,
      }
      isValid = false
    }
    if (!formData.processUnit) {
      newErrors.processUnit = {
        isError: true,
        errorMessage: `${FIELD_LABELS.processUnit} is required`,
      }
      isValid = false
    }

    const maxVals = maxPossibleByProcessUnit[formData.processUnit]
    if (maxVals) {
      MONTH_FIELDS.forEach((m) => {
        const val = Math.round((parseFloat(formData[m]) || 0) * 100) / 100
        if (val > maxVals[m] + 0.01) {
          isValid = false
        }
      })
    }

    setFormError(newErrors)
    return isValid
  }

  const handleSave = () => {
    if (!validateForm()) return

    setIsButtonDisabled(true)
    try {
      const payload = {
        id: isEditMode ? editRowData.id : null,
        cppPlantId: formData.cppPlantId, // → CPPPlant_FK_ID   (from source row)
        sourceId: formData.sourceId, // → ImportPower_FK_ID
        normParameterFkId: formData.normParameterFkId, // → NormParameter_FK_Id (from source row)
        processPlantName: formData.processPlantName,
        processPlantCode: formData.processPlantCode,
        procurementPlant: formData.procurementPlant,
        utility: formData.utility,
        material: formData.material,
        uom: formData.uom,
        processUnit: formData.processUnit,
        aopYear: AOP_YEAR,
        remarks: formData.remarks,
        ...MONTH_FIELDS.reduce(
          (acc, m) => ({ ...acc, [m]: parseFloat(formData[m]) || 0 }),
          {},
        ),
      }

      onClose()
      if (onSuccess) onSuccess(payload)
    } catch (error) {
      console.error('Error saving process unit allocation:', error)
    } finally {
      setIsButtonDisabled(false)
    }
  }

  const handleCancel = () => {
    onClose()
  }

  return (
    <Dialog open={open} onClose={handleCancel} maxWidth='md' fullWidth>
      <DialogTitle sx={{ borderBottom: '1px solid #ccc' }}>
        <Typography variant='h5' className='dialog-title'>
          {isEditMode
            ? 'Edit Process Unit Allocation'
            : 'Add Process Unit Allocation'}
        </Typography>
      </DialogTitle>
      <DialogContent dividers>
        <Box sx={{ mt: 2 }}>
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <TextField
                select
                fullWidth
                label={FIELD_LABELS.sourceId}
                value={formData.sourceId}
                onChange={(e) => handleFieldChange('sourceId', e.target.value)}
                error={formError.sourceId.isError}
                helperText={formError.sourceId.errorMessage}
                required
                size='small'
                disabled={isEditMode}
              >
                {sourceOptions.map((option) => (
                  <MenuItem key={option.value} value={option.value}>
                    {option.label}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>

            <Grid item xs={6}>
              <TextField
                fullWidth
                label='Plant'
                value={formData.procurementPlant}
                size='small'
                disabled
              />
            </Grid>

            <Grid item xs={6}>
              <TextField
                fullWidth
                label='UOM'
                value={formData.uom}
                size='small'
                disabled
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                select
                fullWidth
                label={FIELD_LABELS.processUnit}
                value={formData.processUnit}
                onChange={(e) =>
                  handleFieldChange('processUnit', e.target.value)
                }
                error={formError.processUnit.isError}
                helperText={formError.processUnit.errorMessage}
                required
                size='small'
              >
                {processUnitOptions.map((option) => {
                  const isAllocated = allocatedProcessUnitsForSource.has(
                    option.value,
                  )
                  const isEditingSameRow =
                    isEditMode && editRowData.processUnit === option.value
                  const isDisabled = isAllocated && !isEditingSameRow
                  return (
                    <MenuItem
                      key={option.value}
                      value={option.value}
                      disabled={isDisabled}
                    >
                      {option.label}
                      {isDisabled ? ' (Already allocated)' : ''}
                    </MenuItem>
                  )
                })}
              </TextField>
            </Grid>

            {MONTH_FIELDS.map((m) => {
              const maxVal = maxPossibleByProcessUnit[formData.processUnit]?.[m]
              const currentVal =
                Math.round((parseFloat(formData[m]) || 0) * 100) / 100
              const isError = maxVal !== undefined && currentVal > maxVal + 0.01
              return (
                <Grid item xs={3} key={m}>
                  <TextField
                    fullWidth
                    type='number'
                    label={MONTH_LABELS[m]}
                    value={formData[m]}
                    onChange={(e) => handleFieldChange(m, e.target.value)}
                    size='small'
                    error={isError}
                    helperText={
                      maxVal !== undefined
                        ? `Max: ${maxVal.toFixed(2)} MW${isError ? ' — Exceeded!' : ''}`
                        : ''
                    }
                  />
                </Grid>
              )
            })}

            <Grid item xs={12}>
              <TextField
                fullWidth
                label={FIELD_LABELS.remarks}
                value={formData.remarks}
                onChange={(e) => handleFieldChange('remarks', e.target.value)}
                size='small'
              />
            </Grid>
          </Grid>
        </Box>
      </DialogContent>
      <DialogActions>
        <Button
          onClick={handleCancel}
          color='secondary'
          disabled={isButtonDisabled}
          className='btn-no'
        >
          Cancel
        </Button>
        <Button
          onClick={handleSave}
          variant='contained'
          disabled={isButtonDisabled}
          className='btn-save'
          startIcon={
            <Box component='img' src={SaveImageIcon} className='w16-icon' />
          }
        >
          {isButtonDisabled ? 'Saving...' : 'Save'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export default AddProcessUnitDialog
