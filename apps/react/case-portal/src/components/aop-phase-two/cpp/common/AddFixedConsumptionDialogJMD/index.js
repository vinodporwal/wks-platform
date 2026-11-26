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
  Typography,
  Autocomplete,
} from '@mui/material'
import { useSelector } from 'react-redux'
import { useSession } from 'SessionStoreContext'
import { UtilityPlantApiServiceV2 } from 'components/aop-phase-two/services/cpp/jmd/utilityPlantApiServiceV2'
import { SaveIcon as SaveImageIcon } from 'assets/images/icons'
import { getRoleName } from 'services/role-service'

const INITIAL_FORM_DATA = {
  parentPlantId: '',
  senderPlantId: '',
  recieverPlantId: '',
  costCenterId: '',
  receiverCostCenterId: '',
  senderNormParameterId: '',
  remarks: '',
}

const INITIAL_FORM_ERROR = {
  parentPlantId: { isError: false, errorMessage: '' },
  senderPlantId: { isError: false, errorMessage: '' },
  recieverPlantId: { isError: false, errorMessage: '' },
  costCenterId: { isError: false, errorMessage: '' },
  receiverCostCenterId: { isError: false, errorMessage: '' },
  senderNormParameterId: { isError: false, errorMessage: '' },
  remarks: { isError: false, errorMessage: '' },
}

const REQUIRED_FIELDS = [
  'parentPlantId',
  'senderPlantId',
  'recieverPlantId',
  'costCenterId',
  'receiverCostCenterId',
  'senderNormParameterId',
]

const FIELD_LABELS = {
  parentPlantId: 'CPP Plant',
  senderPlantId: 'Sender Plant',
  recieverPlantId: 'Receiver Plant',
  costCenterId: 'Sender Cost Center',
  receiverCostCenterId: 'Receiver Cost Center',
  senderNormParameterId: 'Sender Material',
  remarks: 'Remarks',
}

const mapCostCenters = (data) =>
  (data || []).map((cc) => ({
    value: cc.id,
    label: cc.costCenterName || '',
    code: cc.costCenterCode || '',
    cppPlantFkId: cc.cppPlantFkId || '',
  }))

