import React, { useState, useEffect } from "react"
import { QuestionCircleOutlined } from "@ant-design/icons"
import CloseIcon from "@mui/icons-material/Close"
import { Box, Tooltip, CircularProgress } from "@mui/material"
import AppBar from "@mui/material/AppBar"
import Button from "@mui/material/Button"
import Dialog from "@mui/material/Dialog"
import Grid from "@mui/material/Grid"
import IconButton from "@mui/material/IconButton"
import Slide from "@mui/material/Slide"
import Toolbar from "@mui/material/Toolbar"
import Typography from "@mui/material/Typography"
import { Form } from "@formio/react"
import { useSession } from "SessionStoreContext"
import { CaseService, FormService } from "../../services"
import { StorageService } from "plugins/storage"
import { Snackbar, SnackbarContent } from "@mui/material"
import { useNavigate } from "react-router-dom"

const Transition = React.forwardRef(function Transition(props, ref) {
  return <Slide direction='up' ref={ref} {...props} />
})

export const NewCaseFormPage = ({ open = true, caseDefId = "create" }) => {
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
        console.log("new page form data", data)
        setForm(data)
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
    console.log("currentParams", params)
    navigate(`/case-list/create${params}`)
  }

  const onSave = () => {
    setLoading(true)
    const requiredFields = ["caseDescription", "dueDate", "faultCategory"]

    const missingFields = requiredFields.filter(
      (field) => !formData.data.container[field],
    )

    if (missingFields.length > 0) {
      setSnackbarMessages(["Please fill in all required fields."])
      setSnackbarOpen(true)
      setLoading(false)
      return
    }

    const currentParams = window.location.search
    setCurrentParams(currentParams)
    const urlParams = new URLSearchParams(window.location.search)

    const assetName = urlParams.get("assetName") || "default"
    const hierarchyName = urlParams.get("hierarchyName") || "default"
    const eventIdsParam = urlParams.get("eventIds")
    const sourceSystem = urlParams.get("sourceSystem") || "default"
    const eventIds = eventIdsParam ? eventIdsParam.split(",") : []
    const caseAttributes = Object.keys(formData.data).map((key) => ({
      name: key,
      value:
        typeof formData.data[key] !== "object"
          ? formData.data[key]
          : JSON.stringify(formData.data[key]),
      type: typeof formData.data[key] !== "object" ? "String" : "Json",
    }))

    // First API call to createCase to get the businessKey
    CaseService.createCase(
      keycloak,
      JSON.stringify({
        caseDefinitionId: caseDefId,
        isDraft: "n",
        owner: {
          id: keycloak.subject || "",
          name: keycloak.idTokenParsed.name || "",
          email: keycloak.idTokenParsed.email || "",
          phone: keycloak.idTokenParsed.phone || "",
        },
        attributes: caseAttributes,
      }),
    )
      .then((data) => {
        const businessKey = data.businessKey
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
              id: keycloak.subject || "",
              name: keycloak.idTokenParsed.name || "",
              email: keycloak.idTokenParsed.email || "",
              phone: keycloak.idTokenParsed.phone || "",
            },
            attributes: caseAttributes,
          }),
        )
      })
      .then((data) => {
        setLastCreatedCase(data)
        setSnackOpen(true)
        setTimeout(() => {
          handleClose()
        }, 2000)
      })
      .catch((err) => {
        console.error(err.message)
      })
      .finally(() => {
        setLoading(false) // Stop loading after the process finishes
      })
  }

  // const onSave = (event) => {
  //   const requiredFields = [
  //     'caseDescription',
  //     'dueDate',
  //     'faultCategory',
  //     'caseCauseCategory',
  //     'caseCauseDescription',
  //     'analysisDesc',
  //     'caseStatus',
  //     'caseAssignedTo',
  //   ]

  //   const missingFields = requiredFields.filter((field) => !event.data[field])

  //   if (missingFields.length > 0) {
  //     setSnackbarMessages(['Please fill in all required fields.'])
  //     setSnackbarOpen(true)
  //     return
  //   }

  //   // Proceed with the rest of your code
  //   const currentParams = window.location.search
  //   setCurrentParams(currentParams)
  //   const urlParams = new URLSearchParams(window.location.search)

  //   const assetName = urlParams.get('assetName') || 'default'
  //   const hierarchyName = urlParams.get('hierarchyName') || 'default'
  //   const eventIdsParam = urlParams.get('eventIds')
  //   const sourceSystem = urlParams.get('sourceSystem') || 'default'
  //   const eventIds = eventIdsParam ? eventIdsParam.split(',') : []

  //   const caseAttributes = Object.keys(event.data).map((key) => ({
  //     name: key,
  //     value:
  //       typeof event.data[key] !== 'object'
  //         ? event.data[key]
  //         : JSON.stringify(event.data[key]),
  //     type: typeof event.data[key] !== 'object' ? 'String' : 'Json',
  //   }))

  //   // First API call to createCase to get the businessKey
  //   CaseService.createCase(
  //     keycloak,
  //     JSON.stringify({
  //       caseDefinitionId: caseDefId,
  //       isDraft: false,
  //       owner: {
  //         id: keycloak.subject || '',
  //         name: keycloak.idTokenParsed.name || '',
  //         email: keycloak.idTokenParsed.email || '',
  //         phone: keycloak.idTokenParsed.phone || '',
  //       },
  //       attributes: caseAttributes,
  //     }),
  //   )
  //     .then((data) => {
  //       const businessKey = data.businessKey

  //       return CaseService.saveCase(
  //         keycloak,
  //         JSON.stringify({
  //           caseDefinitionId: caseDefId,
  //           assetName: assetName,
  //           hierarchyName: hierarchyName,
  //           sourceSystem: sourceSystem,
  //           eventIds: eventIds,
  //           businessKey: businessKey,
  //           owner: {
  //             id: keycloak.subject || '',
  //             name: keycloak.idTokenParsed.name || '',
  //             email: keycloak.idTokenParsed.email || '',
  //             phone: keycloak.idTokenParsed.phone || '',
  //           },
  //           attributes: caseAttributes,
  //         }),
  //       )
  //     })
  //     .then((data) => {
  //       setLastCreatedCase(data)
  //       setSnackOpen(true)
  //       setTimeout(() => {
  //         handleClose()
  //       }, 2000)
  //     })
  //     .catch((err) => {
  //       console.error(err.message)
  //     })
  // }

  const onSubmitRecommendation = () => {
    setSnackbarMessages(["Cannot submit recommendation without a case number."])
    setSnackbarOpen(true)

    setTimeout(() => {
      setSnackbarOpen(false)
    }, 2000)

    return
  }

  const onSubmitForm = () => {
    setLoading(true)
    const currentParams = window.location.search
    setCurrentParams(currentParams)
    const urlParams = new URLSearchParams(window.location.search)

    const assetName = urlParams.get("assetName") || "default"
    const hierarchyName = urlParams.get("hierarchyName") || "default"
    const eventIdsParam = urlParams.get("eventIds")
    const sourceSystem = urlParams.get("sourceSystem") || "default"
    const eventIds = eventIdsParam ? eventIdsParam.split(",") : []
    const caseAttributes = Object.keys(formData.data).map((key) => ({
      name: key,
      value:
        typeof formData.data[key] !== "object"
          ? formData.data[key]
          : JSON.stringify(formData.data[key]),
      type: typeof formData.data[key] !== "object" ? "String" : "Json",
    }))

    CaseService.createCase(
      keycloak,
      JSON.stringify({
        caseDefinitionId: caseDefId,
        isDraft: "y",
        owner: {
          id: keycloak.subject || "",
          name: keycloak.idTokenParsed.name || "",
          email: keycloak.idTokenParsed.email || "",
          phone: keycloak.idTokenParsed.phone || "",
        },
        attributes: caseAttributes,
      }),
    )
      .then((data) => {
        const businessKey = data.businessKey
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
              id: keycloak.subject || "",
              name: keycloak.idTokenParsed.name || "",
              email: keycloak.idTokenParsed.email || "",
              phone: keycloak.idTokenParsed.phone || "",
            },
            attributes: caseAttributes,
          }),
        )
      })
      .then((data) => {
        setLastCreatedCase(data)
        setSnackOpen(true)
        setTimeout(() => {
          handleClose()
        }, 2000)
      })
      .catch((err) => {
        console.error(err.message)
      })
      .finally(() => {
        setLoading(false) // Stop loading after the process finishes
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
        <AppBar sx={{ position: "relative" }}>
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
          sx={{ display: "flex", flexDirection: "column", p: 3 }}
        >
          <Grid item xs={12}>
            <Box sx={{ pb: 1, display: "flex", alignItems: "center" }}>
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
                console.log("Validation failed:", error)
                setValidationSnackbarOpen(true)
              }}
              onCustomEvent={(event) => {
                console.log("event event:", event)
                if (event.component.key === "saveAsDraft") {
                  onSubmitForm()
                } else if (event.component.key === "RecommendationSubmit3") {
                  onSubmitRecommendation()
                } else if (event.component.key === "onSave") {
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
          anchorOrigin={{ vertical: "bottom", horizontal: "left" }}
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
          anchorOrigin={{ vertical: "bottom", horizontal: "left" }}
        >
          <SnackbarContent
            message={
              <div>
                <Typography variant='body2' color='error' component='div'>
                  {"Please fill the required fields"}
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
              position: "fixed",
              top: 0,
              left: 0,
              width: "100vw",
              height: "100vh",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              backgroundColor: "rgba(255, 255, 255, 0.8)",
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
