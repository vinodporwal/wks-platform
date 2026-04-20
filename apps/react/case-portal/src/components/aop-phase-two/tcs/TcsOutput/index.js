import { Box, Tab, Tabs } from '@mui/material'
import Notification from 'components/Utilities/Notification'
import { useEffect, useMemo, useState } from 'react'
import { useSelector } from 'react-redux'
import { TcsOutputApiService } from 'components/aop-phase-two/services/tcs/tcsOutputApiService'
import { TcsWorkflowApiService } from 'components/aop-phase-two/services/tcs/tcsWorkflowApiService'
import { useSession } from 'SessionStoreContext'
import UnitCapacity from './UnitCapacity'
import NetUnitCapacity from './NetUnitCapacity'
import Shutdown from './Shutdown'
import Slowdown from './Slowdown'
import CPPUnitsSdPlan from './CPPUnitsSdPlan'
import CrudBlendWindow from './CrudBlendWindow'
import ROGC from './ROGC'
import PCGOutlook from './PCGOutlook'
import RemarkDialog from '../TcsInput/workflow/RemarkDialog'
import ApproveDialog from '../TcsInput/workflow/ApproveDialog'
import SubmitSection from '../TcsInput/workflow/SubmitSection'
import { getUserRole, ROLES } from '../utils/roleUtils'
import AuditTrail from '../TcsInput/workflow/AuditTrail'
import AopTabs from '../../common/components/AopTabs'

