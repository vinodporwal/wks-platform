export default function GetFinancialYear(yearRange) {
    const [startYear, endYear] = yearRange.split('-').map(Number)
    if (!startYear || !endYear) {
        console.error('Invalid YEAR format')
        return {}
    }
    
    // Calculate current and previous financial years
    const prevStartYear = startYear - 1;
    const prevEndYear = (prevStartYear + 1) % 100;
    const previousFYFormatted = `${prevStartYear}-${prevEndYear}`; // Outputs: 2025-26
    
    return {
      startYear,
      formatted: `${startYear}-${endYear}`,
      prevStartYear,
      prevEndYear,
      previousFYFormatted
    };
} 

