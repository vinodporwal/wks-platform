import React, { useState, useEffect, useMemo } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Autocomplete,
  Button,
  Box,
  Grid,
  Typography,
} from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { getRoleName } from 'services/role-service'
import { InputApiService } from 'components/aop-phase-two/services/cpp/jmd/inputApiService'
import { UtilityPlantApiServiceV2 } from 'components/aop-phase-two/services/cpp/jmd/utilityPlantApiServiceV2'
import { SaveIcon as SaveImageIcon } from 'assets/images/icons'

const INITIAL_FORM_DATA = {
  cppPlant: '',
  procurementPlant: '',
  utilityPlant: '',
  utilityCostCenter: '',
  materialCostCenter: '',
  name: '',
  displayName: '',
  sapCode: '',
  uom: 'MW',
}

const INITIAL_FORM_ERROR = {
  cppPlant: { isError: false, errorMessage: '' },
  procurementPlant: { isError: false, errorMessage: '' },
  utilityPlant: { isError: false, errorMessage: '' },
  utilityCostCenter: { isError: false, errorMessage: '' },
  materialCostCenter: { isError: false, errorMessage: '' },
  name: { isError: false, errorMessage: '' },
  displayName: { isError: false, errorMessage: '' },
  sapCode: { isError: false, errorMessage: '' },
  uom: { isError: false, errorMessage: '' },
}

// Fields that are required for form submission
const REQUIRED_FIELDS = [
  'cppPlant',
  'procurementPlant',
  'utilityPlant',
  'name',
  'uom',
]

// Human-readable labels for each field
const FIELD_LABELS = {
  cppPlant: 'CPP Plant',
  procurementPlant: 'Procurement Plant',
  utilityPlant: 'Utility Plant',
  utilityCostCenter: 'Utility Cost Center',
  materialCostCenter: 'Material Cost Center',
  name: 'Name',
  displayName: 'Display Name',
  sapCode: 'SAP Code',
  uom: 'UOM',
}

// UUIDs from different sources (Redux plant list vs SQL-backed APIs) can
// differ in casing — compare ids case-insensitively everywhere.
const sameId = (a, b) =>
  a != null && b != null && String(a).toLowerCase() === String(b).toLowerCase()

