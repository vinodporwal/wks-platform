/**
 * React UI Component Example for Excel Download Button
 * 
 * This is a reference implementation for the React frontend team.
 * Add this component to your calculation logs table/grid.
 */

import React, { useState } from 'react';
import { Download, FileSpreadsheet } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { toast } from '@/components/ui/use-toast';

interface CalculationLog {
  id: string;
  financialYear: number;
  month: number;
  status: string;
  executionDateTime: string;
  // ... other fields
}

interface ExcelDownloadButtonProps {
  logId: string;
  monthYear?: string;
}

/**
 * Download button component for Excel balance summary reports
 */
export const ExcelDownloadButton: React.FC<ExcelDownloadButtonProps> = ({ 
  logId, 
  monthYear 
}) => {
  const [isDownloading, setIsDownloading] = useState(false);
  const [hasExcel, setHasExcel] = useState<boolean | null>(null);

  // Check if Excel exists on component mount
  React.useEffect(() => {
    checkExcelExists();
  }, [logId]);

  const checkExcelExists = async () => {
    try {
      const response = await fetch(`/api/calculation-logs/${logId}/has-excel`);
      if (response.ok) {
        const data = await response.json();
        setHasExcel(data.hasExcel);
      }
    } catch (error) {
      console.error('Error checking Excel existence:', error);
    }
  };

  const downloadExcel = async () => {
    setIsDownloading(true);
    
    try {
      const response = await fetch(`/api/calculation-logs/${logId}/download-excel`);
      
      if (!response.ok) {
        if (response.status === 404) {
          toast({
            title: 'Excel Not Found',
            description: 'No Excel report available for this calculation log.',
            variant: 'destructive',
          });
          return;
        }
        throw new Error('Failed to download Excel report');
      }

      // Get filename from Content-Disposition header
      const contentDisposition = response.headers.get('Content-Disposition');
      let filename = `balance_summary_${monthYear || 'report'}.xlsx`;
      
      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename="?(.+)"?/);
        if (filenameMatch) {
          filename = filenameMatch[1];
        }
      }

      // Download the file
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      
      // Cleanup
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);

      toast({
        title: 'Download Complete',
        description: `${filename} has been downloaded successfully.`,
      });

    } catch (error) {
      console.error('Error downloading Excel:', error);
      toast({
        title: 'Download Failed',
        description: 'Failed to download Excel report. Please try again.',
        variant: 'destructive',
      });
    } finally {
      setIsDownloading(false);
    }
  };

  // Don't show button if no Excel exists
  if (hasExcel === false) {
    return null;
  }

  return (
    <Button
      variant="outline"
      size="sm"
      onClick={downloadExcel}
      disabled={isDownloading || hasExcel === null}
      className="gap-2"
    >
      {isDownloading ? (
        <>
          <div className="animate-spin h-4 w-4 border-2 border-current border-t-transparent rounded-full" />
          Downloading...
        </>
      ) : (
        <>
          <Download className="h-4 w-4" />
          Download Excel
        </>
      )}
    </Button>
  );
};

/**
 * Example usage in a table row
 */
export const CalculationLogTableRow: React.FC<{ log: CalculationLog }> = ({ log }) => {
  const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 
                      'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  const monthYear = `${monthNames[log.month - 1]}_${log.financialYear}`;

  return (
    <tr>
      <td>{log.executionDateTime}</td>
      <td>{monthYear}</td>
      <td>{log.status}</td>
      <td>
        <div className="flex gap-2">
          <Button variant="ghost" size="sm">
            View Details
          </Button>
          <ExcelDownloadButton 
            logId={log.id} 
            monthYear={monthYear} 
          />
        </div>
      </td>
    </tr>
  );
};

/**
 * Alternative: Icon-only button for compact tables
 */
export const ExcelDownloadIconButton: React.FC<ExcelDownloadButtonProps> = ({ 
  logId, 
  monthYear 
}) => {
  const [isDownloading, setIsDownloading] = useState(false);

  const downloadExcel = async () => {
    setIsDownloading(true);
    
    try {
      const response = await fetch(`/api/calculation-logs/${logId}/download-excel`);
      
      if (!response.ok) {
        throw new Error('Failed to download');
      }

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `balance_summary_${monthYear || 'report'}.xlsx`;
      a.click();
      window.URL.revokeObjectURL(url);

    } catch (error) {
      console.error('Error:', error);
    } finally {
      setIsDownloading(false);
    }
  };

  return (
    <button
      onClick={downloadExcel}
      disabled={isDownloading}
      className="p-2 hover:bg-gray-100 rounded-md transition-colors"
      title="Download Excel Report"
    >
      {isDownloading ? (
        <div className="animate-spin h-4 w-4 border-2 border-current border-t-transparent rounded-full" />
      ) : (
        <FileSpreadsheet className="h-4 w-4 text-green-600" />
      )}
    </button>
  );
};
