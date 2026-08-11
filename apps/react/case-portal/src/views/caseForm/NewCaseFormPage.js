import React, { useState, useEffect, useRef } from 'react'
import { QuestionCircleOutlined } from '@ant-design/icons'
import CloseIcon from '@mui/icons-material/Close'
import NotificationsActiveIcon from '@mui/icons-material/NotificationsActive' // Missing import
import {
  Box,
  Tooltip,
  CircularProgress,
  Alert,
  Dialog,
  DialogTitle, // Missing import
  DialogContent, // Missing import
  DialogContentText, // Missing import
  DialogActions, // Missing import
} from '@mui/material'
import AppBar from '@mui/material/AppBar'
import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid'
import IconButton from '@mui/material/IconButton'
import Slide from '@mui/material/Slide'
import Toolbar from '@mui/material/Toolbar'
import Typography from '@mui/material/Typography'
import { useSession } from 'SessionStoreContext'
import { CaseService, FormService } from '../../services'
import { StorageService } from 'plugins/storage'
import { Snackbar, SnackbarContent } from '@mui/material'
import { useNavigate } from 'react-router-dom'
import { buildCreateUrl } from 'utils/util'
import { Formio } from 'formiojs'
import { Form } from '@formio/react'
import { CaseDefService } from 'services/CaseDefService'
import Config from 'consts/index'

Formio.options = {
  vm: {
    timeout: 25000
  }
}
const Transition = React.forwardRef(function Transition(props, ref) {
  return <Slide direction='up' ref={ref} {...props} />
})

