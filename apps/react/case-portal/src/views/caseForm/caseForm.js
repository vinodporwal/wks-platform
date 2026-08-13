/* eslint-disable no-unused-vars */
import QuestionCircleOutlined from '@ant-design/icons/QuestionCircleOutlined'
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
import Tooltip from '@mui/material/Tooltip'
import Typography from '@mui/material/Typography'
import { CaseStatus } from 'common/caseStatus'
import { StorageService } from 'plugins/storage'
import PropTypes from 'prop-types'
import React, { useEffect, useState, useRef } from 'react'
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
import { accountStore } from './../../store'
import html2pdf from "html2pdf.js/dist/html2pdf"

import pdfMake from 'pdfmake/build/pdfmake'
import pdfFonts from 'pdfmake/build/vfs_fonts'

pdfMake.vfs = pdfFonts?.pdfMake?.vfs || pdfFonts?.vfs

export const CaseForm = ({ open, handleClose, aCase, keycloak }) => {
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
  const [isFinalRecommendationConfirmationOpen, setIsFinalRecommendationConfirmationOpen] = useState(false)
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const [currentData, setCurrentData] = useState(null);
  const initialRender = useRef(true); // Track initial render
  const [isCommentEnabled, setIsCommentEnabled] = useState(true);
  const [isAttachmentEnabled, setIsAttachmentEnabled] = useState(true);
  const [isDraft, setIsDraft] = useState(true);

const initialDataRef = useRef(null);
const isFormReadyRef = useRef(false);
const hasUnsavedChangesRef = useRef(false); 

useEffect(() => {
  hasUnsavedChangesRef.current = hasUnsavedChanges;
}, [hasUnsavedChanges]);



const handleFormChange = (submission) => {
  if (!submission?.data?.container) return;
  if (!isFormReadyRef.current) return;

  // If initialDataRef is somehow null, capture and wait
  if (!initialDataRef.current) {
    initialDataRef.current = JSON.parse(JSON.stringify(submission.data.container));
    return;
  }

  const excludedKeys = [
    'caseNo', 'textField1', 'saveAsDraft1', 'onSave', 'saveAsDraft',
    'analysisSubmit', 'analysisEdit', 'valueRealizationSubmit',
    'recommendationFinalSubmit'
  ];

  const clean = (obj) => {
    const clone = JSON.parse(JSON.stringify(obj));
    excludedKeys.forEach(key => delete clone[key]);
    return clone;
  };

  const isEqual =
    JSON.stringify(clean(initialDataRef.current)) ===
    JSON.stringify(clean(submission.data.container));

  setHasUnsavedChanges(!isEqual);
};
  
  const handleBeforeUnload = (event) => {
    if (hasUnsavedChangesRef.current) {
      const message = "You have unsaved changes. Are you sure you want to leave?";
      event.preventDefault();
      event.returnValue = message; 
      return message; 
    }
  };
  
  const areObjectsEqualExcludingKeys = (obj1, obj2, keysToExclude = []) => {
    const allKeys = new Set([...Object.keys(obj1), ...Object.keys(obj2)]);
    keysToExclude.forEach(key => allKeys.delete(key));

    for (const key of allKeys) {
      if (Array.isArray(obj1[key]) && Array.isArray(obj2[key])) {
        // Compare based on array size
        if (obj1[key].length !== obj2[key].length) {
          return false;
        }
      } else if (obj1[key] !== obj2[key]) {
        return false;
      }
    }

    return true; 
  };

  // const handleFormChange = (submission) => {
  //   if (initialRender.current) {
  //     initialRender.current = false;
  //     return; // Skip handling changes on the initial render
  //   }

  //   // Specify which keys to exclude from the key-value comparison
  //   const excludedKeys = [ 'caseNo', 'textField1', 'saveAsDraft1', 'onSave', 'saveAsDraft', 'analysisSubmit', 'analysisEdit', 'valueRealizationSubmit', 'recommendationFinalSubmit', 'valueRealizationCategory', 'valueRealizationConclusion'];

  //   // Custom comparison logic
  //   if (!areObjectsEqualExcludingKeys(currentData, submission.data.container, excludedKeys)) {
  //     setHasUnsavedChanges(true);
  //   } else {
  //     setHasUnsavedChanges(false);
  //   }
  // };
  
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
    localStorage.setItem('aCaseOwnerEmail', JSON.stringify(aCase.owner?.email))
    getCaseInfo(aCase)
    //   FileService.downloadForPrintPreview(aCase.documents[0], keycloak),
    // )
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
    isFormReadyRef.current = false;
    initialDataRef.current = null;
    setHasUnsavedChanges(false);
    await loadOptions(keycloak);
    console.log('Fetching EED case data of ', aCase)
    // setLoading(true)
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
          console.log('Form data -> ', formData)
          updatedFormStructure = { ...formData }
        } else {
          console.error('Form structure or components are undefined.')
        }
        setIsFormData(true)

        // return CaseService.getCaseById(keycloak, aCase.businessKey);

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
        setIsDraft(isDraft);
        const isFinalRecommendationSubmitted = caseData?.isFinalRecommendationSubmitted;

        const attributeValue = caseData.attributes[0].value;
        const parsedAttributeValue = JSON.parse(attributeValue);
        parsedAttributeValue.caseNo = aCase.caseNo;
        setCurrentData(parsedAttributeValue)
        initialDataRef.current = JSON.parse(JSON.stringify(parsedAttributeValue));
        const userEmailIds = [];
        const currentUserName = keycloak.idTokenParsed.preferred_username;
        const caseAssignedToEmail = caseData?.assignedTo?.emailId;
        userEmailIds.push(caseData?.owner?.email);
        userEmailIds.push(parsedAttributeValue.caseAssignedTo);
        

        const analysisTeamEmails = Array.isArray(parsedAttributeValue.analysisTeam) ? parsedAttributeValue.analysisTeam : (parsedAttributeValue.analysisTeam ? [parsedAttributeValue.analysisTeam] : []);
        const userEmailIdsIncludingAnalysisTeam = userEmailIds.concat(analysisTeamEmails);

        const currentUserEmail = keycloak.idTokenParsed.email;
        const checkEmailMatch = (email) => {
          if (!email) return false;
          const emailLower = email.toLowerCase();
          return (
            emailLower.startsWith(currentUserName.toLowerCase() + '@') ||
            emailLower === currentUserName.toLowerCase() ||
            emailLower === currentUserName.toLowerCase() + '@' ||
            (currentUserEmail && emailLower === currentUserEmail.toLowerCase())
          );
        };
        let shouldDisable = !userEmailIds.some(checkEmailMatch);
        let shouldDisableAnalysis = !userEmailIdsIncludingAnalysisTeam.some(checkEmailMatch);

        console.log('--- Permissions Check ---');
        console.log('Current Logged-in Username:', currentUserName);
        console.log('Analysis Team Emails on Case:', analysisTeamEmails);
        console.log('All Authorized Emails (Including Analysis Team):', userEmailIdsIncludingAnalysisTeam);
        console.log('Is User an Analysis Team Member? (!shouldDisableAnalysis):', !shouldDisableAnalysis);

        const recommendations = parsedAttributeValue.dataGrid1;
        const recommendationAssignees = recommendations?.map((item) => item.recommendationAssignedTo2).filter((assignee) => assignee !== "");
        const recommendationReviewers = recommendations?.map((item) => item.recommendationReviewer).filter((assignee) => assignee !== "");
        const userEmailIdsWithRecommendationAssignees = userEmailIds.concat(recommendationAssignees);
        const userEmailIdsWithRecommendationUsers = userEmailIdsWithRecommendationAssignees.concat(recommendationReviewers);
        let shouldDisableValueRealization = !userEmailIdsWithRecommendationUsers.some(email => email.startsWith(currentUserName + '@'));

        if(accountStore.isManagerUser(keycloak)){
          shouldDisable = shouldDisableAnalysis = shouldDisableValueRealization = false;
        }
        // Disable fields (with proper null checks)
        const level1 = updatedFormStructure.structure.components[0]

        if (shouldDisable && shouldDisableAnalysis && shouldDisableValueRealization) {
          // Disable the top-level component
          level1.disabled = true;
          setIsAttachmentEnabled(false);
          setIsCommentEnabled(false);
          if (level1?.components) {
            const [level2, , , , analysisSection, , level6, valueRealizationSection, level7] = level1.components;

            // Analysis Section: Hide submit and edit buttons
            if (analysisSection?.components[0]?.columns.length > 2) {
              const analysisColumns = analysisSection.components[0].columns[2];
              const analysisSubmitButton = analysisColumns.components?.[3] ?? null;
              const analysisEditButton = analysisColumns.components?.[4] ?? null;

              analysisSubmitButton && (analysisSubmitButton.hidden = true);
              analysisEditButton && (analysisEditButton.hidden = true);
            }

            // Level 7: Hide draft, create, and save buttons
            if (level7?.columns) {
              const saveAsDraft = level7.columns?.[2]?.components?.[0] ?? null;
              const createButton = level7.columns?.[2]?.components?.[1] ?? null;
              const saveButton = level7.columns?.[3]?.components?.[0] ?? null;

              saveAsDraft && (saveAsDraft.hidden = true);
              createButton && (createButton.hidden = true);
              saveButton && (saveButton.hidden = true);
            }

            // Level 6: Hide add-more and final submit buttons
            if (level6?.components) {
              const [submitContainer, addMoreContainer] = level6.components;
              const recommendationAddMore = addMoreContainer?.columns?.[0]?.components?.[0] ?? null;
              const recommendationFinalSubmit = addMoreContainer?.columns?.[1]?.components?.[0] ?? null;

              recommendationAddMore && (recommendationAddMore.hidden = true);
              recommendationFinalSubmit && (recommendationFinalSubmit.hidden = true);
            }

            // Value Realization Section: Hide value realization submit button
            if (valueRealizationSection?.components) {
              const valueRealizationSubmit = valueRealizationSection.components?.[3] ?? null;
              valueRealizationSubmit && (valueRealizationSubmit.hidden = true);
            }
          }
        } else {
          if (!isDraft) {
            const analysis = level1.components?.[4] ?? null;
            const recommendationRadio = level1.components?.[5] ?? null;
            const recommendation = level1.components?.[6] ?? null;
            const caseDetails = level1.components?.[3] ?? null;
            const valueRealization = level1.components?.[7] ?? null;
            level1.components?.forEach((component) => {
              if (
                component.id !== recommendation?.id &&
                component.id !== caseDetails?.id &&
                component.id !== analysis?.id &&
                component.id !== valueRealization?.id &&
                component.id !== recommendationRadio.id
              ) {
                component.disabled = true;
              }
            });

            if (parsedAttributeValue.valueRealizationCategory !== '') {
              valueRealization.disabled = true;
            }
            const caseDetails0 = caseDetails?.components?.[0];
            if (caseDetails0) {
              caseDetails0.disabled = true;
            }

            const caseDetails1 = caseDetails?.components?.[1];
            const analysisTeam = caseDetails1?.columns?.[0]?.components?.[0] ?? null;
            const caseStatus = caseDetails1?.columns?.[1]?.components?.[0] ?? null;

            // Disable all components inside columns of caseDetails1, except caseStatus
            caseDetails1?.columns?.forEach((column) => {
              column?.components?.forEach((component) => {
                if (component.id !== caseStatus?.id) {
                  if (component.id === analysisTeam?.id && !shouldDisableAnalysis) {
                    console.log('Unlocking Analysis Team dropdown for Analysis Team member.');
                    component.disabled = false;
                  } else {
                    component.disabled = true;
                  }
                }
              });
            });
          }

          if (level1 && level1.components) {
            const level2 = level1.components[0]
            const level7 =
              level1.components.length > 8 ? level1.components[8] : null
            if (level2 && level2.components) {
              const caseDescriptionField =
                level2.components.length > 1 ? level2.components[1] : null
              if (caseDescriptionField) {
                caseDescriptionField.disabled = false
              }

              // const recommendation =
              //   level1.components.length > 5 ? level1.components[5] : null
              // if (recommendation) {
              //   recommendation.disabled = true
              // }

              if (level2.components[0] && level2.components[0].columns) {
                const caseNo =
                  level2.components[0].columns.length > 1
                    ? level2.components[0].columns[0].components[0]
                    : null

                if (caseNo) {
                  caseNo.calculateValue = `value = ${aCase.caseNo}`
                }

                // const caseTitleField =
                //   level2.components[0].columns.length > 1
                //     ? level2.components[0].columns[1].components[0]
                //     : null
                // if (caseTitleField) {
                //   caseTitleField.disabled = true
                // }

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

                if (shouldDisable && caseStatus) {
                  caseStatus.disabled = true;
                }

                const faultCategorySelect =
                  level2.components[0].columns.length > 2
                    ? level2.components[0].columns[3].components[0]
                    : null
                if (faultCategorySelect && caseData.isDraft == 'n') {
                  faultCategorySelect.disabled = true
                }

                // const caseAssign1 = level2.components[0].columns.length > 2 ? level2.components[0].columns[3].components[0] : null;
                // console.log('caseAssign1', caseAssign1, aCase)
                // if (caseAssign1) {
                //   caseAssign1.defaultValue = `1_true`;
                // }
              }

              //Hide analysis save and edit button conditionally.
              const analysisSection = level1.components.find(comp => comp.title === 'Analysis') || null

              if (analysisSection) {
                const analysisSubmitButton = analysisSection.components[0].columns.length > 2 ? analysisSection.components[0].columns[2].components[3] : null;
                const analysisEditButton = analysisSection.components[0].columns.length > 2 ? analysisSection.components[0].columns[2].components[4] : null;

                if (isFinalRecommendationSubmitted) {
                  analysisSection.disabled = true
                  analysisSubmitButton.hidden = true;
                  analysisEditButton.hidden = true;
                } else {
                  if (analysisSubmitButton && parsedAttributeValue.analysisDesc !== '') {
                    analysisSubmitButton.hidden = true;
                  }
                  if (analysisEditButton && parsedAttributeValue.analysisDesc === '') {
                    analysisEditButton.hidden = true;
                  }

                  if (shouldDisableAnalysis) {
                    analysisSubmitButton.disabled = true;
                    analysisEditButton.disabled = true;
                  }
                }
              }

              if (level7 && level7.columns) {
                const saveAsDraft =
                  level7.columns.length > 2
                    ? level7.columns[2].components[0]
                    : null
                if (saveAsDraft) {
                  saveAsDraft.hidden = isDraft ? false : true;
                  if (shouldDisable) {
                    saveAsDraft.disabled = true;
                  }
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
                  saveButton.hidden = isDraft ? false : true;
                  if (shouldDisable) {
                    saveButton.disabled = true;
                  }
                }
              }

              const level6 = level1.components[6] ?? null;
              if (level6) {
                const [submitContainer, addMoreContainer] = level6.components;

                const recommendationSubmit = submitContainer?.components?.[0]?.columns?.[4]?.components?.[0]?.columns?.[0]?.components?.[0] ?? null;
                const recommendationDelete = submitContainer?.components?.[0]?.columns?.[4]?.components?.[0]?.columns?.[1]?.components?.[0] ?? null;
                const recommendationAddMore = addMoreContainer?.columns[0]?.components[0] ?? null;
                const recommendationFinalSubmit = addMoreContainer?.columns[1]?.components[0] ?? null;

                if (shouldDisableAnalysis) {
                  if (recommendationSubmit) recommendationSubmit.disabled = true;
                  if (recommendationDelete) recommendationDelete.disabled = true;
                }

                if (recommendationAddMore && (isFinalRecommendationSubmitted || shouldDisableAnalysis)) {
                  recommendationAddMore.disabled = true;
                }

                const recommendations = parsedAttributeValue.dataGrid1;
                if (recommendationFinalSubmit && (isFinalRecommendationSubmitted || shouldDisableAnalysis || !recommendations || (recommendations?.length >= 1 && recommendations[0]?.recommendationNo1 === ''))) {
                  recommendationFinalSubmit.disabled = true;
                }
              }

              const valueRealizationSection = level1.components[7] ?? null;
              if (valueRealizationSection && shouldDisableValueRealization) {
                valueRealizationSection.disabled = true;
                const valueRealizationSubmit = valueRealizationSection?.components?.[3] ?? null;

                if (valueRealizationSubmit) {
                  valueRealizationSubmit.hidden = true;
                }
              }

            }
          }
        }

        setForm({
          ...updatedFormStructure,
        })

        // setIsDraft(caseData?.isDraft === 'y')
        isFormReadyRef.current = false;
        setHasUnsavedChanges(false);
        setTimeout(() => {
          isFormReadyRef.current = true;
        }, 2000);

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
        console.log(err.message)
      })
    // .finally(() => {
    //   setLoading(false)
    // })
  }

  const onSave = () => {
   // setHasUnsavedChanges(false);
    setLoading(true)
    const {
      caseDescription,
      dueDate,
      faultCategory,
      analysisTeam,
    } = formData.data.container

    const missingFields = []
    if (!caseDescription)
      missingFields.push('Case Description')
    if (!dueDate)
      missingFields.push('Due Date')
    if (!faultCategory)
      missingFields.push('Fault Category')
    if (!analysisTeam || analysisTeam.length === 0)
      missingFields.push('Analysis Team')

    if (missingFields.length > 0) {
      setSnackbarMessages(missingFields)
      setSnackbarOpen(true)
      setTimeout(() => {
        setSnackbarOpen(false)
      }, 2000)
      setLoading(false)
      return
    }

    if (formData.data.container.caseStatus === 1) {
      const actionAssignedId = getcaseStatusValue('Under Analysis');
      formData.data.container.caseStatus = actionAssignedId;
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
        caseNo: aCase.caseNo,
        attributes: caseAttributes,
        caseUrl: buildCreateUrl(window.location.href),
        businessKey: aCase.businessKey,
      }),
    )
      .then((data) => {
        const businessKey = data.businessKey

        return CaseService.updateCase(
          keycloak,
          JSON.stringify({
            caseDefinitionId: aCase.caseDefinitionId,
            assetName: assetName,
            isDraft: 'n',
            hierarchyName: hierarchyName,
            sourceSystem: sourceSystem,
            eventIds: eventIds,
            businessKey: businessKey,
            attributes: caseAttributes,
            caseUrl: buildCreateUrl(window.location.href),
            assignedTo: { emailId: formData.data.container.caseAssignedTo },
            isFinalRecommendationSubmitted: aCase.isFinalRecommendationSubmitted,
            owner: aCase.owner,
            path: formData.data.container.path,
          }),
        )
      })
      .then((data) => {
        initialDataRef.current = JSON.parse(JSON.stringify(formData.data.container));
        setHasUnsavedChanges(false);
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

  const onAnalysisSave = () => {
    setLoading(true)
    const {
      caseCauseCategory,
      caseCauseDescription,
      analysisDesc,
      diagnosis,
    } = formData.data.container

    const missingFields = []
    if (!caseCauseCategory)
      missingFields.push('Case cause category.')
    if (!caseCauseDescription || caseCauseDescription.length === 0)
      missingFields.push('Case cause description.')
    if (!analysisDesc)
      missingFields.push('Observations')
    if (!diagnosis)
      missingFields.push('Diagnosis')

    if (missingFields.length > 0) {
      setSnackbarMessages(missingFields)
      setSnackbarOpen(true)
      setTimeout(() => {
        setSnackbarOpen(false)
      }, 2000)
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
        caseNo: aCase.caseNo,
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
            isDraft: aCase.isDraft,
            hierarchyName: hierarchyName,
            sourceSystem: sourceSystem,
            eventIds: eventIds,
            businessKey: businessKey,
            attributes: caseAttributes,
            caseUrl: buildCreateUrl(window.location.href),
            assignedTo: { emailId: formData.data.container.caseAssignedTo },
            isFinalRecommendationSubmitted: aCase.isFinalRecommendationSubmitted,
            owner: aCase.owner,
            path: formData.data.container.path,
          }),
        )
      })
      .then((data) => {
        initialDataRef.current = JSON.parse(JSON.stringify(formData.data.container));
        setHasUnsavedChanges(false);
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

  const onValueRealizationSubmit = () => {
    setLoading(true)
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
        caseNo: aCase.caseNo,
        attributes: caseAttributes,
        caseUrl: buildCreateUrl(window.location.href),
        businessKey: aCase.businessKey,
      }),
    )
      .then((data) => {
        const businessKey = data.businessKey

        return CaseService.saveValueRealization(
          keycloak,
          JSON.stringify({
            caseDefinitionId: aCase.caseDefinitionId,
            assetName: assetName,
            isDraft: aCase.isDraft,
            hierarchyName: hierarchyName,
            sourceSystem: sourceSystem,
            eventIds: eventIds,
            businessKey: businessKey,
            attributes: caseAttributes,
            caseUrl: buildCreateUrl(window.location.href),
            assignedTo: { emailId: formData.data.container.caseAssignedTo },
            isFinalRecommendationSubmitted: aCase.isFinalRecommendationSubmitted,
            owner: aCase.owner,
            path: formData.data.container.path,
          }),
        )
      })
      .then((data) => {
        initialDataRef.current = JSON.parse(JSON.stringify(formData.data.container));
        setHasUnsavedChanges(false);
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

  const onRecommendationFinalSubmit = () => {
    setLoading(true)

    const actionAssignedId = getcaseStatusValue('Resolved');
    formData.data.container.caseStatus = actionAssignedId;

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
        caseNo: aCase.caseNo,
        attributes: caseAttributes,
        caseUrl: buildCreateUrl(window.location.href),
        businessKey: aCase.businessKey,
      }),
    )
      .then((data) => {
        const businessKey = data.businessKey

        return CaseService.submitFinalRecommendation(
          keycloak,
          JSON.stringify({
            caseDefinitionId: aCase.caseDefinitionId,
            assetName: assetName,
            isDraft: aCase.isDraft,
            hierarchyName: hierarchyName,
            sourceSystem: sourceSystem,
            eventIds: eventIds,
            businessKey: businessKey,
            attributes: caseAttributes,
            caseUrl: buildCreateUrl(window.location.href),
            assignedTo: { emailId: formData.data.container.caseAssignedTo },
            isFinalRecommendationSubmitted: true,
            owner: aCase.owner
          }),
        )
      })
      .then((data) => {
        initialDataRef.current = JSON.parse(JSON.stringify(formData.data.container));
        setHasUnsavedChanges(false);
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
        setIsFinalRecommendationConfirmationOpen(false);
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
    setCurrentData(updatedFormData)

    if (!formData.data.container.analysisDesc) {
      setSnackbarMessages(['Please submit analysis before posting recommendation.'])
      setSnackbarOpen(true)
      setTimeout(() => {
        setSnackbarOpen(false)
      }, 2000)
      return
    }

    setSnackbarMessages([])

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
      if (response.status !== 500) {
        console.log('Recommendation submitted successfully:', response)
        setSnackbarMessages(['Recommendation submitted successfully'])
        setSnackbarOpen(true)
        setIsConfirmationOpen(false)
        setTimeout(() => {
          window.location.href = response.caseUrl;
          // window.location.reload()
        }, 1000)
      } else {
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
    CaseService.createCase(
      keycloak,
      JSON.stringify({
        caseDefinitionId: aCase.caseDefinitionId,
        caseNo: aCase.caseNo,
        owner: {
          id: keycloak.subject || '',
          // id: '0fcfac9f-acf8-4a59-8992-0006bb6909c5',
          name: keycloak.idTokenParsed.name || '',
          email: keycloak.idTokenParsed.email || '',
          phone: keycloak.idTokenParsed.phone || '',
        },
        attributes: caseAttributes,
        caseUrl: buildCreateUrl(window.location.href),
        businessKey: aCase.businessKey,
      }),
    )
      .then((data) => {
        const businessKey = data.businessKey // Extract businessKey from the response
        // setLastCreatedCase(data);
        initialDataRef.current = JSON.parse(JSON.stringify(formData.data.container));
        setHasUnsavedChanges(false);
        // Second API call to saveCase with the businessKey
        return CaseService.updateCase(
          keycloak,
          JSON.stringify({
            caseDefinitionId: aCase.caseDefinitionId,
            assetName: assetName,
            isDraft: 'y',
            hierarchyName: hierarchyName,
            sourceSystem: sourceSystem,
            eventIds: eventIds,
            businessKey: businessKey, // Include businessKey in the payload
            owner: aCase.owner,
            attributes: caseAttributes,
            caseUrl: buildCreateUrl(window.location.href),
            assignedTo: { emailId: formData.data.container.caseAssignedTo },
            path: formData.data.container.path,
          }),
        )
      })
      .then((data) => {
        initialDataRef.current = JSON.parse(JSON.stringify(formData.data.container));
        setHasUnsavedChanges(false);
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

  const getcaseStatusValue = (label) => {
    // Retrieve options from localStorage
    const options = JSON.parse(localStorage.getItem('caseStatusOptions')) || []

    // Find the option with the matching value and return its label
    const matchingOption = options.find((option) => option.label === label)
    return matchingOption ? matchingOption.value : label // Fallback to value if no match is found
  }

  const handleMainTabChanged = async (event, newValue) => {
    if (newValue == 1) {
      const caseData = await CaseService.getCaseById(
        keycloak,
        aCase.businessKey,
      )
      const documents = caseData?.documents || []
      setDocuments(documents)
    }
    setMainTabIndex(newValue)
  }

  // const handleRightTabChanged = (event, newValue) => {
  //   setRightTabIndex(newValue)
  // }

  const handleUpdateCaseStatus = (newStatus) => {
    CaseService.patch(
      keycloak,
      aCase.businessKey,
      JSON.stringify({
        status: newStatus,
      }),
    )
      .then(() => {
        close()
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

  const getValueRealizationCategoryLabel = (value) => {
    // Retrieve options from localStorage
    const options = [
      {
        "label": "Value Realization category",
        "value": "valueRealizationCategory"
      },
      {
        "label": "Condition normalized before alarm",
        "value": "conditionNormalizedBeforeAlarm"
      },
      {
        "label": "Job could be planned in next opportunity",
        "value": "jobCouldBePlannedInNextOpportunity"
      },
      {
        "label": "Instrument malfunction detected",
        "value": "instrumentMalfunctionDetected"
      },
      {
        "label": "Operating condition could be normalized",
        "value": "operatingConditionCouldBeNormalized"
      },
      {
        "label": "Performance deterioration could be identified",
        "value": "performanceDeteriorationCouldBeIdentified"
      }
    ];

    // Find the option with the matching value and return its label
    const matchingOption = options.find((option) => option.value === value)
    return matchingOption ? matchingOption.label : value // Fallback to value if no match is found
  }

  const getSAPRequestLabel = (value) => {
    // Retrieve options from localStorage
    const options = [
      {
        "label": "Yes",
        "value": "y"
      },
      {
        "label": "No",
        "value": "n"
      },
    ];

    // Find the option with the matching value and return its label
    const matchingOption = options.find((option) => option.value.toLowerCase() === value.toLowerCase())
    return matchingOption ? matchingOption.label : value // Fallback to value if no match is found
  }

  const formatPdfValue = (value) => {
    if (value === null || value === undefined || value === '') return 'N/A'
    if (Array.isArray(value)) return value.join(', ')
    return String(value)
  }

  const renderKeyValueGrid = (items) => {
    return `
    <div style="display: flex; flex-wrap: wrap; font-size: 11.5px; line-height: 1.35;">
      ${items.map((item) => `
        <div style="
          box-sizing: border-box;
          width: ${item.full ? '100%' : '50%'};
          padding: 2px 6px 3px 0;
          margin-bottom: 3px;
          word-break: break-word;
        ">
          <span style="font-weight: bold;">${item.label}:</span>
          <span>${formatPdfValue(item.value)}</span>
        </div>
      `).join('')}
    </div>
  `
  }

  // Function to format data grids in a 2-column layout without colons in labels, skipping specific fields
  const formatDataGrid = (dataGrid, getLabel) => {
    if (!dataGrid || dataGrid.length === 0) return '<p>No data available</p>'

    const fieldsToSkip = [
      'textField1',
      'RecommendationSubmit',
      'recommendationAssignedTo1',
      'deleteRowButton4',
      'RecommendationSubmit3',
      'deleteRowButton5',
    ] // Add any keys you want to skip here

    return dataGrid
      .map((item) => {
        return `
      <div style="display: flex; flex-wrap: wrap; border: 1px solid #ccc; padding: 5px; margin-bottom: 7px; page-break-inside: avoid; break-inside: avoid;">
        ${Object.entries(item)
            .map(([key, value]) =>
              fieldsToSkip.includes(key)
                ? ''
                : `
            <div style="flex: 0 0 calc(50% - 8px); max-width: calc(50% - 8px); box-sizing: border-box; border: 1px solid #ddd; margin: 3px; padding: 5px; font-size: 11.5px; line-height: 1.3; word-break: break-word;">
              <p style="font-weight: bold; margin: 0;">${getLabel(key)}</p>
              <p style="margin: 0;">
                ${key === 'equipmentFunctionLocation' ? getEquipmentFunctionLocationLabel(value) : key === 'RecommendationConfirmSAP3' ? getSAPRequestLabel(value) : value || ''}
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

  const generatePrintContent = (aCase, structure, uploadedDocuments, base64Map = {}) => {
    const containerData = JSON.parse(
      aCase.attributes.find((attr) => attr.name === 'container').value,
    )
    const labelMap = createLabelMapFromStructure(structure)
    console.log('labelMap', labelMap)
    const getLabel = (key) => labelMap[key] || key || ''

    // Files are stored inside the container attribute JSON by formio — same source as the detail page

    let content = `
    <div style="font-family: Arial, sans-serif; padding: 20px; border: 1px solid #333;">
      <div style="display: flex; align-items: center; justify-content: center; margin-bottom: 20px;">
        <h2 style="text-align: center; margin: 0;">EED Case Management System</h2>
      </div>

      <!-- Case Information Panel -->
      <div style="border: 1px solid #333; border-radius: 5px; margin-bottom: 20px; page-break-inside: avoid;">
        <h3 style="background-color: #333; color: #fff; padding: 10px; margin-left: 1px; margin-right: 1px;">Case Information</h3>
        <div style="padding: 10px;">
          <p><strong>${getLabel('caseNo')}</strong>: ${aCase.caseNo}</p>
          <p><strong>${getLabel('caseTitle')}</strong>: ${containerData.caseTitle}</p>
          <p><strong>${getLabel('caseAssignedTo')}</strong>: ${containerData.caseAssignedTo}</p>
          <p><strong>${getLabel('faultCategory')}</strong>: ${getFaultCategoryLabel(containerData.faultCategory)}</p>
          <p><strong>${getLabel('caseDescription')}</strong>: ${containerData.caseDescription}</p>
        </div>
      </div>

      <!-- Case Details -->
      <div style="border: 1px solid #333; border-radius: 5px; margin-bottom: 20px; page-break-inside: avoid;">
        <h3 style="background-color: #333; color: #fff; padding: 10px; margin-left: 1px; margin-right: 1px;">Case Details</h3>
        <div style="padding: 10px;">
          <p><strong>${getLabel('createdOn')}</strong>: ${new Date(containerData.createdOn).toLocaleDateString()}</p>
          <p><strong>${getLabel('dueDate')}</strong>: ${containerData?.dueDate || 'N/A'}</p>
          <p><strong>${getLabel('endDate')}</strong>: ${containerData?.endDate || 'N/A'}</p>
          <p><strong>${getLabel('caseStatus')}</strong>: ${getcaseStatusLabel(containerData.caseStatus)}</p>
          <p><strong>${getLabel('analysisTeam')}</strong>: ${containerData.analysisTeam.join(', ')}</p>
        </div>
      </div>

      <!-- Associated Faults -->
      <div style="border: 1px solid #333; border-radius: 5px; margin-bottom: 20px; page-break-inside: avoid;">
        <h3 style="background-color: #333; color: #fff; padding: 10px; margin-left: 1px; margin-right: 1px;">Associated Faults</h3>
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
    const files = Array.isArray(containerData.file) ? containerData.file : []
    content += `
      <!-- Analysis -->
      <div style="border: 1px solid #333; border-radius: 5px; margin-bottom: 20px;">
        <div style="page-break-inside: avoid; break-inside: avoid; padding: 20px;">
          <h3 style="background-color: #333; color: #fff; padding: 10px; margin: 0;">Analysis</h3>
          <div style="padding: 10px;">
            <p><strong>${getLabel('caseCauseCategory')}</strong>: ${caseCauseCategoryLabel}</p>
            <p><strong>${getLabel('caseCauseDescription')}</strong>: ${caseCauseDescriptionLabel}</p>
            <p><strong>${getLabel('analysisDesc')}</strong>: <span style="white-space: pre-wrap;">${containerData.analysisDesc}</span></p>
            <p><strong>${getLabel('diagnosis')}</strong>: <span style="white-space: pre-wrap;">${containerData.diagnosis}</span></p>
          </div>
        </div>
        ${files.length > 0 ? `
          <div style="padding: 10px;">
            <p style="font-weight: bold; margin-bottom: 8px;">Uploaded Files</p>
            ${files.map((file) => {
              const src = base64Map[file.name] || `${Config.StorageUrl}/storage/files1/${file.dir || 'cases'}/downloads/${encodeURIComponent(file.name)}?content-type=${encodeURIComponent(file.type)}`
              const isImage = file.type && file.type.startsWith('image/')
              return `
                <div style="page-break-inside: avoid; break-inside: avoid; margin-bottom: 16px;">
                  <p style="margin: 0 0 4px 0; font-size: 12px; color: #555;">${file.name}</p>
                  ${isImage
                    ? `<img src="${src}" alt="${file.name}" style="max-width: 100%; max-height: 240mm; height: auto; display: block; object-fit: contain;" />`
                    : `<span style="font-size: 13px;">${file.name}</span>`
                  }
                </div>`
            }).join('')}
          </div>
        ` : ''}
      </div>`
    if (containerData.RecommendationsRadio === 'yes') {
      content += `
      <!-- Data Grid 1 -->
      <div class="no-break" style="border: 1px solid #333; border-radius: 5px; margin-bottom: 20px;  padding: 20px;">
        <h3 style="background-color: #333; color: #fff; padding: 10px; margin: 0;">${getLabel('dataGrid1')}</h3>
        ${formatDataGrid(containerData.dataGrid1, getLabel)}
      </div>
    `
    }

    // Value Realization section
    content += `
      <!-- Value Realization -->
      <div style="border: 1px solid #333; border-radius: 5px; margin-bottom: 20px; padding: 20px; page-break-inside: avoid;">
        <h3 style="background-color: #333; color: #fff; padding: 10px; margin: 0;">Value Realization</h3>
        <div style="padding: 10px;">
          <p><strong>${getLabel('valueRealizationCategory')}</strong>: ${getValueRealizationCategoryLabel(containerData.valueRealizationCategory)}</p>
          <p><strong>${getLabel('productionLoss')}</strong>: ${containerData.productionLoss || ''}</p>
          <p><strong>${getLabel('manHoursCost')}</strong>: ${containerData.manHoursCost || ''}</p>
          <p><strong>${getLabel('spareCost')}</strong>: ${containerData.spareCost || ''}</p>
          <p><strong>${getLabel('totalValueCaptured')}</strong>: ${containerData.totalValueCaptured}</p>
          <p><strong>${getLabel('valueRealizationConclusion')}</strong>: ${containerData.valueRealizationConclusion}</p>
        </div>
      </div>
  `

    return content
  }

  const storageDownloadUrl = (file) => {
    if (!file?.name) return null

    const storageBaseUrl = String(Config.StorageUrl || '').trim().replace(/\/+$/, '')
    if (!storageBaseUrl) return null

    const storageApiUrl = /\/storage$/i.test(storageBaseUrl)
      ? storageBaseUrl
      : `${storageBaseUrl}/storage`
    const directory = String(file.dir || 'cases')
      .split('/')
      .filter(Boolean)
      .map((segment) => encodeURIComponent(segment))
      .join('/')
    const contentType = encodeURIComponent(file.type || 'application/octet-stream')

    return `${storageApiUrl}/files1/${directory}/downloads/${encodeURIComponent(file.name)}?content-type=${contentType}`
  }

  const preparedImageKey = (file) =>
    `${String(file?.dir || 'cases').replace(/^\/+|\/+$/g, '')}/${file?.name || ''}`

  const blobAsDataUrl = (blob, url) =>
    new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onloadend = () => resolve(reader.result)
      reader.onerror = () => reject(new Error(`Image conversion failed for ${url}`))
      reader.readAsDataURL(blob)
    })

  const fetchImageAsBase64 = async (file) => {
    const url = storageDownloadUrl(file)
    if (!url) throw new Error('Image download URL could not be constructed.')

    let response
    try {
      response = await fetch(url, {
        headers: { Authorization: `Bearer ${keycloak.token}` },
      })
    } catch (error) {
      throw new Error(`Image request failed for ${url}: ${error.message}`)
    }

    if (!response.ok) {
      throw new Error(`Image download failed for ${url} with status ${response.status}.`)
    }

    const blob = await response.blob()
    try {
      const dataUrl = await blobAsDataUrl(blob, url)
      if (typeof dataUrl !== 'string' || !dataUrl.startsWith('data:image/')) {
        throw new Error(`Image conversion returned an invalid Data URL for ${url}`)
      }
      return dataUrl
    } catch (error) {
      throw new Error(
        `${error.message} (content-type: ${response.headers.get('content-type') || 'unknown'}, blob type: ${blob.type || 'unknown'}, blob size: ${blob.size})`,
      )
    }
  }

  const pdfText = (value) => {
    if (value === null || value === undefined || value === '') return 'N/A'
    if (Array.isArray(value)) return value.join(', ')
    return String(value)
  }

  const pdfKeyValueCell = (label, value) => ({
    text: [
      { text: `${label}: `, bold: true },
      { text: pdfText(value) },
    ],
    fontSize: 8.5,
    margin: [3, 2, 3, 2],
  })

  const pdfKeyValueGrid = (items) => {
    const rows = []
    let i = 0

    while (i < items.length) {
      const first = items[i]

      if (first.full) {
        rows.push([
          {
            ...pdfKeyValueCell(first.label, first.value),
            colSpan: 2,
          },
          {},
        ])
        i += 1
        continue
      }

      const second = items[i + 1]

      if (second && !second.full) {
        rows.push([
          pdfKeyValueCell(first.label, first.value),
          pdfKeyValueCell(second.label, second.value),
        ])
        i += 2
      } else {
        rows.push([
          pdfKeyValueCell(first.label, first.value),
          { text: '', border: [false, false, false, false] },
        ])
        i += 1
      }
    }

    return {
      table: {
        widths: ['50%', '50%'],
        body: rows,
      },
      layout: 'noBorders',
    }
  }

  const pdfSection = (title, body, unbreakable = true) => ({
    unbreakable,
    margin: [0, 0, 0, 7],
    table: {
      widths: ['*'],
      body: [
        [
          {
            text: title,
            bold: true,
            color: '#ffffff',
            fillColor: '#333333',
            fontSize: 10,
            margin: [5, 3, 5, 3],
          },
        ],
        [
          {
            stack: Array.isArray(body) ? body : [body],
            margin: [5, 5, 5, 5],
          },
        ],
      ],
    },
    layout: {
      hLineColor: () => '#333333',
      vLineColor: () => '#333333',
      hLineWidth: () => 0.7,
      vLineWidth: () => 0.7,
      paddingLeft: () => 0,
      paddingRight: () => 0,
      paddingTop: () => 0,
      paddingBottom: () => 0,
    },
  })

  const pdfFaultCard = (item, getLabel, removeMainAsset = false) => {
    const fieldsToSkip = [
      'textField1',
      'RecommendationSubmit',
      'recommendationAssignedTo1',
      'deleteRowButton4',
      'RecommendationSubmit3',
      'deleteRowButton5',
    ]

    const entries = Object.entries(item).filter(([key]) => {
      const label = getLabel(key)
      const isMainAsset =
        removeMainAsset &&
        label &&
        label.replace(/\s+/g, '').toLowerCase() === 'mainasset'

      return !fieldsToSkip.includes(key) && !isMainAsset
    })

    const rows = []

    for (let i = 0; i < entries.length; i += 2) {
      const [key1, value1] = entries[i]
      const second = entries[i + 1]

      const firstValue =
        key1 === 'equipmentFunctionLocation'
          ? getEquipmentFunctionLocationLabel(value1)
          : key1 === 'RecommendationConfirmSAP3'
            ? getSAPRequestLabel(value1)
            : value1

      const firstCell = {
        stack: [
          { text: getLabel(key1), bold: true, fontSize: 8.5 },
          { text: pdfText(firstValue), fontSize: 8.5 },
        ],
        margin: [4, 3, 4, 3],
      }

      let secondCell = { text: '', border: [false, false, false, false] }

      if (second) {
        const [key2, value2] = second

        const secondValue =
          key2 === 'equipmentFunctionLocation'
            ? getEquipmentFunctionLocationLabel(value2)
            : key2 === 'RecommendationConfirmSAP3'
              ? getSAPRequestLabel(value2)
              : value2

        secondCell = {
          stack: [
            { text: getLabel(key2), bold: true, fontSize: 8.5 },
            { text: pdfText(secondValue), fontSize: 8.5 },
          ],
          margin: [4, 3, 4, 3],
        }
      }

      rows.push([firstCell, secondCell])
    }

    return {
      unbreakable: true,
      margin: [0, 0, 0, 6],
      table: {
        widths: ['50%', '50%'],
        body: rows,
        dontBreakRows: true,
      },
      layout: {
        hLineColor: () => '#dddddd',
        vLineColor: () => '#dddddd',
        hLineWidth: () => 0.6,
        vLineWidth: () => 0.6,
        paddingLeft: () => 0,
        paddingRight: () => 0,
        paddingTop: () => 0,
        paddingBottom: () => 0,
      },
    }
  }

  const pdfDataGrid = (dataGrid, getLabel, removeMainAsset = false) => {
    if (!dataGrid || dataGrid.length === 0) {
      return [{ text: 'No data available', fontSize: 8.5 }]
    }

    return dataGrid.map((item) => pdfFaultCard(item, getLabel, removeMainAsset))
  }


  const generatePdfMakeDefinition = (
    aCase,
    structure,
    preparedImages = {},
    caseDocuments = [],
  ) => {
    const containerData = JSON.parse(
      aCase.attributes.find((attr) => attr.name === 'container').value,
    )

    const labelMap = createLabelMapFromStructure(structure)
    const getLabel = (key) => labelMap[key] || key || ''

    const caseCauseCategoryLabel = getCategoryLabel(
      containerData.caseCauseCategory,
    )

    const caseCauseDescriptionLabel = getCaseCauseDescriptionLabel(
      containerData.caseCauseDescription,
      containerData.caseCauseCategory,
    )

    const files = Array.isArray(containerData.file)
      ? containerData.file.filter((file) => file?.name)
      : []
    const validDocuments = Array.isArray(caseDocuments)
      ? caseDocuments.filter((file) => file?.name && storageDownloadUrl(file))
      : []

    const content = [
      {
        text: 'EED Case Management System',
        alignment: 'center',
        fontSize: 14,
        margin: [0, 0, 0, 8],
      },

      pdfSection(
        'Case Information',
        pdfKeyValueGrid([
          { label: getLabel('caseNo'), value: aCase.caseNo },
          { label: getLabel('caseTitle'), value: containerData.caseTitle },
          { label: getLabel('caseAssignedTo'), value: containerData.caseAssignedTo },
          {
            label: getLabel('faultCategory'),
            value: getFaultCategoryLabel(containerData.faultCategory),
          },
          {
            label: getLabel('caseDescription'),
            value: containerData.caseDescription,
            full: true,
          },
        ]),
        true,
      ),

      pdfSection(
        'Case Details',
        pdfKeyValueGrid([
          {
            label: getLabel('createdOn'),
            value: containerData.createdOn
              ? new Date(containerData.createdOn).toLocaleDateString()
              : 'N/A',
          },
          { label: getLabel('dueDate'), value: containerData?.dueDate || 'N/A' },
          { label: getLabel('endDate'), value: containerData?.endDate || 'N/A' },
          {
            label: getLabel('caseStatus'),
            value: getcaseStatusLabel(containerData.caseStatus),
          },
          {
            label: getLabel('analysisTeam'),
            value: containerData.analysisTeam,
            full: true,
          },
        ]),
        true,
      ),

      pdfSection(
        'Associated Faults',
        [
          {
            text: [
              { text: `${getLabel('textField1')}: `, bold: true },
              { text: pdfText(containerData.textField1) },
            ],
            fontSize: 8.5,
            margin: [3, 0, 3, 5],
          },
          ...pdfDataGrid(containerData.dataGrid2, getLabel, true),
        ],
        false,
      ),

      pdfSection(
        'Analysis',
        [
          pdfKeyValueGrid([
            {
              label: getLabel('caseCauseCategory'),
              value: caseCauseCategoryLabel,
            },
            {
              label: getLabel('caseCauseDescription'),
              value: caseCauseDescriptionLabel,
            },
          ]),
          {
            text: [
              { text: `${getLabel('analysisDesc')}: `, bold: true },
              { text: pdfText(containerData.analysisDesc) },
            ],
            fontSize: 8.5,
            margin: [3, 4, 3, 2],
          },
          {
            text: [
              { text: `${getLabel('diagnosis')}: `, bold: true },
              { text: pdfText(containerData.diagnosis) },
            ],
            fontSize: 8.5,
            margin: [3, 2, 3, 2],
          },
          ...files.map((file) => {
            const isImage = file.type && file.type.startsWith('image/')
            const preparedImage = preparedImages[preparedImageKey(file)]
            const imageSrc = preparedImage?.dataUrl

            if (isImage && imageSrc) {
              return {
                stack: [
                  {
                    text: file.name,
                    fontSize: 8,
                    color: '#555555',
                    margin: [0, 5, 0, 2],
                  },
                  {
                    image: imageSrc,
                    fit: [500, 400],
                    margin: [0, 0, 0, 5],
                  },
                ],
              }
            }

            return {
              stack: [
                {
                  text: file.name,
                  fontSize: 8.5,
                  margin: [0, 5, 0, 2],
                },
                ...(isImage && preparedImage?.failed
                  ? [{ text: 'Image unavailable', italics: true, color: '#777777', fontSize: 8 }]
                  : []),
              ],
            }
          }),
        ],
        false,
      ),
    ]

    // if (containerData.RecommendationsRadio === 'yes') {
    //   content.push(
    //     pdfSection(
    //       getLabel('dataGrid1'),
    //       pdfDataGrid(containerData.dataGrid1, getLabel, false),
    //       false,
    //     ),
    //   )
    // }

    if (containerData.RecommendationsRadio === 'yes') {
      const recommendationSection = pdfSection(
        getLabel('dataGrid1'),
        pdfDataGrid(containerData.dataGrid1, getLabel, false),
        false,
      )

      recommendationSection.pageBreak = 'before'

      content.push(recommendationSection)
    }

    content.push(
      pdfSection(
        'Value Realization',
        pdfKeyValueGrid([
          {
            label: getLabel('valueRealizationCategory'),
            value: getValueRealizationCategoryLabel(
              containerData.valueRealizationCategory,
            ),
          },
          { label: getLabel('productionLoss'), value: containerData.productionLoss },
          { label: getLabel('manHoursCost'), value: containerData.manHoursCost },
          { label: getLabel('spareCost'), value: containerData.spareCost },
          {
            label: getLabel('totalValueCaptured'),
            value: containerData.totalValueCaptured,
          },
          {
            label: getLabel('valueRealizationConclusion'),
            value: containerData.valueRealizationConclusion,
            full: true,
          },
        ]),
        true,
      ),
    )

    if (validDocuments.length > 0) {
      content.push(
        pdfSection(
          'Uploaded Files',
          validDocuments.map((file) => ({
            text: pdfText(file.name),
            link: storageDownloadUrl(file),
            color: '#1155cc',
            decoration: 'underline',
            fontSize: 8.5,
            margin: [3, 2, 3, 2],
          })),
          false,
        ),
      )
    }

    return {
      pageSize: 'A4',
      pageMargins: [18, 16, 18, 18],
      content,
      defaultStyle: {
        font: 'Roboto',
      },
    }
  }

  // Print function
  const printCaseDetails = async () => {
    const containerData = JSON.parse(
      aCase.attributes.find((attr) => attr.name === 'container').value,
    )

    // Collect all image files from both Analysis and Uploaded Files sections
    const analysisFiles = Array.isArray(containerData.file) ? containerData.file : []
    const allImageFiles = analysisFiles.filter(
      (file, index, allFiles) =>
        file?.name &&
        file.type?.startsWith('image/') &&
        allFiles.findIndex((candidate) => preparedImageKey(candidate) === preparedImageKey(file)) === index,
    )

    // Pre-fetch all images as base64
    const preparedImages = {}
    await Promise.all(
      allImageFiles.map(async (file) => {
        const key = preparedImageKey(file)
        try {
          preparedImages[key] = { dataUrl: await fetchImageAsBase64(file), failed: false }
        } catch (error) {
          preparedImages[key] = { dataUrl: null, failed: true }
          console.warn('Unable to prepare an Analysis image for the PDF:', error.message)
        }
      }),
    )

    // const printContent = generatePrintContent(aCase, formStructure, documents, base64Map);

    // const ssetName =
    //   formData?.data?.container?.textField1 || "Asset";
    // const safeAssetName = ssetName.replace(/[^a-zA-Z0-9]/g, '_');  const fileName = `${aCase.caseNo}_${safeAssetName}.pdf`;  const element = document.createElement("div");
    // element.innerHTML = printContent;

    // html2pdf()
    //   .set({
    //     filename: fileName,
    //     margin: 8,
    //     html2canvas: { scale: 2, useCORS: true },
    //     jsPDF: { unit: "mm", format: "a4", orientation: "portrait" },
    //     pagebreak: {
    //       mode: ['css', 'legacy'],
    //       avoid: ['.pdf-section', '.fault-card', 'img']
    //     }
    //   })
    //   .from(element)
    //   .save();

    const ssetName = formData?.data?.container?.textField1 || "Asset"
    const safeAssetName = ssetName.replace(/[^a-zA-Z0-9]/g, '_')
    const fileName = `${aCase.caseNo}_${safeAssetName}.pdf`

    const docDefinition = generatePdfMakeDefinition(
      aCase,
      formStructure,
      preparedImages,
      documents || aCase.documents || [],
    )

    pdfMake.createPdf(docDefinition).download(fileName)
  };

  // const printCaseDetails = () => {
  //   const printContent = generatePrintContent(aCase, formStructure);

  //   // Open a new window and print the generated content
  //   // const printWindow = window.open('', '_blank');
  //   // if (printWindow) {
  //   //   printWindow.document.write(`
  //   // <html>
  //   //   <head>
  //   //     <title>Print Case Details</title>
  //   //   </head>
  //   //   <body>
  //   //     ${printContent}
  //   //   </body>
  //   // </html>
  //   //   `);
  //   //   printWindow.document.close();
  //   //   setTimeout(() => {
  //   //     printWindow.print();
  //   //   }, 500); // 500ms delay (you can adjust this if needed)
  //   // } else {
  //   //   console.error('Failed to open the print window.');
  //   // }

  //   const iframe = document.createElement('iframe');
  //   iframe.style.display = 'none';
  //   document.body.appendChild(iframe);

  //   const doc = iframe.contentWindow.document;
  //   doc.open();
  //   doc.write(`

  //   <!DOCTYPE html>
  //   <html>
  //   <head>
  //     <title>Case Details - ${aCase.caseNo}</title>
  //     <style>
  //       @media print {
  //         body { 
  //           margin: 0; 
  //         }
          
  //         .no-break {
  //           break-inside: avoid;
  //           page-break-inside: avoid;
  //         }
  //       }
  //     </style>
  //   </head>
  //   <body>
  //     ${printContent}
  //   </body>
  //   </html>
  //   `);
  //   doc.close();

  //   iframe.onload = function () {
  //     iframe.contentWindow.focus();
  //     iframe.contentWindow.print();
  //     setTimeout(() => document.body.removeChild(iframe), 1000);
  //   };  
  // }

  const close = () => {
    if (hasUnsavedChanges) {
      const confirmLeave = window.confirm("You have unsaved changes. Do you really want to leave?");
      if (!confirmLeave) return; // Stop closing modal if user cancels
    }
    handleClose()
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
          onClose={close}
          TransitionComponent={Transition}
        >
          <AppBar sx={{ position: 'relative' }}>
            <Toolbar>
              <IconButton
                edge='start'
                color='inherit'
                onClick={close}
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
              <Button color='inherit' onClick={printCaseDetails}>
                {'Print'}
              </Button>

              <Button color='inherit' hidden={!isDraft} onClick={onSave}>
                {'Save'}
              </Button>
              {/* Case Actions Menu */}
              {/* <IconButton
                edge='end'
                color='inherit'
                onClick={handleMenuOpen}
                aria-label='manual-actions'
              >
                <MoreVertIcon />
              </IconButton> */}
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
                    label={t('pages.caseform.tabs.attachments') + ` (${documents ? documents.length : 0})`}
                    {...a11yProps(1)}
                  />
                  <Tab
                    label={t('pages.caseform.tabs.comments') + ` (${comments ? comments.length : 0})`}
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
                      {/* <Tooltip title={form.toolTip}>
                        <QuestionCircleOutlined />
                      </Tooltip> */}
                    </Box>
                    {isFormData && (
                      <Form
                        form={form.structure}
                        submission={formData}
                        onChange={(submission) => handleFormChange(submission)} // Listen for changes
                        options={{
                          // readOnly: true,
                          fileService: new StorageService(),
                        }}
                        // onSubmit={(submission) => {
                        //   console.log('Validation passed:', true)
                        //   console.log('Form data:', submission)

                        //   onSave(submission)
                        // }}
                        onCustomEvent={(event) => {
                          console.log('Form event:', event)
                          if (event.component.key === 'saveAsDraft') {
                            onSubmitForm()
                          } else if (
                            event.component.key === 'RecommendationSubmit3'
                          ) {
                            onSubmitRecommendation(event)
                          } else if (event.component.key === 'onSave') {
                            onSave()
                          } else if (event.component.key === 'analysisSubmit') {
                            onAnalysisSave()
                          } else if (event.component.key === 'valueRealizationSubmit') {
                            onValueRealizationSubmit()
                          } else if (event.component.key === 'analysisEdit') {
                            onAnalysisSave()
                          } else if (event.component.key === 'recommendationFinalSubmit') {
                            setIsFinalRecommendationConfirmationOpen(true)
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

                    <Dialog
                      open={isFinalRecommendationConfirmationOpen}
                      onClose={() => setIsFinalRecommendationConfirmationOpen(false)}
                    >
                      <DialogTitle>Confirm Submission</DialogTitle>
                      <DialogContent>
                        <DialogContentText>
                          You are about to make final submission after which no changes will be allowed.
                          Are you sure you want to proceed?
                        </DialogContentText>
                      </DialogContent>
                      <DialogActions>
                        <Button
                          onClick={() => setIsFinalRecommendationConfirmationOpen(false)}
                          color='primary'
                        >
                          Cancel
                        </Button>
                        <Button
                          onClick={onRecommendationFinalSubmit}
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
                  <Documents 
                    aCase={aCase} 
                    getCaseInfo={getCaseInfo} 
                    initialValue={documents || []} 
                    isAttachmentEnabled={isAttachmentEnabled}
                  />
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
                        isCommentEnabled={isCommentEnabled}
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
  if (cachedOptions.length > 0 && cacheKey !== 'caseStatusOptions') {
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

  // const caseDefinitionUsers = await fetchAndCacheOptions(
  //   () => CaseDefService.getCaseDefinitionUsers(keycloak),
  //   'caseAssignedOptions',
  //   (item) => ({ label: item.userId, value: item.emailId })
  // );
  const caseDefinitionCategories = await fetchAndCacheOptions(
    () => CaseDefService.getCaseDefinitionCategories(keycloak),
    'categoryOptions',
    (item) => ({ label: item.name, value: item.id })
  );
  const caseDefinitionGEAPMUsers = await fetchAndCacheOptions(
    () => CaseDefService.getCaseDefinitionGEAPMUsers(keycloak),
    'geAPMUsers',
    (item) => ({ label: item.userId, value: item.emailId })
  );
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