// Handler to render tab component based on displayName
const renderTabComponent = (tabDisplayName, props) => {
  switch (tabDisplayName) {
    case 'Unit Capacity':
      return <UnitCapacity {...props} />
    case 'Net Unit Capacity':
      return <NetUnitCapacity {...props} />
    case 'Shutdown':
      return <Shutdown {...props} />
    case 'Slowdown':
      return <Slowdown {...props} />
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

const TcsOutput = () => {
  const keycloak = useSession()
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

  // State management - Snackbar notifications
  const [snackbarData, setSnackbarData] = useState({
    message: '',
    severity: 'info',
  })
  const [snackbarOpen, setSnackbarOpen] = useState(false)

  // Tab management
  const [tabObj, setTabObj] = useState([])
  const [tabIndex, setTabIndex] = useState(0)

  // Remark state
  const [remarkDialogOpen, setRemarkDialogOpen] = useState(false)
  const [historyDialogOpen, setHistoryDialogOpen] = useState(false)
  const [approveDialogOpen, setApproveDialogOpen] = useState(false)
  const [isSubmitEligible, setIsSubmitEligible] = useState(false)
  const [isCheckingEligibility, setIsCheckingEligibility] = useState(false)
  const [isSubmittingRemark, setIsSubmittingRemark] = useState(false)
  const [timelineData, setTimelineData] = useState([])

  const handleViewHistory = () => {
    setHistoryDialogOpen(true)
  }

  const handleReviewClick = async () => {
    await checkSubmitEligibility()
    setApproveDialogOpen(true)
  }

  const userRole = useMemo(() => {
    let allUsers = keycloak?.realmAccess?.roles
    return getUserRole(allUsers)
  }, [keycloak?.realmAccess?.roles])

  const userName = useMemo(() => {
    return keycloak.tokenParsed.name
  }, [keycloak])

  // Get current tab object (has id, displayName, displaySequence)
  const currentTab =
    tabIndex !== null && tabObj[tabIndex] ? tabObj[tabIndex] : {}

  // Generate dynamic tooltip based on role and eligibility
  const submitTooltip = useMemo(() => {
    // Parse approvalStatus from timelineData
    let approvalStatus = null
    const approvalStatusVar = timelineData?.find(
      (v) => v.name === 'approvalStatus',
    )
    if (approvalStatusVar && approvalStatusVar.value) {
      try {
        approvalStatus = JSON.parse(approvalStatusVar.value)
      } catch (e) {
        console.error('Error parsing approvalStatus:', e)
      }
    }

    if (!isSubmitEligible) {
      if (userRole === ROLES.EPS_ENGINEER) {
        // Check if already submitted
        if (approvalStatus?.aom_approved === true) {
          return 'You have already submitted to CTS Head'
        }
        return 'All plants must be approved before submission'
      } else if (userRole === ROLES.CTS_HEAD) {
        // Check if already submitted
        if (approvalStatus?.cts_approved === true) {
          return 'You have already submitted to EPS Head'
        }
        // Check if EPS Engineer has submitted
        if (approvalStatus?.aom_approved === false) {
          return 'Waiting for AOM submission'
        }
        return 'Waiting for AOM submission, or you have already submitted.'
      } else if (userRole === ROLES.EPS_HEAD) {
        // Check if already submitted
        if (approvalStatus?.eps_approved === true) {
          return 'You have already submitted to Site President'
        }
        // Check if CTS Head has submitted
        if (approvalStatus?.cts_approved === false) {
          return 'Waiting for CTS Head submission'
        }
        return 'Waiting for CTS Head submission, or you have already submitted.'
      } else if (userRole === ROLES.CLUSTER_HEAD) {
        // Check if already submitted
        if (approvalStatus?.cluster_head_approved === true) {
          return 'You have already finalized the data for PIMS Output'
        }
        // Check if EPS Head has submitted
        if (approvalStatus?.eps_approved === false) {
          return 'Waiting for EPS Head submission'
        }
        return 'Waiting for EPS Head submission, or you have already submitted.'
      }

      return 'Submission not available'
    } else {
      if (userRole === ROLES.EPS_ENGINEER) {
        return 'Submit all approved plants to CTS Head'
      } else if (userRole === ROLES.CTS_HEAD) {
        return 'Submit to EPS Head'
      } else if (userRole === ROLES.EPS_HEAD) {
        return 'Submit to Site President'
      } else if (userRole === ROLES.CLUSTER_HEAD) {
        return 'Finalize data for PIMS Output'
      }
      return 'Submit data for next approval'
    }
  }, [isSubmitEligible, userRole, timelineData])

  // Fetch all tabs and visible tab IDs from backend
  useEffect(() => {
    fetchTabsData()
  }, [AOP_YEAR, PLANT_ID, SITE_ID, VERTICAL_ID])

  // Reset tabIndex to 0 when tabObj changes (after filtering)
  useEffect(() => {
    if (tabObj.length > 0) {
      setTabIndex(0)
      checkSubmitEligibility()
    } else {
      setTabIndex(null)
    }
  }, [tabObj])

  // Check if user can submit based on workflow variables
  const checkSubmitEligibility = async () => {
    try {
      if (!keycloak || !SITE_ID || !AOP_YEAR) {
        return
      }

      setIsCheckingEligibility(true)

      // Fetch workflow variables to check approval status
      const variables = await TcsWorkflowApiService.getWorkflowVariables(
        keycloak,
        VERTICAL_ID,
        SITE_ID,
        AOP_YEAR,
      )

      setTimelineData(variables)

      if (variables.length === 0) {
        setIsSubmitEligible(false)
        return
      }

      // Find approvalStatus variable
      const approvalStatusVar = variables?.find(
        (v) => v.name === 'approvalStatus',
      )

      if (approvalStatusVar && approvalStatusVar.value) {
        try {
          // Parse the JSON value
          const approvalStatus = JSON.parse(approvalStatusVar.value)

          // For EPS Engineer: Check if all plants have been approved and AOM not yet submitted
          if (userRole === ROLES.EPS_ENGINEER) {
            // Check if AOM approval is already done from approvalStatus
            const aomApproved = approvalStatus.aom_approved === true

            // If AOM already approved, EPS Engineer cannot submit again
            if (aomApproved) {
              setIsSubmitEligible(false)
            } else {
              // Check if approved count equals total count
              const plantCountVar = variables?.find(
                (v) => v.name === 'plantCount',
              )

              if (plantCountVar && plantCountVar.value) {
                try {
                  const plantCount = JSON.parse(plantCountVar.value)
                  const approvedCount = plantCount.approved_plants || 0
                  const totalCount = plantCount.total_plants || 0

                  const allPlantsApproved =
                    approvedCount === totalCount && totalCount > 0

                  setIsSubmitEligible(allPlantsApproved)
                } catch (parseError) {
                  console.error('Error parsing plantCount:', parseError)
                  setIsSubmitEligible(false)
                }
              } else {
                setIsSubmitEligible(false)
              }
            }
          }
          // For CTS Head: Check if EBS approved is true AND CTS Head not yet approved
          else if (userRole === ROLES.CTS_HEAD) {
            const aomApproved = approvalStatus.aom_approved === true
            const ctsApproved = approvalStatus.cts_approved === true

            // Enable submit button only if AOM is approved but CTS Head is not yet approved
            const canSubmit = aomApproved && !ctsApproved
            setIsSubmitEligible(canSubmit)
          }
          // For EPS Head: Check if CTS Head approved is true AND EPS Head not yet approved
          else if (userRole === ROLES.EPS_HEAD) {
            const ctsApproved = approvalStatus.cts_approved === true
            const epsApproved = approvalStatus.eps_approved === true

            // Enable submit button only if CTS Head is approved but EPS Head is not yet approved
            const canSubmit = ctsApproved && !epsApproved
            setIsSubmitEligible(canSubmit)
          }
          // For Cluster Head: Check if EPS Head approved is true AND Cluster Head not yet approved
          else if (userRole === ROLES.CLUSTER_HEAD) {
            const epsApproved = approvalStatus.eps_approved === true
            const clusterHeadApproved =
              approvalStatus.cluster_head_approved === true

            // Enable submit button only if EPS Head is approved but Cluster Head is not yet approved
            const canSubmit = epsApproved && !clusterHeadApproved
            setIsSubmitEligible(canSubmit)
          } else {
            setIsSubmitEligible(false)
          }
        } catch (parseError) {
          console.error('Error parsing approvalStatus:', parseError)
          setIsSubmitEligible(false)
        }
      } else {
        setIsSubmitEligible(false)
      }
    } catch (err) {
      console.error('Error checking submit eligibility:', err)
      setIsSubmitEligible(false)
    } finally {
      setIsCheckingEligibility(false)
    }
  }

  const fetchTabsData = async () => {
    try {
      if (!PLANT_ID || !SITE_ID || !VERTICAL_ID) return

      // First API: Get list of all tabs
      const allTabsResponse = await TcsOutputApiService.getTcsAllTabs(keycloak)
      const allTabsList = allTabsResponse?.data?.configurationTypeList || []
      // setTabObj(allTabsList)

      // Second API: Get array of tab IDs to show
      const visibleTabsResponse = await TcsOutputApiService.getTcsVisibleTabs(
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

  // Handle submission based on user role
  const handleRemarkSubmit = async (remark) => {
    try {
      // Validate required parameters
      if (
        !keycloak ||
        !plantObject?.name ||
        !SITE_ID ||
        !userRole ||
        !AOP_YEAR
      ) {
        setSnackbarData({
          message: 'Missing required parameters. Please refresh and try again.',
          severity: 'error',
        })
        setSnackbarOpen(true)
        return
      }

      setIsSubmittingRemark(true)

      // Call appropriate API based on user role
      if (userRole === ROLES.EPS_ENGINEER) {
        // EPS Engineer submission - uses submittedBy and submissionRemark
        await TcsWorkflowApiService.epsEngineerSubmission(
          keycloak,
          plantObject.name,
          SITE_ID,
          VERTICAL_ID,
          AOP_YEAR,
          remark,
          userRole, // submittedBy
          userName,
        )
      }

      setSnackbarData({
        message: 'Submission completed successfully!',
        severity: 'success',
      })
      setSnackbarOpen(true)

      // Refresh eligibility after submission
      await checkSubmitEligibility()

      // Close the remark dialog on success
      setRemarkDialogOpen(false)
    } catch (err) {
      console.error('Error submitting:', err)

      setSnackbarData({
        message: 'Failed to complete submission. Please try again.',
        severity: 'error',
      })
      setSnackbarOpen(true)
    } finally {
      setIsSubmittingRemark(false)
    }
  }

  // Handle approve/reject actions for CTS_HEAD, EPS_HEAD, and CLUSTER_HEAD
  const handleReviewAction = async (action, remark) => {
    try {
      // Validate required parameters
      if (!keycloak || !SITE_ID || !VERTICAL_ID || !userRole || !AOP_YEAR) {
        setSnackbarData({
          message: 'Missing required parameters. Please refresh and try again.',
          severity: 'error',
        })
        setSnackbarOpen(true)
        return
      }

      setIsSubmittingRemark(true)

      const isApprove = action === 'approve'

      // Create common payload object
      const payload = {
        keycloak,
        SITE_ID,
        VERTICAL_ID,
        AOP_YEAR,
        remark,
        userRole,
        userName,
      }

      // Call appropriate APIs based on role
      if (userRole === ROLES.CTS_HEAD) {
        // CTS_HEAD - Approves after EPS_ENGINEER
        // First API: Approve/Reject
        await TcsWorkflowApiService.ctsHeadApproveReject(payload, isApprove)

        // Second API: Submission (only for approve)
        if (isApprove) {
          await TcsWorkflowApiService.ctsHeadSubmission(payload)
        }
      } else if (userRole === ROLES.EPS_HEAD) {
        // EPS_HEAD - Approves after CTS_HEAD
        // First API: Approve/Reject
        await TcsWorkflowApiService.epsHeadApproveReject(payload, isApprove)

        // Second API: Submission (only for approve)
        if (isApprove) {
          await TcsWorkflowApiService.epsHeadSubmission(payload)
        }
      } else if (userRole === ROLES.CLUSTER_HEAD) {
        // CLUSTER_HEAD - Final approval after EPS_HEAD
        // First API: Approve/Reject
        await TcsWorkflowApiService.clusterHeadApproveReject(payload, isApprove)

        // Second API: Submission (only for approve)
        if (isApprove) {
          await TcsWorkflowApiService.clusterHeadSubmission(payload)
        }
      }

      setSnackbarData({
        message: `${isApprove ? 'Approved' : 'Rejected'} successfully!`,
        severity: 'success',
      })
      setSnackbarOpen(true)

      // Refresh eligibility after action
      await checkSubmitEligibility()

      // Close the remark dialog on success
      setRemarkDialogOpen(false)
    } catch (err) {
      console.error(`Error ${action}ing:`, err)

      setSnackbarData({
        message: `Failed to ${action}. Please try again.`,
        severity: 'error',
      })
      setSnackbarOpen(true)
      throw err
    } finally {
      setIsSubmittingRemark(false)
    }
  }

  const handleResetWorkflow = async () => {
    try {
      // Validate required parameters
      if (!keycloak || !SITE_ID || !VERTICAL_ID || !userRole || !AOP_YEAR) {
        setSnackbarData({
          message: 'Missing required parameters. Please refresh and try again.',
          severity: 'error',
        })
        setSnackbarOpen(true)
        return
      }
      // Call reset workflow API
      await TcsWorkflowApiService.resetWorkflow(
        keycloak,
        SITE_ID,
        AOP_YEAR,
        userRole,
        VERTICAL_ID,
      )
      setSnackbarData({
        message: 'Workflow reset successfully!',
        severity: 'success',
      })
      setSnackbarOpen(true)

      // Refresh eligibility after reset
      await checkSubmitEligibility()
    } catch (err) {
      setSnackbarData({
        message: 'Failed to reset workflow. Please try again.',
        severity: 'error',
      })
      setSnackbarOpen(true)
      throw err
    }
  }

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
          onReviewClick={handleReviewClick}
          isEligible={isSubmitEligible}
          isLoading={isSubmittingRemark}
          submitTooltip={submitTooltip}
          showReviewBtn={userRole === ROLES.EPS_ENGINEER}
          reviewTooltip='Review and approve/reject plants'
          onResetWorkflow={handleResetWorkflow}
          showResetBtn={
            keycloak?.realmAccess?.roles?.includes('reset_workflow') &&
            timelineData?.length > 0
          }
        />
        )}
      </Box>

      {/* Tab Content */}
      <Box>
        {currentTab?.displayName &&
          renderTabComponent(currentTab.displayName, {
          currentTab,
          PLANT_ID,
          AOP_YEAR,
          SITE_ID,
          VERTICAL_ID,
          snackbarData,
          setSnackbarData,
          snackbarOpen,
          setSnackbarOpen,
          userRole,
        })}
      </Box>

      <RemarkDialog
        open={remarkDialogOpen}
        handleClose={() => setRemarkDialogOpen(false)}
        placeholder='Enter your remarks here...'
        onSubmit={handleRemarkSubmit}
        onApprove={(remark) => handleReviewAction('approve', remark)}
        onReject={(remark) => handleReviewAction('reject', remark)}
        maxLength={500}
        role={userRole}
        keycloak={keycloak}
      />

      {/* History Dialog */}
      <AuditTrail
        open={historyDialogOpen}
        onClose={() => setHistoryDialogOpen(false)}
        title='Audit Trail'
        userRole={userRole}
        timelineData={timelineData}
      />

      {/* Approve/Reject Dialog */}
      <ApproveDialog
        open={approveDialogOpen}
        onClose={async () => {
          setApproveDialogOpen(false)
          await checkSubmitEligibility()
        }}
        tab={currentTab.displayName}
        year={AOP_YEAR}
        userRole={userRole}
        userName={userName}
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

export default TcsOutput
