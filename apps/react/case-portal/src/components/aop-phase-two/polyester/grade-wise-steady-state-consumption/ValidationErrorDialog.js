import {
  Box,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  IconButton,
} from '@mui/material'
import { styled } from '@mui/material/styles'
import CloseIcon from '@mui/icons-material/Close'
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline'
import AdvanceKendoTable from '../../common/AdvanceKendoTable/index'
import { customValueFormatterPhaseTwo } from '../../common/ValueFormatterPhaseTwo'
import { generateExcelNameWithoutExt } from 'components/aop-phase-two/common/utilities/excelNameUtil'
import { useSelector } from 'react-redux'

const StyledDialog = styled(Dialog)(() => ({
  '& .MuiPaper-root': {
    borderRadius: '16px',
    boxShadow:
      '0 20px 25px -5px rgb(0 0 0 / 0.1), 0 8px 10px -6px rgb(0 0 0 / 0.1)',
    borderTop: '5px solid #ef4444',
    padding: '8px',
  },
}))

const ValidationErrorDialog = ({ open, onClose, errors = [] }) => {
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const valueFormat = customValueFormatterPhaseTwo(5)
  const EXCEL_NAME = generateExcelNameWithoutExt(
    dataGridStore,
    'Grade_Wise_Steady_State_Consumption_Validation_Errors',
  )
  const columns = [
    {
      field: 'type',
      title: 'Type',
      minWidth: 250,
      type: 'text',
      editable: false,
    },
    {
      field: 'materialName',
      title: 'Particular',
      minWidth: 250,
      type: 'text',
      editable: false,
    },
    {
      field: 'uom',
      title: 'UOM',
      minWidth: 100,
      type: 'text',
      editable: false,
    },
    {
      field: 'month',
      title: 'Month',
      minWidth: 100,
      type: 'text',
      editable: false,
    },
    {
      field: 'expectedValue',
      title: 'Wt Avg Norms',
      minWidth: 150,
      type: 'number1',
      editable: false,
      format: valueFormat,
    },
    {
      field: 'actualValue',
      title: 'Plant Wise Norms',
      minWidth: 150,
      type: 'number1',
      editable: false,
      format: valueFormat,
    },
    {
      field: 'difference',
      title: 'Difference',
      minWidth: 150,
      type: 'number1',
      editable: false,
      format: valueFormat,
    },
  ]

  const permissions = {
    allAction: true,
    downloadExcelBtnFromUI: true,
    showExport: false,
    showImport: false,
    saveBtn: false,
    showCalculate: false,
    showDropdown: false,
    showTitle: false,
    ExcelName: EXCEL_NAME,
  }

  return (
    <StyledDialog
      open={open}
      onClose={onClose}
      disableScrollLock
      maxWidth='lg'
      fullWidth
    >
      <DialogTitle
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          pb: 1,
          borderBottom: '1px solid #f3f4f6',
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <ErrorOutlineIcon sx={{ color: '#ef4444', fontSize: '1.8rem' }} />
          <Typography
            variant='h6'
            sx={{
              fontWeight: 700,
              color: '#1f2937',
              fontSize: '1.1rem',
            }}
          >
            Validation Error
          </Typography>
        </Box>
        <IconButton size='small' onClick={onClose} sx={{ color: '#9ca3af' }}>
          <CloseIcon fontSize='small' />
        </IconButton>
      </DialogTitle>

      <DialogContent sx={{ mt: 2, pb: 1 }}>
        <Box
          sx={{
            p: 2,
            borderRadius: '10px',
            bgcolor: '#fef2f2',
            border: '1px solid #fee2e2',
          }}
        >
          <Typography
            sx={{
              fontSize: '0.75rem',
              color: '#991b1b',
              fontWeight: 600,
              letterSpacing: '0.5px',
              mb: 0.5,
            }}
          >
            The system encountered a validation error between plant norms and
            grade wise norms. Please update grade wise norms and save the
            details.
          </Typography>
        </Box>

        <Box sx={{ width: '100%', mb: 2 }}>
          <AdvanceKendoTable
            columns={columns}
            rows={errors}
            setRows={() => {}}
            modifiedCells={{}}
            setModifiedCells={() => {}}
            permissions={permissions}
            customHeight={40}
            paginationConfig={{
              threshold: 10,
              buttonCount: 3,
              pageSizes: [5, 10, 20],
              defaultPageSize: 10,
            }}
          />
        </Box>
      </DialogContent>

      <DialogActions sx={{ p: 2, pt: 1 }}>
        <Button
          onClick={onClose}
          variant='contained'
          size='medium'
          sx={{
            bgcolor: '#ef4444',
            color: '#ffffff',
            borderRadius: '8px',
            textTransform: 'none',
            fontWeight: 600,
            px: 3,
            '&:hover': {
              bgcolor: '#dc2626',
            },
          }}
        >
          Close
        </Button>
      </DialogActions>
    </StyledDialog>
  )
}

export default ValidationErrorDialog
