import QuestionCircleOutlined from '@ant-design/icons/QuestionCircleOutlined'
import { Form } from '@formio/react'
import CloseIcon from '@mui/icons-material/Close'
import MoreVertIcon from '@mui/icons-material/MoreVert'
import NotificationsActiveIcon from '@mui/icons-material/NotificationsActive'
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
import React, { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ProcessDefService } from 'services/ProcessDefService'
import { Comments } from 'views/caseComment/Comments'
import { CaseEmailsList } from 'views/caseEmail/caseEmailList'
import { CaseService, FormService } from '../../services'
import { tryParseJSONObject } from '../../utils/jsonStringCheck'
import { TaskList } from '../taskList/taskList'
import Documents from './Documents'
import { Snackbar, SnackbarContent } from '@mui/material';
import { useNavigate } from 'react-router-dom';

export const CaseForm = ({ open, handleClose, aCase, keycloak }) => {
  const [caseDef, setCaseDef] = useState(null)
  const [form, setForm] = useState(null)
  const [formData, setFormData] = useState(null)
  const [comments, setComments] = useState(null)
  const [documents, setDocuments] = useState(null)
  const [mainTabIndex, setMainTabIndex] = useState(0)
  const [rightTabIndex, setRightTabIndex] = useState(0)
  const [activeStage, setActiveStage] = React.useState(0)
  const [stages, setStages] = useState([])
  const { t } = useTranslation()

  const [anchorEl, setAnchorEl] = React.useState(null)
  const isMenuOpen = Boolean(anchorEl)

  const [openProcessesDialog, setOpenProcessesDialog] = useState(false)
  const [manualInitProcessDefs, setManualInitProcessDefs] = useState([])

  const [isFollowing, setIsFollowing] = useState(false)
  const [isFormData, setIsFormData] = useState(false)

  const navigate = useNavigate();
  const [snackbarOpen, setSnackbarOpen] = useState(false);
  const [snackbarMessages, setSnackbarMessages] = useState([]);
  const [currentParams, setCurrentParams ] = useState([]);
  const [lastCreatedCase, setLastCreatedCase] = useState(null);
  const [snackOpen, setSnackOpen] = useState(false);
  const [formStructure, setFormStructure] = useState(null);
  
  const handleFollowClick = () => {
    setIsFollowing(!isFollowing)
  }

  useEffect(() => {
    getCaseInfo(aCase)
  }, [open, aCase])

  useEffect(() => {
    console.log('{keycloak.idTokenParsed.given_name}', keycloak.idTokenParsed)
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
    setSnackOpen(false);
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




  // const getCaseInfo = (aCase) => {
  //   CaseService.getCaseDefinitionsById(keycloak, aCase.caseDefinitionId)
  //     .then((data) => {
  //       setCaseDef(data)
  //       setStages(
  //         data.stages.sort((a, b) => a.index - b.index).map((o) => o.name),
  //       )
  //       return FormService.getByKey(keycloak, data.formKey)
  //     })
  //     .then((data) => {
  //       setForm(data)
  //       console.log('data', data);
  //       return CaseService.getCaseById(keycloak, aCase.businessKey)
  //     })
  //     .then((caseData) => {
  //       setComments(
  //         caseData?.comments?.sort(
  //           (a, b) =>
  //             new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
  //         ),
  //       )
  //       setDocuments(caseData?.documents)
  //       setFormData({
  //         data: caseData.attributes.reduce(
  //           (obj, item) =>
  //             Object.assign(obj, {
  //               [item.name]: tryParseJSONObject(item.value)
  //                 ? JSON.parse(item.value)
  //                 : item.value,
  //             }),
  //           {},
  //         ),
  //         metadata: {},
  //         isValid: true,
  //       })
  //       setActiveStage(caseData.stage)
  //     })
  //     .catch((err) => {
  //       console.log(err.message)
  //     })
  // }
  
  
  const getCaseInfo = (aCase) => {

    CaseService.getCaseDefinitionsById(keycloak, aCase.caseDefinitionId)
      .then(async (data) => {
        setCaseDef(data);
        setStages(
          data.stages.sort((a, b) => a.index - b.index).map((o) => o.name)
        );

        const formData = await FormService.getByKey(keycloak, data.formKey);
        setFormStructure(formData)
        if (formData && formData.structure && formData.structure.components) {
          const updatedFormStructure = { ...formData };
          console.log('formData', formData);
  
          // Disable fields (with proper null checks)
          const level1 = updatedFormStructure.structure.components[0];
          if (level1 && level1.components) {
            const level2 = level1.components[0];
            const level7 = level1.components.length > 8 ? level1.components[8] : null;
            const level6 = level1.components.length > 6 ? level1.components[6] : null;
            console.log('level6', level6)
            if (level2 && level2.components) {
              const caseDescriptionField = level2.components.length > 1 ? level2.components[1] : null;
              if (caseDescriptionField) {
                caseDescriptionField.disabled = false;
              }
  
              const recommendation = level1.components.length > 5 ? level1.components[5] : null;
              if (recommendation) {
                recommendation.disabled = true;
              }
  
              if (level2.components[0] && level2.components[0].columns) {
                const caseNo = level2.components[0].columns.length > 1 ? level2.components[0].columns[0].components[0] : null;
               
                if (caseNo) {
                  caseNo.calculateValue = `value = ${aCase.caseNo}`;
                }

                const caseTitleField = level2.components[0].columns.length > 1 ? level2.components[0].columns[1].components[0] : null;
                if (caseTitleField) {
                  caseTitleField.disabled = true;
                }
  
                const caseAssign = level2.components[0].columns.length > 2 ? level2.components[0].columns[2].components[0] : null;
                if (caseAssign) {
                  caseAssign.disabled = true;
                }
                console.log('aCase', aCase)

                // const caseAssign1 = level2.components[0].columns.length > 2 ? level2.components[0].columns[3].components[0] : null;
                // console.log('caseAssign1', caseAssign1, aCase)
                // if (caseAssign1) {
                //   caseAssign1.defaultValue = `1_true`;
                // }
                
              }
  
              if (level7 && level7.columns) {
                const saveAsDraft = level7.columns.length > 2 ? level7.columns[2].components[0] : null;
                if (saveAsDraft) {
                  saveAsDraft.hidden = false;
                }
  
                const saveButton = level7.columns.length > 3 ? level7.columns[3].components[0] : null;
                if (saveButton) {
                  saveButton.hidden = false;
                }
              }

              if (level6) {
                const recommendationDescription = level6.components[0].components[0].columns[0].components[1];
                if (recommendationDescription) {
                  recommendationDescription.disabled = !(aCase.owner?.email === keycloak.idTokenParsed?.email);
                }
                // console.log('saveAsDraft', recommendationDescription)
              }
            }
          }
  
  
          setForm(updatedFormStructure);
        } else {
          console.error("Form structure or components are undefined.");
        }
        setIsFormData(true)

        return CaseService.getCaseById(keycloak, aCase.businessKey);

      })
      .then((caseData) => {
        setComments(
          caseData?.comments?.sort(
            (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
          )
        );
        setDocuments(caseData?.documents);
        setFormData({
          data: caseData.attributes.reduce(
            (obj, item) =>
              Object.assign(obj, {
                [item.name]: tryParseJSONObject(item.value)
                  ? JSON.parse(item.value)
                  : item.value,
              }),
            {}
          ),
          metadata: {},
          isValid: true,
        });
        setActiveStage(caseData.stage);
      })
      .catch((err) => {
        console.log(err.message);
      });
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
  

    CaseService.createCase(
      keycloak,
      JSON.stringify({
        caseDefinitionId: aCase.caseDefinitionId,
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

        return CaseService.saveCase(
          keycloak,
          JSON.stringify({
            caseDefinitionId: aCase.caseDefinitionId,
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
  
    // First API call to createCase to get the businessKey
    CaseService.createCase(
      keycloak,
      JSON.stringify({
        caseDefinitionId: aCase.caseDefinitionId,
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
        const businessKey = data.businessKey; // Extract businessKey from the response
        // setLastCreatedCase(data);
  
        // Second API call to saveCase with the businessKey
        return CaseService.saveCase(
          keycloak,
          JSON.stringify({
            caseDefinitionId: aCase.caseDefinitionId,
            assetName: assetName,
            hierarchyName: hierarchyName,
            sourceSystem: sourceSystem,
            eventIds: eventIds,
            businessKey: businessKey, // Include businessKey in the payload
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
        setSnackOpen(true); // Show success notification
        setTimeout(() => {
          handleClose();
        }, 6000);
      })
      .catch((err) => {
        console.error(err.message);
      });
  };
  
  
  const handleMainTabChanged = (event, newValue) => {
    console.log(event, newValue)
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

  const updateActiveState = () => {
    CaseService.getCaseById(keycloak, aCase.businessKey).then((data) =>
      setActiveStage(data.stage),
    )
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

 
  const printCaseDetails = () => {
    // Add CSS rules to hide buttons, icons, and borders for print
    const printStyles = `
      <style>
        @media print {
          button, input[type="button"], .print-hidden, .fa, [ref="icon"] {
            display: none !important;
          }
          * {
            border: none !important;
          }
        }
      </style>
    `;
  
    // Insert print styles into the document head
    document.head.insertAdjacentHTML('beforeend', printStyles);
  
    // Print the current page
    window.print();
  
    // Optionally, remove the print styles after printing to avoid affecting the current page view
    const styleElement = document.head.querySelector('style');
    if (styleElement) {
      document.head.removeChild(styleElement);
    }
  };
  

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
                  {caseDef.name}: {aCase?.businessKey}
                </div>
                {/* <div style={{ fontSize: '13px' }}>
                  {aCase?.statusDescription}
                </div> */}
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
              {/* <Button
                color='inherit'
                onClick={handleFollowClick}
                startIcon={<NotificationsActiveIcon />}
              >
                {isFollowing ? 'Unfollow' : 'Follow'}
              </Button> */}
              <Button
                color="inherit"
                onClick={printCaseDetails}
              >
                {'Print'}
              </Button>

              <Button
                color='inherit'
                onClick={onSave}
              >
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
                  {/* <Tab
                    label={t('pages.caseform.tabs.comments')}
                    {...a11yProps(2)}
                  /> */}
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
                      <Tooltip title={form.toolTip}>
                        <QuestionCircleOutlined />
                      </Tooltip>
                    </Box>
                    {isFormData && <Form
                      form={form.structure}
                      submission={formData}
                      options={{
                        // readOnly: true,
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
                    />}
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
                    <Snackbar
                      open={snackOpen}
                      autoHideDuration={6000}
                      message="Case saved as draft"
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
