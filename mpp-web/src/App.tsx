import React, { useState, useEffect } from 'react';
// @ts-ignore - mpp-core types will be available after build
import * as mppCore from '@autodev/mpp-core';
import { ConfigService, type ModelConfig } from './services/ConfigService';
import { LLMService } from './services/LLMService';

export const App: React.FC = () => {
  const [message, setMessage] = useState<string>('');
  const [response, setResponse] = useState<string>('');
  const [coreLoaded, setCoreLoaded] = useState<boolean>(false);
  const [llmService, setLlmService] = useState<LLMService | null>(null);
  const [isStreaming, setIsStreaming] = useState<boolean>(false);
  const [showConfig, setShowConfig] = useState<boolean>(false);
  const [config, setConfig] = useState<ModelConfig>(ConfigService.getDefaultConfig());
  const [chatHistory, setChatHistory] = useState<Array<{role: string; content: string}>>([]);

  useEffect(() => {
    // Test mpp-core loading
    try {
      console.log('mpp-core loaded:', mppCore);
      setCoreLoaded(true);

      // Load saved configuration
      const savedConfig = ConfigService.load();
      if (savedConfig) {
        setConfig(savedConfig);
        try {
          const service = new LLMService(savedConfig);
          setLlmService(service);
        } catch (error) {
          console.error('Failed to initialize LLM service:', error);
        }
      } else {
        setShowConfig(true); // Show config dialog if no config exists
      }
    } catch (error) {
      console.error('Failed to load mpp-core:', error);
    }
  }, []);

  const handleSaveConfig = () => {
    if (!config.apiKey) {
      alert('Please enter an API key');
      return;
    }

    ConfigService.save(config);
    
    try {
      const service = new LLMService(config);
      setLlmService(service);
      setShowConfig(false);
      alert('✓ Configuration saved successfully!');
    } catch (error: any) {
      alert(`Failed to initialize LLM service: ${error.message}`);
    }
  };

  const handleSend = async () => {
    if (!message.trim() || !llmService || isStreaming) return;

    const userMessage = message;
    setMessage('');
    setResponse('');
    setIsStreaming(true);

    // Add user message to chat history
    setChatHistory(prev => [...prev, { role: 'user', content: userMessage }]);

    let fullResponse = '';
    
    try {
      await llmService.streamMessage(
        userMessage,
        (chunk: string) => {
          fullResponse += chunk;
          setResponse(fullResponse);
        },
        (error: any) => {
          console.error('Streaming error:', error);
          setResponse(`Error: ${error.message || error}`);
        },
        () => {
          // Streaming completed
          setIsStreaming(false);
          // Add assistant response to chat history
          setChatHistory(prev => [...prev, { role: 'assistant', content: fullResponse }]);
        }
      );
    } catch (error: any) {
      setResponse(`Error: ${error.message || error}`);
      setIsStreaming(false);
      // Remove failed user message from history
      setChatHistory(prev => prev.slice(0, -1));
    }
  };

  const handleClearHistory = () => {
    if (llmService) {
      llmService.clearHistory();
    }
    setChatHistory([]);
    setResponse('');
    alert('✓ Chat history cleared');
  };

  return (
    <div style={{ 
      maxWidth: '1200px', 
      margin: '0 auto', 
      padding: '20px', 
      fontFamily: 'system-ui, -apple-system, sans-serif' 
    }}>
      <header style={{ marginBottom: '30px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1 style={{ fontSize: '2.5rem', fontWeight: 'bold', marginBottom: '10px' }}>
            🤖 AutoDev Web UI
          </h1>
          <p style={{ color: '#666', fontSize: '1.1rem' }}>
            Lightweight React-based web interface using mpp-core
          </p>
          {coreLoaded && llmService && (
            <p style={{ color: '#28a745', fontSize: '0.9rem', marginTop: '5px' }}>
              ✅ Connected: {config.provider}/{config.model}
            </p>
          )}
          {coreLoaded && !llmService && (
            <p style={{ color: '#ffa500', fontSize: '0.9rem', marginTop: '5px' }}>
              ⚠️ Please configure your LLM provider
            </p>
          )}
        </div>
        <div>
          <button
            onClick={() => setShowConfig(!showConfig)}
            style={{
              padding: '10px 20px',
              marginRight: '10px',
              backgroundColor: '#6c757d',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer',
              fontWeight: '500'
            }}
          >
            ⚙️ Settings
          </button>
          {chatHistory.length > 0 && (
            <button
              onClick={handleClearHistory}
              style={{
                padding: '10px 20px',
                backgroundColor: '#dc3545',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer',
                fontWeight: '500'
              }}
            >
              🗑️ Clear History
            </button>
          )}
        </div>
      </header>

      {/* Configuration Modal */}
      {showConfig && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0,0,0,0.5)',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          zIndex: 1000
        }}>
          <div style={{
            backgroundColor: 'white',
            padding: '30px',
            borderRadius: '8px',
            maxWidth: '500px',
            width: '90%'
          }}>
            <h2 style={{ marginBottom: '20px' }}>⚙️ LLM Configuration</h2>
            
            <div style={{ marginBottom: '15px' }}>
              <label style={{ display: 'block', marginBottom: '5px', fontWeight: '500' }}>Provider</label>
              <select
                value={config.provider}
                onChange={(e) => setConfig({ ...config, provider: e.target.value })}
                style={{
                  width: '100%',
                  padding: '10px',
                  fontSize: '14px',
                  borderRadius: '4px',
                  border: '1px solid #ccc'
                }}
              >
                <option value="openai">OpenAI</option>
                <option value="anthropic">Anthropic</option>
                <option value="google">Google</option>
                <option value="deepseek">DeepSeek</option>
                <option value="ollama">Ollama</option>
                <option value="openrouter">OpenRouter</option>
              </select>
            </div>

            <div style={{ marginBottom: '15px' }}>
              <label style={{ display: 'block', marginBottom: '5px', fontWeight: '500' }}>Model</label>
              <input
                type="text"
                value={config.model}
                onChange={(e) => setConfig({ ...config, model: e.target.value })}
                placeholder="e.g., gpt-4, claude-3-opus-20240229"
                style={{
                  width: '100%',
                  padding: '10px',
                  fontSize: '14px',
                  borderRadius: '4px',
                  border: '1px solid #ccc'
                }}
              />
            </div>

            <div style={{ marginBottom: '15px' }}>
              <label style={{ display: 'block', marginBottom: '5px', fontWeight: '500' }}>API Key</label>
              <input
                type="password"
                value={config.apiKey}
                onChange={(e) => setConfig({ ...config, apiKey: e.target.value })}
                placeholder="Enter your API key"
                style={{
                  width: '100%',
                  padding: '10px',
                  fontSize: '14px',
                  borderRadius: '4px',
                  border: '1px solid #ccc'
                }}
              />
            </div>

            <div style={{ marginBottom: '15px' }}>
              <label style={{ display: 'block', marginBottom: '5px', fontWeight: '500' }}>Base URL (optional)</label>
              <input
                type="text"
                value={config.baseUrl || ''}
                onChange={(e) => setConfig({ ...config, baseUrl: e.target.value })}
                placeholder="e.g., https://api.openai.com/v1"
                style={{
                  width: '100%',
                  padding: '10px',
                  fontSize: '14px',
                  borderRadius: '4px',
                  border: '1px solid #ccc'
                }}
              />
            </div>

            <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
              <button
                onClick={() => setShowConfig(false)}
                style={{
                  padding: '10px 20px',
                  backgroundColor: '#6c757d',
                  color: 'white',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer'
                }}
              >
                Cancel
              </button>
              <button
                onClick={handleSaveConfig}
                style={{
                  padding: '10px 20px',
                  backgroundColor: '#007bff',
                  color: 'white',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer',
                  fontWeight: '500'
                }}
              >
                Save & Connect
              </button>
            </div>
          </div>
        </div>
      )}

      <div style={{ 
        display: 'flex',
        flexDirection: 'column',
        gap: '20px'
      }}>
        {/* Chat History */}
        {chatHistory.length > 0 && (
          <div style={{
            padding: '20px',
            border: '1px solid #ddd',
            borderRadius: '8px',
            backgroundColor: '#f9f9f9',
            maxHeight: '400px',
            overflowY: 'auto'
          }}>
            <h2 style={{ marginBottom: '15px', marginTop: 0 }}>📜 Chat History</h2>
            {chatHistory.map((msg, index) => (
              <div 
                key={index}
                style={{
                  marginBottom: '15px',
                  padding: '12px',
                  backgroundColor: msg.role === 'user' ? '#e3f2fd' : '#fff',
                  borderLeft: `4px solid ${msg.role === 'user' ? '#2196F3' : '#4CAF50'}`,
                  borderRadius: '4px'
                }}
              >
                <strong style={{ 
                  display: 'block', 
                  marginBottom: '8px',
                  color: msg.role === 'user' ? '#1976D2' : '#388E3C'
                }}>
                  {msg.role === 'user' ? '👤 You' : '🤖 Assistant'}
                </strong>
                <div style={{ whiteSpace: 'pre-wrap', fontSize: '14px' }}>
                  {msg.content}
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Chat Interface */}
        <div style={{ 
          padding: '20px', 
          border: '1px solid #ddd',
          borderRadius: '8px',
          backgroundColor: '#f9f9f9'
        }}>
          <h2 style={{ marginBottom: '15px', marginTop: 0 }}>💬 New Message</h2>
          
          <textarea
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
                handleSend();
              }
            }}
            placeholder="Type your message here... (Ctrl/Cmd+Enter to send)"
            disabled={!llmService || isStreaming}
            style={{
              width: '100%',
              minHeight: '120px',
              padding: '12px',
              fontSize: '14px',
              borderRadius: '4px',
              border: '1px solid #ccc',
              fontFamily: 'inherit',
              resize: 'vertical',
              opacity: !llmService || isStreaming ? 0.6 : 1
            }}
          />
          
          <button
            onClick={handleSend}
            disabled={!llmService || isStreaming || !message.trim()}
            style={{
              marginTop: '10px',
              padding: '10px 24px',
              fontSize: '14px',
              backgroundColor: (llmService && !isStreaming && message.trim()) ? '#007bff' : '#ccc',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: (llmService && !isStreaming && message.trim()) ? 'pointer' : 'not-allowed',
              fontWeight: '500'
            }}
          >
            {isStreaming ? '⏳ Sending...' : '📤 Send Message'}
          </button>

          {/* Current response (streaming) */}
          {response && (
            <div style={{
              marginTop: '15px',
              padding: '15px',
              backgroundColor: '#fff',
              border: '1px solid #ddd',
              borderRadius: '4px',
              whiteSpace: 'pre-wrap'
            }}>
              <strong style={{ display: 'block', marginBottom: '10px', color: '#388E3C' }}>
                🤖 Assistant {isStreaming && '(typing...)'}
              </strong>
              <div style={{ fontSize: '14px', lineHeight: '1.6' }}>
                {response}
              </div>
            </div>
          )}
        </div>

        {/* Features */}
        <div style={{ 
          padding: '20px', 
          border: '1px solid #ddd',
          borderRadius: '8px',
          backgroundColor: '#f0f8ff'
        }}>
          <h2 style={{ marginBottom: '15px', marginTop: 0 }}>📋 Features</h2>
          <ul style={{ lineHeight: '1.8', paddingLeft: '20px', marginBottom: '20px' }}>
            <li>✅ Real-time streaming responses from LLM</li>
            <li>✅ Support multiple providers (OpenAI, Anthropic, DeepSeek, etc.)</li>
            <li>✅ Persistent configuration in browser localStorage</li>
            <li>✅ Chat history management</li>
            <li>✅ Pure TypeScript/React + mpp-core</li>
            <li>✅ No backend required - runs entirely in browser</li>
          </ul>

          <div style={{ marginTop: '20px', padding: '15px', backgroundColor: '#fff', borderRadius: '4px' }}>
            <h3 style={{ fontSize: '1rem', marginBottom: '10px', marginTop: 0 }}>⚡ Quick Start:</h3>
            <ol style={{ fontSize: '14px', lineHeight: '1.8', paddingLeft: '20px', marginBottom: 0 }}>
              <li>Click "⚙️ Settings" to configure your LLM provider</li>
              <li>Enter your API key and select a model</li>
              <li>Start chatting! Use Ctrl/Cmd+Enter to send messages</li>
            </ol>
          </div>

          <div style={{ marginTop: '15px', padding: '12px', backgroundColor: '#fff3cd', borderRadius: '4px', fontSize: '13px' }}>
            <strong>💡 Tip:</strong> Your API key is stored locally in your browser and never sent to any server except the LLM provider you configured.
          </div>
        </div>
      </div>
    </div>
  );
};

