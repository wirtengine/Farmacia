import React, { useState, useRef, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { askAssistant } from '../services/assistant';
import ReactMarkdown from 'react-markdown';
import { Send, X, MessageSquare, Bot, User, TrendingUp, AlertCircle, Calendar, Sparkles, Trash2 } from 'lucide-react';
import './ChatAssistant.css';

export default function ChatAssistant({ isDrawerOpen }) {
    const [isOpen, setIsOpen] = useState(false);
    const [messages, setMessages] = useState([
        { text: "¡Hola! Soy tu asistente de FarmaSystem. ¿En qué puedo ayudarte hoy?", sender: 'bot' }
    ]);
    const [input, setInput] = useState('');
    const [loading, setLoading] = useState(false);

    const location = useLocation();
    const messagesEndRef = useRef(null);

    // Scroll automático
    useEffect(() => {
        if (isOpen && messagesEndRef.current) {
            messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
        }
    }, [messages, isOpen, loading]);

    // Rutas donde se muestra el chat
    const mainRoutes = [
        '/dashboard',
        '/ventas',
        '/medicamentos',
        '/alerts',
        '/recommendations',
        '/perdidas'
    ];

    const isMainScreen = mainRoutes.includes(location.pathname);

    const handleSend = async (text) => {
        const query = typeof text === 'string' ? text : input;
        if (!query.trim() || loading) return;

        setMessages(prev => [...prev, { text: query, sender: 'user' }]);
        setInput('');
        setLoading(true);

        try {
            const res = await askAssistant(query);
            setMessages(prev => [...prev, { text: res.data.answer, sender: 'bot' }]);
        } catch (err) {
            setMessages(prev => [...prev, {
                text: 'Lo siento, hubo un problema de conexión.',
                sender: 'bot',
                isError: true
            }]);
        } finally {
            setLoading(false);
        }
    };

    const clearChat = () => {
        setMessages([
            { text: "Conversación reiniciada. ¿En qué puedo ayudarte?", sender: 'bot' }
        ]);
    };

    const quickSuggestions = [
        { text: "Ventas del día", icon: <TrendingUp size={14} /> },
        { text: "Bajo stock", icon: <AlertCircle size={14} /> },
        { text: "Lotes por vencer", icon: <Calendar size={14} /> },
        { text: "Sugerencias", icon: <Sparkles size={14} /> }
    ];

    if (!isMainScreen || isDrawerOpen) return null;

    return (
        <div className="fs-chat-wrapper">
            {!isOpen ? (
                <button className="fs-chat-launcher" onClick={() => setIsOpen(true)}>
                    <MessageSquare size={26} />
                    <span className="fs-online-pulse"></span>
                </button>
            ) : (
                <div className="fs-chat-card">
                    <div className="fs-chat-header">
                        <div className="fs-bot-identity">
                            <div className="fs-avatar-main">
                                <Bot size={22} />
                                <div className="fs-status-dot"></div>
                            </div>
                            <div className="fs-header-text">
                                <h3>Asistente Farma</h3>
                                <p>IA • En línea</p>
                            </div>
                        </div>
                        <div className="fs-header-actions">
                            <button className="fs-clear-chat" onClick={clearChat} title="Limpiar conversación">
                                <Trash2 size={16} />
                            </button>
                            <button className="fs-close-icon" onClick={() => setIsOpen(false)}>
                                <X size={18} />
                            </button>
                        </div>
                    </div>

                    <div className="fs-chat-body">
                        {messages.map((msg, i) => (
                            <div key={i} className={`fs-msg-group ${msg.sender}`}>
                                <div className="fs-msg-avatar">
                                    {msg.sender === 'bot' ? <Bot size={12} /> : <User size={12} />}
                                </div>
                                <div className={`fs-msg-bubble ${msg.isError ? 'error' : ''}`}>
                                    {msg.sender === 'bot' ? (
                                        <ReactMarkdown>{msg.text}</ReactMarkdown>
                                    ) : (
                                        msg.text
                                    )}
                                </div>
                            </div>
                        ))}
                        {loading && (
                            <div className="fs-msg-group bot">
                                <div className="fs-msg-avatar"><Bot size={12} /></div>
                                <div className="fs-typing"><span></span><span></span><span></span></div>
                            </div>
                        )}
                        <div ref={messagesEndRef} />
                    </div>

                    <div className="fs-footer-area">
                        <div className="fs-suggestion-row">
                            {quickSuggestions.map((s, i) => (
                                <button key={i} className="fs-pill" onClick={() => handleSend(s.text)}>
                                    {s.icon} {s.text}
                                </button>
                            ))}
                        </div>
                        <div className="fs-input-box">
                            <input
                                value={input}
                                onChange={(e) => setInput(e.target.value)}
                                onKeyDown={(e) => e.key === 'Enter' && handleSend()}
                                placeholder="Escribe..."
                                disabled={loading}
                            />
                            <button
                                className="fs-send-btn"
                                onClick={() => handleSend()}
                                disabled={!input.trim() || loading}
                            >
                                <Send size={16} />
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}