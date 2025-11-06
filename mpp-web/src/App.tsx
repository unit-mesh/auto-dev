import React, { useState, useEffect } from 'react';
// @ts-ignore - mpp-core types will be available after build
import * as mppCore from '@autodev/mpp-core';

export const App: React.FC = () => {
  const [message, setMessage] = useState<string>('');
  const [response, setResponse] = useState<string>('');
  const [coreLoaded, setCoreLoaded] = useState<boolean>(false);

  useEffect(() => {
    // Test mpp-core loading
    try {
      console.log('mpp-core loaded:', mppCore);
      setCoreLoaded(true);
    } catch (error) {
      console.error('Failed to load mpp-core:', error);
    }
  }, []);

  const handleSend = async () => {
    if (!message.trim()) return;

    setResponse('Processing...');
    
    try {
      // TODO: Call mpp-core API to process the message
      // Example: const result = await mppCore.cc.unitmesh.devins.compiler.compile(message);
      
      // For now, just echo
      setTimeout(() => {
        setResponse(`Echo: ${message}`);
      }, 500);
    } catch (error) {
      setResponse(`Error: ${error}`);
    }
  };

  return (
    <div style={{ 
      maxWidth: '1200px', 
      margin: '0 auto', 
      padding: '20px', 
      fontFamily: 'system-ui, -apple-system, sans-serif' 
    }}>
      <header style={{ marginBottom: '30px' }}>
        <h1 style={{ fontSize: '2.5rem', fontWeight: 'bold', marginBottom: '10px' }}>
          🤖 AutoDev Web UI
        </h1>
        <p style={{ color: '#666', fontSize: '1.1rem' }}>
          Lightweight React-based web interface using mpp-core
        </p>
        {coreLoaded && (
          <p style={{ color: '#28a745', fontSize: '0.9rem', marginTop: '5px' }}>
            ✅ mpp-core loaded successfully
          </p>
        )}
      </header>

      <div style={{ 
        display: 'grid', 
        gap: '20px',
        gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))'
      }}>
        {/* Chat Interface */}
        <div style={{ 
          padding: '20px', 
          border: '1px solid #ddd',
          borderRadius: '8px',
          backgroundColor: '#f9f9f9'
        }}>
          <h2 style={{ marginBottom: '15px' }}>Chat Interface</h2>
          
          <textarea
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            placeholder="Type your message here..."
            style={{
              width: '100%',
              minHeight: '120px',
              padding: '12px',
              fontSize: '14px',
              borderRadius: '4px',
              border: '1px solid #ccc',
              fontFamily: 'inherit',
              resize: 'vertical'
            }}
          />
          
          <button
            onClick={handleSend}
            disabled={!coreLoaded}
            style={{
              marginTop: '10px',
              padding: '10px 24px',
              fontSize: '14px',
              backgroundColor: coreLoaded ? '#007bff' : '#ccc',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: coreLoaded ? 'pointer' : 'not-allowed',
              fontWeight: '500'
            }}
          >
            Send Message
          </button>

          {response && (
            <div style={{
              marginTop: '15px',
              padding: '12px',
              backgroundColor: '#fff',
              border: '1px solid #ddd',
              borderRadius: '4px',
              whiteSpace: 'pre-wrap'
            }}>
              <strong>Response:</strong><br />
              {response}
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
          <h2 style={{ marginBottom: '15px' }}>Architecture</h2>
          <ul style={{ lineHeight: '1.8', paddingLeft: '20px' }}>
            <li>✅ Pure TypeScript/React</li>
            <li>✅ Direct mpp-core integration</li>
            <li>✅ No Kotlin/JS compilation</li>
            <li>✅ Vite for fast dev/build (~2s)</li>
            <li>✅ Lightweight (~50KB React + mpp-core)</li>
            <li>✅ Browser-native APIs only</li>
          </ul>

          <div style={{ marginTop: '20px', padding: '15px', backgroundColor: '#fff', borderRadius: '4px' }}>
            <h3 style={{ fontSize: '1rem', marginBottom: '10px' }}>Same as CLI:</h3>
            <pre style={{ fontSize: '12px', overflow: 'auto' }}>
{`mpp-ui (CLI)
  ├── TypeScript/React (Ink)
  └── mpp-core

mpp-web (Web UI)
  ├── TypeScript/React (DOM)
  └── mpp-core ← Same!`}
            </pre>
          </div>
        </div>
      </div>
    </div>
  );
};

