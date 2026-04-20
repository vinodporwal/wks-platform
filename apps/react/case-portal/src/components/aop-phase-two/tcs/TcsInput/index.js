import { Box, Tab, Tabs } from '@mui/material'
import Notification from 'components/Utilities/Notification'
import { useEffect, useMemo, useState } from 'react'
import { useSelector } from 'react-redux'
import { TcsApiService } from 'components/aop-phase-two/services/tcs/tcsApiService'
import { useSession } from 'SessionStoreContext'
import UnitCapacity from './UnitCapacity'
import Shutdown from './Shutdown'
import Slowdown from './Slowdown'
import CPPUnitsSdPlan from './CPPUnitsSdPlan'
import CrudBlendWindow from './CrudBlendWindow'
import ROGC from './ROGC'
import PCGOutlook from './PCGOutlook'
import NetUnitCapacity from './NetUnitCapacity'
import RemarkDialog from './workflow/RemarkDialog'
import SubmitSection from './workflow/SubmitSection'
import { getUserRole, ROLES } from '../utils/roleUtils'
import { TcsWorkflowApiService } from 'components/aop-phase-two/services/tcs/tcsWorkflowApiService'
import AuditTrail from './workflow/AuditTrail'
import AopTabs from '../../common/components/AopTabs'

// Handler to render tab component based on displayName
const renderTabComponent = (tabDisplayName, props) => {
  switch (tabDisplayName) {
    case 'Unit Capacity':
      return <UnitCapacity {...props} />
    case 'Shutdown':
      return <Shutdown {...props} />
    case 'Slowdown':
      return <Slowdown {...props} />
    case 'Net Unit Capacity':
      return <NetUnitCapacity {...props} />
    case 'CPP Units SD Plan':
      return <CPPUnitsSdPlan {...props} />
    case 'PCG Outlook':
      return <PCGOutlook {...props} />
    case 'ROGC':
      return <ROGC {...props} />
    case 'Crude Blend Window':
      return <CrudBlendWindow {...props} />
    default:
      return null
  }
}

