import React, { useState, useEffect } from 'react'
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
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { getRoleName } from 'services/role-service'
import { InputApiService } from 'components/aop-phase-two/services/cpp/inputApiService'
import { SaveIcon as SaveImageIcon } from 'assets/images/icons'

const INITIAL_FORM_DATA = {
  procurementPlant: '',
  name: '',
  displayName: '',
  materialCode: '',
  sapCode: '',
  uom: 'MW',
}

const INITIAL_FORM_ERROR = {
  procurementPlant: { isError: false, errorMessage: '' },
  name: { isError: false, errorMessage: '' },
  displayName: { isError: false, errorMessage: '' },
  materialCode: { isError: false, errorMessage: '' },
  sapCode: { isError: false, errorMessage: '' },
  uom: { isError: false, errorMessage: '' },
}

// Fields that are required for form submission (procurementPlant only in add mode)
const REQUIRED_FIELDS = ['procurementPlant', 'name', 'uom']

// Human-readable labels for each field
const FIELD_LABELS = {
  procurementPlant: 'Procurement Plant',
  name: 'Utility/Material',
  displayName: 'Display Name',
  materialCode: 'Material Code',
  sapCode: 'SAP Code',
  uom: 'UOM',
}

const AddSourceDialog = ({ open, onClose, onSuccess, editRowData = null }) => {
  const keycloak = useSession()
  const isEditMode = !!editRowData

  const [formData, setFormData] = useState(INITIAL_FORM_DATA)
  const [formError, setFormError] = useState(INITIAL_FORM_ERROR)
  const [isButtonDisabled, setIsButtonDisabled] = useState(false)
  const [procurementPlantOptions, setProcurementPlantOptions] = useState([])

  // Read plant, year, and role flags from the global store
  const { plantObject, year, oldYear, isReleased } = useSelector(
    (state) => state.dataGridStore,
  )

  const PLANT_ID = plantObject?.id
  const IS_OLD_YEAR = oldYear?.oldYear
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const AOP_YEAR = year?.selectedYear

  // Fetch procurement plants linked to the current CPP plant
  // (Plants rows where SourceName = CPP plant UUID)
  useEffect(() => {
    const fetchProcurementPlants = async () => {
      if (!open || !PLANT_ID) {
        setProcurementPlantOptions([])
        return
      }
      try {
        const response = await InputApiService.getCapacityProcurementPlants(
          keycloak,
          PLANT_ID,
        )
        const apiOptions = (response?.data || []).map((plant) => ({
          value: plant.procurementPlantId,
          label: plant.name || plant.procurementPlantId,
        }))

        // In edit mode, ensure the existing selection is present
        if (isEditMode && editRowData?.plantFkId) {
          if (!apiOptions.find((o) => o.value === editRowData.plantFkId)) {
            apiOptions.push({
              value: editRowData.plantFkId,
              label: editRowData.plantName || editRowData.plantFkId,
            })
          }
        }
        setProcurementPlantOptions(apiOptions)

        // In edit mode the grid row may not carry plantFkId — resolve it by
        // matching the plantName against the loaded options
        if (isEditMode && !editRowData?.plantFkId && editRowData?.plantName) {
          const matched = apiOptions.find(
            (o) => o.label === editRowData.plantName,
          )
          if (matched) {
            setFormData((prev) => ({
              ...prev,
              procurementPlant: matched.value,
            }))
          }
        }
      } catch (error) {
        console.error('Error fetching procurement plants:', error)
      }
    }

    fetchProcurementPlants()
  }, [open, PLANT_ID, keycloak, isEditMode, editRowData])

  // Reset / pre-populate form whenever the dialog opens
  useEffect(() => {
    if (open) {
      if (editRowData) {
        // Edit mode — pre-fill with the row's existing source fields
        setFormData({
          procurementPlant: editRowData.plantFkId || '',
          name: editRowData.sourceName || '',
          displayName: editRowData.sourceName || '',
          materialCode: editRowData.materialCode || '',
          sapCode: editRowData.sapCode || '',
          uom: editRowData.uom || 'MW',
        })
      } else {
        // Add mode — blank form
        setFormData(INITIAL_FORM_DATA)
      }
      setFormError(INITIAL_FORM_ERROR)
      setIsButtonDisabled(false)
    }
  }, [open, editRowData])

  const handleFieldChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }))
    // Clear error for the changed field
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

    REQUIRED_FIELDS.forEach((field) => {
      // procurementPlant is only required when adding (cannot be changed in edit)
      if (isEditMode && field === 'procurementPlant') return
      const value = formData[field]
      if (!value || value.toString().trim() === '') {
        newErrors[field] = {
          isError: true,
          errorMessage: `${FIELD_LABELS[field]} is required`,
        }
        isValid = false
      }
    })

    setFormError(newErrors)
    return isValid
  }

  // ── API handler ─────────────────────────────────────────────────────────

  const handleSave = async () => {
    if (!validateForm()) return

    setIsButtonDisabled(true)
    try {
      if (isEditMode) {
        // Update existing source
        await InputApiService.updateCapacitySource(
          keycloak,
          editRowData.sourceId,
          {
            name: formData.name,
            displayName: formData.displayName,
            materialCode: formData.materialCode,
            sapCode: formData.sapCode,
            uom: formData.uom,
          },
        )
      } else {
        // Add new source
        await InputApiService.addCapacitySource(keycloak, {
          cppPlant: PLANT_ID,
          procurementPlant: formData.procurementPlant,
          name: formData.name,
          displayName: formData.displayName,
          materialCode: formData.materialCode,
          sapCode: formData.sapCode,
          uom: formData.uom,
          aopYear: AOP_YEAR,
        })
      }

      onClose()
      if (onSuccess) onSuccess()
    } catch (error) {
      console.error(
        isEditMode ? 'Error updating source:' : 'Error adding source:',
        error,
      )
    } finally {
      setIsButtonDisabled(false)
    }
  }

  const handleCancel = () => {
    onClose()
  }

  return (
    <Dialog open={open} onClose={handleCancel} maxWidth='sm' fullWidth>
      <DialogTitle sx={{ borderBottom: '1px solid #ccc' }}>
        <Typography variant='h6' className='dialog-title'>
          {isEditMode ? 'Edit Source' : 'Add Source'}
        </Typography>
      </DialogTitle>
      <DialogContent dividers>
        <Box sx={{ mt: 2 }}>
          <Grid container spacing={2}>
            {/* Procurement Plant — dropdown (disabled in edit mode) */}
            <Grid item xs={6}>
              <TextField
                select
                fullWidth
                label={FIELD_LABELS.procurementPlant}
                value={formData.procurementPlant}
                onChange={(e) =>
                  handleFieldChange('procurementPlant', e.target.value)
                }
                error={formError.procurementPlant.isError}
                helperText={formError.procurementPlant.errorMessage}
                required
                size='small'
                disabled={isEditMode}
              >
                {procurementPlantOptions.map((option) => (
                  <MenuItem key={option.value} value={option.value}>
                    {option.label}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>

            {/* Name / Utility-Material */}
            <Grid item xs={6}>
              <TextField
                fullWidth
                label={FIELD_LABELS.name}
                value={formData.name}
                onChange={(e) => handleFieldChange('name', e.target.value)}
                error={formError.name.isError}
                helperText={formError.name.errorMessage}
                required
                size='small'
                autoFocus
              />
            </Grid>

            {/* Display Name */}
            <Grid item xs={6}>
              <TextField
                fullWidth
                label={FIELD_LABELS.displayName}
                value={formData.displayName}
                onChange={(e) =>
                  handleFieldChange('displayName', e.target.value)
                }
                error={formError.displayName.isError}
                helperText={formError.displayName.errorMessage}
                size='small'
              />
            </Grid>

            {/* Material Code */}
            <Grid item xs={6}>
              <TextField
                fullWidth
                label={FIELD_LABELS.materialCode}
                value={formData.materialCode}
                onChange={(e) =>
                  handleFieldChange('materialCode', e.target.value)
                }
                error={formError.materialCode.isError}
                helperText={formError.materialCode.errorMessage}
                size='small'
              />
            </Grid>

            {/* SAP Code */}
            <Grid item xs={6}>
              <TextField
                fullWidth
                label={FIELD_LABELS.sapCode}
                value={formData.sapCode}
                onChange={(e) => handleFieldChange('sapCode', e.target.value)}
                error={formError.sapCode.isError}
                helperText={formError.sapCode.errorMessage}
                size='small'
              />
            </Grid>

            {/* UOM */}
            <Grid item xs={6}>
              <TextField
                fullWidth
                label={FIELD_LABELS.uom}
                value={formData.uom}
                onChange={(e) => handleFieldChange('uom', e.target.value)}
                error={formError.uom.isError}
                helperText={formError.uom.errorMessage}
                required
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
          disabled={isButtonDisabled || READ_ONLY}
          className='btn-save'
          startIcon={
            <Box component='img' src={SaveImageIcon} className='w16-icon' />
          }
        >
          {isButtonDisabled
            ? isEditMode
              ? 'Updating...'
              : 'Saving...'
            : isEditMode
              ? 'Update'
              : 'Save'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export default AddSourceDialog
