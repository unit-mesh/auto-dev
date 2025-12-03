/**
 * Model Selector Component
 * 
 * Dropdown for selecting LLM models with a configure option.
 * Similar to IdeaModelSelector.kt from mpp-idea.
 */

import React, { useState, useRef, useEffect } from 'react';
import './ModelSelector.css';

export interface ModelConfig {
  name: string;
  provider: string;
  model: string;
}

interface ModelSelectorProps {
  availableConfigs: ModelConfig[];
  currentConfigName: string | null;
  onConfigSelect: (config: ModelConfig) => void;
  onConfigureClick: () => void;
}

export const ModelSelector: React.FC<ModelSelectorProps> = ({
  availableConfigs,
  currentConfigName,
  onConfigSelect,
  onConfigureClick
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const currentConfig = availableConfigs.find(c => c.name === currentConfigName);
  const displayText = currentConfig 
    ? `${currentConfig.provider}/${currentConfig.model}`
    : 'Configure Model';

  return (
    <div className="model-selector" ref={dropdownRef}>
      <button 
        className={`model-selector-trigger ${isOpen ? 'open' : ''}`}
        onClick={() => setIsOpen(!isOpen)}
        title="Select LLM Model"
      >
        <svg className="model-icon" width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
          <path d="M20 9V7c0-1.1-.9-2-2-2h-3c0-1.66-1.34-3-3-3S9 3.34 9 5H6c-1.1 0-2 .9-2 2v2c-1.66 0-3 1.34-3 3s1.34 3 3 3v4c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2v-4c1.66 0 3-1.34 3-3s-1.34-3-3-3zM7.5 11.5c0-.83.67-1.5 1.5-1.5s1.5.67 1.5 1.5S9.83 13 9 13s-1.5-.67-1.5-1.5zM16 17H8v-2h8v2zm-1-4c-.83 0-1.5-.67-1.5-1.5S14.17 10 15 10s1.5.67 1.5 1.5S15.83 13 15 13z"/>
        </svg>
        <span className="model-text">{displayText}</span>
        <svg className="dropdown-arrow" width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
          <path d="M7 10l5 5 5-5z"/>
        </svg>
      </button>

      {isOpen && (
        <div className="model-dropdown">
          {availableConfigs.length > 0 ? (
            <>
              {availableConfigs.map((config) => (
                <button
                  key={config.name}
                  className={`dropdown-item ${config.name === currentConfigName ? 'selected' : ''}`}
                  onClick={() => {
                    onConfigSelect(config);
                    setIsOpen(false);
                  }}
                >
                  <span className="config-label">{config.provider} / {config.model}</span>
                  {config.name === currentConfigName && (
                    <svg className="check-icon" width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
                    </svg>
                  )}
                </button>
              ))}
              <div className="dropdown-separator" />
            </>
          ) : (
            <>
              <div className="dropdown-item disabled">No saved configs</div>
              <div className="dropdown-separator" />
            </>
          )}
          
          <button
            className="dropdown-item configure-item"
            onClick={() => {
              onConfigureClick();
              setIsOpen(false);
            }}
          >
            <svg className="settings-icon" width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19.14 12.94c.04-.31.06-.63.06-.94 0-.31-.02-.63-.06-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.04.31-.06.63-.06.94s.02.63.06.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"/>
            </svg>
            <span>Configure Model...</span>
          </button>
        </div>
      )}
    </div>
  );
};

