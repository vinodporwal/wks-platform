/* eslint-disable no-unused-vars */
import React, { useState, useEffect } from 'react'
import { Form } from '@formio/react'
import CloseIcon from '@mui/icons-material/Close'
import MoreVertIcon from '@mui/icons-material/MoreVert'
import { Grid } from '@mui/material'
import AppBar from '@mui/material/AppBar'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogTitle from '@mui/material/DialogTitle'
import Divider from '@mui/material/Divider'
import IconButton from '@mui/material/IconButton'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import Menu from '@mui/material/Menu'
import MenuItem from '@mui/material/MenuItem'
import Slide from '@mui/material/Slide'
import Step from '@mui/material/Step'
import StepLabel from '@mui/material/StepLabel'
import Stepper from '@mui/material/Stepper'
import Tab from '@mui/material/Tab'
import Tabs from '@mui/material/Tabs'
import Toolbar from '@mui/material/Toolbar'
import Typography from '@mui/material/Typography'
import { CaseStatus } from 'common/caseStatus'
import { StorageService } from 'plugins/storage'
import PropTypes from 'prop-types'
import { useTranslation } from 'react-i18next'
import { ProcessDefService } from 'services/ProcessDefService'
import { Comments } from 'views/caseComment/Comments'
import { CaseService, FormService, CaseDefService } from '../../services'
import { tryParseJSONObject } from '../../utils/jsonStringCheck'
import Documents from './Documents'
import { Snackbar, SnackbarContent, CircularProgress } from '@mui/material'
import { useNavigate } from 'react-router-dom'
import { DialogActions, DialogContent, DialogContentText } from '@mui/material'
import Config from '../../consts'
import { buildCreateUrl } from 'utils/util'

const Transition = React.forwardRef(function Transition(props, ref) {
  return <Slide direction='up' ref={ref} {...props} />
})

