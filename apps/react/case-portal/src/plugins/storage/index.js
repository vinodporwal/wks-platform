import { Formio } from 'formiojs'
import Config from '../../consts'
import MemoryTokenManager from '../MemoryTokenManager'

export class StorageService {
  // Modify the file name to include a timestamp
  async modifyFileName(fileName){
    const lastDotIndex = fileName.lastIndexOf('.');
    const baseName = (lastDotIndex === -1) ? fileName : fileName.substring(0, lastDotIndex);
    const extension = (lastDotIndex === -1) ? '' : fileName.substring(lastDotIndex);

    // Get the current timestamp
    const now = new Date();
    const timestamp = now.toISOString().replace(/[-:T]/g, '').slice(0, 14);

    // Construct the new file name with the timestamp
    const modifiedFileName = baseName + '_' + timestamp + extension;

    return modifiedFileName;
  }

  // Upload a file
  async uploadFile(storage, file, fileName, dir = 'cases', evt) {
    try {
      // Ensure directory is set
      dir = dir || 'cases';

      // Modify the file name
      const modifiedFileName = await this.modifyFileName(file.name);

      // Create updated file metadata object
      const updatedFile = {
        name: modifiedFileName, // Modified file name
        size: file.size,        // Retain 'size'
        type: file.type,        // Retain 'type'
        originalFile: file      // Store the original file if needed
      };

      // Call the assumed `minio().uploadFile()` method (ensure minio() is imported/defined)
      return minio().uploadFile(updatedFile, dir, evt);
    } catch (err) {
      console.error('Error uploading file:', err);
      throw err;
    }
  }

  // Placeholder for deleteFile method
  async deleteFile() {
    // Implement deletion logic here
  }

  // Download a file
  async downloadFile(file){
    try {
      return minio().downloadFile(file);
    } catch (err) {
      console.error('Error downloading file:', err);
      throw err;
    }
  }
}

export function minio() {
  function createHeaders() {
    return {
      headers: {
        Authorization: `Bearer ${MemoryTokenManager.getToken()}`,
      },
    }
  }

  return {
    uploadFile(file, dir, progressCallback, abortCallback) {
      function doUpload(url, fd) {
        return new Promise((resolve, reject) => {
          let request = new XMLHttpRequest()

          request.open('POST', url)

          request.addEventListener('openAndSetHeaders', function (...params) {
            request.open(...params)
            request.setRequestHeader(
              'Authorization',
              `Bearer ${Formio.getToken()}`,
            )
          })

          request.upload.addEventListener('progress', function (e) {
            if (typeof progressCallback === 'function') {
              progressCallback(e)
            }

            if (typeof abortCallback === 'function') {
              abortCallback(() => request.abort())
            }
          })

          request.addEventListener('load', function () {
            if (request.status >= 200 && request.status < 300) {
              resolve({
                storage: 'minio',
                dir: dir,
                name: file.name,
                url: file.name,
                size: file.size,
                type: file.type,
              })
            } else {
              reject(request.response || 'Unable to upload file')
            }
          })

          request.addEventListener('error', function (e) {
            e.networkError = true
            reject(e)
          })

          request.addEventListener('abort', function (e) {
            e.networkError = true
            reject(e)
          })

          request.send(fd)
        })
      }

      let goUploadToFileUrl = `${Config.StorageUrl}/storage/files/${dir}/uploads/${file.name}?content-type=${file.type}`
      if (!dir) {
        goUploadToFileUrl = `${Config.StorageUrl}/storage/files/uploads/${file.name}?content-type=${file.type}`
      }

      return fetch(goUploadToFileUrl, createHeaders())
        .then((resp) => resp.json())
        .then((data) => {
          const form = new FormData()

          for (const key in data.formData) {
            form.append(key, data.formData[key])
          }

          if (!dir) {
            form.append('key', file.name)
          } else {
            form.append('key', dir + '/' + file.name)
          }

          form.append('content-type', file.type)
          form.append('file', file.originalFile)

          return doUpload(data.url, form)
        })
    },
    downloadFile(file) {
      let getObjectForUrl = `${Config.StorageUrl}/storage/files1/${file.dir}/downloads/${file.name}?content-type=${file.type}`
      if (!file.dir) {
        getObjectForUrl = `${Config.StorageUrl}/storage/files1/downloads/${file.name}?content-type=${file.type}`
      }

      return {url:getObjectForUrl};
      // return fetch(getObjectForUrl)
      //   .then((resp) => resp.json())
      //   .then(async (data) => {
          // const resp = await fetch(data.url)
          // const blob = await resp.blob()
          // const downloadUrl = window.URL.createObjectURL(blob)

          // const anchor = document.createElement('a')
          // document.body.appendChild(anchor)
          // anchor.href = downloadUrl

          // const url = new URL(data.url)
          // if (url.pathname) {
          //   anchor.download = url.pathname
          //     .slice(url.pathname.lastIndexOf('/') + 1)
          //     .replaceAll("'")
          // } else {
          //   anchor.download = downloadUrl
          // }

          // anchor.click()

          // setTimeout(() => {
          //   window.URL.revokeObjectURL(downloadUrl)
          //   document.body.removeChild(anchor)
          // }, 0)
        //   return;
        // })
    },
  }
}

minio.title = 's3'