const AddFixedConsumptionDialogJMD = ({
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
  const [submitError, setSubmitError] = useState('')

  const [plantsDropdown, setPlantsDropdown] = useState([])
  const [parentCostCentersDropdown, setParentCostCentersDropdown] = useState([])
  const [senderCostCentersDropdown, setSenderCostCentersDropdown] = useState([])
  const [receiverCostCentersDropdown, setReceiverCostCentersDropdown] =
    useState([])
  const [allUtilities, setAllUtilities] = useState([])
  const [loadingPlants, setLoadingPlants] = useState(false)
  const [loadingParentCostCenters, setLoadingParentCostCenters] =
    useState(false)
  const [loadingSenderCostCenters, setLoadingSenderCostCenters] =
    useState(false)
  const [loadingReceiverCostCenters, setLoadingReceiverCostCenters] =
    useState(false)
  const [loadingUtilities, setLoadingUtilities] = useState(false)

  const {
    jmdSelectedPlants,
    year,
    oldYear,
    isReleased,
    plantObject,
    siteObject,
  } = useSelector((state) => state.dataGridStore)

  const selectedPlant =
    siteObject?.name?.toLowerCase() == 'jmd' ? jmdSelectedPlants : [plantObject]

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

  // Plant options with edit-mode fallback for sender/receiver plants
  const senderPlantOptions = useMemo(() => {
    const options = [...plantsDropdown]
    if (
      isEditMode &&
      editRowData?.senderPlantId &&
      !options.find((o) => o.value === editRowData.senderPlantId)
    ) {
      options.push({
        value: editRowData.senderPlantId,
        label: editRowData.senderPlant || '',
        code: editRowData.senderPlantCode || '',
      })
    }
    return options
  }, [plantsDropdown, isEditMode, editRowData])

  const receiverPlantOptions = useMemo(() => {
    const options = [...plantsDropdown]
    const editReceiverId =
      editRowData?.recieverPlantId || editRowData?.plantFkId
    if (
      isEditMode &&
      editReceiverId &&
      !options.find((o) => o.value === editReceiverId)
    ) {
      options.push({
        value: editReceiverId,
        label: editRowData.plant || editRowData.recieverPlant || '',
        code: editRowData.plantId || '',
      })
    }
    return options
  }, [plantsDropdown, isEditMode, editRowData])

  const mergeCostCenterOptions = useCallback(
    (baseOptions, extraOptions, editRowData, fallbackLabelFields) => {
      const map = new Map()
      ;(baseOptions || []).forEach((cc) => map.set(cc.value, cc))
      ;(extraOptions || []).forEach((cc) => map.set(cc.value, cc))
      const options = Array.from(map.values())
      const editValue = editRowData?.[fallbackLabelFields.idField]
      if (
        isEditMode &&
        editValue &&
        !options.find((o) => o.value === editValue)
      ) {
        options.push({
          value: editValue,
          label: editRowData?.[fallbackLabelFields.labelField] || '',
          code: editRowData?.[fallbackLabelFields.codeField] || '',
        })
      }
      return options
    },
    [isEditMode],
  )

  // Sender cost center options: CPP plant cost centers + sender plant cost centers
  const senderCostCenterOptions = useMemo(
    () =>
      mergeCostCenterOptions(
        parentCostCentersDropdown,
        senderCostCentersDropdown,
        editRowData,
        {
          idField: 'costCenterId',
          labelField: 'costCenter',
          codeField: 'costCenterCode',
        },
      ),
    [
      parentCostCentersDropdown,
      senderCostCentersDropdown,
      mergeCostCenterOptions,
      editRowData,
    ],
  )

  // Receiver cost center options: CPP plant cost centers + receiver plant cost centers
  const receiverCostCenterOptions = useMemo(
    () =>
      mergeCostCenterOptions(
        parentCostCentersDropdown,
        receiverCostCentersDropdown,
        editRowData,
        {
          idField: 'receiverCostCenterId',
          labelField: 'receiverCostCenter',
          codeField: 'receiverCostCenterCode',
        },
      ),
    [
      parentCostCentersDropdown,
      receiverCostCentersDropdown,
      mergeCostCenterOptions,
      editRowData,
    ],
  )

  // Sender Material options: norm parameters belonging to the sender plant
  const senderMaterialDropdown = useMemo(() => {
    if (!formData.senderPlantId) return []
    const options = allUtilities.filter(
      (u) => u.plantFkId === formData.senderPlantId && u.normTypeFkId === 2,
    )
    if (
      isEditMode &&
      editRowData?.senderNormParameterId &&
      !options.find((o) => o.value === editRowData.senderNormParameterId)
    ) {
      options.push({
        value: editRowData.senderNormParameterId,
        label: editRowData.senderMaterial || '',
        sapMaterialCode: editRowData.senderMaterialCode || '',
        uom: editRowData.senderMaterialUom || '',
      })
    }
    return options
  }, [allUtilities, formData.senderPlantId, isEditMode, editRowData])

  // Reset / pre-populate form whenever the dialog opens
  useEffect(() => {
    if (open) {
      if (editRowData) {
        setFormData({
          parentPlantId: editRowData.parentPlantId || '',
          senderPlantId: editRowData.senderPlantId || '',
          recieverPlantId:
            editRowData.recieverPlantId || editRowData.plantFkId || '',
          costCenterId:
            editRowData.costCenterId || editRowData.cppCostCenterFkId || '',
          receiverCostCenterId:
            editRowData.receiverCostCenterId ||
            editRowData.receiverCostCenterFkId ||
            '',
          senderNormParameterId:
            editRowData.senderNormParameterId ||
            editRowData.senderNormParameterFkId ||
            '',
          remarks: editRowData.remarks || '',
        })
      } else {
        setFormData(INITIAL_FORM_DATA)
        setPlantsDropdown([])
        setParentCostCentersDropdown([])
        setSenderCostCentersDropdown([])
        setReceiverCostCentersDropdown([])
        setAllUtilities([])
      }
      setFormError(INITIAL_FORM_ERROR)
      setIsButtonDisabled(false)
      setSubmitError('')
    }
  }, [open, editRowData])

  const fetchPlants = useCallback(
    async (parentPlantId) => {
      if (!parentPlantId) {
        setPlantsDropdown([])
        return
      }

      setLoadingPlants(true)
      try {
        const plantsResponse =
          await UtilityPlantApiServiceV2.getSRMappingPlants(keycloak, [
            parentPlantId,
          ])
        const plantsData = plantsResponse?.data || []
        const plantsOptions = plantsData.map((plant) => ({
          value: plant.plantId,
          label: plant.plantName || plant.plantCode || 'Unknown Plant',
          code: plant.plantCode || '',
        }))
        setPlantsDropdown(plantsOptions)
      } catch (error) {
        console.error('Error fetching plants:', error)
        setPlantsDropdown([])
      } finally {
        setLoadingPlants(false)
      }
    },
    [keycloak],
  )

  const fetchCostCenters = useCallback(
    async (plantId, setter, setLoading) => {
      if (!plantId) {
        setter([])
        return
      }
      setLoading(true)
      try {
        const response = await UtilityPlantApiServiceV2.getSRMappingCostCenters(
          keycloak,
          [plantId],
        )
        setter(mapCostCenters(response?.data))
      } catch (error) {
        console.error('Error fetching cost centers:', error)
        setter([])
      } finally {
        setLoading(false)
      }
    },
    [keycloak],
  )

  // Fetch norm parameters (sender material source) when CPP plant changes.
  // API uses parentPlantId as sourceName to get all norm parameters for all
  // plants under that source; sender material is filtered by sender plant.
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
        const utilityOptions = data.map((np) => ({
          value: np.id || np.displayName || np.name,
          label: np.displayName || np.name,
          sapMaterialCode: np.sapMaterialCode || '',
          uom: np.uom || '',
          plantFkId: np.plantFkId || '',
          normTypeFkId: np.normTypeFkId || '',
        }))
        setAllUtilities(utilityOptions)
      } catch (error) {
        console.error('Error fetching norm parameters:', error)
        setAllUtilities([])
      } finally {
        setLoadingUtilities(false)
      }
    },
    [keycloak],
  )

  // Trigger fetches when CPP plant (parentPlantId) changes
  useEffect(() => {
    if (open && formData.parentPlantId) {
      fetchPlants(formData.parentPlantId)
      fetchCostCenters(
        formData.parentPlantId,
        setParentCostCentersDropdown,
        setLoadingParentCostCenters,
      )
      fetchUtilities(formData.parentPlantId)
    } else {
      setPlantsDropdown([])
      setParentCostCentersDropdown([])
      setAllUtilities([])
    }
  }, [
    open,
    formData.parentPlantId,
    fetchPlants,
    fetchCostCenters,
    fetchUtilities,
  ])

  // Trigger sender-side fetches when sender plant changes
  useEffect(() => {
    if (open && formData.senderPlantId) {
      fetchCostCenters(
        formData.senderPlantId,
        setSenderCostCentersDropdown,
        setLoadingSenderCostCenters,
      )
    } else {
      setSenderCostCentersDropdown([])
    }
  }, [open, formData.senderPlantId, fetchCostCenters])

  // Trigger receiver-side fetches when receiver plant changes
  useEffect(() => {
    if (open && formData.recieverPlantId) {
      fetchCostCenters(
        formData.recieverPlantId,
        setReceiverCostCentersDropdown,
        setLoadingReceiverCostCenters,
      )
    } else {
      setReceiverCostCentersDropdown([])
    }
  }, [open, formData.recieverPlantId, fetchCostCenters])

  const handleFieldChange = (field, value) => {
    setFormData((prev) => {
      const updates = { [field]: value }

      // Reset dependent fields when parent plant changes
      if (field === 'parentPlantId') {
        updates.senderPlantId = ''
        updates.recieverPlantId = ''
        updates.costCenterId = ''
        updates.receiverCostCenterId = ''
        updates.senderNormParameterId = ''
      } else if (field === 'senderPlantId') {
        updates.costCenterId = ''
        updates.senderNormParameterId = ''
      } else if (field === 'recieverPlantId') {
        updates.receiverCostCenterId = ''
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
      const selectedSenderPlant = plantsDropdown.find(
        (p) => p.value === formData.senderPlantId,
      )
      const selectedReceiverPlant = plantsDropdown.find(
        (p) => p.value === formData.recieverPlantId,
      )
      const selectedSenderCostCenter = senderCostCenterOptions.find(
        (cc) => cc.value === formData.costCenterId,
      )
      const selectedReceiverCostCenter = receiverCostCenterOptions.find(
        (cc) => cc.value === formData.receiverCostCenterId,
      )
      const selectedSenderMaterial = senderMaterialDropdown.find(
        (u) => u.value === formData.senderNormParameterId,
      )
      const selectedMainPlant = mainPlantOptions.find(
        (p) => p.value === formData.parentPlantId,
      )

      const rowData = {
        parentPlantId: formData.parentPlantId,
        parentPlant: selectedMainPlant?.label || '',
        senderPlantId: formData.senderPlantId,
        senderPlant: selectedSenderPlant?.label || '',
        senderPlantCode: selectedSenderPlant?.code || '',
        recieverPlantId: formData.recieverPlantId,
        recieverPlant: selectedReceiverPlant?.label || '',
        recieverPlantCode: selectedReceiverPlant?.code || '',
        receiverCostCenterId: formData.receiverCostCenterId,
        receiverCostCenter: selectedReceiverCostCenter?.label || '',
        receiverCostCenterCode: selectedReceiverCostCenter?.code || '',
        costCenterId: formData.costCenterId,
        costCenter: selectedSenderCostCenter?.label || '',
        costCenterCode: selectedSenderCostCenter?.code || '',
        senderNormParameterId: formData.senderNormParameterId,
        senderMaterial: selectedSenderMaterial?.label || '',
        senderMaterialCode: selectedSenderMaterial?.sapMaterialCode || '',
        senderMaterialUom: selectedSenderMaterial?.uom || '',
        // Receiver utility is resolved on the backend ("No Material" norm parameter)
        remarks: formData.remarks || '',
        aopYear: AOP_YEAR,
      }

      const response = isEditMode
        ? await UtilityPlantApiServiceV2.updateFixedConsumptionRowJMD(
            keycloak,
            rowData,
            AOP_YEAR,
          )
        : await UtilityPlantApiServiceV2.addFixedConsumptionRowJMD(
            keycloak,
            rowData,
            AOP_YEAR,
          )

      if (response?.code !== 200) {
        setSubmitError(response?.message || 'Failed to save. Please try again.')
        return
      }

      onClose()
      if (onSuccess) onSuccess()
    } catch (error) {
      console.error(
        isEditMode
          ? 'Error updating fixed consumption row:'
          : 'Error adding fixed consumption row:',
        error,
      )
      setSubmitError(
        isEditMode
          ? 'Failed to update row. Please try again.'
          : 'Failed to add row. Please try again.',
      )
    } finally {
      setIsButtonDisabled(false)
    }
  }

  const handleCancel = () => {
    onClose()
  }

  const codeLabel = (option) =>
    option.code ? `${option.label} (${option.code})` : option.label || ''

  const renderAutocompleteField = ({
    field,
    label,
    options,
    loading,
    disabled,
    getOptionLabel = codeLabel,
  }) => (
    <Grid item xs={12} key={field}>
      <Autocomplete
        options={options}
        getOptionLabel={getOptionLabel}
        isOptionEqualToValue={(option, value) => option.value === value.value}
        value={options.find((o) => o.value === formData[field]) || null}
        onChange={(e, newValue) =>
          handleFieldChange(field, newValue ? newValue.value : '')
        }
        disabled={disabled}
        loading={loading}
        renderInput={(params) => (
          <TextField
            {...params}
            label={label}
            error={formError[field].isError}
            helperText={formError[field].errorMessage}
            required
            size='small'
          />
        )}
      />
    </Grid>
  )

  return (
    <Dialog open={open} onClose={handleCancel} maxWidth='sm' fullWidth>
      <DialogTitle sx={{ borderBottom: '1px solid #ccc' }}>
        <Typography variant='h6' className='dialog-title'>
          {isEditMode ? 'Edit Fixed Consumption' : 'Add Fixed Consumption'}
        </Typography>
      </DialogTitle>
      <DialogContent dividers>
        <Box sx={{ mt: 2 }}>
          {submitError && (
            <Typography color='error' variant='body2' sx={{ mb: 2 }}>
              {submitError}
            </Typography>
          )}
          <Grid container spacing={2}>
            {/* CPP Plant — autocomplete */}
            {renderAutocompleteField({
              field: 'parentPlantId',
              label: FIELD_LABELS.parentPlantId,
              options: mainPlantOptions,
              loading: false,
              disabled: isEditMode,
            })}

            {/* Sender group */}
            <Grid item xs={12}>
              <Typography variant='subtitle2' color='text.secondary'>
                Sender
              </Typography>
            </Grid>

            {/* Sender Plant — autocomplete */}
            {renderAutocompleteField({
              field: 'senderPlantId',
              label: FIELD_LABELS.senderPlantId,
              options: senderPlantOptions,
              loading: loadingPlants,
              disabled: !formData.parentPlantId || loadingPlants,
            })}

            {/* Sender Material — autocomplete (norm parameters of sender plant) */}
            {renderAutocompleteField({
              field: 'senderNormParameterId',
              label: FIELD_LABELS.senderNormParameterId,
              options: senderMaterialDropdown,
              loading: loadingUtilities,
              disabled: !formData.senderPlantId || loadingUtilities,
              getOptionLabel: (option) =>
                option.sapMaterialCode
                  ? `${option.label} (${option.sapMaterialCode})`
                  : option.label || '',
            })}

            {/* Sender Cost Center — autocomplete */}
            {renderAutocompleteField({
              field: 'costCenterId',
              label: FIELD_LABELS.costCenterId,
              options: senderCostCenterOptions,
              loading: loadingSenderCostCenters || loadingParentCostCenters,
              disabled:
                !formData.senderPlantId ||
                loadingSenderCostCenters ||
                loadingParentCostCenters,
            })}

            {/* Receiver group */}
            <Grid item xs={12}>
              <Typography variant='subtitle2' color='text.secondary'>
                Receiver
              </Typography>
            </Grid>

            {/* Receiver Plant — autocomplete */}
            {renderAutocompleteField({
              field: 'recieverPlantId',
              label: FIELD_LABELS.recieverPlantId,
              options: receiverPlantOptions,
              loading: loadingPlants,
              disabled: !formData.parentPlantId || loadingPlants,
            })}

            {/* Receiver Cost Center — autocomplete */}
            {renderAutocompleteField({
              field: 'receiverCostCenterId',
              label: FIELD_LABELS.receiverCostCenterId,
              options: receiverCostCenterOptions,
              loading: loadingReceiverCostCenters || loadingParentCostCenters,
              disabled:
                !formData.recieverPlantId ||
                loadingReceiverCostCenters ||
                loadingParentCostCenters,
            })}

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

export default AddFixedConsumptionDialogJMD