const AddSourceDialog = ({ open, onClose, onSuccess, editRowData = null }) => {
  const keycloak = useSession()
  const isEditMode = !!editRowData

  const [formData, setFormData] = useState(INITIAL_FORM_DATA)
  const [formError, setFormError] = useState(INITIAL_FORM_ERROR)
  const [isButtonDisabled, setIsButtonDisabled] = useState(false)
  const [procurementPlantOptions, setProcurementPlantOptions] = useState([])
  const [utilityPlantOptions, setUtilityPlantOptions] = useState([])
  const [costCenterOptions, setCostCenterOptions] = useState([])

  // Read plant list, year, and role flags from the global store
  const { jmdSelectedPlants, year, oldYear, isReleased } = useSelector(
    (state) => state.dataGridStore,
  )

  const IS_OLD_YEAR = oldYear?.oldYear
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const AOP_YEAR = year?.selectedYear

  const cppPlantOptions = useMemo(() => {
    const options = (jmdSelectedPlants || []).map((plant) => ({
      value: plant.id,
      label: plant.name || plant.id,
    }))
    if (isEditMode && editRowData?.cppPlantFkId) {
      if (!options.find((o) => sameId(o.value, editRowData.cppPlantFkId))) {
        options.push({
          value: editRowData.cppPlantFkId,
          label: editRowData.plantName || editRowData.cppPlantFkId,
        })
      }
    }
    return options
  }, [jmdSelectedPlants, isEditMode, editRowData])

  // Fetch procurement plants whenever cppPlant changes
  useEffect(() => {
    const fetchProcurementPlants = async () => {
      if (!formData.cppPlant) {
        setProcurementPlantOptions([])
        return
      }
      try {
        const response = await InputApiService.getImportProcurementPlants(
          keycloak,
          formData.cppPlant,
        )
        if (response && response.data) {
          const apiOptions = response.data.map((plant) => ({
            value: plant.procurementPlantId,
            label: plant.name || plant.procurementPlantId,
          }))

          // In edit mode, ensure the existing selection is present
          if (isEditMode && editRowData?.importPlantFkId) {
            if (
              !apiOptions.find((o) =>
                sameId(o.value, editRowData.importPlantFkId),
              )
            ) {
              apiOptions.push({
                value: editRowData.importPlantFkId,
                label:
                  editRowData.procurementPlant || editRowData.importPlantFkId,
              })
            }
          }
          setProcurementPlantOptions(apiOptions)
        }
      } catch (error) {
        console.error('Error fetching procurement plants:', error)
      }
    }

    fetchProcurementPlants()
  }, [formData.cppPlant, keycloak, isEditMode, editRowData])

  // Fetch utility plants whenever cppPlant changes
  useEffect(() => {
    const fetchUtilityPlants = async () => {
      if (!formData.cppPlant) {
        setUtilityPlantOptions([])
        return
      }
      try {
        const response = await InputApiService.getImportUtilityPlants(
          keycloak,
          formData.cppPlant,
        )
        if (response && response.data) {
          const apiOptions = response.data.map((plant) => ({
            value: plant.id,
            label: plant.name || plant.id,
          }))

          // In edit mode, ensure the existing selection is present
          if (isEditMode && editRowData?.utilityPlantId) {
            if (
              !apiOptions.find((o) => sameId(o.value, editRowData.utilityPlantId))
            ) {
              apiOptions.push({
                value: editRowData.utilityPlantId,
                label: editRowData.utilityPlantName || editRowData.utilityPlantId,
              })
            }
          }
          // Default-select the utility plant — each CPP plant has exactly one
          // Utility plant with a single Power_dis norm. The functional update
          // keeps any existing selection (e.g. edit-mode prefill) untouched.
          if (apiOptions.length > 0) {
            setFormData((prev) => ({
              ...prev,
              utilityPlant: prev.utilityPlant || apiOptions[0].value,
            }))
          }
          setUtilityPlantOptions(apiOptions)
        }
      } catch (error) {
        console.error('Error fetching utility plants:', error)
      }
    }

    fetchUtilityPlants()
  }, [formData.cppPlant, keycloak, isEditMode, editRowData])

  // Fetch cost centers (CPPCostCentersMaster, filtered by CPP plant) — same
  // source as the Sender/Receiver Mapping screen
  useEffect(() => {
    const fetchCostCenters = async () => {
      if (!formData.cppPlant) {
        setCostCenterOptions([])
        return
      }
      try {
        const response = await UtilityPlantApiServiceV2.getSRMappingCostCenters(
          keycloak,
          [formData.cppPlant],
        )
        if (response && response.data) {
          const apiOptions = response.data.map((cc) => ({
            value: cc.id,
            label: cc.costCenterName || cc.id,
          }))

          // In edit mode, ensure existing selections are present
          if (isEditMode) {
            const extras = []
            if (
              editRowData?.utilityCostCenterId &&
              !apiOptions.find((o) =>
                sameId(o.value, editRowData.utilityCostCenterId),
              )
            ) {
              extras.push({
                value: editRowData.utilityCostCenterId,
                label:
                  editRowData.utilityCostCenterName ||
                  editRowData.utilityCostCenterId,
              })
            }
            if (
              editRowData?.materialCostCenterId &&
              !apiOptions.find((o) =>
                sameId(o.value, editRowData.materialCostCenterId),
              )
            ) {
              extras.push({
                value: editRowData.materialCostCenterId,
                label:
                  editRowData.materialCostCenterName ||
                  editRowData.materialCostCenterId,
              })
            }
            apiOptions.push(...extras)
          }
          setCostCenterOptions(apiOptions)
        }
      } catch (error) {
        console.error('Error fetching cost centers:', error)
      }
    }

    fetchCostCenters()
  }, [formData.cppPlant, keycloak, isEditMode, editRowData])

  // Reset / pre-populate form whenever the dialog opens
  useEffect(() => {
    if (open) {
      if (editRowData) {
        // Edit mode — pre-fill with the row's existing source fields
        setFormData({
          cppPlant: editRowData.cppPlantFkId || '',
          procurementPlant: editRowData.importPlantFkId || '',
          utilityPlant: editRowData.utilityPlantId || '',
          utilityCostCenter: editRowData.utilityCostCenterId || '',
          materialCostCenter: editRowData.materialCostCenterId || '',
          name: editRowData.materialName || '',
          displayName: editRowData.materialDisplayName || '',
          sapCode: editRowData.materialCode || '',
          uom: editRowData.uom || '',
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
      const sourceData = {
        cppPlant: formData.cppPlant,
        procurementPlant: formData.procurementPlant,
        utilityPlant: formData.utilityPlant,
        utilityCostCenter: formData.utilityCostCenter || null,
        materialCostCenter: formData.materialCostCenter || null,
        name: formData.name,
        displayName: formData.displayName,
        sapCode: formData.sapCode,
        uom: formData.uom,
      }

      if (isEditMode) {
        // Update existing source
        await InputApiService.updateSource(
          keycloak,
          editRowData.normParameterFkId,
          {
            procurementPlant: formData.procurementPlant,
            utilityPlant: formData.utilityPlant,
            utilityCostCenter: formData.utilityCostCenter || null,
            materialCostCenter: formData.materialCostCenter || null,
            name: formData.name,
            displayName: formData.displayName,
            sapCode: formData.sapCode,
            uom: formData.uom,
          },
        )
      } else {
        // Add new source
        await InputApiService.addSource(keycloak, {
          ...sourceData,
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
            {/* CPP Plant — autocomplete */}
            <Grid item xs={6}>
              <Autocomplete
                fullWidth
                size='small'
                options={cppPlantOptions}
                getOptionLabel={(option) => option.label}
                value={
                  cppPlantOptions.find((o) =>
                    sameId(o.value, formData.cppPlant),
                  ) || null
                }
                onChange={(e, newValue) =>
                  handleFieldChange('cppPlant', newValue?.value || '')
                }
                disabled={isEditMode}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    label={FIELD_LABELS.cppPlant}
                    required
                    error={formError.cppPlant.isError}
                    helperText={formError.cppPlant.errorMessage}
                  />
                )}
              />
            </Grid>

            {/* Procurement Plant — autocomplete */}
            <Grid item xs={6}>
              <Autocomplete
                fullWidth
                size='small'
                options={procurementPlantOptions}
                getOptionLabel={(option) => option.label}
                value={
                  procurementPlantOptions.find((o) =>
                    sameId(o.value, formData.procurementPlant),
                  ) || null
                }
                onChange={(e, newValue) =>
                  handleFieldChange('procurementPlant', newValue?.value || '')
                }
                disabled={isEditMode}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    label={FIELD_LABELS.procurementPlant}
                    required
                    error={formError.procurementPlant.isError}
                    helperText={formError.procurementPlant.errorMessage}
                  />
                )}
              />
            </Grid>

            {/* Name */}
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

            {/* Utility Plant — autocomplete */}
            <Grid item xs={6}>
              <Autocomplete
                fullWidth
                size='small'
                options={utilityPlantOptions}
                getOptionLabel={(option) => option.label}
                value={
                  utilityPlantOptions.find((o) =>
                    sameId(o.value, formData.utilityPlant),
                  ) || null
                }
                onChange={(e, newValue) =>
                  handleFieldChange('utilityPlant', newValue?.value || '')
                }
                renderInput={(params) => (
                  <TextField
                    {...params}
                    label={FIELD_LABELS.utilityPlant}
                    required
                    error={formError.utilityPlant.isError}
                    helperText={formError.utilityPlant.errorMessage}
                  />
                )}
              />
            </Grid>

            {/* Utility Cost Center — autocomplete (optional) */}
            <Grid item xs={6}>
              <Autocomplete
                fullWidth
                size='small'
                options={costCenterOptions}
                getOptionLabel={(option) => option.label}
                value={
                  costCenterOptions.find((o) =>
                    sameId(o.value, formData.utilityCostCenter),
                  ) || null
                }
                onChange={(e, newValue) =>
                  handleFieldChange('utilityCostCenter', newValue?.value || '')
                }
                renderInput={(params) => (
                  <TextField
                    {...params}
                    label={FIELD_LABELS.utilityCostCenter}
                    error={formError.utilityCostCenter.isError}
                    helperText={formError.utilityCostCenter.errorMessage}
                  />
                )}
              />
            </Grid>

            {/* Material Cost Center — autocomplete (optional) */}
            <Grid item xs={6}>
              <Autocomplete
                fullWidth
                size='small'
                options={costCenterOptions}
                getOptionLabel={(option) => option.label}
                value={
                  costCenterOptions.find((o) =>
                    sameId(o.value, formData.materialCostCenter),
                  ) || null
                }
                onChange={(e, newValue) =>
                  handleFieldChange('materialCostCenter', newValue?.value || '')
                }
                renderInput={(params) => (
                  <TextField
                    {...params}
                    label={FIELD_LABELS.materialCostCenter}
                    error={formError.materialCostCenter.isError}
                    helperText={formError.materialCostCenter.errorMessage}
                  />
                )}
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
