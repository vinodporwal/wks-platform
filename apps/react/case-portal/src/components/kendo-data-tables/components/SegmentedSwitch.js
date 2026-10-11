import React from 'react';
import PropTypes from 'prop-types';
import { Box, Typography, styled } from '@mui/material';

// Outer pill container
const SwitchContainer = styled(Box, {
  shouldForwardProp: (prop) => !['bgColor', 'customWidth', 'customHeight'].includes(prop),
})(({ bgColor, customWidth, customHeight }) => ({
  display: 'inline-flex',
  alignItems: 'center',
  backgroundColor: bgColor,
  borderRadius: '50px',
  padding: '6px',
  position: 'relative',
  cursor: 'pointer',
  userSelect: 'none',
  width: typeof customWidth === 'number' ? `${customWidth}px` : customWidth,
  height: typeof customHeight === 'number' ? `${customHeight}px` : customHeight,
  boxSizing: 'border-box',
}));

// Sliding indicator background capsule
const SelectionIndicator = styled(Box, {
  shouldForwardProp: (prop) => !['selectedIndex', 'totalOptions', 'indicatorBgColor'].includes(prop),
})(({ selectedIndex, totalOptions, indicatorBgColor }) => {
  const widthPercentage = 100 / totalOptions;
  return {
    position: 'absolute',
    top: '6px',
    bottom: '6px',
    left: '6px',
    width: `calc(${widthPercentage}% - ${12 / totalOptions}px)`,
    backgroundColor: indicatorBgColor,
    borderRadius: '40px',
    transform: `translateX(${selectedIndex * 100}%)`,
    transition: 'transform 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
    boxShadow: '0px 2px 6px rgba(0, 0, 0, 0.1)',
  };
});

// Option Label
const TabOption = styled(Typography, {
  shouldForwardProp: (prop) => !['active', 'activeColor', 'inactiveColor'].includes(prop),
})(({ active, activeColor, inactiveColor }) => ({
  flex: 1,
  textAlign: 'center',
  zIndex: 1,
  fontWeight: 600,
  fontSize: '0.75rem',
  color: active ? activeColor : inactiveColor,
  transition: 'color 0.3s ease',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  height: '100%',
}));

export default function SegmentedSwitch({
  options = ['On', 'Off'],
  value,
  onChange,
  bgColor = '#4a4dda',
  indicatorBgColor = '#FFFFFF',
  activeTextColor = '#4a4dda',
  inactiveTextColor = '#FFFFFF',
  width = 160,
  height = 30,
}) {
  // Support both array of strings OR array of objects with { label, value }
  const getOptionLabel = (option) => (typeof option === 'object' ? option.label : option);
  const getOptionValue = (option) => (typeof option === 'object' ? option.value : option);

  const selectedIndex = Math.max(
    0,
    options.findIndex((opt) => getOptionValue(opt) === value)
  );

  const handleSelect = (optionValue) => {
    if (onChange) {
      onChange(optionValue);
    }
  };

  return (
    <SwitchContainer bgColor={bgColor} customWidth={width} customHeight={height}>
      {/* Dynamic Sliding Pill Indicator */}
      <SelectionIndicator
        selectedIndex={selectedIndex}
        totalOptions={options.length}
        indicatorBgColor={indicatorBgColor}
      />

      {/* Options List */}
      {options.map((option) => {
        const optValue = getOptionValue(option);
        const optLabel = getOptionLabel(option);
        const isActive = optValue === value;

        return (
          <TabOption
            key={optValue}
            active={isActive}
            activeColor={activeTextColor}
            inactiveColor={inactiveTextColor}
            onClick={() => handleSelect(optValue)}
          >
            {optLabel}
          </TabOption>
        );
      })}
    </SwitchContainer>
  );
}

// PropTypes validation
SegmentedSwitch.propTypes = {
  options: PropTypes.array.isRequired,
  value: PropTypes.any.isRequired,
  onChange: PropTypes.func.isRequired,
  bgColor: PropTypes.string,
  indicatorBgColor: PropTypes.string,
  activeTextColor: PropTypes.string,
  inactiveTextColor: PropTypes.string,
  width: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  height: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
};