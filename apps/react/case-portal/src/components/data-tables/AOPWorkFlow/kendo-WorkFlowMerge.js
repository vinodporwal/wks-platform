import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
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
      getCaseId()
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

  useEffect(() => {
    getCaseId()
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
      getCaseId()
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
      // getCaseId()
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
    'Annual Production Plan (T-15)',
    'Month Wise Production Plan (T-16)',
    'Specific Consumption Norms (T-17)',
    'Month Wise Raw Data (T-18)',
    'Turnaround Report (T-19A)',
    'Shutdown Report (T-19B)',
    'Shutdown Break-up Last Four Year (T-19C)',
    'Norms for Shutdown & Slowdown (T-19D)',
    'MonthWise Operating Hours (T-20)',
    'Plant Contribution(T-21)',
    'Plant Contribution Summary (T-22)',
    'Norms Entry Sheet',
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
        <Stepper
          activeStep={activeStep}
          alternativeLabel
          sx={{
            marginBottom: '10px',
            '& .MuiStepLabel-label': {
              fontWeight: 'normal',
            },
            '& .MuiStepLabel-label.Mui-active': {
              fontWeight: 'bold',
              color: '#000',
            },
            '& .MuiStepLabel-alternativeLabel': {
              marginTop: '3px !important',
            },
          }}
        >
          {masterSteps?.map((step) => (
            <Step
              key={step.displayName}
              completed={step.status === 'completed'}
              sx={{
                cursor: 'pointer',
                '& .MuiStepIcon-root.Mui-active': {
                  color: '#0100cb',
                },
              }}
            >
              <StepLabel
                error={step.status === 'error'}
                StepIconProps={{
                  sx: {
                    color: step.status === 'completed' ? '#0100cb' : 'grey',
                  },
                }}
              >
                {' '}
                {step.displayName}
              </StepLabel>
            </Step>
          ))}
        </Stepper>

        <Typography
          component='div'
          // className='info-note'
          sx={{
            mb: 1.5,
            px: 1.5,
            py: 1,
            fontSize: '14px',
            fontWeight: 500,
            letterSpacing: '0.2px',
            lineHeight: 1.7,
            borderRadius: '10px',
            background:
              'linear-gradient(90deg, rgba(25,118,210,0.08) 0%, rgba(25,118,210,0.02) 100%)',
            borderLeft: '4px solid #1976d2',
            color: '#1f2937',
            display: 'flex',
            alignItems: 'center',
            gap: 0.5,
            boxShadow: '0 2px 8px rgba(0,0,0,0.04)',
          }}
        >
          <span
            className='info-note__asterisk'
            style={{
              color: '#d32f2f',
              fontSize: '18px',
              fontWeight: 700,
            }}
          >
            *
          </span>

          <span>
            Prices - <strong>MIIS BPC</strong> (Last Budget Year), Actual Values
            -<strong> MIIS Contribution</strong> (YTD).
          </span>
        </Typography>

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

          {/* RIGHT: Buttons */}
          <Stack direction='row' spacing={1} alignItems='center'>
            {taskId && (
              <Button
                variant='contained'
                className='btn-save'
                onClick={handleRejectClick}
                disabled={actionDisabled}
                sx={{ height: 'auto' }}
              >
                Accept
              </Button>
            )}

            {/* <Button
              variant='outlined'
              className='btn-save2'
              sx={{
                color: '#0100cb',
                border: '1px solid',
                height: 'auto',
                width: 'fit-content',
              }}
              onClick={handleAuditOpen}
            >
              Audit Trail
            </Button> */}
          </Stack>
        </Stack>

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
            {gridVerticals?.includes(lowerVertName) ? (
              <>
                {activeTabs[tabIndex] === 'Plant Production Summary (T-14)' && <PlantsProductionSummary />}
                {activeTabs[tabIndex] === 'Month Wise Production Plan (T-16)' && <MonthwiseProduction />}
                {activeTabs[tabIndex] === 'Month Wise Raw Data (T-18)' && <MonthwiseRawMaterial />}
                {activeTabs[tabIndex] === 'Turnaround Report (T-19A)' && <TurnaroundReport />}
                {activeTabs[tabIndex] === 'Annual Production Plan (T-15)' && <AnnualProductionPlan />}
                {(activeTabs[tabIndex] === 'Plant Contribution (T-21)' || activeTabs[tabIndex] === 'Plant Contribution(T-21)') && <PlantContribution />}
                {activeTabs[tabIndex] === 'Plant Contribution Summary (T-22)' && <PlantContributionLastFourYears />}
                {activeTabs[tabIndex] === 'Specific Consumption Norms (T-17)' && <SpecificConsumptionNormsII />}
                {activeTabs[tabIndex] === 'Norms Entry Sheet' && <SpecificConsumptionNorm />}
                {activeTabs[tabIndex] === 'Shutdown Report (T-19B)' && <ShutdownReport />}
                {activeTabs[tabIndex] === 'Shutdown Break-up Last Four Year (T-19C)' && <ShutdownSummaryReport />}
                {activeTabs[tabIndex] === 'Norms for Shutdown & Slowdown (T-19D)' && <PlantShutdownSlowdown />}
                {activeTabs[tabIndex] === 'MonthWise Operating Hours (T-20)' && <MonthwiseOperatingHours />}
              </>
            ) : (
              <>
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
            )}
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