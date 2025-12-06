/**
 * NanoUI Renderer for VSCode Webview
 * 
 * Renders NanoIR components to React elements.
 * Follows the component-specific method pattern from the Kotlin NanoRenderer interface.
 * 
 * Each component type has its own render function, making it easy to identify
 * missing implementations when new components are added.
 * 
 * @see xuiper-ui/src/main/kotlin/cc/unitmesh/xuiper/render/NanoRenderer.kt
 * @see mpp-ui/src/jvmMain/kotlin/cc/unitmesh/devins/ui/nano/ComposeNanoRenderer.kt
 */

import React from 'react';
import { NanoIR, NanoRenderContext, DEFAULT_THEME } from '../../types/nano';
import './NanoRenderer.css';

interface NanoRendererProps {
  ir: NanoIR;
  context?: Partial<NanoRenderContext>;
  className?: string;
}

// Default context
const defaultContext: NanoRenderContext = {
  state: {},
  theme: DEFAULT_THEME,
};

/**
 * Main NanoRenderer component
 */
export const NanoRenderer: React.FC<NanoRendererProps> = ({
  ir,
  context = {},
  className,
}) => {
  const fullContext = { ...defaultContext, ...context };
  
  return (
    <div className={`nano-renderer ${className || ''}`}>
      <RenderNode ir={ir} context={fullContext} />
    </div>
  );
};

/**
 * Dispatch rendering based on component type
 */
const RenderNode: React.FC<{ ir: NanoIR; context: NanoRenderContext }> = ({ ir, context }) => {
  switch (ir.type) {
    // Layout
    case 'VStack': return <RenderVStack ir={ir} context={context} />;
    case 'HStack': return <RenderHStack ir={ir} context={context} />;
    // Container
    case 'Card': return <RenderCard ir={ir} context={context} />;
    case 'Form': return <RenderForm ir={ir} context={context} />;
    // Content
    case 'Text': return <RenderText ir={ir} context={context} />;
    case 'Image': return <RenderImage ir={ir} context={context} />;
    case 'Badge': return <RenderBadge ir={ir} context={context} />;
    case 'Divider': return <RenderDivider />;
    // Input
    case 'Button': return <RenderButton ir={ir} context={context} />;
    case 'Input': return <RenderInput ir={ir} context={context} />;
    case 'Checkbox': return <RenderCheckbox ir={ir} context={context} />;
    case 'TextArea': return <RenderTextArea ir={ir} context={context} />;
    case 'Select': return <RenderSelect ir={ir} context={context} />;
    // Control Flow
    case 'Conditional': return <RenderConditional ir={ir} context={context} />;
    case 'ForLoop': return <RenderForLoop ir={ir} context={context} />;
    // Meta
    case 'Component': return <RenderComponent ir={ir} context={context} />;
    default: return <RenderUnknown ir={ir} />;
  }
};

// ============================================================================
// Layout Components
// ============================================================================

const RenderVStack: React.FC<{ ir: NanoIR; context: NanoRenderContext }> = ({ ir, context }) => {
  const spacing = ir.props.spacing || 'md';
  const align = ir.props.align || 'stretch';
  
  return (
    <div className={`nano-vstack spacing-${spacing} align-${align}`}>
      {ir.children?.map((child, i) => (
        <RenderNode key={i} ir={child} context={context} />
      ))}
    </div>
  );
};

const RenderHStack: React.FC<{ ir: NanoIR; context: NanoRenderContext }> = ({ ir, context }) => {
  const spacing = ir.props.spacing || 'md';
  const align = ir.props.align || 'center';
  const justify = ir.props.justify || 'start';
  
  return (
    <div className={`nano-hstack spacing-${spacing} align-${align} justify-${justify}`}>
      {ir.children?.map((child, i) => (
        <RenderNode key={i} ir={child} context={context} />
      ))}
    </div>
  );
};

// ============================================================================
// Container Components
// ============================================================================

const RenderCard: React.FC<{ ir: NanoIR; context: NanoRenderContext }> = ({ ir, context }) => {
  const padding = ir.props.padding || 'md';
  const shadow = ir.props.shadow || 'sm';
  
  return (
    <div className={`nano-card padding-${padding} shadow-${shadow}`}>
      {ir.children?.map((child, i) => (
        <RenderNode key={i} ir={child} context={context} />
      ))}
    </div>
  );
};

const RenderForm: React.FC<{ ir: NanoIR; context: NanoRenderContext }> = ({ ir, context }) => {
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (ir.props.onSubmit && context.dispatch) {
      context.dispatch({ type: 'Fetch', url: ir.props.onSubmit, method: 'POST' });
    }
  };
  
  return (
    <form className="nano-form" onSubmit={handleSubmit}>
      {ir.children?.map((child, i) => (
        <RenderNode key={i} ir={child} context={context} />
      ))}
    </form>
  );
};

// ============================================================================
// Content Components
// ============================================================================

