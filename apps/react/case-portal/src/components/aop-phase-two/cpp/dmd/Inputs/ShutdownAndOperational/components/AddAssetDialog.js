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
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { getRoleName } from 'services/role-service'
import { SaveIcon as SaveImageIcon } from 'assets/images/icons'
import { AssetApiService } from 'components/aop-phase-two/services/cpp/jmd/assetApiService'

const INITIAL_FORM_DATA = {
  cppPlant: '',
  assetName: '',
  assetType: '',
  assetCategory: '',
  utilityDistributed: '',
  distributedSapCode: '',
  utilityGenerated: '',
  generatedUtilityCode: '',
}

const INITIAL_FORM_ERROR = {
  cppPlant: { isError: false, errorMessage: '' },
  assetName: { isError: false, errorMessage: '' },
  assetType: { isError: false, errorMessage: '' },
  utilityDistributed: { isError: false, errorMessage: '' },
  utilityGenerated: { isError: false, errorMessage: '' },
}

const REQUIRED_FIELDS = ['cppPlant', 'assetName', 'assetType']

const FIELD_LABELS = {
  cppPlant: 'CPP Plant',
  assetName: 'Asset Name',
  assetType: 'Asset Type',
  assetCategory: 'Asset Category',
  utilityDistributed: 'Utility Distributed',
  distributedSapCode: 'Distributed SAP Code',
  utilityGenerated: 'Utility Generated',
  generatedUtilityCode: 'Generated SAP Code',
}

const ASSET_TYPE_OPTIONS = [
  { value: 'GT', label: 'GT' },
  { value: 'STG', label: 'STG' },
  { value: 'HRSG', label: 'HRSG' },
  { value: 'Boiler', label: 'Boiler' },
  { value: 'TG', label: 'TG' },
]

