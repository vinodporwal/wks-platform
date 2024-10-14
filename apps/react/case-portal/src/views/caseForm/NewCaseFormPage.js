import React, { useState, useEffect } from 'react';
import { QuestionCircleOutlined } from '@ant-design/icons';
import CloseIcon from '@mui/icons-material/Close';
import { Box, Tooltip } from '@mui/material';
import AppBar from '@mui/material/AppBar';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import Grid from '@mui/material/Grid';
import IconButton from '@mui/material/IconButton';
import Slide from '@mui/material/Slide';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import { Form } from '@formio/react';
import { useSession } from 'SessionStoreContext';
import { CaseService, FormService } from '../../services';
import { StorageService } from 'plugins/storage';
import { Snackbar, SnackbarContent } from '@mui/material';
import { useNavigate } from 'react-router-dom';

const Transition = React.forwardRef(function Transition(props, ref) {
  return <Slide direction="up" ref={ref} {...props} />;
});

export const NewCaseFormPage = ({
  open = true,
  caseDefId = 'create',
}) => {
  const [caseDef, setCaseDef] = useState([]);
  const [form, setForm] = useState([]);
  const [formData, setFormData] = useState(null);
  const [lastCreatedCase, setLastCreatedCase] = useState(null);
  const [snackOpen, setSnackOpen] = useState(false);
  const keycloak = useSession();
  const navigate = useNavigate();
  const [snackbarOpen, setSnackbarOpen] = useState(false);
  const [snackbarMessages, setSnackbarMessages] = useState([]);
  const [currentParams, setCurrentParams ] = useState([]);


  useEffect(() => {
    CaseService.getCaseDefinitionsById(keycloak, caseDefId)
      .then((data) => {
        setCaseDef(data);
        return FormService.getByKey(keycloak, data.formKey);
      })
      .then((data) => {
        console.log("new page form data", data);
        setForm(data);
        setFormData({
          data: {},
          metadata: {},
          isValid: true,
        });
      })
      .catch((err) => {
        console.error(err.message);
      });
  }, [caseDefId, keycloak]);

  const handleCloseSnack = () => {
    setSnackOpen(false);
  };

  const handleClose = () => {
    navigate('/home');
  };


  const onSave = () => {
    const currentParams = window.location.search;
    setCurrentParams(currentParams);
    const urlParams = new URLSearchParams(window.location.search);
  
    const assetName = urlParams.get('assetName') || 'default';
    const hierarchyName = urlParams.get('hierarchyName') || 'default';
    const eventIdsParam = urlParams.get('eventIds');
    const sourceSystem = urlParams.get('sourceSystem') || 'default';
    const eventIds = eventIdsParam ? eventIdsParam.split(',') : [];
    const caseAttributes = Object.keys(formData.data).map((key) => ({
      name: key,
      value: typeof formData.data[key] !== 'object'
        ? formData.data[key]
        : JSON.stringify(formData.data[key]),
      type: typeof formData.data[key] !== 'object' ? 'String' : 'Json',
    }));
  
    // First API call to createCase to get the businessKey
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
      })
    )
      .then((data) => {
        const businessKey = data.businessKey; 
        // setLastCreatedCase(data);
  
       
        return CaseService.saveCase(
          keycloak,
          JSON.stringify({
            caseDefinitionId: caseDefId,
            assetName: assetName,
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
          })
        );
      })
      .then((data) => {
        setLastCreatedCase(data);
        setSnackOpen(true);
        setTimeout(() => {
          handleClose();
        }, 6000);
      })
      .catch((err) => {
        console.error(err.message);
      });
  };



  const onSubmitRecommendation = (event) => {
    console.log('event onSubmitRecommendation', event);
  
    const { recommendationReviewer, recommendationAssignedTo1, recommendationHeadline, recommendationTargetCompletionDate1, RecommendationConfirm } = event.data;
  
    const missingFields = [];
    if (!recommendationReviewer) missingFields.push('Recommendation Reviewer');
    if (!recommendationAssignedTo1) missingFields.push('Recommendation Assigned To');
    if (!recommendationHeadline) missingFields.push('Recommendation Headline');
    if (!recommendationTargetCompletionDate1) missingFields.push('Target Completion Date');
    
    // New validation for RecommendationConfirm
    if (!RecommendationConfirm || !['Yes', 'No'].includes(RecommendationConfirm)) {
      missingFields.push('Recommendation Confirm');
    }
  
    if (missingFields.length > 0) {
      setSnackbarMessages(missingFields);
      setSnackbarOpen(true);
      setTimeout(() => {
        setSnackbarOpen(false);
      }, 6000);
      return;
    }
  
    setSnackbarMessages([]);
    // event.component.disabled = true;
  };
  


  const onSubmitForm = () => {
    const currentParams = window.location.search;
    setCurrentParams(currentParams);
    const urlParams = new URLSearchParams(window.location.search);
  
    const assetName = urlParams.get('assetName') || 'default';
    const hierarchyName = urlParams.get('hierarchyName') || 'default';
    const eventIdsParam = urlParams.get('eventIds');
    const sourceSystem = urlParams.get('sourceSystem') || 'default';
    const eventIds = eventIdsParam ? eventIdsParam.split(',') : [];
    const caseAttributes = Object.keys(formData.data).map((key) => ({
      name: key,
      value: typeof formData.data[key] !== 'object'
        ? formData.data[key]
        : JSON.stringify(formData.data[key]),
      type: typeof formData.data[key] !== 'object' ? 'String' : 'Json',
    }));
  
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
      })
    )
      .then((data) => {
        const businessKey = data.businessKey;
        // setLastCreatedCase(data);
  

        return CaseService.saveCase(
          keycloak,
          JSON.stringify({
            caseDefinitionId: caseDefId,
            assetName: assetName,
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
          })
        );
      })
      .then((data) => {
        setLastCreatedCase(data);
        setSnackOpen(true);
        setTimeout(() => {
          handleClose();
        }, 6000);
      })
      .catch((err) => {
        console.error(err.message);
      });
  };
  
  const snackAction = lastCreatedCase && (
    <React.Fragment>
      <Button
        color="primary"
        size="small"
        onClick={() => {
          navigate(`/case-list/create${currentParams}`);
          handleCloseSnack();
        }}
      >
        {lastCreatedCase.caseNo}
      </Button>
      <IconButton size="small" aria-label="close" color="inherit" onClick={handleCloseSnack}>
        <CloseIcon fontSize="small" />
      </IconButton>
    </React.Fragment>
  );

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
              edge="start"
              color="inherit"
              onClick={handleClose}
              aria-label="close"
            >
              <CloseIcon />
            </IconButton>
            <Typography sx={{ ml: 2, flex: 1 }} component="div">
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
              <Typography variant="h5" sx={{ ml: 1 }}>{form.title}</Typography>
            </Box>

            {/* Form Component */}
            <Form
              form={form.structure}
              submission={formData}
              options={{
                fileService: new StorageService(),
              }}
              onSubmit={(submission) => {
                console.log('Validation passed:', true); 
                console.log('Form data:', submission); 

                onSave(submission)
              }}
              onCustomEvent={(event) => {
                if (event.component.key === 'saveAsDraft') {
                  onSubmitForm(); 
                } else if (event.component.key === 'RecommendationSubmit') {
                  onSubmitRecommendation(event); 
                }

              }}
            />
      <Snackbar
        open={snackbarOpen}
        autoHideDuration={6000}
        onClose={() => setSnackbarOpen(false)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
      >
        <SnackbarContent
          message={
            <div>
              <Typography variant="body2" color="error" component="div">
                The following fields are required:
              </Typography>
              {snackbarMessages.map((message, index) => (
                <Typography key={index} variant="body2" component="div">
                  - {message}
                </Typography>
              ))}
            </div>
          }
          action={
            <Button color="secondary" size="small" onClick={() => setSnackbarOpen(false)}>
              Close
            </Button>
          }
        />
      </Snackbar>
          </Grid>
        </Grid>

        <Snackbar
          open={snackOpen}
          autoHideDuration={6000}
          message="Case saved as draft"
          onClose={handleCloseSnack}
          action={snackAction}
        />
      </Dialog>
    </div>
  );
};
