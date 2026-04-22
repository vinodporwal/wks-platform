/* eslint-disable no-unused-vars */
import QuestionCircleOutlined from '@ant-design/icons/QuestionCircleOutlined'
import CloseIcon from '@mui/icons-material/Close'
import NotificationsActiveIcon from '@mui/icons-material/NotificationsActive';
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
import Tooltip from '@mui/material/Tooltip'
import Typography from '@mui/material/Typography'
import { CaseStatus } from 'common/caseStatus'
import { StorageService } from 'plugins/storage'
import PropTypes from 'prop-types'
import React, { useEffect, useState , useRef} from 'react'
import { useTranslation } from 'react-i18next'
import { ProcessDefService } from 'services/ProcessDefService'
import { Comments } from 'views/caseComment/Comments'
import { CaseService, FormService, CaseDefService } from '../../services'
import { tryParseJSONObject } from '../../utils/jsonStringCheck'
import Documents from './Documents'
import { Snackbar, SnackbarContent, CircularProgress } from '@mui/material'
import { useNavigate } from 'react-router-dom'
import logo from 'assets/images/logo.svg'
import { DialogActions, DialogContent, DialogContentText } from '@mui/material'
import Config from '../../consts'
import { buildCreateUrl } from 'utils/util'
import { Formio } from 'formiojs'
import { Form } from '@formio/react'

Formio.options = {
  vm: {
    timeout: 25000
  }
}