export const NewCaseFormPage = ({ open = true, caseDefId = 'create', handleFormClose, openedFromList = false }) => {
  const [caseDef, setCaseDef] = useState([])
  const [form, setForm] = useState([])
  const [formData, setFormData] = useState(null)
  const [lastCreatedCase, setLastCreatedCase] = useState(null)
  const [snackOpen, setSnackOpen] = useState(false)
  const keycloak = useSession()
  const navigate = useNavigate()
  const [snackbarOpen, setSnackbarOpen] = useState(false)
  const [snackbarMessages, setSnackbarMessages] = useState([])
  const [currentParams, setCurrentParams] = useState([])
  const [validationSnackbarOpen, setValidationSnackbarOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  //const[eventTrendUrl, setEventTrendUrl] = useState('')
 
 // const[eventReportUrl, setEventReportUrl] = useState('')
 const eventTrendUrlRef = useRef('')
 const eventReportUrlRef = useRef('')
  const eventTrendUrlArrayRef = useRef([])
  const eventReportUrlArrayRef = useRef([])
  

  // const realmRoles = keycloak.idTokenParsed.realm_access?.roles || []
  // const clientRoles = keycloak.idTokenParsed.resource_access
  //   ? Object.values(keycloak.idTokenParsed.resource_access).flatMap(
  //       (client) => client.roles || [],
  //     )
  //   : []

  const token = keycloak.tokenParsed;
  const clientId = token?.azp || token?.client_id; 
  const clientRoles = token?.resource_access?.[clientId]?.roles || [];
  console.log("Client roles:", clientRoles);

    console.log("keycloak access token parsed : ", keycloak.tokenParsed);
  // const allRoles = [...realmRoles, ...clientRoles]
  // console.log('all roles: ',  allRoles)
 

//   const createApmUrlBasedOnSelectedEvent = () => {
//     const urlParams = new URLSearchParams(window.location.search);
//     if (urlParams.size === 0) {
//       console.log('No URL parameters found.. No APM URL will be created');
//       return;  }
//     const eventIds = urlParams.get('eventIds');

//     if (!eventIds) {
//       console.error('eventIds parameter not found in the URL');
//       return;
//     }

//     console.log('creating APM URL based on selected event..........')

//     // Split on commas, spaces, or other separators, then take the first part
//     const firstEventId = eventIds.split(/[,\s]+/)[0];
       
//     const encodedEventId = encodeURIComponent(firstEventId);
//     CaseDefService.getFaultEvent(keycloak, encodedEventId)
//     .then((data) => {
//       console.log('*********data', data)
//       const faultEvent = data[0];

//       console.log('###### ********* falutEvent: ', faultEvent)

//       const startTimeStampRaw = faultEvent.startTime;
//       const endTimeStampRaw = faultEvent.endTime;

     
//  const assetDisplayName = encodeURIComponent(faultEvent.AssetDisplayName) || '';
//  const assetName = encodeURIComponent(faultEvent.assetName) || '';
 
// const eventName = encodeURIComponent(faultEvent.events.eventName) || '';
// const selectedEventId = encodeURIComponent(faultEvent.events.eventPkId) || '';   
// const assetId = encodeURIComponent(faultEvent.assetId) || '';  
  
// let startTimeStamp = null;
// let endTimeStamp = null;
// if(startTimeStampRaw) {
//   startTimeStamp = new Date(startTimeStampRaw.replace(" ", "T") + "Z").toISOString();
// } 

// if(endTimeStampRaw) {
//  endTimeStamp = new Date(endTimeStampRaw.replace(" ", "T") + "Z").toISOString();  } 
// // const rootNode = '';
//  //  const assetType = '';

// const event_TrendUrl = `https://apm-exxonmobil-useast.connectedplant.honeywell.com/Forge/APM/ShellUI/#/trends?rootNode=${assetName}&assetDisplayName=${assetDisplayName}&period=Custom+Range&startTimeStamp=${startTimeStamp}&endTimeStamp=${endTimeStamp}&selectedEventId=${selectedEventId}&eventName=${eventName}&eventId=${selectedEventId}&assetId=${assetId}&hierarchyName=Planthierarchy&hierarchyLevel=null`

// const event_ReportUrl = `https://apm-exxonmobil-useast.connectedplant.honeywell.com/ReportServer/Pages/ReportViewer.aspx?%2fDailyFaultReport_Test&rs:Command=Render&EventID=${selectedEventId}`

// console.log('apmUrl', event_TrendUrl)

//   //  setEventTrendUrl(event_TrendUrl)
//   eventTrendUrlRef.current = event_TrendUrl
//   //  setEventReportUrl(event_ReportUrl)
//   eventReportUrlRef.current = event_ReportUrl
//     })
//     .catch((err) => {
//       console.error(err.message)
//     })



//   }

const createApmUrlBasedOnSelectedEvent = () => {


  const urlParams = new URLSearchParams(window.location.search);
  if (urlParams.size === 0) {
    console.log('No URL parameters found.. No APM URL will be created');
    return;  }
  const eventIds = urlParams.get('eventIds');

  if (!eventIds) {
    console.error('eventIds parameter not found in the URL');
    return;
  }

  console.log('creating APM URL based on selected event..........')

 
  const encodedEventIds = encodeURIComponent(eventIds);

  CaseDefService.getFaultEvent(keycloak, encodedEventIds).then((data) => {  
      console.log('falut event data : ', data)

      const mappedData = data.map((item) => {  
        return { 
          assetDisplayName: encodeURIComponent(item.AssetDisplayName) || '',
          eventName: encodeURIComponent(item.events.eventName) || '',
          selectedEventId: encodeURIComponent(item.events.eventPkId) || '',
          assetId: encodeURIComponent(item.assetId) || '',
          assetName: encodeURIComponent(item.assetName) || '',
          startTimeStamp:  item.startTime ?  new Date(item.startTime.replace(" ", "T") + "Z").toISOString() : null,
          endTimeStamp:  item.endTime ?  new Date(item.endTime.replace(" ", "T") + "Z").toISOString() : null
        }
      })

      console.log('mapped data : ', mappedData)

      const eventTrendUrlArray = mappedData.map((item) => ({

         urlId : item.selectedEventId,
         url: `${Config.ApmBaseUrl}/Forge/APM/ShellUI/#/trends?rootNode=${item.assetName}&assetDisplayName=${item.assetDisplayName}&period=Custom+Range&startTimeStamp=${item.startTimeStamp}&endTimeStamp=${item.endTimeStamp}&selectedEventId=${item.selectedEventId}&eventName=${item.eventName}&eventId=${item.selectedEventId}&assetId=${item.assetId}&hierarchyName=Planthierarchy&hierarchyLevel=null`

      }))

      const eventReportUrlArray = mappedData.map((item) => ({
        urlId : item.selectedEventId,
        url: `${Config.ApmBaseUrl}/ReportServer/Pages/ReportViewer.aspx?%2fDailyFaultReport_Test&rs:Command=Render&EventID=${item.selectedEventId}`
      }))


      eventTrendUrlArrayRef.current = eventTrendUrlArray
      eventReportUrlArrayRef.current = eventReportUrlArray

  })



}



  useEffect(() => { 
    
    const params = window.location.search
    setCurrentParams(params)
    createApmUrlBasedOnSelectedEvent();
  }, [])

  useEffect(() => {
    localStorage.setItem('aCaseOwnerEmail', JSON.stringify(keycloak.idTokenParsed.email || ''))

    CaseService.getCaseDefinitionsById(keycloak, caseDefId)
      .then((data) => {
        setCaseDef(data)
        console.log('**** formkey : ', data.formKey)
       
        return FormService.getByKey(keycloak, data.formKey)
        
      })
      .then((data) => {
        console.log('**** new page form data', data)
        setForm(data)

        const level1 = data.structure.components[0]
        if (level1 && level1.components) {
          const level2 = level1.components[0]
          const level7 =
            level1.components.length > 8 ? level1.components[8] : null
          if (level2 && level2.components) {
            if (level7 && level7.columns) {
              const saveAsDraft =
                level7.columns.length > 2
                  ? level7.columns[2].components[0]
                  : null
              if (saveAsDraft) {
                //saveAsDraft.hidden = true
              }

              const createButton =
                level7.columns.length > 2
                  ? level7.columns[2].components[1]
                  : null
              if (createButton) {
                //createButton.hidden = false
              }

              const saveButton =
                level7.columns.length > 3
                  ? level7.columns[3].components[0]
                  : null
              if (saveButton) {
                //saveButton.hidden = true
              }
            }
          }
        }

        setFormData({
          data: {},
          metadata: {},
          isValid: true,
        })
      })
      .catch((err) => {
        console.error(err.message)
      })
  }, [caseDefId, keycloak])

  const navigateToCaseUrl = (caseUrl) => {
    // When opened as a modal from the case list, just close the dialog �
    // CaseList's useEffect will re-fetch because openNewCaseForm toggles
    if (openedFromList && typeof handleFormClose === 'function') {
      handleFormClose()
      return
    }

    // Standalone: if external app params present, redirect to caseUrl (opens case detail)
    const urlParams = new URLSearchParams(window.location.search)
    const isFromExternalApp = urlParams.has('eventIds') || urlParams.has('assetName') || urlParams.has('hierarchyName')

    if (isFromExternalApp && caseUrl) {
      try {
        const target = new URL(caseUrl)
        const cleanPath = target.pathname.replace(/\/([\w-]+)\/\1(\/|$)/g, '/$1$2')
        navigate(cleanPath + target.search)
        return
      } catch (e) {
        // fall through
      }
    }

    // Standalone WKS: go to case list with a unique state to force re-fetch
    navigate(`/case-list/cases?case_def=${caseDefId}`, { state: { refresh: Date.now() }, replace: true })
  }

  const handleCloseSnack = () => {
    setSnackOpen(false)
  }

  const handleClose = () => {
    const params = currentParams.length > 0 ? currentParams : window.location.search
    if (openedFromList && typeof handleFormClose === 'function') {
    
      handleFormClose()
      return
    } else {
      navigate(`/case-list/create`)
    }
    navigate(`/case-list/create${params}`)
  }

  const onSave = () => {
    
    setLoading(true)

    const requiredFields = ['caseDescription', 'dueDate', 'faultCategory']

    // NOTE: You earlier had logic pushing analysis fields conditionally.
    // If you removed analyst logic, remove this block; if you still need it, keep it.
    const faultCategoryValue = formData?.data?.container?.faultCategory
    if (faultCategoryValue && faultCategoryValue.endsWith('_false')) {
      requiredFields.push(
        'caseCauseCategory',
        'caseCauseDescription',
        'analysisDesc',
      )
    }

    const missingFields = requiredFields.filter(
      (field) => !formData.data.container[field],
    )

    if (missingFields.length > 0) {
      setSnackbarMessages(['Please fill in all required fields.'])
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
	
	let updFormData = formData.data;
	// updFormData.businessKey = data.businessKey;
	// updFormData.caseNo = data.businessKey;

    const caseAttributes = Object.keys(updFormData).map((key) => ({
      name: key,
      value:
        typeof updFormData[key] !== 'object'
          ? updFormData[key]
          : JSON.stringify(updFormData[key]),
      type: typeof updFormData[key] !== 'object' ? 'String' : 'Json',
    }))
    
    // First API call to createCase to get the businessKey

    console.log('Cleaned URL', buildCreateUrl(window.location.href))

    CaseService.createCase(
      keycloak,
      JSON.stringify({
        caseDefinitionId: caseDefId,
         caseNo: null,
        businessKey: null,
        owner: {
          // id: keycloak.subject || '',
          id: keycloak.idTokenParsed.sub,
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

      }),
    )
    .then((data) => {
        const businessKey = data.businessKey
        // setLastCreatedCase(data);

        console.log('Cleaned URL', buildCreateUrl(window.location.href))
        return CaseService.saveCase(
          keycloak,
          JSON.stringify({
            caseDefinitionId: caseDefId,
            assetName: assetName,
            isDraft: 'y',
            hierarchyName: hierarchyName,
            sourceSystem: sourceSystem,
            eventIds: eventIds,
            businessKey: businessKey,
		  caseNo: businessKey,
			caseNumber: businessKey,
            owner: {
              id: keycloak.idTokenParsed.sub || '',
              // id: '0fcfac9f-acf8-4a59-8992-0006bb6909c5',
              name: keycloak.idTokenParsed.name || '',
              email: keycloak.idTokenParsed.email || '',
              phone: keycloak.idTokenParsed.phone || '1234543211',
            },
            attributes: caseAttributes,
           caseUrl: buildCreateUrl(window.location.href),
        //  caseUrl: (() => { 
        //   const uri = window.location.pathname;
        //   return uri === '/case-list/create' ? '/case-list/create?' : buildCreateUrl(window.location.href);
        //  })(),
         
          }),
        ).then((saveData) => ({ ...saveData, caseNo: businessKey, businessKey }))
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
        setLoading(false)
      })
   }

  const onSubmitRecommendation = () => {
    setSnackbarMessages(['Cannot submit recommendation without a case number.'])
    setSnackbarOpen(true)

    setTimeout(() => {
      setSnackbarOpen(false)
    }, 2000)

    return
  }

  const handleEventTrendClick = (eventPkId) => {
    console.log('handleEventTrendClick..........')
    // console.log('eventTrendUrl: ', eventTrendUrlRef.current)
    // if (eventTrendUrlRef.current) {
    //   window.open(eventTrendUrlRef.current, "_blank");
    // }

    console.log('eventTrendUrlArray: ', eventTrendUrlArrayRef.current, 'eventPkId: ', eventPkId)

    if(eventTrendUrlArrayRef.current?.find((item) => item.urlId === eventPkId)) {
      window.open(eventTrendUrlArrayRef.current.find((item) => item.urlId === eventPkId).url, "_blank");
    }
    else {
      console.log('eventPkId not found in eventTrendUrlArray: ', eventPkId)
    }
  }

  const handleEventLinkClick = (eventPkId) => {
    console.log('handleEventLinkClick..........')
    // console.log('eventReportUrl: ', eventReportUrlRef.current)
    // if (eventReportUrlRef.current) {
    //   window.open(eventReportUrlRef.current, "_blank");
    // }

    if(eventReportUrlArrayRef.current?.find((item) => item.urlId === eventPkId)) {
      window.open(eventReportUrlArrayRef.current.find((item) => item.urlId === eventPkId).url, "_blank");
    }
    else {
      console.log('eventPkId not found in eventReportUrlArray: ', eventPkId)
    }
  }

  // ---------- SUBMIT FORM GUARD ----------
  const onSubmitForm = () => {

    console.log('onSubmitForm...... eventReportUrl: ', eventReportUrlRef.current)
    console.log('onSubmitForm...... eventTrendUrl: ', eventTrendUrlRef.current)

    console.log('***** formData', formData)

 let caseAssignedToLabelAndValue = formData.data.container.caseAssignedTo
   formData.data.container.caseAssignedTo = caseAssignedToLabelAndValue.email

   // set caseAssignedToLabel in attributes
   formData.data.container.caseAssignedToLabel = caseAssignedToLabelAndValue.label

   // set case owner in attributes
   formData.data.container.caseOwner = keycloak.idTokenParsed.name || '';




   
    setLoading(true)
    const requiredFields = ['caseTitle']
    const missingFields = requiredFields.filter(
      (field) => !formData.data.container[field],
    )
    if (missingFields.length > 0) {
      setSnackbarMessages(['Please fill required case title field.'])
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

    console.log('Case Attributes: ', caseAttributes)
    CaseService.createCase(
      keycloak,
      JSON.stringify({
        caseDefinitionId: caseDefId,
      caseNo: null,
        businessKey: null,
        owner: {
          id: keycloak.idTokenParsed.sub || '',
          // id: '0fcfac9f-acf8-4a59-8992-0006bb6909c5',
          name: keycloak.idTokenParsed.name || '',
          email: keycloak.idTokenParsed.email || '',
          phone: keycloak.idTokenParsed.phone || '',
        },
        attributes: caseAttributes,
         caseUrl: buildCreateUrl(window.location.href),
        // eventTrendUrl: eventTrendUrlRef.current,
        // eventReportUrl: eventReportUrlRef.current,
        eventTrendUrls: eventTrendUrlArrayRef.current,
        eventReportUrls: eventReportUrlArrayRef.current,
        eventIds: eventIds
      //  caseUrl: (() => { 
      //   const uri = window.location.pathname;
      //   return uri === '/case-list/create' ? '/case-list/create?' : buildCreateUrl(window.location.href);
      //  })(),

      }),
    )
      .then((data) => {
        const businessKey = data.businessKey
        // setLastCreatedCase(data);

        console.log('Cleaned URL', buildCreateUrl(window.location.href))
        return CaseService.saveCase(
          keycloak,
          JSON.stringify({
            caseDefinitionId: caseDefId,
            assetName: assetName,
            isDraft: 'n',
            hierarchyName: hierarchyName,
            sourceSystem: sourceSystem,
            eventIds: eventIds,
            businessKey: businessKey, 
	 		caseNo: businessKey,
			caseNumber: businessKey,	
            owner: {
              id: keycloak.idTokenParsed.sub|| '',
              // id: '0fcfac9f-acf8-4a59-8992-0006bb6909c5',
              name: keycloak.idTokenParsed.name || '',
              email: keycloak.idTokenParsed.email || '',
              phone: keycloak.idTokenParsed.phone || '',
            },
            attributes: caseAttributes,
            caseUrl: buildCreateUrl(window.location.href),
            // eventTrendUrl: eventTrendUrlRef.current,
            // eventReportUrl: eventReportUrlRef.current,
            eventTrendUrls: eventTrendUrlArrayRef.current,
            eventReportUrls: eventReportUrlArrayRef.current,
            assignedToLabel: caseAssignedToLabelAndValue.label,

        /*   caseUrl: (() => { 
             const uri = window.location.pathname;
             return uri === '/case-list/create' ? '/case-list/create?' : buildCreateUrl(window.location.href);
            })(),  */


           // assignedTo: {emailId: formData.data.container.caseAssignedTo}
           assignedTo: formData.data.container.caseAssignedTo.map(email => ({ emailId: email }))

    //     assignedTo: formData.data.container.caseAssignedTo.email.map(email => ({ emailId: email }))

          }),
        ).then((saveData) => ({ ...saveData, caseNo: businessKey, businessKey }))
      })
      .then((data) => {
        setLastCreatedCase(data);
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
       
        setLoading(false)
        setIsSubmitting(false);
     
      })
  }

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

  return (
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
              {caseDef.name} 
            </Typography>

            {/* {eventTrendUrl && (
              <Button 
                color='inherit' 
                onClick={() => window.open(eventTrendUrl, '_blank')}
                sx={{ mr: 1 }}
              >
                Event Trend
              </Button>
            )}
            {eventReportUrl && (
              <Button 
                color='inherit' 
                onClick={() => window.open(eventReportUrl, '_blank')}
                sx={{ mr: 1 }}
              >
                Event Report
              </Button>
            )} */}

            { <Button color='inherit' hidden={true} onClick={onSave}>
              Save As Draft
            </Button>}			
          </Toolbar>
        </AppBar>

        <Grid
          container
          spacing={2}
          sx={{ display: 'flex', flexDirection: 'column', p: 3 }}
        >
          <Grid item xs={12}>
            <Box sx={{ pb: 1, display: 'flex', alignItems: 'center' }}>
              {form.toolTip && (
                <Tooltip title={form.toolTip}>
                  <QuestionCircleOutlined />
                </Tooltip>
              )}
              <Typography variant='h5' sx={{ ml: 1 }}>
                {form.title} 
              </Typography>
            </Box>

            {/* Form Component */}
            <Form
              form={form.structure}
              submission={formData}
              options={{
                fileService: new StorageService(),
              }}
              // onSubmit={(submission) => {
              //   console.log('Validation passed:', true)
              //   console.log('Form data:', submission)

              //   onSave(submission)
              // }}
              onError={(error) => {
                console.log('Validation failed:', error)
                setValidationSnackbarOpen(true)
              }}
              onCustomEvent={(event) => {
                console.log('event event:', event)
                if (
                  (event.component.key === 'saveAsDraft' || event.component.key === 'saveAsDraft1') &&
                  !isSubmitting
                ) {
                  setIsSubmitting(true);
                  onSubmitForm();
                   
                } else if (event.component.key === 'RecommendationSubmit3') {
                  onSubmitRecommendation()
                } else if (event.component.key === 'onSave') {
                  // onSubmitRecommendation()
                  onSave()
                }

                else if (event.component.key === 'btnEventTrend') {
                  handleEventTrendClick(event.data.eventPkId)
                }
                else if (event.component.key === 'btnEventLink') {
                  handleEventLinkClick(event.data.eventPkId)
                }
              }}
            />
          </Grid>
        </Grid>

        <Snackbar
          open={snackbarOpen}
          autoHideDuration={3000}
          onClose={() => setSnackbarOpen(false)}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
        >
          <SnackbarContent
            message={
              <div>
                {snackbarMessages.map((message, index) => (
                  <Typography
                    key={index}
                    variant='body2'
                    color='error'
                    component='div'
                  >
                    {message}
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
          open={validationSnackbarOpen}
          autoHideDuration={3000}
          onClose={() => setValidationSnackbarOpen(false)}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
        >
          <SnackbarContent
            message={
              <div>
                <Typography variant='body2' color='error' component='div'>
                  {'Please fill the required fields'}
                </Typography>
              </div>
            }
            action={
              <Button
                color='secondary'
                size='small'
                onClick={() => setValidationSnackbarOpen(false)}
              >
                Close
              </Button>
            }
          />
        </Snackbar>

        <Snackbar
          open={snackOpen}
          autoHideDuration={3000}
          message='Case Created'
          onClose={handleCloseSnack}
          action={snackAction}
        />
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
    </div>
  )
}
