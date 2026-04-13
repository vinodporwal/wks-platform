import React from 'react'
import { Tooltip } from '@mui/material'
import './../../../css/WorkflowTimeline.css'

const WorkflowTimeline = ({ steps, currentStep }) => {
  const getStepStatus = (index) => {
    if (index < currentStep) return 'completed'
    if (index === currentStep) return 'active'
    return 'pending'
  }

  const getTooltipContent = (step, status, isLastStep) => {
    // Special message for last step (Cluster Head) when completed
    const showFinalizedMessage =
      isLastStep &&
      status === 'completed' &&
      step.role?.toLowerCase().includes('cluster head')

    // For parallel steps, show detailed status of each sub-step
    if (step.isParallel && step.parallelSteps) {
      return (
        <div style={{ padding: '4px' }}>
          <div style={{ fontWeight: 600, marginBottom: '8px' }}>
            {step.label}
          </div>
          {step.parallelSteps.map((pStep, idx) => (
            <div key={idx} style={{ marginBottom: '6px', fontSize: '0.75rem' }}>
              <div style={{ fontWeight: 600, color: '#fff' }}>{pStep.role}</div>
              <div
                style={{
                  color:
                    pStep.status === 'completed'
                      ? '#a7f3d0'
                      : pStep.status === 'active'
                        ? '#93c5fd'
                        : '#d1d5db',
                }}
              >
                {pStep.status === 'completed'
                  ? '✓ Completed'
                  : pStep.status === 'active'
                    ? '⏳ Pending'
                    : '○ Pending'}
              </div>
            </div>
          ))}
        </div>
      )
    }

    return (
      <div style={{ padding: '4px' }}>
        <div style={{ fontWeight: 600, marginBottom: '8px' }}>{step.label}</div>
        <div style={{ fontWeight: 600, marginBottom: '4px' }}>{step.role}</div>
        {showFinalizedMessage && (
          <div
            style={{
              fontSize: '0.75rem',
              color: '#a7f3d0',
              marginTop: '4px',
            }}
          >
            Finalised data for PIMS Output
          </div>
        )}
        {step.completedDate && status === 'completed' && (
          <div style={{ fontSize: '0.75rem' }}>
            <div style={{ color: '#a7f3d0' }}>{step.completedDate}</div>
            {step.completedBy && (
              <div style={{ color: '#d1d5db', marginTop: '2px' }}>
                by {step.completedBy}
              </div>
            )}
          </div>
        )}
      </div>
    )
  }

  const getStepLabel = (step) => {
    if (step.isParallel && step.parallelSteps) {
      return (
        <>
          {step.parallelSteps.map((pStep, idx) => (
            <React.Fragment key={idx}>
              {idx > 0 && (
                <div style={{ textAlign: 'center', margin: '2px 0' }}>&</div>
              )}
              <div>{pStep.role}</div>
            </React.Fragment>
          ))}
        </>
      )
    }
    return step.role
  }

  return (
    <div className='workflow-timeline'>
      <div className='timeline-container'>
        {steps.map((step, index) => {
          const status = step.status || getStepStatus(index)
          const isLastStep = index === steps.length - 1

          return (
            <div key={step.id} className='timeline-step-wrapper'>
              <div className={`timeline-step ${status}`}>
                {/* Step Circle/Icon with Tooltip */}
                <Tooltip
                  title={getTooltipContent(step, status, isLastStep)}
                  arrow
                  placement='top'
                >
                  <div className='step-indicator'>
                    <div className='step-circle'>
                      {status === 'completed' ? (
                        <svg
                          className='check-icon'
                          viewBox='0 0 24 24'
                          fill='none'
                          stroke='currentColor'
                          strokeWidth='3'
                        >
                          <polyline points='20 6 9 17 4 12' />
                        </svg>
                      ) : (
                        <span className='step-number'>{index + 1}</span>
                      )}
                    </div>
                  </div>
                </Tooltip>

                {/* Connecting Line */}
                {!isLastStep && (
                  <div
                    className={`step-connector ${status === 'completed' ? 'completed' : ''}`}
                  />
                )}

                {/* Step Details - Only Label */}
                <div className='step-details'>
                  <div className='step-label'>{getStepLabel(step)}</div>
                </div>
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}

export default WorkflowTimeline