const AddAssetDialog = ({
  open,
  onClose,
  onSuccess,
  editRowData = null,
  assetCategory = 'Power',
}) => {
  const keycloak = useSession()
  const isEditMode = !!editRowData

  const [formData, setFormData] = useState(INITIAL_FORM_DATA)
  const [formError, setFormError] = useState(INITIAL_FORM_ERROR)
  const [isButtonDisabled, setIsButtonDisabled] = useState(false)

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
    if (isEditMode && editRowData?.plantFkId) {
      if (!options.find((o) => o.value === editRowData.plantFkId)) {
        options.push({
          value: editRowData.plantFkId,
          label: editRowData.plantName || editRowData.plantFkId,
        })
      }
    }
    return options
  }, [jmdSelectedPlants, isEditMode, editRowData])

  useEffect(() => {
    if (open) {
      if (editRowData) {
        setFormData({
          cppPlant: editRowData.plantFkId || '',
          assetName: editRowData.assetName || '',
          assetType: editRowData.assetType || '',
          assetCategory: editRowData.assetCategory || assetCategory,
          utilityDistributed:
            editRowData.utilityDistributed?.name ||
            editRowData.utilityDistributed ||
            '',
          distributedSapCode:
            editRowData.utilityDistributed?.sapCode ||
            editRowData.distributedSapCode ||
            '',
          utilityGenerated:
            editRowData.utilityGenerated?.name ||
            editRowData.utilityGenerated ||
            '',
          generatedUtilityCode:
            editRowData.utilityGenerated?.sapCode ||
            editRowData.generatedUtilityCode ||
            '',
        })
      } else {
        setFormData({
          ...INITIAL_FORM_DATA,
          assetCategory,
        })
      }
      setFormError(INITIAL_FORM_ERROR)
      setIsButtonDisabled(false)
    }
  }, [open, editRowData, assetCategory])

  const handleFieldChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }))
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

  const handleSave = async () => {
    if (!validateForm()) return

    setIsButtonDisabled(true)
    try {
      const assetData = {
        cppPlant: formData.cppPlant,
        assetName: formData.assetName,
        assetType: formData.assetType,
        assetCategory: formData.assetCategory,
        utilityDistributed: formData.utilityDistributed,
        distributedSapCode: formData.distributedSapCode,
        utilityGenerated: formData.utilityGenerated,
        generatedUtilityCode: formData.generatedUtilityCode,
        aopYear: AOP_YEAR,
      }

      if (isEditMode) {
        await AssetApiService.updateAsset(
          keycloak,
          editRowData.assetFkId || editRowData.id,
          {
            assetName: formData.assetName,
            assetType: formData.assetType,
            utilityDistributed: formData.utilityDistributed,
            distributedSapCode: formData.distributedSapCode,
            utilityGenerated: formData.utilityGenerated,
            generatedUtilityCode: formData.generatedUtilityCode,
          },
        )
      } else {
        await AssetApiService.addAsset(keycloak, assetData)
      }

      onClose()
      if (onSuccess) onSuccess()
    } catch (error) {
      console.error(
        isEditMode ? 'Error updating asset:' : 'Error adding asset:',
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
        <Typography variant='h5' className='dialog-title'>
          {isEditMode ? 'Edit Asset' : 'Add Asset'}
        </Typography>
      </DialogTitle>
      <DialogContent dividers>
        <Box sx={{ mt: 2 }}>
          <Grid container spacing={2}>
            {/* CPP Plant — dropdown */}
            <Grid item xs={6}>
              <TextField
                select
                fullWidth
                label={FIELD_LABELS.cppPlant}
                value={formData.cppPlant}
                onChange={(e) => handleFieldChange('cppPlant', e.target.value)}
                error={formError.cppPlant.isError}
                helperText={formError.cppPlant.errorMessage}
                required
                size='small'
                disabled={isEditMode}
              >
                {cppPlantOptions.map((option) => (
                  <MenuItem key={option.value} value={option.value}>
                    {option.label}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>

            {/* Asset Category — disabled, prefilled */}
            <Grid item xs={6}>
              <TextField
                fullWidth
                label={FIELD_LABELS.assetCategory}
                value={formData.assetCategory}
                size='small'
                disabled
              ></TextField>
            </Grid>

            {/* Asset Name */}
            <Grid item xs={6}>
              <TextField
                fullWidth
                label={FIELD_LABELS.assetName}
                value={formData.assetName}
                onChange={(e) => handleFieldChange('assetName', e.target.value)}
                error={formError.assetName.isError}
                helperText={formError.assetName.errorMessage}
                required
                size='small'
                autoFocus
              />
            </Grid>

            {/* Asset Type */}
            <Grid item xs={6}>
              <TextField
                select
                fullWidth
                label={FIELD_LABELS.assetType}
                value={formData.assetType}
                onChange={(e) => handleFieldChange('assetType', e.target.value)}
                error={formError.assetType.isError}
                helperText={formError.assetType.errorMessage}
                required
                size='small'
              >
                {ASSET_TYPE_OPTIONS.map((option) => (
                  <MenuItem key={option.value} value={option.value}>
                    {option.label}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>

            {/* Utility Distributed */}
            <Grid item xs={6}>
              <TextField
                fullWidth
                label={FIELD_LABELS.utilityDistributed}
                value={formData.utilityDistributed}
                onChange={(e) =>
                  handleFieldChange('utilityDistributed', e.target.value)
                }
                error={formError.utilityDistributed.isError}
                helperText={formError.utilityDistributed.errorMessage}
                size='small'
              />
            </Grid>

            {/* Distributed SAP Code */}
            <Grid item xs={6}>
              <TextField
                fullWidth
                label={FIELD_LABELS.distributedSapCode}
                value={formData.distributedSapCode}
                onChange={(e) =>
                  handleFieldChange('distributedSapCode', e.target.value)
                }
                size='small'
              />
            </Grid>

            {/* Utility Generated */}
            <Grid item xs={6}>
              <TextField
                fullWidth
                label={FIELD_LABELS.utilityGenerated}
                value={formData.utilityGenerated}
                onChange={(e) =>
                  handleFieldChange('utilityGenerated', e.target.value)
                }
                error={formError.utilityGenerated.isError}
                helperText={formError.utilityGenerated.errorMessage}
                size='small'
              />
            </Grid>

            {/* Generated SAP Code */}
            <Grid item xs={6}>
              <TextField
                fullWidth
                label={FIELD_LABELS.generatedUtilityCode}
                value={formData.generatedUtilityCode}
                onChange={(e) =>
                  handleFieldChange('generatedUtilityCode', e.target.value)
                }
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
              : 'Save...'
            : isEditMode
              ? 'Update'
              : 'Save'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export default AddAssetDialog
