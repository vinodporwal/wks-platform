import React, { useState, useEffect } from 'react'
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

Formio.options = {
  vm: {
    timeout: 25000
  }
}
const Transition = React.forwardRef(function Transition(props, ref) {
  return <Slide direction='up' ref={ref} {...props} />
})

export const NewCaseFormPage = ({ open = true, caseDefId = 'create' }) => {
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

  // ---------- ROLE LOGIC: viewer vs creator ----------
  if (!keycloak || !keycloak.idTokenParsed) {
    console.log('Keycloak or idTokenParsed missing:', keycloak)
    return null
  }

  // collect roles (realm + client)
  const realmRoles = keycloak.idTokenParsed.realm_access?.roles || []
  const clientRoles = keycloak.idTokenParsed.resource_access
    ? Object.values(keycloak.idTokenParsed.resource_access).flatMap(
        (client) => client.roles || [],
      )
    : []
  const allRoles = [...realmRoles, ...clientRoles]
  console.log('all roles: ' + allRoles)

  // independent role flags
  const hasViewerRole = allRoles.includes('case_viewer')
  const hasCreatorRole = allRoles.includes('case_creator')

  // who can view? creators should also be able to view
  const canView = hasViewerRole || hasCreatorRole
  const canCreate = hasCreatorRole

  // view-only mode when user has viewer role but NOT creator role
  const viewOnly = hasViewerRole && !hasCreatorRole

  const [showViewOnlyDialog, setShowViewOnlyDialog] = useState(viewOnly)

  const handleCloseViewOnly = () => {
    setShowViewOnlyDialog(false) // closes the popup
    handleClose() // also close parent if needed
  }

  // keep canAnalyze (if you still use it elsewhere)
  const canAnalyze = allRoles.includes('case_analyst')

  // Show dialog if user cannot view at all
  const [noAccessOpen, setNoAccessOpen] = useState(true)
  if (!canView) {
    return (
      <Dialog open={noAccessOpen} onClose={() => setNoAccessOpen(false)}>
        <Box sx={{ p: 4, minWidth: 300 }}>
          <Typography variant='h6' color='error' sx={{ mb: 2 }}>
            You do not have permission to view this page.
          </Typography>
          <Button
            variant='contained'
            color='primary'
            onClick={() => setNoAccessOpen(false)}
          >
            Close
          </Button>
        </Box>
      </Dialog>
    )
  }
  // ---------------------------------------------------

  useEffect(() => {
    
    const params = window.location.search
    setCurrentParams(params)
  }, [])

  useEffect(() => {
    CaseService.getCaseDefinitionsById(keycloak, caseDefId)
      .then((data) => {
        setCaseDef(data)
       
        return FormService.getByKey(keycloak, data.formKey)
      })
      .then((data) => {
        console.log('new page form data', data)
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

  const handleCloseSnack = () => {
    setSnackOpen(false)
  }

  const handleClose = () => {
    const params =
      currentParams.length > 0 ? currentParams : window.location.search
    console.log('currentParams', params)
    navigate(`/case-list/create${params}`)
  }

  // ---------- SAVE GUARD uses viewOnly/canCreate ----------
  const onSave = () => {
    if (viewOnly) {
      setSnackbarMessages([
        'You have view-only permission. Cannot create or edit.',
      ])
      setSnackbarOpen(true)
      return
    }
    if (!canCreate) {
      setSnackbarMessages(['You do not have permission to create cases.'])
      setSnackbarOpen(true)
      return // stop function here
    }

    console.log('In new case form page onSave')
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
	updFormData.businessKey = data.businessKey;
	updFormData.caseNo = data.businessKey;

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
          id: keycloak.subject || '',
          // id: '0fcfac9f-acf8-4a59-8992-0006bb6909c5',
          name: keycloak.idTokenParsed.name || '',
          email: keycloak.idTokenParsed.email || '',
          phone: keycloak.idTokenParsed.phone || '',
        },
        attributes: caseAttributes,
        caseUrl: buildCreateUrl(window.location.href),
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
              id: keycloak.subject || '',
              // id: '0fcfac9f-acf8-4a59-8992-0006bb6909c5',
              name: keycloak.idTokenParsed.name || '',
              email: keycloak.idTokenParsed.email || '',
              phone: keycloak.idTokenParsed.phone || '1234543211',
            },
            attributes: caseAttributes,
            caseUrl: buildCreateUrl(window.location.href),
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

  // ---------- SUBMIT FORM GUARD ----------
  const onSubmitForm = () => {
    if (viewOnly) {
      setSnackbarMessages(['You have view-only permission. Cannot submit.'])
      setSnackbarOpen(true)
      return
    }
    if (!canCreate) {
      setSnackbarMessages(['You do not have permission to create cases.'])
      setSnackbarOpen(true)
      return // stop function here
    }

    console.log('In new case form page : onSubmitForm')
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

    console.log('Cleaned URL', buildCreateUrl(window.location.href))
    CaseService.createCase(
      keycloak,
      JSON.stringify({
        caseDefinitionId: caseDefId,
      caseNo: null,
        businessKey: null,
        owner: {
          id: keycloak.subject || '',
          // id: '0fcfac9f-acf8-4a59-8992-0006bb6909c5',
          name: keycloak.idTokenParsed.name || '',
          email: keycloak.idTokenParsed.email || '',
          phone: keycloak.idTokenParsed.phone || '',
        },
        attributes: caseAttributes,
        caseUrl: buildCreateUrl(window.location.href),
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
              id: keycloak.subject || '',
              // id: '0fcfac9f-acf8-4a59-8992-0006bb6909c5',
              name: keycloak.idTokenParsed.name || '',
              email: keycloak.idTokenParsed.email || '',
              phone: keycloak.idTokenParsed.phone || '',
            },
            attributes: caseAttributes,
            caseUrl: buildCreateUrl(window.location.href),
           // assignedTo: {emailId: formData.data.container.caseAssignedTo}
           assignedTo: formData.data.container.caseAssignedTo.map(email => ({ emailId: email }))
          }),
        )
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
            {/* Save button visible only if user can create (not view-only) */}
            {canCreate && (
              <Button color='inherit' onClick={onSave}>
                Save As Draft
              </Button>
            )}
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
                readOnly: viewOnly, // READ-ONLY for view-only users
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
                // block custom events for view-only users
                if (viewOnly) {
                  return
                }
                if (
                  event.component.key === 'saveAsDraft' ||
                  event.component.key === 'saveAsDraft1'
                ) {
                  onSubmitForm()
                } else if (event.component.key === 'RecommendationSubmit3') {
                  onSubmitRecommendation()
                } else if (event.component.key === 'onSave') {
                  // onSubmitRecommendation()
                  onSave()
                }
              }}
            />

            {/* Show Alert Dialog if user is view-only */}
            {viewOnly && (
              <Dialog
                open={showViewOnlyDialog}
                onClose={handleCloseViewOnly}
                aria-labelledby='view-only-dialog'
                maxWidth='sm'
                fullWidth
              >
                <DialogTitle id='view-only-dialog'>
                  <Box sx={{ display: 'flex', alignItems: 'center' }}>
                    <NotificationsActiveIcon color='warning' sx={{ mr: 1 }} />
                    View-Only Access
                  </Box>
                </DialogTitle>
                <DialogContent>
                  <DialogContentText>
                    You have view-only permission. Creating or editing cases is
                    disabled.
                  </DialogContentText>
                </DialogContent>
                <DialogActions>
                  <Button
                    onClick={handleCloseViewOnly}
                    variant='contained'
                    color='primary'
                    autoFocus
                  >
                    OK
                  </Button>
                </DialogActions>
              </Dialog>
            )}
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