export const CaseForm = ({ open, handleClose, aCase, keycloak, isCaseViewer = false, isCaseEditor = false, taskId = null, isAdmin = false, isCaseCreator = false }) => {
  const [caseDef, setCaseDef] = useState(null)
  const [form, setForm] = useState(null)
  const [formData, setFormData] = useState(null)
  const [comments, setComments] = useState(null)
  const [documents, setDocuments] = useState(null)
  const [mainTabIndex, setMainTabIndex] = useState(0)
  const [rightTabIndex, setRightTabIndex] = useState(0)
  const [activeStage, setActiveStage] = React.useState(null)
  const [stages, setStages] = useState([])
  const { t } = useTranslation()

  const [anchorEl, setAnchorEl] = React.useState(null)
  const isMenuOpen = Boolean(anchorEl)

  const [openProcessesDialog, setOpenProcessesDialog] = useState(false)
  const [manualInitProcessDefs, setManualInitProcessDefs] = useState([])

  const [isFollowing, setIsFollowing] = useState(false)
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
  
  const [processErrorSnackbarOpen, setProcessErrorSnackbarOpen] = useState(false)
  const [processSuccessSnackbarOpen, setProcessSuccessSnackbarOpen] = useState(false)
  const [taskCompletedSnackbarOpen, setTaskCompletedSnackbarOpen] = useState(false)

// if the task is completed use this state to show modal and block code execution
  const [isBlocked, setIsBlocked] = useState(true);
  const [showBlockedModal, setShowBlockedModal] = useState(false);
  const[taskExists, setTaskExists] = useState(false);
  const [processExistsForBusinessKey, setProcessExistsForBusinessKey] = useState(false)

  const isLastStageofProcessRef = useRef(false);
  



console.log('*****  taskId:  ', taskId);

  const handleFollowClick = () => {
    setIsFollowing(!isFollowing)
  }

 useEffect(() => {

 // if the task with given taskId is completed or does not exitst then logout the user
 // applicable only for task url
  if(taskId) {
   
    ProcessDefService.taskExists(keycloak, taskId)
    .then((data) => {
     if(!data) {
      console.log("Task does not exist.... logging out the user")
     
       setIsBlocked(true);
      // setShowBlockedModal(true);
       setTaskExists(true);
      // Show task completed message before logout
      setTaskCompletedSnackbarOpen(true)
      // Logout after a short delay to allow user to see the message
      setTimeout(() => {
        keycloak.logout({ redirectUri: window.location.origin })
      }, 2000)
     }
     else {
      setIsBlocked(false);
      console.log("Task exists....")
     }
    })
    .catch((err) => {
      console.error('Error checking task existence', err)
    })
    
  }
 }, [taskId])

  useEffect(() => {
   // do not execute the effect if the task does not exits
    if(taskId && isBlocked) return;
    if(!aCase || !aCase.businessKey) return;
    console.log("useEffect isBlocked: ", isBlocked)
    localStorage.setItem('aCaseOwnerEmail', JSON.stringify(aCase.owner?.email))

 

    // taskAssignedTo = taskId ? ProcessDefService.getTaskByTaskId(keycloak, taskId).assignee : null;
    // console.log('*****  taskAssignedTo:  ', taskAssignedTo);
    const urlParams = new URLSearchParams(window.location.search)

    const assetName = urlParams.get('assetName') || 'default'
     console.log("#################3assetName : ", assetName)
    getCaseInfo(aCase)
    //   FileService.downloadForPrintPreview(aCase.documents[0], keycloak),
    // )
  }, [open, aCase, isBlocked  ])

  useEffect(() => {

     // If the processExistsForBusinessKey then check if the process has reached the last stage. This is to set the case status to
    // 'closed' in the payload of caseInstance.

      if(processExistsForBusinessKey) {  
     if(!taskId) {   //skip the logic if the taskId is present to prevent unnecessary API call
      if(aCase.caseDefinitionId === 'create-Asset' || aCase.caseDefinitionId === 'asset-onboarding')  {
        console.log("checking if the process has reached the last stage....")
          
            ProcessDefService.isTaskActive(keycloak, aCase.businessKey, 'aot-publish').then((isTaskActive) => { 
                 if(isTaskActive) {
                  console.log("############### process has reached the last stage")
                  isLastStageofProcessRef.current = true;

                 }
            } )

        }
      }  
    }

   }, [processExistsForBusinessKey])
    

  useEffect(() => {
    if(taskId && isBlocked) return;
    if (activeStage) {
    
      console.log("aCase : ", aCase)
      const stage = caseDef.stages.find((o) => o.name === activeStage)
       console.log('CaseForm : Stage : ', stage)
      // const stageProcesses = stage ? stage.processesDefinitions : []
       ProcessDefService.find(keycloak)
        .then((data) => {
          console.log('CaseForm : Process Definitions : ', data)
       // const autoStartProcesses =    data.filter((o) => o.autoStart === false)
        // setManualInitProcessDefs(autoStartProcesses)
        let filteredProcessDefs = data;

       if(aCase.caseDefinitionId === 'create-Asset' || aCase.caseDefinitionId === 'asset-onboarding')

        filteredProcessDefs =    data.filter((o) => {  
             return o.name === 'XOM Asset Train Onboarding'
           })

           console.log("filteredProcessDefs : ", filteredProcessDefs)

          
        setManualInitProcessDefs(filteredProcessDefs)
        })
        .catch((err) => {
          console.error('Error fetching stage processes', err)
          return []
        })
      
      // const autoStartProcesses = stageProcesses
      //   ? stageProcesses.filter((o) => o.autoStart === false)
      //   : undefined
      // setManualInitProcessDefs(autoStartProcesses)
    }
  }, [activeStage, isBlocked])

  const handleMenuOpen = (event) => {
    setAnchorEl(event.currentTarget)
  }

  const handleMenuClose = () => {
    setAnchorEl(null)
  }

  const handleCloseSnack = () => {
    setSnackOpen(false)
  }

  // const handleConfirmSubmit = () => {
  //   // Proceed with the submit action if confirmed
  //   console.log('Submit confirmed');
  //   setIsConfirmationOpen(false); // Close the dialog
  //   // Add your submission logic here
  // };

  // const handleCancelSubmit = () => {
  //   setIsConfirmationOpen(false); // Close the dialog if canceled
  // };

  const snackAction = lastCreatedCase && (
    <React.Fragment>
      <Button
        color='primary'
        size='small'
        onClick={() => {
          navigate(`/case-list/create${currentParams}`)
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
    console.log('CaseForm : getCaseInfo')
    if (!aCase || !aCase.businessKey) {
      console.warn('getCaseInfo called with invalid aCase:', aCase)
      return
    }
    // Capture a stable local reference so async callbacks always see the correct value
    const currentCase = aCase;
    await loadOptions(keycloak);
    console.log('Fetching case data of ', currentCase)
    // setLoading(true)
    await CaseService.getCaseDefinitionsById(keycloak, currentCase.caseDefinitionId)
      .then(async (data) => {
        setCaseDef(data)
        setStages(
          data.stages.sort((a, b) => a.index - b.index).map((o) => o.name),
        )

        const formData = await FormService.getByKey(keycloak, data.formKey)

       console.log(" ****formData : ", formData)

       // unhide the caaseOwner field in case update form
             if(data.formKey === 'case-management-system'){
       
          formData.structure.components[0].components[0].components[0].columns[0].components[0].hidden = false;
      
      }
       
       
        const processExists = await ProcessDefService.processExistsForBusinessKey(keycloak, currentCase.businessKey)
        console.log(" ****processExists : ", processExists)
        setProcessExistsForBusinessKey(processExists)

        // show task tabs if the process exists and the form is asset-train-create-case  or if the process has been completed
        if(processExists || currentCase.caseStatus === '3'){
         

            if(data.formKey === 'asset-train-create-case') {
              formData.structure.components[0].components.forEach((c) => { 
                
                if(c.title === 'Mods Engineer Checklist' || c.title === 'Process Engineer Checklist' || c.title === 'Machinery Engineer Checklist')
                  c.hidden = false; 
              })
            }
        }

    // show only the task form if the taskId is present
        if(taskId) {
              const task = await ProcessDefService.getTaskByTaskId(keycloak, taskId)
              console.log(" ****task : ", task)

            if(task.taskDefinitionKey === 'aot-processengr') {
                formData.structure.components[0].components.forEach((c) => { 
                  // if(c.title === 'Columns') {
                  //   return;
                  // }
                  if(c.title != 'Process Engineer Checklist' && c.label != 'Columns') // also show the submit button
                    c.hidden = true; 
                  else 
                    c.hidden = false;
                  c.collapsible = false;
                  c.collapsed = false;
                }) 
            }

            if(task.taskDefinitionKey === 'aot-machineryengr')  {
              formData.structure.components[0].components.forEach((c) => { 
                // if(c.title === 'Columns') {
                //   return;
                // }
                if(c.title != 'Machinery Engineer Checklist' && c.label != 'Columns') // also show the submit button
                  c.hidden = true; 
                else 
                  c.hidden = false;
                c.collapsible = false;
                c.collapsed = false;
              }) 
            }

            if(task.taskDefinitionKey === 'aot-modsengr') {
              formData.structure.components[0].components.forEach((c) => { 
                // if(c.title === 'Columns') {
                //   return;
                // }
                if(c.title != 'Mods Engineer Checklist' && c.label != 'Columns') // also show the submit button
                  c.hidden = true; 
                else 
                  c.hidden = false;
                c.collapsible = false;
                c.collapsed = false;
              }) 
            }
              
              
        }

        // for role based access control. if the user has editor role, disable only two fields else if is the user is not a admin or isCaseViewer then disable all the feilds 

        // if isCaseCreator then do not disable the fields regardless of other conditons
        if(data.formKey === 'case-management-system'){
        if((isCaseEditor ||!isAdmin || isCaseViewer) && !isCaseCreator){

          console.log("*** isCaseEditor : ", isCaseEditor)
          formData.structure.components[0].components.forEach((c) => {

            if(isCaseEditor)  {
            if(c.title === 'Analysis' || c.title === 'Value Realization' || c.label === 'Columns')
             return;

              c.disabled = isCaseEditor 
             }
             else c.disabled = true;
          })
      
        }
      }

     




        setFormStructure(formData)
        let updatedFormStructure = null
        if (formData && formData.structure && formData.structure.components) {
          console.log('Form data -> ', formData)
          updatedFormStructure = { ...formData }
        } else {
          console.error('Form structure or components are undefined.')
        }
        // Prefer fetching by businessKey (exact match). If not available, fall back to getCaseById.
        let caseData = null;
        try {
          if (currentCase && currentCase.businessKey) {
            console.log('[DEBUG] Fetching case by businessKey:', currentCase.businessKey, '| caseDefinitionId:', currentCase.caseDefinitionId)
            const resp = await CaseService.getCaseByBusinessKey(
              keycloak,
              currentCase.caseDefinitionId,
              currentCase.businessKey,
            );
            console.log('[DEBUG] getCaseByBusinessKey raw response:', resp)
            if (resp && resp.data && resp.data.length > 0) {
              console.log("in caseForm : caseData.........", resp.data[0]);
              console.log('[DEBUG] attributes from API:', resp.data[0]?.attributes)
              console.log('[DEBUG] textField1 in attributes:', resp.data[0]?.attributes?.find(a => a.name === 'textField1'))
              // API returns an array in the same mapped format
              caseData = resp.data[0];
            } else {
              console.warn('[DEBUG] getCaseByBusinessKey returned no data. resp:', resp)
            }
          }
        } catch (err) {
          console.error('Error fetching case by businessKey', err)
        }

        if (!caseData) {
          console.log("in caseForm : caseData not found.........", caseData);
          console.log('[DEBUG] Falling back to getCaseById with businessKey:', currentCase.businessKey)
          caseData = await CaseService.getCaseById(
            keycloak,
            currentCase.businessKey,
          )
          console.log('[DEBUG] getCaseById result:', caseData)
        }

        currentCase.documents = caseData?.documents || []
        currentCase.comments = caseData?.comments || []
        console.log('[DEBUG] caseData before return:', caseData)
        console.log('[DEBUG] caseData.attributes:', caseData?.attributes)
        return { caseData: currentCase, updatedFormStructure }
      })
      .then(({ caseData, updatedFormStructure }) => {
        const isDraft = caseData?.isDraft === 'y'

        // Disable fields (with proper null checks)
        const level1 = updatedFormStructure.structure.components[0]
          if(!isDraft){
          const analysis = level1.components?.[5] ?? null;
          const recommendation = level1.components?.[6] ?? null;
          const caseDetails = level1.components?.[3] ?? null;
          level1.components?.forEach((component) => {
            if (
              component.id !== recommendation?.id &&
              component.id !== caseDetails?.id && 
              component.id !== analysis?.id
            ) {
              //component.disabled = true;
            }
          });
          const caseDetails0 = caseDetails?.components?.[0];
          if (caseDetails0) {
            //caseDetails0.disabled = true;
          }
        
          const caseDetails1 = caseDetails?.components?.[1];
          const caseStatus = caseDetails1?.columns?.[1]?.components?.[0] ?? null;
        
          // Disable all components inside columns of caseDetails1, except caseStatus
          caseDetails1?.columns?.forEach((column) => {
            column?.components?.forEach((component) => {
              if (component.id !== caseStatus?.id) {
                //component.disabled = true;
              }
            });
          });
        }

        if (level1 && level1.components) {
          const level2 = level1.components[0]
          const level7 =
            level1.components.length > 8 ? level1.components[8] : null
          const level6 =
            level1.components.length > 6 ? level1.components[6] : null
          if (level2 && level2.components) {
            const caseDescriptionField =
              level2.components.length > 1 ? level2.components[1] : null
            if (caseDescriptionField) {
              //caseDescriptionField.disabled = false
            }

            // const recommendation =
            //   level1.components.length > 5 ? level1.components[5] : null
            // if (recommendation) {
            //   recommendation.disabled = true
            // }

            //caseNo.calculateValue = `value = ${aCase.businessKey}`
			if (level2.components[0] && level2.components[0].columns) {
              const caseNo =
                level2.components[0].columns.length > 1
                  ? level2.components[0].columns[0].components[0]
                  : null

              if (caseNo) {
                caseNo.calculateValue = `value = ${currentCase.businessKey}`
              }

              const caseTitleField =
                level2.components[0].columns.length > 1
                  ? level2.components[0].columns[1].components[0]
                  : null
              if (caseTitleField) {
                //caseTitleField.disabled = true
              }

              // Commented as per client requirement
              // const caseAssign =
              //   level2.components[0].columns.length > 2
              //     ? level2.components[0].columns[2].components[0]
              //     : null
              // if (caseAssign) {
              //   caseAssign.disabled = true
              // }

              // Disable case status if currentUser is different than case owner
              const level3 = level1.components?.[3] ?? null;

              const caseStatus = level3?.components?.[1]?.columns?.[1]?.components?.[0] ?? null;

              const caseOwner = caseData?.owner?.id;
              const currentUser = keycloak?.subject;

              if (caseOwner !== currentUser && caseStatus) {
               // caseStatus.disabled = true;
              }

              const faultCategorySelect =
                level2.components[0].columns.length > 2
                  ? level2.components[0].columns[3].components[0]
                  : null
              if (faultCategorySelect && caseData.isDraft == 'n') {
                //faultCategorySelect.disabled = true
              }

              // const caseAssign1 = level2.components[0].columns.length > 2 ? level2.components[0].columns[3].components[0] : null;
              // console.log('caseAssign1', caseAssign1, aCase)
              // if (caseAssign1) {
              //   caseAssign1.defaultValue = `1_true`;
              // }
            }

            if (level7 && level7.columns) {
              const saveAsDraft =
                level7.columns.length > 2
                  ? level7.columns[2].components[0]
                  : null
              if (saveAsDraft) {
                //saveAsDraft.hidden = isDraft ? false : true;
              }
              
              const createButton =
                level7.columns.length > 2
                  ? level7.columns[2].components[1]
                  : null
              if (createButton) {
                //createButton.hidden = true
              }

              const saveButton =
                level7.columns.length > 3
                  ? level7.columns[3].components[0]
                  : null
              if (saveButton) {
                  //saveButton.hidden =  isDraft ? false : true;
              }
            }

            // if (level6) {
            //   const recommendationDescription =
            //     level6.components[0].components[0].columns[0].components[1]
            //   if (recommendationDescription) {
            //     recommendationDescription.disabled = !(
            //       aCase.owner?.email === keycloak.idTokenParsed?.email
            //     )
            //   }
            //   // console.log('saveAsDraft', recommendationDescription)
            // }
          }
        }

        setForm({
          ...updatedFormStructure,
        })

       

        // setIsDraft(caseData?.isDraft === 'y')
        setComments(
          caseData?.comments?.sort(
            (a, b) =>
              new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
          ),
        )
        setDocuments(caseData?.documents)
        
        if(caseData && caseData.attributes) {
          console.log('[DEBUG] Building formData from attributes. Count:', caseData.attributes.length)
          console.log('[DEBUG] Raw attributes array:', JSON.stringify(caseData.attributes))
          const mappedData = caseData.attributes.reduce(
            (obj, item) =>
              Object.assign(obj, {
                [item.name]: tryParseJSONObject(item.value)
                  ? JSON.parse(item.value)
                  : item.value,
              }),
            {},
          )
          console.log('[DEBUG] Mapped formData.data:', mappedData)
          console.log('[DEBUG] textField1 in mapped data:', mappedData?.textField1)
          console.log('[DEBUG] mainAsset / assetName in mapped data:', mappedData?.mainAsset ?? mappedData?.assetName ?? 'NOT FOUND')
		  setFormData({
            data: mappedData,
            metadata: {},
            isValid: true,
          });
		} else {
          console.warn('[DEBUG] caseData.attributes is missing or empty. caseData:', caseData)
        }
        setIsFormData(true)
        setActiveStage(caseData.stage)
        console.log('CaseForm : Active Stage : ', caseData.stage)
      })
      // .catch((err) => {
      //   console.log(err.message)
      // })
    // .finally(() => {
    //   setLoading(false)
    // })
  }

  const onSave = () => {
    setLoading(true)
    const requiredFields = ['caseDescription', 'dueDate', 'faultCategory']

    // const faultCategoryValue = formData.data.container.faultCategory
    // if (faultCategoryValue && faultCategoryValue.endsWith('_false')) {
    //   requiredFields.push(
    //     'caseCauseCategory',
    //     'caseCauseDescription',
    //     'analysisDesc',
    //   )
    // }
    const missingFields = requiredFields.filter(
      (field) => !formData.data.container[field],
    )

    if (missingFields.length > 0) {
      setSnackbarMessages([`Please fill in all required fields. ${requiredFields}`])
      setSnackbarOpen(true)
      setLoading(false)
      return
    }
    const currentParams = window.location.search
    setCurrentParams(currentParams)
    const urlParams = new URLSearchParams(window.location.search)

    const assetName = urlParams.get('assetName') || 'default'
    const hierarchyName = urlParams.get('hierarchyName') || 'default'
    const eventIdsParam = urlParams.get('eventIds')
    const sourceSystem = urlParams.get('sourceSystem') || 'default'
    const eventIds = eventIdsParam ? eventIdsParam.split(',') : []
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
        caseNo: aCase.businessKey,
        owner: {
          id: keycloak.subject || '',
          // id: '0fcfac9f-acf8-4a59-8992-0006bb6909c5',
          name: keycloak.idTokenParsed.name || '',
          email: keycloak.idTokenParsed.email || '',
          phone: keycloak.idTokenParsed.phone || '1234567890',
        },
        attributes: caseAttributes,
        caseUrl: buildCreateUrl(window.location.href),
        // caseUrl: (() => { 
        //   const uri = window.location.pathname;
        //   return uri === '/case-list/create' ? '/case-list/create?' : buildCreateUrl(window.location.href);
        //  })(),
    

        businessKey: aCase.businessKey,
      }),
    )
      .then((data) => {
        const businessKey = data.businessKey

        return CaseService.saveCase(
          keycloak,
          JSON.stringify({
            caseDefinitionId: aCase.caseDefinitionId,
            assetName: assetName,
            isDraft: 'n',
            hierarchyName: hierarchyName,
            sourceSystem: sourceSystem,
            eventIds: eventIds,
            businessKey: businessKey,
			caseNo: businessKey,
      	caseNumber: businessKey,	
            owner: {
              id: keycloak.subject || '',
              // id: '0fcfac9f-acf8-4a59-8992-0006bb6909c5',
              name: keycloak.idTokenParsed.name || '',
              email: keycloak.idTokenParsed.email || '',
              phone: keycloak.idTokenParsed.phone || '',
            },
            attributes: caseAttributes,
            caseUrl: buildCreateUrl(window.location.href),
          // caseUrl: (() => { 
          //   const uri = window.location.pathname;
          //   return uri === '/case-list/create' ? '/case-list/create?' : buildCreateUrl(window.location.href);
          //  })(),
       
          //  assignedTo: {emailId: formData.data.container.caseAssignedTo}
		  assignedTo: formData.data.container.caseAssignedTo.map(email => ({ emailId: email }))
          }),
        )
      })
      .then((data) => {
        setLastCreatedCase(data)
        setSnackOpen(true)
        setTimeout(() => {
          window.location.href = data.caseUrl;
          handleClose()
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
    const requiredFields = ['caseDescription', 'dueDate', 'faultCategory','caseCauseCategory', 'caseCauseDescription', 'analysisDesc']
    const missingFields = requiredFields.filter(
      (field) => !formData.data.container[field],
    )
    if (missingFields.length > 0) {
      setSnackbarMessages([`Please fill in all required fields. ${requiredFields}`])
      setSnackbarOpen(true)
      setLoading(false)
      return
    }
    const currentParams = window.location.search
    setCurrentParams(currentParams)
    const urlParams = new URLSearchParams(window.location.search)
    const assetName = urlParams.get('assetName') || 'default'
    const hierarchyName = urlParams.get('hierarchyName') || 'default'
    const eventIdsParam = urlParams.get('eventIds')
    const sourceSystem = urlParams.get('sourceSystem') || 'default'
    const eventIds = eventIdsParam ? eventIdsParam.split(',') : []
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
        caseNo: aCase.businessKey,
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

        return CaseService.saveAnalysis(
          keycloak,
          JSON.stringify({
            caseDefinitionId: aCase.caseDefinitionId,
            assetName: assetName,
            isDraft: aCase?.isDraft,
            hierarchyName: hierarchyName,
            sourceSystem: sourceSystem,
            eventIds: eventIds,
            businessKey: businessKey,
            owner: {
              id: keycloak.subject || '',
              name: keycloak.idTokenParsed.name || '',
              email: keycloak.idTokenParsed.email || '',
              phone: keycloak.idTokenParsed.phone || '',
            },
            attributes: caseAttributes,
            caseUrl: buildCreateUrl(window.location.href),
          // caseUrl: (() => { 
          //   const uri = window.location.pathname;
          //   return uri === '/case-list/create' ? '/case-list/create?' : buildCreateUrl(window.location.href);
          //  })(),
          //  assignedTo: {emailId: formData.data.container.caseAssignedTo}
		  assignedTo: formData.data.container.caseAssignedTo.map(email => ({ emailId: email }))
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

  // const onSubmitRecommendation = async (event) => {
  //   console.log('event onSubmitRecommendation', event)
  //   let updatedFormData = JSON.parse(JSON.stringify(formData))

  //   // Log the current formData to check its structure
  //   console.log('Current formData:', updatedFormData)

  //   // // Check if dataGrid1 exists inside the container
  //   // if (
  //   //   updatedFormData.data &&
  //   //   updatedFormData.data.container &&
  //   //   updatedFormData.data.container.dataGrid1
  //   // ) {
  //   //   // Iterate through dataGrid1 and update the recommendationNo1 field for each row
  //   //   updatedFormData.data.container.dataGrid1 =
  //   //     updatedFormData.data.container.dataGrid1.map((row, index) => {
  //   //       console.log(`Updating row ${index}`, row)
  //   //       // Set the recommendationNo1 field to '123'
  //   //       return {
  //   //         ...row,
  //   //         recommendationNo1: '123',
  //   //       }
  //   //     })
  //   // } else {
  //   //   console.error('dataGrid1 not found in the form data.')
  //   // }

  //   // Update the formData state with the modified values
  //   setFormData(updatedFormData)

  //   // Log the updated formData to verify changes
  //   console.log('Updated formData:', updatedFormData)
  //   // Update the formData state with the new values
  //   setFormData(updatedFormData)
  //   const {
  //     recommendationReviewer,
  //     recommendationAssignedTo2,
  //     recommendationHeadline,
  //     recommendationTargetCompletionDate1,
  //     recommendationDescription1,
  //     equipmentFunctionLocation,
  //     RecommendationConfirmSAP3
  //   } = event.data

  //   const missingFields = []
  //   if (!recommendationReviewer) missingFields.push('Recommendation Reviewer')
  //   if (!recommendationAssignedTo2)
  //     missingFields.push('Recommendation Assigned To')
  //   if (!recommendationHeadline) missingFields.push('Recommendation Headline')
  //   if (!recommendationTargetCompletionDate1)
  //     missingFields.push('Target Completion Date')

  //   // New validation for RecommendationConfirm
  //   // if (!RecommendationConfirm || !['Yes', 'No'].includes(RecommendationConfirm)) {
  //   //   missingFields.push('Recommendation Confirm');
  //   // }

  //   if (missingFields.length > 0) {
  //     setSnackbarMessages(missingFields)
  //     setSnackbarOpen(true)
  //     setTimeout(() => {
  //       setSnackbarOpen(false)
  //     }, 6000)
  //     return
  //   }

  //   setSnackbarMessages([])
  //   // event.component.disabled = true;

  //   const apiBody = {
  //     recommendationHeadline,
  //     recommendationDescription1,
  //     recommendationAssignedTo2,
  //     equipmentFunctionLocation,
  //     recommendationTargetCompletionDate1,
  //     recommendationReviewer,
  //     RecommendationConfirmSAP3,
  //     deleteRowButton4: false,
  //     RecommendationSubmit3: false,
  //     caseNo: aCase?.caseNo,
  //   }

  //   try {
  //     console.log('apiBody', apiBody, event.data);
  //     const response = await CaseService.saveRecommendation(keycloak, apiBody)

  //     console.log('Recommendation submitted successfully:', response)
  //     setSnackbarMessages(['Recommendation submitted successfully'])
  //     setSnackbarOpen(true)
  //   } catch (error) {
  //     console.error('Error submitting recommendation:', error)
  //     setSnackbarMessages(['Error submitting recommendation'])
  //     setSnackbarOpen(true)
  //   }

  //   setIsConfirmationOpen(true)
  // }

  const onSubmitRecommendation = (event) => {
    console.log('event onSubmitRecommendation', event)
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
    if (!equipmentFunctionLocation)
      missingFields.push('Equipment Function Location')

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
      const response = await CaseService.saveRecommendation(keycloak, apiBody)
      if(response.status !== 500){
      console.log('Recommendation submitted successfully:', response)
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

  const onSubmitForm = () => {

    
       console.log("***** formData stringify *****   ", JSON.stringify(formData))
       console.log("***** formData *****   ", formData)

     localStorage.setItem('formData1', JSON.stringify(formData))
   
    console.log("in caseForm : onSubmitForm.........");
    const currentParams = window.location.search
    setCurrentParams(currentParams)
    const urlParams = new URLSearchParams(window.location.search)

    const assetName = urlParams.get('assetName') || 'default'
    const hierarchyName = urlParams.get('hierarchyName') || 'default'
    const eventIdsParam = urlParams.get('eventIds')
    const sourceSystem = urlParams.get('sourceSystem') || 'default'
    const eventIds = eventIdsParam ? eventIdsParam.split(',') : []
    // const caseAttributes = Object.keys(formData.data).map((key) => ({
    //   name: key,
    //   value:
    //     typeof formData.data[key] !== 'object'
    //       ? formData.data[key]
    //       : JSON.stringify(formData.data[key]),
    //   type: typeof formData.data[key] !== 'object' ? 'String' : 'Json',
    // }))

    if(isLastStageofProcessRef.current === true) {
      formData.data.container = {...formData.data.container, caseStatus: 3};
    }

    const caseAttributes = Object.keys(formData.data).map((key) => {
      console.log("key : ", key)

      let value = formData.data[key];
      let type = typeof value !== "object" ? "String" : "Json";
    
      if (typeof value === "object") {
        value = JSON.stringify(value);
      }
    
      // if (isLastStageofProcessRef.current === true && key === "caseStatus") {
      //   value = "3"; 
      // }
    
      return { name: key, value, type };
    });
    


   


    // First API call to createCase to get the businessKey
    CaseService.createCase(
      keycloak,
      JSON.stringify({
        caseDefinitionId: aCase.caseDefinitionId,
        caseNo: aCase.businessKey,
        owner: {
          id: keycloak.subject || '',
          // id: '0fcfac9f-acf8-4a59-8992-0006bb6909c5',
          name: keycloak.idTokenParsed.name || '',
          email: keycloak.idTokenParsed.email || '',
          phone: keycloak.idTokenParsed.phone || '',
        },
        attributes: caseAttributes,
        caseUrl: buildCreateUrl(window.location.href),
        // caseUrl: (() => { 
        //   const uri = window.location.pathname;
        //   return uri === '/case-list/create' ? '/case-list/create?' : buildCreateUrl(window.location.href);
        //  })(),
     

        businessKey: aCase.businessKey,
      }),
    )
      .then((data) => {
        const businessKey = data.businessKey // Extract businessKey from the response
        // setLastCreatedCase(data);

        // Second API call to saveCase with the businessKey
        return CaseService.saveCase(
          keycloak,
          JSON.stringify({
            caseDefinitionId: aCase.caseDefinitionId,
            assetName: assetName,
            isDraft: 'n',
            hierarchyName: hierarchyName,
            sourceSystem: sourceSystem,
            eventIds: eventIds,
            businessKey: businessKey, // Include businessKey in the payload
			caseNo: businessKey,
      	caseNumber: businessKey,	
            owner: {
              id: keycloak.subject || '',
              // id: '0fcfac9f-acf8-4a59-8992-0006bb6909c5',
              name: keycloak.idTokenParsed.name || '',
              email: keycloak.idTokenParsed.email || '',
              phone: keycloak.idTokenParsed.phone || '',
            },
            attributes: caseAttributes,
            caseUrl: buildCreateUrl(window.location.href),
          //  caseUrl: (() => { 
          //   const uri = window.location.pathname;
          //   return uri === '/case-list/create' ? '/case-list/create?' : buildCreateUrl(window.location.href);
          //  })(),

          //  assignedTo: {emailId: formData.data.container.caseAssignedTo}
		  assignedTo: formData.data.container.caseAssignedTo.map(email => ({ emailId: email }))
          }),
        )
      })
      .then((data) => {
      // complete the task if this is the task form
        if(taskId){ 
          console.error('caseForm: In complete task block')
          ProcessDefService.completeTask(keycloak, taskId, caseAttributes)
          .then(() => {
            console.log(' Engineering Task completed successfully: ', taskId)
          })
          .catch((err) => {
            console.error('Error completing Engineering task: ', taskId, err)
          })
        }
      // logic to end the business process if the process has reached the last stage ie all three forms are submitted. applicable only for asset train onboarding process.

      //  if(!taskId) {   //skip the logic if the taskId is present to prevent unnecessary API call
      // if(aCase.caseDefinitionId === 'create-Asset')  {
      //   console.log("checking if the process has reached the last stage....")
          
      //       ProcessDefService.isTaskActive(keycloak, aCase.businessKey, 'aot-publish').then((isTaskActive) => { 
      //            if(isTaskActive) {
      //             console.log("############### process has reached the last stage")
      //             ProcessDefService.completeTaskWithbusinessKey(keycloak, aCase.businessKey, 'aot-publish', caseAttributes).then((data) => {
      //               console.log("Task completed successfully: ")
      //             }).catch((err) => {
      //               console.error("Error completing task: ", err)
      //             })

      //            }
      //       } )

      //   }
      // }

            // logic to end the business process if the process has reached the last stage ie all three forms are submitted. applicable only for asset train onboarding process.

      if(!taskId) { //skip the logic if the taskId is present to prevent unnecessary API call
        if(isLastStageofProcessRef.current) { 
          console.log("process has reached the last stage completing the final task to end the process....")
          ProcessDefService.completeTaskWithbusinessKey(keycloak, aCase.businessKey, 'aot-publish', caseAttributes).then((data) => {
            console.log("Final task completed successfully to end the process: ")
          }).catch((err) => {
            console.error("Error completing final task : ", err) 
          })
        }  
      }

        setLastCreatedCase(data)
        setSnackOpen(true) // Show success notification
        setTimeout(() => {
          window.location.href = data.caseUrl;
          // handleClose()
        }, 1000)
      })
      .catch((err) => {
        console.error(err.message)
      })
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

  const handleRightTabChanged = (event, newValue) => {
     setRightTabIndex(newValue)
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
        console.log(err.message)
      })
  }

  // const updateActiveState = () => {
  //   CaseService.getCaseById(keycloak, aCase.businessKey).then((data) =>
  //     setActiveStage(data.stage),
  //   )
  // }

  const handleOpenProcessesDialog = () => {
    console.log('CaseForm : handleOpenProcessesDialog')
    setOpenProcessesDialog(true)
    handleMenuClose()
  }

  const handleCloseProcessesDialog = () => {
    setOpenProcessesDialog(false)
  }

  const handleEventTrendClick = (eventPkId) => {
  //  window.open(aCase.eventTrendUrl, '_blank')
  if(aCase.eventTrendUrls?.find((item) => item.urlId === eventPkId)) {
    window.open(aCase.eventTrendUrls.find((item) => item.urlId === eventPkId).url, '_blank')
  }
  else {
    console.log('eventPkId not found in eventTrendUrls: ', eventPkId)
  }


  }
  const handleEventLinkClick = (eventPkId) => {
  //  window.open(aCase.eventReportUrl, '_blank')
  if(aCase.eventReportUrls?.find((item) => item.urlId === eventPkId)) {
    window.open(aCase.eventReportUrls.find((item) => item.urlId === eventPkId).url, '_blank')
  }
  else {
    console.log('eventPkId not found in eventReportUrls: ', eventPkId)
  }
  }

  const startProcess = (key) => {
    handleCloseProcessesDialog()
    console.log('CaseForm : startProcess : ', key)

    ProcessDefService.start(keycloak, key, aCase.businessKey).then((data) => {
      console.log('CaseForm : process started for businessKey: ', aCase.businessKey, ' : ', data)
      // Show success snackbar notification
      
      setProcessSuccessSnackbarOpen(true)
    }).catch((err) => {
      console.error('Error starting process', err)
      // Close the dialog
      handleCloseProcessesDialog()
      // Show snackbar notification
      setProcessErrorSnackbarOpen(true)
    })
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
    console.log('labelMap', labelMap)
    const getLabel = (key) => labelMap[key] || key || ''

    const uploadedFiles = aCase.documents || []

    let content = `
    <div style="font-family: Arial, sans-serif; padding: 20px; border: 1px solid #333;">
      <div style="display: flex; align-items: center; justify-content: center; margin-bottom: 20px;">
        <h2 style="text-align: center; margin: 0;">XOM Case Management System</h2>
      </div>

      <!-- Case Information Panel -->
      <div style="border: 1px solid #333; border-radius: 5px; margin-bottom: 20px;">
        <h3 style="background-color: #333; color: #fff; padding: 10px; margin: 0;">Case Information</h3>
        <div style="padding: 10px;">
          <p><strong>${getLabel('caseNo')}</strong>: ${aCase.caseNo}</p>
          <p><strong>${getLabel('caseTitle')}</strong>: ${containerData.caseTitle}</p>
          <p><strong>${getLabel('caseAssignedTo')}</strong>: ${containerData.caseAssignedTo}</p>
          <p><strong>${getLabel('faultCategory')}</strong>: ${getFaultCategoryLabel(containerData.faultCategory)}</p>
          <p><strong>${getLabel('caseDescription')}</strong>: ${containerData.caseDescription}</p>
        </div>
      </div>

      <!-- Case Details -->
      <div style="border: 1px solid #333; border-radius: 5px; margin-bottom: 20px;">
        <h3 style="background-color: #333; color: #fff; padding: 10px; margin: 0;">Case Details</h3>
        <div style="padding: 10px;">
          <p><strong>${getLabel('createdOn')}</strong>: ${new Date(containerData.createdOn).toLocaleDateString()}</p>
          <p><strong>${getLabel('dueDate')}</strong>: ${containerData?.dueDate || 'N/A'}</p>
          <p><strong>${getLabel('endDate')}</strong>: ${containerData?.endDate || 'N/A'}</p>
          <p><strong>${getLabel('caseStatus')}</strong>: ${getcaseStatusLabel(containerData.caseStatus)}</p>
          <p><strong>${getLabel('analysisTeam')}</strong>: ${containerData.analysisTeam.join(', ')}</p>
        </div>
      </div>

      <!-- Associated Faults -->
      <div style="border: 1px solid #333; border-radius: 5px; margin-bottom: 20px;">
        <h3 style="background-color: #333; color: #fff; padding: 10px; margin: 0;">Associated Faults</h3>
        <p style="padding: 10px; margin: 0;"><strong>${getLabel('textField1')}</strong>: ${containerData.textField1}</p>
        ${formatDataGrid(containerData.dataGrid2, getLabel)}
      </div>
  `

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
                      src="${Config.StorageUrl}/files1/cases/downloads/${encodeURIComponent(file.name)}?content-type=${encodeURIComponent(file.type)}"
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
    if (containerData.RecommendationsRadio === 'yes') {
      content += `
      <!-- Data Grid 1 -->
      <div style="border: 1px solid #333; border-radius: 5px; margin-bottom: 20px;">
        <h3 style="background-color: #333; color: #fff; padding: 10px; margin: 0;">${getLabel('dataGrid1')}</h3>
        ${formatDataGrid(containerData.dataGrid1, getLabel)}
      </div>
    `
    }

    // Value Realization section
    content += `
      <!-- Value Realization -->
      <div style="border: 1px solid #333; border-radius: 5px; margin-bottom: 20px;">
        <h3 style="background-color: #333; color: #fff; padding: 10px; margin: 0;">Value Realization</h3>
        <div style="padding: 10px;">
          <p><strong>${getLabel('valueRealizationCategory')}</strong>: ${containerData.valueRealizationCategory}</p>
          <p><strong>${getLabel('productionLoss')}</strong>: ${containerData.productionLoss || ''}</p>
          <p><strong>${getLabel('manHoursCost')}</strong>: ${containerData.manHoursCost || ''}</p>
          <p><strong>${getLabel('spareCost')}</strong>: ${containerData.spareCost || ''}</p>
          <p><strong>${getLabel('totalValueCaptured')}</strong>: ${containerData.totalValueCaptured}</p>
          <p><strong>${getLabel('valueRealizationConclusion')}</strong>: ${containerData.valueRealizationConclusion}</p>
        </div>
      </div>
  `

    // Append uploaded files section at the bottom
    if (uploadedFiles.length > 0) {
      content += `
      <div style="border: 1px solid #333; padding: 20px; border-radius: 5px; margin-bottom: 20px;">
      <h3 style="margin-bottom: 10px;">Uploaded Files</h3>
      <ul style="list-style: none; padding: 0; margin: 0;">
        ${uploadedFiles
          .map(
            (file, index) => `
                  <li style="margin-bottom: 16px;">
                    <a 
                      href="${Config.StorageUrl}/files1/cases/downloads/${encodeURIComponent(file.name)}?content-type=${encodeURIComponent(file.type)}"
                      alt="${file.name}"
                      target="_blank"
                    >${file.name}</a>
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

    // Open a new window and print the generated content
    const printWindow = window.open('', '_blank');
    if (printWindow) {
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
      printWindow.document.close();
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
                {/* <div style={{ fontSize: '13px' }}>
                  {aCase?.statusDescription}
                </div> */}
              </Typography>
              {!isCaseViewer && aCase.status === CaseStatus.WipCaseStatus.description && (
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
              {/* <Button
                color='primary'
                onClick={() =>
                  window.open(
                    'http://localhost:9000/localhost/cases/demo_test2.png',
                    '_blank',
                  )
                }
              >
                Open Image
              </Button> */}
              {!isCaseViewer && aCase.status === CaseStatus.ClosedCaseStatus.description && (
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
              {!isCaseViewer && aCase.status === CaseStatus.ArchivedCaseStatus.description && (
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
              <Button
                color='inherit'
                onClick={handleFollowClick}
                startIcon={<NotificationsActiveIcon />}
                disabled={(isCaseViewer || isCaseEditor || !isAdmin) && !isCaseCreator}
              >
                {isFollowing ? 'Unfollow' : 'Follow'}
              </Button> 
              <Button color='inherit' onClick={printCaseDetails}>
                {'Print'}
              </Button>

               <Button color='inherit' onClick={onSave} disabled={(!isAdmin || isCaseViewer || isCaseEditor) && !isCaseCreator}>
                {'Save'}
              </Button>
            

              {/* start process three dot button */}
               <IconButton
                edge='end'
                disabled={(!isAdmin || isCaseViewer || isCaseEditor) && !isCaseCreator}
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

{/* hide the stages if the taskId is present */}
    {  taskId == null && (<Box
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
          )}
          <Grid container spacing={2} sx={{ paddingLeft: 1, paddingRight: 1 }}>
            <Grid item xs={12}>

              {/* hide the tabs if the taskId is present */}
          {taskId == null && (<Box>
                <Tabs value={mainTabIndex} onChange={handleMainTabChanged} >
                  <Tab
                    label={t('pages.caseform.tabs.details')}
                    disabled={(isCaseViewer || isCaseEditor || !isAdmin) && !isCaseCreator}
                    {...a11yProps(0)}
                  />
                  <Tab
                    label={t('pages.caseform.tabs.attachments')}
                    disabled={(isCaseViewer || isCaseEditor || !isAdmin) && !isCaseCreator}
                    {...a11yProps(1)}
                   
                  />
                  <Tab
                    label={t('pages.caseform.tabs.comments')}
                    disabled={(isCaseViewer || isCaseEditor || !isAdmin) && !isCaseCreator}
                    {...a11yProps(2)}
                  
                  />
                </Tabs>
              </Box> )}
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
                      {/* <Tooltip title={form.toolTip}>
                        <QuestionCircleOutlined />
                      </Tooltip> */}
                    </Box>  


                    {showBlockedModal && (
  <div
    style={{
      position: 'fixed',
      top: 0,
      left: 0,
      width: '100vw',
      height: '100vh',
      backgroundColor: 'rgba(0,0,0,0.6)',   // dark overlay
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      zIndex: 9999,
    }}
  >
    <div
      style={{
        backgroundColor: '#fff',
        padding: '30px',
        borderRadius: '10px',
        minWidth: '300px',
        textAlign: 'center',
        boxShadow: '0 0 20px rgba(0,0,0,0.3)',
      }}
    >
      <h2>Task Not Found</h2>
      <p>The task you are trying to access no longer exists.</p>

      <button
        onClick={() => window.location.href = "/case-list"}
        style={{
          marginTop: '20px',
          padding: '10px 20px',
          cursor: 'pointer',
        }}
      >
        Go Back
      </button>
    </div>
  </div>
)}
{console.log('***** formData at render', formData)}
                    
                    {isFormData && (
                      <Form
                        form={form.structure}
                         submission={formData}
                    //   submission={{ ...formData, processExistsForBusinessKey }}
                        // options={{
                        //   readOnly: isCaseViewer,
                        //   fileService: new StorageService(),
                        // }}
                        // onSubmit={(submission) => {
                        //   console.log('Validation passed:', true)
                        //   console.log('Form data:', submission)

                        //   onSave(submission)
                        // }}
                        onCustomEvent={(event) => {
                          console.log("in caseForm : onCustomEvent.........");
                          console.log('event: ', event)
                          if (isCaseViewer) {
                            return; // Prevent any form submission for case viewers
                          }
                          
                          if (event.component.key === 'saveAsDraft') {
                            onSubmitForm()
                          } else if (
                            event.component.key === 'RecommendationSubmit3'
                          ) {
                            onSubmitRecommendation(event)
                          } else if (event.component.key === 'onSave') {
                            // onSubmitRecommendation()
                            onSave()
                          }  else if (event.component.key === 'analysisSubmit') {
                            onAnalysisSave()
                          }
                            
                          else if (event.component.key === 'btnEventTrend') {
                            handleEventTrendClick(event.data.eventPkId)
                          }
                          else if (event.component.key === 'btnEventLink') {
                            handleEventLinkClick(event.data.eventPkId)
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
                    <Snackbar
                      open={processErrorSnackbarOpen}
                      autoHideDuration={3000}
                      onClose={() => setProcessErrorSnackbarOpen(false)}
                      anchorOrigin={{ vertical: 'center', horizontal: 'center' }}
                    >
                      <SnackbarContent
                        message={
                          <div>
                            <Typography
                              variant='body1'
                              color='error'
                              component='div'
                              sx={{ fontSize: '1.1rem', fontWeight: 500 }}
                            >
                              Business process already exists
                            </Typography>
                          </div>
                        }
                        action={
                          <Button
                            color='secondary'
                            size='small'
                            onClick={() => setProcessErrorSnackbarOpen(false)}
                          >
                            Ok
                          </Button>
                        }
                      />
                    </Snackbar>

                 

                    {/* show the success snackbar notification if the process is started successfully */}
                    <Snackbar
                      open={processSuccessSnackbarOpen}
                      autoHideDuration={3000}
                      onClose={() => setProcessSuccessSnackbarOpen(false)}
                      anchorOrigin={{ vertical: 'center', horizontal: 'center' }}
                    >
                      <SnackbarContent
                        message={
                          <div>
                            <Typography
                              variant='body1'
                              component='div'
                              sx={{ fontSize: '1.1rem', fontWeight: 500, color: 'success.main' }}
                            >
                              Process started successfully
                            </Typography>
                          </div>
                        }
                        action={
                          <Button
                            color='secondary'
                            size='small'
                            onClick={() => setProcessSuccessSnackbarOpen(false)}
                          >
                            Ok
                          </Button>
                        }
                      />
                    </Snackbar>
                    <Snackbar
                      open={taskCompletedSnackbarOpen}
                      autoHideDuration={2000}
                      onClose={() => setTaskCompletedSnackbarOpen(false)}
                      anchorOrigin={{ vertical: 'center', horizontal: 'center' }}
                    >
                      <SnackbarContent
                        message={
                          <div>
                            <Typography
                              variant='body1'
                              component='div'
                              sx={{ fontSize: '1.1rem', fontWeight: 500 }}
                            >
                              Task already completed
                            </Typography>
                          </div>
                        }
                        action={
                          <Button
                            color='secondary'
                            size='small'
                            onClick={() => setTaskCompletedSnackbarOpen(false)}
                          >
                            Ok
                          </Button>
                        }
                      />
                    </Snackbar>
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

              //  <React.Fragment key={process.definitionKey}>
              <React.Fragment key={process.key}>
                  <ListItem
                    button
                  
                   // onClick={() => startProcess(process.definitionKey)}
                   onClick={() => startProcess(process.key)}
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
    console.log(`Using cached options for ${cacheKey}`);
    return cachedOptions;
  }
  try {
    const data = await serviceMethod();
    const options = data.map(mapCallback);
    localStorage.setItem(cacheKey, JSON.stringify(options));
    console.log(`Fetched and cached options for ${cacheKey}`);
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
  // const caseDefinitionGEAPMUsers = await fetchAndCacheOptions(
  //   () => CaseDefService.getCaseDefinitionGEAPMUsers(keycloak),
  //   'geAPMUsers',
  //   (item) => ({ label: item.userId, value: item.emailId })
  // );
};
const Transition = React.forwardRef(function Transition(props, ref) {
  return <Slide direction='up' ref={ref} {...props} />
})

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
