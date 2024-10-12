import { KeyboardArrowLeft, KeyboardArrowRight } from '@mui/icons-material'
import CloseIcon from '@mui/icons-material/Close'
import ViewKanbanIcon from '@mui/icons-material/ViewKanban'
import ViewListIcon from '@mui/icons-material/ViewList'
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
} from 'react'
import { useTranslation } from 'react-i18next'
import { CaseService } from '../../services'
import { Grid, GridColumn } from '@progress/kendo-react-grid';
import '@progress/kendo-theme-material/dist/all.css';
import { useLocation } from 'react-router-dom';

const DataGrid = lazy(() =>
  import('@mui/x-data-grid').then((module) => ({ default: module.DataGrid })),
)
const Kanban = lazy(() =>
  import('components/Kanban/kanban').then((module) => ({
    default: module.Kanban,
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
    limit: 10,
    after: '',
    before: '',
    cursors: {},
    hasPrevious: false,
    hasNext: false,
  })
  const location = useLocation();

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
        )
      }
      return () => {
        ws.close() // Close WebSocket connection when component unmounts
      }
    }
  }, [])
  

  useEffect(() => {
    fetchCases(
      setFetching,
      keycloak,
      caseDefId,
      setStages,
      status,
      filter,
      setCases,
      setFilter,
    )
  }, [caseDefId, status, openNewCaseForm])

  useEffect(() => {
    CaseService.getCaseDefinitions(keycloak).then((resp) => {
      setCaseDefs(resp)
    })
  }, [])

  // useEffect(() => {
  //   const searchParams = new URLSearchParams(location.search);
  //   const eventIds = searchParams.get('eventIds');

  //   if (eventIds) {
  //     setNewCaseDefId(caseDefId);
  //     setOpenNewCaseForm(true);
  //   }
  // }, [location, caseDefId]);

  const makeColumns = () => {
    return [
      {
        field: 'caseNumber',
        headerName: t('pages.caselist.datagrid.columns.caseNumber'),
        width: 150,
      },
      {
        field: 'caseTitle',
        headerName: t('pages.caselist.datagrid.columns.caseTitle'),
        width: 250,
      },
      // {
      //   field: 'businessKey',
      //   headerName: t('pages.caselist.datagrid.columns.businesskey'),
      //   width: 150,
      // },
      // {
      //   field: 'statusDescription',
      //   headerName: t('pages.caselist.datagrid.columns.statusdescription'),
      //   width: 150,
      // },
      // {
      //   field: 'stage',
      //   headerName: t('pages.caselist.datagrid.columns.stage'),
      //   width: 220,
      // },
      // {
      //   field: 'createdAt',
      //   headerName: t('pages.caselist.datagrid.columns.createdat'),
      //   width: 220,
      // },
      // {
      //   field: 'assetName',
      //   headerName: 'Asset Name',
      //   width: 150,
      // },
      {
        field: 'hierarchyName',
        headerName: 'Hierarchy Name',
        width: 150,
      },
      {
        field: 'ownerName',
        headerName: t('pages.caselist.datagrid.columns.caseOwnerName'),
        width: 150,
        valueGetter: (value, row) => row?.owner?.name,
      },
      // {
      //   field: 'queueId',
      //   headerName: t('pages.caselist.datagrid.columns.queue'),
      //   width: 200,
      // },
      {
        field: 'action',
        headerName: 'Action',
        sortable: false,
        renderCell: (data) => {
          const onClick = (e) => {
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

  return (
    <div style={{ height: 650, width: '100%' }}>
      {caseDefId && (
        <div>
          <Button
            id='basic-button'
            onClick={handleNewCaseAction}
            variant='contained'
          >
            {t('pages.caselist.action.newcase')}
          </Button>
        </div>
      )}

      {caseDefId && (
        <ToggleButtonGroup
          orientation='horizontal'
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
                    width: '100%',
                    backgroundColor: '#ffffff',
                    mt: 1,
                  }}
                  rows={cases}
                  columns={makeColumns()}
                  getRowId={(row) => row.caseNo}
                  loading={fetching}
                  components={{ Pagination: CustomPagination }}
                />
              </Suspense>
            </div>
          )}
          {/* {view === 'list' && (
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
{/* 
      {openNewCaseForm && (
        <NewCaseForm
          handleClose={handleCloseNewCaseForm}
          cases={cases}
          open={openNewCaseForm}
          caseDefId={newCaseDefId}
          setLastCreatedCase={setLastCreatedCase}
        />
      )} */}
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

// function fetchCases(
//   setFetching,
//   keycloak,
//   caseDefId,
//   setStages,
//   status,
//   filter,
//   setCases,
//   setFilter,
// ) {
//   setFetching(true)

//   CaseService.getCaseDefinitionsById(keycloak, caseDefId)
//     .then((resp) => {
//       resp.stages.sort((a, b) => a.index - b.index).map((o) => o.name)
//       setStages(resp.stages)
//       return CaseService.filterCase(keycloak, caseDefId, status, filter)
//     })
//     .then((resp) => {
//       const { data, paging } = resp
//       console.log('resp', resp)
//        const updatedCases = data.map((singleCase) => {
//         let caseTitle = "";
//         let caseNumber = "";

//         try {
//           const containerValue = singleCase.attributes.find(
//             (attr) => attr.name === "container"
//           )?.value;

//           if (containerValue) {
//             const parsedValue = JSON.parse(containerValue);
//             caseTitle = parsedValue?.textField5 || parsedValue?.caseTitle;
//             caseNumber = parsedValue?.textField || parsedValue.caseNo;
//           }
//         } catch (error) {
//           console.error("Error parsing container value:", error);
//         }

//         return {
//           ...singleCase,
//           caseTitle,
//           caseNumber,
//         };
//       });

//       setCases(updatedCases)
//       setFilter({
//         ...filter,
//         cursors: paging.cursors,
//         hasPrevious: paging.hasPrevious,
//         hasNext: paging.hasNext,
//       })
//     })
//     .finally(() => {
//       setFetching(false)
//     })
// }


function fetchCases(
  setFetching,
  keycloak,
  caseDefId,
  setStages,
  status,
  filter,
  setCases,
  setFilter
) {
  setFetching(true);
  const searchParams = new URLSearchParams(window.location.search);
  const assetName = searchParams.get('assetName') || 'defaultAssetName';
  const hierarchyName = searchParams.get('hierarchyName') || 'defaultHierarchyName';

  CaseService.getCasesById(keycloak, caseDefId, assetName, hierarchyName)
    .then((resp) => {

      const caseList = Array.isArray(resp) ? resp : [];

      const updatedCases = caseList.map((singleCase) => {
        let caseTitle = "";
        let caseNumber = singleCase.caseNo;

        try {
          const containerValue = singleCase.attributes.find(
            (attr) => attr.name === "container"
          )?.value;

          if (containerValue) {
            const parsedValue = JSON.parse(containerValue);
            caseTitle = parsedValue?.textField5 || parsedValue?.caseTitle;
            caseNumber = caseNumber || parsedValue.caseNo;
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

      setCases(updatedCases); 
      setFilter({
        ...filter,
        cursors: {}, // Reset cursors here
        hasPrevious: false,
        hasNext: false,
      });
    })
    .catch((error) => {
      console.error("Error fetching cases:", error);
    })
    .finally(() => {
      setFetching(false);
    });
}

