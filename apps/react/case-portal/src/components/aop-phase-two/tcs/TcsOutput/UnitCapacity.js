import { Box } from '@mui/material'
import UnitCapacityGrid from './UnitCapacityComponents/UnitCapacityGrid'
import UnitCapacityGridRowwise from './UnitCapacityComponents/UnitCapacityGridRowwise'
import UnitCapacitySimple from './UnitCapacityComponents/UnitCapacitySimple'

const UnitCapacity = ({
  VERTICAL_ID,
  SITE_ID,
  PLANT_ID,
  AOP_YEAR,
  currentTab,
  snackbarData,
  setSnackbarData,
  snackbarOpen,
  setSnackbarOpen,
  userRole,
}) => {
  const capacityTypes = [
    { key: 'design', title: 'Design Capacity' },
    { key: 'maxAchieved', title: 'Max Achieved Capacity (From MCU Portal)' },
    // { key: 'currentOperating', title: 'Current Operating Capacity' },
  ]

  return (
    <Box>
      {capacityTypes.map((type) => (
        <UnitCapacitySimple
          key={type.key}
          capacityType={type.key}
          title={type.title}
          SITE_ID={SITE_ID}
          VERTICAL_ID={VERTICAL_ID}
          PLANT_ID={PLANT_ID}
          AOP_YEAR={AOP_YEAR}
          snackbarData={snackbarData}
          setSnackbarData={setSnackbarData}
          snackbarOpen={snackbarOpen}
          setSnackbarOpen={setSnackbarOpen}
          userRole={userRole}
        />
      ))}
    </Box>
  )
}

export default UnitCapacity
