import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import SendIcon from '@mui/icons-material/Send'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import UndoIcon from '@mui/icons-material/Undo'
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined'
import { Box, Step, StepLabel, Stepper } from '@mui/material'
import MuiAccordion from '@mui/material/Accordion'
import MuiAccordionDetails from '@mui/material/AccordionDetails'
import MuiAccordionSummary from '@mui/material/AccordionSummary'
import { styled } from '@mui/material/styles'
import Notification from 'components/Utilities/Notification'
import React, { useEffect, useMemo, useState } from 'react'
import { useSelector } from 'react-redux'
// import { CaseService } from 'services/CaseService'
import { DataService } from 'services/DataService'
import { AOPWorkFlowService } from 'services/AOPWorkFlowService'
// import { TaskService } from 'services/TaskService'
import { useSession } from 'SessionStoreContext'
import postmanData from '../../../assets/postmandata.json'

import {
  Button,
  Stack,
  Tab,
  Tabs,
  Typography,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
} from '../../../../node_modules/@mui/material/index'
// import '../data-tables/data-grid-css.css'
// import { CaseService } from 'services/CaseService'
// import { TaskService } from 'services/TaskService'
// import { useSession } from 'SessionStoreContext'
import { remarkColumn } from 'components/Utilities/remarkColumn'

import './jio-grid-style.css'

import ProductionAopView from 'components/data-tables-views/kendo-DataTable-production-aop'
import KendoDataTablesReports from 'components/kendo-data-tables/index-reports'
import PlantsProductionSummary from '../Reports-kendo/kendo-PlantsProductionData'
import MonthwiseProduction from '../Reports-kendo/kendo-MonthwiseProduction'
import MonthwiseRawMaterial from '../Reports-kendo/kendo-MonthwiseRawMaterial'
import TurnaroundReport from '../Reports-kendo/kendo-TurnaroundReport'
import AnnualProductionPlan from '../Reports-kendo/AnnualProductionPlan'
import PlantContribution from '../Reports-kendo/kendo-PlantContribution'
import PlantContributionLastFourYears from '../Reports-kendo/kendo-PlantContribution-Last-Four-Years'

