/**
 * PlanSummaryBar Component
 * 
 * Displays a collapsible summary of the current plan above the input box.
 * Shows progress, current step, and allows expanding to see full plan details.
 */

import React, { useState } from 'react';
import './PlanSummaryBar.css';

// Plan data types (matching mpp-core's PlanSummaryData)
export interface PlanStep {
  id: string;
  description: string;
  status: TaskStatus;
}

export interface PlanTask {
  id: string;
  title: string;
  status: TaskStatus;
  completedSteps: number;
  totalSteps: number;
  steps: PlanStep[];
}

export interface PlanData {
  planId: string;
  title: string;
  totalSteps: number;
  completedSteps: number;
  failedSteps: number;
  progressPercent: number;
  status: TaskStatus;
  currentStepDescription: string | null;
  tasks: PlanTask[];
}

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'BLOCKED';

interface PlanSummaryBarProps {
  plan: PlanData | null;
  onViewDetails?: () => void;
  onDismiss?: () => void;
}

export const PlanSummaryBar: React.FC<PlanSummaryBarProps> = ({
  plan,
  onViewDetails: _onViewDetails, // Reserved for future use
  onDismiss
}) => {
  const [isExpanded, setIsExpanded] = useState(false);

  // Don't render if no plan
  if (!plan || plan.tasks.length === 0) {
    return null;
  }

  const getStatusIcon = (status: TaskStatus) => {
    switch (status) {
      case 'COMPLETED':
        return <span className="status-icon completed">✓</span>;
      case 'FAILED':
        return <span className="status-icon failed">✗</span>;
      case 'IN_PROGRESS':
        return <span className="status-icon in-progress">⟳</span>;
      case 'BLOCKED':
        return <span className="status-icon blocked">⚠</span>;
      default:
        return <span className="status-icon todo">○</span>;
    }
  };

  const getStatusClass = (status: TaskStatus) => {
    switch (status) {
      case 'COMPLETED': return 'status-completed';
      case 'FAILED': return 'status-failed';
      case 'IN_PROGRESS': return 'status-in-progress';
      case 'BLOCKED': return 'status-blocked';
      default: return 'status-todo';
    }
  };

  const findCurrentStep = (): string | null => {
    for (const task of plan.tasks) {
      for (const step of task.steps) {
        if (step.status === 'IN_PROGRESS') {
          return step.description;
        }
      }
    }
    for (const task of plan.tasks) {
      for (const step of task.steps) {
        if (step.status === 'TODO') {
          return step.description;
        }
      }
    }
    return null;
  };

  const currentStep = findCurrentStep() || plan.currentStepDescription;

  return (
    <div className={`plan-summary-bar ${getStatusClass(plan.status)}`}>
      {/* Collapsed header */}
      <div
        className="plan-summary-header"
        onClick={() => setIsExpanded(!isExpanded)}
      >
        <div className="plan-header-left">
          {getStatusIcon(plan.status)}
          <span className={`expand-arrow ${isExpanded ? 'expanded' : ''}`}>
            {isExpanded ? '▼' : '▶'}
          </span>
          <span className="plan-title">{plan.title}</span>
          
          {/* Progress indicator */}
          <div className="plan-progress">
            <div className="progress-bar">
              <div
                className={`progress-fill ${getStatusClass(plan.status)}`}
                style={{ width: `${plan.progressPercent}%` }}
              />
            </div>
            <span className="progress-text">
              {plan.completedSteps}/{plan.totalSteps}
            </span>
          </div>
        </div>

        <div className="plan-header-right">
          {/* Current step description (when collapsed) */}
          {currentStep && !isExpanded && (
            <span className="current-step">{currentStep}</span>
          )}
          
          {/* Dismiss button */}
          {onDismiss && (
            <button
              className="dismiss-button"
              onClick={(e) => {
                e.stopPropagation();
                onDismiss();
              }}
              title="Dismiss"
            >
              ×
            </button>
          )}
        </div>
      </div>

      {/* Expanded content */}
      {isExpanded && (
        <div className="plan-expanded-content">
          {plan.tasks.map((task) => (
            <TaskItem key={task.id} task={task} />
          ))}
        </div>
      )}
    </div>
  );
};

const TaskItem: React.FC<{ task: PlanTask }> = ({ task }) => {
  const [isExpanded, setIsExpanded] = useState(true);

  const getStatusIcon = (status: TaskStatus) => {
    switch (status) {
      case 'COMPLETED':
        return <span className="step-icon completed">✓</span>;
      case 'FAILED':
        return <span className="step-icon failed">✗</span>;
      case 'IN_PROGRESS':
        return <span className="step-icon in-progress">⟳</span>;
      default:
        return <span className="step-icon todo">○</span>;
    }
  };

  return (
    <div className="task-item">
      <div
        className="task-header"
        onClick={() => setIsExpanded(!isExpanded)}
      >
        <div className="task-header-left">
          {getStatusIcon(task.status)}
          <span className="task-title">{task.title}</span>
        </div>
        <span className="task-progress">
          {task.completedSteps}/{task.totalSteps}
        </span>
      </div>

      {isExpanded && task.steps.length > 0 && (
        <div className="task-steps">
          {task.steps.map((step) => (
            <StepItem key={step.id} step={step} />
          ))}
        </div>
      )}
    </div>
  );
};

const StepItem: React.FC<{ step: PlanStep }> = ({ step }) => {
  const getStepIcon = (status: TaskStatus) => {
    switch (status) {
      case 'COMPLETED':
        return <span className="step-icon completed">✓</span>;
      case 'FAILED':
        return <span className="step-icon failed">✗</span>;
      case 'IN_PROGRESS':
        return <span className="step-icon in-progress spinning">⟳</span>;
      default:
        return <span className="step-icon todo">•</span>;
    }
  };

  const getStepClass = (status: TaskStatus) => {
    switch (status) {
      case 'COMPLETED': return 'step-completed';
      case 'FAILED': return 'step-failed';
      case 'IN_PROGRESS': return 'step-in-progress';
      default: return 'step-todo';
    }
  };

  return (
    <div className={`step-item ${getStepClass(step.status)}`}>
      {getStepIcon(step.status)}
      <span className="step-description">{step.description}</span>
    </div>
  );
};

export default PlanSummaryBar;

