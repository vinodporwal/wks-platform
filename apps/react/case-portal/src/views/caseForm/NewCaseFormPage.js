import React, { useState, useEffect, useRef } from 'react'
import { QuestionCircleOutlined } from '@ant-design/icons'
import CloseIcon from '@mui/icons-material/Close'
import { Box, Tooltip, CircularProgress } from '@mui/material'
import AppBar from '@mui/material/AppBar'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import Grid from '@mui/material/Grid'
import IconButton from '@mui/material/IconButton'
import Slide from '@mui/material/Slide'
import Toolbar from '@mui/material/Toolbar'
import Typography from '@mui/material/Typography'
import { Form } from '@formio/react'
import { useSession } from 'SessionStoreContext'
import { CaseService, FormService } from '../../services'
import { StorageService } from 'plugins/storage'
import { Snackbar, SnackbarContent } from '@mui/material'
import { useNavigate } from 'react-router-dom'
import { buildCreateUrl } from 'utils/util'

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
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const [currentData, setCurrentData] = useState(null);
  const initialRender = useRef(true); // Track initial render

  const handleBeforeUnload = (event) => {
    if (hasUnsavedChanges) {
      const message = "You have unsaved changes. Are you sure you want to leave?";
      event.preventDefault();
      event.returnValue = message; 
      return message; 
    }
  };

  // const areObjectsEqualExcludingKeys = (obj1, obj2, keysToExclude) => {
  //   const allKeys = new Set([...Object.keys(obj1), ...Object.keys(obj2)]);
  //   keysToExclude.forEach(key => allKeys.delete(key));

  //   for (const key of allKeys) {
  //     if (Array.isArray(obj1[key]) && Array.isArray(obj2[key])) {
  //       // Compare based on array size
  //       if (obj1[key].length !== obj2[key].length) {
  //         return false;
  //       }
  //     } else if (obj1[key] !== obj2[key]) {
  //       return false;
  //     }
  //   }

  //   return true; 
  // };

  const areObjectsEqualExcludingKeys = (obj1, obj2, keysToExclude = []) => {
  const cleanObject = (obj) => {
    if (obj === null || typeof obj !== 'object') return obj;

    if (Array.isArray(obj)) {
      return obj.map(item => cleanObject(item));
    }

    return Object.keys(obj)
      .filter(key => !keysToExclude.includes(key))
      .sort() // ensure consistent key order
      .reduce((acc, key) => {
        acc[key] = cleanObject(obj[key]);
        return acc;
      }, {});
  };

  try {
    return JSON.stringify(cleanObject(obj1)) === JSON.stringify(cleanObject(obj2));
  } catch (e) {
    // fallback in rare cases (circular refs)
    return false;
  }
};


  const handleFormChange = (submission) => {
    if (initialRender.current) {
      initialRender.current = false;
      return; // Skip handling changes on the initial render
    }

    // Specify which keys to exclude from the key-value comparison
    const excludedKeys = [ 'caseNo', 'textField1', 'saveAsDraft1', 'onSave', 'saveAsDraft', 'analysisSubmit', 'analysisEdit', 'valueRealizationSubmit', 'recommendationFinalSubmit'];

    if(currentData){
      // Custom comparison logic
      if (!areObjectsEqualExcludingKeys(currentData, submission.data.container, excludedKeys)) {
        setHasUnsavedChanges(true);
      } else {
        setHasUnsavedChanges(false);
      }
    }
  };

  useEffect(() => {
    // Add the event listener for beforeunload
    window.addEventListener('beforeunload', handleBeforeUnload);

    // Cleanup function to remove the event listener
    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
      setHasUnsavedChanges(false);
    };
  }, [hasUnsavedChanges]); // Empty dependency array ensures this runs once on mount and unmount

  useEffect(() => {
    setHasUnsavedChanges(hasUnsavedChanges);
  }, [hasUnsavedChanges])
  
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
                saveAsDraft.hidden = true
              }

              const createButton =
                level7.columns.length > 2
                  ? level7.columns[2].components[1]
                  : null
              if (createButton) {
                createButton.hidden = false
              }

              const saveButton =
                level7.columns.length > 3
                  ? level7.columns[3].components[0]
                  : null
              if (saveButton) {
                saveButton.hidden = true
              }
            }
          }

          //Hide analysis save and edit button on case create page.
          const analysisSection = level1.components.find(comp => comp.title === 'Analysis') || null

          if(analysisSection){
            const analysisSubmitButton = analysisSection.components[0].columns.length > 2 ? analysisSection.components[0].columns[2].components[3]: null;
            const analysisEditButton = analysisSection.components[0].columns.length > 2 ? analysisSection.components[0].columns[2].components[4]: null;

            if(analysisSubmitButton){
              analysisSubmitButton.hidden = true;
            }

            if(analysisEditButton){
              analysisEditButton.hidden = true;
            }
          }

          //  const level6 = level1.components[6] ?? null;
          //   if (level6) {
          //    const recommendationFinalSubmit = level6.components[1]?.columns[1]?.components[0] ?? null;
          //     recommendationFinalSubmit.disabled = true;
            // }  
            const recommendations = level1.components.find(comp => comp.label === '"Recommendations"') || null;
            if (recommendations) {
              recommendationFinalSubmit.disabled = true;
            }  
        }

        setFormData({
          data: {container: {createdBy:keycloak.idTokenParsed.preferred_username}},
          metadata: {},
          isValid: true,
        })

        setCurrentData({createdBy:keycloak.idTokenParsed.preferred_username})
      })
      .catch((err) => {
        console.error(err.message)
      })
  }, [caseDefId, keycloak])

  const handleCloseSnack = () => {
    setSnackOpen(false)
  }

  const handleClose = () => {
    const params = currentParams.length > 0 ? currentParams : window.location.search
    if (hasUnsavedChanges) {
      const confirmLeave = window.confirm("You have unsaved changes. Do you really want to leave?");
      if (!confirmLeave) return; // Stop closing modal if user cancels
    }
    navigate(`/case-list/create${params}`)
  }

  const onSave = () => {
    setLoading(true)

    const requiredFields = ['caseDescription', 'dueDate', 'faultCategory']

    const faultCategoryValue = formData.data.container.faultCategory
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
    const caseAttributes = Object.keys(formData.data).map((key) => ({
      name: key,
      value:
        typeof formData.data[key] !== 'object'
          ? formData.data[key]
          : JSON.stringify(formData.data[key]),
      type: typeof formData.data[key] !== 'object' ? 'String' : 'Json',
    }))

    // First API call to createCase to get the businessKey

    console.log('Cleaned URL', buildCreateUrl(window.location.href))

    CaseService.createCase(
      keycloak,
      JSON.stringify({
        caseDefinitionId: caseDefId,
        owner: {
          id: keycloak.subject || '',
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
            owner: {
              id: keycloak.subject || '',
              // id: '0fcfac9f-acf8-4a59-8992-0006bb6909c5',
              name: keycloak.idTokenParsed.name || '',
              email: keycloak.idTokenParsed.email || '',
              phone: keycloak.idTokenParsed.phone || '1234543211',
            },
            attributes: caseAttributes,
            caseUrl: buildCreateUrl(window.location.href),
            isFinalRecommendationSubmitted: false
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

  const onSubmitForm = () => {
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
        owner: {
          id: keycloak.subject || '',
          // id: '0fcfac9f-acf8-4a59-8992-0006bb6909c5',
          name: keycloak.idTokenParsed.name || '',
          email: keycloak.idTokenParsed.email || '',
          phone: keycloak.idTokenParsed.phone || '',
        },
        attributes: caseAttributes,
        caseUrl: buildCreateUrl(window.location.href),
        isFinalRecommendationSubmitted: false
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
            path: formData.data.container.path,
            attributes: caseAttributes,
            caseUrl: buildCreateUrl(window.location.href),
            assignedTo: {emailId: formData.data.container.caseAssignedTo}
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
              onChange={(submission) => handleFormChange(submission)} // Listen for changes
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
                if (event.component.key === 'saveAsDraft' || event.component.key === 'saveAsDraft1') {
                  setHasUnsavedChanges(false);
                  onSubmitForm()
                } else if (event.component.key === 'RecommendationSubmit3') {
                  onSubmitRecommendation()
                } else if (event.component.key === 'onSave') {
                  // onSubmitRecommendation()
                  onSave()
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
