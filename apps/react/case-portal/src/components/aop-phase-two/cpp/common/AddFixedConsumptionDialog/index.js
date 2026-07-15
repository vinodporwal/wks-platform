import React, { useState, useEffect, useMemo, useCallback } from 'react'
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
  CircularProgress,
} from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { UtilityPlantApiServiceV2 } from 'components/aop-phase-two/services/cpp/jmd/utilityPlantApiServiceV2'
import { SaveIcon as SaveImageIcon } from 'assets/images/icons'
import { getRoleName } from 'services/role-service'

const INITIAL_FORM_DATA = {
  parentPlantId: '',
  recieverPlantId: '',
  costCenterId: '',
  senderPlantId: '',
  cppUtilityId: '',
  remarks: '',
}

const INITIAL_FORM_ERROR = {
  parentPlantId: { isError: false, errorMessage: '' },
  recieverPlantId: { isError: false, errorMessage: '' },
  costCenterId: { isError: false, errorMessage: '' },
  senderPlantId: { isError: false, errorMessage: '' },
  cppUtilityId: { isError: false, errorMessage: '' },
  remarks: { isError: false, errorMessage: '' },
}

const REQUIRED_FIELDS = ['parentPlantId', 'recieverPlantId', 'costCenterId', 'senderPlantId', 'cppUtilityId']

const FIELD_LABELS = {
  parentPlantId: 'CPP Plant',
  recieverPlantId: 'Reciever Plant',
  costCenterId: 'Cost Center',
  senderPlantId: 'Sender Plant',
  cppUtilityId: 'Utility',
  remarks: 'Remarks',
}

