/**
 * Determine the submission status of a single sub-step (Plant Manager or CTS Tech Manager)
 * for Step 1's parallel block.
 *
 * Logic:
 *  - If a selectedPlant is provided  → check only that plant's entry in the status map.
 *  - If no selectedPlant (site-level)  → ALL plants must be true for it to be 'completed'.
 *
 * @param {Object|null} statusJson  - e.g. { "CDU-1": true, "CDU-2": false }
 * @param {string|null} selectedPlant
 * @returns {'completed'|'active'}
 */
const getParallelSubStepStatus = (statusJson, selectedPlant) => {
  if (!statusJson) return 'active'

  if (selectedPlant) {
    return statusJson[selectedPlant] === true ? 'completed' : 'active'
  }

  // Site-level: every plant must be submitted
  const allDone = Object.values(statusJson).every((v) => v === true)
  return allDone ? 'completed' : 'active'
}

/**
 * Transform approval status API response to workflow timeline steps
 * @param {Object} approvalStatusJson        - Parsed JSON from approvalStatus.value
 * @param {string|null} selectedPlant        - Currently selected plant name (null = site level)
 * @param {Object|null} submissionStatusJson    - Parsed JSON from submissionStatus.value
 * @param {Object|null} ctsTechSubmissionStatusJson - Parsed JSON from ctsTechSubmissionStatus.value
 * @returns {Array} Timeline steps array
 */
export const transformApprovalStatusToSteps = (
  approvalStatusJson,
  selectedPlant,
  submissionStatusJson,
  ctsTechSubmissionStatusJson,
) => {
  // Fixed sequence of workflow steps
  // Step 1 is a PARALLEL step: Plant Manager + CTS Tech Manager must both submit.
  const workflowSequence = [
    {
      id: 1,
      label: 'Step 1',
      isParallel: true,
      parallelSteps: [
        {
          id: '1a',
          role: 'Plant Manager',
          statusJson: submissionStatusJson,
        },
        {
          id: '1b',
          role: 'CTS Tech Manager',
          statusJson: ctsTechSubmissionStatusJson,
        },
      ],
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

  // Resolve each parallel sub-step status
  const parallelStepsStatus = workflowSequence[0].parallelSteps.map(
    (pStep) => ({
      id: pStep.id,
      role: pStep.role,
      status: getParallelSubStepStatus(pStep.statusJson, selectedPlant),
    }),
  )

  // Step 1 is completed only when BOTH sub-steps are completed
  const isStep1Completed = parallelStepsStatus.every(
    (pStep) => pStep.status === 'completed',
  )

  // Find the first unfinished step in the remaining sequence
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
    if (index === 0) {
      // Always a parallel step
      const overallStatus = isStep1Completed
        ? 'completed'
        : 'active'

      return {
        id: step.id,
        label: step.label,
        isParallel: true,
        parallelSteps: parallelStepsStatus,
        status: overallStatus,
      }
    }

    let status = 'pending'
    if (!isStep1Completed) {
      status = 'pending'
    } else if (approvalStatusJson[step.key] === true) {
      status = 'completed'
    } else if (index === firstFalseIndex) {
      status = 'active'
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
 * Parse and transform the API response variables array into workflow timeline steps.
 * @param {Array} apiResponse - The full variables array from the process instance
 * @param {string|null} selectedPlant - Currently selected plant name (null = site level)
 * @returns {Array} Timeline steps array
 */
export const parseApprovalStatusResponse = (apiResponse, selectedPlant) => {
  try {
    const approvalStatusItem = apiResponse.find(
      (item) => item.name === 'approvalStatus',
    )

    if (!approvalStatusItem) {
      // approvalStatus not present yet (e.g., during initial load)
      return []
    }

    const submissionStatusItem = apiResponse.find(
      (item) => item.name === 'submissionStatus',
    )
    const ctsTechSubmissionStatusItem = apiResponse.find(
      (item) => item.name === 'ctsTechSubmissionStatus',
    )

    const approvalStatusJson = JSON.parse(approvalStatusItem.value)
    const submissionStatusJson = submissionStatusItem
      ? JSON.parse(submissionStatusItem.value)
      : null
    const ctsTechSubmissionStatusJson = ctsTechSubmissionStatusItem
      ? JSON.parse(ctsTechSubmissionStatusItem.value)
      : null

    return transformApprovalStatusToSteps(
      approvalStatusJson,
      selectedPlant,
      submissionStatusJson,
      ctsTechSubmissionStatusJson,
    )
  } catch (error) {
    console.error('Error parsing approval status:', error)
    return []
  }
}
