import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import './Login.css';

export default function Login() {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const { login } = useAuth();
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setIsLoading(true);

        const result = await login(username, password);

        if (result.success) {
            navigate('/dashboard');
        } else {
            setError(result.message || 'Credenciales no autorizadas');
            setIsLoading(false);
        }
    };

    return (
        <div className="pharmacy-login-wrapper">
            <div className="login-card">
                {/* Decoración superior: Icono de salud */}
                <div className="brand-icon">
                    <div className="cross-icon">+</div>
                </div>

                <div className="login-header">
                    <h2>Portal Farmacéutico</h2>
                    <p>Gestión de Inventario y Ventas</p>
                </div>

                <form onSubmit={handleSubmit} className="login-form">
                    {error && <div className="error-badge">{error}</div>}

                    <div className="input-field">
                        <label>Usuario / Matrícula</label>
                        <input
                            type="text"
                            placeholder="Ingrese su usuario"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            required
                        />
                    </div>

                    <div className="input-field">
                        <label>Contraseña</label>
                        <input
                            type="password"
                            placeholder="••••••••"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />
                    </div>

                    <button type="submit" className={`pharmacy-btn ${isLoading ? 'loading' : ''}`} disabled={isLoading}>
                        {isLoading ? 'Verificando...' : 'Acceder al Sistema'}
                    </button>
                </form>

                <div className="login-support">
                    <p>¿Problemas con el acceso? <span>Contactar a Soporte IT</span></p>
                </div>
            </div>
        </div>
    );
}