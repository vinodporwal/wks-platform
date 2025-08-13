/* eslint-disable no-unused-vars */
import { useLocation, useSearchParams } from 'react-router-dom';
import { KeyboardArrowLeft, KeyboardArrowRight } from '@mui/icons-material'
import CloseIcon from '@mui/icons-material/Close'
import ViewKanbanIcon from '@mui/icons-material/ViewKanban'
import ViewListIcon from '@mui/icons-material/ViewList'
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth'
import { useTheme } from '@mui/material'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import IconButton from '@mui/material/IconButton'
import Snackbar from '@mui/material/Snackbar'
import TablePagination from '@mui/material/TablePagination'
import ToggleButton from '@mui/material/ToggleButton'
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup'
import { useSession } from 'SessionStoreContext'
import MainCard from 'components/MainCard'
import Config from 'consts/index'
import React, {
  Suspense,
  createContext,
  lazy,
  useContext,
  useEffect,
  useState,
  useRef
} from 'react'
import { useTranslation } from 'react-i18next'
import { CaseService } from '../../services'
// import { Grid, GridColumn } from '@progress/kendo-react-grid';
//import '@progress/kendo-theme-material/dist/all.css'
import { getQueryParamValue } from 'utils/util'
// import { useLocation } from 'react-router-dom';
import { accountStore } from '../../store'
import moment from 'moment'

const DataGrid = lazy(() =>
  import('@mui/x-data-grid').then((module) => ({ default: module.DataGrid })),
)
const Kanban = lazy(() =>
  import('components/Kanban/kanban').then((module) => ({
    default: module.Kanban,
  })),
)
const ScheduleView = lazy(() =>
  import('components/ScheduleView/scheduleView').then((module) => ({
    default: module.ScheduleView,
  })),
)

const CaseForm = lazy(() =>
  import('../caseForm/caseForm').then((module) => ({
    default: module.CaseForm,
  })),
)
const NewCaseForm = lazy(() =>
   import('../caseForm/newCaseForm').then((module) => ({
     default: module.NewCaseForm,
   })),
)
const CaseNewFormPage = lazy(() =>
  import('../caseForm/NewCaseFormPage').then((module) => ({
    default: module.NewCaseFormPage,
  })),
)

