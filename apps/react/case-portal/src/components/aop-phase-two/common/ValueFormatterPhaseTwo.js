import { useSelector } from 'react-redux'

export default function ValueFormatterPhaseTwo() {
  const dataGridStore = useSelector((state) => state.dataGridStore)

  const VERTICAL_NAME = dataGridStore?.verticalObject?.name?.toLowerCase()

  if (VERTICAL_NAME === 'cpp') {
    return '{0:0.0000}'
  }
  return '{0:0.00}'
}

export function customValueFormatterPhaseTwo(length = 2) {
  const decimals = Math.max(0, Math.min(length, 20))
  return '{0:0.' + '0'.repeat(decimals) + '}'
}
