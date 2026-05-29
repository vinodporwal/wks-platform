/* eslint-disable no-unused-vars */

import {
  GridToolbarContainer,
  GridToolbarFilterButton,
  GridToolbarExport,
  GridToolbarQuickFilter,
  GridFilterPanel,
} from '@mui/x-data-grid'


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

import CircularProgress from '@mui/material/CircularProgress'


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
// import { Grid, GridColumn } from '@progress/kendo-react-grid';
import '@progress/kendo-theme-material/dist/all.css'
import { getQueryParamValue } from 'utils/util'
// import { useLocation } from 'react-router-dom';

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
const PICaseForm = lazy(() =>
  import('../caseForm/piCaseFormPage').then((module) => ({
    default: module.PICaseFormPage,
  })),
)
const CaseNewFormPage = lazy(() =>
  import('../caseForm/NewCaseFormPage').then((module) => ({
    default: module.NewCaseFormPage,
  })),
)

// const NewCaseForm = lazy(() =>
//   import('../caseForm/newCaseForm').then((module) => ({
//     default: module.NewCaseForm,
//   })),
// )

function CustomToolbar({ searchText, onSearchChange, caseStatusFilter, onCaseStatusChange, onExport, exportLoading, onClear }) {
  const caseStatusOptions = JSON.parse(localStorage.getItem('caseStatusOptions')) || [
    { label: 'Assigned', value: 1 },
    { label: 'Under Analysis', value: 2 },
    { label: 'Closed', value: 3 },
    { label: 'Rejected', value: 10002 },
  ]

  const hasFilters = searchText || caseStatusFilter

  return (
    <GridToolbarContainer sx={{ p: 1, gap: 1, flexWrap: 'wrap' }}>
      {/* Search */}
      <input
        value={searchText}
        onChange={(e) => onSearchChange(e.target.value)}
        placeholder="Search..."
        style={{
          width: 300,
          border: '1px solid #e0e0e0',
          borderRadius: '4px',
          padding: '6px 10px',
          fontSize: '14px',
          outline: 'none',
        }}
      />

      {/* Case Status Filter */}
      <select
        value={caseStatusFilter}
        onChange={(e) => onCaseStatusChange(e.target.value)}
        style={{
          border: '1px solid #e0e0e0',
          borderRadius: '4px',
          padding: '6px 10px',
          fontSize: '14px',
          outline: 'none',
          backgroundColor: '#fff',
          cursor: 'pointer',
        }}
      >
        <option value="">All Statuses</option>
        {caseStatusOptions.map((opt) => (
          <option key={opt.value} value={opt.value}>{opt.label}</option>
        ))}
      </select>

      {/* Clear button — only shown when filters are active */}
      {hasFilters && (
        <Button
          variant="outlined"
          size="small"
          onClick={onClear}
          sx={{ borderColor: '#e0e0e0', color: '#666' }}
        >
          Clear
        </Button>
      )}

      {/* Server-side Export */}
      <Button
        variant="contained"
        size="small"
        onClick={onExport}
        disabled={exportLoading}
        sx={{ ml: 'auto' }}
      >
        {exportLoading && <CircularProgress size={14} color="inherit" sx={{ mr: 1 }} />}
        Export
      </Button>
    </GridToolbarContainer>
  )
}
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
  // const location = useLocation();
  const [exportLoading, setExportLoading] = useState(false)
  const [searchText, setSearchText] = useState('')
  const [caseStatusFilter, setCaseStatusFilter] = useState('')
  const searchDebounceRef = React.useRef(null)


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
  }, [caseDefId, status, openNewCaseForm])

  useEffect(() => {
    CaseService.getCaseDefinitions(keycloak).then((resp) => {
      setCaseDefs(resp)
    })
  }, [])

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
        flex: 1
      },
      {
        field: 'path',
        headerName: 'Path',
        flex: 1,
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
          return value ? value?.userId : '';
        },
      },
      // {
      //   field: "createdOn",
      //   headerName: "Created On",
      //   width: 100,
      // },
      // {
      //   field: 'creationDate',
      //   headerName: 'Created On',
      //   width: 150,
      //   valueGetter: (value, row) => {
      //     const date = row?.creationDate
      //     if (date) {
      //       const dateObj = new Date(date)
      //       const day = String(dateObj.getDate()).padStart(2, '0')
      //       const month = String(dateObj.getMonth() + 1).padStart(2, '0')
      //       const year = String(dateObj.getFullYear())
      //       const hours = String(dateObj.getHours()).padStart(2, '0')
      //       const minutes = String(dateObj.getMinutes()).padStart(2, '0')

      //       return `${day}-${month}-${year} ${hours}:${minutes}`
      //     }
      //     return ''
      //   },
      // },

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
          const date = row?.creationDate
          if (date) {
            const dateObj = new Date(date)
            const day = String(dateObj.getDate()).padStart(2, '0')
            const month = String(dateObj.getMonth() + 1).padStart(2, '0')
            const year = String(dateObj.getFullYear())
            const hours = String(dateObj.getHours()).padStart(2, '0')
            const minutes = String(dateObj.getMinutes()).padStart(2, '0')
            return `${day}-${month}-${year} ${hours}:${minutes}`
          }
          return ''
        },
      },

      // {
      //   field: 'ownerName',
      //   headerName: t('pages.caselist.datagrid.columns.caseOwnerName'),
      //   width: 150,
      //   valueGetter: (value, row) => row?.owner?.name,
      // },
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

  // const handleOpenCaseForm = (selectedCase) => {
  //   setACase(selectedCase);  // Set the selected case to be displayed in the form
  //   setOpenCaseForm(true);   // Open the case form modal
  // };

  // const handlePageChange = (event) => {
  //   const newPage = {
  //     limit: event.page.take,
  //     skip: event.page.skip,
  //   };
  //   fetchCases(setFetching, keycloak, caseDefId, setStages, status, newPage, setCases, setFilter);
  // };

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


  const handleSearchChange = (value) => {
    setSearchText(value)
    clearTimeout(searchDebounceRef.current)
    searchDebounceRef.current = setTimeout(() => {
      const searchParams = new URLSearchParams(window.location.search)
      const assetName = searchParams.get('assetName') || ''
      const hierarchyName = searchParams.get('hierarchyName') || ''
      fetchCasesFromSql(setFetching, keycloak, caseDefId, setCases, assetName, hierarchyName, value, caseStatusFilter)
    }, 400)
  }

  const handleCaseStatusChange = (value) => {
    setCaseStatusFilter(value)
    const searchParams = new URLSearchParams(window.location.search)
    const assetName = searchParams.get('assetName') || ''
    const hierarchyName = searchParams.get('hierarchyName') || ''
    fetchCasesFromSql(setFetching, keycloak, caseDefId, setCases, assetName, hierarchyName, searchText, value)
  }

  const handleClear = () => {
    setSearchText('')
    setCaseStatusFilter('')
    fetchCases(setFetching, keycloak, caseDefId, setStages, status, filter, setCases, setFilter)
  }

  const handleExportCsv = async () => {
  try {
    setExportLoading(true);
    const searchParams = new URLSearchParams(window.location.search);
    const assetName = searchParams.get('assetName') || '';
    const hierarchyName = searchParams.get('hierarchyName') || '';

    const blob = await CaseService.exportCasesCsv(
      keycloak,
      caseDefId,
      assetName,
      hierarchyName
    );

    const downloadUrl = window.URL.createObjectURL(blob);
    const link = document.createElement("a");

    link.href = downloadUrl;
    link.download = `cases_${new Date().toISOString().split('T')[0]}.xlsx`;

    document.body.appendChild(link);
    link.click();
    link.remove();

    window.URL.revokeObjectURL(downloadUrl);

  } catch (error) {
    console.error("Export error:", error);
  }
  finally {
    setExportLoading(false)   
  }
};





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

        console.log('Setting case 111')
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

        console.log('Setting case 222')
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

  return (
    <div style={{ height: '78vh', width: '100%' }}>
      {/* {caseDefId && (
        <div>
          <Button
            id='basic-button'
            onClick={handleNewCaseAction}
            variant='contained'
          >
            {t('pages.caselist.action.newcase')}
          </Button>
        </div>
      )} */}
      {/* 
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
      )} */}

      <MainCard sx={{ mt: 2 }} content={false}>
        <Box>

          {view === 'list' && (
            <div>
              <Suspense fallback={<div>Loading...</div>}>
                <DataGrid
                  sx={{
                    height: '78vh',
                    width: '100%',
                    backgroundColor: '#ffffff',
                    mt: 1,
                    '& .MuiDataGrid-cell': {
                      borderRight: '1px solid #e0e0e0',
                    },
                    '& .MuiDataGrid-columnHeader': {
                      borderRight: '1px solid #e0e0e0',
                    },
                  }}
                  rows={cases}
                  columns={makeColumns()}
                  getRowId={(row) => row.caseNo || row._id}
                  loading={fetching}
                  slots={{
                    toolbar: CustomToolbar,
                    pagination: CustomPagination,
                  }}
                  slotProps={{
                    toolbar: {
                      searchText,
                      onSearchChange: handleSearchChange,
                      caseStatusFilter,
                      onCaseStatusChange: handleCaseStatusChange,
                      onExport: handleExportCsv,
                      exportLoading,
                      onClear: handleClear,
                    },
                  }}
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

      {openCaseForm && caseDefId === 'create'  && (
        <CaseForm
          aCase={aCase}
          handleClose={handleCloseCaseForm}
          open={openCaseForm}
          keycloak={keycloak}
        />
      )}
      {openCaseForm && caseDefId === 'picreate' && (
        <PICaseForm
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
  setFilter,
  setACase,
  setOpenCaseForm,
) {
  setFetching(true)
  const searchParams = new URLSearchParams(window.location.search)
  const assetName = searchParams.get('assetName') || 'defaultAssetName'
  const hierarchyName =
    searchParams.get('hierarchyName') || 'defaultHierarchyName'

  CaseService.getCasesById(keycloak, caseDefId, assetName, hierarchyName)
    .then((resp) => {
      const caseList = Array.isArray(resp) ? resp : []

      const updatedCases = caseList.map((singleCase) => {
        let caseTitle = ''
        let caseNumber = singleCase.caseNo

        try {
          const containerValue = singleCase.attributes.find(
            (attr) => attr.name === 'container',
          )?.value

          if (containerValue) {
            const parsedValue = JSON.parse(containerValue)
            caseTitle = parsedValue?.textField5 || parsedValue?.caseTitle
            caseNumber = caseNumber || parsedValue.caseNo
          }
        } catch (error) {
          console.error('Error parsing container value:', error)
        }

        return {
          ...singleCase,
          caseTitle,
          caseNumber,
        }
      })

      setCases(updatedCases)
      setFilter({
        ...filter,
        cursors: {}, // Reset cursors here
        hasPrevious: false,
        hasNext: false,
      })

      const caseNo = getQueryParamValue(window.location.href, 'caseNo')

      if (caseNo) {
        const selectedCase = updatedCases.find((c) => c.caseNo == caseNo)
        if (selectedCase && setACase) {
          setACase(selectedCase)
          setOpenCaseForm(true)

          // Remove 'caseNo' from the URL without reloading
          searchParams.delete('caseNo')
          const newUrl = `${window.location.pathname}?${searchParams.toString()}`
          window.history.replaceState(null, '', newUrl)
        }
      }
    })
    .catch((error) => {
      console.error('Error fetching cases:', error)
    })
    .finally(() => {
      setFetching(false)
    })
}

function fetchCasesFromSql(setFetching, keycloak, caseDefId, setCases, assetName, hierarchyName, search, caseStatus) {
  setFetching(true)
  CaseService.filterCasesByCaseDefinitionId(keycloak, caseDefId, assetName, hierarchyName, search, caseStatus)
    .then((data) => {
      const updatedCases = data.map((singleCase) => {
        let caseTitle = ''
        let caseNumber = ''
        try {
          const containerValue = singleCase.attributes?.find((attr) => attr.name === 'container')?.value
          if (containerValue) {
            const parsed = JSON.parse(containerValue)
            caseTitle = parsed?.textField5 || parsed?.caseTitle || ''
            caseNumber = parsed?.textField || parsed?.caseNo || ''
          }
        } catch (e) {
          console.error('Error parsing container value:', e)
        }
        return { ...singleCase, caseTitle, caseNumber }
      })
      setCases(updatedCases)
    })
    .catch((e) => console.error('Filter error:', e))
    .finally(() => setFetching(false))
}