export const CaseList = ({ status, caseDefId }) => {
  const PaginationContext = createContext()
  const { t } = useTranslation()
  const [stages, setStages] = useState([])
  const [cases, setCases] = useState([])
  const [aCase, setACase] = useState(null)
  const [newCaseDefId, setNewCaseDefId] = useState(null)
  const [lastCreatedCase, setLastCreatedCase] = useState(null)
  const [openCaseForm, setOpenCaseForm] = useState(false)
  const [openNewCaseForm, setOpenNewCaseForm] = useState(false)
  const [view, setView] = React.useState('list')
  const [snackOpen, setSnackOpen] = useState(false)
  const keycloak = useSession()
  const [caseDefs, setCaseDefs] = useState([])
  const [fetching, setFetching] = useState(false)
  const [filter, setFilter] = useState({
    sort: '',
    limit: 100,
    after: '',
    before: '',
    cursors: {},
    hasPrevious: false,
    hasNext: false,
  })

  // XOM - Route changes -Start
  const [error, setError] = useState(false);
  const [loading, setLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState("");
  const [searchParams] = useSearchParams();
  const location = useLocation();
  const currentPath = location.pathname;
  const isCaseCreatePath = currentPath && currentPath === '/case/create';
  const isCaseViewPath = currentPath && currentPath === '/case/view';
  let createButtonRef = useRef(null);
  let caseBusinessKey = null;
  const [accepted, setAccepted] = useState(false);
  const isCaseDefValid = (caseDefId && caseDefId.length !== 0 && caseDefId.trim().length !== 0);
  
  if(isCaseCreatePath && !isCaseDefValid)
  {
    const isCaseDefPresentInRoute = searchParams.has('case_def');
    caseDefId = isCaseDefPresentInRoute ? searchParams.get('case_def') : 'create';
    if(!caseDefId || caseDefId.length === 0 || caseDefId.trim().length === 0) {
      setLoading(false);
      setError(true);
      setErrorMsg('Invalid Case Definition "' + caseDefId + '" !! ');
    }
  }
  
  if(isCaseViewPath)
  {
    const isCaseDefPresentInRoute = searchParams.has('case_def');
    const isBusinessKeyPresentInRoute = searchParams.has('businessKey') || searchParams.has('caseNo');
    caseDefId = isCaseDefPresentInRoute ? searchParams.get('case_def') : 'create';

    if(!isBusinessKeyPresentInRoute) {
      setLoading(false);
      setError(true);
      setErrorMsg('Business Key missing in parameters !! ');
    } else {
      caseBusinessKey = searchParams.get('businessKey') || searchParams.get('caseNo');
      if(!caseBusinessKey || caseBusinessKey.length === 0 || caseBusinessKey.trim().length === 0) {
        setLoading(false);
        setError(true);
        setErrorMsg('Invalid Business Key !! ');
      } 
    }
  }

  useEffect(() => {
    if(isCaseCreatePath && !error && !accepted ) {
      const fireEvent = (el, eventName) => {
        const event = new Event(eventName, { bubbles: true });
        el.dispatchEvent(event);
      };
    
	  var autoClickTimeout = null;
      autoClickTimeout = setTimeout(() => {
      if (createButtonRef && createButtonRef.current) {
          setOpenNewCaseForm(true);
          setAccepted(true);
		  createButtonRef.current.click();
          //fireEvent(createButtonRef, "click");
        }
      }, 500);

      return () => {
        if(autoClickTimeout) { clearInterval(autoClickTimeout); } // Clear the interval when component unmounts
      };
    }
  }, [accepted]);  

  // XOM - Route changes - End

  useEffect(() => {
    if (Config.WebsocketsEnabled) {
      const websocketUrl = Config.WebsocketUrl
      const topic = Config.WebsocketsTopicCaseCreated
      const ws = new WebSocket(`${websocketUrl}/${topic}`)
      ws.onmessage = () => {
        fetchCases(
          setFetching,
          keycloak,
          caseDefId,
          setStages,
          status,
          filter,
          setCases,
          setFilter,
          setACase,
          setOpenCaseForm,
        )
      }
      return () => {
        ws.close() // Close WebSocket connection when component unmounts
      }
    }
  }, [])

  useEffect(() => {
	const isNavToView = (isCaseViewPath && !error && caseBusinessKey);

	fetchCases(
      setFetching,
      keycloak,
      caseDefId,
      setStages,
      status,
      filter,
      setCases,
      setFilter,
      isNavToView ? [isNavToView, caseBusinessKey, setACase, setOpenCaseForm, setAccepted, setError, handleCloseSnack] : null
    );
  }, [caseDefId, status, openNewCaseForm])

  useEffect(() => {
    CaseService.getCaseDefinitions(keycloak).then((resp) => {
      setCaseDefs(resp);
      // XOM - Route changes - Start
      if(!resp.some(i => i.id === caseDefId)) {
        setLoading(false);
        setError(true);
        setErrorMsg('Invalid Case Definition "' + caseDefId + '" !! ');
      }
      // XOM - Route changes - End

    })
  }, [])

  const makeColumns = () => {
    return [
      {
        field: 'caseNumber',
        headerName: t('pages.caselist.datagrid.columns.caseNumber'),
        width: 150,
		valueGetter: (value, row) => {
          try {
			if(row.businessKey && row.businessKey !== '')
				return row.businessKey;
			
            const attributes =
              typeof row.attributes === 'string'
                ? JSON.parse(row.attributes)
                : row.attributes

            const containerValue = attributes?.find(
              (attr) => attr.name === 'container',
            )?.value

            const parsedContainer = containerValue
              ? JSON.parse(containerValue)
              : {}

            return parsedContainer.caseNo || parsedContainer.businessKey || ''
          } catch (error) {
            console.error('Error parsing mainAsset:', error)
            return ''
          }			
		}
      },
      {
        field: 'caseTitle',
        headerName: t('pages.caselist.datagrid.columns.caseTitle'),
        flex: 1
      },
      {
        field: 'mainAsset',
        headerName: 'Main Asset',
        flex: 1,
        valueGetter: (value, row) => {
          try {
            const attributes =
              typeof row.attributes === 'string'
                ? JSON.parse(row.attributes)
                : row.attributes

            const containerValue = attributes?.find(
              (attr) => attr.name === 'container',
            )?.value

            const parsedContainer = containerValue
              ? JSON.parse(containerValue)
              : {}

            return parsedContainer.textField1 || ''
          } catch (error) {
            console.error('Error parsing mainAsset:', error)
            return ''
          }
        },
      },
      {
        field: 'caseStatus',
        headerName: 'Case Status',
        flex: 1,
        valueGetter: (value, row) => {
          try {
            if (!row) {
              return ''
            }
            const caseStatusOptions = JSON.parse(
              localStorage.getItem('caseStatusOptions'),
            ) || [
              {
                label: 'Assigned',
                value: 1,
              },
              {
                label: 'Under Analysis',
                value: 2,
              },
              {
                label: 'Closed',
                value: 3,
              },
              {
                label: 'Rejected',
                value: 10002,
              },
            ]

            // Parse the container value to get the caseStatus value
            const attributes =
              typeof row.attributes === 'string'
                ? JSON.parse(row.attributes)
                : row.attributes

            if (!attributes) {
              return ''
            }

            const containerValue = attributes.find(
              (attr) => attr.name === 'container',
            )?.value
            const parsedContainer = containerValue
              ? JSON.parse(containerValue)
              : {}

            const caseStatusValue = parsedContainer.caseStatus || ''

            // Find the label corresponding to the value
            const matchingOption = caseStatusOptions.find(
              (option) => option.value === caseStatusValue,
            )
            return matchingOption ? matchingOption.label : caseStatusValue
          } catch (error) {
            console.error('Error parsing caseStatus:', error)
            return ''
          }
        },
      },
      {
        field: 'isDraft',
        headerName: 'Status',
        width: 150,
        valueGetter: (value, row) => (value === 'y' ? 'Draft' : 'Submitted'),
      },
      {
        field: 'assignedTo',
        headerName: 'Case Assigned To',
        flex: 1,
        valueGetter: (value, row) => {
          //return value ? value?.userId : '';
		  return row ? row?.caseAssignedTo : '';
        },
      },
      // {
      //   field: 'ownerName',
      //   headerName: t('pages.caselist.datagrid.columns.caseOwnerName'),
      //   width: 150,
      //   valueGetter: (value, row) => row?.owner?.name,
      // },
      {
        field: 'creationDate',
        headerName: 'Created On',
        width: 150,
        valueGetter: (value, row) => {
          try {
            const attributes =
              typeof row.attributes === 'string'
                ? JSON.parse(row.attributes)
                : row.attributes

            const containerValue = attributes?.find(
              (attr) => attr.name === 'container',
            )?.value

            const parsedContainer = containerValue
              ? JSON.parse(containerValue)
              : {}

            return (parsedContainer.createdOn && moment(parsedContainer.createdOn).format("DD-MM-YYYY")) || ''
          } catch (error) {
            console.error('Error parsing createdOn:', error)
            return ''
          }
        }
      },
      {
        field: 'dueDate',
        headerName: 'Due On',
        width: 150,
        valueGetter: (value, row) => {
          try {
            const attributes =
              typeof row.attributes === 'string'
                ? JSON.parse(row.attributes)
                : row.attributes

            const containerValue = attributes?.find(
              (attr) => attr.name === 'container',
            )?.value

            const parsedContainer = containerValue
              ? JSON.parse(containerValue)
              : {}

            return (parsedContainer.dueDate && moment(parsedContainer.dueDate).format("DD-MM-YYYY")) || ''
          } catch (error) {
            console.error('Error parsing dueDate:', error)
            return ''
          }
        }
      },
      {
        field: 'action',
        headerName: 'Action',
        sortable: false,
        renderCell: (data) => {
          const onClick = (e) => {
            console.log('data.row', data.row)
            setACase(data.row)
            e.stopPropagation()
            setOpenCaseForm(true)
          }

          return (
            <Button onClick={onClick}>
              {t('pages.caselist.datagrid.action.details')}
            </Button>
          )
        },
      },
    ]
  }

  const handleOpenCaseForm = (selectedCase) => {
    setACase(selectedCase);  // Set the selected case to be displayed in the form
    setOpenCaseForm(true);   // Open the case form modal
  };

  const handlePageChange = (event) => {
    const newPage = {
      limit: event.page.take,
      skip: event.page.skip,
    };
    fetchCases(setFetching, keycloak, caseDefId, setStages, status, newPage, setCases, setFilter);
  };

  const handleCloseCaseForm = () => {
    setOpenCaseForm(false)
    fetchCases(
      setFetching,
      keycloak,
      caseDefId,
      setStages,
      status,
      filter,
      setCases,
      setFilter,
      setACase,
      setOpenCaseForm,
    )
  }

  const handleCloseNewCaseForm = () => {
    setOpenNewCaseForm(false)
    setSnackOpen(true)
  }

  const handleNewCaseAction = () => {
    setLastCreatedCase(null)
    setNewCaseDefId(caseDefId)
    setOpenNewCaseForm(true)
  }

  const handleChangeView = (event, nextView) => {
    if (nextView !== null) {
      setView(nextView)
    }
  }

  const fetchKanbanConfig = () => {
    return caseDefs.find((o) => o.id === caseDefId).kanbanConfig
  }

  const handleCloseSnack = (event, reason) => {
    if (reason === 'clickaway') {
      return
    }

    setSnackOpen(false)
  }

  const snackAction = lastCreatedCase && (
    <React.Fragment>
      <Button
        color='primary'
        size='small'
        onClick={() => {
          setACase({
            businessKey: lastCreatedCase.businessKey,
            caseDefinitionId: caseDefId,
          })
          setOpenCaseForm(true)
          handleCloseSnack()
        }}
      >
        {lastCreatedCase.businessKey}
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

  const handlerNextPage = () => {
    setFetching(true)

  const next = {
      sort: filter.sort,
      limit: filter.limit,
      after: filter.cursors.after,
  }

  CaseService.filterCase(keycloak, caseDefId, status, next)
      .then((resp) => {
        const { data, paging } = resp
        setCases(data)
        setFilter({
          ...filter,
          cursors: paging.cursors,
          hasPrevious: paging.hasPrevious,
          hasNext: paging.hasNext,
        })
      })
      .finally(() => {
        setFetching(false)
      })
  }

  const handlerPriorPage = () => {
    setFetching(true)

    const prior = {
      sort: filter.sort,
      limit: filter.limit,
      before: filter.cursors.before,
    }

    CaseService.filterCase(keycloak, caseDefId, status, prior)
      .then((resp) => {
        const { data, paging } = resp
        setCases(data)
        setFilter({
          ...filter,
          cursors: paging.cursors,
          hasPrevious: paging.hasPrevious,
          hasNext: paging.hasNext,
        })
      })
      .finally(() => {
        setFetching(false)
      })
  }

  function TablePaginationActions(props) {
    const theme = useTheme()
    const filter = useContext(PaginationContext)
    const { onPageChange } = props

    const handleBackButtonClick = (event) => {
      onPageChange(event, 'back')
    }

    const handleNextButtonClick = (event) => {
      onPageChange(event, 'next')
    }

    const { hasPrevious, hasNext } = filter

    return (
      <Box sx={{ flexShrink: 0, ml: 2.5 }}>
        <IconButton
          onClick={handleBackButtonClick}
          disabled={!hasPrevious}
          aria-label='previous page'
        >
          {theme.direction === 'rtl' ? (
            <KeyboardArrowRight />
          ) : (
            <KeyboardArrowLeft />
          )}
        </IconButton>
        <IconButton
          onClick={handleNextButtonClick}
          disabled={!hasNext}
          aria-label='next page'
        >
          {theme.direction === 'rtl' ? (
            <KeyboardArrowLeft />
          ) : (
            <KeyboardArrowRight />
          )}
        </IconButton>
      </Box>
    )
  }

  const CustomPagination = () => {
    return (
      <PaginationContext.Provider value={filter}>
        <TablePagination
          component='div'
          count={-1}
          page={0}
          labelRowsPerPage={
            <div style={{ paddingTop: 15 }}>Rows per page:</div>
          }
          rowsPerPage={filter.limit}
          rowsPerPageOptions={[5, 10, 25, 50]}
          getItemAriaLabel={() => ''}
          labelDisplayedRows={() => ''}
          onPageChange={(e, type) => {
            const action = {
              next: handlerNextPage,
              back: handlerPriorPage,
            }
            action[type]()
          }}
          onRowsPerPageChange={(e) => {
            setFetching(true)

            CaseService.filterCase(keycloak, caseDefId, status, {
              limit: e.target.value,
            })
              .then((resp) => {
                const { data, paging } = resp

                console.log('Setting case 333')
                setCases(data)
                setFilter({
                  ...filter,
                  limit: e.target.value,
                  cursors: paging.cursors,
                  hasPrevious: paging.hasPrevious,
                  hasNext: paging.hasNext,
                })
              })
              .finally(() => {
                setFetching(false)
              })
          }}
          SelectProps={{
            inputProps: {
              'aria-label': 'rows per page',
            },
            native: true,
          }}
          ActionsComponent={TablePaginationActions}
        />
      </PaginationContext.Provider>
    )
  }

  const hasWaitMsg = loading || error;
  const waitOrErrMsg = (loading && 'Loading...') || (error && (errorMsg || 'Something wrong happened'))

  function generateRandom() {
    var length = 8,
        charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789",
        retVal = "";
    for (var i = 0, n = charset.length; i < length; ++i) {
        retVal += charset.charAt(Math.floor(Math.random() * n));
    }
    return retVal;
  }

  // {{loading && (<p>'Loading...'</p>)} || {error && (<p>{errorMsg || 'Something wrong happened'}</p>)}}
  // {hasWaitMsg && (<p>{waitOrErrMsg}</p>)}
  return (
    <div style={{ height: 650, width: '100%' }}>
      {caseDefId && accountStore.isManagerUser(keycloak) && (
          <Button
            id='basic-button'
            onClick={handleNewCaseAction}
			ref={createButtonRef}
            variant='contained'
          >
            {t('pages.caselist.action.newcase')}
          </Button>
      )}
      {caseDefId && (
        <ToggleButtonGroup
          orientation='horizontal'
		  style={{ float: 'right' }}
          value={view}
          exclusive
          onChange={handleChangeView}
        >
          <ToggleButton value='list' aria-label='list'>
            <ViewListIcon />
          </ToggleButton>
          <ToggleButton value='kanban' aria-label='kanban'>
            <ViewKanbanIcon />
          </ToggleButton>
          <ToggleButton value='calendar' aria-label='calendar'>
            <CalendarMonthIcon />
          </ToggleButton>
        </ToggleButtonGroup>
      )}
      <MainCard sx={{ mt: 2 }} content={false}>
        <Box>
          {view === 'list' && (
            <div>
              <Suspense fallback={<div>Loading...</div>}>
                <DataGrid
                  sx={{
                    height: 500,
                    width: '99%',
                    backgroundColor: '#ffffff',
                    mt: 1,
                    '& .MuiDataGrid-cell': {
                      borderRight: '1px solid #e0e0e0', // Light gray border for column separation in cells
                    },
                    '& .MuiDataGrid-columnHeader': {
                      borderRight: '1px solid #e0e0e0', // Light gray border for column separation in header
                    },
                  }}
                  rows={cases}
                  columns={makeColumns()}
                  getRowId={(row) => {
                    return generateRandom();
                    //console.log((isCaseCreatePath || isCaseViewPath)? generateRandom() : row.businessKey?.caseNo?.id?._id)
                    //return (isCaseCreatePath || isCaseViewPath)? generateRandom() : row.businessKey?.caseNo?.id?._id
                  }}
                  loading={fetching}
                  components={{ Pagination: CustomPagination }}
                />
              </Suspense>
            </div>
          )}
          {/*{view === 'list' && (
            <div>
              <Grid
                data={cases}
                style={{ height: '500px', width: '100%' }}
                sortable={true}
                pageable={true}
                total={cases.length}
                skip={filter.skip}
                pageSize={filter.limit}
                onPageChange={handlePageChange}
              >
                {makeColumns().map((col, idx) => (
                  <GridColumn key={idx} field={col.field} title={col.title} width={col.width} />
                ))}
                <GridColumn
                  field="action"
                  title=""
                  sortable={false}
                  cell={(props) => {
                    return (
                      <td>
                        <Button onClick={() => handleOpenCaseForm(props.dataItem)}>
                          {t('pages.caselist.datagrid.action.details')}
                        </Button>
                      </td>
                    );
                  }}
                />
              </Grid>
            </div>
          )} */}
          {view === 'kanban' && (
            <Suspense fallback={<div>Loading...</div>}>
              <Kanban
                stages={stages}
                cases={cases}
                caseDefId={caseDefId}
                kanbanConfig={fetchKanbanConfig()}
                setACase={setACase}
                setOpenCaseForm={setOpenCaseForm}
              />
            </Suspense>
          )}
          {view === 'calendar' && (
            <Suspense fallback={<div>Loading...</div>}>
              <ScheduleView
                cases={cases}
                caseDefId={caseDefId}
                setACase={setACase}
                setOpenCaseForm={setOpenCaseForm}
              />
            </Suspense>
          )}  		  
        </Box>
      </MainCard>

      <br />

      {openCaseForm && (
        <CaseForm
          aCase={aCase}
          handleClose={handleCloseCaseForm}
          open={openCaseForm}
          keycloak={keycloak}
        />
      )}
      {/*openNewCaseForm && (
        <NewCaseForm
          handleClose={handleCloseNewCaseForm}
          cases={cases}
          open={openNewCaseForm}
          caseDefId={newCaseDefId}
          setLastCreatedCase={setLastCreatedCase}
        />
      )*/}
      {openNewCaseForm && (
        <CaseNewFormPage
          handleClose={handleCloseNewCaseForm}
          cases={cases}
          open={openNewCaseForm}
          caseDefId={newCaseDefId}
          setLastCreatedCase={setLastCreatedCase}
        />
      )}
      {lastCreatedCase && (
        <Snackbar
          open={snackOpen}
          autoHideDuration={6000}
          message='Case created'
          onClose={handleCloseSnack}
          action={snackAction}
        />
      )}
    </div>
  )
}

function fetchCases(
  setFetching,
  keycloak,
  caseDefId,
  setStages,
  status,
  filter,
  setCases,
  setFilter,
  navToView = null
) {
  setFetching(true)

  CaseService.getCaseDefinitionsById(keycloak, caseDefId)
    .then((resp) => {
      resp.stages.sort((a, b) => a.index - b.index).map((o) => o.name)
      setStages(resp.stages)
      return CaseService.filterCase(keycloak, caseDefId, status, filter)
    })
    .then((resp) => {
      const { data, paging } = resp
      console.log('resp', resp)
      const updatedCases = data.map((singleCase) => {
        let caseTitle = "";
        let caseNumber = "";

        try {
          const containerValue = singleCase.attributes.find(
            (attr) => attr.name === "container"
          )?.value;

          if (containerValue) {
            const parsedValue = JSON.parse(containerValue);
            caseTitle = parsedValue?.textField5 || parsedValue?.caseTitle;
            caseNumber = parsedValue?.textField || parsedValue.caseNo;
          }
        } catch (error) {
          console.error("Error parsing container value:", error);
        }

        return {
          ...singleCase,
          caseTitle,
          caseNumber,
        };
      });
	  
	  const uniqueUpdatedCases = updatedCases.filter((obj, index, self) =>
        index === self.findIndex((t) => t.businessKey === obj.businessKey)
      );
      setCases(uniqueUpdatedCases)
      setFilter({
        ...filter,
        cursors: paging.cursors,
        hasPrevious: paging.hasPrevious,
        hasNext: paging.hasNext,
      })
	  
	  if(navToView && navToView[0]) {
        const selectedCase = uniqueUpdatedCases.find((c) => c.businessKey == navToView[1]);
        if (selectedCase && navToView[2]) {
          navToView[2]({...selectedCase});
          navToView[3](true);
          navToView[4](true);
          navToView[5](false);
          navToView[6]();
        }
	  }
    })
    .finally(() => {
      setFetching(false)
    })
  }