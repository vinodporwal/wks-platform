/**
 * Transform approval status API response to workflow timeline steps
 * @param {Object} approvalStatusJson - Parsed JSON from approvalStatus.value
 * @param {string} selectedPlant - Currently selected plant name
 * @param {Object} submissionStatusJson - Parsed JSON from submissionStatus.value
 * @returns {Array} Timeline steps array
 */
export const transformApprovalStatusToSteps = (
  approvalStatusJson,
  selectedPlant,
  submissionStatusJson,
) => {
  // Fixed sequence of workflow steps with parallel steps support
  const workflowSequence = [
    {
      id: 1,
      label: 'Step 1',
      role: 'CTS Engineer',
      key: 'plant_manager_approved', // Not in API response, always completed
      // isParallel: true, // Indicates this step has parallel sub-steps
      // parallelSteps: [
      //   {
      //     id: '1a',
      //     role: 'Plant Manager',
      //     key: 'plant_manager_approved',
      //   },
      //   {
      //     id: '1b',
      //     role: 'CTS Tech Manager',
      //     key: 'cts_tech_manager_approved',
      //   },
      // ],
    },
    {
      id: 2,
      label: 'Step 2',
      role: 'AOM',
      key: 'aom_approved',
    },
    {
      id: 3,
      label: 'Step 3',
      role: 'CTS Head',
      key: 'cts_approved',
    },
    {
      id: 4,
      label: 'Step 4',
      role: 'EPS Head',
      key: 'eps_approved',
    },
    {
      id: 5,
      label: 'Step 5',
      role: 'Site President',
      key: 'cluster_head_approved',
    },
  ]

  // Check if Step 1 (parallel steps) is completed
  // Both Plant Manager AND CTS Tech Manager must be approved
  let isStep1Completed = false
  const parallelStep = workflowSequence[0]
  if (parallelStep.isParallel && parallelStep.parallelSteps) {
    const allParallelCompleted = parallelStep.parallelSteps.every(
      (pStep) => approvalStatusJson[pStep.key] === true,
    )
    isStep1Completed = allParallelCompleted
  }

  // Find the first false status in sequence (only if Step 1 is completed)
  let firstFalseIndex = -1
  if (isStep1Completed) {
    for (let i = 1; i < workflowSequence.length; i++) {
      const step = workflowSequence[i]
      if (approvalStatusJson[step.key] === false) {
        firstFalseIndex = i
        break
      }
    }
  }

  // Transform to timeline steps
  const timelineSteps = workflowSequence.map((step, index) => {
    let status = 'pending'

    if (index === 0 && step.isParallel) {
      // Parallel steps - check if all are completed
      if (isStep1Completed) {
        status = 'completed'
      } else {
        // At least one parallel step is active
        status = 'active'
      }

      // Add parallel steps status
      const parallelStepsStatus = step.parallelSteps.map((pStep) => ({
        id: pStep.id,
        role: pStep.role,
        status: approvalStatusJson[pStep.key] === true ? 'completed' : 'active',
      }))

      return {
        id: step.id,
        label: step.label,
        isParallel: true,
        parallelSteps: parallelStepsStatus,
        status,
      }
    } else if (!isStep1Completed) {
      // If Step 1 is not completed, all other steps are pending
      status = 'pending'
    } else if (approvalStatusJson[step.key] === true) {
      // If true in API response, it's completed
      status = 'completed'
    } else if (index === firstFalseIndex) {
      // First false in sequence is active
      status = 'active'
    } else {
      // All other false values are pending
      status = 'pending'
    }

    return {
      id: step.id,
      label: step.label,
      role: step.role,
      status,
    }
  })

  return timelineSteps
}

/**
 * Parse and transform the API response
 * @param {Array} apiResponse - The full API response array
 * @param {string} selectedPlant - Currently selected plant name
 * @returns {Array} Timeline steps array
 */
export const parseApprovalStatusResponse = (apiResponse, selectedPlant) => {
  try {
    // Find the approvalStatus object in the response
    const approvalStatusItem = apiResponse.find(
      (item) => item.name === 'approvalStatus',
    )

    if (!approvalStatusItem) {
      // approvalStatus not present yet (e.g., during initial load)
      return []
    }

    // Find the submissionStatus object in the response
    const submissionStatusItem = apiResponse.find(
      (item) => item.name === 'submissionStatus',
    )

    // Parse the JSON strings
    const approvalStatusJson = JSON.parse(approvalStatusItem.value)
    const submissionStatusJson = submissionStatusItem
      ? JSON.parse(submissionStatusItem.value)
      : null

    // Transform to timeline steps
    return transformApprovalStatusToSteps(
      approvalStatusJson,
      selectedPlant,
      submissionStatusJson,
    )
  } catch (error) {
    console.error('Error parsing approval status:', error)
    return []
  }
}
