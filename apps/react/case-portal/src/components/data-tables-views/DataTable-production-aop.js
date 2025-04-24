import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import MuiAccordion from '@mui/material/Accordion'
import MuiAccordionDetails from '@mui/material/AccordionDetails'
import MuiAccordionSummary from '@mui/material/AccordionSummary'
import { styled } from '@mui/material/styles'
import { useEffect, useState } from 'react'
import { useSelector } from 'react-redux'
import { DataService } from 'services/DataService'
import { useSession } from 'SessionStoreContext'

import DataGridTable from 'components/data-tables/ASDataGrid'

const ProductionAopView = () => {
  const keycloak = useSession()
  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState([])
  const [columns, setColumns] = useState([])
  const plantId = JSON.parse(localStorage.getItem('selectedPlant'))?.id
  const dataGridStore = useSelector((state) => state.dataGridStore)
  const { sitePlantChange, verticalChange, yearChanged, oldYear } =
    dataGridStore
  const vertName = verticalChange?.selectedVertical
  const lowerVertName = vertName?.toLowerCase() || 'meg'

  const fetchData = async () => {
    setLoading(true)
    try {
      var data = await DataService.getWorkflowDataProduction(keycloak, plantId)
      const formattedRows = data.results.map((row, id) => {
        const newRow = { id }
        Object.entries(row).forEach(([key, val]) => {
          if (!isNaN(val) && val !== '') {
            newRow[key] = Number(val).toFixed(2)
          } else {
            newRow[key] = val
          }
        })
        return newRow
      })
      setRows(formattedRows)
      const generateColumns = ({ headers, keys }) => {
        return headers.map((header, idx) => ({
          field: keys[idx],
          headerName: header,
          minWidth: idx === 0 ? 300 : 150,
          ...(idx === 0 && {
            renderHeader: (params) => <div>{params.colDef.headerName}</div>,
          }),
        }))
      }
      setColumns(generateColumns(data))
      setLoading(false)
    } catch (error) {
      console.error('Error fetching Business Demand data:', error)
      setRows([])
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [sitePlantChange, oldYear, yearChanged, keycloak, lowerVertName])

  const defaultCustomHeight = { mainBox: '22vh', otherBox: '114%' }

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        gap: '5px',
        marginTop: '20px',
      }}
    >
      <DataGridTable
        columns={columns}
        rows={rows}
        loading={loading}
        setRows={setRows}
        className='jio-data-grid'
        permissions={{
          customHeight: defaultCustomHeight,
        }}
      />
    </div>
  )
}

export default ProductionAopView