const RenderText: React.FC<{ ir: NanoIR; context: NanoRenderContext }> = ({ ir }) => {
  const content = ir.props.content || '';
  const style = ir.props.style || 'body';
  
  const Tag = getTextTag(style);
  return <Tag className={`nano-text style-${style}`}>{content}</Tag>;
};

function getTextTag(style: string): keyof JSX.IntrinsicElements {
  switch (style) {
    case 'h1': return 'h1';
    case 'h2': return 'h2';
    case 'h3': return 'h3';
    case 'h4': return 'h4';
    case 'caption': return 'small';
    default: return 'p';
  }
}

const RenderImage: React.FC<{ ir: NanoIR; context: NanoRenderContext }> = ({ ir }) => {
  const src = ir.props.src || '';
  const radius = ir.props.radius || 'none';
  const alt = ir.props.alt ?? 'Image';

  return <img src={src} className={`nano-image radius-${radius}`} alt={alt} />;
};

const RenderBadge: React.FC<{ ir: NanoIR; context: NanoRenderContext }> = ({ ir }) => {
  const text = ir.props.text || '';
  const color = ir.props.color || 'default';

  return <span className={`nano-badge color-${color}`}>{text}</span>;
};

const RenderDivider: React.FC = () => {
  return <hr className="nano-divider" />;
};

// ============================================================================
// Input Components
// ============================================================================

const RenderButton: React.FC<{ ir: NanoIR; context: NanoRenderContext }> = ({ ir, context }) => {
  const label = ir.props.label || 'Button';
  const intent = ir.props.intent || 'default';
  const icon = ir.props.icon;

  const handleClick = () => {
    if (ir.actions?.onClick && context.dispatch) {
      const action = ir.actions.onClick;
      if (action.type === 'Navigate' && action.payload?.to) {
        context.dispatch({ type: 'Navigate', to: action.payload.to });
      } else if (action.type === 'Fetch' && action.payload?.url) {
        context.dispatch({ type: 'Fetch', url: action.payload.url, method: action.payload.method });
      } else if (action.type === 'ShowToast' && action.payload?.message) {
        context.dispatch({ type: 'ShowToast', message: action.payload.message });
      }
    }
  };

  return (
    <button className={`nano-button intent-${intent}`} onClick={handleClick}>
      {icon && <span className="icon">{icon}</span>}
      {label}
    </button>
  );
};

const RenderInput: React.FC<{ ir: NanoIR; context: NanoRenderContext }> = ({ ir }) => {
  const placeholder = ir.props.placeholder || '';
  const type = ir.props.type || 'text';

  return (
    <input
      type={type}
      className="nano-input"
      placeholder={placeholder}
    />
  );
};

const RenderCheckbox: React.FC<{ ir: NanoIR; context: NanoRenderContext }> = ({ ir }) => {
  const label = ir.props.label;
  return (
    <label className="nano-checkbox-wrapper">
      <input type="checkbox" className="nano-checkbox" />
      {label && <span>{label}</span>}
    </label>
  );
};

const RenderTextArea: React.FC<{ ir: NanoIR; context: NanoRenderContext }> = ({ ir }) => {
  const placeholder = ir.props.placeholder || '';
  const rows = ir.props.rows || 4;

  return (
    <textarea
      className="nano-textarea"
      placeholder={placeholder}
      rows={rows}
    />
  );
};

const RenderSelect: React.FC<{ ir: NanoIR; context: NanoRenderContext }> = ({ ir }) => {
  const placeholder = ir.props.placeholder;

  return (
    <select className="nano-select" defaultValue="">
      {placeholder && (
        <option value="" disabled>{placeholder}</option>
      )}
    </select>
  );
};

// ============================================================================
// Control Flow Components
// ============================================================================

const RenderConditional: React.FC<{ ir: NanoIR; context: NanoRenderContext }> = ({ ir, context }) => {
  // In static preview, render the then branch
  return (
    <div className="nano-conditional">
      {ir.children?.map((child, i) => (
        <RenderNode key={i} ir={child} context={context} />
      ))}
    </div>
  );
};

const RenderForLoop: React.FC<{ ir: NanoIR; context: NanoRenderContext }> = ({ ir, context }) => {
  // In static preview, show a single iteration
  return (
    <div className="nano-loop">
      {ir.children?.map((child, i) => (
        <RenderNode key={i} ir={child} context={context} />
      ))}
    </div>
  );
};

// ============================================================================
// Meta Components
// ============================================================================

const RenderComponent: React.FC<{ ir: NanoIR; context: NanoRenderContext }> = ({ ir, context }) => {
  const name = ir.props.name || 'Component';

  return (
    <div className="nano-component" data-name={name}>
      {ir.children?.map((child, i) => (
        <RenderNode key={i} ir={child} context={context} />
      ))}
    </div>
  );
};

const RenderUnknown: React.FC<{ ir: NanoIR }> = ({ ir }) => {
  return (
    <div className="nano-unknown">
      Unknown component: {ir.type}
    </div>
  );
};

