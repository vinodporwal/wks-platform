import { Box, Chip } from '@mui/material'

const CHIP_COLORS = [
  { bg: '#E3F2FD', text: '#1565C0', delete: '#1976D2' },
  { bg: '#F3E5F5', text: '#7B1FA2', delete: '#9C27B0' },
  { bg: '#E8F5E9', text: '#2E7D32', delete: '#43A047' },
  { bg: '#FFF3E0', text: '#E65100', delete: '#FB8C00' },
  { bg: '#FCE4EC', text: '#C2185B', delete: '#E91E63' },
  { bg: '#E0F7FA', text: '#00838F', delete: '#00ACC1' },
  { bg: '#FFF8E1', text: '#F57F17', delete: '#FBC02D' },
  { bg: '#EDE7F6', text: '#4527A0', delete: '#5E35B1' },
  { bg: '#E1F5FE', text: '#0277BD', delete: '#039BE5' },
  { bg: '#F1F8E9', text: '#558B2F', delete: '#7CB342' },
]

const getFieldColorMap = (activeFilters) => {
  const uniqueFields = [...new Set(activeFilters.map((f) => f.field))]
  const map = {}
  uniqueFields.forEach((field, idx) => {
    map[field] = CHIP_COLORS[idx % CHIP_COLORS.length]
  })
  return map
}

/**
 * Reusable filter chips component that displays active Kendo grid filters
 * as removable chips, with a "Clear All" option.
 *
 * @param {Array} activeFilters - Flat array of { field, operator, value } objects
 * @param {Function} getColumnTitle - Maps field name to column display title
 * @param {Function} handleRemoveFilter - (field, value, operator) => void
 * @param {Function} handleClearAllFilters - () => void
 * @param {boolean} multicolor - When true, chips get distinct colors per column (default: true)
 */
const DEFAULT_COLOR = { bg: '#ECEEFF', text: '#1e293b', delete: '#64748b' }

const FilterChips = ({
  activeFilters = [],
  getColumnTitle = (f) => f,
  handleRemoveFilter = () => {},
  handleClearAllFilters = () => {},
  multicolor = true,
}) => {
  if (activeFilters.length === 0) return null

  return (
    <Box
      sx={{
        display: 'flex',
        flexWrap: 'wrap',
        gap: 0.5,
        mb: 1,
        px: 1,
        alignItems: 'center',
      }}
    >
      {(() => {
        const colorMap = multicolor ? getFieldColorMap(activeFilters) : null
        return activeFilters.map((f, idx) => {
          const color = multicolor ? colorMap[f.field] : DEFAULT_COLOR
          return (
            <Chip
              key={`${f.field}-${f.value}-${idx}`}
              label={
                <span>
                  <strong>{getColumnTitle(f.field)}</strong>: {f.value}
                </span>
              }
              onDelete={() => handleRemoveFilter(f.field, f.value, f.operator)}
              size='medium'
              sx={{
                backgroundColor: color.bg,
                color: color.text,
                fontWeight: 500,
                fontSize: '12px',
                '& .MuiChip-deleteIcon': {
                  color: color.delete,
                  '&:hover': { color: '#dc2626' },
                },
              }}
            />
          )
        })
      })()}
      <Chip
        label='Clear All'
        onDelete={handleClearAllFilters}
        size='medium'
        sx={{
          backgroundColor: 'transparent',
          color: '#dc2626',
          fontWeight: 600,
          fontSize: '12px',
          border: '1px solid #dc2626',
          '& .MuiChip-deleteIcon': {
            color: '#dc2626',
            '&:hover': { color: '#b91c1c' },
          },
        }}
      />
    </Box>
  )
}

export default FilterChips
