import { DatePicker } from '@progress/kendo-react-dateinputs'
import { useEffect, useRef } from 'react'

const DateOnlyPicker = ({ dataItem, field, onChange }) => {
  const currentRaw = dataItem[field]
  const currentDate = currentRaw ? new Date(currentRaw) : null
  const inputRef = useRef(null)

  const handleChange = (event) => {
    onChange({
      dataItem,
      field,
      value: event.value,
      syntheticEvent: event.syntheticEvent,
    })
  }

  const handleBlur = () => {
    const currentValue = inputRef.current?.value
    if (currentValue !== currentRaw) {
      onChange({
        dataItem,
        field,
        value: currentValue,
        syntheticEvent: null,
      })
    }
  }

  useEffect(() => {
    const timer = setTimeout(() => {
      if (inputRef.current?.element) {
        inputRef.current.element.focus()
      }
    }, 50)
    return () => clearTimeout(timer)
  }, [])

  return (
    <td>
      <DatePicker
        ref={inputRef}
        value={currentDate}
        format='dd-MM-yyyy'
        onChange={handleChange}
        onBlur={handleBlur}
        width='100%'
        size='small'
        style={{
          width: '100%',
          fontSize: '15px',
          height: '40px',
        }}
        className='input-editor'
      />
    </td>
  )
}

export default DateOnlyPicker
