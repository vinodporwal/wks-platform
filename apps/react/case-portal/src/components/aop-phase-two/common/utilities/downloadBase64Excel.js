/**
 * Decodes a base64-encoded Excel file from an API response and triggers a
 * browser download. Use this whenever an import API returns code 400 with a
 * base64 error-file payload.
 *
 * @param {string} base64Data - Base64-encoded Excel file content (response.data)
 * @param {string} fileName   - File name for the download (e.g. 'Prices_Errors.xlsx')
 */
export function downloadBase64Excel(base64Data, fileName) {
  const binaryString = window.atob(base64Data)
  const bytes = new Uint8Array(binaryString.length)
  for (let i = 0; i < binaryString.length; i++) {
    bytes[i] = binaryString.charCodeAt(i)
  }
  const blob = new Blob([bytes], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}