const AddFixedConsumptionDialog = ({
  open,
  onClose,
  onSuccess,
  editRowData = null,
}) => {
  const keycloak = useSession()
  const isEditMode = !!editRowData

  const [formData, setFormData] = useState(INITIAL_FORM_DATA)
  const [formError, setFormError] = useState(INITIAL_FORM_ERROR)
  const [isButtonDisabled, setIsButtonDisabled] = useState(false)

  const [plantsDropdown, setPlantsDropdown] = useState([])
  const [costCentersDropdown, setCostCentersDropdown] = useState([])
  const [allUtilities, setAllUtilities] = useState([])
  const [loadingPlants, setLoadingPlants] = useState(false)
  const [loadingCostCenters, setLoadingCostCenters] = useState(false)
  const [loadingUtilities, setLoadingUtilities] = useState(false)

  // Filtered utilities based on selected sender plant
  const utilitiesDropdown = useMemo(() => {
    if (!formData.senderPlantId) return []
    return allUtilities.filter((u) => u.plantFkId === formData.senderPlantId)
  }, [allUtilities, formData.senderPlantId])

  const { jmdSelectedPlants, year, oldYear, isReleased ,plantObject,siteObject } = useSelector(
    (state) => state.dataGridStore,
  )

  const selectedPlant = siteObject?.name?.toLowerCase()=='jmd' ? jmdSelectedPlants : [plantObject];

  const IS_OLD_YEAR = oldYear?.oldYear
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const AOP_YEAR = year?.selectedYear

  const mainPlantOptions = useMemo(() => {
    const options = (selectedPlant || []).map((plant) => ({
      value: plant.id,
      label: plant.name || plant.id,
    }))
    if (isEditMode && editRowData?.parentPlantId) {
      if (!options.find((o) => o.value === editRowData.parentPlantId)) {
        options.push({
          value: editRowData.parentPlantId,
          label: editRowData.parentPlant || editRowData.parentPlantId,
        })
      }
    }
    return options
  }, [selectedPlant, isEditMode, editRowData])

  // Reset / pre-populate form whenever the dialog opens
  useEffect(() => {
    if (open) {
      if (editRowData) {
        setFormData({
          parentPlantId: editRowData.parentPlantId || '',
          recieverPlantId: editRowData.recieverPlantId || '',
          costCenterId: editRowData.costCenterId || '',
          senderPlantId: editRowData.senderPlantId || '',
          cppUtilityId: editRowData.cppUtilityId || '',
          remarks: editRowData.remarks || '',
        })
      } else {
        setFormData(INITIAL_FORM_DATA)
        setPlantsDropdown([])
        setCostCentersDropdown([])
        setAllUtilities([])
      }
      setFormError(INITIAL_FORM_ERROR)
      setIsButtonDisabled(false)
    }
  }, [open, editRowData])

  // Fetch plants and cost centers when CPP plant (parentPlantId) changes
  const fetchPlantsAndCostCenters = useCallback(
    async (parentPlantId) => {
      if (!parentPlantId) {
        setPlantsDropdown([])
        setCostCentersDropdown([])
        return
      }

      setLoadingPlants(true)
      setLoadingCostCenters(true)

      try {
        const [plantsResponse, costCentersResponse] = await Promise.all([
          UtilityPlantApiServiceV2.getSRMappingPlants(keycloak, [parentPlantId]),
          UtilityPlantApiServiceV2.getSRMappingCostCenters(keycloak, [
            parentPlantId,
          ]),
        ])

        const plantsData = plantsResponse?.data || []
        const plantsOptions = plantsData.map((plant) => ({
          value: plant.plantId,
          label: plant.plantName || plant.plantCode || 'Unknown Plant',
          code: plant.plantCode || '',
          sourceName: plant.sourceName || '',
        }))
        setPlantsDropdown(plantsOptions)

        const costCentersData = costCentersResponse?.data || []
        const costCentersOptions = costCentersData.map((cc) => ({
          value: cc.id,
          label: cc.costCenterName || '',
          code: cc.costCenterCode || '',
          cppPlantFkId: cc.cppPlantFkId || '',
        }))
        setCostCentersDropdown(costCentersOptions)
      } catch (error) {
        console.error('Error fetching plants and cost centers:', error)
        setPlantsDropdown([])
        setCostCentersDropdown([])
      } finally {
        setLoadingPlants(false)
        setLoadingCostCenters(false)
      }
    },
    [keycloak],
  )

  // Fetch utilities when CPP plant (parentPlantId) changes
  // API uses parentPlantId as sourceName to get all utilities for all plants under that source
  const fetchUtilities = useCallback(
    async (parentPlantId) => {
      if (!parentPlantId) {
        setAllUtilities([])
        return
      }

      setLoadingUtilities(true)
      try {
        const response = await UtilityPlantApiServiceV2.getNormParameters(
          keycloak,
          parentPlantId,
        )
        const data = response?.data || []
        const utilityOptions = data
          .filter((np) => np.normTypeFkId === 1 || np.normTypeFkId === 2)
          .map((np) => ({
            value: np.id || np.displayName || np.name,
            label: np.displayName || np.name,
            sapMaterialCode: np.sapMaterialCode || '',
            uom: np.uom || '',
            plantFkId: np.plantFkId || '',
          }))
        setAllUtilities(utilityOptions)
      } catch (error) {
        console.error('Error fetching utilities:', error)
        setAllUtilities([])
      } finally {
        setLoadingUtilities(false)
      }
    },
    [keycloak],
  )

  // Trigger fetch when CPP plant (parentPlantId) changes
  useEffect(() => {
    if (open && formData.parentPlantId) {
      fetchPlantsAndCostCenters(formData.parentPlantId)
      fetchUtilities(formData.parentPlantId)
    } else {
      setAllUtilities([])
    }
  }, [open, formData.parentPlantId, fetchPlantsAndCostCenters, fetchUtilities])

  const handleFieldChange = (field, value) => {
    setFormData((prev) => {
      const updates = { [field]: value }

      // Reset dependent fields when parent plant changes
      if (field === 'parentPlantId') {
        updates.recieverPlantId = ''
        updates.costCenterId = ''
        updates.senderPlantId = ''
        updates.cppUtilityId = ''
      } else if (field === 'senderPlantId') {
        updates.cppUtilityId = ''
      }

      return { ...prev, ...updates }
    })

    if (formError[field]?.isError) {
      setFormError((prev) => ({
        ...prev,
        [field]: { isError: false, errorMessage: '' },
      }))
    }
  }

  console.log('formData',formData)
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
      const selectedReceiverPlant = plantsDropdown.find(
        (p) => p.value === formData.recieverPlantId,
      )
      const selectedSenderPlant = plantsDropdown.find(
        (p) => p.value === formData.senderPlantId,
      )
      const selectedCostCenter = costCentersDropdown.find(
        (cc) => cc.value === formData.costCenterId,
      )
      const selectedUtility = utilitiesDropdown.find(
        (u) => u.value === formData.cppUtilityId,
      )
      const selectedMainPlant = mainPlantOptions.find(
        (p) => p.value === formData.parentPlantId,
      )

      const rowData = {
        parentPlantId: formData.parentPlantId,
        parentPlant: selectedMainPlant?.label || '',
        recieverPlantId: formData.recieverPlantId,
        recieverPlant: selectedReceiverPlant?.label || '',
        recieverPlantCode: selectedReceiverPlant?.code || '',
        costCenterId: formData.costCenterId,
        costCenter: selectedCostCenter?.label || '',
        costCenterCode: selectedCostCenter?.code || '',
        senderPlantId: formData.senderPlantId,
        senderPlant: selectedSenderPlant?.label || '',
        senderPlantCode: selectedSenderPlant?.code || '',
        cppUtilityId: formData.cppUtilityId,
        cppUtility: selectedUtility?.label || '',
        uom: selectedUtility?.uom || '',
        remarks: formData.remarks || '',
        aopYear: AOP_YEAR,
      }

      if (isEditMode && editRowData?.id) {
        rowData.id = editRowData.id
        await UtilityPlantApiServiceV2.updateFixedConsumptionRow(
          keycloak,
          rowData,
          AOP_YEAR,
        )
      } else {
        await UtilityPlantApiServiceV2.addFixedConsumptionRow(
          keycloak,
          rowData,
          AOP_YEAR,
        )
      }

      onClose()
      if (onSuccess) onSuccess()
    } catch (error) {
      console.error(
        isEditMode ? 'Error updating fixed consumption row:' : 'Error adding fixed consumption row:',
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
          {isEditMode ? 'Edit Fixed Consumption' : 'Add Fixed Consumption'}
        </Typography>
      </DialogTitle>
      <DialogContent dividers>
        <Box sx={{ mt: 2 }}>
          <Grid container spacing={2}>
            {/* CPP Plant — dropdown */}
            <Grid item xs={12}>
              <TextField
                select
                fullWidth
                label={FIELD_LABELS.parentPlantId}
                value={formData.parentPlantId}
                onChange={(e) => handleFieldChange('parentPlantId', e.target.value)}
                error={formError.parentPlantId.isError}
                helperText={formError.parentPlantId.errorMessage}
                required
                size='small'
                disabled={isEditMode}
              >
                {mainPlantOptions.map((option) => (
                  <MenuItem key={option.value} value={option.value}>
                    {option.label}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>

            {/* Receiver Plant — dropdown (from fetched plant list) */}
            <Grid item xs={12}>
              <TextField
                select
                fullWidth
                label={FIELD_LABELS.recieverPlantId}
                value={formData.recieverPlantId}
                onChange={(e) => handleFieldChange('recieverPlantId', e.target.value)}
                error={formError.recieverPlantId.isError}
                helperText={formError.recieverPlantId.errorMessage}
                required
                size='small'
                disabled={!formData.parentPlantId || loadingPlants}
              >
                {loadingPlants ? (
                  <MenuItem disabled>
                    <CircularProgress size={20} /> Loading...
                  </MenuItem>
                ) : (
                  plantsDropdown.map((option) => (
                    <MenuItem key={option.value} value={option.value}>
                      {option.label}
                    </MenuItem>
                  ))
                )}
              </TextField>
            </Grid>

            {/* Cost Center — dropdown (from fetched cost centers) */}
            <Grid item xs={12}>
              <TextField
                select
                fullWidth
                label={FIELD_LABELS.costCenterId}
                value={formData.costCenterId}
                onChange={(e) =>
                  handleFieldChange('costCenterId', e.target.value)
                }
                error={formError.costCenterId.isError}
                helperText={formError.costCenterId.errorMessage}
                required
                size='small'
                disabled={!formData.parentPlantId || loadingCostCenters}
              >
                {loadingCostCenters ? (
                  <MenuItem disabled>
                    <CircularProgress size={20} /> Loading...
                  </MenuItem>
                ) : (
                  costCentersDropdown.map((option) => (
                    <MenuItem key={option.value} value={option.value}>
                      {option.label}
                    </MenuItem>
                  ))
                )}
              </TextField>
            </Grid>

            {/* Sender Plant — dropdown (same options as receiver plant) */}
            <Grid item xs={12}>
              <TextField
                select
                fullWidth
                label={FIELD_LABELS.senderPlantId}
                value={formData.senderPlantId}
                onChange={(e) => handleFieldChange('senderPlantId', e.target.value)}
                error={formError.senderPlantId.isError}
                helperText={formError.senderPlantId.errorMessage}
                required
                size='small'
                disabled={!formData.parentPlantId || loadingPlants}
              >
                {loadingPlants ? (
                  <MenuItem disabled>
                    <CircularProgress size={20} /> Loading...
                  </MenuItem>
                ) : (
                  plantsDropdown.map((option) => (
                    <MenuItem key={option.value} value={option.value}>
                      {option.label}
                    </MenuItem>
                  ))
                )}
              </TextField>
            </Grid>

            {/* Utility — dropdown (filtered by sender plant) */}
            <Grid item xs={12}>
              <TextField
                select
                fullWidth
                label={FIELD_LABELS.cppUtilityId}
                value={formData.cppUtilityId}
                onChange={(e) =>
                  handleFieldChange('cppUtilityId', e.target.value)
                }
                error={formError.cppUtilityId.isError}
                helperText={formError.cppUtilityId.errorMessage}
                required
                size='small'
                disabled={!formData.senderPlantId || loadingUtilities}
              >
                {loadingUtilities ? (
                  <MenuItem disabled>
                    <CircularProgress size={20} /> Loading...
                  </MenuItem>
                ) : (
                  utilitiesDropdown.map((option) => (
                    <MenuItem key={option.value} value={option.value}>
                      {option.label}
                    </MenuItem>
                  ))
                )}
              </TextField>
            </Grid>

            {/* Remarks — text field */}
            <Grid item xs={12}>
              <TextField
                fullWidth
                label={FIELD_LABELS.remarks}
                value={formData.remarks}
                onChange={(e) => handleFieldChange('remarks', e.target.value)}
                error={formError.remarks.isError}
                helperText={formError.remarks.errorMessage}
                size='small'
                multiline
                rows={2}
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

export default AddFixedConsumptionDialog