const TcsInput = () => {
  const keycloak = useSession()
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { plantObject, siteObject, verticalObject, year } = dataGridStore

  const PLANT_ID = plantObject?.id
  const PLANT_NAME = plantObject?.name
  const SITE_ID = siteObject?.id
  const VERTICAL_ID = verticalObject?.id
  const AOP_YEAR = year?.selectedYear

  // State management - Snackbar notifications
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  // Tab management
  const [tabObj, setTabObj] = useState([])
  const [tabIndex, setTabIndex] = useState(0)

  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [historyDialogOpen, setHistoryDialogOpen] = useState(false)
  const [isSubmitEligible, setIsSubmitEligible] = useState(false)
  const [isCheckingEligibility, setIsCheckingEligibility] = useState(false)
  const [isWorkflowTriggered, setIsWorkflowTriggered] = useState(false)
  const [isSubmittingRemark, setIsSubmittingRemark] = useState(false)
  const [timelineData, setTimelineData] = useState([])
  // Generate dynamic tooltip for Plant Manager
  const submitTooltip = useMemo(() => {
    if (!isSubmitEligible) {
      return 'Plant submission already done'
    }
    return 'Submit plant data to AOM for approval'
  }, [isSubmitEligible])

  // Check workflow status on mount
  useEffect(() => {
    if (
      PLANT_ID &&
      AOP_YEAR &&
      SITE_ID &&
      VERTICAL_ID &&
      PLANT_NAME &&
      tabObj.length > 0
    ) {
      checkSubmitEligibility()
      checkWorkflowTriggered()
    }
  }, [PLANT_ID, AOP_YEAR, SITE_ID, VERTICAL_ID, PLANT_NAME, tabObj])

  const checkSubmitEligibility = async (showMessage = true) => {
    try {
      setIsCheckingEligibility(true)
      if (!PLANT_ID || !AOP_YEAR || !SITE_ID || !VERTICAL_ID || !PLANT_NAME) {
        setIsSubmitEligible(false)
        return
      }
      // Fetch workflow variables to check submission status
      const variables = await TcsWorkflowApiService.getWorkflowVariables(
        keycloak,
        VERTICAL_ID,
        SITE_ID,
        AOP_YEAR,
      )

      setTimelineData(variables)
      if (variables.length == 0) {
        setIsSubmitEligible(true)
      } else {
        // Find submissionStatus variable (for PLANT_MANAGER)
        const submissionStatusVar = variables?.find(
          (v) => v.name === 'submissionStatus',
        )

        // Find ctsTechSubmissionStatus variable (for CTS_TECH_MANAGER)
        const ctsTechSubmissionStatusVar = variables?.find(
          (v) => v.name === 'ctsTechSubmissionStatus',
        )

        // Check based on user role
        if (userRole === ROLES.PLANT_MANAGER) {
          if (submissionStatusVar && submissionStatusVar.value) {
            try {
              const submissionStatus = JSON.parse(submissionStatusVar.value)
              const isPlantSubmitted = submissionStatus[PLANT_NAME] === true

              if (isPlantSubmitted) {
                setIsSubmitEligible(false)
                return
              } else {
                setIsSubmitEligible(true)
                return
              }
            } catch (parseError) {
              console.error('Error parsing submissionStatus:', parseError)
            }
          } else {
            // No submission status yet - enable submit
            setIsSubmitEligible(true)
            return
          }
        } else if (userRole === ROLES.CTS_TECH_MANAGER) {
          if (ctsTechSubmissionStatusVar && ctsTechSubmissionStatusVar.value) {
            try {
              const ctsTechSubmissionStatus = JSON.parse(
                ctsTechSubmissionStatusVar.value,
              )
              const isCtsTechSubmitted =
                ctsTechSubmissionStatus[PLANT_NAME] === true

              if (isCtsTechSubmitted) {
                setIsSubmitEligible(false)
                return
              } else {
                setIsSubmitEligible(true)
                return
              }
            } catch (parseError) {
              console.error(
                'Error parsing ctsTechSubmissionStatus:',
                parseError,
              )
            }
          } else {
            // No submission status yet - enable submit
            setIsSubmitEligible(true)
            return
          }
        }

        // Fallback for all other roles - eligible by default
        setIsSubmitEligible(false)
      }
    } catch (err) {
      console.error('Error checking submit eligibility:', err)
      setIsSubmitEligible(false)
      setSnackbarData({
        message: 'Failed to check submit eligibility',
        severity: 'error',
      })
      setSnackbarOpen(true)
    } finally {
      setIsCheckingEligibility(false)
    }
  }

  const checkWorkflowTriggered = async () => {
    try {
      const response = await TcsWorkflowApiService.checkWorkflowStatus(
        keycloak,
        VERTICAL_ID,
        SITE_ID,
        AOP_YEAR,
      )

      // If workflow is already triggered, disable submit button
      setIsWorkflowTriggered(response)
      return response
    } catch (err) {
      console.error('Error checking workflow status:', err)
    }
  }
  // PRECHECK DONE

  const handleViewHistory = () => {
    setHistoryDialogOpen(true)
  }

  const handleCloseHistory = () => {
    setHistoryDialogOpen(false)
  }

  // Get current tab object (has id, displayName, displaySequence)
  const currentTab =
    tabIndex !== null && tabObj[tabIndex] ? tabObj[tabIndex] : {}

  const userRole = useMemo(() => {
    let allUsers = keycloak?.realmAccess?.roles
    console.log('allUsers', allUsers)
    return getUserRole(allUsers)
  }, [keycloak?.realmAccess?.roles])

  const userName = useMemo(() => {
    return keycloak.tokenParsed.name
  }, [keycloak])

  // Fetch all tabs and visible tab IDs from backend
  useEffect(() => {
    fetchTabsData()
  }, [PLANT_ID, SITE_ID, VERTICAL_ID])

  // Reset tabIndex to 0 when tabObj changes (after filtering)
  useEffect(() => {
    if (tabObj.length > 0) {
      setTabIndex(0)
    } else {
      setTabIndex(null)
    }
  }, [tabObj])

  const fetchTabsData = async () => {
    try {
      if (!PLANT_ID || !SITE_ID || !VERTICAL_ID) return

      // First API: Get list of all tabs
      const allTabsResponse = await TcsApiService.getTcsAllTabs(keycloak)
      const allTabsList = allTabsResponse?.data?.configurationTypeList || []

      // Second API: Get array of tab IDs to show
      const visibleTabsResponse = await TcsApiService.getTcsVisibleTabs(
        keycloak,
        VERTICAL_ID,
        SITE_ID,
        PLANT_ID,
      )

      let visibleTabIds = []
      if (visibleTabsResponse?.data) {
        visibleTabIds =
          typeof visibleTabsResponse.data === 'string'
            ? JSON.parse(visibleTabsResponse.data)
            : visibleTabsResponse.data
      }

      // Filter tabs to show only visible ones
      if (
        allTabsList &&
        Array.isArray(visibleTabIds) &&
        visibleTabIds.length > 0
      ) {
        const visibleTabIdsLower = visibleTabIds.map((id) => id.toLowerCase())
        const filteredTabs = allTabsList
          .filter((tab) => visibleTabIdsLower.includes(tab.id.toLowerCase()))
          .sort((a, b) => a.displaySequence - b.displaySequence)
        setTabObj(filteredTabs)
      } else {
        // If no visible tabs are returned, show empty
        console.warn('No visible tabs configured')
        setTabObj([])
      }
    } catch (err) {
      console.error('Error fetching tabs:', err)
      setSnackbarData({
        message: 'Failed to load tabs configuration',
        severity: 'error',
      })
      setSnackbarOpen(true)
    }
  }

  // Handle workflow trigger
  const handleTriggerWorkflow = async () => {
    try {
      if (!keycloak || !SITE_ID || !VERTICAL_ID) {
        setSnackbarData({
          message: 'Missing required parameters to trigger workflow',
          severity: 'error',
        })
        setSnackbarOpen(true)
        return { success: false }
      }

      // Trigger workflow (start process)
      await TcsWorkflowApiService.triggerWorkflow(
        keycloak,
        VERTICAL_ID,
        SITE_ID,
        AOP_YEAR,
      )

      // Update workflow triggered state
      setIsWorkflowTriggered(true)

      return { success: true }
    } catch (err) {
      console.error('Error triggering workflow:', err)
      setSnackbarData({
        message: 'Failed to trigger workflow',
        severity: 'error',
      })
      setSnackbarOpen(true)
      return { success: false, error: err }
    }
  }

  // Handle remark submission
  const handleRemarkSubmit = async (remark) => {
    // Validation: Check for missing required parameters
    if (
      !keycloak ||
      !PLANT_ID ||
      !PLANT_NAME ||
      !SITE_ID ||
      !VERTICAL_ID ||
      !AOP_YEAR
    ) {
      setSnackbarData({
        message: 'Missing required parameters. Please refresh and try again.',
        severity: 'error',
      })
      setSnackbarOpen(true)
      return // Don't close dialog, allow user to retry
    }

    // Show loading state
    setIsSubmittingRemark(true)
    let workflowWasTriggered = false

    try {
      // If workflow not triggered, trigger it first and wait for success
      if (!isWorkflowTriggered) {
        const triggerResult = await handleTriggerWorkflow()

        workflowWasTriggered = triggerResult
      }

      const payload = {
        keycloak,
        plantId: PLANT_ID,
        plantName: PLANT_NAME,
        siteId: SITE_ID,
        verticalId: VERTICAL_ID,
        userRole,
        userName,
        remark,
        aopYear: AOP_YEAR,
      }

      // Step 1: Save the current role's remark
      if (userRole == ROLES.PLANT_MANAGER) {
        await TcsWorkflowApiService.savePlantManagerRemark(payload)
      } else if (userRole == ROLES.CTS_TECH_MANAGER) {
        await TcsWorkflowApiService.saveCTSTechManagerRemark(payload)
      }

      // Step 2: Check if the other role has already submitted (using existing timelineData)
      // Note: Current role is NOW submitting (will be true after API call above)
      // So we only need to check if the OTHER role has already submitted
      const submissionStatusVar = timelineData?.find(
        (v) => v.name === 'submissionStatus',
      )
      const ctsTechSubmissionStatusVar = timelineData?.find(
        (v) => v.name === 'ctsTechSubmissionStatus',
      )

      let otherRoleAlreadyApproved = false

      if (userRole === ROLES.PLANT_MANAGER) {
        // Current role is PLANT_MANAGER (now submitting = true)
        // Check if CTS_TECH_MANAGER already submitted
        if (ctsTechSubmissionStatusVar) {
          try {
            const ctsTechSubmissionStatus = JSON.parse(
              ctsTechSubmissionStatusVar.value,
            )
            otherRoleAlreadyApproved =
              ctsTechSubmissionStatus[PLANT_NAME] === true
          } catch (parseError) {
            console.error('Error parsing ctsTechSubmissionStatus:', parseError)
          }
        }
      } else if (userRole === ROLES.CTS_TECH_MANAGER) {
        // Current role is CTS_TECH_MANAGER (now submitting = true)
        // Check if PLANT_MANAGER already submitted
        if (submissionStatusVar) {
          try {
            const submissionStatus = JSON.parse(submissionStatusVar.value)
            otherRoleAlreadyApproved = submissionStatus[PLANT_NAME] === true
          } catch (parseError) {
            console.error('Error parsing submissionStatus:', parseError)
          }
        }
      }

      // // Step 3: If other role already approved, submit plant to AOM
      // // (Current role just approved in Step 1, so both are now approved)
      if (otherRoleAlreadyApproved) {
        setSnackbarOpen(true)
        setSnackbarData({
          message: `${PLANT_NAME} TCS data submitted to AOM successfully (both roles approved)`,
          severity: 'success',
        })
      } else {
        setSnackbarOpen(true)
        setSnackbarData({
          message: `${PLANT_NAME} TCS data submission completed successfully. Waiting for ${userRole === ROLES.PLANT_MANAGER ? 'CTS Tech Manager' : 'Plant Manager'} approval.`,
          severity: 'success',
        })
      }

      // Refresh submit eligibility after submission (without showing "already submitted" message)
      await checkSubmitEligibility(false)

      // Close the remark dialog on success
      setRemarkDialogOpen(false)
      // }
    } catch (err) {
      console.error('Error saving remark:', err)

      // Handle partial failure: workflow started but submission failed
      if (workflowWasTriggered) {
        setSnackbarData({
          message:
            'Workflow started but plant submission failed. Please try submitting again.',
          severity: 'warning',
        })
      } else {
        setSnackbarData({
          message: 'Failed to complete plant submission. Please try again.',
          severity: 'error',
        })
      }
      setSnackbarOpen(true)
      // Don't close dialog, allow user to retry
    } finally {
      setIsSubmittingRemark(false)
    }
  }

  console.log('userRole', userRole)

  return (
    <Box
      sx={{
        p: 2,
        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)',
        borderRadius: '4px',
        backgroundColor: '#fff',
      }}
    >
      {/* Tabs and Action Buttons in One Row */}
      <Box
        sx={{ display: 'flex', alignItems: 'center', gap: 2, width: '100%' }}
      >
        {/* Tabs Section - Flex grow to fill available space */}
        <Box sx={{ flex: 1, overflowX: 'auto' }}>
          <AopTabs
            tabIndex={tabIndex}
            setTabIndex={setTabIndex}
            tabs={tabObj?.map((tab) => tab.displayName || tab.name) || []}
          />
        </Box>

        {/* Submit button and History icon - Fixed on right */}
        {tabObj.length !== 0 && (
        <SubmitSection
          onSubmitClick={() => setRemarkDialogOpen(true)}
          onViewHistory={handleViewHistory}
          isEligible={isSubmitEligible}
          isLoading={isSubmittingRemark}
          isWorkflowTriggered={isWorkflowTriggered}
          submitTooltip={submitTooltip}
        />
        )}
      </Box>

      {/* Tab Content */}
      <Box>
        {currentTab?.displayName &&
          renderTabComponent(currentTab.displayName, {
          currentTab,
          PLANT_ID,
          PLANT_NAME,
          AOP_YEAR,
          SITE_ID,
            VERTICAL_ID,
          snackbarData,
          setSnackbarData,
          snackbarOpen,
          setSnackbarOpen,
          isSubmitEligible,
        })}
      </Box>

      <RemarkDialog
        open={remarkDialogOpen}
        handleClose={() => setRemarkDialogOpen(false)}
        title='TCS Input Submission'
        placeholder='Enter your remarks here...'
        onSubmit={handleRemarkSubmit}
        maxLength={500}
        role={userRole}
        keycloak={keycloak}
        snackbarData={snackbarData}
        setSnackbarData={setSnackbarData}
        setSnackbarOpen={setSnackbarOpen}
      />

      {/* History Dialog */}
      <AuditTrail
        open={historyDialogOpen}
        onClose={handleCloseHistory}
        title='Audit Trail'
        userRole={userRole}
        timelineData={timelineData}
      />

      <Notification
        open={snackbarOpen}
        message={snackbarData.message}
        severity={snackbarData.severity}
        onClose={() => setSnackbarOpen(false)}
      />
    </Box>
  )
}

export default TcsInput