import BestAchievedReport from '../Reports/BestAchievedReport'
import MonthWiseRawData from '../Reports/MonthWiseRawData'
import FurnaceRawData from '../Reports/FurnaceRawData'
import OptimizerReport from '../Reports/OptimizerReport'
import TurnaroundReportCracker from '../Reports/TurnaroundReportCracker'
import ValueFormatterProduction from 'utils/ValueFormatterProduction'
import SpecificConsumptionNorm from '../Reports-kendo/SpecificConsumptionnorm'
import SpecificConsumptionNormsII from '../Reports-kendo/specificConsumptionNormsII'
import MonthwiseOperatingHours from '../Reports-kendo/kendo-MonthwiseOperatingHours'
import PlantShutdownSlowdown from '../Reports-kendo/kendo-PlantShutdownSlowdown'
import { getRoleName } from 'services/role-service'
import ShutdownReport from '../Reports-kendo/kendo_DetailsPlannedShutdown'
import ShutdownSummaryReport from '../Reports-kendo/kendo_ShutdownBreak_UpLastFourYear'
import SpecificConsumptionnormForMeg from '../Reports-kendo/SpecificConsumptionnormForMeg'
import AopTabs from 'components/AopTabs'
import { AopApprovalService } from 'services/AopApprovalService'
import AopMyApprovals from 'components/data-tables/AOPWorkFlow/AopMyApprovals'
import WorkflowRemarksDialog from 'components/Utilities/WorkflowRemarksDialog'
import AopWorkflowStepper from 'components/Utilities/AopWorkflowStepper'
const WorkFlowMerge = () => {
  const keycloak = useSession()
  // const READ_ONLY = getRoleName(keycloak)
  // const [steps, setSteps] = useState([])
  const [activeStep, setActiveStep] = useState(0)
  // const [openRejectDialog, setOpenRejectDialog] = useState(false)
  // const [status, setStatus] = useState('')
  // const [text, setText] = useState('')
  // const [role, setRole] = useState('plant_manager')
  // const [showTextBox, setShowTextBox] = useState(false)
  // const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState([])
  const [columns, setColumns] = useState([])
  const [loading, setLoading] = useState(false)
  const [loadingCalculate, setLoadingCalculate] = useState(false)
  const [isCreatingCase, setIsCreatingCase] = useState(false)
  const [showCreateCasebutton, setShowCreateCasebutton] = useState(false)
  // const [isEdit, setIsEdit] = useState(false)
  const [modifiedCells, setModifiedCells] = React.useState({})

  // remark dialog state
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [currentRemark, setCurrentRemark] = useState('')
  const [currentRowId, setCurrentRowId] = useState(null)

  // audit trail state
  const [openAuditPopup, setOpenAuditPopup] = useState(false)
  const handleAuditOpen = () => setOpenAuditPopup(true)
  const handleAuditClose = () => setOpenAuditPopup(false)

  // reject flow state
  const [openRejectDialog, setOpenRejectDialog] = useState(false)
  const [actionDisabled, setActionDisabled] = useState(false)
  const [text, setText] = useState('')
  const [taskId, setTaskId] = useState('')

  // New AOP approval flow (task/aop-approval/*) — kept separate from the legacy
  // /task flow so existing child-grid behaviour is untouched. Buttons here are
  // driven entirely by the server-computed `viewer` block.
  const [viewer, setViewer] = useState(null)
  const [aopGate, setAopGate] = useState('')
  const [aopTaskId, setAopTaskId] = useState('')
  const [aopRole, setAopRole] = useState('')
  const [aopExists, setAopExists] = useState(false)
  const [aopRejectOpen, setAopRejectOpen] = useState(false)

  // Utility Remarks Dialog state for all workflow actions (Submit, Approve, Revert)
  const [workflowDialogOpen, setWorkflowDialogOpen] = useState(false)
  const [workflowActionConfig, setWorkflowActionConfig] = useState({
    type: 'APPROVE',
    label: 'Approve',
    decision: 'APPROVED',
  })

  const dataGridStore = useSelector((state) => state.dataGridStore)
  const {
    verticalChange,
    yearChanged,
    oldYear,
    plantID,
    plantObject,
    siteObject,
    verticalObject,
    year,
  } = dataGridStore

  const PLANT_ID = plantObject?.id
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const AOP_YEAR = year?.selectedYear
  const VERTICAL_NAME = verticalObject?.name
  const SITE_NAME = siteObject?.name
  const PLANT_NAME = plantObject?.name
  const isOldYear = false
  const IS_OLD_YEAR = oldYear?.oldYear

  const IS_CRACKER_HMD =
    VERTICAL_NAME?.toLowerCase() === 'cracker' &&
    SITE_NAME?.toLowerCase() === 'hmd'

  const { isReleased } = dataGridStore
  const IS_RELEASED = isReleased
  const READ_ONLY = getRoleName(keycloak, IS_OLD_YEAR, IS_RELEASED)
  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase()
  const [businessKey, setBusinessKey] = useState('')
  const [masterSteps, setMasterSteps] = useState([])
  const [workflowDto, setWorkFlowDto] = useState({})
  const [status, setStatus] = useState('')
  const [caseId, setCaseId] = useState('')
  const [role, setRole] = useState('')
  // UI feedback
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [tabIndex, setTabIndex] = useState(0)
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const unsavedChangesRef = React.useRef({
    unsavedRows: {},
    rowsBeforeChange: {},
  })
  const [rowModesModel, setRowModesModel] = useState({})

  const onRowModesModelChange = (newRowModesModel) => {
    setRowModesModel(newRowModesModel)
  }
  useEffect(() => {
    setTabIndex(0)
    fetchData()
  }, [PLANT_ID, AOP_YEAR])

  const handleExport = () => {
    handleExportAll()
  }

  const handleCalculate = async () => {
    try {
      const a = true

      if (a) {
        return handleCalculateForMEG()
      }

      setLoadingCalculate(true)

      if (!PLANT_ID || !AOP_YEAR) {
        throw new Error('PLANT_ID or AOP_YEAR not found ')
      }

      const [data, res1, res2, res3, res4, res5, res6, res7, res8, res9] =
        await Promise.all([
          AOPWorkFlowService.handleCalculateAnnualAopCostMiisContribution(
            PLANT_ID,
            AOP_YEAR,
            keycloak,
          ),
          AOPWorkFlowService.handleCalculateProductionVolData2(
            PLANT_ID,
            AOP_YEAR,
            keycloak,
          ),
          AOPWorkFlowService.handleCalculatePlantProductionData(
            PLANT_ID,
            AOP_YEAR,
            keycloak,
          ),
          AOPWorkFlowService.handleCalculateMonthwiseProduction(
            PLANT_ID,
            AOP_YEAR,
            keycloak,
          ),
          AOPWorkFlowService.calculateTurnAroundPlanReportData(
            PLANT_ID,
            AOP_YEAR,
            keycloak,
          ),
          AOPWorkFlowService.calculateAnnualProductionPlanData(
            PLANT_ID,
            AOP_YEAR,
            keycloak,
          ),
          AOPWorkFlowService.handleCalculatePlantConsumptionData(
            PLANT_ID,
            AOP_YEAR,
            keycloak,
          ),
          AOPWorkFlowService.calculatePlantContributionReportData(
            PLANT_ID,
            AOP_YEAR,
            keycloak,
          ),

          AOPWorkFlowService.calculatePlantContributionSummaryYearly(
            PLANT_ID,
            AOP_YEAR,
            keycloak,
          ),

          AOPWorkFlowService.calculatePlantContributionBusinessDemand(
            PLANT_ID,
            AOP_YEAR,
            keycloak,
          ),
          AOPWorkFlowService.calculateGradeSpecificConsumptionNorm(
            PLANT_ID,
            AOP_YEAR,
            keycloak,
          ),

          Promise.resolve(null),
        ])

      const responses = [
        data,
        res1,
        res2,
        res3,
        res4,
        res5,
        res6,
        res7,
        res8,
        res9,
      ]

      const allSuccess = responses.every(
        (res) => res !== null && res !== undefined,
      )

      if (allSuccess) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data refreshed successfully!',
          severity: 'success',
        })
        setLoadingCalculate(false)
        fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Refresh Failed!',
          severity: 'error',
        })
        setLoadingCalculate(false)
      }

      return data
    } catch (error) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: error.message || 'An error occurred',
        severity: 'error',
      })
      setLoadingCalculate(false)
      console.error('Error!', error)
    } finally {
      // setLoadingCalculate(false)
      // console.log('false 1')
    }
  }

  const handleCalculateForMEG = async () => {
    try {
      setLoadingCalculate(true)

      if (!PLANT_ID || !AOP_YEAR) {
        throw new Error('PLANT_ID or AOP_YEAR not found ')
      }

      const [data] = await Promise.all([
        AOPWorkFlowService.handleCalculateAll(PLANT_ID, AOP_YEAR, keycloak),

        Promise.resolve(null),
      ])

      const responses = [data]

      const allSuccess = responses.every(
        (res) => res !== null && res !== undefined,
      )

      if (allSuccess) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data refreshed successfully!',
          severity: 'success',
        })
        setLoadingCalculate(false)
        fetchData()
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: 'Data Refresh Failed!',
          severity: 'error',
        })
        setLoadingCalculate(false)
      }

      return data
    } catch (error) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: error.message || 'An error occurred',
        severity: 'error',
      })
      setLoadingCalculate(false)
      console.error('Error!', error)
    } finally {
      // setLoadingCalculate(false)
      // console.log('false 1')
    }
  }

  const handleExportAll = async () => {
    try {
      setLoading(true)

      if (!PLANT_ID || !AOP_YEAR) {
        throw new Error('PLANT_ID or AOP_YEAR not found')
      }

      const payload = postmanData
      const EXCEL_NAME = `ANNUAL_AOP_REPORT_${VERTICAL_NAME}_${SITE_NAME}_${PLANT_NAME}_${AOP_YEAR}`
      // Await the API call here to ensure completion
      const data = await AOPWorkFlowService.getExcel(
        keycloak,
        payload,
        PLANT_ID,
        AOP_YEAR,
        EXCEL_NAME,
      )

      setSnackbarOpen(true)
      setSnackbarData({
        message: 'Report downloaded successfully!',
        severity: 'success',
      })

      return data
    } catch (error) {
      setSnackbarOpen(true)
      setSnackbarData({
        message: error.message || 'An error occurred',
        severity: 'error',
      })
      console.error('Error!', error)
    } finally {
      setLoading(false)
    }
  }

  const handleRemarkCellClick = async (row) => {
    if (READ_ONLY) return
    // do not delete commented code
    // try {
    //   const cases = await AOPWorkFlowService.getCaseId(keycloak)
    //   console.log(cases?.workflowList?.length)
    //   if (cases?.workflowList?.length !== 0) return
    setCurrentRemark(row.remark || '')
    setCurrentRowId(row.id)
    setRemarkDialogOpen(true)
    // } catch (err) {
    //   console.error('Error fetching case', err)
    // }
  }

  const caseData = {
    caseDefinitionId: 'aopv5',
    owner: {
      id: keycloak.subject || '',
      name: keycloak.idTokenParsed.name || '',
      email: keycloak.idTokenParsed.email || '',
      phone: keycloak.idTokenParsed.phone || '',
    },
    attributes: [
      { name: 'textField', value: '9', type: 'String' },
      { name: 'submit', value: false, type: 'String' },
      { name: 'submit1', value: false, type: 'String' },
    ],
  }

  function getNumericKeysInAllRows(rows = []) {
    if (!Array.isArray(rows) || rows.length === 0) return []

    // collect every key that appears in any row
    const allKeys = Array.from(
      rows.reduce((set, row) => {
        if (row && typeof row === 'object') {
          Object.keys(row).forEach((k) => set.add(k))
        }
        return set
      }, new Set()),
    )

    return allKeys.filter((key) =>
      rows.every((row) => {
        const v = row?.[key]
        // ignore missing / null / empty-string values (they don't disqualify the key)
        if (v === undefined || v === null || String(v).trim() === '')
          return true

        const n = Number(String(v).trim())
        return Number.isFinite(n)
      }),
    )
  }

  const VALUE_FORMATOR = ValueFormatterProduction()

  const generateColumns = (data, numericKeys, handleRemarkCellClick) => {
    const cols = data.headers?.map((header, i) => {
      const field = data.keys[i]
      const isNumeric = numericKeys.includes(field)
      return {
        field,
        headerName: header,
        // minWidth: i === 0 ? 300 : 150,
        minWidth: 120,
        flex: 1,
        ...(i === 0 && {
          renderHeader: (p) => <div>{p.colDef.headerName}</div>,
        }),
        ...(isNumeric && {
          type: 'number',
          format: VALUE_FORMATOR,
        }),
      }
    })

    const remarkIdx = cols.findIndex((c) => c.field === 'remark')
    if (remarkIdx > -1) {
      cols[remarkIdx] = remarkColumn(handleRemarkCellClick)
    }

    return cols
    // The column is considered numeric if:
    // - It's a valid number (including empty values)
  }

  const fetchData = async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    try {
      const { headers, keys, results } =
        await AOPWorkFlowService.getWorkflowData(keycloak, PLANT_ID, AOP_YEAR)
      const numericKeys = getNumericKeysInAllRows(results)
      const formatted = results?.map((row, idx) => ({
        id: idx,
        ...row,
        ...Object.fromEntries(
          Object.entries(row).map(([k, v]) => [
            k,
            numericKeys.includes(k) && v !== '' ? Number(v) : v,
          ]),
        ),
      }))

      setRows(formatted)
      setColumns(
        generateColumns({ headers, keys }, numericKeys, handleRemarkCellClick),
      )
    } catch (err) {
      console.error('Error fetching grid', err)
      setRows([])
      setColumns([])
    } finally {
      // setLoading(false)
      // console.log('false 3')
    }
  }

  const getCaseId = async () => {
    if (!PLANT_ID || !AOP_YEAR || !SITE_ID || !VERTICAL_ID) return
    try {
      const cases = await AOPWorkFlowService.getCaseId(
        keycloak,
        PLANT_ID,
        AOP_YEAR,
        SITE_ID,
        VERTICAL_ID,
      )
      setCaseId(cases?.workflowMasterDTO?.casedefId || '')
      setShowCreateCasebutton(cases?.workflowList?.length === 0)
      setTaskId(cases?.taskId || '')
      setStatus(cases?.status || '')
      setRole(cases?.role || '')
      setWorkFlowDto(cases?.workflowList[0])
      if (cases?.workflowList.length > 0) {
        setBusinessKey(cases?.workflowList[0].caseId)
      }
      const master = cases?.workflowMasterDTO

      setMasterSteps(master?.steps)
      // console.log(master?.steps, 'masterSteps')
      // auto-pick the in-progress or next step
      // setSteps(cases?.workflowMasterDTO?.steps.map((i) => i.displayName))

      const activeIdx = master?.steps?.findIndex(
        (s) => s.status === 'inprogress',
      )
      // console.log(activeIdx, 'activeIdx')
      setActiveStep(
        activeIdx > -1
          ? activeIdx
          : master?.steps?.findIndex((s) => s.status !== 'completed'),
      )
    } catch (err) {
      console.error('Error fetching case', err)
    } finally {
      // setLoading(false)
    }
  }

  const createCase = async () => {
    // 1. Prevent double‐submit
    setIsCreatingCase(true)

    try {
      // 2. Create case + save workflow
      const payload = {
        caseInstance: {
          caseDefinitionId: caseId || caseData.caseDefinitionId,
          owner: {
            id: keycloak.subject || '',
            name: keycloak.idTokenParsed.name || '',
            email: keycloak.idTokenParsed.email || '',
            phone: keycloak.idTokenParsed.phone || '',
          },
          attributes: [
            { name: 'textField', value: '9', type: 'String' },
            { name: 'submit', value: false, type: 'String' },
            { name: 'submit1', value: false, type: 'String' },
          ],
        },
        workflowDTO: {
          year: AOP_YEAR,
          plantFkId: PLANT_ID,
          caseDefId: caseId || caseData.caseDefinitionId,
          // caseId: result.businessKey,
          siteFKId: SITE_ID,
          verticalFKId: VERTICAL_ID,
        },
        variables: caseData.attributes,
        // allData: rows,
        workflowYearDTO: rows,
      }
      const result = await AOPWorkFlowService.submitWorkFlow(payload, keycloak)
      // console.log(result)
      if (result) {
        // console.log('Workflow instance created successfully')
      }
      setSnackbarData({
        message: 'Workflow instance created successfully',
        severity: 'success',
      })
      setLoading(true)
      await getCaseId()
      await fetchAopStatus()
      fetchData()
    } catch (error) {
      console.error('Error creating workflow:', error)
      setSnackbarData({
        message: error.message || 'Failed to create workflow',
        severity: 'error',
      })
      setIsCreatingCase(false)
    } finally {
      // 5. Show snackbar regardless
      setSnackbarOpen(true)
      // setIsCreatingCase(false)
    }
  }

  // --- New AOP approval flow (task/aop-approval/*) ---------------------------------

  // Fetch status + server-computed button state; drives the buttons below.
  const fetchAopStatus = async () => {
    if (!PLANT_ID || !AOP_YEAR) return
    try {
      const data = await AopApprovalService.getStatus(keycloak, PLANT_ID, AOP_YEAR)
      console.log('=== [AOP Approval Status Debug] ===')
      console.log('Plant ID:', PLANT_ID, '| Year:', AOP_YEAR)
      console.log('Workflow Exists:', data?.exists)
      console.log('Current Gate Name:', data?.currentGateName, '(', data?.currentGateDisplayName, ')')
      console.log('Task ID:', data?.taskId)
      console.log('Assigned Role:', data?.assignedRole)
      console.log('Viewer Mode:', data?.viewer?.mode)
      console.log('Viewer Permissions -> Can Submit:', data?.viewer?.canSubmit, '| Can Approve:', data?.viewer?.canApprove, '| Can Revert:', data?.viewer?.canRevert)
      console.log('User Roles (from Viewer/Keycloak):', data?.viewer?.roles || keycloak?.realmAccess?.roles)
      console.log('===================================')
      setViewer(data?.viewer || null)
      setAopGate(data?.currentGateName || '')
      setAopTaskId(data?.taskId || '')
      setAopRole(data?.assignedRole || '')
      setAopExists(Boolean(data?.exists))
    } catch (err) {
      console.error('Error fetching AOP approval status', err)
    }
  }

  // Start the AOP approval workflow (Prepare -> Gate 1).
  const aopStart = async (remarkText = '') => {
    setIsCreatingCase(true)
    try {
      await AopApprovalService.start(keycloak, PLANT_ID, AOP_YEAR, remarkText, aopRole || 'preparer')
      setSnackbarData({ message: 'AOP workflow submitted for approval', severity: 'success' })
      await fetchAopStatus()
      await getCaseId()
    } catch (error) {
      setSnackbarData({
        message: error.message || 'Failed to start workflow',
        severity: 'error',
      })
    } finally {
      setIsCreatingCase(false)
      setSnackbarOpen(true)
    }
  }

  // Apply a gate decision (APPROVED / REVERTED) with the remark from `remarkText`.
  const aopAct = async (decision, remarkText = '') => {
    if (!aopTaskId) return
    if (decision === 'REVERTED' && viewer?.remarkMandatory && !remarkText?.trim()) {
      setSnackbarData({ message: 'A remark is required to revert', severity: 'error' })
      setSnackbarOpen(true)
      return
    }
    setActionDisabled(true)
    try {
      await AopApprovalService.act(keycloak, {
        taskId: aopTaskId,
        plantId: PLANT_ID,
        year: AOP_YEAR,
        gateName: aopGate,
        decision,
        remark: remarkText,
        actorRole: aopRole,
      })
      setSnackbarData({
        message: decision === 'APPROVED' ? 'Approved successfully' : 'Reverted for update successfully',
        severity: 'success',
      })
      setText('')
      setAopRejectOpen(false)
      await fetchAopStatus()
      await getCaseId()
    } catch (err) {
      setSnackbarData({ message: err.message, severity: 'error' })
    } finally {
      setActionDisabled(false)
      setSnackbarOpen(true)
    }
  }

  // Handlers for opening the WorkflowRemarksDialog for each button action
  const handleOpenSubmitDialog = () => {
    setWorkflowActionConfig({
      type: 'SUBMIT',
      label: 'Submit for Approval',
      decision: 'START',
    })
    setWorkflowDialogOpen(true)
  }

  const handleOpenApproveDialog = () => {
    setWorkflowActionConfig({
      type: 'APPROVE',
      label: 'Approve',
      decision: 'APPROVED',
    })
    setWorkflowDialogOpen(true)
  }

  const handleOpenRevertDialog = () => {
    setWorkflowActionConfig({
      type: 'REVERT',
      label: 'Revert',
      decision: 'REVERTED',
    })
    setWorkflowDialogOpen(true)
  }

  const handleWorkflowRemarksSubmit = async (remarkText) => {
    const { type, decision } = workflowActionConfig
    setWorkflowDialogOpen(false)

    if (type === 'SUBMIT') {
      await aopStart(remarkText)
    } else if (type === 'APPROVE' || type === 'REVERT') {
      await aopAct(decision, remarkText)
    }
  }

  useEffect(() => {
    getCaseId()
    fetchAopStatus()
  }, [PLANT_ID, AOP_YEAR])

  // handle reject click
  const handleRejectClick = () => {
    setActionDisabled(true)
    setOpenRejectDialog(true)
  }
  const handleRejectCancel = () => {
    setActionDisabled(false)
    setOpenRejectDialog(false)
    setText('')
  }

  // complete task and post comment
  const handleSubmit = async () => {
    try {
      const comment = {
        body: text,
        parentId: '',
        userId: keycloak.tokenParsed.preferred_username,
        userName: keycloak.tokenParsed.given_name,
        caseId: businessKey,
        role: role,
        status: status,
      }
      const payloadOfCompleteTask = {
        taskId: taskId,
        CaseComment: comment,
        variables: caseData.attributes,
        workflowDTO: workflowDto,
      }
      await AOPWorkFlowService.completeTask(keycloak, payloadOfCompleteTask)
      // await CaseService.addComment(keycloak, text, '', businessKey)
      setSnackbarData({
        message: 'Task completed and comment added!',
        severity: 'success',
      })
      setActionDisabled(true)
      await getCaseId()
      await fetchAopStatus()
    } catch (err) {
      console.error('Error submitting', err)
      setSnackbarData({ message: err.message, severity: 'error' })
      setActionDisabled(false)
    } finally {
      setSnackbarOpen(true)
      setOpenRejectDialog(false)
      setText('')
    }
  }
  const saveChanges = async () => {
    try {
      // console.log(rows, 'workflowDto')
      await AOPWorkFlowService.saveAnnualWorkFlowData(keycloak, rows, PLANT_ID)
      setSnackbarData({
        message: 'Data Saved Successfully!',
        severity: 'success',
      })
      setActionDisabled(true)
      await getCaseId()
      await fetchAopStatus()
    } catch (err) {
      console.error('Error while save', err)
      setSnackbarData({ message: err.message, severity: 'error' })
      setActionDisabled(false)
    } finally {
      setSnackbarOpen(true) // ✅ THIS was the only missing piece
    }
  }

  // Define tab sets
  const defaultTabs = [
    'Annual AOP Cost',
    'Plant Production Summary (T-14)',
    'Month Wise Production Plan (T-16)',
    'Month Wise Raw Data (T-18)',
    'Turnaround Report (T-19A)',
    'Annual Production Plan (T-15)',
    'Plant Contribution (T-21)',
    'Plant Contribution Summary (T-22)',
  ]
  const customMegTabs = [
    'Annual AOP Cost', // Index 0
    'Plant Production Summary (T-14)', // Index 1
    'Annual Production Plan (T-15)', // Index 2 (Moved from 8)
    'Month Wise Production Plan (T-16)', // Index 3 (Moved from 2)
    'Specific Consumption Norms (T-17)', // Index 4 (Moved from 11)
    'Month Wise Raw Data (T-18)', // Index 5 (Moved from 3)
    'Turnaround Report (T-19A)', // Index 6 (Moved from 4)
    'Shutdown Report (T-19B)', // Index 7 (Moved from 5)
    'Shutdown Break-up Last Four Year (T-19C)', // Index 8 (Moved from 6)
    'Norms for Shutdown & Slowdown (T-19D)', // Index 9 (Moved from 7)
    'MonthWise Operating Hours (T-20)', // Index 10
    'Plant Contribution (T-21)', // Index 11 (Moved from 9)
    'Plant Contribution Summary (T-22)', // Index 12
  ]
  const customPETTabs = [
    'Annual AOP Cost',
    'Plant Production Summary (T-14)',
    'Month Wise Production Plan (T-16)',
    'Month Wise Raw Data (T-18)',
    'Turnaround Report (T-19A)',
    'Annual Production Plan (T-15)',
    'Plant Contribution (T-21)',
    'Plant Contribution Summary (T-22)',
    'Specific Consumption Norms (T-17)',
  ]
  const customPPTabs = [
    'Annual AOP Cost',
    'Plant Production Summary (T-14)',
    'Month Wise Production Plan (T-16)',
    'Month Wise Raw Data (T-18)',
    'Turnaround Report (T-19A)',
    'Annual Production Plan (T-15)',
    'Plant Contribution(T-21)',
    'Plant Contribution Summary (T-22)',
    'Specific Consumption Norms (T-17)',
    'Norms Entry Sheet',
  ]
  const customPETabs = [
    'Annual AOP Cost',
    'Plant Production Summary (T-14)',
    'Month Wise Production Plan (T-16)',
    'Month Wise Raw Data (T-18)',
    'Turnaround Report (T-19A)',
    'Annual Production Plan (T-15)',
    'Plant Contribution(T-21)',
    'Plant Contribution Summary (T-22)',
    'Specific Consumption Norms (T-17)',
    'Norms Entry Sheet',
    'Shutdown Report (T-19B)',
    'Shutdown Break-up Last Four Year (T-19C)',
    'Norms for Shutdown & Slowdown (T-19D)',
    'MonthWise Operating Hours (T-20)',
  ]
  const PPTabs = [
    'Annual AOP Cost',
    'Plant Production Summary (T-14)',
    'Month Wise Production Plan (T-16)',
    'Month Wise Raw Data (T-18)',
    'Turnaround Report (T-19A)',
    'Annual Production Plan (T-15)',
    'Plant Contribution (T-21)',
    'Plant Contribution Summary (T-22)',
    'Shutdown Report (T-19B)',
    'Shutdown Break-up Last Four Year (T-19C)',
    'Norms for Shutdown & Slowdown (T-19D)',
    'MonthWise Operating Hours (T-20)',
  ]
  const crackerTabs = [
    'Annual AOP Cost',
    'Optimizer Input / Output',
    'Month Wise Production Plan (T-16)',
    'Month Wise Norms',
    'Furnace Data',
    'Turnaround (T-19A)',
    'Plant Contribution (T-21)',
    'Plant Contribution Summary (T-22)',
  ]
  const elastomerTabs = [
    'Annual AOP Cost',
    'Plant Production Summary (T-14)',
    'Month Wise Production Plan (T-16)',
    'Month Wise Consumption (T-18)',
    'Turnaround Report (T-19A)',
    'Annual Production Plan (T-15)',
    'Plant Contribution (T-21)',
    'Plant Contribution Summary (T-22)',
  ]
  const ptaTabs = [
    'Annual AOP Cost',
    'Plant Production Summary (T-14)',
    'Month Wise Production Plan (T-16)',
    'Month Wise Consumption (T-18)',
    'Turnaround Report (T-19A)',
    'Annual Production Plan (T-15)',
    'Plant Contribution (T-21)',
    'Plant Contribution Summary (T-22)',
  ]
  const vcmTabs = [
    'Annual AOP Cost',
    'Plant Production Summary (T-14)',
    'Month Wise Production Plan (T-16)',
    'Month Wise Raw Data (T-18)', // Changed for VCM
    'Turnaround Report (T-19A)',
    'Annual Production Plan (T-15)',
    'Plant Contribution (T-21)',
    'Plant Contribution Summary (T-22)',
  ]

  // Pick tabs based on vertical
  // Pick tabs based on vertical

  const gridVerticals = ['pp', 'pe', 'pet']

  const activeTabs = useMemo(() => {
    if (lowerVertName === 'cracker') {
      return IS_CRACKER_HMD
        ? crackerTabs.filter((tab) => tab !== 'Furnace Data')
        : crackerTabs
    } else if (gridVerticals?.includes(lowerVertName)) {
      return customPETabs
    } else {
      return customMegTabs
    }
  }, [lowerVertName, crackerTabs, customPETabs, customMegTabs, IS_CRACKER_HMD])
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        gap: '5px',
        // marginTop: '-40px',
      }}
    >
      <Box>
        <AopWorkflowStepper
          steps={masterSteps}
          activeStep={activeStep}
        />

        <Box
          sx={{
            mb: 1,
            px: 1.5,
            py: 0.6,
            borderRadius: '6px',
            backgroundColor: '#f0f7ff',
            border: '1px solid #bae6fd',
            display: 'flex',
            alignItems: 'center',
            gap: 1,
          }}
        >
          <InfoOutlinedIcon sx={{ fontSize: 16, color: '#0284c7', flexShrink: 0 }} />
          <Typography
            variant='body2'
            sx={{
              color: '#334155',
              fontSize: '0.78rem',
              fontWeight: 500,
              fontFamily: "'Honeywell Sans Web', 'Inter', sans-serif",
            }}
          >
            Prices - <strong>MIIS BPC</strong> (Last Budget Year) &nbsp;|&nbsp; Actual Values - <strong>MIIS Contribution</strong> (YTD)
          </Typography>
        </Box>

        {/* AOP approval buttons — visibility comes from the server `viewer` */}
        {((viewer?.canSubmit && !aopExists) || (viewer?.mode === 'ACTION' && aopTaskId)) && (
          <Stack
            direction='row'
            spacing={1.5}
            alignItems='center'
            justifyContent='flex-end'
            sx={{ mt: 1, mb: 1.5 }}
          >
            {viewer?.canSubmit && !aopExists && (
              <Button
                variant='outlined'
                className='btn-save'
                onClick={handleOpenSubmitDialog}
                disabled={isCreatingCase}
                startIcon={<SendIcon sx={{ fontSize: '16px !important' }} />}
                sx={{
                  height: '34px',
                  px: 2.2,
                  fontSize: '0.78rem',
                  fontWeight: 600,
                  borderRadius: '6px',
                  textTransform: 'none',
                  color: '#1565c0',
                  backgroundColor: '#e3f2fd',
                  border: '1.5px solid #1976d2',
                  boxShadow: '0 2px 4px rgba(25, 118, 210, 0.12)',
                  transition: 'all 0.2s ease',
                  '&:hover': {
                    backgroundColor: '#bbdefb',
                    borderColor: '#1565c0',
                    color: '#0d47a1',
                    boxShadow: '0 4px 8px rgba(25, 118, 210, 0.25)',
                  },
                  '&:disabled': {
                    backgroundColor: '#f5f5f5',
                    color: '#bdbdbd',
                    borderColor: '#e0e0e0',
                  },
                }}
              >
                Submit for Approval
              </Button>
            )}
            {viewer?.mode === 'ACTION' && aopTaskId && (
              <>
                <Button
                  variant='outlined'
                  className='btn-add'
                  onClick={handleOpenApproveDialog}
                  disabled={actionDisabled}
                  startIcon={<CheckCircleOutlineIcon sx={{ fontSize: '16px !important' }} />}
                  sx={{
                    height: '34px',
                    px: 2.2,
                    fontSize: '0.78rem',
                    fontWeight: 600,
                    borderRadius: '6px',
                    textTransform: 'none',
                    color: '#2e7d32',
                    backgroundColor: '#e8f5e9',
                    border: '1.5px solid #2e7d32',
                    boxShadow: '0 2px 4px rgba(46, 125, 50, 0.12)',
                    transition: 'all 0.2s ease',
                    '&:hover': {
                      backgroundColor: '#c8e6c9',
                      borderColor: '#1b5e20',
                      color: '#1b5e20',
                      boxShadow: '0 4px 8px rgba(46, 125, 50, 0.25)',
                    },
                    '&:disabled': {
                      backgroundColor: '#f5f5f5',
                      color: '#bdbdbd',
                      borderColor: '#e0e0e0',
                    },
                  }}
                >
                  Approve
                </Button>
                <Button
                  variant='outlined'
                  onClick={handleOpenRevertDialog}
                  disabled={actionDisabled}
                  startIcon={<UndoIcon sx={{ fontSize: '16px !important' }} />}
                  sx={{
                    height: '34px',
                    px: 2.2,
                    fontSize: '0.78rem',
                    fontWeight: 600,
                    borderRadius: '6px',
                    textTransform: 'none',
                    color: '#c62828',
                    backgroundColor: '#ffebee',
                    border: '1.5px solid #c62828',
                    boxShadow: '0 2px 4px rgba(198, 40, 40, 0.12)',
                    transition: 'all 0.2s ease',
                    '&:hover': {
                      backgroundColor: '#ffcdd2',
                      borderColor: '#b71c1c',
                      color: '#b71c1c',
                      boxShadow: '0 4px 8px rgba(198, 40, 40, 0.25)',
                    },
                    '&:disabled': {
                      backgroundColor: '#f5f5f5',
                      color: '#bdbdbd',
                      borderColor: '#e0e0e0',
                    },
                  }}
                >
                  Revert
                </Button>
              </>
            )}
          </Stack>
        )}

        <Stack
          direction='row'
          alignItems='center'
          justifyContent='space-between' // push children to extremes
          sx={{ mt: 0, mb: 1 }}
        >
          {/* LEFT: Tabs */}

          <AopTabs
            tabIndex={tabIndex}
            setTabIndex={setTabIndex}
            tabs={activeTabs}
          >
            {activeTabs.map((label, idx) => (
              <Tab
                key={idx}
                label={label}
                sx={{
                  border: '1px solid #ADD8E6',
                  borderBottom: '1px solid #ADD8E6',
                  fontSize: '0.75rem',
                  padding: '9px',
                  minHeight: '12px',
                }}
              />
            ))}
          </AopTabs>
        </Stack>

        {/* Utility Remarks PopUp for Workflow Actions (Submit, Approve, Revert) */}
        <WorkflowRemarksDialog
          open={workflowDialogOpen}
          onClose={() => setWorkflowDialogOpen(false)}
          onSubmit={handleWorkflowRemarksSubmit}
          actionType={workflowActionConfig.type}
          actionLabel={workflowActionConfig.label}
          role={aopRole || 'Workflow User'}
          gateName={aopGate || 'AOP Approval'}
          plantName={PLANT_NAME}
          year={AOP_YEAR}
          isMandatory={true}
          loading={isCreatingCase || actionDisabled}
        />

        {/* For CRACKER */}
        {lowerVertName === 'cracker' ? (
          <>
            {tabIndex === 0 && (
              <>
                <ProductionAopView
                  handleCalculate={handleCalculate}
                  handleExport={handleExport}
                  fetchSecondGridData={fetchData}
                />
                {tabIndex === 0 && (
                  <KendoDataTablesReports
                    title='Annual AOP Cost'
                    modifiedCells={modifiedCells}
                    autoHeight={true}
                    rows={rows}
                    setRows={setRows}
                    onRowUpdate={(updatedRow) =>
                      console.log('Row Updated:', updatedRow)
                    }
                    columns={columns}
                    loading={loadingCalculate}
                    remarkDialogOpen={remarkDialogOpen}
                    unsavedChangesRef={unsavedChangesRef}
                    setRemarkDialogOpen={setRemarkDialogOpen}
                    currentRemark={currentRemark}
                    setCurrentRemark={setCurrentRemark}
                    currentRowId={currentRowId}
                    setCurrentRowId={setCurrentRowId}
                    rowModesModel={rowModesModel}
                    onRowModesModelChange={onRowModesModelChange}
                    handleCalculate={handleCalculate}
                    handleExport={handleExport}
                    isCreatingCase={isCreatingCase}
                    createCase={createCase}
                    saveChanges={saveChanges}
                    showCreateCasebutton={showCreateCasebutton}
                    permissions={{
                      saveBtn: !isOldYear,
                      saveBtnForWorkflow: true,
                      remarksEditable: true,
                      showCreateCasebutton: showCreateCasebutton,
                      showTitle: true,
                      showWorkFlowBtns: true,
                      // approveBtn: false,
                    }}
                    openAuditPopup={openAuditPopup}
                    handleAuditOpen={handleAuditOpen}
                    handleAuditClose={handleAuditClose}
                    handleRejectClick={handleRejectClick}
                    openRejectDialog={openRejectDialog}
                    handleRejectCancel={handleRejectCancel}
                    handleRemarkCellClick={handleRemarkCellClick}
                    handleSubmit={handleSubmit}
                    taskId={taskId}
                    text={text}
                    setText={setText}
                  />
                )}
                {/* <AopMyApprovals /> */}
              </>
            )}
            {activeTabs[tabIndex] === 'Optimizer Input / Output' && (
              <OptimizerReport />
            )}
            {activeTabs[tabIndex] === 'Month Wise Production Plan (T-16)' && (
              <BestAchievedReport />
            )}
            {activeTabs[tabIndex] === 'Month Wise Norms' && (
              <MonthWiseRawData />
            )}
            {activeTabs[tabIndex] === 'Furnace Data' && <FurnaceRawData />}
            {activeTabs[tabIndex] === 'Turnaround (T-19A)' && (
              <TurnaroundReportCracker />
            )}

            {activeTabs[tabIndex] === 'Plant Contribution (T-21)' && (
              <PlantContribution />
            )}
            {activeTabs[tabIndex] === 'Plant Contribution Summary (T-22)' && (
              <PlantContributionLastFourYears />
            )}
          </>
        ) : gridVerticals.includes(lowerVertName) ? (
          <>
            {tabIndex === 0 && (
              <ProductionAopView
                handleCalculate={handleCalculate}
                handleExport={handleExport}
                fetchSecondGridData={fetchData}
              />
            )}
            {tabIndex === 0 && (
              <KendoDataTablesReports
                title='Annual AOP Cost'
                modifiedCells={modifiedCells}
                autoHeight={true}
                rows={rows}
                setRows={setRows}
                onRowUpdate={(updatedRow) =>
                  console.log('Row Updated:', updatedRow)
                }
                columns={columns}
                loading={loadingCalculate}
                remarkDialogOpen={remarkDialogOpen}
                unsavedChangesRef={unsavedChangesRef}
                setRemarkDialogOpen={setRemarkDialogOpen}
                currentRemark={currentRemark}
                setCurrentRemark={setCurrentRemark}
                currentRowId={currentRowId}
                setCurrentRowId={setCurrentRowId}
                rowModesModel={rowModesModel}
                onRowModesModelChange={onRowModesModelChange}
                handleCalculate={handleCalculate}
                handleExport={handleExport}
                isCreatingCase={isCreatingCase}
                createCase={createCase}
                saveChanges={saveChanges}
                showCreateCasebutton={showCreateCasebutton}
                permissions={{
                  saveBtn: !isOldYear,
                  saveBtnForWorkflow: true,
                  remarksEditable: true,
                  showCreateCasebutton: showCreateCasebutton,
                  showTitle: true,
                  showWorkFlowBtns: true,
                  // approveBtn: false,
                }}
                openAuditPopup={openAuditPopup}
                handleAuditOpen={handleAuditOpen}
                handleAuditClose={handleAuditClose}
                handleRejectClick={handleRejectClick}
                openRejectDialog={openRejectDialog}
                handleRejectCancel={handleRejectCancel}
                handleRemarkCellClick={handleRemarkCellClick}
                handleSubmit={handleSubmit}
                taskId={taskId}
                text={text}
                setText={setText}
              />
            )}
            {/* {tabIndex === 0 && <AopMyApprovals />} */}
            {tabIndex === 1 && <PlantsProductionSummary />}
            {tabIndex === 2 && <MonthwiseProduction />}
            {tabIndex === 3 && <MonthwiseRawMaterial />}
            {tabIndex === 4 && <TurnaroundReport />}
            {tabIndex === 5 && <AnnualProductionPlan />}
            {tabIndex === 6 && <PlantContribution />}
            {tabIndex === 7 && <PlantContributionLastFourYears />}
            {tabIndex === 8 && <SpecificConsumptionNormsII />}
            {tabIndex === 9 && <SpecificConsumptionNorm />}
            {tabIndex === 10 && <ShutdownReport />} {/* T-19B */}
            {tabIndex === 11 && <ShutdownSummaryReport />} {/* T-19C */}
            {tabIndex === 12 && <PlantShutdownSlowdown />} {/* T-19D */}
            {/* Remaining Reports */}
            {tabIndex === 13 && <MonthwiseOperatingHours />} {/* T-20 */}
          </>
        ) : (
          <>
            {tabIndex === 0 && (
              <>
                <ProductionAopView
                  handleCalculate={handleCalculate}
                  handleExport={handleExport}
                  fetchSecondGridData={fetchData}
                />
                {tabIndex === 0 && (
                  <KendoDataTablesReports
                    title='Annual AOP Cost'
                    modifiedCells={modifiedCells}
                    autoHeight={true}
                    rows={rows}
                    setRows={setRows}
                    onRowUpdate={(updatedRow) =>
                      console.log('Row Updated:', updatedRow)
                    }
                    columns={columns}
                    loading={loadingCalculate}
                    remarkDialogOpen={remarkDialogOpen}
                    unsavedChangesRef={unsavedChangesRef}
                    setRemarkDialogOpen={setRemarkDialogOpen}
                    currentRemark={currentRemark}
                    setCurrentRemark={setCurrentRemark}
                    currentRowId={currentRowId}
                    setCurrentRowId={setCurrentRowId}
                    rowModesModel={rowModesModel}
                    onRowModesModelChange={onRowModesModelChange}
                    handleCalculate={handleCalculate}
                    handleExport={handleExport}
                    isCreatingCase={isCreatingCase}
                    createCase={createCase}
                    saveChanges={saveChanges}
                    showCreateCasebutton={showCreateCasebutton}
                    permissions={{
                      saveBtn: !isOldYear,
                      saveBtnForWorkflow: true,
                      remarksEditable: true,
                      showCreateCasebutton: showCreateCasebutton,
                      showTitle: true,
                      showWorkFlowBtns: true,
                      // approveBtn: false,
                    }}
                    openAuditPopup={openAuditPopup}
                    handleAuditOpen={handleAuditOpen}
                    handleAuditClose={handleAuditClose}
                    handleRejectClick={handleRejectClick}
                    openRejectDialog={openRejectDialog}
                    handleRejectCancel={handleRejectCancel}
                    handleRemarkCellClick={handleRemarkCellClick}
                    handleSubmit={handleSubmit}
                    taskId={taskId}
                    text={text}
                    setText={setText}
                  />
                )}
                {/* <AopMyApprovals /> */}
              </>
            )}
            {/* Sorted T-Series Components */}
            {tabIndex === 1 && <PlantsProductionSummary />} {/* T-14 */}
            {tabIndex === 2 && <AnnualProductionPlan />} {/* T-15 */}
            {tabIndex === 3 && <MonthwiseProduction />} {/* T-16 */}
            {tabIndex === 4 && <SpecificConsumptionnormForMeg />} {/* T-17 */}
            {tabIndex === 5 && <MonthwiseRawMaterial />} {/* T-18 */}
            {/* T-19 Group */}
            {tabIndex === 6 && <TurnaroundReport />} {/* T-19A */}
            {tabIndex === 7 && <ShutdownReport />} {/* T-19B */}
            {tabIndex === 8 && <ShutdownSummaryReport />} {/* T-19C */}
            {tabIndex === 9 && <PlantShutdownSlowdown />} {/* T-19D */}
            {/* Remaining Reports */}
            {tabIndex === 10 && <MonthwiseOperatingHours />} {/* T-20 */}
            {tabIndex === 11 && <PlantContribution />} {/* T-21 */}
            {tabIndex === 12 && <PlantContributionLastFourYears />}
            {/* T-22 */}
          </>
        )}
        <Notification
          open={snackbarOpen}
          message={snackbarData.message}
          severity={snackbarData.severity}
          onClose={() => setSnackbarOpen(false)}
        />
      </Box>
    </div>
  )
}

export default WorkFlowMerge