export const PICaseFormPage = ({ open, handleClose, aCase, keycloak }) => {
  const [caseDef, setCaseDef] = useState(null)
  const [form, setForm] = useState(null)
  const [formData, setFormData] = useState(null)
  const [comments, setComments] = useState(null)
  const [documents, setDocuments] = useState(null)
  const [mainTabIndex, setMainTabIndex] = useState(0)
  // const [rightTabIndex, setRightTabIndex] = useState(0)
  const [activeStage, setActiveStage] = React.useState(0)
  const [stages, setStages] = useState([])
  const { t } = useTranslation()

  const [anchorEl, setAnchorEl] = React.useState(null)
  const isMenuOpen = Boolean(anchorEl)

  const [openProcessesDialog, setOpenProcessesDialog] = useState(false)
  const [manualInitProcessDefs, setManualInitProcessDefs] = useState([])

  // const [isFollowing, setIsFollowing] = useState(false)
  const [isFormData, setIsFormData] = useState(false)

  const navigate = useNavigate()
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarMessages, setSnackbarMessages] = useState([])
  const [currentParams, setCurrentParams] = useState([])
  const [lastCreatedCase, setLastCreatedCase] = useState(null)
  const [snackOpen, setSnackOpen] = useState(false)
  const [formStructure, setFormStructure] = useState(null)
  const [isConfirmationOpen, setIsConfirmationOpen] = useState(false)
  const [apiBody, setApiBody] = useState(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    localStorage.setItem('aCaseOwnerEmail', JSON.stringify(aCase.owner?.email))
    getCaseInfo(aCase)
  }, [open, aCase])

  useEffect(() => {
    if (activeStage) {
      const stage = caseDef.stages.find((o) => o.name === activeStage)
      const stageProcesses = stage ? stage.processesDefinitions : []
      const autoStartProcesses = stageProcesses
        ? stageProcesses.filter((o) => o.autoStart === false)
        : undefined
      setManualInitProcessDefs(autoStartProcesses)
    }
  }, [activeStage])

  const handleMenuOpen = (event) => {
    setAnchorEl(event.currentTarget)
  }

  const handleMenuClose = () => {
    setAnchorEl(null)
  }

  const handleCloseSnack = () => {
    setSnackOpen(false)
  }

  const snackAction = lastCreatedCase && (
    <React.Fragment>
      <Button
        color='primary'
        size='small'
        onClick={() => {
          navigate(`/case-list/cms${currentParams}`)
          handleCloseSnack()
        }}
      >
        {lastCreatedCase.caseNo}
      </Button>
      <IconButton
        size='small'
        aria-label='close'
        color='inherit'
        onClick={handleCloseSnack}
      >
        <CloseIcon fontSize='small' />
      </IconButton>
    </React.Fragment>
  )

  const getCaseInfo = async (aCase) => {
    await loadOptions(keycloak);
    CaseService.getCaseDefinitionsById(keycloak, aCase.caseDefinitionId)
      .then(async (data) => {
        setCaseDef(data)
        setStages(
          data.stages.sort((a, b) => a.index - b.index).map((o) => o.name),
        )

        const formData = await FormService.getByKey(keycloak, data.formKey)
        setFormStructure(formData)
        let updatedFormStructure = null
        if (formData && formData.structure && formData.structure.components) {
          updatedFormStructure = { ...formData }
        } else {
          console.error('Form structure or components are undefined.')
        }
        setIsFormData(true)

        const caseData = await CaseService.getCaseById(
          keycloak,
          aCase.businessKey,
        )

        aCase.documents = caseData?.documents || []
        aCase.comments = caseData?.comments || []

        // aCase.stage = caseData?.stage || "Stage 0";
        return { caseData: aCase, updatedFormStructure }
      })
      .then(({ caseData, updatedFormStructure }) => {
        const isDraft = caseData?.isDraft === 'y'

        // Disable fields (with proper null checks)
        const level1 = updatedFormStructure.structure.components[0]
        if (!isDraft) {
          const analysis = level1.components?.[2] ?? null;
          const recommendation = level1.components?.[3] ?? null;
          const siteRecommendation = level1.components?.[4] ?? null;

          // Disable all components in level1 except recommendation and caseDetails
          level1.components?.forEach((component) => {
            if (
              component.id !== recommendation?.id &&
              component.id !== siteRecommendation?.id && 
              component.id !== analysis?.id
            ) {
              component.disabled = true;
            }
          });
        }        

        if (level1 && level1.components) {
          const level2 = level1.components[0]
          const level7 =
            level1.components.length > 6 ? level1.components[6] : null
          if (level2 && level2.components) {
            const caseDescriptionField =
              level2.components.length > 1 ? level2.components[1] : null
            if (caseDescriptionField) {
              caseDescriptionField.disabled = false
            }

            if (level2.components[0] && level2.components[0].columns) {
              const caseNo =
                level2.components[0].columns.length > 1
                  ? level2.components[0].columns[0].components[0]
                  : null

              if (caseNo) {
                caseNo.calculateValue = `value = ${aCase.caseNo}`
              }

              const caseTitleField =
                level2.components[0].columns.length > 1
                  ? level2.components[0].columns[1].components[0]
                  : null
              if (caseTitleField) {
                caseTitleField.disabled = true
              }

              // Disable case status if currentUser is different than case owner
              const level3 = level1.components?.[3] ?? null;

              const caseStatus = level3?.components?.[1]?.columns?.[1]?.components?.[0] ?? null;

              const caseOwner = caseData?.owner?.id;
              const currentUser = keycloak?.subject;

              if (caseOwner !== currentUser && caseStatus) {
                caseStatus.disabled = true;
              }
            }

            if (level7 && level7.columns) {
              const saveAsDraft =
                level7.columns.length > 2
                  ? level7.columns[2].components[0]
                  : null
              if (saveAsDraft) {
                saveAsDraft.hidden = true;
              }
              
              const createButton =
                level7.columns.length > 2
                  ? level7.columns[2].components[1]
                  : null
              if (createButton) {
                createButton.hidden = true
              }

              const saveButton =
                level7.columns.length > 3
                  ? level7.columns[3].components[0]
                  : null
              if (saveButton) {
                  saveButton.hidden = true;
              }
            }
          }
        }

        setForm({
          ...updatedFormStructure,
        })

        setComments(
          caseData?.comments?.sort(
            (a, b) =>
              new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
          ),
        )
        setDocuments(caseData?.documents)
        setFormData({
          data: caseData.attributes.reduce(
            (obj, item) =>
              Object.assign(obj, {
                [item.name]: tryParseJSONObject(item.value)
                  ? JSON.parse(item.value)
                  : item.value,
              }),
            {},
          ),
          metadata: {},
          isValid: true,
        })
        setActiveStage(caseData.stage)
      })
      .catch((err) => {
        console.error(err.message)
      })
  }

  const onSave = () => {
    setLoading(true)
    const requiredFields = []

    const missingFields = requiredFields.filter(
      (field) => !formData.data.container[field],
    )

    if (missingFields.length > 0) {
      setSnackbarMessages(['Please fill in all required fields.'])
      setSnackbarOpen(true)
      setLoading(false)
      return
    }

    const caseAttributes = Object.keys(formData.data).map((key) => ({
      name: key,
      value:
        typeof formData.data[key] !== 'object'
          ? formData.data[key]
          : JSON.stringify(formData.data[key]),
      type: typeof formData.data[key] !== 'object' ? 'String' : 'Json',
    }))

    CaseService.createCase(
      keycloak,
      JSON.stringify({
        caseDefinitionId: aCase.caseDefinitionId,
        caseNo: aCase.caseNo,
        attributes: caseAttributes,
        caseUrl: buildCreateUrl(window.location.href, aCase.caseDefinitionId),
        businessKey: aCase.businessKey,
      }),
    )
      .then((data) => {
        const businessKey = data.businessKey

        return CaseService.savePICase(
          keycloak,
          JSON.stringify({
            caseDefinitionId: aCase.caseDefinitionId,
            caseNo: aCase.caseNo,
            isDraft: 'n',
            businessKey: businessKey,
            attributes: caseAttributes,
            caseUrl: buildCreateUrl(window.location.href, aCase.caseDefinitionId),
            assignedTo: {emailId: formData.data.container.caseCreatedBy}
          }),
        )
      })
      .then((data) => {
        setLastCreatedCase(data)
        setSnackOpen(true)
        setTimeout(() => {
          window.location.href = data.caseUrl;
        }, 1000)
      })
      .catch((err) => {
        console.error(err.message)
      })
      .finally(() => {
        setLoading(false) // Stop loading after the process finishes
      })
  }

  const onAnalysisSave = () => {
    setLoading(true)
    const requiredFields = ['analysisDesc']
    const missingFields = requiredFields.filter(
      (field) => !formData.data.container[field],
    )
    if (missingFields.length > 0) {
      setSnackbarMessages(['Please fill in all required fields.'])
      setSnackbarOpen(true)
      setLoading(false)
      return
    }

    const actionAssignedId = getcaseStatusValue('Under Analysis');

    formData.data.container.caseStatus = actionAssignedId;
    
    const caseAttributes = Object.keys(formData.data).map((key) => ({
      name: key,
      value:
        typeof formData.data[key] !== 'object'
          ? formData.data[key]
          : JSON.stringify(formData.data[key]),
      type: typeof formData.data[key] !== 'object' ? 'String' : 'Json',
    }))
    CaseService.createCase(
      keycloak,
      JSON.stringify({
        caseDefinitionId: aCase.caseDefinitionId,
        caseNo: aCase.caseNo,
        owner: {
          id: keycloak.subject || '',
          name: keycloak.idTokenParsed.name || '',
          email: keycloak.idTokenParsed.email || '',
          phone: keycloak.idTokenParsed.phone || '1234567890',
        },
        attributes: caseAttributes,
        caseUrl: buildCreateUrl(window.location.href),
        businessKey: aCase.businessKey,
      }),
    )
      .then((data) => {
        const businessKey = data.businessKey

        return CaseService.saveCMSAnalysis(
          keycloak,
          JSON.stringify({
            caseDefinitionId: aCase.caseDefinitionId,
            isDraft: aCase.isDraft,
            businessKey: businessKey,
            owner: {
              id: keycloak.subject || '',
              name: keycloak.idTokenParsed.name || '',
              email: keycloak.idTokenParsed.email || '',
              phone: keycloak.idTokenParsed.phone || '',
            },
            attributes: caseAttributes,
            caseUrl: buildCreateUrl(window.location.href),
            assignedTo: {emailId: formData.data.container.caseCreatedBy}
          }),
        )
      })
      .then((data) => {
        setLastCreatedCase(data)
        setSnackOpen(true)
        setTimeout(() => {
          window.location.href = data.caseUrl;
          // handleClose()
        }, 1000)
      })
      .catch((err) => {
        console.error(err.message)
      })
      .finally(() => {
        setLoading(false) // Stop loading after the process finishes
      })
  }

  const onSubmitRecommendation = (event) => {
    let updatedFormData = JSON.parse(JSON.stringify(formData))
    setFormData(updatedFormData)

    const {
      recommendationReviewer,
      recommendationAssignedTo2,
      recommendationHeadline,
      recommendationTargetCompletionDate1,
      recommendationDescription1,
      equipmentFunctionLocation,
      RecommendationConfirmSAP3,
    } = event.data

    const missingFields = []
    if (!recommendationReviewer) 
      missingFields.push('Recommendation Reviewer')
    if (!recommendationAssignedTo2)
      missingFields.push('Recommendation Assigned To')
    if (!recommendationHeadline) 
      missingFields.push('Recommendation Headline')
    if (!recommendationTargetCompletionDate1)
      missingFields.push('Target Completion Date')
    // if (!equipmentFunctionLocation)
    //   missingFields.push('Equipment Function Location')

    if (missingFields.length > 0) {
      setSnackbarMessages(missingFields)
      setSnackbarOpen(true)
      setTimeout(() => {
        setSnackbarOpen(false)
      }, 2000)
      return
    }

    setSnackbarMessages([])

    const apiBodyData = {
      recommendationHeadline,
      recommendationDescription1,
      recommendationAssignedTo2,
      equipmentFunctionLocation,
      recommendationTargetCompletionDate1,
      recommendationReviewer,
      RecommendationConfirmSAP3,
      deleteRowButton4: false,
      RecommendationSubmit3: false,
      caseNo: aCase?.caseNo,
      createdBy: keycloak.idTokenParsed.sub,
    }
    setApiBody(apiBodyData)
    setIsConfirmationOpen(true)
  }

  const submitRecommendation = async () => {
    try {
      const response = await CaseService.savePIRecommendation(keycloak, apiBody)
      if(response.status !== 500){
      setSnackbarMessages(['Recommendation submitted successfully'])
      setSnackbarOpen(true)
      setIsConfirmationOpen(false)
      setTimeout(() => {
        window.location.href = response.caseUrl;
        // window.location.reload()
      }, 1000)
      }else{
        setIsConfirmationOpen(false)
        console.error('Error submitting recommendation:', response)
        console.error('Error submitting recommendation:', JSON.stringify(response.body))
        setSnackbarMessages(['Error submitting recommendation'])
        setSnackbarOpen(true)
      }
      // getCaseInfo(aCase)
    } catch (error) {
      console.error('Error submitting recommendation:', error)
      setSnackbarMessages(['Error submitting recommendation'])
      setSnackbarOpen(true)
    }
  }

  const onSubmitCMSRecommendation = async (event) => {
    setLoading(true)
    let updatedFormData = JSON.parse(JSON.stringify(formData))
    setFormData(updatedFormData)

    const {
      recommendationAssignedTo,
      recommendationCategory,
      recommendationTargetDate,
      recommendationDescription,
    } = event.data

    const missingFields = []
    if (!recommendationAssignedTo)
      missingFields.push('Recommendation Assigned To')
    if (!recommendationCategory) 
      missingFields.push('Recommendation Category')
    if (!recommendationTargetDate)
      missingFields.push('Target Date')
    if (!recommendationDescription)
      missingFields.push('Recommended Actions')

    if (missingFields.length > 0) {
      setLoading(false)
      setSnackbarMessages(missingFields)
      setSnackbarOpen(true)
      setTimeout(() => {
        setSnackbarOpen(false)
      }, 2000)
      return
    }

    setSnackbarMessages([])

    const apiBodyData = {
      recommendationCategory: recommendationCategory,
      recommendationDescription1: recommendationDescription,
      recommendationAssignedTo2: recommendationAssignedTo,
      recommendationTargetCompletionDate1: recommendationTargetDate,
      deleteRowButton4: false,
      recommendationSubmit: false,
      caseNo: aCase?.caseNo,
      createdBy: keycloak.idTokenParsed.sub,
    }

    try {
      const response = await CaseService.savePIRecommendation(keycloak, apiBodyData)
      if(response.status !== 500){
        setLoading(false)
        setSnackbarMessages(['CMS Recommendation submitted successfully'])
        setSnackbarOpen(true)
        setTimeout(() => {
          window.location.href = response.caseUrl;
        }, 1000)
      }else{
        setLoading(false)
        console.error('Error submitting recommendation:', response)
        console.error('Error submitting recommendation:', JSON.stringify(response.body))
        setSnackbarMessages(['Error submitting recommendation'])
        setSnackbarOpen(true)
      }
    } catch (error) {
      setLoading(false)
      console.error('Error submitting recommendation:', error)
      setSnackbarMessages(['Error submitting recommendation'])
      setSnackbarOpen(true)
    }
  }

  const onCaseAssignedToSubmit = async (event) => {
    setLoading(true)
    
    const caseAssignedTo = formData.data.container.caseAssignedTo;
    if (caseAssignedTo.length === 0) {
      setSnackbarMessages(['Please assign user to case.'])
      setSnackbarOpen(true)
      setLoading(false)
      return
    }

    const actionAssignedId = getcaseStatusValue('Assigned');

    formData.data.container.caseStatus = actionAssignedId;

    const caseAttributes = Object.keys(formData.data).map((key) => ({
      name: key,
      value:
        typeof formData.data[key] !== 'object'
          ? formData.data[key]
          : JSON.stringify(formData.data[key]),
      type: typeof formData.data[key] !== 'object' ? 'String' : 'Json',
    }))

    CaseService.createCase(
      keycloak,
      JSON.stringify({
        caseDefinitionId: aCase.caseDefinitionId,
        caseNo: aCase.caseNo,
        owner: {
          id: keycloak.subject || '',
          name: keycloak.idTokenParsed.name || '',
          email: keycloak.idTokenParsed.email || '',
          phone: keycloak.idTokenParsed.phone || '1234567890',
        },
        attributes: caseAttributes,
        caseUrl: buildCreateUrl(window.location.href),
        businessKey: aCase.businessKey,
      }),
    )
      .then((data) => {
        const businessKey = data.businessKey

        return CaseService.saveCMSCaseAssignedTo(
          keycloak,
          JSON.stringify({
            caseDefinitionId: aCase.caseDefinitionId,
            isDraft: aCase.isDraft,
            businessKey: businessKey,
            owner: {
              id: keycloak.subject || '',
              name: keycloak.idTokenParsed.name || '',
              email: keycloak.idTokenParsed.email || '',
              phone: keycloak.idTokenParsed.phone || '',
            },
            attributes: caseAttributes,
            caseUrl: buildCreateUrl(window.location.href),
            assignedTo: {emailId: formData.data.container.caseCreatedBy}
          }),
        )
      })
      .then((data) => {
        setLastCreatedCase(data)
        setSnackOpen(true)
        setTimeout(() => {
          window.location.href = data.caseUrl;
          // handleClose()
        }, 1000)
      })
      .catch((err) => {
        console.error(err.message)
      })
      .finally(() => {
        setLoading(false) // Stop loading after the process finishes
      })
  }

  const onActionSubmit = async (event) => {
    setLoading(true)

    const actionDetails = formData.data.container.actionDetails;
    if (actionDetails === "" || actionDetails === null) {
      setSnackbarMessages(['Please fill action details.'])
      setSnackbarOpen(true)
      setLoading(false)
      return
    }

    const actionAssignedId = getcaseStatusValue('Action Completed');

    formData.data.container.caseStatus = actionAssignedId;

    const caseAttributes = Object.keys(formData.data).map((key) => ({
      name: key,
      value:
        typeof formData.data[key] !== 'object'
          ? formData.data[key]
          : JSON.stringify(formData.data[key]),
      type: typeof formData.data[key] !== 'object' ? 'String' : 'Json',
    }))

    CaseService.createCase(
      keycloak,
      JSON.stringify({
        caseDefinitionId: aCase.caseDefinitionId,
        caseNo: aCase.caseNo,
        owner: {
          id: keycloak.subject || '',
          name: keycloak.idTokenParsed.name || '',
          email: keycloak.idTokenParsed.email || '',
          phone: keycloak.idTokenParsed.phone || '1234567890',
        },
        attributes: caseAttributes,
        caseUrl: buildCreateUrl(window.location.href),
        businessKey: aCase.businessKey,
      }),
    )
      .then((data) => {
        const businessKey = data.businessKey

        return CaseService.submitCaseAction(
          keycloak,
          JSON.stringify({
            caseDefinitionId: aCase.caseDefinitionId,
            isDraft: aCase.isDraft,
            businessKey: businessKey,
            owner: {
              id: keycloak.subject || '',
              name: keycloak.idTokenParsed.name || '',
              email: keycloak.idTokenParsed.email || '',
              phone: keycloak.idTokenParsed.phone || '',
            },
            attributes: caseAttributes,
            caseUrl: buildCreateUrl(window.location.href),
            assignedTo: {emailId: formData.data.container.caseCreatedBy}
          }),
        )
      })
      .then((data) => {
        setLastCreatedCase(data)
        setSnackOpen(true)
        setTimeout(() => {
          window.location.href = data.caseUrl;
          // handleClose()
        }, 1000)
      })
      .catch((err) => {
        console.error(err.message)
      })
      .finally(() => {
        setLoading(false) // Stop loading after the process finishes
      })
  }

  const onCaseClosureSubmit = async (event) => {
    setLoading(true)
    const requiredFields = ['actionsCompleted', 'notes']
    const missingFields = requiredFields.filter(
      (field) => !formData.data.container[field],
    )
    if (missingFields.length > 0) {
      setSnackbarMessages(['Please fill in all required fields.'])
      setSnackbarOpen(true)
      setLoading(false)
      return
    }


    const actionAssignedId = getcaseStatusValue('Closed');

    formData.data.container.caseStatus = actionAssignedId;

    const caseAttributes = Object.keys(formData.data).map((key) => ({
      name: key,
      value:
        typeof formData.data[key] !== 'object'
          ? formData.data[key]
          : JSON.stringify(formData.data[key]),
      type: typeof formData.data[key] !== 'object' ? 'String' : 'Json',
    }))

    CaseService.createCase(
      keycloak,
      JSON.stringify({
        caseDefinitionId: aCase.caseDefinitionId,
        caseNo: aCase.caseNo,
        owner: {
          id: keycloak.subject || '',
          name: keycloak.idTokenParsed.name || '',
          email: keycloak.idTokenParsed.email || '',
          phone: keycloak.idTokenParsed.phone || '1234567890',
        },
        attributes: caseAttributes,
        caseUrl: buildCreateUrl(window.location.href),
        businessKey: aCase.businessKey,
      }),
    )
      .then((data) => {
        const businessKey = data.businessKey

        return CaseService.submitCaseClosure(
          keycloak,
          JSON.stringify({
            caseDefinitionId: aCase.caseDefinitionId,
            isDraft: aCase.isDraft,
            businessKey: businessKey,
            owner: {
              id: keycloak.subject || '',
              name: keycloak.idTokenParsed.name || '',
              email: keycloak.idTokenParsed.email || '',
              phone: keycloak.idTokenParsed.phone || '',
            },
            attributes: caseAttributes,
            caseUrl: buildCreateUrl(window.location.href),
            assignedTo: {emailId: formData.data.container.caseCreatedBy}
          }),
        )
      })
      .then((data) => {
        setLastCreatedCase(data)
        setSnackOpen(true)
        setTimeout(() => {
          window.location.href = data.caseUrl;
          // handleClose()
        }, 1000)
      })
      .catch((err) => {
        console.error(err.message)
      })
      .finally(() => {
        setLoading(false) // Stop loading after the process finishes
      })
  }

  const openPIHome = async (evnet) => {
    const piHomeURL = 'https://portal-pimvo1oidcsit1.dev.forge.connected.honeywell.com/HCP/login?contentId=66c4da87-db90-d362-2197-1cf5423881df';
    window.open(piHomeURL, '_blank')
  }

  const handleMainTabChanged = async (event, newValue) => {
    if(newValue == 1){
      const caseData = await CaseService.getCaseById(
        keycloak,
        aCase.businessKey,
      )
      const documents = caseData?.documents || []
      setDocuments(documents)
    }
    setMainTabIndex(newValue)
  }

  const handleUpdateCaseStatus = (newStatus) => {
    CaseService.patch(
      keycloak,
      aCase.businessKey,
      JSON.stringify({
        status: newStatus,
      }),
    )
      .then(() => {
        handleClose()
      })
      .catch((err) => {
        console.error(err.message)
      })
  }

  const handleOpenProcessesDialog = () => {
    setOpenProcessesDialog(true)
    handleMenuClose()
  }

  const handleCloseProcessesDialog = () => {
    setOpenProcessesDialog(false)
  }

  const startProcess = (key) => {
    ProcessDefService.start(keycloak, key, aCase.businessKey)

    // Close the dialog
    handleCloseProcessesDialog()
  }

  // Function to get label for a given category value from localStorage
  const getCategoryLabel = (value) => {
    // Retrieve options from localStorage
    const options = JSON.parse(localStorage.getItem('categoryOptions')) || []

    // Find the option with the matching value and return its label
    const matchingOption = options.find((option) => option.value === value)
    return matchingOption ? matchingOption.label : value // Fallback to value if no match is found
  }

  const getCaseCauseDescriptionLabel = (value, categoryId) => {
    // Retrieve options for the specific category from localStorage
    const options =
      JSON.parse(
        localStorage.getItem(`caseCauseDescriptionOptions_${categoryId}`),
      ) || []

    // Find the option with the matching value and return its label
    const matchingOption = options.find((option) => option.value === value)
    return matchingOption ? matchingOption.label : value // Fallback to value if no match is found
  }

  // Function to get label for a given fault category value from localStorage
  const getFaultCategoryLabel = (value) => {
    // Retrieve options from localStorage
    const options =
      JSON.parse(localStorage.getItem('faultCategoryOptions')) || []

    // Find the option with the matching value and return its label
    const matchingOption = options.find((option) => option.value === value)
    return matchingOption ? matchingOption.label : value // Fallback to value if no match is found
  }

  // Function to get label for a given fault category value from localStorage
  const getcaseStatusLabel = (value) => {
    // Retrieve options from localStorage
    const options = JSON.parse(localStorage.getItem('caseStatusOptions')) || []

    // Find the option with the matching value and return its label
    const matchingOption = options.find((option) => option.value === value)
    return matchingOption ? matchingOption.label : value // Fallback to value if no match is found
  }

  const getcaseStatusValue = (label) => {
    // Retrieve options from localStorage
    const options = JSON.parse(localStorage.getItem('caseStatusOptions')) || []

    // Find the option with the matching value and return its label
    const matchingOption = options.find((option) => option.label === label)
    return matchingOption ? matchingOption.value : label // Fallback to value if no match is found
  }

  const getEquipmentFunctionLocationLabel = (id) => {
    const storedLocations = localStorage.getItem('functionalLocationOptions')

    if (!storedLocations) {
      console.error('No functionalLocationOptions found in localStorage')
      return id
    }

    let locations = []

    try {
      locations = JSON.parse(storedLocations)
    } catch (error) {
      console.error(
        'Error parsing functionalLocationOptions from localStorage:',
        error,
      )
      return id
    }

    const location = locations.find((location) => location.value === id)
    return location ? location.label : id
  }

  // Function to dynamically create labelMap from the form structure
  const createLabelMapFromStructure = (structure) => {
    const labelMap = {}

    const extractLabels = (components) => {
      if (!components || !Array.isArray(components)) return

      components.forEach((component) => {
        if (component.key && component.label) {
          labelMap[component.key] = component.label
        }

        // Recursively check nested components
        if (component.components) {
          extractLabels(component.components)
        }

        // Handle columns in case they contain components
        if (component.columns) {
          component.columns.forEach((col) => {
            if (col.components) {
              extractLabels(col.components)
            }
          })
        }
      })
    }

    // Check for nested structure and extract components from it
    const mainComponents =
      structure.components ||
      (structure.structure && structure.structure.components)
    if (mainComponents) {
      extractLabels(mainComponents)
    }

    return labelMap
  }

  // Function to format data grids in a 2-column layout without colons in labels, skipping specific fields
  const formatDataGrid = (dataGrid, getLabel) => {
    if (!dataGrid || dataGrid.length === 0) return '<p>No data available</p>'

    const fieldsToSkip = [
      // 'textField1',
      'RecommendationSubmit',
      'recommendationAssignedTo1',
      'deleteRowButton4',
      'RecommendationSubmit3',
      'deleteRowButton5',
      'siteRecommendationSubmit'
    ] // Add any keys you want to skip here

    return dataGrid
      .map((item) => {
        return `
      <div style="display: flex; flex-wrap: wrap; border: 1px solid #ccc; padding: 10px; margin-bottom: 5px;">
        ${Object.entries(item)
          .map(([key, value]) =>
            fieldsToSkip.includes(key)
              ? ''
              : `
            <div style="flex: 1 1 45%; border: 1px solid #ccc; margin: 5px; padding: 10px;">
              <p style="font-weight: bold; margin: 0;">${getLabel(key)}</p>
              <p style="margin: 0;">
                ${key === 'equipmentFunctionLocation' ? getEquipmentFunctionLocationLabel(value) : value || ''}
              </p>
            </div>
        `,
          )
          .join('')}
      </div>
    `
      })
      .join('')
  }

  const generatePrintContent = (aCase, structure) => {
    const containerData = JSON.parse(
      aCase.attributes.find((attr) => attr.name === 'container').value,
    )
    const labelMap = createLabelMapFromStructure(structure)
    const getLabel = (key) => labelMap[key] || key || ''

    const uploadedFiles = aCase.documents || []

    let content = `
    <div style="font-family: Arial, sans-serif; padding: 20px; border: 1px solid #333;">
      <div style="display: flex; align-items: center; justify-content: center; margin-bottom: 20px;">
        <h2 style="text-align: center; margin: 0;">PI Case Management System</h2>
      </div>


      <!-- Case Information Panel -->
      <div style="border: 1px solid #333; border-radius: 5px; margin-bottom: 20px;">
        <h3 style="background-color: #333; color: #fff; padding: 10px; margin-left: 1px; margin-right: 1px;">Case Information</h3>
        <div style="padding: 10px;">
          <p><strong>${getLabel('caseNo')}</strong>: ${aCase.caseNo}</p>
          <p><strong>${getLabel('caseTitle')}</strong>: ${containerData.caseTitle}</p>
          <p><strong>${getLabel('caseAssignedTo')}</strong>: ${containerData.caseCreatedBy}</p>
          <p><strong>${getLabel('faultCategory')}</strong>: ${containerData.caseCategory?.toUpperCase()}</p>
          <p><strong>${getLabel('caseDescription')}</strong>: ${containerData.caseDescription}</p>
          <p><strong>${getLabel('createdOn')}</strong>: ${new Date(containerData.createdOn).toLocaleDateString()}</p>
          <p><strong>${getLabel('dueDate')}</strong>: ${containerData?.dueDate || 'N/A'}</p>
          <p><strong>${getLabel('endDate')}</strong>: ${containerData?.endDate || 'N/A'}</p>
        </div>
      </div>

      <!-- KPI Information Panel -->
      <div style="border: 1px solid #333; border-radius: 5px; margin-bottom: 20px;">
        <h3 style="background-color: #333; color: #fff; padding: 10px; margin-left: 1px; margin-right: 1px;">KPI Information</h3>
        <div style="padding: 10px;">
          <p><strong>${getLabel('kpiName')}</strong>: ${containerData.kpiName}</p>
          <p><strong>${getLabel('kpiDisplayName')}</strong>: ${containerData.kpiDisplayName}</p>
          <p><strong>${getLabel('timeVariant')}</strong>: ${containerData.timeVariant}</p>
          <p><strong>${getLabel('kpiDescription')}</strong>: ${containerData.kpiDescription}</p>
        </div>
      </div>`


    // Conditional display based on RecommendationsRadio value
    // if (containerData.RecommendationsRadio === 'no') {
      const caseCauseCategoryLabel = getCategoryLabel(
        containerData.caseCauseCategory,
      )
      const caseCauseDescriptionLabel = getCaseCauseDescriptionLabel(
        containerData.caseCauseDescription,
        containerData.caseCauseCategory,
      )
      const files = containerData.file;
      content += `
      <!-- Analysis -->
      <div style="border: 1px solid #333; border-radius: 5px; margin-bottom: 20px;">
        <h3 style="background-color: #333; color: #fff; padding: 10px; margin: 0;">Analysis</h3>
        <div style="padding: 10px;">
          <p><strong>${getLabel('caseCauseCategory')}</strong>: ${caseCauseCategoryLabel}</p>
          <p><strong>${getLabel('caseCauseDescription')}</strong>: ${caseCauseDescriptionLabel}</p>
          <p><strong>${getLabel('analysisTeam')}</strong>: ${containerData.analysisTeam?.join(', ')}</p>
          <p><strong>${getLabel('caseStatus')}</strong>: ${getcaseStatusLabel(containerData.caseStatus)}</p>
          <p><strong>${getLabel('analysisDesc')}</strong>: ${containerData.analysisDesc}</p>
        </div>`
      if (files.length > 0) {
        content += `
            <ul style="list-style: none; padding: 0; margin: 0;">
              ${files
                .map(
                  (file, index) => `
                    <li style="margin-bottom: 16px;">
                      <img 
                        src="${Config.StorageUrl}/storage/files1/cases/downloads/${encodeURIComponent(file.name)}?content-type=${encodeURIComponent(file.type)}"
                        alt="${file.name}"
                        style="max-width: 100%; height: auto;"
                      />
                    </li>
      `
                )
                .join('')}
            </ul>
        `;
      }
      content +=`</div>`      

      content += `
        <!-- GE APM Recommendations -->
        <div style="border: 1px solid #333; border-radius: 5px; margin-bottom: 20px;  padding: 20px;">
          <h3 style="background-color: #333; color: #fff; padding: 10px; margin: 0;">${getLabel('dataGrid1')}</h3>
          ${formatDataGrid(containerData.dataGrid1, getLabel)}
        </div>
        `
    
      content += `
        <!-- Site Recommendations -->
        <div style="border: 1px solid #333; border-radius: 5px; margin-bottom: 20px;  padding: 20px;">
          <h3 style="background-color: #333; color: #fff; padding: 10px; margin: 0;">${getLabel('siteRecommendations')}</h3>
          ${formatDataGrid(containerData.siteRecommendations, getLabel)}
        </div>
        `

      if (uploadedFiles.length > 0) {
        content += `
          <div style="border: 1px solid #333; padding: 20px; border-radius: 5px; margin-bottom: 20px;">
            <h3 style="margin-bottom: 10px;">Uploaded Files</h3>
            <ul style="list-style: none; padding: 0; margin: 0;">
              ${uploadedFiles
                .map(
                  (file, index) => `
                    <li style="margin-bottom: 8px; cursor: pointer; color: #007bff; text-decoration: underline;">
                      <a 
                        href="${Config.StorageUrl}/storage/files1/cases/downloads/${encodeURIComponent(file.name)}?content-type=${encodeURIComponent(file.type)}"
                        alt="${file.name}"
                        target="_blank"
                      />
                      ${file.name}
                      </a>
                    </li>
                  `
                )
                .join('')}
            </ul>
          </div>
        `;
      } else {
        content += `<p>No files uploaded.</p>`;
      }
  
    return content
  }

  // Print function
  const printCaseDetails = () => {
    const printContent = generatePrintContent(aCase, formStructure);
  
    // Open a new window for printing
    const printWindow = window.open('', '_blank');
  
    // Check if the window is successfully opened
    if (printWindow) {
      // Once the window is loaded, write content to it
      printWindow.document.write(`
        <html>
          <head>
            <title>Print Case Details</title>
          </head>
          <body>
            ${printContent}
          </body>
        </html>
      `);
  
      // Close the document to allow rendering of the content
      printWindow.document.close();
  
      // Use setTimeout to ensure content is fully loaded before calling print()
      setTimeout(() => {
        printWindow.print();
      }, 500); // 500ms delay (you can adjust this if needed)
    } else {
      console.error('Failed to open the print window.');
    }
  }  

  return (
    aCase &&
    caseDef &&
    form &&
    formData && (
      <div>
        <Dialog
          fullScreen
          open={open}
          onClose={handleClose}
          TransitionComponent={Transition}
        >
          <AppBar sx={{ position: 'relative' }}>
            <Toolbar>
              <IconButton
                edge='start'
                color='inherit'
                onClick={handleClose}
                aria-label='close'
              >
                <CloseIcon />
              </IconButton>
              <Typography sx={{ ml: 2, flex: 1 }} component='div'>
                <div>
                  {caseDef.name}: {aCase?.caseNo}
                </div>
              </Typography>
              {aCase.status === CaseStatus.WipCaseStatus.description && (
                <Button
                  color='inherit'
                  onClick={() =>
                    handleUpdateCaseStatus(
                      CaseStatus.ClosedCaseStatus.description,
                    )
                  }
                >
                  {t('pages.caseform.actions.close')}
                </Button>
              )}
              {aCase.status === CaseStatus.ClosedCaseStatus.description && (
                <React.Fragment>
                  <Button
                    color='inherit'
                    onClick={() =>
                      handleUpdateCaseStatus(
                        CaseStatus.WipCaseStatus.description,
                      )
                    }
                  >
                    {t('pages.caseform.actions.reopen')}
                  </Button>

                  <Button
                    color='inherit'
                    onClick={() =>
                      handleUpdateCaseStatus(
                        CaseStatus.ArchivedCaseStatus.description,
                      )
                    }
                  >
                    {t('pages.caseform.actions.archive')}
                  </Button>
                </React.Fragment>
              )}
              {aCase.status === CaseStatus.ArchivedCaseStatus.description && (
                <React.Fragment>
                  <Button
                    color='inherit'
                    onClick={() =>
                      handleUpdateCaseStatus(
                        CaseStatus.WipCaseStatus.description,
                      )
                    }
                  >
                    {t('pages.caseform.actions.reopen')}
                  </Button>
                </React.Fragment>
              )}
              <Button color='inherit' onClick={printCaseDetails}>
                {'Print'}
              </Button>

              <Button color='inherit' onClick={onSave}>
                {'Save'}
              </Button>
              {/* Case Actions Menu */}
              <IconButton
                edge='end'
                color='inherit'
                onClick={handleMenuOpen}
                aria-label='manual-actions'
              >
                <MoreVertIcon />
              </IconButton>
              <Menu
                anchorEl={anchorEl}
                anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
                id='manual-actions-menu'
                keepMounted
                transformOrigin={{ vertical: 'top', horizontal: 'right' }}
                open={isMenuOpen}
                onClose={handleMenuClose}
              >
                {
                  <MenuItem onClick={handleOpenProcessesDialog}>
                    {t('pages.caseform.actions.startProcess')}
                  </MenuItem>
                }
              </Menu>
            </Toolbar>
          </AppBar>

          <Box
            sx={{
              pl: 10,
              pr: 10,
              pt: 2,
              pb: 2,
              borderBottom: 1,
              borderColor: 'divider',
            }}
          >
            <Stepper
              activeStep={stages.findIndex((o) => {
                return o === activeStage
              })}
            >
              {stages.map((label) => {
                const stagesProps = {}
                const labelProps = {}
                return (
                  <Step key={label} {...stagesProps}>
                    <StepLabel {...labelProps}>{label}</StepLabel>
                  </Step>
                )
              })}
            </Stepper>
          </Box>

          <Grid container spacing={2} sx={{ paddingLeft: 1, paddingRight: 1 }}>
            <Grid item xs={12}>
              <Box>
                <Tabs value={mainTabIndex} onChange={handleMainTabChanged}>
                  <Tab
                    label={t('pages.caseform.tabs.details')}
                    {...a11yProps(0)}
                  />
                  <Tab
                    label={t('pages.caseform.tabs.attachments')}
                    {...a11yProps(1)}
                  />
                  <Tab
                    label={t('pages.caseform.tabs.comments')}
                    {...a11yProps(2)}
                  />
                </Tabs>
              </Box>
              <Box
                sx={{ border: 1, borderColor: 'divider', borderRadius: '5px' }}
              >
                <TabPanel value={mainTabIndex} index={0}>
                  {/* Case Details  */}
                  <Grid
                    container
                    spacing={2}
                    sx={{ display: 'flex', flexDirection: 'column' }}
                  >
                    <Box
                      sx={{
                        pb: 1,
                        display: 'flex',
                        flexDirection: 'row',
                      }}
                    >
                      <Typography
                        variant='h5'
                        color='textSecondary'
                        sx={{ pr: 0.5 }}
                      >
                        {form.title}
                      </Typography>
                    </Box>
                    {isFormData && (
                      <Form
                        form={form.structure}
                        submission={formData}
                        options={{
                          // readOnly: true,
                          fileService: new StorageService(),
                        }}
  
                        onCustomEvent={(event) => {
                          if (
                            event.component.key === 'recommendationSubmit'
                          ) {
                            onSubmitCMSRecommendation(event)
                          } else if (event.component.key === 'onSave') {
                            onSave()
                          } else if (event.component.key === 'analysisSubmit') {
                            onAnalysisSave()
                          } else if(event.component.key === 'siteRecommendationSubmit'){
                            onSubmitSiteRecommendation(event)
                          } else if(event.component.key === 'caseAssignedToSubmit'){
                            onCaseAssignedToSubmit(event)
                          } else if(event.component.key === 'actionsSubmit'){
                            onActionSubmit(event)
                          } else if(event.component.key === 'caseClosureSubmit'){
                            onCaseClosureSubmit(event)
                          } else if(event.component.key === 'piHomeSubmit'){
                            openPIHome(event)
                          }
                        }}
                      />
                    )}
                    <Dialog
                      open={isConfirmationOpen}
                      onClose={() => setIsConfirmationOpen(false)}
                    >
                      <DialogTitle>Confirm Submission</DialogTitle>
                      <DialogContent>
                        <DialogContentText>
                          Are you sure you want to submit this recommendation?
                        </DialogContentText>
                        {apiBody &&
                          apiBody?.RecommendationConfirmSAP3 == 'n' && (
                            <DialogContentText sx={{ color: 'red' }}>
                              Note: Create SAP Request is not selected
                            </DialogContentText>
                          )}
                      </DialogContent>
                      <DialogActions>
                        <Button
                          onClick={() => setIsConfirmationOpen(false)}
                          color='primary'
                        >
                          Cancel
                        </Button>
                        <Button
                          onClick={submitRecommendation}
                          color='primary'
                          autoFocus
                        >
                          Submit
                        </Button>
                      </DialogActions>
                    </Dialog>                    
                    <Snackbar
                      open={snackbarOpen}
                      autoHideDuration={2000}
                      onClose={() => setSnackbarOpen(false)}
                      anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
                    >
                      <SnackbarContent
                        message={
                          <div>
                            <Typography
                              variant='body2'
                              // color='white'
                              component='div'
                            >
                              {snackbarMessages.length > 1
                                ? 'The following fields are required:'
                                : snackbarMessages[0]}
                            </Typography>
                            {snackbarMessages.length > 1 &&
                              snackbarMessages.map((message, index) => (
                                <Typography
                                  key={index}
                                  variant='body2'
                                  component='div'
                                >
                                  - {message}
                                </Typography>
                              ))}
                          </div>
                        }
                        action={
                          <Button
                            color='secondary'
                            size='small'
                            onClick={() => setSnackbarOpen(false)}
                          >
                            Close
                          </Button>
                        }
                      />
                    </Snackbar>
                    <Snackbar
                      open={snackOpen}
                      autoHideDuration={2000}
                      message='Case Saved'
                      onClose={handleCloseSnack}
                      action={snackAction}
                    />
                  </Grid>
                </TabPanel>
                <TabPanel value={mainTabIndex} index={1}>
                  <Documents aCase={aCase} initialValue={documents || []} />
                </TabPanel>

                <TabPanel value={mainTabIndex} index={2}>
                  <Grid
                    container
                    spacing={2}
                    sx={{ display: 'flex', flexDirection: 'column' }}
                  >
                    <Grid item xs={12}>
                      <Comments
                        aCase={aCase}
                        getCaseInfo={getCaseInfo}
                        comments={comments ? comments : []}
                      />
                    </Grid>
                  </Grid>
                </TabPanel>
              </Box>
            </Grid>
          </Grid>
          {loading && (
            <Box
              sx={{
                position: 'fixed',
                top: 0,
                left: 0,
                width: '100vw',
                height: '100vh',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                backgroundColor: 'rgba(255, 255, 255, 0.8)',
                zIndex: 1300,
              }}
            >
              <CircularProgress color='inherit' />
            </Box>
          )}
        </Dialog>

        {manualInitProcessDefs && (
          <Dialog
            onClose={handleCloseProcessesDialog}
            open={openProcessesDialog}
          >
            <DialogTitle sx={{ paddingBottom: 2 }}>
              {t('pages.caseform.manualProcesses.title')}
            </DialogTitle>
            <List>
              {manualInitProcessDefs.map((process, index) => (
                <React.Fragment key={process.definitionKey}>
                  <ListItem
                    button
                    onClick={() => startProcess(process.definitionKey)}
                    sx={{
                      '&:hover': {
                        backgroundColor: 'action.hover',
                      },
                    }}
                  >
                    <ListItemText
                      primary={process.name || process.definitionKey}
                    />
                  </ListItem>
                  {index !== manualInitProcessDefs.length - 1 && <Divider />}
                </React.Fragment>
              ))}
            </List>
          </Dialog>
        )}
      </div>
    )
  )
}

const fetchAndCacheOptions = async (serviceMethod, cacheKey, mapCallback) => {
  const cachedOptions = JSON.parse(localStorage.getItem(cacheKey)) || [];
  if (cachedOptions.length > 0) {
    return cachedOptions;
  }
  try {
    const data = await serviceMethod();
    const options = data.map(mapCallback);
    localStorage.setItem(cacheKey, JSON.stringify(options));
    return options;
  } catch (error) {
    console.error(`Error fetching options for ${cacheKey}:`, error);
    return [];
  }
};

const loadOptions = async (keycloak) => {
  const faultCategoryOptions = await fetchAndCacheOptions(
    () => FormService.getFaultCategoriesOptions(keycloak),
    'faultCategoryOptions',
    (item) => ({ label: item.name, value: `${item.id}_${item.recommendationFlag}` })
  );
  const caseStatusOptions = await fetchAndCacheOptions(
    () => FormService.getCaseStatusOptions(keycloak),
    'caseStatusOptions',
    (item) => ({ label: item.name, value: item.id })
  );

  const caseDefinitionUsers = await fetchAndCacheOptions(
    () => CaseDefService.getCaseDefinitionUsers(keycloak),
    'caseAssignedOptions',
    (item) => ({ label: item.userId, value: item.emailId })
  );
  const caseDefinitionCategories = await fetchAndCacheOptions(
    () => CaseDefService.getCaseDefinitionCategories(keycloak),
    'categoryOptions',
    (item) => ({ label: item.name, value: item.id })
  );
};

function a11yProps(index) {
  return {
    id: `simple-tab-${index}`,
    'aria-controls': `simple-tabpanel-${index}`,
  }
}

function TabPanel(props) {
  const { children, value, index, ...other } = props

  return (
    <div
      role='tabpanel'
      hidden={value !== index}
      id={`simple-tabpanel-${index}`}
      aria-labelledby={`simple-tab-${index}`}
      {...other}
    >
      {value === index && (
        <Box sx={{ p: 3 }}>
          <Typography component={'span'}>{children}</Typography>
        </Box>
      )}
    </div>
  )
}

TabPanel.propTypes = {
  children: PropTypes.node,
  index: PropTypes.number.isRequired,
  value: PropTypes.number.isRequired,
}
